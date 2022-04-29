package tech.phegy.api.mapper.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tech.phegy.api.dto.notification.response.NotificationResponseDto;
import tech.phegy.api.model.notification.Notification;
import tech.phegy.api.model.notification.NotificationCategory;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMapperImplTest {
    NotificationMapperImpl notificationMapper;

    @BeforeEach
    void setUp() {
        notificationMapper = new NotificationMapperImpl();
    }

    @Test
    @DisplayName("Should map notification to notification response dto")
    void shouldMapNotificationToNotificationResponseDto() {
        long id = 1L;
        String title = "title";
        String message = "message";
        NotificationCategory category = NotificationCategory.INFO;
        Notification notification = Notification.builder()
                .id(id)
                .title(title)
                .message(message)
                .category(category)
                .build();

        NotificationResponseDto actual = notificationMapper.notificationToNotificationResponseDto(notification);
        assertThat(actual)
                .matches(x -> x.getId() == id, "id is set")
                .matches(x-> x.getTitle().equals(title), "title is set")
                .matches(x -> x.getMessage().equals(message), "message is set")
                .matches(x -> x.getCategory().equals(category.toString().toLowerCase()), "category is set");
    }

    @Test
    @DisplayName("Should map notification to notification response dto when null")
    void shouldMapNotificationToNotificationResponseDtoWhenNull() {
        NotificationResponseDto actual = notificationMapper.notificationToNotificationResponseDto(null);
        assertThat(actual).isNull();
    }
}