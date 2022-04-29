package tech.phegy.api.mapper.notification;

import org.springframework.stereotype.Component;
import tech.phegy.api.dto.notification.response.NotificationResponseDto;
import tech.phegy.api.model.notification.Notification;

/**
 * Notification mapper.
 *
 * @author Nikita
 */
@Component
public class NotificationMapperImpl implements NotificationMapper {
    @Override
    public NotificationResponseDto notificationToNotificationResponseDto(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationResponseDto.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .category(notification.getCategory().name().toLowerCase())
                .build();
    }
}
