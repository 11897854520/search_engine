package searchengine.parser;

import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Snippet {

    // Метод для склонения слов из запроса
    public static Set<String> declension(String word) throws IOException {
        String site = "https://sklonili.ru/";
        String oneMoreSite = "https://skloneniya.ru/";
        Document document;
        Elements elements;
        document = Jsoup.connect(site.concat(word)).userAgent("Yandex").get();
        elements = document.getElementsByAttributeValue("data-title", "склонение");
        Set<String> declensions = new HashSet<>(elements.stream().map(Element::text).toList());
        document = Jsoup.connect(oneMoreSite.concat(word)).userAgent("Yandex").get();
        elements = document.select("td");
        declensions.addAll(elements.stream().map(Element::text).collect(Collectors.toSet()));
        declensions.add(word.toLowerCase(Locale.ROOT));
        return declensions;
    }

    // Метод для получения строки сниппета.
    public static String getSnippet(String content, Map<String, Set<String>> forms) throws IOException {
        String[] arrayOfLemmasFromContent = content.split(" ");
        AtomicReference<String> result = new AtomicReference<>("");
        Set<String> passedWords = new HashSet<>();
        AtomicInteger count = new AtomicInteger();
        List<String> forbiddenKey = new ArrayList<>();
        AtomicInteger anotherCount = new AtomicInteger();
        for (String string : arrayOfLemmasFromContent) {
            count.getAndIncrement();
            createStringOfSnippet(forms, string, result, arrayOfLemmasFromContent, passedWords, count
                    , forbiddenKey);
        }
        passedWords.forEach(s -> result.set(Arrays.stream(result.get()
                        .split(" ")).map(s1 -> s1.toLowerCase(Locale.ROOT)
                        .equals(s.toLowerCase(Locale.ROOT))
                        ? s1.replace(s1, "<b>".concat(s1).concat("</b>")) : " ".concat(s1).concat(" "))
                .collect(Collectors.joining())));
        result.set("<html>".concat(result.get()).concat("</html>"));
        return result.get();
    }

    // Метод для формирования строки сниппета
    private static void createStringOfSnippet(Map<String, Set<String>> forms, String string
            , AtomicReference<String> result
            , String[] arrayOfLemmasFromContent, Set<String> passedWords, AtomicInteger count,
                                              List<String> forbiddenKey) {
        forms.forEach((s, strings) -> strings.forEach(s1 -> {
            if (s1.equalsIgnoreCase(string.replaceAll("[^А-я]", ""))
                    && !string.matches("[1-9]") && result.get().length() < 220
                    && !forbiddenKey.contains(s)
            ) {
                result.set(result.get()
                        .concat(result.get().length() == 0 ? "" : "... ")
                        .concat(!string.equals(arrayOfLemmasFromContent[arrayOfLemmasFromContent.length - 1])
                                ? String.join(" ", Arrays.copyOfRange(arrayOfLemmasFromContent
                                , count.get() - 1, count.get() + 5))
                                : string));
                passedWords.add(string);
                forbiddenKey.add(s);
            }
            if (forbiddenKey.size() == forms.size()) {
                forbiddenKey.clear();
            }
        }));
    }
}
