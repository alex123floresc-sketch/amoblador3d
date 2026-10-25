package pe.edu.unaj.amoblador3d.ui;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

/**
 * Convierte una imagen de JavaFX a PNG. Lo hacemos a mano (el formato es simple) para no
 * depender del módulo javafx.swing solo por {@code ImageIO}.
 */
final class Png {
    private Png() {}

    static byte[] encode(Image image) throws IOException {
        int w = (int) image.getWidth(), h = (int) image.getHeight();
        PixelReader reader = image.getPixelReader();
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (DeflaterOutputStream z = new DeflaterOutputStream(compressed)) {
            byte[] row = new byte[1 + w * 3]; // cada fila empieza con el tipo de filtro (0 = ninguno)
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = reader.getArgb(x, y);
                    row[1 + x * 3] = (byte) (argb >> 16);
                    row[2 + x * 3] = (byte) (argb >> 8);
                    row[3 + x * 3] = (byte) argb;
                }
                z.write(row);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(out);
        data.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'});
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        DataOutputStream hd = new DataOutputStream(header);
        hd.writeInt(w);
        hd.writeInt(h);
        hd.write(new byte[] {8, 2, 0, 0, 0}); // 8 bits por canal, RGB
        chunk(data, "IHDR", header.toByteArray());
        chunk(data, "IDAT", compressed.toByteArray());
        chunk(data, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void chunk(DataOutputStream out, String type, byte[] body) throws IOException {
        byte[] t = type.getBytes(StandardCharsets.US_ASCII);
        out.writeInt(body.length);
        out.write(t);
        out.write(body);
        CRC32 crc = new CRC32();
        crc.update(t);
        crc.update(body);
        out.writeInt((int) crc.getValue());
    }
}
