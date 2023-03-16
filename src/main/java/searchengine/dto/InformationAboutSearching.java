package searchengine.dto;
import java.util.List;

// Рекорд-класс для выведения результатов поиска.
public record InformationAboutSearching(boolean result, int count, List<InformationAboutLemmas> data) {

}
