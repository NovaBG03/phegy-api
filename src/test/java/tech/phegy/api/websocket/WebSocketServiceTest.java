package tech.phegy.api.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketServiceTest {
    @Mock
    SimpMessagingTemplate messagingTemplate;
    @Mock
    ObjectMapper objectMapper;

    WebSocketService webSocketService;

    @BeforeEach
    void setUp() {
        webSocketService = new WebSocketService(messagingTemplate, objectMapper);
    }

    @Test
    @DisplayName("Should send message successfully")
    void shouldSendMessageSuccessfully() throws JsonProcessingException {
        String username = "username";
        String destination = "dest";
        Object paylaod = new Object();
        String serializedPayload = "payload";

        when(objectMapper.writeValueAsString(paylaod)).thenReturn(serializedPayload);

        webSocketService.sendTo(username, destination, paylaod);

        verify(messagingTemplate).convertAndSendToUser(username, destination, serializedPayload);
    }

    @Test
    @DisplayName("Should ignore message when can not serialize payload")
    void shouldIgnoreMessageWhenCanNotSerializePayload() throws JsonProcessingException {
        String username = "username";
        String destination = "dest";
        Object paylaod = new Object();

        when(objectMapper.writeValueAsString(paylaod)).thenThrow(JsonProcessingException.class);

        webSocketService.sendTo(username, destination, paylaod);

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), anyString());
    }
}