package course.project.ua.tirevault.integration;

import course.project.ua.tirevault.Entities.Enums.OrderStatus;
import course.project.ua.tirevault.Entities.Models.*;
import course.project.ua.tirevault.Entities.Models.Order;
import course.project.ua.tirevault.Repositories.*;
import course.project.ua.tirevault.Services.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceIntegrationTest {
    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private AuthService authService;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IProductCategoryRepository productCategoryRepository;

    @Autowired
    private IOrderRepository orderRepository;

    private User testUser;

    private Product testProduct;

    @BeforeEach
    void setUp() throws Exception {
        authService.register("Іван", "Тест", null,
                "+380501111111", "orderuser", "order@gmail.com", "password123");
        testUser = userRepository.findByUsername("orderuser").orElseThrow();

        ProductCategory category = new ProductCategory();
        category.setName("Тест категорія");
        category = productCategoryRepository.save(category);

        testProduct = new Product();
        testProduct.setName("Тест товар");
        testProduct.setArticle("ORD-001");
        testProduct.setAvailability(true);
        testProduct.setQuantity(10);
        testProduct.setPrice(new BigDecimal("500.00"));
        testProduct.setCategory(category);
        testProduct = productRepository.save(testProduct);
    }

    private Order createOrder() {
        cartService.addToCart(testUser, testProduct.getId(), 1);
        cartService.checkout(testUser, "Київ", "CARD");
        return orderService.getActiveByUser(testUser).get(0);
    }

    @Test
    void getActiveByUser_shouldReturnOrders() {
        createOrder();
        assertFalse(orderService.getActiveByUser(testUser).isEmpty());
    }

    @Test
    void getCompletedByUser_shouldReturnEmpty_whenNone() {
        assertTrue(orderService.getCompletedByUser(testUser).isEmpty());
    }

    @Test
    void getAllActive_shouldReturnOrders() {
        createOrder();
        assertFalse(orderService.getAllActive().isEmpty());
    }

    @Test
    void setStatus_shouldUpdateStatus() {
        Order order = createOrder();
        orderService.setStatus(order.getId(), OrderStatus.PROCESSING);
        assertEquals(OrderStatus.PROCESSING,
                orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    void cancel_shouldCancelOrder() {
        Order order = createOrder();
        orderService.cancel(order.getId(), testUser);
        assertEquals(OrderStatus.CANCELLED,
                orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    void cancel_shouldThrow_whenAlreadyCompleted() {
        Order order = createOrder();
        orderService.setStatus(order.getId(), OrderStatus.COMPLETED);
        assertThrows(RuntimeException.class, () ->
                orderService.cancel(order.getId(), testUser));
    }

    @Test
    void deleteByUser_shouldThrow_whenOrderIsActive() {
        Order order = createOrder();
        assertThrows(RuntimeException.class, () ->
                orderService.deleteByUser(order.getId(), testUser));
    }
}