package searchengine.dto;

import lombok.Data;

// Класс содержащий форму ответа индексации.
@Data
public class StatisticsResponse {
    private boolean result;
    private StatisticsData statistics;
}
