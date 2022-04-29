package tech.phegy.api.service.register.listener;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import tech.phegy.api.model.token.EmailConfirmationToken;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.service.register.EmailConfirmationTokenService;
import tech.phegy.api.service.register.event.OnEmailConfirmTokenNoLongerValidEvent;

@Component
public class EmailConfirmationTokenValidityListener implements ApplicationListener<OnEmailConfirmTokenNoLongerValidEvent> {
    private final EmailConfirmationTokenService emailConfirmationTokenService;

    public EmailConfirmationTokenValidityListener(EmailConfirmationTokenService emailConfirmationTokenService) {
        this.emailConfirmationTokenService = emailConfirmationTokenService;
    }

    @Override
    public void onApplicationEvent(OnEmailConfirmTokenNoLongerValidEvent onConfirmTokenNoLongerValidEvent) {
        final EmailConfirmationToken confirmationToken = onConfirmTokenNoLongerValidEvent.getConfirmationToken();
        final PhegyUser user = confirmationToken.getUser();

        if (user.isConfirmed()) {
            this.emailConfirmationTokenService.deleteAllTokens(user);
            return;
        }

        this.emailConfirmationTokenService.deleteAllExpiredTokens(user);
    }
}
