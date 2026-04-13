package course.project.ua.tirevault.Services;

import course.project.ua.tirevault.Entities.Enums.ReviewTargetType;
import course.project.ua.tirevault.Entities.Models.*;
import course.project.ua.tirevault.Repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReviewService {
    @Autowired
    private IReviewRepository reviewRepository;

    @Autowired
    private IReviewLikeRepository reviewLikeRepository;

    @Autowired
    private IReviewNotificationRepository notificationRepository;

    @Autowired
    private IUserRepository userRepository;

    public Optional<Review> getReviewById(Long id) {
        return reviewRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Review> getTopLevelReviews(ReviewTargetType type, Long targetId) {
        return reviewRepository.findByTargetTypeAndTargetIdAndParentIsNullOrderByCreatedAtDesc(type, targetId);
    }

    @Transactional(readOnly = true)
    public Set<Long> getAllLikedReviewIds(User user, List<Review> topLevelReviews) {
        if (user == null || topLevelReviews.isEmpty()) return Collections.emptySet();
        List<Review> all = new ArrayList<>(topLevelReviews);
        topLevelReviews.forEach(r -> all.addAll(r.getReplies()));
        List<ReviewLike> likes = reviewLikeRepository.findByUserAndReviewIn(user, all);
        return likes.stream().map(l -> l.getReview().getId()).collect(Collectors.toSet());
    }

    @Transactional
    public Review addReview(User user, ReviewTargetType targetType, Long targetId, String content, Long parentId, Long replyToReviewId, Long replyToUserId) {
        Review review = new Review();
        review.setUser(user);
        String raw = content.trim();
        String cleanContent;

        if (replyToUserId != null) {
            Optional<User> replyUser = userRepository.findById(replyToUserId);

            if (replyUser.isPresent()) {
                String mention = "@" + replyUser.get().getUsername();
                cleanContent = raw.startsWith(mention) ? raw.substring(mention.length()).trim() : raw;
            } else {
                cleanContent = raw;
            }
        } else {
            cleanContent = raw;
        }

        review.setContent(cleanContent);
        review.setTargetType(targetType);
        review.setTargetId(targetId);
        review.setCreatedAt(LocalDateTime.now());
        Review directParent = null;

        if (parentId != null) {
            directParent = reviewRepository.findById(parentId).orElse(null);
        }

        if (replyToReviewId != null && directParent == null) {
            directParent = reviewRepository.findById(replyToReviewId).orElse(null);
        }

        if (directParent != null) {
            Review root = directParent.getParent() != null ? directParent.getParent() : directParent;
            review.setParent(root);
        }

        if (replyToReviewId != null) {
            reviewRepository.findById(replyToReviewId).ifPresent(review::setReplyToReview);
        }

        if (replyToUserId != null) {
            userRepository.findById(replyToUserId).ifPresent(review::setReplyToUser);
        }

        Review saved = reviewRepository.save(review);

        if (directParent != null) {
            Review root = directParent.getParent() != null ? directParent.getParent() : directParent;
            Set<Long> notified = new HashSet<>();

            if (!root.getUser().getId().equals(user.getId())) {
                createNotification(root.getUser(), saved);
                notified.add(root.getUser().getId());
            }

            if (directParent.getParent() != null && !directParent.getUser().getId().equals(user.getId()) && !notified.contains(directParent.getUser().getId())) {
                createNotification(directParent.getUser(), saved);
            }
        }

        return saved;
    }

    private void createNotification(User recipient, Review reply) {
        ReviewNotification n = new ReviewNotification();
        n.setUser(recipient);
        n.setReview(reply);
        n.setCreatedAt(LocalDateTime.now());
        n.setSeen(false);
        notificationRepository.save(n);
    }

    @Transactional
    public void deleteReview(Long id) {
        reviewRepository.clearReplyToReviewReferences(id);

        reviewRepository.findById(id).ifPresent(review -> {
            review.getReplies().forEach(notificationRepository::deleteByReview);
            notificationRepository.deleteByReview(review);
            reviewRepository.delete(review);
        });
    }

    @Transactional
    public Map<String, Object> toggleLike(Long reviewId, User user) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new RuntimeException("Review not found: " + reviewId));
        Optional<ReviewLike> existing = reviewLikeRepository.findByUserAndReview(user, review);
        boolean liked;

        if (existing.isPresent()) {
            reviewLikeRepository.delete(existing.get());
            liked = false;
        } else {
            ReviewLike like = new ReviewLike();
            like.setUser(user);
            like.setReview(review);
            reviewLikeRepository.save(like);
            liked = true;

            if (!review.getUser().getId().equals(user.getId())) {
                ReviewNotification n = new ReviewNotification();
                n.setUser(review.getUser());
                n.setReview(review);
                n.setType("LIKE");
                n.setCreatedAt(java.time.LocalDateTime.now());
                n.setSeen(false);
                notificationRepository.save(n);
            }
        }

        int count = reviewLikeRepository.countByReview(review);
        return Map.of("liked", liked, "count", count);
    }

    @Transactional(readOnly = true)
    public List<Review> getReviewsByUser(User user) {
        return reviewRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<ReviewNotification> getNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public long countUnseenNotifications(User user) {
        return notificationRepository.countByUserAndSeenFalse(user);
    }

    @Transactional
    public void markAllNotificationsSeen(User user) {
        List<ReviewNotification> unseen = notificationRepository.findByUserAndSeenFalse(user);
        unseen.forEach(n -> n.setSeen(true));
        notificationRepository.saveAll(unseen);
    }

    @Transactional(readOnly = true)
    public List<Review> getAllReviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }
}