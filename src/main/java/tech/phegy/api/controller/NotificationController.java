package tech.phegy.api.controller;

import org.springframework.web.bind.annotation.*;
import tech.phegy.api.service.NotificationService;
import tech.phegy.api.dto.notification.request.NotificationIdListDto;
import tech.phegy.api.dto.notification.response.NotificationListResponseDto;
import tech.phegy.api.mapper.notification.NotificationMapper;

import java.security.Principal;
import java.util.stream.Collectors;

/**
 * Notification controller.
 *
 * @author Nikita
 */
@RestController
@RequestMapping("/api/v1/notification")
public class NotificationController {
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    public NotificationController(NotificationService notificationService, NotificationMapper notificationMapper) {
        this.notificationService = notificationService;
        this.notificationMapper = notificationMapper;
    }

    @GetMapping
    public NotificationListResponseDto getAll(Principal principal) {
        return new NotificationListResponseDto(this.notificationService
                .getAllNotifications(principal.getName())
                .stream()
                .map(notificationMapper::notificationToNotificationResponseDto)
                .collect(Collectors.toList()));
    }

    @DeleteMapping
    public void closeNotifications(@RequestBody NotificationIdListDto idList, Principal principal) {
        this.notificationService.deleteAll(idList.getNotificationIds(), principal.getName());
    }
}
