package tech.phegy.api.service.jwt.listener;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import tech.phegy.api.service.jwt.RefreshTokenService;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.service.jwt.event.OnTooManyRefreshTokensForUser;

@Component
public class RefreshTokenCleanerListener implements ApplicationListener<OnTooManyRefreshTokensForUser> {
    private final RefreshTokenService refreshTokenService;

    public RefreshTokenCleanerListener(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public void onApplicationEvent(OnTooManyRefreshTokensForUser onTooMuchRefreshTokensForUser) {
        final PhegyUser user = onTooMuchRefreshTokensForUser.getUser();
        refreshTokenService.clearTokens(user);
    }
}
