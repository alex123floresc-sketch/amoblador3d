package pe.edu.unaj.amoblador3d.catalog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry.Size;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry.Source;

/**
 * Lee los modelos 3D que vienen con la aplicación: {@code src/main/resources/assets/catalog.json}.
 *
 * <p>Formato de cada entrada: {@code { "id", "name", "file", "width", "depth", "height", "rotation", "category" }},
 * donde {@code file} es relativo a la carpeta {@code assets} y {@code category} es opcional
 * (una de {@link Catalog#CATEGORIES}; por defecto "Decoración").
 */
public final class BuiltinCatalog {
    private static final String ROOT = "/assets/";

    private BuiltinCatalog() {}

    /** Devuelve las entradas válidas y agrega a {@code warnings} los problemas encontrados. */
    public static List<CatalogEntry> load(List<String> warnings) {
        List<CatalogEntry> result = new ArrayList<>();
        try (InputStream in = BuiltinCatalog.class.getResourceAsStream(ROOT + "catalog.json")) {
            if (in == null) return result;
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray models = json.has("models") ? json.getAsJsonArray("models") : new JsonArray();
            for (JsonElement el : models) {
                CatalogEntry entry = parse(el, warnings);
                if (entry != null) result.add(entry);
            }
        } catch (Exception e) {
            warnings.add("catalog.json tiene un error de formato: " + e.getMessage());
        }
        return result;
    }

    private static CatalogEntry parse(JsonElement el, List<String> warnings) {
        if (!el.isJsonObject()) {
            warnings.add("Entrada inválida en catalog.json.");
            return null;
        }
        JsonObject m = el.getAsJsonObject();
        String id = string(m, "id"), name = string(m, "name"), file = string(m, "file");
        if (id == null || name == null || file == null) {
            warnings.add("Entrada inválida en catalog.json: necesita id, name y file.");
            return null;
        }
        double w = number(m, "width"), d = number(m, "depth"), h = number(m, "height");
        if (!(w > 0 && d > 0 && h > 0)) {
            warnings.add("\"" + name + "\": width, depth y height deben ser números mayores que 0 (en metros).");
            return null;
        }
        URL url = BuiltinCatalog.class.getResource(ROOT + file.replace('\\', '/'));
        if (url == null) {
            warnings.add("\"" + name + "\": no se encontró el archivo " + file + ".");
            return null;
        }
        double rotation = m.has("rotation") ? number(m, "rotation") : 0;
        String category = string(m, "category");
        if (category == null || !Catalog.CATEGORIES.contains(category)) category = Catalog.DECORATION;
        return CatalogEntry.model(id, name, Source.BUILTIN, new Size(w, d, h), url, Double.isFinite(rotation) ? rotation : 0,
                category);
    }

    private static String string(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
    }

    private static double number(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsDouble() : Double.NaN;
    }
}
