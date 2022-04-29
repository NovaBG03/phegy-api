package tech.phegy.api.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import tech.phegy.api.service.jwt.JwtService;
import tech.phegy.api.service.jwt.JwtProps;

import java.util.List;

public class AuthChannelInterceptor implements ChannelInterceptor {
    private final JwtService jwtService;
    private final JwtProps jwtConfig;

    public AuthChannelInterceptor(JwtService jwtService, JwtProps jwtConfig) {
        this.jwtService = jwtService;
        this.jwtConfig = jwtConfig;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        List<String> tokenList = accessor.getNativeHeader(jwtConfig.getAuthTokenHeader());
        String authorizationToken;
        if (tokenList == null || tokenList.size() < 1) {
            return message;
        } else {
            authorizationToken = tokenList.get(0);
            if (authorizationToken == null) {
                return message;
            }
        }
        Authentication user = jwtService.getAuthentication(authorizationToken); // access authentication header(s)
        accessor.setUser(user);

        return message;
    }
}
