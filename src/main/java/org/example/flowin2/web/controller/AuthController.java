package org.example.flowin2.web.controller;

import org.example.flowin2.application.usuario.UsuarioService;
import org.example.flowin2.domain.usuario.model.Usuario;
import org.example.flowin2.infrastructure.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.CrossOrigin;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UsuarioService usuarioService, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password) {
        Optional<Usuario> optionalUsuario = usuarioService.findByUsername(username);

        if (optionalUsuario.isEmpty() || !passwordEncoder.matches(password, optionalUsuario.get().getPassword())) {
            return ResponseEntity.status(401).body("Credenciales inválidas");
        }

        Usuario usuario = optionalUsuario.get();
        String token = jwtService.generateToken(usuario);
        return ResponseEntity.ok("Bearer " + token);
    }
}