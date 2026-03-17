package org.example.flowin2.application.usuario;

import org.example.flowin2.domain.usuario.model.Usuario;
import org.example.flowin2.domain.usuario.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado con el nombre de usuario: " + username));

        String role = usuario.getTipo() != null ? usuario.getTipo().name() : "USUARIO";

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .roles(role)
                .build();
    }
}
