package course.project.ua.tirevault.Controllers.RESTful_API;

import course.project.ua.tirevault.Configuration.ApiRole;
import course.project.ua.tirevault.Entities.Enums.ReviewTargetType;
import course.project.ua.tirevault.Entities.Enums.UserRole;
import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Services.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Відгуки", description = "Відгуки на товари та послуги")
public class ReviewApiController {
    @Autowired
    private ReviewService reviewService;

    @ApiRole(UserRole.USER)
    @GetMapping("/my")
    @Operation(summary = "Мої відгуки")
    public ResponseEntity<?> myReviews(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));
        return ResponseEntity.ok(reviewService.getReviewsByUser(user));
    }

    @ApiRole(UserRole.USER)
    @GetMapping("/notifications")
    @Operation(summary = "Мої сповіщення (відповіді / лайки)")
    public ResponseEntity<?> notifications(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));
        var notifs = reviewService.getNotifications(user);
        reviewService.markAllNotificationsSeen(user);
        return ResponseEntity.ok(notifs);
    }

    @ApiRole(UserRole.USER)
    @PostMapping
    @Operation(summary = "Написати відгук (targetType: PRODUCT або SERVICE)")
    public ResponseEntity<?> addReview(@RequestParam String targetType,
                                       @RequestParam Long targetId,
                                       @RequestParam String content,
                                       @RequestParam(required = false) Long parentId,
                                       @RequestParam(required = false) Long replyToReviewId,
                                       @RequestParam(required = false) Long replyToUserId,
                                       HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));
        if (content == null || content.trim().isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Текст відгуку не може бути порожнім"));
        try {
            ReviewTargetType type = ReviewTargetType.valueOf(targetType.toUpperCase());
            reviewService.addReview(user, type, targetId, content.trim(), parentId, replyToReviewId, replyToUserId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Допустимі значення targetType: PRODUCT, SERVICE"));
        }
    }

    @ApiRole(UserRole.USER)
    @GetMapping("/product/{targetId}")
    @Operation(summary = "Відгуки на товар")
    public ResponseEntity<?> getProductReviews(@PathVariable Long targetId) {
        return ResponseEntity.ok(reviewService.getTopLevelReviews(ReviewTargetType.PRODUCT, targetId));
    }

    @ApiRole(UserRole.USER)
    @GetMapping("/service/{targetId}")
    @Operation(summary = "Відгуки на послугу")
    public ResponseEntity<?> getServiceReviews(@PathVariable Long targetId) {
        return ResponseEntity.ok(reviewService.getTopLevelReviews(ReviewTargetType.SERVICE, targetId));
    }

    @ApiRole(UserRole.USER)
    @PostMapping("/{id}/like")
    @Operation(summary = "Лайк / зняти лайк з відгуку")
    public ResponseEntity<?> toggleLike(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));
        return ResponseEntity.ok(reviewService.toggleLike(id, user));
    }

    @ApiRole(UserRole.USER)
    @DeleteMapping("/{id}/my")
    @Operation(summary = "Видалити свій відгук")
    public ResponseEntity<?> deleteMy(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));
        return reviewService.getReviewById(id).map(review -> {
            if (!review.getUser().getId().equals(user.getId()))
                return ResponseEntity.status(403).body(Map.of("error", "Це не ваш відгук"));
            reviewService.deleteReview(id);
            return ResponseEntity.ok(Map.<String, Object>of("success", true));
        }).orElse(ResponseEntity.notFound().build());
    }

    @ApiRole(UserRole.ADMIN)
    @GetMapping
    @Operation(summary = "Всі відгуки (менеджер/адмін)")
    public ResponseEntity<?> getAll(HttpSession session) {
        if (!isManagerOrAdmin(session)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @ApiRole(UserRole.ADMIN)
    @DeleteMapping("/{id}")
    @Operation(summary = "Видалити будь-який відгук (менеджер/адмін)")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        if (!isManagerOrAdmin(session)) return ResponseEntity.status(403).build();
        reviewService.deleteReview(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private boolean isManagerOrAdmin(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        return user != null && (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.MANAGER);
    }
}