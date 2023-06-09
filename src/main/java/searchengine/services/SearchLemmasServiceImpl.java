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
import searchengine.entities.Lemma;
import searchengine.entities.Page;
import searchengine.entities.SearchIndex;
import searchengine.parser.ContentHandling;
import searchengine.parser.Snippet;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


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

    private Map<Lemma, List<Page>> getListOfPages(List<Lemma> shortedListOfLemmas) {
        listOfInformation.clear();
        Map<Lemma, List<Page>> listOfPages = new LinkedHashMap<>();
        shortedListOfLemmas.forEach(lemma -> {
            List<Page> pages = searchIndexRepository.findByLemmaId(lemma.getId()).stream()
                    .map(SearchIndex::getPage)
                    .collect(Collectors.toList());
            listOfPages.put(lemma, pages);
        });
        return listOfPages;
    }

    private void recordInformationAboutLemmas(Map<Lemma, List<Page>> getListOfPages) {
        listOfInformation.clear();
        Map<String, InformationAboutLemmas> mapOfInformation = new TreeMap();
        List<Float> listOfAbsoluteRelevance = new ArrayList<>();
        Set<String> lemmas = getListOfPages.keySet().stream().map(Lemma::getLemma).collect(Collectors.toSet());
        getListOfPages.values().stream().flatMap(Collection::parallelStream)
                .forEach(page -> compileInformationAboutLemmas(page, getListOfPages, listOfAbsoluteRelevance
                        , mapOfInformation, lemmas));
        float maxOfAbsoluteRelevance = !listOfAbsoluteRelevance.isEmpty() ? listOfAbsoluteRelevance.stream()
                .max(Comparator.naturalOrder()).get() : 0;
        listOfInformation.addAll(createListOfInformationAboutLemmas(mapOfInformation
                , maxOfAbsoluteRelevance));
    }

    private void compileInformationAboutLemmas(Page page, Map<Lemma, List<Page>> getListOfPages
    , List<Float> listOfAbsoluteRelevance, Map<String, InformationAboutLemmas> mapOfInformation, Set<String> lemmas) {
        String title = Jsoup.parse(page.getContent()).title();
        List<Float> listOfRelevance = new ArrayList<>();
        String key = page.getPath() + title + page.getSite().getName();
        writeRanksIntoList(getListOfPages.keySet()
                , page, listOfRelevance);
        float absoluteRelevance = (float) listOfRelevance.stream()
                .mapToDouble(Float::floatValue).sum();
        listOfAbsoluteRelevance.add(absoluteRelevance);
        createInformationAboutLemmasAndPutIntoMap(mapOfInformation, lemmas, listOfRelevance, key, page, title
                , absoluteRelevance);
    }

    private List<InformationAboutLemmas> createListOfInformationAboutLemmas(
            Map<String, InformationAboutLemmas> mapOfInformation, float maxOfAbsoluteRelevance) {
        return mapOfInformation.entrySet().stream().map(stringInformationAboutLemmasEntry
                        -> stringInformationAboutLemmasEntry
                        .setValue(new InformationAboutLemmas(stringInformationAboutLemmasEntry.getValue().site()
                                , stringInformationAboutLemmasEntry.getValue().siteName()
                                , stringInformationAboutLemmasEntry.getValue().uri()
                                , stringInformationAboutLemmasEntry.getValue().title()
                                , stringInformationAboutLemmasEntry.getValue().snippet()
                                , stringInformationAboutLemmasEntry.getValue().relevance()
                                / maxOfAbsoluteRelevance)))
                .collect(Collectors.toList());
    }

    private void createInformationAboutLemmasAndPutIntoMap(Map<String, InformationAboutLemmas> mapOfInformation
            , Set<String> lemmas, List<Float> listOfRelevance, String key, Page page, String title, float absoluteRelevance) {
        if (lemmas.size() == listOfRelevance.size() && !mapOfInformation.containsKey(key)) {
            String content = ContentHandling.cleanedPageContents(page.getContent());
            try {
                mapOfInformation.put(key, new InformationAboutLemmas(page.getSite()
                        .getUrl()
                        , page.getSite().getName(), page.getPath()
                        , title
                        , Snippet.getSnippet(content, lemmas)
                        , absoluteRelevance));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (lemmas.size() == listOfRelevance.size() && mapOfInformation.containsKey(key)) {
            mapOfInformation.put(key, new InformationAboutLemmas(page.getSite().getUrl()
                    , page.getSite().getName(), page.getPath()
                    , title
                    , mapOfInformation.get(key).snippet()
                    , mapOfInformation.get(key).relevance()));
        }
    }

    private void writeRanksIntoList(Set<Lemma> listOfLemmas
            , Page page, List<Float> listOfRelevance) {
        listOfLemmas.forEach(lemma -> {
            if (searchIndexRepository.getRankByLemmaIdAndPageId(lemma.getId(), page.getId()) != null) {
                listOfRelevance.add(searchIndexRepository.getRankByLemmaIdAndPageId(lemma.getId()
                        , page.getId()));
            }
        });
    }

    private InformationAboutSearching searchResult(String query, String site, int offset, int limit)
            throws IOException {
        if (offset == 0) {
            if (site != null) {
                recordInformationAboutLemmas(getListOfPages(shortedListOfLemmas(listOfLemmas(query, site))));
            } else {
                recordInformationAboutLemmas(getListOfPages(shortedListOfLemmas(listOfLemmas(query))));
            }
        }
       return createInformationAboutSearching(offset, limit);
    }

    private InformationAboutSearching createInformationAboutSearching(int offset, int limit) {
        AtomicInteger count = new AtomicInteger();
        listOfInformation.sort(Comparator.comparing(InformationAboutLemmas::relevance).reversed());
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

    public ResponseEntity<?> startSearchingLemmas(String query, String site, int offset, int limit)
            throws IOException {
        String errorResponse = "Задан пустой поисковый запрос";
        return query.isEmpty()
                ? ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, errorResponse))
                : ResponseEntity.ok(searchResult(query, site, offset, limit == 0 ? 20 : limit));
    }
}
