package org.example.flowin2;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FlowIn2Application {
    public static void main(String[] args) {
        // 1. Cargamos dotenv de forma opcional (no explota si no hay archivo)
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        // 2. Solo inyectamos a System properties si el archivo .env existe
        // En Railway esto no hará nada, permitiendo que Spring use las variables del
        // panel.
        dotenv.entries().forEach(entry -> {
            if (System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });

        SpringApplication.run(FlowIn2Application.class, args);
    }
}