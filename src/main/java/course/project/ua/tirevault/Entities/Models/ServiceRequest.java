package course.project.ua.tirevault.Entities.Models;

import course.project.ua.tirevault.Entities.Enums.PaymentMethod;
import course.project.ua.tirevault.Entities.Enums.ServiceRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "service_requests")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceRequestStatus status = ServiceRequestStatus.PENDING;

    @Column(nullable = false)
    private boolean seen = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    public String getFormattedDate() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "";
    }

    public String getFormattedScheduledDate() {
        return scheduledAt != null ? scheduledAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "";
    }
}