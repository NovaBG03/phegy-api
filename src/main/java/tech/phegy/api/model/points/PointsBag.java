package tech.phegy.api.model.points;

import lombok.*;
import tech.phegy.api.model.user.PhegyUser;

import javax.persistence.*;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointsBag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "STATISTICS_POINTS_CAN_NOT_BE_NULL")
    @DecimalMin(value = "0", message = "STATISTICS_POINTS_TOO_LOW")
    private Double points;

    @OneToOne
    @JoinColumn(name = "user_id")
    private PhegyUser user;

    public void addPoints(Double points) {
        this.points += points;
    }

    public void removePoints(Double points) {
        this.points -= points;
    }
}
