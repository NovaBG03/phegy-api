package tech.phegy.api.model.user;

import lombok.*;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhegyRole {
    @Id
    @GeneratedValue
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 25, unique = true)
    private PhegyRoleLevel level;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhegyRole role = (PhegyRole) o;
        return Objects.equals(id, role.id) && level.equals(role.level);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, level);
    }
}
