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
public class SwaggerAccessInterceptor implements HandlerInterceptor {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("loggedUser") : null;
        String uri = request.getRequestURI();

        if (user == null) return true;
        UserRole role = user.getRole();

        if (isAdminDocsRequest(uri) && role != UserRole.ADMIN) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    objectMapper.writeValueAsString(Map.of("error", "Доступ заборонено"))
            );
            return false;
        }

        if (isManagerDocsRequest(uri) && role != UserRole.ADMIN && role != UserRole.MANAGER) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    objectMapper.writeValueAsString(Map.of("error", "Доступ заборонено"))
            );
            return false;
        }

        return true;
    }

    private boolean isAdminDocsRequest(String uri) {
        return uri.matches(".*/v3/api-docs/admin(/.*)?");
    }

    private boolean isManagerDocsRequest(String uri) {
        return uri.matches(".*/v3/api-docs/manager(/.*)?");
    }
}