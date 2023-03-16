package searchengine.model;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(indexes = {@Index(columnList = "lemma_id"), @Index(columnList = "page_id")})
public class SearchIndex {

    public SearchIndex(Page page, Lemma lemma, float lemmaRank) {

        this.page = page;
        this.lemma = lemma;
        this.lemmaRank = lemmaRank;

    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "page_id")
    private Page page;

    @ManyToOne
    @JoinColumn(name = "lemma_id")
    private Lemma lemma;

    @Column(nullable = false)
    private float lemmaRank;
}
