package org.fab.notificationorderlive.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fab.notificationorderlive.enums.Status;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String orderId;

    @Enumerated(EnumType.STRING)
    private Status status;
    private LocalDateTime createdAt;

}
