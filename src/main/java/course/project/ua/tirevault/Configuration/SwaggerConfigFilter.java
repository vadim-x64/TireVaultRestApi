package course.project.ua.tirevault.Configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import course.project.ua.tirevault.Entities.Enums.UserRole;
import course.project.ua.tirevault.Entities.Models.User;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(1)
public class SwaggerConfigFilter implements Filter {
    private static final String SWAGGER_CONFIG_URI = "/v3/api-docs/swagger-config";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (!request.getRequestURI().endsWith(SWAGGER_CONFIG_URI)) {
            chain.doFilter(req, res);
            return;
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        chain.doFilter(request, wrappedResponse);
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("loggedUser") : null;
        String originalBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);

        if (user == null || originalBody.isBlank()) {
            wrappedResponse.copyBodyToResponse();
            return;
        }

        String filteredBody = filterGroupsByRole(originalBody, user.getRole());
        byte[] filteredBytes = filteredBody.getBytes(StandardCharsets.UTF_8);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setContentLength(filteredBytes.length);
        response.getOutputStream().write(filteredBytes);
        response.getOutputStream().flush();
    }

    private String filterGroupsByRole(String json, UserRole role) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode urlsNode = root.get("urls");

            if (urlsNode == null || !urlsNode.isArray()) {
                return json;
            }

            ArrayNode filtered = objectMapper.createArrayNode();

            for (JsonNode urlEntry : urlsNode) {
                String url = urlEntry.has("url") ? urlEntry.get("url").asText() : "";
                String name = urlEntry.has("name") ? urlEntry.get("name").asText() : "";

                if (isGroupAllowed(url, name, role)) {
                    filtered.add(urlEntry);
                }
            }

            ((ObjectNode) root).set("urls", filtered);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return json;
        }
    }

    private boolean isGroupAllowed(String url, String name, UserRole role) {
        boolean isAdminGroup = url.contains("/admin") || name.equalsIgnoreCase("admin");
        boolean isManagerGroup = url.contains("/manager") || name.equalsIgnoreCase("manager");

        if (isAdminGroup) {
            return role == UserRole.ADMIN;
        }

        if (isManagerGroup) {
            return role == UserRole.ADMIN || role == UserRole.MANAGER;
        }

        return true;
    }
}