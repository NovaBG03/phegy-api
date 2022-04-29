package tech.phegy.api.service.register.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import tech.phegy.api.model.user.PhegyUser;

@Getter
public class OnEmailConfirmationNeededEvent extends ApplicationEvent {
    private final PhegyUser user;

    public OnEmailConfirmationNeededEvent(Object source, PhegyUser user) {
        super(source);
        this.user = user;
    }
}
