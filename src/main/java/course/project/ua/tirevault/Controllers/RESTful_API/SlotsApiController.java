package course.project.ua.tirevault.Controllers.RESTful_API;

import course.project.ua.tirevault.Configuration.ApiRole;
import course.project.ua.tirevault.Entities.Enums.UserRole;
import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Services.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/slots")
@Tag(name = "Слоти запису", description = "Доступні часові слоти для запису на СТО")
public class SlotsApiController {
    @Autowired
    private ServiceRequestService serviceRequestService;

    @ApiRole(UserRole.MANAGER)
    @GetMapping
    @Operation(summary = "Отримати зайняті слоти на дату (менеджер)")
    public ResponseEntity<?> getBookedSlots(@RequestParam String date, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null || user.getRole() == UserRole.USER)
            return ResponseEntity.status(403).body(Map.of("error", "Доступ заборонено"));
        List<String> booked = serviceRequestService.getBookedHoursForDate(LocalDate.parse(date));
        return ResponseEntity.ok(booked);
    }
}