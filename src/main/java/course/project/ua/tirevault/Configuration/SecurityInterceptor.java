package course.project.ua.tirevault.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import course.project.ua.tirevault.Entities.Enums.UserRole;
import course.project.ua.tirevault.Entities.Models.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class SecurityInterceptor implements HandlerInterceptor {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("loggedUser") : null;
        String requestURI = request.getRequestURI();

        if (user == null) {
            sendJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Потрібна авторизація");
            return false;
        }

        UserRole userRole = user.getRole();

        if (requestURI.startsWith("/admin")) {
            if (userRole == UserRole.ADMIN) return true;
            sendJson(response, HttpServletResponse.SC_FORBIDDEN, "Доступ заборонено");
            return false;
        }

        if (userRole == UserRole.ADMIN || userRole == UserRole.MANAGER) return true;
        sendJson(response, HttpServletResponse.SC_FORBIDDEN, "Доступ заборонено");
        return false;
    }

    private void sendJson(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of("error", message)));
    }
}