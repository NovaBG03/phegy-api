package tech.phegy.api.service;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tech.phegy.api.dto.user.response.AchievementResponseDto;
import tech.phegy.api.dto.user.response.AchievementsListResponseDto;
import tech.phegy.api.service.points.VoteService;

import java.util.List;

/**
 * Service for user achievements management.
 *
 * @author Nikita
 */
@Service
public class AchievementsService {
    private final ImageService imageService;
    private final VoteService voteService;

    /**
     * Constructs new instance with needed dependencies.
     */
    public AchievementsService(ImageService imageService, VoteService voteService) {
        this.imageService = imageService;
        this.voteService = voteService;
    }

    /**
     * Get achievements for a specific user.
     *
     * @param username user to search for.
     * @return object containing all achievements of a specific user.
     * @throws UsernameNotFoundException when the user does not exist
     */
    public AchievementsListResponseDto getAchievements(String username) throws UsernameNotFoundException {
        return AchievementsListResponseDto.builder()
                .username(username)
                .achievements(List.of(
                        AchievementResponseDto.builder()
                                .name("Качени снимки")
                                .value(imageService.getImagesCount(username).toString())
                                .build(),
                        AchievementResponseDto.builder()
                                .name("Получени точки")
                                .value(voteService.getPointsReceivedBy(username).toString())
                                .build(),
                        AchievementResponseDto.builder()
                                .name("Изпратени точки")
                                .value(voteService.getPointsSentBy(username).toString())
                                .build()
                )).build();
    }
}
