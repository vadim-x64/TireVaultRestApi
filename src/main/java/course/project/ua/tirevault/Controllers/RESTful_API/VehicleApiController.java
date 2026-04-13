package course.project.ua.tirevault.Controllers.RESTful_API;

import course.project.ua.tirevault.Configuration.ApiRole;
import course.project.ua.tirevault.Entities.Enums.UserRole;
import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Entities.Models.Vehicle;
import course.project.ua.tirevault.Repositories.IProductRepository;
import course.project.ua.tirevault.Repositories.IVehicleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
@Tag(name = "Автомобілі", description = "Довідник автомобілів")
public class VehicleApiController {
    @Autowired private IVehicleRepository vehicleRepository;
    @Autowired private IProductRepository productRepository;

    @ApiRole(UserRole.USER)
    @GetMapping
    @Operation(summary = "Отримати всі автомобілі")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(vehicleRepository.findAllByOrderByBrandAscModelAscYearAsc());
    }

    @ApiRole(UserRole.USER)
    @GetMapping("/{id}")
    @Operation(summary = "Отримати автомобіль за ID")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return vehicleRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @ApiRole(UserRole.ADMIN)
    @PostMapping
    @Operation(summary = "Додати автомобіль")
    public ResponseEntity<?> add(@RequestParam String brand, @RequestParam String model,
                                 @RequestParam Integer year, @RequestParam String modification,
                                 HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).build();
        Vehicle v = new Vehicle();
        v.setBrand(brand); v.setModel(model); v.setYear(year); v.setModification(modification);
        vehicleRepository.save(v);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @ApiRole(UserRole.ADMIN)
    @PutMapping("/{id}")
    @Operation(summary = "Редагувати автомобіль")
    public ResponseEntity<?> edit(@PathVariable Long id,
                                  @RequestParam String brand, @RequestParam String model,
                                  @RequestParam Integer year, @RequestParam String modification,
                                  HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).build();
        return vehicleRepository.findById(id).map(v -> {
            v.setBrand(brand); v.setModel(model); v.setYear(year); v.setModification(modification);
            vehicleRepository.save(v);
            return ResponseEntity.ok(Map.of("success", true));
        }).orElse(ResponseEntity.notFound().build());
    }

    @ApiRole(UserRole.ADMIN)
    @DeleteMapping("/{id}")
    @Operation(summary = "Видалити автомобіль")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).build();
        vehicleRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @ApiRole(UserRole.ADMIN)
    @PostMapping("/{vehicleId}/products/{productId}")
    @Operation(summary = "Прив'язати товар до автомобіля")
    public ResponseEntity<?> linkProduct(@PathVariable Long vehicleId, @PathVariable Long productId,
                                         HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).build();
        return vehicleRepository.findById(vehicleId).map(vehicle ->
                productRepository.findById(productId).map(product -> {
                    if (!product.getVehicles().contains(vehicle)) {
                        product.getVehicles().add(vehicle);
                        productRepository.save(product);
                    }
                    return ResponseEntity.ok(Map.of("success", true));
                }).orElse(ResponseEntity.notFound().build())
        ).orElse(ResponseEntity.notFound().build());
    }

    @ApiRole(UserRole.ADMIN)
    @DeleteMapping("/{vehicleId}/products/{productId}")
    @Operation(summary = "Відв'язати товар від автомобіля")
    public ResponseEntity<?> unlinkProduct(@PathVariable Long vehicleId, @PathVariable Long productId,
                                           HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).build();
        return vehicleRepository.findById(vehicleId).map(vehicle ->
                productRepository.findById(productId).map(product -> {
                    product.getVehicles().remove(vehicle);
                    productRepository.save(product);
                    return ResponseEntity.ok(Map.of("success", true));
                }).orElse(ResponseEntity.notFound().build())
        ).orElse(ResponseEntity.notFound().build());
    }

    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        return user != null && user.getRole() == UserRole.ADMIN;
    }
}