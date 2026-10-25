package pe.edu.unaj.amoblador3d.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pe.edu.unaj.amoblador3d.gltf.ModelData.Material;
import pe.edu.unaj.amoblador3d.gltf.ModelData.Primitive;

/**
 * Lector de modelos glTF 2.0 (.glb binario y .gltf con archivos .bin y texturas).
 *
 * <p>Soporta lo que usan los muebles: mallas de triángulos con posiciones, normales,
 * coordenadas de textura, color base y textura de color. No soporta animaciones,
 * accesores "sparse" ni modelos comprimidos con Draco/Meshopt.
 */
public final class GltfLoader {
    private static final int GLB_MAGIC = 0x46546C67; // "glTF"
    private static final int CHUNK_JSON = 0x4E4F534A;
    private static final int CHUNK_BIN = 0x004E4942;

    private GltfLoader() {}

    public static ModelData load(URL url) throws IOException {
        byte[] bytes;
        try (InputStream in = url.openStream()) {
            bytes = in.readAllBytes();
        }
        return parse(bytes, url);
    }

    /** @param base ubicación del archivo, para resolver rutas relativas (.bin, texturas) */
    public static ModelData parse(byte[] bytes, URL base) throws IOException {
        ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        try {
            if (bytes.length >= 12 && b.getInt(0) == GLB_MAGIC) {
                if (b.getInt(4) != 2) throw new IOException("Solo se admite glTF 2.0.");
                int length = Math.min(b.getInt(8), bytes.length);
                JsonObject json = null;
                byte[] bin = null;
                int pos = 12;
                while (pos + 8 <= length) {
                    int chunkLength = b.getInt(pos), type = b.getInt(pos + 4), start = pos + 8;
                    if (chunkLength < 0 || start + chunkLength > length) throw new IOException("El archivo .glb está incompleto.");
                    if (type == CHUNK_JSON && json == null) {
                        json = JsonParser.parseString(new String(bytes, start, chunkLength, StandardCharsets.UTF_8)).getAsJsonObject();
                    } else if (type == CHUNK_BIN && bin == null) {
                        bin = Arrays.copyOfRange(bytes, start, start + chunkLength);
                    }
                    pos = start + ((chunkLength + 3) & ~3);
                }
                if (json == null) throw new IOException("El archivo .glb no tiene datos JSON.");
                return new Reader(json, bin, base).read();
            }
            return new Reader(JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject(), null, base).read();
        } catch (RuntimeException e) {
            // JSON mal formado, índices fuera de rango, tipos equivocados...
            throw new IOException("El archivo no es un modelo glTF válido (" + e.getMessage() + ").", e);
        }
    }

    /** Lee el JSON de un glTF ya separado de su parte binaria. */
    private static final class Reader {
        private final JsonObject json;
        private final byte[] glbBin;
        private final URL base;
        private final List<byte[]> buffers = new ArrayList<>();
        private final Map<Integer, Material> materials = new HashMap<>();
        private final List<Primitive> primitives = new ArrayList<>();

        Reader(JsonObject json, byte[] glbBin, URL base) {
            this.json = json;
            this.glbBin = glbBin;
            this.base = base;
        }

        ModelData read() throws IOException {
            JsonObject asset = json.has("asset") ? json.getAsJsonObject("asset") : new JsonObject();
            String version = asset.has("version") ? asset.get("version").getAsString() : "";
            if (!version.startsWith("2")) throw new IOException("Solo se admite glTF 2.0.");
            JsonArray required = array(json, "extensionsRequired");
            if (!required.isEmpty()) {
                throw new IOException("El modelo usa extensiones no soportadas (" + required
                        + "). No se admiten modelos comprimidos con Draco/Meshopt.");
            }
            for (JsonElement buffer : array(json, "buffers")) buffers.add(loadBuffer(buffer.getAsJsonObject()));

            JsonArray nodes = array(json, "nodes");
            for (int root : rootNodes(nodes)) visit(nodes, root, identity(), 0);
            if (primitives.isEmpty()) throw new IOException("El modelo no tiene geometría de triángulos.");
            return new ModelData(primitives);
        }

        private List<Integer> rootNodes(JsonArray nodes) {
            List<Integer> roots = new ArrayList<>();
            JsonArray scenes = array(json, "scenes");
            if (!scenes.isEmpty()) {
                int scene = json.has("scene") ? json.get("scene").getAsInt() : 0;
                for (JsonElement n : array(scenes.get(scene).getAsJsonObject(), "nodes")) roots.add(n.getAsInt());
                return roots;
            }
            // Sin escenas: las raíces son los nodos que no son hijos de otro.
            boolean[] child = new boolean[nodes.size()];
            for (JsonElement n : nodes) for (JsonElement c : array(n.getAsJsonObject(), "children")) child[c.getAsInt()] = true;
            for (int i = 0; i < nodes.size(); i++) if (!child[i]) roots.add(i);
            return roots;
        }

        private void visit(JsonArray nodes, int index, double[] parent, int depth) throws IOException {
            if (depth > 64) throw new IOException("La jerarquía de nodos es demasiado profunda (¿ciclo?).");
            JsonObject node = nodes.get(index).getAsJsonObject();
            double[] world = multiply(parent, localMatrix(node));
            if (node.has("mesh")) {
                JsonObject mesh = array(json, "meshes").get(node.get("mesh").getAsInt()).getAsJsonObject();
                for (JsonElement p : array(mesh, "primitives")) addPrimitive(p.getAsJsonObject(), world);
            }
            for (JsonElement c : array(node, "children")) visit(nodes, c.getAsInt(), world, depth + 1);
        }

        private void addPrimitive(JsonObject p, double[] m) throws IOException {
            int mode = p.has("mode") ? p.get("mode").getAsInt() : 4;
            if (mode != 4) return; // solo triángulos (se ignoran líneas y puntos)
            JsonObject attributes = p.getAsJsonObject("attributes");
            if (!attributes.has("POSITION")) return;

            float[] positions = readFloats(attributes.get("POSITION").getAsInt(), 3);
            float[] normals = attributes.has("NORMAL") ? readFloats(attributes.get("NORMAL").getAsInt(), 3) : null;
            float[] uvs = attributes.has("TEXCOORD_0") ? readFloats(attributes.get("TEXCOORD_0").getAsInt(), 2) : null;
            int vertices = positions.length / 3;
            int[] indices;
            if (p.has("indices")) {
                indices = readIndices(p.get("indices").getAsInt());
            } else {
                indices = new int[vertices];
                for (int i = 0; i < vertices; i++) indices[i] = i;
            }
            indices = Arrays.copyOf(indices, indices.length - indices.length % 3);
            for (int i : indices) if (i < 0 || i >= vertices) throw new IOException("Índice de vértice fuera de rango.");
            if (normals != null && normals.length != positions.length) normals = null;
            if (uvs != null && uvs.length / 2 != vertices) uvs = null;

            // Aplicar la transformación del nodo a posiciones y normales.
            double[] n = normalMatrix(m);
            for (int i = 0; i < positions.length; i += 3) {
                double x = positions[i], y = positions[i + 1], z = positions[i + 2];
                positions[i] = (float) (m[0] * x + m[4] * y + m[8] * z + m[12]);
                positions[i + 1] = (float) (m[1] * x + m[5] * y + m[9] * z + m[13]);
                positions[i + 2] = (float) (m[2] * x + m[6] * y + m[10] * z + m[14]);
                if (normals != null) {
                    double nx = normals[i], ny = normals[i + 1], nz = normals[i + 2];
                    double tx = n[0] * nx + n[1] * ny + n[2] * nz;
                    double ty = n[3] * nx + n[4] * ny + n[5] * nz;
                    double tz = n[6] * nx + n[7] * ny + n[8] * nz;
                    double len = Math.sqrt(tx * tx + ty * ty + tz * tz);
                    if (len > 0) {
                        normals[i] = (float) (tx / len);
                        normals[i + 1] = (float) (ty / len);
                        normals[i + 2] = (float) (tz / len);
                    }
                }
            }
            // Una escala negativa (espejo) invierte el orden de los vértices: lo corregimos.
            if (n[9] < 0) {
                for (int i = 0; i < indices.length; i += 3) {
                    int t = indices[i + 1];
                    indices[i + 1] = indices[i + 2];
                    indices[i + 2] = t;
                }
            }
            int material = p.has("material") ? p.get("material").getAsInt() : -1;
            primitives.add(new Primitive(positions, normals, uvs, indices, material(material)));
        }

        private Material material(int index) throws IOException {
            if (index < 0) return Material.DEFAULT;
            Material cached = materials.get(index);
            if (cached != null) return cached;

            JsonObject mat = array(json, "materials").get(index).getAsJsonObject();
            JsonObject pbr = mat.has("pbrMetallicRoughness") ? mat.getAsJsonObject("pbrMetallicRoughness") : new JsonObject();
            double[] color = {1, 1, 1, 1};
            JsonArray factor = array(pbr, "baseColorFactor");
            for (int i = 0; i < Math.min(4, factor.size()); i++) color[i] = factor.get(i).getAsDouble();
            boolean blend = mat.has("alphaMode") && mat.get("alphaMode").getAsString().equals("BLEND");
            if (!blend) color[3] = 1;
            byte[] image = null;
            if (pbr.has("baseColorTexture")) {
                JsonObject tex = pbr.getAsJsonObject("baseColorTexture");
                int texCoord = tex.has("texCoord") ? tex.get("texCoord").getAsInt() : 0;
                if (texCoord == 0) image = textureImage(tex.get("index").getAsInt());
            }
            boolean doubleSided = mat.has("doubleSided") && mat.get("doubleSided").getAsBoolean();
            Material result = new Material(color, image, doubleSided);
            materials.put(index, result);
            return result;
        }

        /** Bytes de la imagen de una textura, o null si JavaFX no puede mostrarla (p. ej. WebP/KTX2). */
        private byte[] textureImage(int textureIndex) throws IOException {
            JsonObject texture = array(json, "textures").get(textureIndex).getAsJsonObject();
            if (!texture.has("source")) return null;
            JsonObject img = array(json, "images").get(texture.get("source").getAsInt()).getAsJsonObject();
            String mime = img.has("mimeType") ? img.get("mimeType").getAsString() : "";
            byte[] data;
            if (img.has("bufferView")) {
                data = bufferViewBytes(img.get("bufferView").getAsInt());
            } else if (img.has("uri")) {
                data = loadUri(img.get("uri").getAsString());
            } else {
                return null;
            }
            boolean png = data.length > 4 && (data[0] & 0xff) == 0x89 && data[1] == 'P';
            boolean jpeg = data.length > 3 && (data[0] & 0xff) == 0xFF && (data[1] & 0xff) == 0xD8;
            return png || jpeg || mime.equals("image/png") || mime.equals("image/jpeg") ? data : null;
        }

        private byte[] loadBuffer(JsonObject buffer) throws IOException {
            if (!buffer.has("uri")) {
                if (glbBin == null) throw new IOException("Falta la parte binaria del modelo.");
                return glbBin;
            }
            return loadUri(buffer.get("uri").getAsString());
        }

        private byte[] loadUri(String uri) throws IOException {
            if (uri.startsWith("data:")) {
                int comma = uri.indexOf(',');
                return Base64.getDecoder().decode(uri.substring(comma + 1));
            }
            if (base == null) throw new IOException("No se puede leer el archivo externo " + uri + ".");
            try {
                URL url = base.toURI().resolve(new URI(null, null, uri, null)).toURL();
                try (InputStream in = url.openStream()) {
                    return in.readAllBytes();
                }
            } catch (URISyntaxException | IllegalArgumentException e) {
                throw new IOException("Ruta no válida en el modelo: " + uri, e);
            } catch (IOException e) {
                throw new IOException("No se encontró el archivo " + uri + " del modelo.", e);
            }
        }

        private byte[] bufferViewBytes(int index) {
            JsonObject bv = array(json, "bufferViews").get(index).getAsJsonObject();
            byte[] buffer = buffers.get(bv.get("buffer").getAsInt());
            int offset = bv.has("byteOffset") ? bv.get("byteOffset").getAsInt() : 0;
            return Arrays.copyOfRange(buffer, offset, offset + bv.get("byteLength").getAsInt());
        }

        private float[] readFloats(int accessorIndex, int components) throws IOException {
            JsonObject acc = array(json, "accessors").get(accessorIndex).getAsJsonObject();
            if (acc.has("sparse")) throw new IOException("Los accesores \"sparse\" no se admiten.");
            int count = acc.get("count").getAsInt();
            int n = componentsOf(acc.get("type").getAsString());
            if (n != components) throw new IOException("Tipo de dato inesperado en el modelo.");
            float[] out = new float[count * n];
            if (!acc.has("bufferView")) return out; // sin datos = ceros
            int type = acc.get("componentType").getAsInt();
            boolean normalized = acc.has("normalized") && acc.get("normalized").getAsBoolean();
            View v = view(acc, n, sizeOf(type), count);
            for (int i = 0; i < count; i++) {
                for (int c = 0; c < n; c++) {
                    int pos = v.offset + i * v.stride + c * sizeOf(type);
                    out[i * n + c] = switch (type) {
                        case 5126 -> v.buffer.getFloat(pos);
                        case 5121 -> norm(v.buffer.get(pos) & 0xff, 255, normalized);
                        case 5120 -> norm(v.buffer.get(pos), 127, normalized);
                        case 5123 -> norm(v.buffer.getShort(pos) & 0xffff, 65535, normalized);
                        case 5122 -> norm(v.buffer.getShort(pos), 32767, normalized);
                        default -> throw new IOException("Tipo de componente no soportado: " + type);
                    };
                }
            }
            return out;
        }

        private static float norm(int value, int max, boolean normalized) {
            return normalized ? Math.max(value / (float) max, -1f) : value;
        }

        private int[] readIndices(int accessorIndex) throws IOException {
            JsonObject acc = array(json, "accessors").get(accessorIndex).getAsJsonObject();
            if (acc.has("sparse")) throw new IOException("Los accesores \"sparse\" no se admiten.");
            int count = acc.get("count").getAsInt();
            int type = acc.get("componentType").getAsInt();
            int[] out = new int[count];
            if (!acc.has("bufferView")) return out;
            View v = view(acc, 1, sizeOf(type), count);
            for (int i = 0; i < count; i++) {
                int pos = v.offset + i * v.stride;
                out[i] = switch (type) {
                    case 5121 -> v.buffer.get(pos) & 0xff;
                    case 5123 -> v.buffer.getShort(pos) & 0xffff;
                    case 5125 -> v.buffer.getInt(pos);
                    default -> throw new IOException("Tipo de índice no soportado: " + type);
                };
            }
            return out;
        }

        private record View(ByteBuffer buffer, int offset, int stride) {}

        private View view(JsonObject acc, int components, int componentSize, int count) throws IOException {
            JsonObject bv = array(json, "bufferViews").get(acc.get("bufferView").getAsInt()).getAsJsonObject();
            byte[] buffer = buffers.get(bv.get("buffer").getAsInt());
            int viewOffset = bv.has("byteOffset") ? bv.get("byteOffset").getAsInt() : 0;
            int viewLength = bv.get("byteLength").getAsInt();
            int offset = viewOffset + (acc.has("byteOffset") ? acc.get("byteOffset").getAsInt() : 0);
            int element = components * componentSize;
            int stride = bv.has("byteStride") ? bv.get("byteStride").getAsInt() : element;
            long end = offset + (long) stride * Math.max(count - 1, 0) + element;
            if (count > 0 && (end > viewOffset + (long) viewLength || end > buffer.length)) {
                throw new IOException("Los datos del modelo están incompletos.");
            }
            return new View(ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN), offset, stride);
        }

        private static int componentsOf(String type) throws IOException {
            return switch (type) {
                case "SCALAR" -> 1;
                case "VEC2" -> 2;
                case "VEC3" -> 3;
                case "VEC4" -> 4;
                default -> throw new IOException("Tipo de accesor no soportado: " + type);
            };
        }

        private static int sizeOf(int componentType) throws IOException {
            return switch (componentType) {
                case 5120, 5121 -> 1;
                case 5122, 5123 -> 2;
                case 5125, 5126 -> 4;
                default -> throw new IOException("Tipo de componente no soportado: " + componentType);
            };
        }
    }

    private static JsonArray array(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : new JsonArray();
    }

    // ---------------------------------------------------------------- Matrices 4x4 (por columnas, como glTF)

    static double[] identity() {
        return new double[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    }

    static double[] multiply(double[] a, double[] b) {
        double[] r = new double[16];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                double sum = 0;
                for (int k = 0; k < 4; k++) sum += a[k * 4 + row] * b[col * 4 + k];
                r[col * 4 + row] = sum;
            }
        }
        return r;
    }

    /** Matriz del nodo: "matrix" o traslación + rotación (cuaternión) + escala. */
    static double[] localMatrix(JsonObject node) {
        JsonArray matrix = array(node, "matrix");
        if (matrix.size() == 16) {
            double[] m = new double[16];
            for (int i = 0; i < 16; i++) m[i] = matrix.get(i).getAsDouble();
            return m;
        }
        double[] t = vector(node, "translation", 0, 0, 0);
        double[] q = vector(node, "rotation", 0, 0, 0, 1);
        double[] s = vector(node, "scale", 1, 1, 1);
        double x = q[0], y = q[1], z = q[2], w = q[3];
        return new double[] {
            (1 - 2 * (y * y + z * z)) * s[0], 2 * (x * y + z * w) * s[0], 2 * (x * z - y * w) * s[0], 0,
            2 * (x * y - z * w) * s[1], (1 - 2 * (x * x + z * z)) * s[1], 2 * (y * z + x * w) * s[1], 0,
            2 * (x * z + y * w) * s[2], 2 * (y * z - x * w) * s[2], (1 - 2 * (x * x + y * y)) * s[2], 0,
            t[0], t[1], t[2], 1,
        };
    }

    private static double[] vector(JsonObject node, String key, double... fallback) {
        JsonArray a = array(node, key);
        if (a.size() != fallback.length) return fallback;
        double[] v = new double[a.size()];
        for (int i = 0; i < v.length; i++) v[i] = a.get(i).getAsDouble();
        return v;
    }

    /**
     * Matriz para transformar normales (inversa transpuesta de la parte 3x3), por filas,
     * con el signo del determinante en la posición 9 (negativo = la malla está espejada).
     */
    static double[] normalMatrix(double[] m) {
        double a00 = m[0], a01 = m[4], a02 = m[8];
        double a10 = m[1], a11 = m[5], a12 = m[9];
        double a20 = m[2], a21 = m[6], a22 = m[10];
        double c00 = a11 * a22 - a12 * a21, c01 = -(a10 * a22 - a12 * a20), c02 = a10 * a21 - a11 * a20;
        double c10 = -(a01 * a22 - a02 * a21), c11 = a00 * a22 - a02 * a20, c12 = -(a00 * a21 - a01 * a20);
        double c20 = a01 * a12 - a02 * a11, c21 = -(a00 * a12 - a02 * a10), c22 = a00 * a11 - a01 * a10;
        double det = a00 * c00 + a01 * c01 + a02 * c02;
        double sign = det < 0 ? -1 : 1;
        return new double[] {
            c00 * sign, c01 * sign, c02 * sign,
            c10 * sign, c11 * sign, c12 * sign,
            c20 * sign, c21 * sign, c22 * sign,
            sign,
        };
    }
}
