package searchengine.parser;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import searchengine.entities.Lemma;
import searchengine.entities.Page;
import searchengine.entities.SearchIndex;
import searchengine.entities.Site;
import searchengine.repositories.*;

import java.io.IOException;
import java.util.*;

// Класс для обработки содержимого страниц.

@RequiredArgsConstructor
public class ContentHandling {

    // Метод для очищения содержимого страниц от html-тегов.
    public static String cleanedPageContents(String html) {
        return Jsoup.parse(html).text()
                .toLowerCase(Locale.ROOT).replace('\u000B', ' ')
                .replaceAll("([^а-я\\s\\t])", " ");
    }

    // Метод для перевода слов содержимого сайта в устойчивую форму (лемму) и подсчета количества лемм
    // на каждой странице.
    public static Map<String, Integer> getAmountOfLemmas(String text) throws IOException {
        Map<String, Integer> lemmas = new HashMap<>();
        String[] words = text.trim().split(" ");
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

    // Метод для определения соответствия слова указанному типу.
    protected static boolean unneededTypeOfWord(List<String> typeOfWord) {
        String[] types = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
        for (String type : types) {
            return (typeOfWord.stream().map(String::toUpperCase).anyMatch(s -> s.contains(type)));
        }
        return false;
    }

    //Метод для записи лемм и поисковых индексов в базу данных.
    public static synchronized void writeLemmasAndSearchIndexIntoSql(Set<Page> pageSet, Site site
            , LemmaRepository lemmaRepository, SearchIndexRepository searchIndexRepository
            , Map<String, Integer> frequencyOfLemmas) {
        Map<String, Lemma> lemmaMap = new HashMap<>();
        Map<Page, Map<String, Integer>> ranksOfLemmas = new HashMap<>();
        if (!pageSet.isEmpty()) {
            enumOfLemmasAndWritingIntoMap(pageSet, frequencyOfLemmas
                    , site, lemmaRepository, lemmaMap, ranksOfLemmas);
            lemmaRepository.saveAll(lemmaMap.values());
            searchIndexRepository.saveAll(enumOfLemmasAndCreateSearchIndex(lemmaMap, ranksOfLemmas));
        }
    }

    private static void enumOfLemmasAndWritingIntoMap(Set<Page> pageSet
            , Map<String, Integer> frequencyOfLemmas, Site site, LemmaRepository lemmaRepository
            , Map<String, Lemma> lemmaMap, Map<Page, Map<String, Integer>> ranksOfLemmas) {
        pageSet.forEach(page -> {
            String cleanedContent = cleanedPageContents(page.getContent());
            Map<String, Integer> ranks = new HashMap<>();
            try {
                getAmountOfLemmas(cleanedContent).forEach((s, integer) -> {
                    Lemma lemma;
                    if (!frequencyOfLemmas.containsKey(s) && !lemmaMap.containsKey(s)) {
                        lemma = new Lemma(site, s, 1);
                        frequencyOfLemmas.put(s, 1);
                        lemmaMap.put(s, lemma);
                    } else if (frequencyOfLemmas.containsKey(s) && !lemmaMap.containsKey(s)) {
                        lemma = lemmaRepository.findBySiteIdAndLemma(site.getId(), s);
                        lemma.setFrequency(lemma.getFrequency() + 1);
                        frequencyOfLemmas.put(s, lemma.getFrequency());
                        lemmaMap.put(s, lemma);
                    } else if (frequencyOfLemmas.containsKey(s) && lemmaMap.containsKey(s)) {
                        lemma = lemmaMap.get(s);
                        lemma.setFrequency(lemma.getFrequency() + 1);
                        frequencyOfLemmas.put(s, lemma.getFrequency());
                        lemmaMap.put(s, lemma);
                    }
                    ranks.put(s, integer);
                });
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            ranksOfLemmas.put(page, ranks);
        });
    }

    private static List<SearchIndex> enumOfLemmasAndCreateSearchIndex(Map<String
            , Lemma> lemmaMap, Map<Page, Map<String, Integer>> ranksOfLemmas) {
        List<SearchIndex> searchIndexList = new ArrayList<>();
        ranksOfLemmas.forEach((page, stringIntegerMap) -> stringIntegerMap.forEach((s, integer) -> searchIndexList.add(new SearchIndex(page
                , lemmaMap.get(s), integer))));
        return searchIndexList;
    }
}




