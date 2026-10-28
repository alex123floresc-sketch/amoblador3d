package pe.edu.unaj.amoblador3d.scene;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import pe.edu.unaj.amoblador3d.gltf.GltfLoader;
import pe.edu.unaj.amoblador3d.gltf.ModelData;
import pe.edu.unaj.amoblador3d.gltf.ModelData.Primitive;
import pe.edu.unaj.amoblador3d.model.Finish;

/**
 * Carga de modelos glTF (con caché: cada archivo se lee una sola vez, en segundo plano)
 * y escalado a medidas reales.
 *
 * <p>Un modelo descargado de internet puede venir en centímetros, en pulgadas o con el origen
 * en cualquier lugar. Para usarlo como mueble lo "normalizamos": lo giramos para que su frente
 * mire a +Z, medimos su caja envolvente, lo escalamos en cada eje para que mida EXACTAMENTE
 * ancho x alto x profundidad y lo movemos para que el centro de su base quede en el origen.
 * Así cumple la misma regla que los muebles paramétricos y las colisiones son exactas.
 */
public final class ModelLibrary {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "carga-modelos");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, CompletableFuture<ModelData>> cache = new HashMap<>();
    /** Mallas de JavaFX de cada modelo (se comparten entre todas las copias del mueble). */
    private final Map<ModelData, List<Part>> parts = new IdentityHashMap<>();
    /** Variantes teñidas o con otro acabado de cada material ("#rrggbb|satinado" -> material). */
    private final Map<PhongMaterial, Map<String, PhongMaterial>> styled = new IdentityHashMap<>();

    private record Part(TriangleMesh mesh, PhongMaterial material) {}

    /** Carga un modelo (con caché). Si falla, se puede reintentar más tarde. */
    public synchronized CompletableFuture<ModelData> load(URL url) {
        String key = url.toString();
        CompletableFuture<ModelData> f = cache.get(key);
        if (f == null || f.isCompletedExceptionally()) {
            f = CompletableFuture.supplyAsync(() -> {
                try {
                    return GltfLoader.load(url);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }, executor);
            cache.put(key, f);
        }
        return f;
    }

    /** Olvida los modelos cargados (al recargar el catálogo). */
    public synchronized void clear() {
        cache.clear();
        parts.clear();
        styled.clear();
    }

    /**
     * Crea una copia del modelo escalada a las medidas pedidas (debe llamarse en el hilo de JavaFX).
     *
     * @param rotationDeg giro para que el frente del modelo mire a +Z
     * @param tint        color con que se tiñe el modelo, o null para sus colores originales
     * @param finish      acabado, o null para el brillo original
     */
    Group fit(ModelData data, double width, double depth, double height, double rotationDeg, String tint, Finish finish) {
        Group meshes = new Group();
        for (Part p : parts.computeIfAbsent(data, ModelLibrary::createParts)) {
            MeshView view = new MeshView(p.mesh);
            view.setMaterial(styled(p.material, tint, finish));
            // Con normales propias, NONE dibuja bien sin importar el orden de los vértices.
            view.setCullFace(CullFace.NONE);
            meshes.getChildren().add(view);
        }
        ModelData.Box box = data.bounds(rotationDeg);
        double safe = 1e-4; // evita dividir entre 0 en modelos planos
        Group fitted = new Group(meshes);
        fitted.getTransforms().addAll(
                new Scale(width / Math.max(box.sizeX(), safe), height / Math.max(box.sizeY(), safe), depth / Math.max(box.sizeZ(), safe)),
                new Translate(-(box.minX() + box.maxX()) / 2, -box.minY(), -(box.minZ() + box.maxZ()) / 2),
                new Rotate(rotationDeg, Rotate.Y_AXIS));
        return fitted;
    }

    /**
     * El material teñido: si tiene textura, el tinte la multiplica; si no, se usa el tinte con el
     * brillo de la pieza original (así las partes oscuras, como patas, siguen viéndose más oscuras).
     */
    private PhongMaterial styled(PhongMaterial base, String tint, Finish finish) {
        if (tint == null && finish == null) return base;
        return styled.computeIfAbsent(base, k -> new java.util.HashMap<>()).computeIfAbsent(tint + "|" + finish, k -> {
            PhongMaterial m = new PhongMaterial(base.getDiffuseColor());
            m.setDiffuseMap(base.getDiffuseMap());
            m.setSpecularColor(base.getSpecularColor());
            m.setSpecularPower(base.getSpecularPower());
            if (tint != null) {
                Color t = Color.web(tint), c = base.getDiffuseColor();
                if (base.getDiffuseMap() != null) {
                    m.setDiffuseColor(t);
                } else {
                    double lum = 0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue();
                    double k2 = Math.min(1, Math.max(0.3, lum / 0.6));
                    m.setDiffuseColor(Color.color(t.getRed() * k2, t.getGreen() * k2, t.getBlue() * k2, c.getOpacity()));
                }
            }
            if (finish != null) {
                m.setSpecularColor(Color.gray(finish.specular()));
                m.setSpecularPower(finish.power());
            }
            return m;
        });
    }

    private static List<Part> createParts(ModelData data) {
        List<Part> result = new ArrayList<>();
        Map<ModelData.Material, PhongMaterial> materials = new IdentityHashMap<>();
        for (Primitive p : data.primitives()) {
            boolean hasNormals = p.normals() != null;
            TriangleMesh mesh = new TriangleMesh(hasNormals ? VertexFormat.POINT_NORMAL_TEXCOORD : VertexFormat.POINT_TEXCOORD);
            mesh.getPoints().setAll(p.positions());
            if (hasNormals) mesh.getNormals().setAll(p.normals());
            mesh.getTexCoords().setAll(p.uvs() != null ? p.uvs() : new float[] {0, 0});
            int stride = hasNormals ? 3 : 2;
            int[] faces = new int[p.indices().length * stride];
            for (int i = 0; i < p.indices().length; i++) {
                int v = p.indices()[i];
                faces[i * stride] = v; // punto
                if (hasNormals) faces[i * stride + 1] = v; // normal
                faces[i * stride + stride - 1] = p.uvs() != null ? v : 0; // coordenada de textura
            }
            mesh.getFaces().setAll(faces);
            result.add(new Part(mesh, materials.computeIfAbsent(p.material(), ModelLibrary::material)));
        }
        return result;
    }

    private static PhongMaterial material(ModelData.Material m) {
        double[] c = m.color();
        PhongMaterial phong = new PhongMaterial(Color.color(toSrgb(c[0]), toSrgb(c[1]), toSrgb(c[2]), clamp(c[3])));
        if (m.image() != null) {
            Image img = new Image(new ByteArrayInputStream(m.image()));
            if (!img.isError()) phong.setDiffuseMap(img);
        }
        return phong;
    }

    /** glTF guarda los colores en espacio lineal; la pantalla espera sRGB. */
    private static double toSrgb(double linear) {
        double c = clamp(linear);
        return c <= 0.0031308 ? 12.92 * c : 1.055 * Math.pow(c, 1 / 2.4) - 0.055;
    }

    private static double clamp(double v) {
        return Math.min(Math.max(v, 0), 1);
    }
}
