package searchengine.model;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.SQLInsert;
import org.springframework.data.jpa.repository.Query;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SearchIndexRepository;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(indexes = {@Index(columnList = "lemma_id"), @Index(columnList = "page_id")})
public class SearchIndex {

    public SearchIndex(Page page, int lemmaId, float lemmaRank) {

        this.page = page;
        this.lemmaRank = lemmaRank;
        this.lemmaId = lemmaId;

    }


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqGen")
    @SequenceGenerator(name = "seqGen", sequenceName = "seq", initialValue = 1)
    private int id;

    @ManyToOne
    @JoinColumn(name = "page_id")
    private Page page;

    @Column(nullable = false)
    private float lemmaRank;


    @Column(name = "lemma_id")
    private int lemmaId;

}
