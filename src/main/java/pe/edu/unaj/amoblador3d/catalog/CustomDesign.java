package pe.edu.unaj.amoblador3d.catalog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import pe.edu.unaj.amoblador3d.model.Finish;
import pe.edu.unaj.amoblador3d.util.Units;

/**
 * Mueble diseñado por el usuario en el Diseñador de muebles: un conjunto de piezas simples
 * (cajas, cilindros, esferas y barras), cada una con su posición, medidas y color.
 *
 * <p>Las piezas se describen en metros con el frente del mueble hacia +Z. Al colocarlo en la
 * habitación, el conjunto se escala para medir exactamente el ancho, alto y profundidad del
 * mueble (igual que los modelos 3D), así las colisiones siguen siendo exactas.
 *
 * <p>Cada pieza se pinta con el color principal o el secundario del mueble (que luego se pueden
 * cambiar en cada copia colocada) o con un color propio fijo (p. ej. tiradores metálicos).
 */
public final class CustomDesign {
    public static final int MAX_PIECES = 300, MAX_NAME = 40;
    /** Medida máxima de una pieza y distancia máxima al origen (m). */
    public static final double MAX_SIZE = 6, MAX_POS = 6, MIN_SIZE = 0.001;
    private static final Pattern COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    public enum Shape {
        BOX("caja", "Caja"), CYLINDER("cilindro", "Cilindro"), SPHERE("esfera", "Esfera"),
        BAR_X("barra-x", "Barra a lo ancho"), BAR_Z("barra-z", "Barra a lo profundo");

        private final String id, label;

        Shape(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String id() {
            return id;
        }

        public String label() {
            return label;
        }

        public static Shape fromId(String id) {
            for (Shape s : values()) if (s.id.equals(id)) return s;
            throw new IllegalArgumentException("Forma desconocida: " + id);
        }
    }

    /** Qué color usa una pieza. */
    public enum Paint {
        MAIN("principal", "Color principal"), ACCENT("secundario", "Color secundario"), OWN("propio", "Color propio");

        private final String id, label;

        Paint(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String id() {
            return id;
        }

        public String label() {
            return label;
        }

        public static Paint fromId(String id) {
            for (Paint p : values()) if (p.id.equals(id)) return p;
            throw new IllegalArgumentException("Pintura desconocida: " + id);
        }
    }

    /**
     * Una pieza. (x, z) es el centro de la pieza en planta e y la altura de su base sobre el piso;
     * width, height y depth son sus medidas en X, Y y Z.
     */
    public static final class Piece {
        public String name;
        public Shape shape = Shape.BOX;
        public double x, y, z;
        public double width = 0.4, height = 0.4, depth = 0.4;
        public Paint paint = Paint.MAIN;
        /** Solo con {@link Paint#OWN}. */
        public String color = "#c0c0c0";
        /** null = el acabado del mueble. */
        public Finish finish;

        public Piece copy() {
            Piece p = new Piece();
            p.name = name;
            p.shape = shape;
            p.x = x;
            p.y = y;
            p.z = z;
            p.width = width;
            p.height = height;
            p.depth = depth;
            p.paint = paint;
            p.color = color;
            p.finish = finish;
            return p;
        }
    }

    /** Caja envolvente de todas las piezas. */
    public record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        public double width() {
            return maxX - minX;
        }

        public double height() {
            return maxY - minY;
        }

        public double depth() {
            return maxZ - minZ;
        }
    }

    public String id;
    public String name = "Mi mueble";
    public String category = Catalog.MY_DESIGNS;
    public String color = "#a0764f";
    public String color2 = "#f4f1ea";
    public Finish finish = Finish.SATINADO;
    public final List<Piece> pieces = new ArrayList<>();

    public CustomDesign copy() {
        CustomDesign d = new CustomDesign();
        d.id = id;
        d.name = name;
        d.category = category;
        d.color = color;
        d.color2 = color2;
        d.finish = finish;
        for (Piece p : pieces) d.pieces.add(p.copy());
        return d;
    }

    public Bounds bounds() {
        if (pieces.isEmpty()) return new Bounds(-0.5, 0, -0.5, 0.5, 1, 0.5);
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (Piece p : pieces) {
            minX = Math.min(minX, p.x - p.width / 2);
            maxX = Math.max(maxX, p.x + p.width / 2);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y + p.height);
            minZ = Math.min(minZ, p.z - p.depth / 2);
            maxZ = Math.max(maxZ, p.z + p.depth / 2);
        }
        return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public boolean uses(Paint paint) {
        for (Piece p : pieces) if (p.paint == paint) return true;
        return false;
    }

    /** Revisa que el diseño se pueda guardar. Devuelve un error o null. */
    public String validate() {
        if (name == null || name.isBlank()) return "Ponle un nombre al mueble.";
        if (name.trim().length() > MAX_NAME) return "El nombre puede tener hasta " + MAX_NAME + " letras.";
        if (pieces.isEmpty()) return "Agrega al menos una pieza.";
        if (pieces.size() > MAX_PIECES) return "Un mueble puede tener hasta " + MAX_PIECES + " piezas.";
        if (!validColor(color) || !validColor(color2)) return "Los colores deben tener el formato #rrggbb.";
        for (int i = 0; i < pieces.size(); i++) {
            Piece p = pieces.get(i);
            String what = "La pieza " + (i + 1);
            for (double v : new double[] {p.x, p.y, p.z, p.width, p.height, p.depth}) {
                if (!Double.isFinite(v)) return what + " tiene un número no válido.";
            }
            if (p.width < MIN_SIZE || p.height < MIN_SIZE || p.depth < MIN_SIZE) return what + " es demasiado delgada (mínimo 1 mm).";
            if (p.width > MAX_SIZE || p.height > MAX_SIZE || p.depth > MAX_SIZE) return what + " mide más de " + Units.number(MAX_SIZE) + " m.";
            if (Math.abs(p.x) > MAX_POS || Math.abs(p.z) > MAX_POS || p.y < 0 || p.y > MAX_POS) return what + " está demasiado lejos.";
            if (p.paint == Paint.OWN && !validColor(p.color)) return what + " tiene un color no válido.";
        }
        Bounds b = bounds();
        if (b.width() > MAX_SIZE || b.depth() > MAX_SIZE || b.height() > 4) {
            return "El mueble completo no puede medir más de 6 x 6 m ni más de 4 m de alto.";
        }
        return null;
    }

    private static boolean validColor(String c) {
        return c != null && COLOR.matcher(c).matches();
    }

    // ---------------------------------------------------------------- JSON

    public JsonObject toJson() {
        JsonObject j = new JsonObject();
        j.addProperty("id", id);
        j.addProperty("name", name);
        j.addProperty("category", category);
        j.addProperty("color", color);
        j.addProperty("color2", color2);
        j.addProperty("finish", finish.id());
        JsonArray list = new JsonArray();
        for (Piece p : pieces) {
            JsonObject o = new JsonObject();
            if (p.name != null && !p.name.isBlank()) o.addProperty("name", p.name);
            o.addProperty("shape", p.shape.id());
            o.addProperty("x", p.x);
            o.addProperty("y", p.y);
            o.addProperty("z", p.z);
            o.addProperty("width", p.width);
            o.addProperty("height", p.height);
            o.addProperty("depth", p.depth);
            o.addProperty("paint", p.paint.id());
            if (p.paint == Paint.OWN) o.addProperty("color", p.color);
            if (p.finish != null) o.addProperty("finish", p.finish.id());
            list.add(o);
        }
        j.add("pieces", list);
        return j;
    }

    /** Lee un diseño. Lanza IllegalArgumentException con el motivo si no es válido. */
    public static CustomDesign fromJson(JsonObject j) {
        try {
            CustomDesign d = new CustomDesign();
            d.id = str(j, "id", null);
            if (d.id == null || d.id.isBlank()) throw new IllegalArgumentException("El diseño no tiene id.");
            d.name = str(j, "name", "Mi mueble");
            String category = str(j, "category", Catalog.MY_DESIGNS);
            d.category = Catalog.CATEGORIES.contains(category) ? category : Catalog.MY_DESIGNS;
            d.color = str(j, "color", d.color);
            d.color2 = str(j, "color2", d.color2);
            Finish f = Finish.fromId(str(j, "finish", ""));
            if (f != null) d.finish = f;
            JsonElement pieces = j.get("pieces");
            if (pieces == null || !pieces.isJsonArray()) throw new IllegalArgumentException("El diseño no tiene piezas.");
            for (JsonElement e : pieces.getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                Piece p = new Piece();
                p.name = str(o, "name", null);
                p.shape = Shape.fromId(str(o, "shape", "caja"));
                p.x = num(o, "x");
                p.y = num(o, "y");
                p.z = num(o, "z");
                p.width = num(o, "width");
                p.height = num(o, "height");
                p.depth = num(o, "depth");
                p.paint = Paint.fromId(str(o, "paint", "principal"));
                p.color = str(o, "color", p.color);
                p.finish = Finish.fromId(str(o, "finish", ""));
                d.pieces.add(p);
            }
            String error = d.validate();
            if (error != null) throw new IllegalArgumentException(error);
            return d;
        } catch (IllegalStateException | UnsupportedOperationException | ClassCastException e) {
            throw new IllegalArgumentException("El diseño tiene datos no válidos.", e);
        }
    }

    private static String str(JsonObject o, String key, String fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
    }

    private static double num(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsDouble() : Double.NaN;
    }
}
