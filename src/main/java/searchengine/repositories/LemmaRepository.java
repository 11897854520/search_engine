package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import java.util.List;

@Repository
public interface LemmaRepository extends CrudRepository<Lemma, Integer> {

    @Query(value = "SELECT * FROM lemma WHERE `lemma` = :lemma", nativeQuery = true)
    List<Lemma> findByLemma(String lemma);

    @Query(value = "SELECT * FROM lemma WHERE site_id = :siteId AND lemma = :lemma", nativeQuery = true)
    Lemma findBySiteIdAndByLemma(int siteId, String lemma);

    @Query(value = "SELECT MAX(frequency) FROM lemma WHERE site_id = :siteId", nativeQuery = true)
    int findMaxFrequencyBySiteId(int siteId);
}
