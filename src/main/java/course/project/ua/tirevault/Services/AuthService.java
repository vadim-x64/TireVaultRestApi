package course.project.ua.tirevault.Services;

import course.project.ua.tirevault.Entities.Enums.UserRole;
import course.project.ua.tirevault.Entities.Models.Customer;
import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Repositories.ICustomerRepository;
import course.project.ua.tirevault.Repositories.IUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {
    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private ICustomerRepository customerRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public User register(String firstName, String lastName, String middleName,
                         String phone, String username, String email, String password) throws Exception {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new Exception("Користувач з таким логіном вже існує.");
        }

        if (customerRepository.findByPhone(phone).isPresent()) {
            throw new Exception("Цей номер телефону вже зареєстровано.");
        }

        if (email != null && !email.isBlank() && userRepository.findByEmail(email).isPresent()) {
            throw new Exception("Ця електронна пошта вже зареєстрована.");
        }

        Customer customer = new Customer();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setMiddleName(middleName);
        customer.setPhone(phone);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(UserRole.USER);
        user.setCustomer(customer);
        return userRepository.save(user);
    }

    public User login(String username, String password) throws Exception {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            throw new Exception("Невірний логін або пароль.");
        }

        User user = userOpt.get();

        if (user.isBlocked()) {
            throw new Exception("Ваш акаунт заблоковано адміністратором.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new Exception("Невірний логін або пароль.");
        }

        return user;
    }

    public void checkEmailForReset(String email) throws Exception {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new Exception("Користувача з такою поштою не знайдено.");
        }
        if ("OAUTH2_GOOGLE_NO_PASSWORD".equals(userOpt.get().getPassword())) {
            throw new Exception("Цей акаунт використовує вхід через Google. Зміна пароля недоступна.");
        }
    }

    @Transactional
    public void resetPassword(String email, String newPassword) throws Exception {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new Exception("Користувача з такою поштою не знайдено.");
        }
        User user = userOpt.get();
        if ("OAUTH2_GOOGLE_NO_PASSWORD".equals(user.getPassword())) {
            throw new Exception("Цей акаунт використовує вхід через Google.");
        }
        if (newPassword == null || newPassword.trim().length() < 8) {
            throw new Exception("Пароль має містити мінімум 8 символів.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}