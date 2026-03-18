package org.example.flowin2.web.controller;

import org.example.flowin2.application.sala.SalaService;
import org.example.flowin2.application.usuario.UsuarioService;
import org.example.flowin2.domain.usuario.model.Usuario;
import org.example.flowin2.shared.exceptions.ResourceNotFoundException;
import org.example.flowin2.web.dto.sala.SalaRequest;
import org.example.flowin2.web.dto.sala.SalaResponse;
import org.example.flowin2.web.dto.sala.SalaUpdateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;

@RestController
@RequestMapping("/sala")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SalaController {

    private final SalaService salaService;
    private final UsuarioService usuarioService;
    private final SimpMessagingTemplate messagingTemplate;

    public SalaController(SalaService salaService, UsuarioService usuarioService, SimpMessagingTemplate messagingTemplate) {
        this.salaService = salaService;
        this.usuarioService = usuarioService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    public ResponseEntity<SalaResponse> crearSala(@RequestBody @Validated SalaRequest salaRequest) {
        Usuario usuario = getAuthenticatedUsuario();
        SalaResponse salaResponse = salaService.save(salaRequest, usuario);
        return ResponseEntity.status(201).body(salaResponse);
    }

    @PostMapping("/salir")
    public ResponseEntity<String> salirDeSala() {
        Usuario usuario = getAuthenticatedUsuario();
        boolean eraHost = usuario.getTipo() == org.example.flowin2.domain.usuario.model.Tipo.HOST;
        Long unbindingSalaId = null;

        if (eraHost && usuario.getSalaComoHost() != null) {
            unbindingSalaId = usuario.getSalaComoHost().getId();
        }

        salaService.salirDeSala(usuario);

        if (eraHost && unbindingSalaId != null) {
            // Avisar a los oyentes que la sala fue cerrada
            messagingTemplate.convertAndSend(
                    "/topic/sala/" + unbindingSalaId, 
                    "{\"type\":\"ROOM_CLOSED\",\"contenido\":\"El Host ha cerrado la sala.\"}"
            );
        }

        return ResponseEntity.ok("Saliste de la sala correctamente");
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<SalaResponse>> buscarSalas(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) String artista
    ) {
        List<SalaResponse> salas = salaService.buscarSalas(nombre, genero, artista);
        return ResponseEntity.ok(salas);
    }

    @GetMapping("/{id}/{nombre}")
    public ResponseEntity<SalaResponse> accederSalaConNombre(
            @PathVariable Long id,
            @PathVariable String nombre,
            @RequestHeader("Authorization") String token
    ) {
        SalaResponse sala = salaService.unirUsuarioASala(token, id, nombre);
        return ResponseEntity.ok(sala);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalaResponse> accederSala(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token
    ) {
        SalaResponse sala = salaService.unirUsuarioASala(token, id);
        return ResponseEntity.ok(sala);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SalaResponse> actualizarSala(
            @PathVariable Long id,
            @RequestBody SalaUpdateRequest request,
            @RequestHeader("Authorization") String token
    ) {
        SalaResponse salaActualizada = salaService.actualizarSalaComoHost(id, request, token);
        return ResponseEntity.ok(salaActualizada);
    }

    // --- Helper ---
    private Usuario getAuthenticatedUsuario() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return usuarioService.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
    }
}