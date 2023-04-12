package searchengine.model;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(indexes = {@Index(columnList = "site_id, lemma", unique = true)})
public class Lemma {
    public Lemma(Site site, String lemma, int frequency) {
        this.site = site;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqGen")
    @SequenceGenerator(name = "seqGen", sequenceName = "seq", initialValue = 1)
    private int id;
    @ManyToOne
    @JoinColumn(name = "site_id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private Site site;
    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String lemma;
    @Column(nullable = false)
    private int frequency;
}
