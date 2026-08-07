package org.fab.notificationorderlive.exception;

// WebSocket : echec de diffusion STOMP. Jamais propagee au client REST : le statut
// est deja persiste, seule la notification live a echoue (best-effort).
public class OrderNotificationException extends RuntimeException {
    public OrderNotificationException(String orderId, Throwable cause) {
        super("Echec de la diffusion WebSocket pour la commande " + orderId, cause);
    }
}
