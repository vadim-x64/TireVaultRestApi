package course.project.ua.tirevault.Controllers.RESTful_API;

import course.project.ua.tirevault.Configuration.ApiRole;
import course.project.ua.tirevault.Entities.Enums.UserRole;
import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Services.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Профіль", description = "Управління власним профілем")
public class ProfileApiController {
    @Autowired
    private ProfileService profileService;

    @ApiRole(UserRole.USER)
    @GetMapping
    @Operation(summary = "Отримати дані свого профілю")
    public ResponseEntity<?> getProfile(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail() != null ? user.getEmail() : "",
                "role", user.getRole().name(),
                "firstName", user.getCustomer().getFirstName(),
                "lastName", user.getCustomer().getLastName(),
                "middleName", user.getCustomer().getMiddleName() != null ? user.getCustomer().getMiddleName() : "",
                "phone", user.getCustomer().getPhone() != null ? user.getCustomer().getPhone() : ""
        ));
    }

    @ApiRole(UserRole.USER)
    @PutMapping("/update")
    @Operation(summary = "Оновити основні дані профілю")
    public ResponseEntity<?> updateProfile(@RequestParam String firstName,
                                           @RequestParam String lastName,
                                           @RequestParam(required = false) String middleName,
                                           @RequestParam String phone,
                                           @RequestParam(required = false) String email,
                                           HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));
        try {
            String cleanDigits = phone.replaceAll("\\D+", "");
            if (cleanDigits.startsWith("38")) cleanDigits = cleanDigits.substring(2);
            if (cleanDigits.length() != 10)
                throw new IllegalArgumentException("Невірний формат телефону. Має бути рівно 10 цифр.");
            String fullPhone = "+38" + cleanDigits;
            User updated = profileService.updateProfile(user.getId(), firstName, lastName, middleName, fullPhone, email);
            session.setAttribute("loggedUser", updated);
            return ResponseEntity.ok(Map.of("success", true, "message", "Профіль оновлено"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @ApiRole(UserRole.USER)
    @PutMapping("/security")
    @Operation(summary = "Змінити логін або пароль")
    public ResponseEntity<?> updateSecurity(@RequestParam String username,
                                            @RequestParam(required = false) String password,
                                            HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));
        try {
            User updated = profileService.updateSecurity(user.getId(), username, password);
            session.setAttribute("loggedUser", updated);
            return ResponseEntity.ok(Map.of("success", true, "message", "Дані безпеки оновлено"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @ApiRole(UserRole.USER)
    @DeleteMapping("/delete")
    @Operation(summary = "Видалити свій акаунт")
    public ResponseEntity<?> deleteAccount(@RequestParam(required = false) String password,
                                           HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Потрібна авторизація"));
        try {
            profileService.deleteAccount(user.getId(), password);
            session.invalidate();
            return ResponseEntity.ok(Map.of("success", true, "message", "Акаунт видалено"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}