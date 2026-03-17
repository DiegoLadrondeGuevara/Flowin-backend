package org.example.flowin2.web.controller;

import org.example.flowin2.application.chat.ChatService;
import org.example.flowin2.domain.chatMessage.ChatMessage;
import org.example.flowin2.infrastructure.security.JwtService;
import org.example.flowin2.web.dto.chatMessage.ChatMessageDTO;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final JwtService jwtService;

    public ChatWebSocketController(SimpMessagingTemplate messagingTemplate,
                                   ChatService chatService,
                                   JwtService jwtService) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
        this.jwtService = jwtService;
    }

    @MessageMapping("/chat.send")
    public void enviarMensaje(ChatMessageDTO message,
                              @org.springframework.messaging.handler.annotation.Header("Authorization") String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");
            String username = jwtService.extractUserName(cleanToken);

            // Save the message and get only the new message back
            ChatMessage nuevoMensaje = chatService.guardarMensaje(
                    message.getSalaId(), username, message.getContenido()
            );

            // Send ONLY the new message, not the entire history
            messagingTemplate.convertAndSend(
                    "/topic/sala/" + message.getSalaId(),
                    nuevoMensaje
            );
        } catch (Exception e) {
            messagingTemplate.convertAndSend(
                    "/topic/error/" + message.getSalaId(),
                    "Error al procesar mensaje: " + e.getMessage()
            );
        }
    }
}