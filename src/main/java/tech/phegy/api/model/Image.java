package tech.phegy.api.model;

import com.google.common.collect.Sets;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import tech.phegy.api.model.points.Vote;
import tech.phegy.api.model.user.PhegyUser;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Collection;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @NotNull(message = "IMAGE_TITLE_NULL")
    @Length.List({
            @Length(min = 3, message = "IMAGE_TITLE_TOO_SHORT"),
            @Length(max = 30, message = "IMAGE_TITLE_TOO_LONG")
    })
    private String title;

    @Length.List({
            @Length(min = 3, message = "IMAGE_DESCRIPTION_TOO_SHORT"),
            @Length(max = 100, message = "IMAGE_DESCRIPTION_TOO_LONG")
    })
    private String description;

    @Lob
    @NotNull(message = "IMAGE_KEY_NULL")
    private String imageKey;

    @ManyToOne
    @JoinColumn(name = "publisher_id", nullable = false)
    private PhegyUser publisher;

    private LocalDateTime publishedOn;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private PhegyUser approvedBy;

    private LocalDateTime approvedOn;

    @Builder.Default
    @OneToMany(mappedBy = "image", cascade = CascadeType.REMOVE)
    private Collection<Vote> votes = Sets.newHashSet();

    public boolean isApproved() {
        return this.approvedOn != null
                && this.approvedOn.isBefore(LocalDateTime.now());
    }
}
