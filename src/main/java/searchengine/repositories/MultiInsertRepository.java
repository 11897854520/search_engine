package searchengine.repositories;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Repository
public class MultiInsertRepository {

    @PersistenceContext
    private EntityManager manager;

    @Transactional
    public void multiInsert(String insert) {

        manager.createNativeQuery(insert, Lemma.class).executeUpdate();
        manager.flush();
        manager.clear();
        manager.close();

    }




}
