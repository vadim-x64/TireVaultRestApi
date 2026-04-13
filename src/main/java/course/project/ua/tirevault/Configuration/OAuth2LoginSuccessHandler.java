package course.project.ua.tirevault.Configuration;

import course.project.ua.tirevault.Entities.Enums.UserRole;
import course.project.ua.tirevault.Entities.Models.Customer;
import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Repositories.ICustomerRepository;
import course.project.ua.tirevault.Repositories.IUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final IUserRepository userRepository;
    private final ICustomerRepository customerRepository;

    public OAuth2LoginSuccessHandler(IUserRepository userRepository,
                                     ICustomerRepository customerRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");
        if (lastName == null) lastName = "";

        Optional<User> existingUser = userRepository.findByUsername(email);
        if (existingUser.isEmpty()) {
            existingUser = userRepository.findByEmail(email);
        }

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            Customer customer = new Customer();
            customer.setFirstName(firstName != null ? firstName : "Google");
            customer.setLastName(lastName);
            customer.setMiddleName(null);
            customer.setPhone(generateRandomPhone());
            customer.setEmail(email);

            User newUser = new User();
            newUser.setUsername(email);
            newUser.setEmail(email);
            newUser.setPassword("OAUTH2_GOOGLE_NO_PASSWORD");
            newUser.setRole(UserRole.USER);
            newUser.setCustomer(customer);
            user = userRepository.save(newUser);
        }

        if (user.isBlocked()) {
            response.sendRedirect("/auth?blocked=true");
            return;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("loggedUser", user);
        response.sendRedirect("/");
    }

    private String generateRandomPhone() {
        String[] mobileCodes = {"067", "068", "096", "097", "098", "050", "066", "095", "099", "063", "073", "093"};
        java.util.Random random = new java.util.Random();
        String phone;
        do {
            String code = mobileCodes[random.nextInt(mobileCodes.length)];
            String number = String.format("%07d", random.nextInt(10_000_000));
            phone = "+38" + code + number;
        } while (customerRepository.findByPhone(phone).isPresent());
        return phone;
    }
}