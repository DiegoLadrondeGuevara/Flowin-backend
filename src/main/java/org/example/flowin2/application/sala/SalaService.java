package org.example.flowin2.application.sala;

import org.example.flowin2.domain.sala.model.Estado;
import org.example.flowin2.domain.sala.model.Sala;
import org.example.flowin2.domain.sala.repository.SalaRepository;
import org.example.flowin2.domain.usuario.model.Tipo;
import org.example.flowin2.domain.usuario.model.Usuario;
import org.example.flowin2.domain.usuario.repository.UsuarioRepository;
import org.example.flowin2.infrastructure.security.JwtService;
import org.example.flowin2.shared.exceptions.ResourceNotFoundException;
import org.example.flowin2.web.dto.sala.SalaRequest;
import org.example.flowin2.web.dto.sala.SalaResponse;
import org.example.flowin2.web.dto.sala.SalaUpdateRequest;
import org.example.flowin2.web.dto.usuario.UsuarioResponse;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SalaService {

    private final SalaRepository salaRepository;
    private final ModelMapper modelMapper;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public SalaService(SalaRepository salaRepository, ModelMapper modelMapper,
                       JwtService jwtService, UsuarioRepository usuarioRepository,
                       SimpMessagingTemplate messagingTemplate) {
        this.salaRepository = salaRepository;
        this.modelMapper = modelMapper;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public SalaResponse save(SalaRequest salaRequest, Usuario usuario) {
        Sala sala = modelMapper.map(salaRequest, Sala.class);

        sala.setHost(usuario);
        usuario.setTipo(Tipo.HOST);
        usuario.setSalaComoHost(sala);
        sala.setUsuariosConectados(List.of(usuario));

        Sala salaGuardada = salaRepository.save(sala);
        SalaResponse response = mapToResponse(salaGuardada);
        broadcastSalasActivas();
        return response;
    }

    @Transactional(readOnly = true)
    public List<SalaResponse> buscarSalas(String nombre, String genero, String artista) {
        List<Sala> salas = salaRepository.findAll();

        return salas.stream()
                .filter(sala -> sala.getEstado() == Estado.ACTIVA)
                .filter(sala -> nombre == null || sala.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .filter(sala -> genero == null || sala.getGenero().stream().anyMatch(g -> g.equalsIgnoreCase(genero)))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public SalaResponse unirUsuarioASala(String token, Long salaId) {
        return unirUsuarioASala(token, salaId, null);
    }

    public SalaResponse unirUsuarioASala(String token, Long salaId, String salaNombre) {
        Usuario usuario = getUsuarioFromToken(token);

        Sala sala = salaRepository.findById(salaId)
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada con ID: " + salaId));

        if (salaNombre != null && !sala.getNombre().equalsIgnoreCase(salaNombre)) {
            throw new IllegalArgumentException("Nombre de sala incorrecto");
        }

        if (!sala.getUsuariosConectados().contains(usuario)) {
            sala.getUsuariosConectados().add(usuario);
            salaRepository.save(sala);
            broadcastSalasActivas();
        }

        return mapToResponse(sala);
    }

    public SalaResponse actualizarSalaComoHost(Long id, SalaUpdateRequest request, String token) {
        Usuario host = getUsuarioFromToken(token);

        Sala sala = salaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada con ID: " + id));

        if (!sala.getHost().getId().equals(host.getId())) {
            throw new SecurityException("No autorizado. Solo el host puede editar esta sala.");
        }

        if (request.getNombre() != null) sala.setNombre(request.getNombre());
        if (request.getGenero() != null) sala.setGenero(Collections.singletonList(request.getGenero()));
        if (request.getCanciones() != null) sala.setCanciones(request.getCanciones());

        salaRepository.save(sala);
        broadcastSalasActivas();
        return mapToResponse(sala);
    }

    public void salirDeSala(Usuario usuario) {
        if (usuario.getTipo() == Tipo.HOST && usuario.getSalaComoHost() != null) {
            Sala sala = usuario.getSalaComoHost();
            sala.setHost(null);
            sala.setEstado(Estado.INACTIVA);

            usuario.setTipo(Tipo.USUARIO);
            usuario.setSalaComoHost(null);

            salaRepository.save(sala);
            broadcastSalasActivas();
        } else {
            // Para usuarios oyentes, lo removemos de las salas conectadas
            List<Sala> salas = salaRepository.findAll();
            boolean changed = false;
            for (Sala s : salas) {
                if (s.getUsuariosConectados().contains(usuario)) {
                    s.getUsuariosConectados().remove(usuario);
                    salaRepository.save(s);
                    changed = true;
                }
            }
            if (changed) {
                broadcastSalasActivas();
            }
        }
    }

    // --- Helper methods ---

    private void broadcastSalasActivas() {
        try {
            List<SalaResponse> activas = buscarSalas(null, null, null);
            messagingTemplate.convertAndSend("/topic/salas", activas);
        } catch (Exception e) {
            System.err.println("Error broadcasting salas: " + e.getMessage());
        }
    }

    private Usuario getUsuarioFromToken(String token) {
        String cleanToken = token;
        if (token != null && token.startsWith("Bearer ")) {
            cleanToken = token.substring(7);
        }
        String username = jwtService.extractUserName(cleanToken);
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
    }

    private SalaResponse mapToResponse(Sala sala) {
        SalaResponse response = modelMapper.map(sala, SalaResponse.class);

        List<UsuarioResponse> usuariosConectados = sala.getUsuariosConectados().stream()
                .map(usuario -> modelMapper.map(usuario, UsuarioResponse.class))
                .collect(Collectors.toList());

        response.setUsuariosConectados(usuariosConectados);
        return response;
    }
}
