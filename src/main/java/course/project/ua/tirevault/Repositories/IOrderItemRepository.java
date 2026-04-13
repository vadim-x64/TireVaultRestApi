package course.project.ua.tirevault.Repositories;

import course.project.ua.tirevault.Entities.Models.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IOrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Modifying
    @Query("delete from OrderItem oi where oi.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);

    @Modifying
    @Query("delete from OrderItem oi where oi.order.user.id = :userId")
    void deleteByOrderUserId(@Param("userId") Long userId);
}