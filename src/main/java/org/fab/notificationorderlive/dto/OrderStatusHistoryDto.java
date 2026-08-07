package org.fab.notificationorderlive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.With;
import org.fab.notificationorderlive.enums.Status;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Builder
@With
public record OrderStatusHistoryDto(

        String orderId,
        @NotNull(message = "Le statut de la commande est requis.")
        Status status,
        @NotNull(message = "La date de création est requise.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime createdAt

) {
}
