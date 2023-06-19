package searchengine.services;
import org.springframework.http.ResponseEntity;
import searchengine.dto.Response;

public interface IndexSitesService {
    Response startIndexingSitesInController();
    Response stopIndexingInController();
    Response startIndexingSingleSiteInController(String url);
    boolean isInterruptIt();
}
