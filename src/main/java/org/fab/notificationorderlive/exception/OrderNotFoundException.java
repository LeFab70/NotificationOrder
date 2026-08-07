package org.fab.notificationorderlive.exception;

// Metier : commande introuvable -> mappee en 404 par GlobalExceptionHandler.
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String orderId) {
        super("Commande introuvable : " + orderId);
    }
}
