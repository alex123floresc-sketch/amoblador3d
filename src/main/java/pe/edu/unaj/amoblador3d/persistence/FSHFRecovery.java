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
import pe.edu.unaj.amoblador3d.model.FSHFHome;

public final class FSHFRecovery {
    public record Snapshot(FSHFHome home, Long projectId, String name, Instant savedAt) {}

    private final Path file;

    public FSHFRecovery(Path file) {
        this.file = file;
    }

    public static FSHFRecovery inDataDir() {
        return new FSHFRecovery(FSHFAppPaths.dataDir().resolve("recuperacion.json"));
    }

    public void write(FSHFHome home, Long projectId, String name) throws IOException {
        JsonObject root = new JsonObject();
        if (projectId != null) root.addProperty("projectId", projectId);
        root.addProperty("name", name);
        root.addProperty("savedAt", Instant.now().toString());
        root.add("project", JsonParser.parseString(FSHFHomeJson.write(home)));
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, root.toString(), StandardCharsets.UTF_8);
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Optional<Snapshot> read() {
        if (!Files.exists(file)) return Optional.empty();
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            FSHFHome p = FSHFHomeJson.read(root.get("project").toString(), "Ambiente 1");
            JsonElement id = root.get("projectId");
            String name = root.has("name") ? root.get("name").getAsString() : "Sin título";
            Instant at = root.has("savedAt") ? Instant.parse(root.get("savedAt").getAsString()) : Instant.now();
            return Optional.of(new Snapshot(p, id == null || id.isJsonNull() ? null : id.getAsLong(), name, at));
        } catch (IOException | FSHFProjectFormatException | JsonParseException | IllegalStateException | NullPointerException
                 | UnsupportedOperationException | DateTimeParseException | NumberFormatException e) {
            delete();
            return Optional.empty();
        }
    }

    public void delete() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }
}
