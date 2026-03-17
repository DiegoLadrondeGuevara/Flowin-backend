package org.example.flowin2.web.controller;

import org.example.flowin2.application.usuario.UsuarioService;
import org.example.flowin2.domain.usuario.model.Usuario;
import org.example.flowin2.infrastructure.security.JwtService;
import org.example.flowin2.shared.exceptions.ResourceNotFoundException;
import org.example.flowin2.web.dto.usuario.UsuarioRequest;
import org.example.flowin2.web.dto.usuario.UsuarioResponse;
import org.example.flowin2.web.dto.usuario.UsuarioUpdateArtistas;
import org.example.flowin2.web.dto.usuario.UsuarioUpdateGustos;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.CrossOrigin;
import java.util.Optional;

@RestController
@RequestMapping("/usuario")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UsuarioController {

    private final UsuarioService usuarioService;

    private final JwtService jwtService;

    public UsuarioController(UsuarioService usuarioService, JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    @PostMapping("/registrarse")
    public ResponseEntity<?> signUpUsuario(@RequestBody @Validated UsuarioRequest usuarioRequest) {
        UsuarioResponse usuarioResponse = usuarioService.save(usuarioRequest);
        Optional<Usuario> optionalUsuario = usuarioService.findByUsername(usuarioRequest.getUsername());
        Usuario usuario = optionalUsuario.orElseThrow(() ->
                new ResourceNotFoundException("Usuario no encontrado para username: " + usuarioRequest.getUsername())
        );
        String token = jwtService.generateToken(usuario);
        return ResponseEntity.status(201).body("Bearer " + token);
    }

    @GetMapping("/perfil")
    public ResponseEntity<UsuarioResponse> verPerfil(@RequestHeader("Authorization") String token) {
        UsuarioResponse perfil = usuarioService.obtenerPerfil(token);
        return ResponseEntity.ok(perfil);
    }

    @PatchMapping("/actualizar-artistas")
    public ResponseEntity<UsuarioResponse> actualizarArtistasFavoritos(
            @RequestHeader("Authorization") String token,
            @RequestBody @Validated UsuarioUpdateArtistas updateRequest
    ) {
        UsuarioResponse actualizado = usuarioService.actualizarArtistasFavoritos(token, updateRequest);
        return ResponseEntity.ok(actualizado);
    }

    @PatchMapping("/actualizar-gustosMusicales")
    public ResponseEntity<UsuarioResponse> actualizarGustosMusicales(
            @RequestHeader("Authorization") String token,
            @RequestBody @Validated UsuarioUpdateGustos updateRequest
    ) {
        UsuarioResponse actualizado = usuarioService.actualizarGustosMusicales(token, updateRequest);
        return ResponseEntity.ok(actualizado);
    }
}