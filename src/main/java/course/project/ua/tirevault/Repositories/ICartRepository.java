package course.project.ua.tirevault.Repositories;

import course.project.ua.tirevault.Entities.Models.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ICartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);
}