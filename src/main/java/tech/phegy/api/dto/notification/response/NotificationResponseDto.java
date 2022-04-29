package tech.phegy.api.dto.notification.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDto {
    private long id;
    private String title;
    private String message;
    private String category;
}
