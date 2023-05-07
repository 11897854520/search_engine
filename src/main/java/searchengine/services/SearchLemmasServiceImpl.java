package searchengine.services;
import lombok.RequiredArgsConstructor;
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
import searchengine.parser.Snippet;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.repositories.SiteRepository;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
    private Map<String, InformationAboutLemmas> mapOfInformation
            = new TreeMap();

    // Получаем список лемм, соответствующих запросу (если не указан сайт для поиска)
    private List<Lemma> listOfLemmas(String text) throws IOException {
        List<Lemma> lemmas = new ArrayList<>();
        ContentHandling.getAmountOfLemmas(text).keySet().forEach(s -> {
            if (lemmaRepository.findByLemma(s) != null) {
                lemmas.addAll(lemmaRepository.findByLemma(s));
            }
        });
        return lemmas;
    }

    // Получаем список лемм, соответствующих запросу (если указан сайт для поиска)
    private List<Lemma> listOfLemmas(String text, String url) throws IOException {
        List<Lemma> lemmas = new ArrayList<>();
        ContentHandling.getAmountOfLemmas(text).keySet().forEach(s -> {
            if (lemmaRepository.findBySiteIdAndLemma(siteRepository.findByUrl(url).getId()
                    , s) != null) {
                lemmas.add(lemmaRepository.findBySiteIdAndLemma(siteRepository.findByUrl(url).getId()
                        , s));
            }
        });
        return lemmas;
    }

    // Возвращаем леммы со значением "frequency" не более 70 % от максимального значения для данного сайта
    private List<Lemma> shortedListOfLemmas(List<Lemma> listOfLemmas) {
        Map<Integer, Integer> maxFrequencies = new HashMap<>();
        siteRepository.findAll().forEach(site ->
                maxFrequencies.put(site.getId(), lemmaRepository.getMaxFrequencyBySiteId(site.getId()))
        );
        return listOfLemmas.stream()
                .filter(lemma -> lemma.getFrequency()
                        <= ((maxFrequencies.get(lemma.getSite().getId()) > 100
                        ? maxFrequencies.get(lemma.getSite().getId()) * 70 / 100
                        : maxFrequencies.get(lemma.getSite().getId()))))
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
    private void recordInformationOfLemmasIntoMap(Map<Lemma, List<Page>> getListOfPages) {
        mapOfInformation.clear();
        List<Float> listOfAbsoluteRelevance = new ArrayList<>();
        Set<Set<String>> forms = getListOfPages.keySet().stream().map(Lemma::getLemma)
                .map(s -> {
                    try {
                        return Snippet.declension(s);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toSet());
        getListOfPages.values().stream().flatMap(Collection::parallelStream)
                .forEach(page -> {
                    String title = Jsoup.parse(page.getContent()).title();
                    List<Float> listOfRelevance = new ArrayList<>();
                    String key = page.getPath() + title;
                    enumerationOfLemmasFromQueryAndWritingRanksIntoLists(getListOfPages.keySet()
                            , page, listOfRelevance);
                    float absoluteRelevance = (float) listOfRelevance.stream()
                            .mapToDouble(Float::floatValue).sum();
                    listOfAbsoluteRelevance.add(absoluteRelevance);
                    float relativeRelevance = absoluteRelevance /
                            (listOfAbsoluteRelevance.stream().max(Float::compareTo).get());
                    if (forms.size() == listOfRelevance.size() && !mapOfInformation.containsKey(key)) {
                        String content = ContentHandling.cleanedPageContents(page.getContent());
                        try {
                            mapOfInformation.put(key, new InformationAboutLemmas(page.getSite()
                                    .getUrl()
                                    , page.getSite().getName(),page.getPath()
                                    , title
                                    , Snippet.getSnippet(content, forms), relativeRelevance));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else if (mapOfInformation.containsKey(key)) {
                        mapOfInformation.put(key, new InformationAboutLemmas(page.getSite().getUrl()
                                , page.getSite().getName(), page.getPath()
                                , title
                                , mapOfInformation.get(key).snippet()
                                , mapOfInformation.get(key).relevance() + relativeRelevance));
                    }
                });
    }

    private void enumerationOfLemmasFromQueryAndWritingRanksIntoLists(Set<Lemma> listOfLemmas
            , Page page, List<Float> listOfRelevance) {
        listOfLemmas.forEach(lemma -> {
            if (searchIndexRepository.getRankByLemmaIdAndPageId(lemma.getId(), page.getId()) != null) {
                listOfRelevance.add(searchIndexRepository.getRankByLemmaIdAndPageId(lemma.getId()
                        , page.getId()));
            }
        });
    }

    // Метод возвращающий результаты обработки запроса.
    private InformationAboutSearching searchResult(String query, String site, int offset, int limit)
            throws IOException {
        long a = System.currentTimeMillis();
        if (offset == 0) {
            if (site != null) {
                recordInformationOfLemmasIntoMap(getListOfPages(shortedListOfLemmas(listOfLemmas(query, site))));
            } else {
                recordInformationOfLemmasIntoMap(getListOfPages(shortedListOfLemmas(listOfLemmas(query))));
            }
        }
        AtomicInteger count = new AtomicInteger();
        List<InformationAboutLemmas> data = new ArrayList<>();
        ArrayList<InformationAboutLemmas> listOfInformation = new ArrayList<>(mapOfInformation.values()
                .stream().toList());
        listOfInformation.sort(Comparator.comparing(InformationAboutLemmas::relevance).reversed());
        listOfInformation.forEach(informationAboutLemmas -> {
            count.getAndIncrement();
            if (count.get() >= offset && count.get() <= offset + limit) {
                data.add(informationAboutLemmas);
            }
        });
        data.sort(Comparator.comparing(InformationAboutLemmas::relevance).reversed());
        long b = System.currentTimeMillis();
        System.out.println(b - a);
        return new InformationAboutSearching(true, mapOfInformation.size(), data);
    }

    public ResponseEntity<?> startSearchingLemmasInController(String query, String site, int offset, int limit)
            throws IOException {
        String errorResponse = "Задан пустой поисковый запрос";
        return query.isEmpty()
                ? ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, errorResponse))
                : ResponseEntity.ok(searchResult(query, site, offset, limit == 0 ? 20 : limit));
    }
}
