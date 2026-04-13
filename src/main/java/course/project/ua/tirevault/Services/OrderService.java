package course.project.ua.tirevault.Services;

import course.project.ua.tirevault.Entities.Enums.OrderStatus;
import course.project.ua.tirevault.Entities.Models.*;
import course.project.ua.tirevault.Repositories.IOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {
    @Autowired
    private IOrderRepository orderRepository;

    @Transactional
    public Order createFromCart(User user, Cart cart, String station, String payMethod) {
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setTotal(cart.getTotal());
        order.setStation(station);
        order.setPayMethod(payMethod);
        order.setStatus(OrderStatus.PENDING);
        order.setSeen(false);

        for (CartItem ci : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(ci.getProduct());
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(ci.getProduct().getPrice());
            order.getItems().add(oi);
        }

        return orderRepository.save(order);
    }

    private String generateOrderNumber() {
        long num = ThreadLocalRandom.current().nextLong(100_000_000_000L, 1_000_000_000_000L);
        return String.valueOf(num);
    }

    public List<Order> getActiveByUser(User user) {
        return orderRepository.findByUserAndStatusInOrderByCreatedAtDesc(user, List.of(OrderStatus.PENDING, OrderStatus.PROCESSING));
    }

    public List<Order> getCompletedByUser(User user) {
        return orderRepository.findByUserAndStatusInOrderByCreatedAtDesc(user, List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
    }

    public List<Order> getAllActive() {
        return orderRepository.findByStatusInOrderByCreatedAtDesc(List.of(OrderStatus.PENDING, OrderStatus.PROCESSING));
    }

    public List<Order> getAllCompleted() {
        return orderRepository.findByStatusInOrderByCreatedAtDesc(List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
    }

    public void setStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Замовлення не знайдено."));
        order.setStatus(status);
        order.setSeen(false);
        orderRepository.save(order);
    }

    public void cancel(Long id, User user) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Замовлення не знайдено."));
        boolean isOwner = order.getUser().getId().equals(user.getId());
        boolean isManager = user.getRole().name().equals("MANAGER");
        if (!isOwner && !isManager) throw new RuntimeException("Немає доступу.");
        if (!List.of(OrderStatus.PENDING, OrderStatus.PROCESSING).contains(order.getStatus())) throw new RuntimeException("Це замовлення не можна скасувати.");
        order.setStatus(OrderStatus.CANCELLED);
        order.setSeen(false);
        orderRepository.save(order);
    }

    public void delete(Long id) {
        orderRepository.deleteById(id);
    }

    public void deleteByUser(Long id, User user) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Замовлення не знайдено."));
        if (!order.getUser().getId().equals(user.getId())) throw new RuntimeException("Немає доступу.");
        if (!List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED).contains(order.getStatus())) throw new RuntimeException("Можна видаляти тільки завершені замовлення.");
        orderRepository.deleteById(id);
    }

    public long countPending() {
        return orderRepository.countByStatus(OrderStatus.PENDING);
    }

    public long countUnseenByUser(User user) {
        return orderRepository.countByUserAndSeenFalse(user);
    }

    @Transactional
    public void markAllSeenByUser(User user) {
        List<Order> unseen = orderRepository.findByUserAndSeenFalse(user);
        unseen.forEach(o -> o.setSeen(true));
        orderRepository.saveAll(unseen);
    }
}