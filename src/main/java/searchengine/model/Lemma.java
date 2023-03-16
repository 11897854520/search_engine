package searchengine.model;
import lombok.*;
import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"site_id", "lemma"}),
        indexes = @Index(columnList = "lemma"))

public class Lemma {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private Site site;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String lemma;

    @Column(nullable = false)
    private int frequency;



}
