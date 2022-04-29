package tech.phegy.api.dto.notification.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationIdListDto {
    private Collection<Long> notificationIds;
}
