package searchengine.parser;
import org.apache.lucene.morphology.LuceneMorphology;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Snippet {

    public static String getSnippet(String content, Set<String> lemmas,
                                    LuceneMorphology luceneMorphology) {
        AtomicReference<String> result = new AtomicReference<>("");
        String[] words = content.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ").trim().split(" ");
        List<String> forbiddenLemmas = new ArrayList<>();
        createStringOfSnippet(words, luceneMorphology, lemmas, forbiddenLemmas, result);
        return result.get();
    }

    private static void createStringOfSnippet(String[] words, LuceneMorphology luceneMorphology
            , Set<String> lemmas, List<String> forbiddenLemmas, AtomicReference<String> result) {
        int count = 0;
        for (String word : words) {
            count++;
            if (word.isEmpty()) {
                continue;
            }
            List<String> typeOfWord = luceneMorphology.getMorphInfo(word);
            List<String> normalWord = luceneMorphology.getNormalForms(word);
            if (normalWord.isEmpty() || ContentHandling.unneededTypeOfWord(typeOfWord)) {
                continue;
            }
            if (lemmas.contains(normalWord.get(0))
                    && !forbiddenLemmas.contains(normalWord.get(0)) && result.get().length() < 220) {
                result.set(result.get().concat(result.get().length() == 0 ? "" : "... ")
                        .concat(!word.equals(words[words.length - 1])
                                ? "<b>".concat(word).concat("</b>").concat(" ")
                                .concat(String.join(" ", Arrays.copyOfRange(words
                                        , count, count + 5)))
                                : word));
                forbiddenLemmas.add(normalWord.get(0));
            }
            if (forbiddenLemmas.size() == lemmas.size()) {
                forbiddenLemmas.clear();
            }
        }
    }
}
