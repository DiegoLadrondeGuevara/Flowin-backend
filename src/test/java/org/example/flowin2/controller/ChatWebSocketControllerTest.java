package org.example.flowin2.controller;

import org.example.flowin2.application.chat.ChatService;
import org.example.flowin2.domain.chatMessage.ChatMessage;
import org.example.flowin2.infrastructure.security.JwtService;
import org.example.flowin2.web.controller.ChatWebSocketController;
import org.example.flowin2.web.dto.chatMessage.ChatMessageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ChatWebSocketControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ChatService chatService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private ChatWebSocketController chatWebSocketController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testEnviarMensaje() {
        // Arrange
        String token = "Bearer testToken";
        String username = "testUser";
        Long salaIdLong = 123L;
        String contenido = "Hola!";

        ChatMessageDTO messageDTO = new ChatMessageDTO();
        messageDTO.setSalaId(salaIdLong);
        messageDTO.setContenido(contenido);

        ChatMessage nuevoMensaje = new ChatMessage(username, contenido, LocalDateTime.now());

        when(jwtService.extractUserName("testToken")).thenReturn(username);
        when(chatService.guardarMensaje(salaIdLong, username, contenido)).thenReturn(nuevoMensaje);

        // Act
        chatWebSocketController.enviarMensaje(messageDTO, token);

        // Assert
        verify(jwtService).extractUserName("testToken");
        verify(chatService).guardarMensaje(salaIdLong, username, contenido);

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messagingTemplate).convertAndSend(destinationCaptor.capture(), messageCaptor.capture());

        assertEquals("/topic/sala/" + salaIdLong, destinationCaptor.getValue());
        assertEquals(nuevoMensaje, messageCaptor.getValue());
    }
}
