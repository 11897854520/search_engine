package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.entities.Lemma;

import java.util.List;

@Repository
public interface LemmaRepository extends CrudRepository<Lemma, Integer> {
    List<Lemma> findByLemma(String lemma);
    @Query(value = "SELECT MAX(frequency) FROM lemma WHERE site_id = :siteId", nativeQuery = true)
    Integer getMaxFrequencyBySiteId(int siteId);
    Lemma findBySiteIdAndLemma(int siteId, String lemma);

}
