package searchengine.parser;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.model.Site;
import searchengine.repositories.*;

import java.io.IOException;
import java.util.*;

// Класс для обработки содержимого страниц.

@RequiredArgsConstructor
public class ContentHandling {


    // Метод для очищения содержимого страниц от html-тегов.
    private static String cleanedPageContents(String html) {

        return Jsoup.parse(html).text();

    }

    // Метод для перевода слов содержимого сайта в устойчивую форму (лемму) и подсчета количества лемм
    // на каждой странице.
    public static Map<String, Integer> amountOfLemmas(String text) throws IOException {

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
    public static boolean lemmasIsEquals(String one, String two) throws IOException {

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
    private static boolean unneededTypeOfWord(List<String> typeOfWord) {

        String[] types = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};

        for (String type : types) {

            return (typeOfWord.stream().map(String::toUpperCase).anyMatch(s -> s.contains(type)));

        }

        return false;

    }

    // Метод для записи данных в таблицы "lemma" и "search_Index"

    public static void writeLemmaAndIndexIntoSql(LemmaRepository lemmaRepository
            , SearchIndexRepository searchIndexRepository, List<Page> pageList, Site site) {

        if (!pageList.isEmpty()) {

           writeLemmaIntoSql(lemmaRepository, pageList, site);
           writeIndexIntoSql(lemmaRepository, searchIndexRepository, pageList, site);

        }

    }

    private static void writeLemmaIntoSql(LemmaRepository lemmaRepository,
                                          List<Page> pageList, Site site) {

        Map<String, Lemma> lemmaMap = new TreeMap<>();

        pageList.forEach(page -> {

            String html = page.getContent();
            String cleanedContent = cleanedPageContents(html);

            try {

                amountOfLemmas(cleanedContent).forEach((s, integer) -> {

                    Lemma lemma = new Lemma(site, s, 1);
                    String key = site.getName() + s;

                    if (!lemmaMap.containsKey(key)) {

                        lemmaMap.put(key, lemma);

                    } else {

                        lemmaMap.put(key, new Lemma(site, s
                                , lemmaMap.get(key).getFrequency() + 1));

                    }

                });


            } catch (Exception e) {

                System.out.println(e.getMessage());

            }

        });

        lemmaRepository.saveAll(lemmaMap.values());

    }

    private static void writeIndexIntoSql(LemmaRepository lemmaRepository
            , SearchIndexRepository searchIndexRepository, List<Page> pageList, Site site) {

        List<SearchIndex>searchIndexList = new ArrayList<>();

        pageList.forEach(page -> {

            String html = page.getContent();
            String cleanedContent = cleanedPageContents(html);

            try {

                amountOfLemmas(cleanedContent).forEach((s, integer) -> {

                    SearchIndex searchIndex = new SearchIndex(page
                            , lemmaRepository.findIdBySiteIdAndLemma(site.getId(), s), integer);
                       searchIndexList.add(searchIndex);

                });


            } catch (Exception e) {

                System.out.println(e.getMessage());

            }

        });

        searchIndexRepository.saveAll(searchIndexList);

    }


}




