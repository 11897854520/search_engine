package searchengine.dto;

import lombok.Data;

import java.util.List;

// Класс для вывода результатов индексации.
@Data
public class StatisticsData {

    private TotalStatistics total;
    private List<DetailedStatisticsItem> detailed;

}
