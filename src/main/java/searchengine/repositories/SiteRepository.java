package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.awt.*;
import java.util.List;

@Repository
public interface SiteRepository extends CrudRepository<Site, Integer> {

    @Query(value = "SELECT * FROM site WHERE url = :page", nativeQuery = true)
    Site findByUrl(String page);

    @Query(value = "SELECT url FROM site", nativeQuery = true)
    List<String> findAllUrl();

}
