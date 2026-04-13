package course.project.ua.tirevault.Controllers.RESTful_API;

import course.project.ua.tirevault.Configuration.ApiRole;
import course.project.ua.tirevault.Entities.Enums.PaymentMethod;
import course.project.ua.tirevault.Entities.Enums.UserRole;
import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Services.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/service-requests")
@Tag(name = "Заявки на СТО", description = "Управління заявками на обслуговування")
public class ServiceRequestApiController {
    @Autowired
    private ServiceRequestService serviceRequestService;

    @ApiRole(UserRole.USER)
    @PostMapping
    @Operation(summary = "Створити заявку на СТО")
    public ResponseEntity<?> create(@RequestParam String customerName,
                                    @RequestParam String phone,
                                    @RequestParam String city,
                                    @RequestParam(required = false) String description,
                                    HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));

        try {
            String cleanDigits = phone.replaceAll("\\D+", "");
            if (cleanDigits.startsWith("38") && cleanDigits.length() > 10) cleanDigits = cleanDigits.substring(2);
            if (cleanDigits.length() != 10)
                throw new IllegalArgumentException("Невірний формат телефону. Має бути 10 цифр (наприклад, 0501234567).");
            String fullPhone = "+38" + cleanDigits;

            serviceRequestService.create(customerName, fullPhone, city, description, user);
            return ResponseEntity.ok(Map.of("success", true, "message", "Заявку успішно створено"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @ApiRole(UserRole.USER)
    @GetMapping("/my/active")
    @Operation(summary = "Мої активні заявки")
    public ResponseEntity<?> myActive(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));
        return ResponseEntity.ok(serviceRequestService.getActiveByUser(user));
    }

    @ApiRole(UserRole.USER)
    @GetMapping("/my/completed")
    @Operation(summary = "Мої завершені заявки")
    public ResponseEntity<?> myCompleted(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));
        return ResponseEntity.ok(serviceRequestService.getCompletedByUser(user));
    }

    @ApiRole(UserRole.USER)
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Скасувати свою заявку")
    public ResponseEntity<?> cancel(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));

        try {
            serviceRequestService.cancel(id, user);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @ApiRole(UserRole.MANAGER)
    @GetMapping("/active")
    @Operation(summary = "Активні заявки (менеджер/адмін)")
    public ResponseEntity<?> getActive(HttpSession session) {
        if (!isManagerOrAdmin(session)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(serviceRequestService.getAllActive());
    }

    @ApiRole(UserRole.MANAGER)
    @GetMapping("/completed")
    @Operation(summary = "Завершені заявки (менеджер/адмін)")
    public ResponseEntity<?> getCompleted(HttpSession session) {
        if (!isManagerOrAdmin(session)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(serviceRequestService.getAllCompleted());
    }

    @ApiRole(UserRole.MANAGER)
    @PatchMapping("/{id}/accept")
    @Operation(summary = "Прийняти заявку")
    public ResponseEntity<?> accept(@PathVariable Long id, HttpSession session) {
        if (!isManagerOrAdmin(session)) return ResponseEntity.status(403).build();
        serviceRequestService.accept(id);
        return ResponseEntity.ok().build();
    }

    @ApiRole(UserRole.MANAGER)
    @PatchMapping("/{id}/schedule")
    @Operation(summary = "Запланувати заявку (формат: yyyy-MM-dd'T'HH:mm)")
    public ResponseEntity<?> schedule(@PathVariable Long id,
                                      @RequestParam String scheduledAt,
                                      HttpSession session) {
        if (!isManagerOrAdmin(session)) return ResponseEntity.status(403).build();
        LocalDateTime dt = LocalDateTime.parse(scheduledAt, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        serviceRequestService.schedule(id, dt);
        return ResponseEntity.ok().build();
    }

    @ApiRole(UserRole.MANAGER)
    @PatchMapping("/{id}/complete")
    @Operation(summary = "Завершити заявку")
    public ResponseEntity<?> complete(@PathVariable Long id,
                                      @RequestParam String paymentMethod,
                                      HttpSession session) {
        if (!isManagerOrAdmin(session)) return ResponseEntity.status(403).build();
        serviceRequestService.complete(id, PaymentMethod.valueOf(paymentMethod));
        return ResponseEntity.ok().build();
    }

    @ApiRole(UserRole.MANAGER)
    @DeleteMapping("/{id}")
    @Operation(summary = "Видалити заявку (менеджер/адмін)")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        if (!isManagerOrAdmin(session)) return ResponseEntity.status(403).build();
        serviceRequestService.delete(id);
        return ResponseEntity.ok().build();
    }

    private boolean isManagerOrAdmin(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return false;
        return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.MANAGER;
    }
}