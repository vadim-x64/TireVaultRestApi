package course.project.ua.tirevault.Configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private SecurityInterceptor securityInterceptor;

    @Autowired
    private SwaggerAccessInterceptor swaggerAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/admin/**", "/manager/**")
                .excludePathPatterns(
                        "/swagger-ui/**", "/swagger-ui.html",
                        "/v3/api-docs/**", "/v3/api-docs",
                        "/webjars/**"
                );

        registry.addInterceptor(swaggerAccessInterceptor)
                .addPathPatterns("/v3/api-docs/**")
                .excludePathPatterns("/v3/api-docs/swagger-config");
    }
}