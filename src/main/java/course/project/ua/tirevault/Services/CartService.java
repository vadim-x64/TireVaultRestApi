package course.project.ua.tirevault.Services;

import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Entities.Models.Cart;
import course.project.ua.tirevault.Entities.Models.CartItem;
import course.project.ua.tirevault.Entities.Models.Product;
import course.project.ua.tirevault.Repositories.ICartItemRepository;
import course.project.ua.tirevault.Repositories.ICartRepository;
import course.project.ua.tirevault.Repositories.IProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Optional;

@Service
public class CartService {
    @Autowired
    private ICartRepository cartRepository;

    @Autowired
    private ICartItemRepository cartItemRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private OrderService orderService;

    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(user);
            cart.setTotal(BigDecimal.ZERO);
            return cartRepository.save(cart);
        });
    }

    @Transactional
    public Cart addToCart(User user, Long productId, int quantity) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Товар не знайдено"));

        if (product.getQuantity() < quantity)
            throw new RuntimeException(
                    product.getQuantity() <= 0
                            ? "Товар закінчився на складі"
                            : "На складі доступно лише " + product.getQuantity() + " шт."
            );

        Cart cart = getOrCreateCart(user);
        Optional<CartItem> existing = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(quantity);
            cartItemRepository.save(item);
            cart.getItems().add(item);
        }

        product.setQuantity(product.getQuantity() - quantity);
        product.setAvailability(product.getQuantity() > 0);
        productRepository.save(product);
        return recalculate(cart);
    }

    @Transactional
    public Cart updateQuantity(User user, Long itemId, int quantity) {
        Cart cart = getOrCreateCart(user);
        CartItem item = cartItemRepository.findById(itemId).orElseThrow(() -> new RuntimeException("Позицію не знайдено"));
        if (!item.getCart().getId().equals(cart.getId())) throw new RuntimeException("Доступ заборонено");
        Product product = item.getProduct();
        int delta = quantity - item.getQuantity();

        if (quantity <= 0) {
            product.setQuantity(product.getQuantity() + item.getQuantity());
            product.setAvailability(true);
            productRepository.save(product);
            cartItemRepository.delete(item);
            cart.getItems().remove(item);
        } else {
            if (delta > 0 && product.getQuantity() < delta) throw new RuntimeException("На складі лише " + product.getQuantity() + " шт.");
            product.setQuantity(product.getQuantity() - delta);
            product.setAvailability(product.getQuantity() > 0);
            productRepository.save(product);
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return recalculate(cart);
    }

    @Transactional
    public Cart removeItem(User user, Long itemId) {
        Cart cart = getOrCreateCart(user);
        CartItem item = cartItemRepository.findById(itemId).orElseThrow(() -> new RuntimeException("Позицію не знайдено"));
        if (!item.getCart().getId().equals(cart.getId())) throw new RuntimeException("Доступ заборонено");
        Product product = item.getProduct();
        product.setQuantity(product.getQuantity() + item.getQuantity());
        product.setAvailability(true);
        productRepository.save(product);
        cartItemRepository.delete(item);
        cart.getItems().remove(item);
        return recalculate(cart);
    }

    @Transactional
    public void checkout(User user, String station, String payMethod) {
        Cart cart = getOrCreateCart(user);
        if (cart.getItems().isEmpty()) throw new RuntimeException("Кошик порожній");
        orderService.createFromCart(user, cart, station, payMethod);
        cart.getItems().clear();
        cart.setTotal(BigDecimal.ZERO);
        cartRepository.save(cart);
    }

    public int getCartItemCount(User user) {
        return cartRepository.findByUserId(user.getId()).map(cart -> cart.getItems().stream().mapToInt(CartItem::getQuantity).sum()).orElse(0);
    }

    private Cart recalculate(Cart cart) {
        BigDecimal total = cart.getItems().stream().map(CartItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotal(total);
        return cartRepository.save(cart);
    }
}