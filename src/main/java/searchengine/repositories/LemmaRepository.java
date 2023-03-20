package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import java.util.List;


@Repository
public interface LemmaRepository extends CrudRepository<Lemma, Integer> {

    List<Lemma> findByLemma(String lemma);

    Lemma findBySiteIdAndLemma(int siteId, String lemma);

    @Query(value = "SELECT MAX(frequency) FROM lemma WHERE site_id = :siteId", nativeQuery = true)
    int getMaxFrequencyBySiteId(int siteId);
}
