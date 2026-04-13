package course.project.ua.tirevault.services;

import course.project.ua.tirevault.Entities.Models.*;
import course.project.ua.tirevault.Repositories.ICartItemRepository;
import course.project.ua.tirevault.Repositories.ICartRepository;
import course.project.ua.tirevault.Repositories.IProductRepository;
import course.project.ua.tirevault.Services.CartService;
import course.project.ua.tirevault.Services.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    @Mock
    private ICartRepository cartRepository;

    @Mock
    private ICartItemRepository cartItemRepository;

    @Mock
    private IProductRepository productRepository;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private CartService cartService;

    @Test
    void addToCart_shouldThrow_whenProductOutOfStock() {
        User user = new User();
        user.setId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setQuantity(0);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                cartService.addToCart(user, 10L, 1)
        );

        assertEquals("Товар закінчився на складі", ex.getMessage());
    }

    @Test
    void addToCart_shouldThrow_whenNotEnoughStock() {
        User user = new User();
        user.setId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setQuantity(2);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                cartService.addToCart(user, 10L, 5)
        );

        assertTrue(ex.getMessage().contains("На складі доступно лише 2 шт."));
    }

    @Test
    void addToCart_shouldThrow_whenProductNotFound() {
        User user = new User();
        user.setId(1L);

        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                cartService.addToCart(user, 99L, 1)
        );
    }

    @Test
    void checkout_shouldThrow_whenCartIsEmpty() {
        User user = new User();
        user.setId(1L);

        Cart emptyCart = new Cart();
        emptyCart.setItems(new ArrayList<>());
        emptyCart.setTotal(BigDecimal.ZERO);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(emptyCart));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                cartService.checkout(user, "м. Київ, вул. Київська 112", "CARD")
        );

        assertEquals("Кошик порожній", ex.getMessage());
    }

    @Test
    void getOrCreateCart_shouldCreateCart_whenNotExists() {
        User user = new User();
        user.setId(1L);

        Cart newCart = new Cart();
        newCart.setTotal(BigDecimal.ZERO);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        Cart result = cartService.getOrCreateCart(user);

        assertNotNull(result);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }
}