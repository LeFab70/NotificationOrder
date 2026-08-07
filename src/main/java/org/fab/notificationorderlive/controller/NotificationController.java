package org.fab.notificationorderlive.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class NotificationController {


    @MessageMapping("/notification")
    @SendTo("/topic/notification")
    public String sendNotification(String message) {
        return message;
    }
}
