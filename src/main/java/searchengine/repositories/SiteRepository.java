package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;
import searchengine.model.SiteStatus;

@Repository
public interface SiteRepository extends CrudRepository<Site, Integer> {
    Site findByUrl(String url);

    Iterable<Site> findAllByStatusAndLastError(SiteStatus siteStatus, String lastError);
}


