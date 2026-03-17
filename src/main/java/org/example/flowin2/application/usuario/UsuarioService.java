package org.example.flowin2.application.usuario;

import org.example.flowin2.domain.usuario.model.Usuario;
import org.example.flowin2.domain.usuario.repository.UsuarioRepository;
import org.example.flowin2.infrastructure.security.JwtService;
import org.example.flowin2.shared.exceptions.ResourceConflictException;
import org.example.flowin2.shared.exceptions.ResourceNotFoundException;
import org.example.flowin2.web.dto.usuario.UsuarioRequest;
import org.example.flowin2.web.dto.usuario.UsuarioResponse;
import org.example.flowin2.web.dto.usuario.UsuarioUpdateArtistas;
import org.example.flowin2.web.dto.usuario.UsuarioUpdateGustos;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UsuarioService {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final UsuarioRepository usuarioRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UsuarioService(ApplicationEventPublisher applicationEventPublisher,
                          UsuarioRepository usuarioRepository,
                          ModelMapper modelMapper,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.usuarioRepository = usuarioRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UsuarioResponse save(UsuarioRequest usuarioRequest) {
        // Check for existing username/email
        if (usuarioRepository.findByUsername(usuarioRequest.getUsername()).isPresent()) {
            throw new ResourceConflictException("El nombre de usuario '" + usuarioRequest.getUsername() + "' ya está en uso");
        }

        applicationEventPublisher.publishEvent(
                new UserRegisteredEvent(this, usuarioRequest.getMail())
        );

        Usuario usuario = modelMapper.map(usuarioRequest, Usuario.class);
        usuario.setPassword(passwordEncoder.encode(usuarioRequest.getPassword()));
        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        return mapUsuarioToResponse(usuarioGuardado);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> findByUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPerfil(String token) {
        String cleanToken = stripBearer(token);
        String username = jwtService.extractUserName(cleanToken);
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado para el token provisto."));
        return mapUsuarioToResponse(usuario);
    }

    public UsuarioResponse actualizarArtistasFavoritos(String token, UsuarioUpdateArtistas updateRequest) {
        Usuario usuario = getUsuarioFromToken(token);
        usuario.setArtistasFavoritos(updateRequest.getArtistasFavoritos());
        Usuario actualizado = usuarioRepository.save(usuario);
        return mapUsuarioToResponse(actualizado);
    }

    public UsuarioResponse actualizarGustosMusicales(String token, UsuarioUpdateGustos updateRequest) {
        Usuario usuario = getUsuarioFromToken(token);
        usuario.setGustosMusicales(updateRequest.getGustosMusicales());
        Usuario actualizado = usuarioRepository.save(usuario);
        return mapUsuarioToResponse(actualizado);
    }

    // --- Helper methods ---

    private Usuario getUsuarioFromToken(String token) {
        String cleanToken = stripBearer(token);
        String username = jwtService.extractUserName(cleanToken);
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private String stripBearer(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    private UsuarioResponse mapUsuarioToResponse(Usuario usuario) {
        UsuarioResponse response = new UsuarioResponse();
        response.setId(usuario.getId());
        response.setUsername(usuario.getUsername());
        response.setMail(usuario.getMail());
        response.setTipo(usuario.getTipo());
        response.setGustosMusicales(usuario.getGustosMusicales());
        response.setArtistasFavoritos(usuario.getArtistasFavoritos());
        return response;
    }
}
