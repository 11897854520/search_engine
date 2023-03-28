package searchengine.controllers;

import org.springframework.http.HttpStatus;
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
    public ResponseEntity<Response> startIndexing() {

      return indexSitesService.startIndexingSitesInController();

    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Response> stopIndexing() {

     return indexSitesService.stopIndexingInController();

    }

    @PostMapping(value = "/indexPage")
    public ResponseEntity<Response> indexSinglePage(@RequestParam String url) {

       return indexSitesService.startIndexingSingleSiteInController(url);

    }

    @GetMapping(value = "/search")
    public ResponseEntity<?> searchLemma(@RequestParam String query, String site, int offset, int limit)
            throws IOException {

        return searchLemmasService.startSearchingLemmasInController(query, site, offset, limit);

    }

}
