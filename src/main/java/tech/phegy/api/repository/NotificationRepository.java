package tech.phegy.api.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tech.phegy.api.model.notification.Notification;

import java.util.Collection;

@Repository
public interface NotificationRepository extends CrudRepository<Notification, Long> {
    Collection<Notification> findAllByUserUsername(String username);
}
