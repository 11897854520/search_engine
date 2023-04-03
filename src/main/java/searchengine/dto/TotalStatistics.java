package searchengine.dto;

import lombok.Data;

// Класс, содержащий общую информацию об индексации.
@Data
public class TotalStatistics {
    private int sites;
    private int pages;
    private int lemmas;
    private boolean indexing;
}
