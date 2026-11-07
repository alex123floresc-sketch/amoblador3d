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
import pe.edu.unaj.amoblador3d.catalog.FSHFCustomDesign;
import pe.edu.unaj.amoblador3d.model.FSHFHome;
import pe.edu.unaj.amoblador3d.model.FSHFProject;

public final class FSHFHomeJson {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private FSHFHomeJson() {}

    public static String write(FSHFHome h) {
        return write(h, List.of());
    }

    public static String write(FSHFHome h, List<FSHFCustomDesign> designs) {
        if (h.spaces.isEmpty()) throw new IllegalArgumentException("La casa no tiene ambientes.");
        FSHFHome.Space first = h.spaces.get(0);
        JsonObject root = JsonParser.parseString(FSHFProjectJson.write(first.project, designs)).getAsJsonObject();
        root.add("space", meta(first));
        if (h.spaces.size() > 1) {
            JsonArray list = new JsonArray();
            for (FSHFHome.Space s : h.spaces.subList(1, h.spaces.size())) {
                JsonObject j = meta(s);
                j.add("project", JsonParser.parseString(FSHFProjectJson.write(s.project)));
                list.add(j);
            }
            root.add("spaces", list);
        }
        return GSON.toJson(root);
    }

    private static JsonObject meta(FSHFHome.Space s) {
        JsonObject j = new JsonObject();
        j.addProperty("id", s.id);
        j.addProperty("name", s.name);
        j.addProperty("x", s.x);
        j.addProperty("z", s.z);
        j.addProperty("rotation", s.rotation);
        j.addProperty("level", s.level);
        return j;
    }

    public static FSHFHome read(String text, String defaultName) throws FSHFProjectFormatException {
        FSHFProject first = FSHFProjectJson.read(text);
        JsonObject root;
        try {
            root = JsonParser.parseString(text).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new FSHFProjectFormatException("El archivo no es un JSON válido.");
        }
        FSHFHome h = new FSHFHome();
        h.spaces.add(space(root.get("space"), first, "ambiente-1", defaultName));
        JsonElement others = root.get("spaces");
        if (others != null && others.isJsonArray()) {
            int n = 2;
            for (JsonElement e : others.getAsJsonArray()) {
                if (!e.isJsonObject() || !e.getAsJsonObject().has("project")) {
                    throw new FSHFProjectFormatException("Un ambiente de la casa está dañado.");
                }
                FSHFProject p = FSHFProjectJson.read(e.getAsJsonObject().get("project").toString());
                h.spaces.add(space(e, p, "ambiente-" + n, "Ambiente " + n));
                n++;
                if (h.spaces.size() > FSHFHome.MAX_SPACES) throw new FSHFProjectFormatException("La casa tiene demasiados ambientes.");
            }
        }
        fix(h);
        return h;
    }

    private static FSHFHome.Space space(JsonElement meta, FSHFProject p, String id, String name) {
        FSHFHome.Space s = new FSHFHome.Space(id, name, 0, 0, p);
        if (meta == null || !meta.isJsonObject()) return s;
        JsonObject j = meta.getAsJsonObject();
        if (j.get("id") instanceof JsonElement e && e.isJsonPrimitive()) s.id = e.getAsString();
        if (j.get("name") instanceof JsonElement e && e.isJsonPrimitive()) s.name = e.getAsString();
        s.x = num(j, "x");
        s.z = num(j, "z");
        double rotation = num(j, "rotation"), level = num(j, "level");
        s.rotation = FSHFHome.normalizeRotation(Double.isNaN(rotation) ? 0 : rotation);
        s.level = Double.isFinite(level) ? (int) Math.max(0, Math.min(FSHFHome.MAX_LEVELS - 1, Math.round(level))) : 0;
        return s;
    }

    private static double num(JsonObject j, String key) {
        JsonElement e = j.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsDouble() : Double.NaN;
    }

    static void fix(FSHFHome h) {
        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        int n = 1;
        for (FSHFHome.Space s : h.spaces) {
            if (s.id == null || s.id.isBlank() || !ids.add(s.id)) {
                while (!ids.add("ambiente-" + n)) n++;
                s.id = "ambiente-" + n;
            }
            if (s.name == null || s.name.isBlank()) s.name = "Ambiente";
            s.name = s.name.trim();
            if (s.name.length() > FSHFHome.MAX_NAME) s.name = s.name.substring(0, FSHFHome.MAX_NAME);
            if (!names.add(s.name.toLowerCase())) {
                String base = s.name;
                int i = 2;
                while (!names.add((base + " " + i).toLowerCase())) i++;
                s.name = base + " " + i;
            }
            s.rotation = FSHFHome.normalizeRotation(s.rotation);
            s.level = Math.max(0, Math.min(FSHFHome.MAX_LEVELS - 1, s.level));
            if (!Double.isFinite(s.x) || !Double.isFinite(s.z) || Math.abs(s.x) > 500 || Math.abs(s.z) > 500) {
                s.x = Double.NaN;
            }
        }

        for (FSHFHome.Space s : h.spaces) {
            if (Double.isNaN(s.x)) {
                FSHFHome rest = new FSHFHome();
                for (FSHFHome.Space o : h.spaces) if (o != s && !Double.isNaN(o.x)) rest.spaces.add(o);
                double[] spot = rest.freeSpot(s.project, s.level);
                s.x = spot[0];
                s.z = spot[1];
            }
        }
    }
}
