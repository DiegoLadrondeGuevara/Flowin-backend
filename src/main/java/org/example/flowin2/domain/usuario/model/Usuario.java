package org.example.flowin2.domain.usuario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.flowin2.domain.sala.model.Sala;

import java.util.List;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String mail;

    @Column(nullable = false)
    private String password;

    @ElementCollection
    private List<String> gustosMusicales;

    @Enumerated(EnumType.STRING)
    private Tipo tipo;

    @ElementCollection
    private List<String> artistasFavoritos;

    @ManyToOne
    @JoinColumn(name = "sala_id")
    private Sala sala;

    @OneToOne
    private Sala salaComoHost;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
