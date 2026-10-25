package pe.edu.unaj.amoblador3d.persistence;

import java.nio.file.Path;

/**
 * Carpetas de datos del usuario. En Windows: {@code %APPDATA%\Amoblador3D}.
 *
 * <p>La propiedad de sistema {@code amoblador.home} permite usar otra carpeta
 * (útil para pruebas: {@code -Damoblador.home=C:\temp\prueba}).
 */
public final class AppPaths {
    private AppPaths() {}

    public static Path dataDir() {
        String override = System.getProperty("amoblador.home");
        if (override != null && !override.isBlank()) return Path.of(override);
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) return Path.of(appData, "Amoblador3D");
        return Path.of(System.getProperty("user.home"), ".amoblador3d");
    }

    /** Base de datos SQLite con los proyectos y el catálogo de modelos importados. */
    public static Path database() {
        return dataDir().resolve("amoblador3d.db");
    }

    /** Archivos .glb/.gltf importados por el usuario. */
    public static Path modelsDir() {
        return dataDir().resolve("modelos");
    }
}
