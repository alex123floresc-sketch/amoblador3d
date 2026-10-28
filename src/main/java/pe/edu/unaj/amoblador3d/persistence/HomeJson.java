package pe.edu.unaj.amoblador3d.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import pe.edu.unaj.amoblador3d.catalog.CustomDesign;
import pe.edu.unaj.amoblador3d.model.Home;
import pe.edu.unaj.amoblador3d.model.Project;

/**
 * Una casa con varios ambientes en el formato .amob3d. El primer ambiente va en la raíz, con el
 * formato de siempre (así una versión anterior puede abrir al menos ese ambiente), y los demás en
 * {@code "spaces"}:
 * <pre>
 * { ...proyecto del primer ambiente...,
 *   "space":  { "id", "name", "x", "z" },
 *   "spaces": [ { "id", "name", "x", "z", "project": { ...proyecto... } } ] }
 * </pre>
 */
public final class HomeJson {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private HomeJson() {}

    public static String write(Home h) {
        return write(h, List.of());
    }

    /** @param designs muebles del Diseñador que se incluyen (al exportar) */
    public static String write(Home h, List<CustomDesign> designs) {
        if (h.spaces.isEmpty()) throw new IllegalArgumentException("La casa no tiene ambientes.");
        Home.Space first = h.spaces.get(0);
        JsonObject root = JsonParser.parseString(ProjectJson.write(first.project, designs)).getAsJsonObject();
        root.add("space", meta(first));
        if (h.spaces.size() > 1) {
            JsonArray list = new JsonArray();
            for (Home.Space s : h.spaces.subList(1, h.spaces.size())) {
                JsonObject j = meta(s);
                j.add("project", JsonParser.parseString(ProjectJson.write(s.project)));
                list.add(j);
            }
            root.add("spaces", list);
        }
        return GSON.toJson(root);
    }

    private static JsonObject meta(Home.Space s) {
        JsonObject j = new JsonObject();
        j.addProperty("id", s.id);
        j.addProperty("name", s.name);
        j.addProperty("x", s.x);
        j.addProperty("z", s.z);
        return j;
    }

    /**
     * Lee una casa. Un archivo de una sola habitación (de versiones anteriores) es una casa con un
     * solo ambiente llamado {@code defaultName}.
     */
    public static Home read(String text, String defaultName) throws ProjectFormatException {
        Project first = ProjectJson.read(text);
        JsonObject root;
        try {
            root = JsonParser.parseString(text).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new ProjectFormatException("El archivo no es un JSON válido.");
        }
        Home h = new Home();
        h.spaces.add(space(root.get("space"), first, "ambiente-1", defaultName));
        JsonElement others = root.get("spaces");
        if (others != null && others.isJsonArray()) {
            int n = 2;
            for (JsonElement e : others.getAsJsonArray()) {
                if (!e.isJsonObject() || !e.getAsJsonObject().has("project")) {
                    throw new ProjectFormatException("Un ambiente de la casa está dañado.");
                }
                Project p = ProjectJson.read(e.getAsJsonObject().get("project").toString());
                h.spaces.add(space(e, p, "ambiente-" + n, "Ambiente " + n));
                n++;
                if (h.spaces.size() > Home.MAX_SPACES) throw new ProjectFormatException("La casa tiene demasiados ambientes.");
            }
        }
        fix(h);
        return h;
    }

    private static Home.Space space(JsonElement meta, Project p, String id, String name) {
        Home.Space s = new Home.Space(id, name, 0, 0, p);
        if (meta == null || !meta.isJsonObject()) return s;
        JsonObject j = meta.getAsJsonObject();
        if (j.get("id") instanceof JsonElement e && e.isJsonPrimitive()) s.id = e.getAsString();
        if (j.get("name") instanceof JsonElement e && e.isJsonPrimitive()) s.name = e.getAsString();
        s.x = num(j, "x");
        s.z = num(j, "z");
        return s;
    }

    private static double num(JsonObject j, String key) {
        JsonElement e = j.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsDouble() : Double.NaN;
    }

    /** Ids repetidos, nombres vacíos o posiciones no válidas se corrigen (no impiden abrir la casa). */
    static void fix(Home h) {
        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        int n = 1;
        for (Home.Space s : h.spaces) {
            if (s.id == null || s.id.isBlank() || !ids.add(s.id)) {
                while (!ids.add("ambiente-" + n)) n++;
                s.id = "ambiente-" + n;
            }
            if (s.name == null || s.name.isBlank()) s.name = "Ambiente";
            s.name = s.name.trim();
            if (s.name.length() > Home.MAX_NAME) s.name = s.name.substring(0, Home.MAX_NAME);
            if (!names.add(s.name.toLowerCase())) {
                String base = s.name;
                int i = 2;
                while (!names.add((base + " " + i).toLowerCase())) i++;
                s.name = base + " " + i;
            }
            if (!Double.isFinite(s.x) || !Double.isFinite(s.z) || Math.abs(s.x) > 500 || Math.abs(s.z) > 500) {
                s.x = Double.NaN;
            }
        }
        // Los que no tienen una posición válida van a un lugar libre.
        for (Home.Space s : h.spaces) {
            if (Double.isNaN(s.x)) {
                Home rest = new Home();
                for (Home.Space o : h.spaces) if (o != s && !Double.isNaN(o.x)) rest.spaces.add(o);
                double[] spot = rest.freeSpot(s.project);
                s.x = spot[0];
                s.z = spot[1];
            }
        }
    }
}
