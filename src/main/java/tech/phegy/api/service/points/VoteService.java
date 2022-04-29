package tech.phegy.api.service.points;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.phegy.api.exception.PhegyHttpException;
import tech.phegy.api.model.Image;
import tech.phegy.api.model.points.PointsBag;
import tech.phegy.api.repository.VoteRepository;
import tech.phegy.api.model.points.Vote;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.service.ImageService;
import tech.phegy.api.service.PhegyUserService;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Service for managing votes
 *
 * @author Nikita
 */
@Service
public class VoteService {
    private final VoteRepository voteRepository;
    private final PointsBagService pointsBagService;
    private final PhegyUserService userService;
    private final ImageService imageService;
    private final VoteProps voteProps;

    /**
     * Constructs new instance with needed dependencies.
     */
    public VoteService(VoteRepository voteRepository,
                       PointsBagService pointsBagService,
                       PhegyUserService userService,
                       ImageService imageService,
                       VoteProps voteProps) {
        this.voteRepository = voteRepository;
        this.pointsBagService = pointsBagService;
        this.userService = userService;
        this.imageService = imageService;
        this.voteProps = voteProps;
    }

    @Transactional
    public void vote(Long imageId, Double points, String voterUsername) {
        final PhegyUser voter = this.userService.getConfirmedUser(voterUsername);
        final Image receiverImage = this.imageService.getImage(imageId, voterUsername);
        final PointsBag pointsBag = this.pointsBagService.getPointsBag(voterUsername);

        if (pointsBag.getPoints() < points) {
            throw new PhegyHttpException("NOT_ENOUGH_POINTS_TO_VOTE", HttpStatus.BAD_REQUEST);
        }
        if (voter.getVotes().stream().anyMatch(x -> x.getImage().equals(receiverImage))) {
            throw new PhegyHttpException("ALREADY_VOTED", HttpStatus.BAD_REQUEST);
        }
        if (points < this.voteProps.getMinPoints()) {
            throw new PhegyHttpException("VOTE_POINTS_TOO_LOW", HttpStatus.BAD_REQUEST);
        }
        if (points > this.voteProps.getMaxPoints()) {
            throw new PhegyHttpException("VOTE_POINTS_TOO_HIGH", HttpStatus.BAD_REQUEST);
        }
        if (voterUsername.equals(receiverImage.getPublisher().getUsername())) {
            throw new PhegyHttpException("CAN_NOT_VOTE_FOR_OWNING_IMAGES", HttpStatus.BAD_REQUEST);
        }

        this.voteRepository.save(Vote.builder()
                .voter(voter)
                .image(receiverImage)
                .points(points)
                .submittedAt(LocalDateTime.now())
                .build());

        this.pointsBagService.transferPoints(
                voterUsername,
                receiverImage.getPublisher().getUsername(),
                points);
    }

    /**
     * Get votes received by a specific user.
     *
     * @param username receiver.
     * @return points received.
     */
    public Double getPointsReceivedBy(String username) {
        final Double votesReceived = voteRepository.getVotePointsReceivedBy(username);
        return Objects.requireNonNullElse(votesReceived, 0d);
    }

    /**
     * Get votes sent by a specific user.
     *
     * @param username sender.
     * @return points sent.
     */
    public Double getPointsSentBy(String username) {
        final Double votesSent = voteRepository.getVotePointsSentBy(username);
        return Objects.requireNonNullElse(votesSent, 0d);
    }
}
