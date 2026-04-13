package course.project.ua.tirevault.Controllers.RESTful_API;

import course.project.ua.tirevault.Configuration.ApiRole;
import course.project.ua.tirevault.Entities.Enums.OrderStatus;
import course.project.ua.tirevault.Entities.Enums.UserRole;
import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Services.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Замовлення", description = "Управління замовленнями магазину")
public class OrderApiController {
    @Autowired
    private OrderService orderService;

    @ApiRole(UserRole.USER)
    @GetMapping("/my/active")
    @Operation(summary = "Мої активні замовлення")
    public ResponseEntity<?> myActive(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));
        orderService.markAllSeenByUser(user);
        return ResponseEntity.ok(orderService.getActiveByUser(user));
    }

    @ApiRole(UserRole.USER)
    @GetMapping("/my/completed")
    @Operation(summary = "Мої завершені замовлення")
    public ResponseEntity<?> myCompleted(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));
        return ResponseEntity.ok(orderService.getCompletedByUser(user));
    }

    @ApiRole(UserRole.USER)
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Скасувати своє замовлення")
    public ResponseEntity<?> cancelMy(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));
        try {
            orderService.cancel(id, user);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @ApiRole(UserRole.USER)
    @DeleteMapping("/{id}/my")
    @Operation(summary = "Видалити завершене замовлення зі свого списку")
    public ResponseEntity<?> deleteMy(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));
        try {
            orderService.deleteByUser(id, user);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @ApiRole(UserRole.MANAGER)
    @GetMapping("/active")
    @Operation(summary = "Всі активні замовлення (менеджер/адмін)")
    public ResponseEntity<?> getActive(HttpSession session) {
        if (!isManagerOrAdmin(session)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(orderService.getAllActive());
    }

    @ApiRole(UserRole.MANAGER)
    @GetMapping("/completed")
    @Operation(summary = "Всі завершені замовлення (менеджер/адмін)")
    public ResponseEntity<?> getCompleted(HttpSession session) {
        if (!isManagerOrAdmin(session)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(orderService.getAllCompleted());
    }

    @ApiRole(UserRole.MANAGER)
    @PatchMapping("/{id}/status")
    @Operation(summary = "Змінити статус замовлення (PENDING/PROCESSING/COMPLETED/CANCELLED)")
    public ResponseEntity<?> setStatus(@PathVariable Long id, @RequestParam String status, HttpSession session) {
        if (!isManagerOrAdmin(session)) return ResponseEntity.status(403).build();
        try {
            orderService.setStatus(id, OrderStatus.valueOf(status));
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @ApiRole(UserRole.MANAGER)
    @DeleteMapping("/{id}")
    @Operation(summary = "Видалити замовлення (менеджер/адмін)")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        if (!isManagerOrAdmin(session)) return ResponseEntity.status(403).build();
        orderService.delete(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private boolean isManagerOrAdmin(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        return user != null && (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.MANAGER);
    }
}