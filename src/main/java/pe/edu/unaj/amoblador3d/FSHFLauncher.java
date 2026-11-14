package pe.edu.unaj.amoblador3d;

import javafx.application.Application;

/**
 * Punto de entrada del programa. Lanza la aplicación JavaFX {@link FSHFApp}; está
 * separado de ella para que el archivo .jar con todas las librerías pueda arrancar
 * directamente.
 */
public final class FSHFLauncher {
    private FSHFLauncher() {}

    public static void main(String[] args) {
        Application.launch(FSHFApp.class, args);
    }
}
