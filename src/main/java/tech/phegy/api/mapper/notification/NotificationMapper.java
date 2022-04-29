package tech.phegy.api.mapper.notification;

import tech.phegy.api.dto.notification.response.NotificationResponseDto;
import tech.phegy.api.model.notification.Notification;

public interface NotificationMapper {
    NotificationResponseDto notificationToNotificationResponseDto(Notification notification);
}
