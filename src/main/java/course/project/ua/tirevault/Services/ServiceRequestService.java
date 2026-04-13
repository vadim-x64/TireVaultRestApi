package course.project.ua.tirevault.Services;

import course.project.ua.tirevault.Entities.Enums.PaymentMethod;
import course.project.ua.tirevault.Entities.Enums.ServiceRequestStatus;
import course.project.ua.tirevault.Entities.Models.ServiceRequest;
import course.project.ua.tirevault.Entities.Models.User;
import course.project.ua.tirevault.Repositories.IServiceRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServiceRequestService {
    @Autowired
    private IServiceRequestRepository serviceRequestRepository;

    public ServiceRequest create(String customerName, String phone, String city, String description, User user) {
        ServiceRequest request = new ServiceRequest();
        request.setCustomerName(customerName);
        request.setPhone(phone);
        request.setCity(city);
        request.setDescription(description);
        request.setUser(user);
        request.setStatus(ServiceRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        return serviceRequestRepository.save(request);
    }

    public void delete(Long id) {
        serviceRequestRepository.deleteById(id);
    }

    public void deleteByUser(Long id, User user) {
        ServiceRequest request = serviceRequestRepository.findById(id).orElseThrow(() -> new RuntimeException("Замовлення не знайдено."));
        if (request.getUser() == null || !request.getUser().getId().equals(user.getId())) throw new RuntimeException("Немає доступу.");
        if (!List.of(ServiceRequestStatus.COMPLETED, ServiceRequestStatus.CANCELLED).contains(request.getStatus())) throw new RuntimeException("Можна видаляти тільки завершені замовлення.");
        serviceRequestRepository.deleteById(id);
    }

    public List<ServiceRequest> getActiveByUser(User user) {
        return serviceRequestRepository.findByUserAndStatusInOrderByCreatedAtDesc(user, List.of(ServiceRequestStatus.PENDING, ServiceRequestStatus.ACCEPTED, ServiceRequestStatus.SCHEDULED));
    }

    public List<ServiceRequest> getCompletedByUser(User user) {
        return serviceRequestRepository.findByUserAndStatusInOrderByCreatedAtDesc(user, List.of(ServiceRequestStatus.COMPLETED, ServiceRequestStatus.CANCELLED));
    }

    public List<ServiceRequest> getAllActive() {
        return serviceRequestRepository.findByStatusInOrderByCreatedAtDesc(List.of(ServiceRequestStatus.PENDING, ServiceRequestStatus.ACCEPTED, ServiceRequestStatus.SCHEDULED));
    }

    public List<ServiceRequest> getAllCompleted() {
        return serviceRequestRepository.findByStatusInOrderByCreatedAtDesc(List.of(ServiceRequestStatus.COMPLETED, ServiceRequestStatus.CANCELLED));
    }

    public void accept(Long id) {
        ServiceRequest request = serviceRequestRepository.findById(id).orElseThrow(() -> new RuntimeException("Замовлення не знайдено."));
        request.setStatus(ServiceRequestStatus.ACCEPTED);
        request.setSeen(false);
        serviceRequestRepository.save(request);
    }

    public void schedule(Long id, LocalDateTime scheduledAt) {
        ServiceRequest request = serviceRequestRepository.findById(id).orElseThrow(() -> new RuntimeException("Замовлення не знайдено."));
        java.time.DayOfWeek dow = scheduledAt.getDayOfWeek();
        LocalTime time = scheduledAt.toLocalTime();

        if (dow == java.time.DayOfWeek.SUNDAY) {
            throw new RuntimeException("Неділя - вихідний день.");
        }

        if (dow == java.time.DayOfWeek.SATURDAY) {
            if (time.isBefore(LocalTime.of(10, 0)) || time.isAfter(LocalTime.of(15, 0))) {
                throw new RuntimeException("У суботу прийом з 10:00 до 15:00.");
            }
        } else {
            if (time.isBefore(LocalTime.of(9, 0)) || time.isAfter(LocalTime.of(17, 0))) {
                throw new RuntimeException("У будні прийом з 09:00 до 17:00.");
            }
        }

        List<ServiceRequestStatus> excluded = List.of(ServiceRequestStatus.CANCELLED, ServiceRequestStatus.COMPLETED);
        boolean slotTaken = serviceRequestRepository.findByScheduledAtBetweenAndStatusNotIn(scheduledAt.minusMinutes(1), scheduledAt.plusMinutes(1), excluded).stream().anyMatch(r -> !r.getId().equals(id));

        if (slotTaken) {
            throw new RuntimeException("Цей час вже заброньований. Оберіть інший.");
        }

        request.setScheduledAt(scheduledAt);
        request.setStatus(ServiceRequestStatus.SCHEDULED);
        request.setSeen(false);
        serviceRequestRepository.save(request);
    }

    public void unschedule(Long id) {
        ServiceRequest request = serviceRequestRepository.findById(id).orElseThrow(() -> new RuntimeException("Замовлення не знайдено."));
        request.setScheduledAt(null);
        request.setStatus(ServiceRequestStatus.ACCEPTED);
        request.setSeen(false);
        serviceRequestRepository.save(request);
    }

    public void complete(Long id, PaymentMethod paymentMethod) {
        ServiceRequest request = serviceRequestRepository.findById(id).orElseThrow(() -> new RuntimeException("Замовлення не знайдено."));
        request.setStatus(ServiceRequestStatus.COMPLETED);
        request.setPaymentMethod(paymentMethod);
        request.setSeen(false);
        serviceRequestRepository.save(request);
    }

    public void cancel(Long id, User user) {
        ServiceRequest request = serviceRequestRepository.findById(id).orElseThrow(() -> new RuntimeException("Замовлення не знайдено."));
        boolean isOwner = request.getUser() != null && request.getUser().getId().equals(user.getId());
        boolean isManager = user.getRole().name().equals("MANAGER");

        if (!isOwner && !isManager) {
            throw new RuntimeException("Немає доступу.");
        }

        List<ServiceRequestStatus> cancellable = List.of(ServiceRequestStatus.PENDING, ServiceRequestStatus.ACCEPTED, ServiceRequestStatus.SCHEDULED);
        if (!cancellable.contains(request.getStatus())) {
            throw new RuntimeException("Це замовлення не можна скасувати.");
        }

        request.setStatus(ServiceRequestStatus.CANCELLED);
        request.setSeen(false);
        serviceRequestRepository.save(request);
    }

    public long countPending() {
        return serviceRequestRepository.countByStatus(ServiceRequestStatus.PENDING);
    }

    public long countUnseenByUser(User user) {
        return serviceRequestRepository.countByUserAndSeenFalse(user);
    }

    @jakarta.transaction.Transactional
    public void markAllSeenByUser(User user) {
        List<ServiceRequest> unseen = serviceRequestRepository.findByUserAndSeenFalse(user);
        unseen.forEach(r -> r.setSeen(true));
        serviceRequestRepository.saveAll(unseen);
    }

    public List<String> getBookedHoursForDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        List<ServiceRequestStatus> excluded = List.of(ServiceRequestStatus.CANCELLED, ServiceRequestStatus.COMPLETED);
        return serviceRequestRepository.findByScheduledAtBetweenAndStatusNotIn(start, end, excluded).stream().map(r -> r.getScheduledAt().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))).collect(Collectors.toList());
    }
}