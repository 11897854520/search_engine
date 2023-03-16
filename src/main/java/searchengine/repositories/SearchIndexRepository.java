package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;

import java.util.List;

@Repository
public interface SearchIndexRepository extends CrudRepository<SearchIndex,Integer > {

    @Query(value = "SELECT * FROM search_index WHERE lemma_id = :id", nativeQuery = true)
    List<SearchIndex>getAllSearchIndexByLemma(int id);

    @Query(value = "SELECT lemma_rank FROM search_index WHERE lemma_id = :lemmaId AND page_id = :pageId"
    , nativeQuery = true)
    Float getRankByLemmaAndPage(int lemmaId, int pageId);
}
