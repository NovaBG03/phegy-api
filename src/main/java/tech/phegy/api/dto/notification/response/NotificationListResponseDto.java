package tech.phegy.api.dto.notification.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

@Getter
@Setter
public class NotificationListResponseDto {
    private Collection<NotificationResponseDto> notifications;

    public NotificationListResponseDto(Collection<NotificationResponseDto> notifications) {
        this.notifications = notifications;
    }
}
