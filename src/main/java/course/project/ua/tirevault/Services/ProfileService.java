package course.project.ua.tirevault.Services;

import course.project.ua.tirevault.Entities.Models.Customer;
import course.project.ua.tirevault.Entities.Models.ServiceRequest;
import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Repositories.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ProfileService {
    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private ICustomerRepository customerRepository;

    @Autowired
    private ICartRepository cartRepository;

    @Autowired
    private IServiceRequestRepository serviceRequestRepository;

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private IOrderItemRepository orderItemRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public User updateProfile(Long userId, String firstName, String lastName,
                              String middleName, String phone, String email) throws Exception {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new Exception("Користувача не знайдено.");
        }

        User user = userOpt.get();
        Customer customer = user.getCustomer();

        if (!phone.equals(customer.getPhone())) {
            Optional<Customer> existingCustomerWithPhone = customerRepository.findByPhone(phone);
            if (existingCustomerWithPhone.isPresent() && !existingCustomerWithPhone.get().getId().equals(customer.getId())) {
                throw new Exception("Цей номер телефону вже використовується іншим користувачем.");
            }
        }

        if (email != null && !email.isBlank()) {
            String currentEmail = user.getEmail();
            if (!email.equals(currentEmail)) {
                if (userRepository.findByEmail(email).isPresent()) {
                    throw new Exception("Ця електронна пошта вже використовується іншим користувачем.");
                }
                user.setEmail(email);
            }
        }

        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setMiddleName(middleName);
        customer.setPhone(phone);
        return userRepository.save(user);
    }

    @Transactional
    public User updateSecurity(Long userId, String username, String newPassword) throws Exception {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) throw new Exception("Користувача не знайдено.");
        User user = userOpt.get();

        if (!user.getUsername().equals(username)) {
            Optional<User> existingUser = userRepository.findByUsername(username);
            if (existingUser.isPresent()) throw new Exception("Користувач з таким логіном вже існує.");
            user.setUsername(username);
        }

        boolean isOAuthUser = "OAUTH2_GOOGLE_NO_PASSWORD".equals(user.getPassword());
        if (!isOAuthUser && newPassword != null && !newPassword.trim().isEmpty()) {
            if (newPassword.length() < 8) throw new Exception("Пароль має містити мінімум 8 символів.");
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(Long userId, String password) throws Exception {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) throw new Exception("Користувача не знайдено.");
        User user = userOpt.get();

        boolean isOAuthUser = "OAUTH2_GOOGLE_NO_PASSWORD".equals(user.getPassword());
        if (!isOAuthUser && !passwordEncoder.matches(password, user.getPassword())) {
            throw new Exception("Невірний пароль! Акаунт не було видалено.");
        }

        List<ServiceRequest> requests = serviceRequestRepository.findByUser(user);
        requests.forEach(r -> r.setUser(null));
        serviceRequestRepository.saveAll(requests);
        orderItemRepository.deleteByOrderUserId(userId);
        orderRepository.deleteByUserId(userId);
        cartRepository.findByUserId(userId).ifPresent(cartRepository::delete);
        userRepository.delete(user);
    }
}