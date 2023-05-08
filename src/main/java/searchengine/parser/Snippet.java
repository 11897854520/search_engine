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
    public static String getSnippet(String content, Set<Set<String>> forms) throws IOException {
        String[] arrayOfLemmasFromContent = content.split(" ");
        AtomicReference<String> result = new AtomicReference<>("");
        Set<String> passedWords = new HashSet<>();
        AtomicInteger count = new AtomicInteger();
        for (String string : arrayOfLemmasFromContent) {
            count.getAndIncrement();
            AtomicInteger anotherCount = new AtomicInteger();
            createStringOfSnippet(forms, string, result, arrayOfLemmasFromContent, passedWords, count
            , anotherCount);
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
    private static void createStringOfSnippet(Set<Set<String>> forms, String string
            , AtomicReference<String> result
            , String[] arrayOfLemmasFromContent, Set<String> passedWords, AtomicInteger count
            , AtomicInteger anotherCount) {
        forms.stream().flatMap(Collection::parallelStream).forEach(s -> {
            if (s.equalsIgnoreCase(string.replaceAll("[^А-я]", ""))
                    && !string.matches("[1-9]") && result.get().length() < 220
                    ) {
                result.set(anotherCount.getAndIncrement() < 2 ? result.get()
                        .concat(result.get().length() == 0 ? "" : "... ")
                        .concat(!string.equals(arrayOfLemmasFromContent[arrayOfLemmasFromContent.length - 1])
                                ? String.join(" ", Arrays.copyOfRange(arrayOfLemmasFromContent
                                , count.get() - 1, count.get() + 5))
                                : string) : result.get() );
                passedWords.add(string);
                anotherCount.getAndIncrement();
            }
        });
    }

}
