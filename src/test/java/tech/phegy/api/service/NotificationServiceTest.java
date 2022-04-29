package tech.phegy.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.phegy.api.dto.notification.response.NotificationResponseDto;
import tech.phegy.api.mapper.notification.NotificationMapper;
import tech.phegy.api.mapper.notification.NotificationMapperImpl;
import tech.phegy.api.model.notification.Notification;
import tech.phegy.api.model.notification.NotificationCategory;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.repository.NotificationRepository;
import tech.phegy.api.service.validator.ModelValidatorService;
import tech.phegy.api.websocket.WebSocketService;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    NotificationRepository notificationRepository;
    @Mock
    WebSocketService webSocketService;
    @Mock
    PhegyUserService userService;
    @Mock
    ModelValidatorService modelValidatorService;
    NotificationMapper notificationMapper = new NotificationMapperImpl();

    NotificationService notificationService;

    @BeforeEach
    void initNotificationService() {
        notificationService = new NotificationService(notificationRepository,
                notificationMapper,
                webSocketService,
                userService,
                modelValidatorService);
    }

    @Test
    @DisplayName("Should push notification to user correctly")
    void shouldPushNotificationToUserCorrectly() {
        String notificationDestination = "/queue/notification";

        UUID userId = UUID.randomUUID();
        String username = "ivan";
        PhegyUser user = PhegyUser.builder().id(userId).username(username).build();

        Long notificationId = 24L;
        String notificationTitle = "title";
        String notificationMessage = "message";
        NotificationCategory notificationCategory = NotificationCategory.DANGER;
        Notification notification = Notification.builder()
                .title(notificationTitle)
                .message(notificationMessage)
                .category(notificationCategory)
                .build();

        Notification savedNotification = Notification.builder()
                .id(notificationId)
                .title(notificationTitle)
                .message(notificationMessage)
                .category(notificationCategory)
                .build();

        when(notificationRepository.save(notification))
                .thenReturn(savedNotification);

        notificationService.pushNotificationTo(notification, user);

        verify(modelValidatorService).validate(notification);

        ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationArgumentCaptor.capture());
        assertThat(notificationArgumentCaptor.getValue().getUser()).isEqualTo(user);

        ArgumentCaptor<NotificationResponseDto> dtoArgumentCaptor = ArgumentCaptor.forClass(NotificationResponseDto.class);
        verify(webSocketService).sendTo(eq(username), eq(notificationDestination), dtoArgumentCaptor.capture());
        assertThat(dtoArgumentCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("Should get all notifications related to user correctly")
    void shouldGetAllNotificationsRelatedToUserCorrectly() {
        String username = "ivan";
        Collection<Notification> notifications = List.of(
                Notification.builder()
                        .id(1L)
                        .title("title 1")
                        .message("message 1")
                        .category(NotificationCategory.INFO)
                        .build(),
                Notification.builder()
                        .id(2L)
                        .title("title 2")
                        .message("message 2")
                        .category(NotificationCategory.DANGER)
                        .build());

        when(notificationRepository.findAllByUserUsername(username))
                .thenReturn(notifications);

        Collection<Notification> notificationsFromService = notificationService.getAllNotifications(username);
        assertThat(notificationsFromService).containsExactlyElementsOf(notifications);
    }

    @Test
    @DisplayName("Should delete all notifications correctly")
    void shouldDeleteAllNotificationsCorrectly() {
        Collection<Long> idsToDelete = List.of(1L, 2L, 3L, 5L);
        String username = "ivan";

        when(userService.getUserByUsername(username))
                .thenReturn(PhegyUser.builder()
                        .username(username)
                        .notifications(List.of(
                                Notification.builder().id(1L).build(),
                                Notification.builder().id(2L).build(),
                                Notification.builder().id(3L).build(),
                                Notification.builder().id(4L).build()
                        ))
                        .build());

        notificationService.deleteAll(idsToDelete, username);

        ArgumentCaptor<List<Notification>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).deleteAll(listArgumentCaptor.capture());
        Collection<Notification> notificationsToDelete = listArgumentCaptor.getValue();

        assertThat(notificationsToDelete.size()).isEqualTo(3);
        assertThat(notificationsToDelete)
                .hasSize(3)
                .contains(Notification.builder().id(1L).build(),
                        Notification.builder().id(2L).build(),
                        Notification.builder().id(3L).build());
    }
}
