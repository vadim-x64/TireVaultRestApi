package course.project.ua.tirevault.integration;

import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Repositories.IUserRepository;
import course.project.ua.tirevault.Services.AuthService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceIntegrationTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private IUserRepository userRepository;

    @Test
    void register_shouldPersistUserToDatabase() throws Exception {
        authService.register("Вадим", "Войцех", null,
                "+380501234567", "vadim", "vadim@gmail.com", "11111111");

        Optional<User> found = userRepository.findByUsername("vadim");
        assertTrue(found.isPresent());
        assertEquals("vadim@gmail.com", found.get().getEmail());
    }

    @Test
    void register_thenLogin_shouldSucceed() throws Exception {
        authService.register("Вадим", "Войцех", null,
                "+380501234567", "vadim2", "vadim2@gmail.com", "11111111");

        assertDoesNotThrow(() -> authService.login("vadim2", "11111111"));
    }

    @Test
    void register_duplicate_shouldThrow() throws Exception {
        authService.register("Вадим", "Войцех", null,
                "+380501234567", "vadim3", "vadim3@gmail.com", "11111111");

        assertThrows(Exception.class, () ->
                authService.register("X", "Y", null,
                        "+380509999999", "vadim3", "other@gmail.com", "11111111")
        );
    }

    @Test
    void login_shouldThrow_whenUserIsBlocked() throws Exception {
        authService.register("Петро", "Іваненко", null,
                "+380502222222", "blockeduser", "blocked@gmail.com", "password123");
        User user = userRepository.findByUsername("blockeduser").orElseThrow();
        user.setBlocked(true);
        userRepository.save(user);

        assertThrows(Exception.class, () -> authService.login("blockeduser", "password123"));
    }

    @Test
    void login_shouldThrow_whenWrongPassword() throws Exception {
        authService.register("Іван", "Петренко", null,
                "+380502222222", "loginuser2", "login2@gmail.com", "password123");

        assertThrows(Exception.class, () -> authService.login("loginuser2", "wrongpass"));
    }
}