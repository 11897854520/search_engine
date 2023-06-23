package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.Response;
import searchengine.dto.StatisticsResponse;
import searchengine.services.*;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexSitesService indexSitesService;
    private final SearchLemmasService searchLemmasService;

    public ApiController(StatisticsService statisticsService, IndexSitesService indexSitesService
            , SearchLemmasService searchLemmasService) {
        this.statisticsService = statisticsService;
        this.indexSitesService = indexSitesService;
        this.searchLemmasService = searchLemmasService;
    }

    @RequestMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public Response startIndexing() {
        return indexSitesService.startIndexingAllSites();
    }

    @GetMapping("/stopIndexing")
    public Response stopIndexing() {
        return indexSitesService.stopIndexingAllSites();
    }

    @PostMapping(value = "/indexPage")
    public Response indexSinglePage(@RequestParam String url) {
        return indexSitesService.startIndexingSingleSite(url);
    }

    @GetMapping(value = "/search")
    public ResponseEntity<?> searchLemma(@RequestParam String query, String site, int offset, int limit)
            throws IOException {
        return searchLemmasService.startSearchingLemmas(query, site, offset, limit);
    }
}
