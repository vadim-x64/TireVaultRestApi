package course.project.ua.tirevault.Controllers.RESTful_API;

import course.project.ua.tirevault.Configuration.ApiRole;
import course.project.ua.tirevault.Entities.Enums.UserRole;
import course.project.ua.tirevault.Repositories.IProductRepository;
import course.project.ua.tirevault.Repositories.IWorkServiceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Пошук", description = "Пошук по товарах та послугах")
public class SearchApiController {
    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IWorkServiceRepository workServiceRepository;

    @ApiRole(UserRole.USER)
    @GetMapping
    @Operation(summary = "Пошук товарів і послуг за ключовим словом (?q=...)")
    public ResponseEntity<?> search(@RequestParam(name = "q", defaultValue = "") String query) {
        String q = query.trim();
        if (q.isEmpty()) return ResponseEntity.ok(Map.of("products", java.util.List.of(), "services", java.util.List.of(), "totalCount", 0));
        var products = productRepository.searchByKeyword(q);
        var services = workServiceRepository.searchByKeyword(q);
        return ResponseEntity.ok(Map.of(
                "query", q,
                "products", products,
                "services", services,
                "totalCount", products.size() + services.size()
        ));
    }
}