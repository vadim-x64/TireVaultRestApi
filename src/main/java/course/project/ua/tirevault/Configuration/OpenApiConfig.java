package course.project.ua.tirevault.Configuration;

import course.project.ua.tirevault.Entities.Enums.UserRole;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI tirVaultOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TireVault API")
                        .description("""
                                ## Авторизація
                                
                                **Крок 1.** Використовуємо `POST /api/auth/login` або `POST /api/auth/register`.
                                
                                **Крок 2.** Після успішного входу сесія зберігається в cookies браузера автоматично.
                                
                                **Крок 3.** Всі наступні запити будуть виконуватись від імені авторизованого користувача.
                                
                                **Вихід.** `POST /api/auth/logout`
                                
                                ---
                                
                                **OAuth2 через Google.** Відкриваємо `/oauth2/authorization/google` в новій вкладці,
                                входимо через власний акаунт, а потім вертаємось на головну і сесія буде активна.
                                """)
                        .version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList("cookieAuth"))
                .components(new Components()
                        .addSecuritySchemes("cookieAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("SESSION")
                                .description("Сесійний cookie. Встановлюється автоматично після логіну.")));
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("user")
                .displayName("Користувач")
                .pathsToMatch("/api/**")
                .addOpenApiMethodFilter(method -> {
                    ApiRole apiRole = method.getAnnotation(ApiRole.class);
                    if (apiRole == null) return true;
                    return apiRole.value() == UserRole.USER;
                })
                .build();
    }

    @Bean
    public GroupedOpenApi managerApi() {
        return GroupedOpenApi.builder()
                .group("manager")
                .displayName("Менеджер")
                .pathsToMatch("/api/**")
                .addOpenApiMethodFilter(method -> {
                    ApiRole apiRole = method.getAnnotation(ApiRole.class);
                    if (apiRole == null) return true;
                    return apiRole.value() == UserRole.USER
                            || apiRole.value() == UserRole.MANAGER;
                })
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("Адміністратор")
                .pathsToMatch("/api/**")
                .build();
    }
}