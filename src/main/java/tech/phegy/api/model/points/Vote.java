package tech.phegy.api.model.points;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import tech.phegy.api.model.Image;
import tech.phegy.api.model.user.PhegyUser;

import javax.persistence.*;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @NotNull(message = "VOTE_SUBMITTED_DATE_TIME_CAN_NOT_BE_NULL")
    private LocalDateTime submittedAt;

    @NotNull(message = "VOTE_POINTS_CAN_NOT_BE_NULL")
    @DecimalMin(value = "1", message = "VOTE_POINTS_TOO_LOW")
    private Double points;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private PhegyUser voter;

    @ManyToOne
    @JoinColumn(name = "image_id")
    private Image image;
}
