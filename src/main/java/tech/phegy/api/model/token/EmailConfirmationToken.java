package tech.phegy.api.model.token;

import lombok.*;
import tech.phegy.api.model.user.PhegyUser;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class EmailConfirmationToken extends Token {

    @NotNull(message = "CONFIRMATION_TOKEN_ORIGIN_EMAIL_NULL")
    private String originEmail;

    @Builder
    public EmailConfirmationToken(Long id,
                                  String hashedToken,
                                  LocalDateTime createdAt,
                                  Duration expirationTime,
                                  PhegyUser user,
                                  String originEmail) {
        super(id, hashedToken, createdAt, expirationTime, user);
        this.originEmail = originEmail;
    }

    @Override
    public boolean isExpired() {
        return super.isExpired() || !super.getUser().getEmail().equals(this.originEmail);
    }
}
