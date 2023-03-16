package searchengine.dto;
import lombok.Data;
import java.time.LocalDateTime;

// Класс для вывода подробной информации о статистике индексации для каждого сайта.
@Data
public class DetailedStatisticsItem {

    private String url;
    private String name;
    private String status;
    private LocalDateTime statusTime;
    private String error;
    private int pages;
    private int lemmas;

}
