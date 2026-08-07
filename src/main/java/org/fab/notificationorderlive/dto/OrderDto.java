package org.fab.notificationorderlive.dto;

import org.fab.notificationorderlive.enums.Status;

import java.time.LocalDateTime;

// Projection en lecture ; pour la creation voir OrderCreateRequest (statut/date fixes par le serveur).
public record OrderDto(
        String id,
        Status status,
        String customerId,
        LocalDateTime createdAt
) {
}