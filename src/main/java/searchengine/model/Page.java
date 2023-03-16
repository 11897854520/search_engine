package searchengine.model;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(indexes = @Index(columnList = "path"))
public class Page {

    public Page(String path, int code, String content, Site site) {

        this.path = path;
        this.code = code;
        this.content = content;
        this.site = site;

    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private Site site;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
    private List<SearchIndex>indexList;
}