package org.fab.notificationorderlive.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fab.notificationorderlive.enums.Status;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_history")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Order order;

    @Enumerated(EnumType.STRING)
    private Status status;
    private LocalDateTime createdAt;

}
