package course.project.ua.tirevault.Controllers.RESTful_API;

import course.project.ua.tirevault.Configuration.ApiRole;
import course.project.ua.tirevault.Entities.Enums.UserRole;
import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Авторизація", description = "Вхід, реєстрація, вихід")
public class AuthApiController {
    @Autowired
    private AuthService authService;

    @ApiRole(UserRole.USER)
    @PostMapping("/login")
    @Operation(summary = "Увійти в систему")
    public ResponseEntity<?> login(@RequestParam String username,
                                   @RequestParam String password,
                                   HttpSession session) {
        if (session.getAttribute("loggedUser") != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ви вже авторизовані"));
        }

        try {
            User user = authService.login(username, password);
            session.setAttribute("loggedUser", user);
            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "role", user.getRole().name(),
                    "blocked", user.isBlocked()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @ApiRole(UserRole.USER)
    @PostMapping("/register")
    @Operation(summary = "Зареєструватись")
    public ResponseEntity<?> register(@RequestParam String firstName,
                                      @RequestParam String lastName,
                                      @RequestParam(required = false) String middleName,
                                      @RequestParam String phone,
                                      @RequestParam String username,
                                      @RequestParam String email,
                                      @RequestParam String password,
                                      HttpSession session) {
        try {
            String cleanDigits = phone.replaceAll("\\D+", "");

            if (cleanDigits.startsWith("38") && cleanDigits.length() > 10) {
                cleanDigits = cleanDigits.substring(2);
            }

            if (cleanDigits.length() != 10) {
                throw new IllegalArgumentException("Невірний формат телефону. Має бути рівно 10 цифр (наприклад, 0501234567).");
            }

            String fullPhone = "+38" + cleanDigits;
            User newUser = authService.register(firstName, lastName, middleName, fullPhone, username, email, password);
            session.setAttribute("loggedUser", newUser);
            return ResponseEntity.ok(Map.of(
                    "id", newUser.getId(),
                    "username", newUser.getUsername(),
                    "role", newUser.getRole().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @ApiRole(UserRole.USER)
    @GetMapping("/me")
    @Operation(summary = "Отримати поточного авторизованого користувача")
    public ResponseEntity<?> me(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизований"));
        }

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole().name(),
                "blocked", user.isBlocked()
        ));
    }

    @ApiRole(UserRole.USER)
    @PostMapping("/logout")
    @Operation(summary = "Вийти з системи")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpSession session) {
        session.invalidate();
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("SESSION", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        ((jakarta.servlet.http.HttpServletResponse)
                ((org.springframework.web.context.request.ServletRequestAttributes)
                        org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                        .getResponse()).addCookie(cookie);
        return ResponseEntity.ok(Map.of("message", "Вихід виконано успішно"));
    }

    @ApiRole(UserRole.USER)
    @PostMapping("/forgot-password/check")
    @Operation(summary = "Перевірити email для відновлення пароля")
    public ResponseEntity<?> forgotPasswordCheck(@RequestParam String email) {
        try {
            authService.checkEmailForReset(email);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @ApiRole(UserRole.USER)
    @PostMapping("/forgot-password/reset")
    @Operation(summary = "Скинути пароль")
    public ResponseEntity<?> forgotPasswordReset(@RequestParam String email, @RequestParam String newPassword) {
        try {
            authService.resetPassword(email, newPassword);
            return ResponseEntity.ok(Map.of("message", "Пароль успішно змінено."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}