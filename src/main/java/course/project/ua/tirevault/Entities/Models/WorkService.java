package course.project.ua.tirevault.Entities.Models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "work_services")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "work_service_category_id", nullable = false)
    private WorkServiceCategory category;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "working_hours", length = 100)
    private String workingHours;
}