package course.project.ua.tirevault.integration;

import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Repositories.IUserRepository;
import course.project.ua.tirevault.Services.AuthService;
import course.project.ua.tirevault.Services.ProfileService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProfileServiceIntegrationTest {
    @Autowired
    private ProfileService profileService;

    @Autowired
    private AuthService authService;

    @Autowired
    private IUserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        authService.register("Іван", "Петров", null,
                "+380501111111", "profileuser", "profile@gmail.com", "password123");
        testUser = userRepository.findByUsername("profileuser").orElseThrow();
    }

    @Test
    void updateProfile_shouldUpdateData() throws Exception {
        profileService.updateProfile(testUser.getId(),
                "Новий", "Прізвище", null, "+380502222222", "new@gmail.com");
        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("Новий", updated.getCustomer().getFirstName());
        assertEquals("new@gmail.com", updated.getEmail());
    }

    @Test
    void updateProfile_shouldThrow_whenPhoneTaken() throws Exception {
        authService.register("Петро", "Іванов", null,
                "+380503333333", "user2", "user2@gmail.com", "password123");
        assertThrows(Exception.class, () ->
                profileService.updateProfile(testUser.getId(),
                        "Іван", "Петров", null, "+380503333333", "profile@gmail.com")
        );
    }

    @Test
    void updateSecurity_shouldChangeUsername() throws Exception {
        profileService.updateSecurity(testUser.getId(), "newlogin", null);
        assertTrue(userRepository.findByUsername("newlogin").isPresent());
    }

    @Test
    void updateSecurity_shouldThrow_whenUsernameTaken() throws Exception {
        authService.register("Петро", "Іванов", null,
                "+380503333333", "takenuser", "taken@gmail.com", "password123");
        assertThrows(Exception.class, () ->
                profileService.updateSecurity(testUser.getId(), "takenuser", null)
        );
    }

    @Test
    void deleteAccount_shouldRemoveUser() throws Exception {
        Long id = testUser.getId();
        profileService.deleteAccount(id, "password123");
        assertFalse(userRepository.findById(id).isPresent());
    }

    @Test
    void deleteAccount_shouldThrow_whenWrongPassword() {
        assertThrows(Exception.class, () ->
                profileService.deleteAccount(testUser.getId(), "wrongpassword")
        );
    }
}