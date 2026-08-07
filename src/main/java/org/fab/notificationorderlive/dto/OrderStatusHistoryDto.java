package org.fab.notificationorderlive.dto;

import lombok.Builder;
import org.fab.notificationorderlive.enums.Status;

import java.time.LocalDateTime;

// Une entree de la timeline d'une commande ; c'est aussi le payload pousse sur /topic/orders/{orderId}.
@Builder
public record OrderStatusHistoryDto(
        String id,
        String orderId,
        Status status,
        LocalDateTime createdAt
) {
}
