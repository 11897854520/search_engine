package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SearchIndex;

import java.util.List;

@Repository
public interface SearchIndexRepository extends CrudRepository<SearchIndex,Integer > {

    List<SearchIndex> findByLemmaId(int id);

    @Query(value = "SELECT lemma_rank FROM search_index WHERE lemma_id = :lemmaId AND page_id = :pageId"
    , nativeQuery = true)
    Float getRankByLemmaIdAndPageId(int lemmaId, int pageId);
}
