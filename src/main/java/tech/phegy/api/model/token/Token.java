package tech.phegy.api.model.token;

import lombok.*;
import tech.phegy.api.model.user.PhegyUser;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Token implements Comparable<Token> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false)
    @NotNull(message = "CONFIRMATION_TOKEN_TOKEN_NULL")
    private String hashedToken;

    @NotNull(message = "CONFIRMATION_TOKEN_CREATED_AT_NULL")
    private LocalDateTime createdAt;

    @NotNull(message = "CONFIRMATION_TOKEN_EXPIRATION_TIME_NULL")
    private Duration expirationTime;

    @ManyToOne()
    @JoinColumn(name = "user_id", nullable = false)
    private PhegyUser user;

    public boolean isExpired() {
        return createdAt.plus(expirationTime).isBefore(LocalDateTime.now());
    }

    @Override
    public int compareTo(@org.jetbrains.annotations.NotNull Token o) {
        if (!this.getClass().equals(o.getClass())) {
            return -1;
        }

        if (this.isExpired() == o.isExpired()) {
            return this.getCreatedAt().compareTo(o.getCreatedAt());
        }
        if (this.isExpired()) {
            return 1;
        }
        return -1;
    }
}
