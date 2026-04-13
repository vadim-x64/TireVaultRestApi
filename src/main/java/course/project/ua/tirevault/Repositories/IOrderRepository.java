package course.project.ua.tirevault.Repositories;

import course.project.ua.tirevault.Entities.Enums.OrderStatus;
import course.project.ua.tirevault.Entities.Models.Order;
import course.project.ua.tirevault.Entities.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IOrderRepository extends JpaRepository<Order, Long> {
    @Modifying
    @Query("delete from Order o where o.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    List<Order> findByUserAndStatusInOrderByCreatedAtDesc(User user, List<OrderStatus> statuses);
    List<Order> findByStatusInOrderByCreatedAtDesc(List<OrderStatus> statuses);
    long countByStatus(OrderStatus status);
    long countByUserAndSeenFalse(User user);
    List<Order> findByUserAndSeenFalse(User user);
}