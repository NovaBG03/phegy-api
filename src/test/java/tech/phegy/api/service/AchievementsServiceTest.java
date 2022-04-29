package tech.phegy.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import tech.phegy.api.dto.user.response.AchievementsListResponseDto;
import tech.phegy.api.service.points.VoteService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchievementsServiceTest {
    @Mock
    ImageService imageService;
    @Mock
    VoteService voteService;

    AchievementsService achievementsService;

    @BeforeEach
    void initAchievementsService() {
        achievementsService = new AchievementsService(imageService, voteService);
    }

    @Test
    @DisplayName("Should get all achievements correctly")
    void shouldGetAllAchievementsCorrectly() {
        String username = "ivan";
        Long imagesCount = 3L;
        Double pointsReceived = 23d;
        Double pointsSent = 11d;

        when(imageService.getImagesCount(username)).thenReturn(imagesCount);
        when(voteService.getPointsReceivedBy(username)).thenReturn(pointsReceived);
        when(voteService.getPointsSentBy(username)).thenReturn(pointsSent);

        AchievementsListResponseDto achievementList = achievementsService.getAchievements(username);

        assertThat(achievementList).isNotNull();
        assertThat(achievementList.getUsername()).isEqualTo(username);
        assertThat(achievementList.getAchievements())
                .anyMatch(x -> x.getName().equals("Качени снимки") && x.getValue().equals(imagesCount.toString()))
                .anyMatch(x -> x.getName().equals("Получени точки") && x.getValue().equals(pointsReceived.toString()))
                .anyMatch(x -> x.getName().equals("Изпратени точки") && x.getValue().equals(pointsSent.toString()));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        String username = "notFoundUser";
        when(imageService.getImagesCount(username)).thenThrow(UsernameNotFoundException.class);
        assertThatThrownBy(() -> achievementsService.getAchievements(username))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
