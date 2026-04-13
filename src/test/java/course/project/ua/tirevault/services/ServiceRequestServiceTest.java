package course.project.ua.tirevault.services;

import course.project.ua.tirevault.Entities.Enums.ServiceRequestStatus;
import course.project.ua.tirevault.Entities.Enums.UserRole;
import course.project.ua.tirevault.Entities.Models.ServiceRequest;
import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Repositories.IServiceRequestRepository;
import course.project.ua.tirevault.Services.ServiceRequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceTest {
    @Mock
    private IServiceRequestRepository serviceRequestRepository;

    @InjectMocks
    private ServiceRequestService serviceRequestService;

    @Test
    void create_shouldSaveWithPendingStatus() {
        User user = new User();
        user.setId(1L);

        ServiceRequest saved = new ServiceRequest();
        saved.setStatus(ServiceRequestStatus.PENDING);

        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenReturn(saved);

        ServiceRequest result = serviceRequestService.create("Вадим Войцех", "+380501234567", "Київ", "Шум в двигуні", user);

        assertEquals(ServiceRequestStatus.PENDING, result.getStatus());
        verify(serviceRequestRepository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    void cancel_shouldThrow_whenRequestAlreadyCompleted() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.USER);

        ServiceRequest request = new ServiceRequest();
        request.setId(1L);
        request.setUser(user);
        request.setStatus(ServiceRequestStatus.COMPLETED);

        when(serviceRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                serviceRequestService.cancel(1L, user)
        );

        assertEquals("Це замовлення не можна скасувати.", ex.getMessage());
    }

    @Test
    void cancel_shouldThrow_whenUserIsNotOwnerOrManager() {
        User owner = new User();
        owner.setId(1L);
        owner.setRole(UserRole.USER);

        User stranger = new User();
        stranger.setId(2L);
        stranger.setRole(UserRole.USER);

        ServiceRequest request = new ServiceRequest();
        request.setId(1L);
        request.setUser(owner);
        request.setStatus(ServiceRequestStatus.PENDING);

        when(serviceRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                serviceRequestService.cancel(1L, stranger)
        );

        assertEquals("Немає доступу.", ex.getMessage());
    }

    @Test
    void schedule_shouldThrow_whenSunday() {
        ServiceRequest request = new ServiceRequest();
        request.setId(1L);
        request.setStatus(ServiceRequestStatus.ACCEPTED);

        when(serviceRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        LocalDateTime sunday = LocalDateTime.of(2025, 4, 6, 10, 0);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                serviceRequestService.schedule(1L, sunday)
        );

        assertEquals("Неділя - вихідний день.", ex.getMessage());
    }

    @Test
    void deleteByUser_shouldThrow_whenRequestIsActive() {
        User user = new User();
        user.setId(1L);

        ServiceRequest request = new ServiceRequest();
        request.setId(1L);
        request.setUser(user);
        request.setStatus(ServiceRequestStatus.PENDING);

        when(serviceRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                serviceRequestService.deleteByUser(1L, user)
        );

        assertEquals("Можна видаляти тільки завершені замовлення.", ex.getMessage());
    }
}