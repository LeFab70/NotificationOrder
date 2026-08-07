package org.fab.notificationorderlive.exception;

import org.fab.notificationorderlive.enums.Status;

// Metier : tentative de faire evoluer une commande deja dans un statut final -> mappee en 409.
public class InvalidOrderStatusTransitionException extends RuntimeException {
    public InvalidOrderStatusTransitionException(String orderId, Status currentStatus) {
        super("La commande %s est dans un statut final (%s) et ne peut plus evoluer.".formatted(orderId, currentStatus));
    }
}
