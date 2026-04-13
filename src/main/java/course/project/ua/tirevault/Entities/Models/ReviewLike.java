package course.project.ua.tirevault.Entities.Models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_likes", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "review_id"}))
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    @ToString.Exclude
    private Review review;
}