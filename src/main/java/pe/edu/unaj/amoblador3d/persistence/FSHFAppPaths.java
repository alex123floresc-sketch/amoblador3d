package pe.edu.unaj.amoblador3d.persistence;

import java.nio.file.Path;

public final class FSHFAppPaths {
    private FSHFAppPaths() {}

    public static Path dataDir() {
        String override = System.getProperty("amoblador.home");
        if (override != null && !override.isBlank()) return Path.of(override);
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) return Path.of(appData, "Amoblador3D");
        return Path.of(System.getProperty("user.home"), ".amoblador3d");
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
