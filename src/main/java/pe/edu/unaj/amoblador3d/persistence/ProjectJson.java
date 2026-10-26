package pe.edu.unaj.amoblador3d.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import pe.edu.unaj.amoblador3d.model.CameraView;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.Light;
import pe.edu.unaj.amoblador3d.model.LightType;
import pe.edu.unaj.amoblador3d.model.Opening;
import pe.edu.unaj.amoblador3d.model.OpeningType;
import pe.edu.unaj.amoblador3d.model.Project;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.model.Surface;
import pe.edu.unaj.amoblador3d.model.SurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.WallSide;

/**
 * Formato de archivo .amob3d (JSON legible, el mismo de la versión anterior):
 * <pre>
 * {
 *   "app": "Amoblador3D", "version": 1,
 *   "room":      { "width": 4, "length": 3.5, "height": 2.5, "wallThickness": 0.15 },
 *   "surfaces":  { "piso": { "material": "madera", "color": "#b07d4f" }, "pared-norte": { ... }, ... },
 *   "openings":  [ { "id", "type": "window"|"door", "wall", "offset", "width", "height", "sill" } ],
 *   "ambient":   0.6,
 *   "lights":    [ { "id", "type": "point"|"spot"|"sun", "color", "intensity", ... } ],
 *   "furniture": [ { "id", "type", "x", "z", "rotation", "width", "depth", "height", "color", "locked" } ],
 *   "views":     [ { "name", "mode": "3d"|"plan"|"walk", "x", "y", "z", "yaw", "pitch", "distance" } ]
 * }
 * </pre>
 * Se usa para exportar/importar proyectos y para las "fotos" de deshacer/rehacer.
 */
public final class ProjectJson {
    public static final String APP = "Amoblador3D";
    public static final int VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ProjectJson() {}

    public static String write(Project p) {
        JsonObject root = new JsonObject();
        root.addProperty("app", APP);
        root.addProperty("version", VERSION);

        JsonObject room = new JsonObject();
        room.addProperty("width", p.room.width());
        room.addProperty("length", p.room.length());
        room.addProperty("height", p.room.height());
        room.addProperty("wallThickness", p.room.wallThickness());
        root.add("room", room);

        JsonObject surfaces = new JsonObject();
        List<Surface> all = new ArrayList<>();
        all.add(p.floor);
        all.addAll(p.walls.values());
        for (Surface s : all) {
            JsonObject j = new JsonObject();
            j.addProperty("material", s.material.id());
            j.addProperty("color", s.color);
            surfaces.add(s.id, j);
        }
        root.add("surfaces", surfaces);

        JsonArray openings = new JsonArray();
        for (Opening o : p.openings) {
            JsonObject j = new JsonObject();
            j.addProperty("id", o.id);
            j.addProperty("type", o.type.id());
            j.addProperty("wall", o.wall.id());
            j.addProperty("offset", o.offset);
            j.addProperty("width", o.width);
            j.addProperty("height", o.height);
            j.addProperty("sill", o.sill);
            openings.add(j);
        }
        root.add("openings", openings);
        root.addProperty("ambient", p.ambient);

        JsonArray lights = new JsonArray();
        for (Light l : p.lights) {
            JsonObject j = new JsonObject();
            j.addProperty("id", l.id);
            j.addProperty("type", l.type.id());
            j.addProperty("color", l.color);
            j.addProperty("intensity", l.intensity);
            j.addProperty("castShadow", l.castShadow);
            if (l.type != LightType.SUN) {
                j.addProperty("x", l.x);
                j.addProperty("y", l.y);
                j.addProperty("z", l.z);
            }
            if (l.type == LightType.SPOT) {
                j.addProperty("targetX", l.targetX);
                j.addProperty("targetZ", l.targetZ);
                j.addProperty("angle", l.angle);
                j.addProperty("penumbra", l.penumbra);
            }
            if (l.type == LightType.SUN) {
                j.addProperty("windowId", l.windowId);
                j.addProperty("elevation", l.elevation);
                j.addProperty("deviation", l.deviation);
            }
            lights.add(j);
        }
        root.add("lights", lights);

        JsonArray furniture = new JsonArray();
        for (Furniture f : p.furniture) {
            JsonObject j = new JsonObject();
            j.addProperty("id", f.id);
            j.addProperty("type", f.type);
            j.addProperty("x", f.x);
            j.addProperty("z", f.z);
            j.addProperty("rotation", f.rotation);
            j.addProperty("width", f.width);
            j.addProperty("depth", f.depth);
            j.addProperty("height", f.height);
            if (f.color != null) j.addProperty("color", f.color);
            if (f.locked) j.addProperty("locked", true);
            furniture.add(j);
        }
        root.add("furniture", furniture);

        if (!p.views.isEmpty()) {
            JsonArray views = new JsonArray();
            for (CameraView v : p.views) {
                JsonObject j = new JsonObject();
                j.addProperty("name", v.name());
                j.addProperty("mode", v.kind().id());
                j.addProperty("x", v.x());
                j.addProperty("y", v.y());
                j.addProperty("z", v.z());
                j.addProperty("yaw", v.yaw());
                j.addProperty("pitch", v.pitch());
                j.addProperty("distance", v.distance());
                views.add(j);
            }
            root.add("views", views);
        }
        return GSON.toJson(root);
    }

    private static JsonArray furnitureArray(List<Furniture> items) {
        Project p = new Project();
        p.furniture.addAll(items);
        return JsonParser.parseString(write(p)).getAsJsonObject().getAsJsonArray("furniture");
    }

    /** Texto para el portapapeles con muebles copiados (se puede pegar en otro proyecto). */
    public static String writeClipboard(List<Furniture> items) {
        JsonObject root = new JsonObject();
        root.addProperty("app", APP);
        root.addProperty("clipboard", "furniture");
        root.add("furniture", furnitureArray(items));
        return GSON.toJson(root);
    }

    /** Muebles del portapapeles, o null si el texto no son muebles copiados de Amoblador3D. */
    public static List<Furniture> readClipboard(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            JsonElement el = JsonParser.parseString(text);
            if (!el.isJsonObject()) return null;
            JsonObject root = el.getAsJsonObject();
            if (!APP.equals(str(root, "app")) || !"furniture".equals(str(root, "clipboard"))) return null;
            Project p = new Project();
            for (JsonObject j : list(root, "furniture")) p.furniture.add(readFurniture(j));
            ProjectValidator.validate(p); // mismos controles que al abrir un archivo
            return p.furniture;
        } catch (JsonParseException | ProjectFormatException | IllegalArgumentException | IllegalStateException
                 | UnsupportedOperationException e) {
            return null;
        }
    }

    private static Furniture readFurniture(JsonObject j) {
        Furniture f = new Furniture(str(j, "id"), str(j, "type"), num(j, "x", Double.NaN), num(j, "z", Double.NaN),
                num(j, "rotation", 0), num(j, "width", Double.NaN), num(j, "depth", Double.NaN),
                num(j, "height", Double.NaN), str(j, "color"));
        f.locked = j.has("locked") && j.get("locked").isJsonPrimitive() && j.get("locked").getAsBoolean();
        return f;
    }

    /** Lee y valida un proyecto. Lanza una excepción con un mensaje claro si no es válido. */
    public static Project read(String text) throws ProjectFormatException {
        JsonObject root;
        try {
            JsonElement el = JsonParser.parseString(text);
            if (!el.isJsonObject()) throw new ProjectFormatException("El archivo no es un proyecto de Amoblador3D.");
            root = el.getAsJsonObject();
        } catch (JsonParseException e) {
            throw new ProjectFormatException("El archivo no es un JSON válido.");
        }
        if (!APP.equals(str(root, "app"))) throw new ProjectFormatException("El archivo no es un proyecto de Amoblador3D.");
        double version = num(root, "version", 1);
        if (version > VERSION) {
            throw new ProjectFormatException("El proyecto fue creado con una versión más nueva de la aplicación.");
        }

        try {
            Project p = new Project();
            JsonObject room = obj(root, "room");
            p.room = new RoomDims(num(room, "width", Double.NaN), num(room, "length", Double.NaN),
                    num(room, "height", Double.NaN), num(room, "wallThickness", Double.NaN));
            p.ambient = num(root, "ambient", Project.DEFAULT_AMBIENT);

            // Los proyectos anteriores no tienen "surfaces": se quedan con los acabados por defecto.
            JsonElement surfaces = root.get("surfaces");
            if (surfaces != null && surfaces.isJsonObject()) {
                List<Surface> all = new ArrayList<>();
                all.add(p.floor);
                all.addAll(p.walls.values());
                for (Surface s : all) {
                    JsonElement e = surfaces.getAsJsonObject().get(s.id);
                    if (e == null || !e.isJsonObject()) continue;
                    JsonObject j = e.getAsJsonObject();
                    if (str(j, "material") != null) s.material = SurfaceMaterial.fromId(str(j, "material"));
                    s.color = str(j, "color");
                }
            }

            for (JsonObject j : list(root, "openings")) {
                OpeningType type = OpeningType.fromId(str(j, "type"));
                p.openings.add(new Opening(str(j, "id"), type, WallSide.fromId(str(j, "wall")), num(j, "offset", Double.NaN),
                        num(j, "width", Double.NaN), num(j, "height", Double.NaN), num(j, "sill", 0)));
            }
            for (JsonObject j : list(root, "lights")) {
                Light l = new Light(str(j, "id"), LightType.fromId(str(j, "type")));
                if (str(j, "color") != null) l.color = str(j, "color");
                l.intensity = num(j, "intensity", l.intensity);
                l.castShadow = !j.has("castShadow") || j.get("castShadow").getAsBoolean();
                l.x = num(j, "x", 0);
                l.y = num(j, "y", 0);
                l.z = num(j, "z", 0);
                l.targetX = num(j, "targetX", l.x);
                l.targetZ = num(j, "targetZ", l.z);
                l.angle = num(j, "angle", 30);
                l.penumbra = num(j, "penumbra", 0.4);
                l.windowId = str(j, "windowId");
                l.elevation = num(j, "elevation", 35);
                l.deviation = num(j, "deviation", 20);
                p.lights.add(l);
            }
            for (JsonObject j : list(root, "furniture")) p.furniture.add(readFurniture(j));
            // Las vistas son un extra: una dañada se descarta sin impedir abrir el proyecto.
            for (JsonObject j : list(root, "views")) {
                try {
                    p.views.add(new CameraView(str(j, "name"), CameraView.Kind.fromId(str(j, "mode")), num(j, "x", 0),
                            num(j, "y", 0), num(j, "z", 0), num(j, "yaw", 0), num(j, "pitch", 0), num(j, "distance", 0)));
                } catch (IllegalArgumentException ignored) {
                    // tipo de vista desconocido
                }
            }
            ProjectValidator.validate(p);
            return p;
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException e) {
            // tipos desconocidos (IllegalArgumentException) o un valor del tipo equivocado (gson)
            throw new ProjectFormatException("El proyecto tiene datos no válidos: " + e.getMessage());
        }
    }

    private static JsonObject obj(JsonObject o, String key) throws ProjectFormatException {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonObject()) throw new ProjectFormatException("Falta \"" + key + "\" en el proyecto.");
        return e.getAsJsonObject();
    }

    private static Iterable<JsonObject> list(JsonObject o, String key) throws ProjectFormatException {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return List.of();
        if (!e.isJsonArray()) throw new ProjectFormatException("\"" + key + "\" debe ser una lista.");
        List<JsonObject> result = new ArrayList<>();
        for (JsonElement item : e.getAsJsonArray()) {
            if (!item.isJsonObject()) throw new ProjectFormatException("\"" + key + "\" tiene un elemento no válido.");
            result.add(item.getAsJsonObject());
        }
        return result;
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private static double num(JsonObject o, String key, double fallback) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return fallback;
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return Double.NaN;
        return e.getAsDouble();
    }
}
