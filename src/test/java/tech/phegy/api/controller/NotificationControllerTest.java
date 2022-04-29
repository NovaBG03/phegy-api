package tech.phegy.api.controller;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tech.phegy.api.dto.notification.request.NotificationIdListDto;
import tech.phegy.api.mapper.notification.NotificationMapper;
import tech.phegy.api.model.notification.Notification;
import tech.phegy.api.service.NotificationService;

import java.util.Collection;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.phegy.api.TestUtils.json;

@SpringBootTest
class NotificationControllerTest {
    @MockBean
    NotificationService notificationService;
    @MockBean
    NotificationMapper notificationMapper;

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Should get all notifications")
    void shouldGetAllNotifications() throws Exception {
        String username = "ivan";
        Collection<Notification> notifications = Lists.newArrayList(
                Notification.builder().build(),
                Notification.builder().build(),
                Notification.builder().build()
        );
        int notificationsCount = notifications.size();

        when(notificationService.getAllNotifications(username)).thenReturn(notifications);

        mvc.perform(get("/api/v1/notification").with(user(username)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(notificationMapper, times(notificationsCount)).notificationToNotificationResponseDto(any());
    }

    @Test
    @DisplayName("Should close notifications")
    void shouldCloseNotifications() throws Exception {
        String username = "ivan";
        NotificationIdListDto idList = new NotificationIdListDto(Lists.newArrayList(1L, 2L, 3L));

        mvc.perform(delete("/api/v1/notification")
                        .with(user(username))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(idList)))
                .andExpect(status().isOk());

        verify(notificationService).deleteAll(idList.getNotificationIds(), username);
    }
}
