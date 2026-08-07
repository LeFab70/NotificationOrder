package org.fab.notificationorderlive.exception;

// WebSocket : echec pendant le traitement d'un evenement de session (connect/subscribe/disconnect).
// Toujours interceptee localement et logguee, jamais propagee (ne doit pas casser la session STOMP).
public class WebSocketEventException extends RuntimeException {
    public WebSocketEventException(String phase, Throwable cause) {
        super("Echec du traitement de l'evenement WebSocket : " + phase, cause);
    }
}
