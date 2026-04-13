package course.project.ua.tirevault.integration;

import course.project.ua.tirevault.Entities.Enums.ServiceRequestStatus;
import course.project.ua.tirevault.Entities.Models.ServiceRequest;
import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Repositories.IServiceRequestRepository;
import course.project.ua.tirevault.Repositories.IUserRepository;
import course.project.ua.tirevault.Services.AuthService;
import course.project.ua.tirevault.Services.ServiceRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ServiceRequestServiceIntegrationTest {
    @Autowired
    private ServiceRequestService serviceRequestService;

    @Autowired
    private IServiceRequestRepository serviceRequestRepository;

    @Autowired
    private IUserRepository userRepository;

    private User testUser;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        authService.register("Петро", "Іванов", null,
                "+380501111111", "testuser", "test@gmail.com", "password123");
        testUser = userRepository.findByUsername("testuser").orElseThrow();
    }

    @Test
    void create_shouldPersistWithPendingStatus() {
        ServiceRequest result = serviceRequestService.create(
                "Вадим Войцех", "+380501234567", "Київ", "Шум в двигуні", testUser
        );

        assertNotNull(result.getId());
        assertEquals(ServiceRequestStatus.PENDING, result.getStatus());

        ServiceRequest fromDb = serviceRequestRepository.findById(result.getId()).orElseThrow();
        assertEquals(ServiceRequestStatus.PENDING, fromDb.getStatus());
    }

    @Test
    void cancel_shouldChangStatusToCancelled() throws Exception {
        ServiceRequest request = serviceRequestService.create(
                "Вадим Войцех", "+380501234567", "Київ", "Шум в двигуні", testUser
        );

        serviceRequestService.cancel(request.getId(), testUser);

        ServiceRequest fromDb = serviceRequestRepository.findById(request.getId()).orElseThrow();
        assertEquals(ServiceRequestStatus.CANCELLED, fromDb.getStatus());
    }

    @Test
    void cancel_shouldThrow_whenAlreadyCompleted() {
        ServiceRequest request = serviceRequestService.create(
                "Вадим Войцех", "+380501234567", "Київ", "Перевірка", testUser
        );

        request.setStatus(ServiceRequestStatus.COMPLETED);
        serviceRequestRepository.save(request);

        assertThrows(RuntimeException.class, () ->
                serviceRequestService.cancel(request.getId(), testUser)
        );
    }

    @Test
    void schedule_shouldThrow_whenSunday() {
        ServiceRequest request = serviceRequestService.create(
                "Вадим Войцех", "+380501234567", "Київ", "Діагностика", testUser
        );

        request.setStatus(ServiceRequestStatus.ACCEPTED);
        serviceRequestRepository.save(request);

        LocalDateTime sunday = LocalDateTime.of(2025, 4, 6, 10, 0);

        assertThrows(RuntimeException.class, () ->
                serviceRequestService.schedule(request.getId(), sunday)
        );
    }

    @Test
    void deleteByUser_shouldThrow_whenRequestIsActive() {
        ServiceRequest request = serviceRequestService.create(
                "Вадим", "+380501234567", "Київ", "Заміна шин", testUser
        );

        assertThrows(RuntimeException.class, () ->
                serviceRequestService.deleteByUser(request.getId(), testUser)
        );
    }

    @Test
    void getAllActive_shouldReturnRequests() {
        serviceRequestService.create("Іван", "+380501234567", "Київ", "Тест", testUser);
        assertFalse(serviceRequestService.getAllActive().isEmpty());
    }

    @Test
    void accept_shouldChangeStatusToAccepted() {
        ServiceRequest request = serviceRequestService.create(
                "Іван", "+380501234567", "Київ", "Тест", testUser
        );
        serviceRequestService.accept(request.getId());
        ServiceRequest fromDb = serviceRequestRepository.findById(request.getId()).orElseThrow();
        assertEquals(ServiceRequestStatus.ACCEPTED, fromDb.getStatus());
    }

    @Test
    void schedule_shouldSetScheduledStatus() {
        ServiceRequest request = serviceRequestService.create(
                "Іван", "+380501234567", "Київ", "Тест", testUser
        );
        request.setStatus(ServiceRequestStatus.ACCEPTED);
        serviceRequestRepository.save(request);

        LocalDateTime monday = LocalDateTime.of(2025, 4, 7, 10, 0);
        serviceRequestService.schedule(request.getId(), monday);

        ServiceRequest fromDb = serviceRequestRepository.findById(request.getId()).orElseThrow();
        assertEquals(ServiceRequestStatus.SCHEDULED, fromDb.getStatus());
    }

    @Test
    void complete_shouldChangeStatusToCompleted() {
        ServiceRequest request = serviceRequestService.create(
                "Іван", "+380501234567", "Київ", "Тест", testUser
        );
        serviceRequestService.complete(request.getId(),
                course.project.ua.tirevault.Entities.Enums.PaymentMethod.CARD);
        ServiceRequest fromDb = serviceRequestRepository.findById(request.getId()).orElseThrow();
        assertEquals(ServiceRequestStatus.COMPLETED, fromDb.getStatus());
    }
}