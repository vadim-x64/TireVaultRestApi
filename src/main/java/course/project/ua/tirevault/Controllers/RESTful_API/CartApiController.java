package course.project.ua.tirevault.Controllers.RESTful_API;

import course.project.ua.tirevault.Configuration.ApiRole;
import course.project.ua.tirevault.Entities.Enums.UserRole;
import course.project.ua.tirevault.Entities.Models.Cart;
import course.project.ua.tirevault.Entities.Models.CartItem;
import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Services.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Кошик", description = "Операції з кошиком та оформлення замовлення")
public class CartApiController {
    @Autowired
    private CartService cartService;

    @ApiRole(UserRole.USER)
    @GetMapping
    @Operation(summary = "Отримати вміст кошика")
    public ResponseEntity<?> getCart(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));
        Cart cart = cartService.getOrCreateCart(user);
        return ResponseEntity.ok(cart);
    }

    @ApiRole(UserRole.USER)
    @PostMapping("/add")
    @Operation(summary = "Додати товар у кошик")
    public ResponseEntity<?> addToCart(@RequestParam Long productId, @RequestParam(defaultValue = "1") int quantity, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));

        try {
            Cart cart = cartService.addToCart(user, productId, quantity);
            int count = cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();
            return ResponseEntity.ok(Map.of("success", true, "cartCount", count, "total", cart.getTotal()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @ApiRole(UserRole.USER)
    @PatchMapping("/update")
    @Operation(summary = "Змінити кількість товару в кошику")
    public ResponseEntity<?> updateItem(@RequestParam Long itemId,
                                        @RequestParam int quantity,
                                        HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));

        try {
            Cart cart = cartService.updateQuantity(user, itemId, quantity);
            CartItem updated = cart.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst().orElse(null);
            int count = cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "subtotal", updated != null ? updated.getSubtotal() : 0,
                    "total", cart.getTotal(),
                    "cartCount", count
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @ApiRole(UserRole.USER)
    @DeleteMapping("/remove")
    @Operation(summary = "Видалити товар з кошика")
    public ResponseEntity<?> removeItem(@RequestParam Long itemId,
                                        HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));

        Cart cart = cartService.removeItem(user, itemId);
        int count = cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "total", cart.getTotal(),
                "cartCount", count,
                "empty", cart.getItems().isEmpty()
        ));
    }

    @ApiRole(UserRole.USER)
    @PostMapping("/checkout")
    @Operation(summary = "Оформити замовлення з кошика")
    public ResponseEntity<?> checkout(@RequestParam(required = false) String station,
                                      @RequestParam(required = false) String payMethod,
                                      HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));

        try {
            cartService.checkout(user, station, payMethod);
            return ResponseEntity.ok(Map.of("success", true, "message", "Замовлення успішно оформлено"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}