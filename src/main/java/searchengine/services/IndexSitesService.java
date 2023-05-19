package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.Response;

public interface IndexSitesService {
    ResponseEntity<Response> startIndexingSitesInController();
    ResponseEntity<Response> stopIndexingInController();
    ResponseEntity<Response> startIndexingSingleSiteInController(String url);
    void interruptThread();
}
