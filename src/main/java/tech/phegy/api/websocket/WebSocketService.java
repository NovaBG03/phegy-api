package tech.phegy.api.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;


/**
 * Service for sending websockert messages.
 *
 * @author Nikita
 */
@Service
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Constructs new instance with needed dependencies.
     */
    public WebSocketService(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Send message viea websockets.
     *
     * @param username receiver.
     * @param destination websocket destination path.
     * @param payload message payload.
     */
    public void sendTo(String username, String destination, Object payload) {
        String serializedPayload;
        try {
            serializedPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return;
        }

        messagingTemplate.convertAndSendToUser(username ,destination, serializedPayload);
    }
}
