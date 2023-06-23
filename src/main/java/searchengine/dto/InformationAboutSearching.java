package searchengine.dto;
import java.util.List;

public record InformationAboutSearching(boolean result, int count, List<InformationAboutLemmas> data) {
}
