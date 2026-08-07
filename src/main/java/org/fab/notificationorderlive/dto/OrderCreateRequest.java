package org.fab.notificationorderlive.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderCreateRequest(
        @NotBlank(message = "L'identifiant du client est requis.")
        String customerId
) {
}
