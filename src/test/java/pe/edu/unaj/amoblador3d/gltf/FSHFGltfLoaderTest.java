package pe.edu.unaj.amoblador3d.gltf;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FSHFGltfLoaderTest {
    private static final double EPS = 1e-6;

    private static final float[] TRIANGLE = {0, 0, 0, 1, 0, 0, 0, 1, 0};

    @Test
    void leeUnGltfConDatosIncrustados() throws IOException {
        FSHFModelData m = FSHFGltfLoader.parse(gltf(dataUri(floats(TRIANGLE)), "", ""), null);

        assertEquals(1, m.primitives().size());
        FSHFModelData.Primitive p = m.primitives().get(0);
        assertArrayEquals(TRIANGLE, p.positions(), 1e-6f);
        assertArrayEquals(new int[] {0, 1, 2}, p.indices(), "sin índices: un vértice tras otro");
        assertNull(p.normals());
        assertSame(FSHFModelData.Material.DEFAULT, p.material());
    }

    @Test
    void leeUnGlbBinarioConIndices() throws IOException {
        float[] square = {0, 0, 0, 2, 0, 0, 2, 1, 0, 0, 1, 0};
        byte[] positions = floats(square);
        byte[] indices = shorts(0, 1, 2, 0, 2, 3);
        byte[] bin = concat(positions, indices);
        String json = """
            {"asset":{"version":"2.0"},
             "buffers":[{"byteLength":%d}],
             "bufferViews":[{"buffer":0,"byteOffset":0,"byteLength":%d},
                            {"buffer":0,"byteOffset":%d,"byteLength":%d}],
             "accessors":[{"bufferView":0,"componentType":5126,"count":4,"type":"VEC3"},
                          {"bufferView":1,"componentType":5123,"count":6,"type":"SCALAR"}],
             "meshes":[{"primitives":[{"attributes":{"POSITION":0},"indices":1}]}],
             "nodes":[{"mesh":0}], "scenes":[{"nodes":[0]}], "scene":0}
            """.formatted(bin.length, positions.length, positions.length, indices.length);

        FSHFModelData m = FSHFGltfLoader.parse(glb(json, bin), null);

        FSHFModelData.Primitive p = m.primitives().get(0);
        assertArrayEquals(new int[] {0, 1, 2, 0, 2, 3}, p.indices());
        FSHFModelData.Box box = m.bounds(0);
        assertEquals(2, box.sizeX(), EPS);
        assertEquals(1, box.sizeY(), EPS);
        assertEquals(0, box.sizeZ(), EPS);
    }

    @Test
    void seAplicaLaTransformacionDeLosNodos() throws IOException {
        String nodes = """
            "nodes":[{"translation":[5,0,0],"children":[1]},{"mesh":0,"scale":[2,2,2]}],
            """;
        FSHFModelData m = FSHFGltfLoader.parse(gltf(dataUri(floats(TRIANGLE)), nodes, ""), null);

        float[] v = m.primitives().get(0).positions();
        assertArrayEquals(new float[] {5, 0, 0, 7, 0, 0, 5, 2, 0}, v, 1e-5f);
    }

    @Test
    void unaEscalaNegativaInvierteElOrdenDeLosTriangulos() throws IOException {
        String nodes = "\"nodes\":[{\"mesh\":0,\"scale\":[-1,1,1]}],";
        FSHFModelData m = FSHFGltfLoader.parse(gltf(dataUri(floats(TRIANGLE)), nodes, ""), null);

        assertArrayEquals(new int[] {0, 2, 1}, m.primitives().get(0).indices());
    }

    @Test
    void elColorDelMaterialSeLeeYLaTransparenciaSoloConBlend() throws IOException {
        String opaque = "\"materials\":[{\"pbrMetallicRoughness\":{\"baseColorFactor\":[0.5,0.25,0.1,0.3]}}],";
        FSHFModelData.Material m = FSHFGltfLoader.parse(gltf(dataUri(floats(TRIANGLE)), "", opaque, 0), null)
                .primitives().get(0).material();
        assertArrayEquals(new double[] {0.5, 0.25, 0.1, 1}, m.color(), EPS);

        String blend = "\"materials\":[{\"alphaMode\":\"BLEND\",\"doubleSided\":true,"
                + "\"pbrMetallicRoughness\":{\"baseColorFactor\":[1,1,1,0.3]}}],";
        FSHFModelData.Material t = FSHFGltfLoader.parse(gltf(dataUri(floats(TRIANGLE)), "", blend, 0), null)
                .primitives().get(0).material();
        assertEquals(0.3, t.color()[3], EPS);
        assertTrue(t.doubleSided());
    }

    @Test
    void laCajaEnvolventeGiradaIntercambiaAnchoYProfundidad() throws IOException {
        float[] bar = {0, 0, 0, 3, 0, 0, 3, 1, 0.5f};
        FSHFModelData m = FSHFGltfLoader.parse(gltf(dataUri(floats(bar)), "", ""), null);

        FSHFModelData.Box straight = m.bounds(0), turned = m.bounds(90);
        assertEquals(3, straight.sizeX(), EPS);
        assertEquals(0.5, straight.sizeZ(), EPS);
        assertEquals(0.5, turned.sizeX(), 1e-5);
        assertEquals(3, turned.sizeZ(), 1e-5);
    }

    @Test
    void elArchivoBinExternoSeBuscaJuntoAlModelo(@TempDir Path dir) throws IOException {
        Files.write(dir.resolve("geometria.bin"), floats(TRIANGLE));
        Path model = dir.resolve("mueble.gltf");
        Files.write(model, gltf("geometria.bin", "", ""));

        FSHFModelData m = FSHFGltfLoader.load(model.toUri().toURL());
        assertArrayEquals(TRIANGLE, m.primitives().get(0).positions(), 1e-6f);
    }

    @Test
    void losModelosComprimidosConDracoSeRechazanConUnMensajeClaro() {
        String json = "{\"asset\":{\"version\":\"2.0\"},\"extensionsRequired\":[\"KHR_draco_mesh_compression\"]}";
        IOException e = assertThrows(IOException.class,
                () -> FSHFGltfLoader.parse(json.getBytes(StandardCharsets.UTF_8), null));
        assertTrue(e.getMessage().contains("Draco"), e.getMessage());
    }

    @Test
    void soloSeAdmiteGltf2() {
        String json = "{\"asset\":{\"version\":\"1.0\"}}";
        IOException e = assertThrows(IOException.class,
                () -> FSHFGltfLoader.parse(json.getBytes(StandardCharsets.UTF_8), null));
        assertTrue(e.getMessage().contains("2.0"), e.getMessage());
    }

    @Test
    void unArchivoQueNoEsUnModeloDaUnErrorClaro() {
        byte[] garbage = "esto no es un modelo".getBytes(StandardCharsets.UTF_8);
        IOException e = assertThrows(IOException.class, () -> FSHFGltfLoader.parse(garbage, null));
        assertTrue(e.getMessage().startsWith("El archivo no es un modelo glTF válido"), e.getMessage());
    }

    @Test
    void unGlbCortadoSeDetecta() throws IOException {
        byte[] full = glb("{\"asset\":{\"version\":\"2.0\"}}", floats(TRIANGLE));
        byte[] cut = java.util.Arrays.copyOf(full, full.length - 8);
        ByteBuffer.wrap(cut).order(ByteOrder.LITTLE_ENDIAN).putInt(8, full.length);
        IOException e = assertThrows(IOException.class, () -> FSHFGltfLoader.parse(cut, null));
        assertTrue(e.getMessage().contains("incompleto"), e.getMessage());
    }

    @Test
    void unIndiceFueraDeRangoSeRechaza() {
        byte[] positions = floats(TRIANGLE);
        byte[] indices = shorts(0, 1, 7);
        String json = """
            {"asset":{"version":"2.0"},
             "buffers":[{"uri":"%s","byteLength":%d}],
             "bufferViews":[{"buffer":0,"byteLength":%d},{"buffer":0,"byteOffset":%d,"byteLength":%d}],
             "accessors":[{"bufferView":0,"componentType":5126,"count":3,"type":"VEC3"},
                          {"bufferView":1,"componentType":5123,"count":3,"type":"SCALAR"}],
             "meshes":[{"primitives":[{"attributes":{"POSITION":0},"indices":1}]}],
             "nodes":[{"mesh":0}]}
            """.formatted(dataUri(concat(positions, indices)), positions.length + indices.length,
                positions.length, positions.length, indices.length);
        IOException e = assertThrows(IOException.class,
                () -> FSHFGltfLoader.parse(json.getBytes(StandardCharsets.UTF_8), null));
        assertTrue(e.getMessage().contains("fuera de rango"), e.getMessage());
    }

    @Test
    void unModeloSinTriangulosSeRechaza() {
        String json = "{\"asset\":{\"version\":\"2.0\"},\"nodes\":[{}]}";
        IOException e = assertThrows(IOException.class,
                () -> FSHFGltfLoader.parse(json.getBytes(StandardCharsets.UTF_8), null));
        assertTrue(e.getMessage().contains("geometría"), e.getMessage());
    }

    private static byte[] gltf(String bufferUri, String nodes, String materials) {
        return gltf(bufferUri, nodes, materials, -1);
    }

    private static byte[] gltf(String bufferUri, String nodes, String materials, int material) {
        String mat = material >= 0 ? ",\"material\":" + material : "";
        String json = """
            {"asset":{"version":"2.0"},
             "buffers":[{"uri":"%s","byteLength":36}],
             "bufferViews":[{"buffer":0,"byteLength":36}],
             "accessors":[{"bufferView":0,"componentType":5126,"count":3,"type":"VEC3"}],
             %s
             "meshes":[{"primitives":[{"attributes":{"POSITION":0}%s}]}],
             %s
             "extras":{}}
            """.formatted(bufferUri, materials, mat, nodes.isEmpty() ? "\"nodes\":[{\"mesh\":0}]," : nodes);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] glb(String json, byte[] bin) throws IOException {
        byte[] j = pad(json.getBytes(StandardCharsets.UTF_8), (byte) ' ');
        byte[] b = pad(bin, (byte) 0);
        int length = 12 + 8 + j.length + 8 + b.length;
        ByteBuffer out = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        out.putInt(0x46546C67).putInt(2).putInt(length);
        out.putInt(j.length).putInt(0x4E4F534A).put(j);
        out.putInt(b.length).putInt(0x004E4942).put(b);
        return out.array();
    }

    private static byte[] pad(byte[] data, byte filler) {
        int size = (data.length + 3) & ~3;
        byte[] out = java.util.Arrays.copyOf(data, size);
        for (int i = data.length; i < size; i++) out[i] = filler;
        return out;
    }

    private static byte[] floats(float... values) {
        ByteBuffer b = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : values) b.putFloat(v);
        return b.array();
    }

    private static byte[] shorts(int... values) {
        ByteBuffer b = ByteBuffer.allocate(values.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int v : values) b.putShort((short) v);
        return b.array();
    }

    private static byte[] concat(byte[] a, byte[] b) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(a);
        out.writeBytes(b);
        return out.toByteArray();
    }

    private static String dataUri(byte[] data) {
        return "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(data);
    }
}
