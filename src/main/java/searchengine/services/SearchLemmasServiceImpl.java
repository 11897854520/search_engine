package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.InformationAboutLemmas;
import searchengine.dto.InformationAboutSearching;
import searchengine.dto.Response;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.parser.ContentHandling;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

// Класс для нахождения страниц, на которых встречаются слова, содержащиеся в запросе.
@Service
@RequiredArgsConstructor
public class SearchLemmasServiceImpl implements SearchLemmasService {

    @Autowired
    private SearchIndexRepository searchIndexRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private SiteRepository siteRepository;

    private List<InformationAboutLemmas> listOfInformation
            = new ArrayList<>();
    private String query = "";

    // Получаем список лемм, соответствующих запросу (если не указан сайт для поиска)
    private List<Lemma> listOfLemmas(String text) throws IOException {

        List<Lemma> lemmas = new ArrayList<>();
        query = "";

        ContentHandling.amountOfLemmas(text).keySet().forEach(s -> {

            if (lemmaRepository.findByLemma(s) != null) {

                lemmas.addAll(lemmaRepository.findByLemma(s));

            }

            query = query.concat(s).concat(" ");

        });

        return lemmas;

    }

    // Получаем список лемм, соответствующих запросу (если указан сайт для поиска)
    private List<Lemma> listOfLemmas(String text, String url) throws IOException {

        List<Lemma> lemmas = new ArrayList<>();
        query = "";

        ContentHandling.amountOfLemmas(text).keySet().forEach(s -> {

            if (lemmaRepository.findBySiteIdAndLemma(siteRepository.findByUrl(url).getId()
                    , s) != null) {

                lemmas.add(lemmaRepository.findBySiteIdAndLemma(siteRepository.findByUrl(url).getId()
                        , s));

            }

            query = query.concat(s).concat(" ");

        });

        return lemmas;

    }

    // Возвращаем леммы со значением "frequency" не более 70 % от максимального значения для данного сайта
    private List<Lemma> shortedListOfLemmas(List<Lemma> listOfLemmas) {

        return listOfLemmas.stream()
                .filter(lemma -> lemma.getFrequency()
                        <= ((lemmaRepository.getMaxFrequencyBySiteId(lemma.getSite().getId()) > 100
                        ? lemmaRepository.getMaxFrequencyBySiteId(lemma.getSite().getId()) * 70 / 100
                        : lemmaRepository.getMaxFrequencyBySiteId(lemma.getSite().getId()))))
                .sorted(Comparator.comparing(Lemma::getFrequency)).collect(Collectors.toList());

    }

    // Для каждой леммы находим список страниц, на которых встречается данная лемма.
    private Map<Lemma, List<Page>> getListOfPages(List<Lemma> shortedListOfLemmas) {

        Map<Lemma, List<Page>> listOfPages = new LinkedHashMap<>();

        shortedListOfLemmas.forEach(lemma -> {

            List<Page> pages = searchIndexRepository.findByLemmaId(lemma.getId()).stream()
                    .map(SearchIndex::getPage)
                    .collect(Collectors.toList());

            listOfPages.put(lemma, pages);


        });


        return listOfPages;

    }

    // Создаем список с объектами, в которых содержится информация о наличии лемм из запроса
    // на каждой странице
    private void recordInformationOfLemmas(Map<Lemma, List<Page>> getListOfPages) {

        listOfInformation.clear();
        List<Float> listOfAbsoluteRelevance = new ArrayList<>();

        getListOfPages.values().stream().flatMap(Collection::parallelStream)
                .forEach(page -> {

                    String title = Jsoup.parse(page.getContent()).title();
                    List<Float> listOfRelevance = new ArrayList<>();
                    Set<String> toRemoveEqualsLemmas = new HashSet<>();

                    getListOfPages.keySet().forEach(lemma -> {

                        toRemoveEqualsLemmas.add(lemma.getLemma());

                        if (searchIndexRepository.getRankByLemmaIdAndPageId(lemma.getId(), page.getId()) != null) {

                            listOfRelevance.add(searchIndexRepository.getRankByLemmaIdAndPageId(lemma.getId()
                                    , page.getId()));


                        }

                    });

                    float absoluteRelevance = (float) listOfRelevance.stream()
                            .mapToDouble(Float::floatValue).sum();
                    listOfAbsoluteRelevance.add(absoluteRelevance);
                    float relativeRelevance = absoluteRelevance /
                            (listOfAbsoluteRelevance.stream().max(Float::compareTo).get());

                    if (toRemoveEqualsLemmas.size() == listOfRelevance.size()) {

                        try {

                            listOfInformation.add(new InformationAboutLemmas(page.getSite().getUrl()
                                    , page.getSite().getName(), page.getPath(), title
                                    , getSnippet(page.getContent(), query), relativeRelevance));


                        } catch (IOException e) {

                            throw new RuntimeException(e);

                        }

                    }

                });


        listOfInformation.sort(Comparator.comparing(InformationAboutLemmas::relevance).reversed());

    }

    // Метод возвращающий результаты обработки запроса.
    private InformationAboutSearching searchResult(String query, String site, int offset, int limit)
            throws IOException {

        if (offset == 0) {

            if (site != null) {

                recordInformationOfLemmas(getListOfPages(shortedListOfLemmas(listOfLemmas(query, site))));

            } else {

                recordInformationOfLemmas(getListOfPages(shortedListOfLemmas(listOfLemmas(query))));

            }

        }

        AtomicInteger count = new AtomicInteger();
        List<InformationAboutLemmas> data = new ArrayList<>();

        listOfInformation.forEach(informationAboutLemmas -> {

            count.getAndIncrement();

            if (count.get() >= offset && count.get() <= offset + limit) {

                data.add(informationAboutLemmas);

            }

        });

        data.sort(Comparator.comparing(InformationAboutLemmas::relevance).reversed());
        return new InformationAboutSearching(true, listOfInformation.size(), data);

    }


    // Метод для получения строки сниппета.
    private String getSnippet(String html, String query) throws IOException {

        String[] arrayOfWordsFromQuery = query.split(" ");
        String[] arrayOfLemmasFromHtml = Jsoup.parse(html).text().split(" ");
        AtomicReference<String> result = new AtomicReference<>("");
        List<String> passedLemmas = new ArrayList<>();
        Set<String> boldWords = new HashSet<>();

        for (String s : arrayOfWordsFromQuery) {

            int count = 0;
            String queryLemma = new RussianLuceneMorphology().getNormalForms(s).get(0);

            for (String word : arrayOfLemmasFromHtml) {

                count++;
                createStringOfSnippet(boldWords, passedLemmas, arrayOfLemmasFromHtml, queryLemma
                        , arrayOfWordsFromQuery, s, word
                        , result, count);

            }

        }

        boldWords.forEach(s -> {

            result.set(Arrays.stream(result.get()
                            .split(" ")).map(s1 -> s1.toLowerCase(Locale.ROOT).contains(s)
                            ? s1.replace(s1, "<b>".concat(s1).concat("</b>")) : " ".concat(s1).concat(" "))
                    .collect(Collectors.joining()));

        });

        result.set("<html>".concat(result.get()).concat("</html>"));
        return result.get();

    }

    // Метод для формирования строки сниппета.
    private void createStringOfSnippet(Set<String> boldWords, List<String> passedLemmas
            , String[] arrayOfLemmasFromHtml, String queryLemma, String[] arrayOfWordsFromQuery, String s
            , String word, AtomicReference<String> result, int count) throws IOException {


        if (!passedLemmas.contains(queryLemma)) {

            if (ContentHandling.lemmasIsEquals(s, word)
                    && passedLemmas.size() <= arrayOfWordsFromQuery.length) {

                boldWords.add(word.toLowerCase(Locale.ROOT).replaceAll("[^а-я]", ""));
                result.set(result.get().length() < 300 ? result.get()
                        .concat(result.get().length() == 0 ? "" : "... ")
                        .concat(!word.equals(arrayOfLemmasFromHtml[arrayOfLemmasFromHtml.length - 1])
                                ? String.join(" ", Arrays.copyOfRange(arrayOfLemmasFromHtml
                                , count - 1, count + 5)) : word) : result.get());
                passedLemmas.add(queryLemma);

            }

        }

    }

    public ResponseEntity<?> startSearchingLemmasInController(String query, String site, int offset, int limit)
            throws IOException {

        String errorResponse = "Задан пустой поисковый запрос";

        return query.isEmpty()
                ? ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, errorResponse))
                : ResponseEntity.ok(searchResult(query, site, offset, limit == 0 ? 20 : limit));

    }

}
