package course.project.ua.tirevault.Entities.Models;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "work_service_categories")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkServiceCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<WorkService> workServices;
}