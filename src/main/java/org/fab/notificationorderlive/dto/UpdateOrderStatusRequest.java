package org.fab.notificationorderlive.dto;

import jakarta.validation.constraints.NotNull;
import org.fab.notificationorderlive.enums.Status;

public record UpdateOrderStatusRequest(
        @NotNull(message = "Le nouveau statut est requis.")
        Status status
) {
}
