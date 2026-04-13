package course.project.ua.tirevault.Controllers.RESTful_API;

import course.project.ua.tirevault.Configuration.ApiRole;
import course.project.ua.tirevault.Entities.Enums.UserRole;
import course.project.ua.tirevault.Entities.Models.*;
import course.project.ua.tirevault.Repositories.ICartItemRepository;
import course.project.ua.tirevault.Repositories.ICartRepository;
import course.project.ua.tirevault.Repositories.IOrderItemRepository;
import course.project.ua.tirevault.Services.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Автотовари", description = "Каталог товарів та категорій")
public class ProductApiController {
    @Autowired private ProductService productService;
    @Autowired private ICartItemRepository cartItemRepository;
    @Autowired private ICartRepository cartRepository;
    @Autowired private IOrderItemRepository orderItemRepository;

    @ApiRole(UserRole.USER)
    @GetMapping
    @Operation(summary = "Отримати всі товари (з фільтрами)")
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean availability,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String vmodel,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String modification) {
        return ResponseEntity.ok(productService.getFilteredProducts(
                categoryId, minPrice, maxPrice, availability,
                brand, vmodel, year, modification));
    }

    @ApiRole(UserRole.USER)
    @GetMapping("/{id}")
    @Operation(summary = "Отримати товар за ID")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @ApiRole(UserRole.USER)
    @GetMapping("/categories")
    @Operation(summary = "Отримати всі категорії товарів")
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }

    @ApiRole(UserRole.ADMIN)
    @PostMapping("/categories")
    @Operation(summary = "Додати категорію товарів")
    public ResponseEntity<?> addCategory(@RequestParam String name, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).build();
        ProductCategory cat = new ProductCategory();
        cat.setName(name);
        productService.saveCategory(cat);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @ApiRole(UserRole.ADMIN)
    @PutMapping("/categories/{id}")
    @Operation(summary = "Редагувати категорію товарів")
    public ResponseEntity<?> editCategory(@PathVariable Long id, @RequestParam String name, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).build();
        return productService.getCategoryById(id).map(cat -> {
            cat.setName(name);
            productService.saveCategory(cat);
            return ResponseEntity.ok(Map.of("success", true));
        }).orElse(ResponseEntity.notFound().build());
    }

    @ApiRole(UserRole.ADMIN)
    @DeleteMapping("/categories/{id}")
    @Transactional
    @Operation(summary = "Видалити категорію (разом з усіма товарами)")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).build();
        List<Product> products = productService.getProductsByCategory(id);
        products.forEach(p -> {
            removeCartItemsForProduct(p.getId());
            productService.deleteProductById(p.getId());
        });
        productService.deleteCategoryById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @ApiRole(UserRole.ADMIN)
    @PostMapping
    @Operation(summary = "Додати товар")
    public ResponseEntity<?> addProduct(
            @RequestParam Long categoryId,
            @RequestParam String name,
            @RequestParam String article,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam(defaultValue = "0") Integer quantity,
            @RequestParam(required = false) String imageUrl,
            HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).build();
        return productService.getCategoryById(categoryId).map(cat -> {
            Product p = new Product();
            p.setCategory(cat);
            p.setName(name);
            p.setArticle(article);
            p.setDescription(description);
            p.setPrice(price);
            p.setQuantity(quantity);
            p.setAvailability(quantity > 0);
            p.setImageUrl(imageUrl);
            productService.saveProduct(p);
            return ResponseEntity.ok(Map.of("success", true));
        }).orElse(ResponseEntity.badRequest().build());
    }

    @ApiRole(UserRole.ADMIN)
    @PutMapping("/{id}")
    @Operation(summary = "Редагувати товар")
    public ResponseEntity<?> editProduct(
            @PathVariable Long id,
            @RequestParam Long categoryId,
            @RequestParam String name,
            @RequestParam(required = false) String article,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam(defaultValue = "0") Integer quantity,
            @RequestParam(required = false) String imageUrl,
            HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).build();
        return productService.getProductById(id).map(p -> {
            productService.getCategoryById(categoryId).ifPresent(p::setCategory);
            p.setName(name);
            if (article != null && !article.isBlank()) p.setArticle(article);
            p.setDescription(description);
            p.setPrice(price);
            p.setQuantity(quantity);
            p.setAvailability(quantity > 0);
            p.setImageUrl(imageUrl);
            productService.saveProduct(p);
            return ResponseEntity.ok(Map.of("success", true));
        }).orElse(ResponseEntity.notFound().build());
    }

    @ApiRole(UserRole.ADMIN)
    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Видалити товар")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).build();
        removeCartItemsForProduct(id);
        productService.deleteProductById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private void removeCartItemsForProduct(Long productId) {
        orderItemRepository.deleteByProductId(productId);
        cartItemRepository.findByProductId(productId).forEach(item -> {
            Cart cart = item.getCart();
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
            BigDecimal total = cart.getItems().stream()
                    .map(CartItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            cart.setTotal(total);
            cartRepository.save(cart);
        });
    }

    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        return user != null && user.getRole() == UserRole.ADMIN;
    }
}