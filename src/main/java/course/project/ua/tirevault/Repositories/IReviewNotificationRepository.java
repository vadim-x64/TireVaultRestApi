package course.project.ua.tirevault.Repositories;

import course.project.ua.tirevault.Entities.Models.Review;
import course.project.ua.tirevault.Entities.Models.ReviewNotification;
import course.project.ua.tirevault.Entities.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IReviewNotificationRepository extends JpaRepository<ReviewNotification, Long> {
    List<ReviewNotification> findByUserOrderByCreatedAtDesc(User user);
    long countByUserAndSeenFalse(User user);
    List<ReviewNotification> findByUserAndSeenFalse(User user);

    @Modifying
    @Query("DELETE FROM ReviewNotification n WHERE n.review = :review")
    void deleteByReview(@Param("review") Review review);
}