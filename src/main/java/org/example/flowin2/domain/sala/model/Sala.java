package org.example.flowin2.domain.sala.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.flowin2.domain.chatMessage.ChatMessage;
import org.example.flowin2.domain.usuario.model.Usuario;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "salas")
@Getter
@Setter
public class Sala {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @ElementCollection
    private List<String> genero;

    @Enumerated(EnumType.STRING)
    private Estado estado = Estado.ACTIVA;

    @ElementCollection
    private List<String> canciones;

    @OneToMany(mappedBy = "sala")
    private List<Usuario> usuariosConectados = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "host_id")
    private Usuario host;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "chat_message", joinColumns = @JoinColumn(name = "sala_id"))
    @OrderColumn(name = "message_index")
    private List<ChatMessage> mensajesChat = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sala other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
