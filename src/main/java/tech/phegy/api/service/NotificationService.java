package tech.phegy.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.phegy.api.dto.notification.response.NotificationResponseDto;
import tech.phegy.api.repository.NotificationRepository;
import tech.phegy.api.websocket.WebSocketService;
import tech.phegy.api.mapper.notification.NotificationMapper;
import tech.phegy.api.model.notification.Notification;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.service.validator.ModelValidatorService;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Service for managing user account notifications
 * @author Nikita
 */
@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final WebSocketService webSocketService;
    private final PhegyUserService userService;
    private final ModelValidatorService modelValidatorService;

    /**
     * Constructs new instance with needed dependencies.
     */
    public NotificationService(NotificationRepository notificationRepository,
                               NotificationMapper notificationMapper,
                               WebSocketService webSocketService,
                               PhegyUserService userService, ModelValidatorService modelValidatorService) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.webSocketService = webSocketService;
        this.userService = userService;
        this.modelValidatorService = modelValidatorService;
    }

    /**
     * Saves notification to the database and sends it to a specific user.
     * @param notification notification to be sent.
     * @param user user to receive the notification.
     */
    @Transactional
    public void pushNotificationTo(Notification notification, PhegyUser user) {
        // link the notification to the specified user
        notification.setUser(user);

        // validate notification and save it to the database
        modelValidatorService.validate(notification);
        final Notification savedNotification = this.notificationRepository.save(notification);

        // map notification to dto
        final NotificationResponseDto notificationDto = notificationMapper
                .notificationToNotificationResponseDto(savedNotification);

        // send notification to live user connections
        webSocketService.sendTo(user.getUsername(), "/queue/notification", notificationDto);
    }

    /**
     * Retrieves all notifications related to a specific user from the database.
     * @param username username of the user to search notifications for.
     * @return collection of all notifications related to the specified user
     */
    public Collection<Notification> getAllNotifications(String username) {
        return this.notificationRepository.findAllByUserUsername(username);
    }

    /**
     * Deletes all notifications associated with the specified user.
     * @param ids ids of the notifications to be deleted.
     * @param username username of the user to delete notifications for.
     */
    @Transactional
    public void deleteAll(Collection<Long> ids, String username) {
        // retrieve user from database
        PhegyUser user = this.userService.getUserByUsername(username);

        // get only notifications related to the specified user
        Collection<Notification> notificationsToDelete = user.getNotifications()
                .stream()
                .filter(notification -> ids.contains(notification.getId()))
                .collect(Collectors.toList());

        // delete notifications from the database
        this.notificationRepository.deleteAll(notificationsToDelete);
    }
}
