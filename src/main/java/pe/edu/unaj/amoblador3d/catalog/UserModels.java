package pe.edu.unaj.amoblador3d.catalog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry.Size;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry.Source;
import pe.edu.unaj.amoblador3d.persistence.CatalogRepository;
import pe.edu.unaj.amoblador3d.persistence.CatalogRepository.ModelRow;

/**
 * Modelos 3D importados por el usuario: los archivos se copian a la carpeta de modelos
 * ({@code %APPDATA%\Amoblador3D\modelos\<carpeta>\}) y sus datos se guardan en SQLite.
 */
public final class UserModels {
    /** Modelo ya copiado a la carpeta, pendiente de confirmar sus medidas. */
    public record Imported(String id, String name, String folder, String file, Path path) {}

    private final CatalogRepository repo;
    private final Path root;

    public UserModels(CatalogRepository repo, Path root) {
        this.repo = repo;
        this.root = root;
    }

    public Path root() {
        return root;
    }

    /** Entradas del catálogo de los modelos importados; avisa de los archivos que faltan. */
    public List<CatalogEntry> load(List<String> warnings) throws SQLException {
        List<CatalogEntry> result = new ArrayList<>();
        for (ModelRow m : repo.list()) {
            Path file = root.resolve(m.file()).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                warnings.add("\"" + m.name() + "\": no se encontró el archivo " + m.file() + ".");
                continue;
            }
            try {
                result.add(CatalogEntry.model(m.id(), m.name(), Source.USER,
                        new Size(m.width(), m.depth(), m.height()), file.toUri().toURL(), m.rotation(), Catalog.IMPORTED));
            } catch (IOException e) {
                warnings.add("\"" + m.name() + "\": ruta no válida.");
            }
        }
        return result;
    }

    /** Copia un .glb (o un .gltf con sus archivos .bin y texturas) a la carpeta de modelos. */
    public Imported copyIntoLibrary(Path src) throws IOException {
        String fileName = src.getFileName().toString();
        String name = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String folder = slugify(name) + "-" + Long.toString(System.currentTimeMillis(), 36);
        Path dest = root.resolve(folder);
        Files.createDirectories(dest);
        try {
            if (fileName.toLowerCase(Locale.ROOT).endsWith(".gltf")) copyGltfWithResources(src, dest);
            else Files.copy(src, dest.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            deleteFolder(dest);
            throw new IOException("No se pudo copiar el modelo: " + e.getMessage(), e);
        }
        return new Imported("user-" + folder, name, folder, folder + "/" + fileName, dest.resolve(fileName));
    }

    /** Si el usuario cancela la importación, se borra la copia. */
    public void discard(Imported imported) throws IOException {
        deleteFolder(root.resolve(imported.folder()));
    }

    public void save(Imported imported, String name, Size size, double rotation) throws SQLException {
        repo.insert(new ModelRow(imported.id(), name, imported.file(), size.width(), size.depth(), size.height(), rotation));
    }

    /** Quita un modelo importado del catálogo y borra sus archivos. */
    public void remove(String id) throws SQLException, IOException {
        for (ModelRow m : repo.list()) {
            if (!m.id().equals(id)) continue;
            repo.delete(id);
            Path folder = root.resolve(m.file()).normalize().getParent();
            if (folder != null && folder.startsWith(root) && !folder.equals(root)) deleteFolder(folder);
        }
    }

    /** Copia un .gltf junto con sus archivos externos (.bin, texturas). */
    private static void copyGltfWithResources(Path src, Path dest) throws IOException {
        JsonObject json = JsonParser.parseString(Files.readString(src)).getAsJsonObject();
        List<String> uris = new ArrayList<>();
        for (String key : List.of("buffers", "images")) {
            JsonArray arr = json.has(key) ? json.getAsJsonArray(key) : new JsonArray();
            for (JsonElement e : arr) {
                JsonElement uri = e.getAsJsonObject().get("uri");
                if (uri != null && !uri.getAsString().startsWith("data:")) uris.add(uri.getAsString());
            }
        }
        Files.copy(src, dest.resolve(src.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
        Path srcDir = src.toAbsolutePath().getParent();
        for (String uri : uris) {
            String rel = URLDecoder.decode(uri, StandardCharsets.UTF_8);
            Path target = dest.resolve(rel).normalize();
            // Seguridad: el .gltf no puede escribir fuera de su carpeta con "../"
            if (!target.startsWith(dest)) throw new IOException("Ruta no permitida dentro del .gltf: " + uri);
            Files.createDirectories(target.getParent());
            Files.copy(srcDir.resolve(rel), target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteFolder(Path folder) throws IOException {
        if (!Files.exists(folder)) return;
        try (Stream<Path> walk = Files.walk(folder)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }

    /** "Sofá Moderno" -> "sofa-moderno" */
    static String slugify(String text) {
        String s = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return s.isEmpty() ? "modelo" : s;
    }
}
