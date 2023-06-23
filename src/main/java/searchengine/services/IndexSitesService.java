package searchengine.services;
import searchengine.dto.Response;

public interface IndexSitesService {
    Response startIndexingAllSites();
    Response stopIndexingAllSites();
    Response startIndexingSingleSite(String url);
    boolean isInterruptIt();
}
