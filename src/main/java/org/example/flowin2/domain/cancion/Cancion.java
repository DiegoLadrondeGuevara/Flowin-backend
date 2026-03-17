package org.example.flowin2.domain.cancion;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Cancion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;
    private String artista;
    private String album;
    private String genero;
    private int anio;
    private String portadaUrl;
    private String url;
    private int duracion;
}