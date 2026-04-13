package course.project.ua.tirevault.Repositories;

import course.project.ua.tirevault.Entities.Enums.ReviewTargetType;
import course.project.ua.tirevault.Entities.Models.Review;
import course.project.ua.tirevault.Entities.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByTargetTypeAndTargetIdAndParentIsNullOrderByCreatedAtDesc(ReviewTargetType targetType, Long targetId);
    List<Review> findByUserOrderByCreatedAtDesc(User user);
    List<Review> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE Review r SET r.replyToReview = null WHERE r.replyToReview.id = :reviewId OR r.replyToReview.id IN (SELECT rep.id FROM Review rep WHERE rep.parent.id = :reviewId)")
    void clearReplyToReviewReferences(@Param("reviewId") Long reviewId);
}