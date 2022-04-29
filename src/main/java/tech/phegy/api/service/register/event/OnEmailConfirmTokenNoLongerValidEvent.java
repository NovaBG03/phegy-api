package tech.phegy.api.service.register.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import tech.phegy.api.model.token.EmailConfirmationToken;

@Getter
public class OnEmailConfirmTokenNoLongerValidEvent extends ApplicationEvent {
    private final EmailConfirmationToken confirmationToken;

    public OnEmailConfirmTokenNoLongerValidEvent(Object source, EmailConfirmationToken confirmationToken) {
        super(source);
        this.confirmationToken = confirmationToken;
    }
}
