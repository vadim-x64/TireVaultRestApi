package course.project.ua.tirevault.integration;

import course.project.ua.tirevault.Entities.Models.Cart;
import course.project.ua.tirevault.Entities.Models.Product;
import course.project.ua.tirevault.Entities.Models.ProductCategory;
import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Repositories.IProductCategoryRepository;
import course.project.ua.tirevault.Repositories.IProductRepository;
import course.project.ua.tirevault.Repositories.IUserRepository;
import course.project.ua.tirevault.Services.AuthService;
import course.project.ua.tirevault.Services.CartService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CartServiceIntegrationTest {
    @Autowired
    private CartService cartService;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IProductRepository productRepository;

    private User testUser;

    private Product testProduct;

    @Autowired
    private AuthService authService;

    @Autowired
    private IProductCategoryRepository productCategoryRepository;

    @BeforeEach
    void setUp() throws Exception {
        authService.register("Іван", "Петров", null,
                "+380501111111", "testuser", "test@gmail.com", "password123");
        testUser = userRepository.findByUsername("testuser").orElseThrow();

        ProductCategory category = new ProductCategory();
        category.setName("Технічні рідини");
        category = productCategoryRepository.save(category);

        testProduct = new Product();
        testProduct.setName("Моторна олива 5W-30");
        testProduct.setArticle("ART-001");
        testProduct.setAvailability(true);
        testProduct.setQuantity(10);
        testProduct.setPrice(new BigDecimal("899.00"));
        testProduct.setCategory(category);
        testProduct = productRepository.save(testProduct);
    }

    @Test
    void addToCart_shouldPersistItem() {
        cartService.addToCart(testUser, testProduct.getId(), 2);

        Cart cart = cartService.getOrCreateCart(testUser);
        assertFalse(cart.getItems().isEmpty());
        assertEquals(0, cart.getItems().get(0).getQuantity());
    }

    @Test
    void addToCart_shouldReduceProductQuantityOnCheckout() {
        cartService.addToCart(testUser, testProduct.getId(), 3);
        cartService.checkout(testUser, "м. Київ, вул. Хрещатик 1", "CARD");

        Product updated = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(7, updated.getQuantity());
    }

    @Test
    void addToCart_shouldThrow_whenOutOfStock() {
        testProduct.setQuantity(0);
        productRepository.save(testProduct);

        assertThrows(RuntimeException.class, () ->
                cartService.addToCart(testUser, testProduct.getId(), 1)
        );
    }

    @Test
    void addToCart_shouldThrow_whenQuantityExceedsStock() {
        assertThrows(RuntimeException.class, () ->
                cartService.addToCart(testUser, testProduct.getId(), 999)
        );
    }

    @Test
    void checkout_shouldThrow_whenCartIsEmpty() {
        assertThrows(RuntimeException.class, () ->
                cartService.checkout(testUser, "Київ", "CARD")
        );
    }
}