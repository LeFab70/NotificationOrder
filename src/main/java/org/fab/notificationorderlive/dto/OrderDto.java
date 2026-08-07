package org.fab.notificationorderlive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.fab.notificationorderlive.enums.Status;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record OrderDto(
        @NotNull(message = "Le statut est requis.")
        Status status,

        @NotBlank(message = "L'identifiant du client est requis.")
        String customerId,

        @NotNull(message = "La date de création est requise.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime createdAt
) {
}