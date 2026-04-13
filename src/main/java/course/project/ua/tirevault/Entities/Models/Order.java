package course.project.ua.tirevault.Entities.Models;

import course.project.ua.tirevault.Entities.Enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 12)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(length = 200)
    private String station;

    @Column(name = "pay_method", length = 20)
    private String payMethod;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean seen = true;

    public String getFormattedDate() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "";
    }

    public String getItemsJson() {
        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < items.size(); i++) {
            OrderItem it = items.get(i);
            String name = it.getProduct().getName().replace("\\", "\\\\").replace("\"", "\\\"");
            sb.append("{\"name\":\"").append(name)
                    .append("\",\"qty\":").append(it.getQuantity())
                    .append(",\"price\":\"").append(it.getPrice())
                    .append("\",\"subtotal\":\"").append(it.getSubtotal()).append("\"}");
            if (i < items.size() - 1) sb.append(",");
        }

        sb.append("]");
        return sb.toString();
    }

    public String getCustomerInfo() {
        if (user == null || user.getCustomer() == null) return "";
        var c = user.getCustomer();
        return (c.getLastName() != null ? c.getLastName() : "") + " " +
                (c.getFirstName() != null ? c.getFirstName() : "") + " " +
                (c.getMiddleName() != null ? c.getMiddleName() : "");
    }

    public String getCustomerPhone() {
        if (user == null || user.getCustomer() == null) return "";
        return user.getCustomer().getPhone() != null ? user.getCustomer().getPhone() : "";
    }
}