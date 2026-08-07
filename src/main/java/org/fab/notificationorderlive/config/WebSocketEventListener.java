package org.fab.notificationorderlive.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fab.notificationorderlive.exception.WebSocketEventException;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

// Ces listeners ne doivent jamais laisser une exception remonter : une erreur non
// interceptee ici perturberait le cycle de vie de la session STOMP en cours.
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        try {
            String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
            log.info("Nouvelle connexion WebSocket : session {}", sessionId);
        } catch (Exception ex) {
            logEventFailure("connect", ex);
        }
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        try {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
            log.info("Session {} s'est abonnee a {}", accessor.getSessionId(), accessor.getDestination());
        } catch (Exception ex) {
            logEventFailure("subscribe", ex);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        try {
            log.info("Connexion WebSocket fermee : session {} (status: {})", event.getSessionId(), event.getCloseStatus());
        } catch (Exception ex) {
            logEventFailure("disconnect", ex);
        }
    }

    private void logEventFailure(String phase, Exception cause) {
        WebSocketEventException wrapped = new WebSocketEventException(phase, cause);
        log.error(wrapped.getMessage(), wrapped);
    }
}
