package searchengine.repositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.entities.Page;

@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {
}
