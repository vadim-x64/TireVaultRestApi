package course.project.ua.tirevault.integration;

import course.project.ua.tirevault.Entities.Enums.ReviewTargetType;
import course.project.ua.tirevault.Entities.Models.Review;
import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Repositories.IUserRepository;
import course.project.ua.tirevault.Services.AuthService;
import course.project.ua.tirevault.Services.ReviewService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReviewServiceIntegrationTest {
    @Autowired
    private ReviewService reviewService;

    @Autowired
    private AuthService authService;

    @Autowired
    private IUserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        authService.register("Іван", "Тест", null,
                "+380501111111", "reviewuser", "review@gmail.com", "password123");
        testUser = userRepository.findByUsername("reviewuser").orElseThrow();
    }

    @Test
    void addReview_shouldPersistReview() {
        Review review = reviewService.addReview(
                testUser, ReviewTargetType.PRODUCT, 1L, "Чудовий товар!", null, null, null);
        assertNotNull(review.getId());
        assertEquals("Чудовий товар!", review.getContent());
    }

    @Test
    void addReview_shouldPersistServiceReview() {
        Review review = reviewService.addReview(
                testUser, ReviewTargetType.SERVICE, 1L, "Якісне обслуговування", null, null, null);
        assertNotNull(review.getId());
        assertEquals(ReviewTargetType.SERVICE, review.getTargetType());
    }

    @Test
    void getTopLevelReviews_shouldReturnReviews() {
        reviewService.addReview(testUser, ReviewTargetType.PRODUCT, 1L, "Відмінно", null, null, null);
        List<Review> reviews = reviewService.getTopLevelReviews(ReviewTargetType.PRODUCT, 1L);
        assertFalse(reviews.isEmpty());
    }

    @Test
    void getAllReviews_shouldReturnAll() {
        reviewService.addReview(testUser, ReviewTargetType.SERVICE, 1L, "Гарний сервіс", null, null, null);
        assertFalse(reviewService.getAllReviews().isEmpty());
    }

    @Test
    void deleteReview_shouldRemove() {
        Review review = reviewService.addReview(
                testUser, ReviewTargetType.PRODUCT, 1L, "Видалити мене", null, null, null);
        Long id = review.getId();
        reviewService.deleteReview(id);
        assertTrue(reviewService.getReviewById(id).isEmpty());
    }

    @Test
    void getTopLevelReviews_shouldReturnEmpty_whenNone() {
        List<Review> reviews = reviewService.getTopLevelReviews(ReviewTargetType.PRODUCT, 999999L);
        assertTrue(reviews.isEmpty());
    }
}