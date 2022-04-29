package tech.phegy.api.service.jwt.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import tech.phegy.api.model.user.PhegyUser;

@Getter
public class OnTooManyRefreshTokensForUser extends ApplicationEvent {
    private final PhegyUser user;

    public OnTooManyRefreshTokensForUser(Object source, PhegyUser user) {
        super(source);
        this.user = user;
    }
}
