package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(indexes = {@Index(columnList = "lemma_id, page_id", unique = true)})
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
