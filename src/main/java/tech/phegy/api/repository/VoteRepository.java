package tech.phegy.api.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.phegy.api.model.points.Vote;

@Repository
public interface VoteRepository extends CrudRepository<Vote, Long> {

    @Query("select sum(d.points) " +
            "from Vote d join PhegyUser u on d.image.publisher.id = u.id " +
            "where u.username = :username " +
            "group by u.username")
    Double getVotePointsReceivedBy(@Param("username") String receiverUsername);

    @Query("select sum(d.points) " +
            "from Vote d join PhegyUser u on d.voter.id = u.id " +
            "where u.username = :username " +
            "group by u.username")
    Double getVotePointsSentBy(@Param("username") String senderUsername);
}
