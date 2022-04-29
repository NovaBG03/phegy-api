package tech.phegy.api.model.notification;

import lombok.*;
import org.hibernate.validator.constraints.Length;
import tech.phegy.api.model.user.PhegyUser;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @NotNull(message = "NOTIFICATION_TITLE_NULL")
    @Length.List({
            @Length(min = 3, message = "NOTIFICATION_TITLE_TOO_SHORT"),
            @Length(max= 30, message = "NOTIFICATION_TITLE_TOO_LONG")
    })
    private String title;

    @NotNull(message = "NOTIFICATION_MESSAGE_NULL")
    @Length.List({
            @Length(min = 3, message = "NOTIFICATION_MESSAGE_TOO_SHORT"),
            @Length(max= 150, message = "NOTIFICATION_MESSAGE_TOO_LONG")
    })
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 25)
    private NotificationCategory category;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private PhegyUser user;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
