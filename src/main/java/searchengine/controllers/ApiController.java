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
    private final IndexSites indexSites;
    private final ContentHandling contentHandling;
    private final SearchLemmas searchLemmas;

    public ApiController(StatisticsService statisticsService, IndexSites indexSites
            , ContentHandling contentHandling, SearchLemmas searchLemmas) {

        this.statisticsService = statisticsService;
        this.indexSites = indexSites;
        this.contentHandling = contentHandling;
        this.searchLemmas = searchLemmas;

    }


    @RequestMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Response> startIndexing() {

        String errorResponse = "Индексация уже запущена";

        if (!indexSites.isIndexed() && !IndexSitesImpl.isInterruptIt()) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, errorResponse));

        }

        indexSites.index();
        return ResponseEntity.ok(new Response(true, null));

    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Response> stopIndexing() {

        String errorResponse = "Индексация не запущена";

        if (indexSites.isInterrupted()) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, errorResponse));

        }

        indexSites.stopIndexing();
        return ResponseEntity.ok(new Response(true, null));

    }

    @PostMapping(value = "/indexPage")
    public ResponseEntity<Response> indexSinglePage(@RequestParam String url) {

        String errorResponse = "Данная страница находится за пределами сайтов, " +
                "указанных в конфигурационном файле";

        indexSites.indexSingleSite(url);

        return IndexSitesImpl.isSitesContainsUrl() ? ResponseEntity.ok(new Response(true, null))
                :  ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, errorResponse));

    }

    @GetMapping(value = "/search")
    public ResponseEntity<?> searchLemma(@RequestParam String query, String site, int offset, int limit)
            throws IOException {

        String errorResponse = "Задан пустой поисковый запрос";

        return query.isEmpty()
                ? ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, errorResponse))
                : ResponseEntity.ok(searchLemmas.searchResult(query, site, offset, limit == 0 ? 20 : limit));

    }

}
