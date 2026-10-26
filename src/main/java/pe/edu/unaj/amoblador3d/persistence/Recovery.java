package pe.edu.unaj.amoblador3d.persistence;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import pe.edu.unaj.amoblador3d.model.Project;

/**
 * Autoguardado: copia del trabajo sin guardar en {@code recuperacion.json}, dentro de la
 * carpeta de datos. Si la aplicación se cierra de golpe (corte de luz, error), al volver a
 * abrirla se ofrece recuperar esa copia.
 *
 * <p>El archivo se escribe primero con otro nombre y luego se renombra: así nunca queda a
 * medio escribir aunque se corte la luz justo en ese momento.
 */
public final class Recovery {
    /** Lo recuperado: el proyecto, su id en la base (null si nunca se guardó), su nombre y cuándo se copió. */
    public record Snapshot(Project project, Long projectId, String name, Instant savedAt) {}

    private final Path file;

    public Recovery(Path file) {
        this.file = file;
    }

    public static Recovery inDataDir() {
        return new Recovery(AppPaths.dataDir().resolve("recuperacion.json"));
    }

    public void write(Project project, Long projectId, String name) throws IOException {
        JsonObject root = new JsonObject();
        if (projectId != null) root.addProperty("projectId", projectId);
        root.addProperty("name", name);
        root.addProperty("savedAt", Instant.now().toString());
        root.add("project", JsonParser.parseString(ProjectJson.write(project)));
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, root.toString(), StandardCharsets.UTF_8);
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * La copia pendiente, o vacío si no hay. Si el archivo está dañado se borra
     * (no tiene sentido preguntar por algo que no se puede abrir).
     */
    public Optional<Snapshot> read() {
        if (!Files.exists(file)) return Optional.empty();
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            Project p = ProjectJson.read(root.get("project").toString());
            JsonElement id = root.get("projectId");
            String name = root.has("name") ? root.get("name").getAsString() : "Sin título";
            Instant at = root.has("savedAt") ? Instant.parse(root.get("savedAt").getAsString()) : Instant.now();
            return Optional.of(new Snapshot(p, id == null || id.isJsonNull() ? null : id.getAsLong(), name, at));
        } catch (IOException | ProjectFormatException | JsonParseException | IllegalStateException | NullPointerException
                 | UnsupportedOperationException | DateTimeParseException | NumberFormatException e) {
            delete();
            return Optional.empty();
        }
    }

    /** Se llama al guardar o al cerrar normalmente: ya no hay nada que recuperar. */
    public void delete() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // si no se puede borrar, la próxima vez se ofrecerá recuperar una copia vieja; no es grave
        }
    }
}
