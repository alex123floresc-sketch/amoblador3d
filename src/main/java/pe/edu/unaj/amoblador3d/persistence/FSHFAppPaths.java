package pe.edu.unaj.amoblador3d.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Rutas de la carpeta de datos del usuario (%APPDATA%\Space Cat en Windows): base de
 * datos, modelos importados y registros de errores. Si existe la carpeta de la versión
 * anterior (Amoblador3D) y todavía no la nueva, los datos se trasladan una sola vez para
 * no perder los proyectos guardados.
 */
public final class FSHFAppPaths {
    private FSHFAppPaths() {}

    private static final Logger LOG = Logger.getLogger(FSHFAppPaths.class.getName());
    private static final String APP_DIR = "Space Cat";
    private static final String LEGACY_DIR = "Amoblador3D";
    private static boolean migrated = false;

    public static synchronized Path dataDir() {
        String override = System.getProperty("spacecat.home");
        if (override == null || override.isBlank()) override = System.getProperty("amoblador.home");
        if (override != null && !override.isBlank()) return Path.of(override);

        String appData = System.getenv("APPDATA");
        Path base = (appData != null && !appData.isBlank())
                ? Path.of(appData) : Path.of(System.getProperty("user.home"));
        Path current = (appData != null && !appData.isBlank())
                ? base.resolve(APP_DIR) : base.resolve(".spacecat");
        Path legacy = (appData != null && !appData.isBlank())
                ? base.resolve(LEGACY_DIR) : base.resolve(".amoblador3d");

        if (!migrated) {
            migrated = true;
            try {
                if (!Files.exists(current) && Files.exists(legacy)) {
                    Files.move(legacy, current);
                    LOG.info(() -> "Datos trasladados de " + legacy + " a " + current);
                }
            } catch (IOException | RuntimeException e) {
                LOG.warning(() -> "No se pudo trasladar la carpeta de datos; se usa la anterior: " + e.getMessage());
                if (Files.exists(legacy)) return legacy;
            }
        }
        return current;
    }

    public static Path database() {
        return dataDir().resolve("amoblador3d.db");
    }

    public static Path modelsDir() {
        return dataDir().resolve("modelos");
    }

    public static Path logsDir() {
        return dataDir().resolve("logs");
    }
}
