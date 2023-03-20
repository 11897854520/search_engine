package searchengine.auxiliary;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.*;

import java.io.IOException;
import java.util.*;

// Класс для обработки содержимого страниц.
@Service
@RequiredArgsConstructor
public class ContentHandlingImpl implements ContentHandling {

    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private SearchIndexRepository searchIndexRepository;
    @Autowired
    private insertRepository insertRepository;


    // Метод для очищения содержимого страниц от html-тегов.
    private String cleanedPageContents(String html) {

        return Jsoup.parse(html).text();

    }

    // Метод для перевода слов содержимого сайта в устойчивую форму (лемму) и подсчета количества лемм
    // на каждой странице.
    public Map<String, Integer> amountOfLemmas(String text) throws IOException {

        Map<String, Integer> lemmas = new HashMap<>();
        String[] words = text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ").trim().split(" ");
        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();

        for (String word : words) {

            if (word.isEmpty()) {

                continue;

            }

            List<String> typeOfWord = luceneMorphology.getMorphInfo(word);
            List<String> normalWord = luceneMorphology.getNormalForms(word);

            if (normalWord.isEmpty() || unneededTypeOfWord(typeOfWord)) {

                continue;

            }

            if (lemmas.containsKey(normalWord.get(0))) {

                lemmas.put(normalWord.get(0), lemmas.get(normalWord.get(0)) + 1);

            } else {

                lemmas.put(normalWord.get(0), 1);

            }

        }

        return lemmas;

    }

    // Метод для сравнения лемм разных слов.
    public boolean lemmasIsEquals(String one, String two) throws IOException {

        String oneChanged = one.toLowerCase(Locale.ROOT).replaceAll("([^а-я\\s])", "");
        String twoChanged = two.toLowerCase(Locale.ROOT).replaceAll("([^а-я\\s])", "");
        String startsWith = oneChanged.length() > 4 ? oneChanged.substring(0, oneChanged.length() / 2)
                : oneChanged;

        if (!oneChanged.isEmpty() && !twoChanged.isEmpty()) {

            if (twoChanged.startsWith(startsWith) && oneChanged.length() > 1) {

                String lemmaOne = new RussianLuceneMorphology().getNormalForms(oneChanged).get(0);
                String lemmaTwo = new RussianLuceneMorphology().getNormalForms(twoChanged).get(0);


                return lemmaOne.equals(lemmaTwo);

            }


        }

        return false;

    }

    // Метод для определения соответствия слова указанному типу.
    private boolean unneededTypeOfWord(List<String> typeOfWord) {

        String[] types = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};

        for (String type : types) {

            return (typeOfWord.stream().map(String::toUpperCase).anyMatch(s -> s.contains(type)));

        }

        return false;

    }

    // Метод для записи данных в таблицы "lemma" и "search_Index"
    @Override
    public void writeLemmasInSql(List<Page> pageList, Site site) {

        StringBuilder builder = new StringBuilder();
        StringBuilder anotherBuilder = new StringBuilder();

        if (!pageList.isEmpty()) {

            pageList.forEach(page -> {

                String html = page.getContent();
                String cleanedContent = cleanedPageContents(html);

                try {

                    amountOfLemmas(cleanedContent).forEach((s, integer) -> {

                        anotherBuilder.append(anotherBuilder.length() == 0 ? "" : ", ").append("(" + "'"
                                + page.getId() + "'" + ", "
                                + "(SELECT id FROM lemma WHERE site_id =" + "'" + site.getId() + "'"
                                + " AND `lemma` =" + "'" + s + "')" + ", " + "'" + integer + "'" + ")");

                        builder.append(builder.length() == 0 ? "" : ", ").append("(" + "'"
                                + site.getId() + "'" + ", " + "'" + s + "'" + ", " + "'" + 1 + "'" + ")");


                    });

                } catch (Exception e) {

                    System.out.println(e.getMessage());

                }


            });


            String lemmaInsert = "INSERT INTO lemma(site_id, lemma, frequency) VALUES"
                    + builder +
                    "ON DUPLICATE KEY UPDATE frequency = frequency + 1";
            String searchIndexInsert = "INSERT INTO search_index(page_id, lemma_id, lemma_rank) VALUES"
                    + anotherBuilder;


            insertRepository.insert(lemmaInsert);
            insertRepository.insert(searchIndexInsert);

        }


    }


}




