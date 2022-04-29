package tech.phegy.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.phegy.api.model.Image;

import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    Optional<Image> findByIdAndApprovedOnNotNull(Long id);

    long countByPublisherUsernameAndApprovedOnNotNull(String publisherUsername);

    Page<Image> findAllByApprovedOnNull(Pageable pageable);

    Page<Image> findAllByApprovedOnNotNull(Pageable pageable);

    Page<Image> findAllByPublisherUsername(String publisherUsername, Pageable pageable);

    Page<Image> findAllByPublisherUsernameAndApprovedOnNull(String publisherUsername, Pageable pageable);

    Page<Image> findAllByPublisherUsernameAndApprovedOnNotNull(String publisherUsername, Pageable pageable);

    @Query(nativeQuery = true,
            value = "select * from image i " +
                    "left join vote d on i.id = d.image_id " +
                    "group by i.id " +
                    "order by max(d.submitted_at) desc, approved_on desc")
    Page<Image> findAllByApprovedOnNotNullOrderByLatestTipped(Pageable pageable);

    @Query(nativeQuery = true,
            value = "select * from image m " +
                    "left join vote d on m.id = d.image_id " +
                    "join user u on m.publisher_id = u.id " +
                    "where u.username = :publisherUsername " +
                    "group by m.id " +
                    "order by max(d.submitted_at) desc, approved_on desc")
    Page<Image> findAllByPublisherUsernameApprovedOnNotNullOrderByLatestTipped(@Param("publisherUsername") String publisherUsername, Pageable pageable);

    @Query(nativeQuery = true,
            value = "select * from image m " +
                    "left join vote d on m.id = d.image_id " +
                    "group by m.id " +
                    "order by sum(d.points) desc, approved_on desc")
    Page<Image> findAllByApprovedOnNotNullOrderByMostTipped(Pageable pageable);

    @Query(nativeQuery = true,
            value = "select * from image m " +
                    "left join vote d on m.id = d.image_id " +
                    "join user u on m.publisher_id = u.id " +
                    "where u.username = :publisherUsername " +
                    "group by m.id " +
                    "order by sum(d.points) desc, approved_on desc")
    Page<Image> findAllByPublisherUsernameApprovedOnNotNullOrderByMostTipped(@Param("publisherUsername") String publisherUsername, Pageable pageable);

    @Query(nativeQuery = true,
            value = "select * from image m " +
                    "left join vote d on m.id = d.image_id " +
                    "where d.submitted_at BETWEEN NOW() - INTERVAL :days DAY AND NOW() " +
                    "group by m.id " +
                    "order by sum(d.points) desc, approved_on desc")
    Page<Image> findAllByApprovedOnNotNullOrderByTopTipped(Pageable pageable, @Param("days") int daysFromNow);
}
