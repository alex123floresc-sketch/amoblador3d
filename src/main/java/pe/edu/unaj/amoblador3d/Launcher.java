package pe.edu.unaj.amoblador3d;

import javafx.application.Application;

/**
 * Punto de entrada. Es una clase aparte de {@link App} a propósito: si la clase principal
 * extiende {@code Application}, Java exige que JavaFX esté en el module path y el .jar
 * con todas las dependencias ({@code mvnw package}) no arrancaría con un simple
 * {@code java -jar}.
 */
public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
