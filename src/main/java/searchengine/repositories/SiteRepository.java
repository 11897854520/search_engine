package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.entities.Site;
import searchengine.entities.SiteStatus;

@Repository
public interface SiteRepository extends CrudRepository<Site, Integer> {
    Site findByUrl(String url);
    Iterable<Site> findAllByStatusAndLastError(SiteStatus siteStatus, String lastError);
}


