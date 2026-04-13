package course.project.ua.tirevault.Repositories;

import course.project.ua.tirevault.Entities.Enums.ServiceRequestStatus;
import course.project.ua.tirevault.Entities.Models.ServiceRequest;
import course.project.ua.tirevault.Entities.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    List<ServiceRequest> findByUser(User user);
    List<ServiceRequest> findByUserAndStatusInOrderByCreatedAtDesc(User user, List<ServiceRequestStatus> statuses);
    List<ServiceRequest> findByStatusInOrderByCreatedAtDesc(List<ServiceRequestStatus> statuses);
    long countByStatus(ServiceRequestStatus status);
    long countByUserAndSeenFalse(User user);
    List<ServiceRequest> findByUserAndSeenFalse(User user);
    List<ServiceRequest> findByScheduledAtBetweenAndStatusNotIn(LocalDateTime start, LocalDateTime end, List<ServiceRequestStatus> excludedStatuses);
}