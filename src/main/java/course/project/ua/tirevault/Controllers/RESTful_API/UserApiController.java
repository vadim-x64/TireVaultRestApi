package course.project.ua.tirevault.Controllers.RESTful_API;

import course.project.ua.tirevault.Configuration.ApiRole;
import course.project.ua.tirevault.Entities.Enums.UserRole;
import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Repositories.ICartRepository;
import course.project.ua.tirevault.Repositories.IOrderRepository;
import course.project.ua.tirevault.Repositories.IServiceRequestRepository;
import course.project.ua.tirevault.Repositories.IUserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Користувачі", description = "Управління користувачами (адмін)")
public class UserApiController {
    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private ICartRepository cartRepository;

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private IServiceRequestRepository serviceRequestRepository;

    @ApiRole(UserRole.ADMIN)
    @GetMapping
    @Operation(summary = "Отримати всіх користувачів (крім адмінів)")
    public ResponseEntity<?> getAll(HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(userRepository.findAllByRoleNotOrderByIdAsc(UserRole.ADMIN));
    }

    @ApiRole(UserRole.ADMIN)
    @GetMapping("/{id}")
    @Operation(summary = "Отримати користувача за ID")
    public ResponseEntity<?> getById(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).build();
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @ApiRole(UserRole.ADMIN)
    @PatchMapping("/{id}/role")
    @Operation(summary = "Змінити роль користувача")
    public ResponseEntity<?> changeRole(@PathVariable Long id, @RequestParam String role, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).build();
        return userRepository.findById(id).map(user -> {
            user.setRole(UserRole.valueOf(role));
            userRepository.save(user);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @ApiRole(UserRole.ADMIN)
    @PatchMapping("/{id}/block")
    @Operation(summary = "Заблокувати / розблокувати користувача")
    public ResponseEntity<?> toggleBlock(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).build();
        return userRepository.findById(id).map(user -> {
            user.setBlocked(!user.isBlocked());
            userRepository.save(user);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @ApiRole(UserRole.ADMIN)
    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Видалити користувача разом з його даними")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).build();
        return userRepository.findById(id).map(user -> {
            cartRepository.findByUserId(id).ifPresent(cartRepository::delete);
            orderRepository.findByUserAndStatusInOrderByCreatedAtDesc(user,
                            List.of(
                                    course.project.ua.tirevault.Entities.Enums.OrderStatus.PENDING,
                                    course.project.ua.tirevault.Entities.Enums.OrderStatus.PROCESSING,
                                    course.project.ua.tirevault.Entities.Enums.OrderStatus.COMPLETED,
                                    course.project.ua.tirevault.Entities.Enums.OrderStatus.CANCELLED))
                    .forEach(orderRepository::delete);
            serviceRequestRepository.findByUserAndStatusInOrderByCreatedAtDesc(user,
                            List.of(
                                    course.project.ua.tirevault.Entities.Enums.ServiceRequestStatus.PENDING,
                                    course.project.ua.tirevault.Entities.Enums.ServiceRequestStatus.ACCEPTED,
                                    course.project.ua.tirevault.Entities.Enums.ServiceRequestStatus.SCHEDULED,
                                    course.project.ua.tirevault.Entities.Enums.ServiceRequestStatus.COMPLETED,
                                    course.project.ua.tirevault.Entities.Enums.ServiceRequestStatus.CANCELLED))
                    .forEach(serviceRequestRepository::delete);
            userRepository.delete(user);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        return user != null && user.getRole() == UserRole.ADMIN;
    }
}