package tech.phegy.api.service.register.listener;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import tech.phegy.api.service.register.RegisterService;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.service.register.event.OnEmailConfirmationNeededEvent;

@Component
public class EmailConfirmationNeededListener implements ApplicationListener<OnEmailConfirmationNeededEvent> {
    private final RegisterService registerService;

    public EmailConfirmationNeededListener(RegisterService registerService) {
        this.registerService = registerService;
    }

    @Override
    public void onApplicationEvent(OnEmailConfirmationNeededEvent onRegistrationCompleteEvent) {
        final PhegyUser user = onRegistrationCompleteEvent.getUser();
        this.registerService.sendActivationLink(user);
    }
}
