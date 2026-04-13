package course.project.ua.tirevault.services;

import course.project.ua.tirevault.Entities.Models.Customer;
import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Repositories.ICustomerRepository;
import course.project.ua.tirevault.Repositories.IUserRepository;
import course.project.ua.tirevault.Services.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private IUserRepository userRepository;

    @Mock
    private ICustomerRepository customerRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldThrow_whenUsernameExists() {
        when(userRepository.findByUsername("vadim")).thenReturn(Optional.of(new User()));

        Exception ex = assertThrows(Exception.class, () ->
                authService.register("Вадим", "Войцех", null, "+380501234567", "vadim", "vadim@gmail.com", "11111111")
        );

        assertEquals("Користувач з таким логіном вже існує.", ex.getMessage());
    }

    @Test
    void register_shouldThrow_whenPhoneExists() {
        when(userRepository.findByUsername("vadim")).thenReturn(Optional.empty());
        when(customerRepository.findByPhone("+380501234567")).thenReturn(Optional.of(new Customer()));

        Exception ex = assertThrows(Exception.class, () ->
                authService.register("Вадим", "Войцех", null, "+380501234567", "vadim", "vadim@gmail.com", "11111111")
        );

        assertEquals("Цей номер телефону вже зареєстровано.", ex.getMessage());
    }

    @Test
    void register_shouldSaveUser_whenDataIsValid() throws Exception {
        when(userRepository.findByUsername("vadim")).thenReturn(Optional.empty());
        when(customerRepository.findByPhone("+380501234567")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("vadim@gmail.com")).thenReturn(Optional.empty());

        User savedUser = new User();
        savedUser.setUsername("vadim");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = authService.register("Вадим", "Войцех", null, "+380501234567", "vadim", "vadim@gmail.com", "11111111");

        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () ->
                authService.login("user", "11111111")
        );

        assertEquals("Невірний логін або пароль.", ex.getMessage());
    }

    @Test
    void login_shouldThrow_whenUserIsBlocked() {
        User blocked = new User();
        blocked.setBlocked(true);
        blocked.setPassword("anyHash");
        when(userRepository.findByUsername("vadim")).thenReturn(Optional.of(blocked));

        Exception ex = assertThrows(Exception.class, () ->
                authService.login("vadim", "11111111")
        );

        assertEquals("Ваш акаунт заблоковано адміністратором.", ex.getMessage());
    }

    @Test
    void resetPassword_shouldThrow_whenPasswordTooShort() {
        User user = new User();
        user.setPassword("someHash");
        when(userRepository.findByEmail("vadim@gmail.com")).thenReturn(Optional.of(user));

        Exception ex = assertThrows(Exception.class, () ->
                authService.resetPassword("vadim@gmail.com", "2222")
        );

        assertEquals("Пароль має містити мінімум 8 символів.", ex.getMessage());
    }
}