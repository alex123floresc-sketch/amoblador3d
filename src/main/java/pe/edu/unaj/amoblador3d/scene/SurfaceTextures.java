package pe.edu.unaj.amoblador3d.scene;

import java.util.LinkedHashMap;
import java.util.Map;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import pe.edu.unaj.amoblador3d.model.SurfaceMaterial;

/**
 * Texturas de paredes y piso dibujadas por código (no hay archivos de imagen): ladrillos,
 * tablas de madera, baldosas, vetas de mármol... teñidas con el color elegido.
 *
 * <p>Cada textura se repite sin costuras y cubre {@link #tileSize} metros, así un ladrillo o
 * una baldosa miden lo mismo en una pared de 2 m que en una de 8 m (ver {@code Meshes.box}).
 */
final class SurfaceTextures {
    /**
     * Texturas ya dibujadas, las usadas más recientemente al final. Tiene un límite porque cada
     * una pesa hasta 1 MB y el selector de color puede producir cientos de colores distintos;
     * las que la escena sigue usando no se pierden (el nodo conserva su material).
     */
    private static final int CACHE_LIMIT = 32;
    private static final Map<String, PhongMaterial> CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, PhongMaterial> eldest) {
            return size() > CACHE_LIMIT;
        }
    };

    private SurfaceTextures() {}

    /** Metros que cubre una repetición de la textura. */
    static double tileSize(SurfaceMaterial m) {
        return switch (m) {
            case PINTURA, SATINADO -> 1.0;
            case PAPEL_TAPIZ, ALFOMBRA -> 0.5;
            case LADRILLO -> 1.0;
            case PIEDRA, MADERA, MARMOL, PORCELANATO -> 1.2;
            case CERAMICO -> 0.6;
            case CONCRETO -> 2.0;
        };
    }

    static PhongMaterial material(SurfaceMaterial m, String color) {
        return CACHE.computeIfAbsent(m.id() + "|" + color, k -> create(m, Color.web(color)));
    }

    private static PhongMaterial create(SurfaceMaterial m, Color base) {
        int size = switch (m) {
            case PINTURA, SATINADO, ALFOMBRA -> 256;
            default -> 512;
        };
        WritableImage img = new WritableImage(size, size);
        PixelWriter pw = img.getPixelWriter();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                pw.setColor(x, y, paint(m, base, (x + 0.5) / size, (y + 0.5) / size));
            }
        }
        PhongMaterial phong = new PhongMaterial(Color.WHITE);
        phong.setDiffuseMap(img);
        // Brillo: las superficies pulidas reflejan las luces; el ladrillo o la alfombra, no.
        double[] spec = switch (m) {
            case SATINADO -> new double[] {0.25, 18};
            case MADERA -> new double[] {0.22, 24};
            case CERAMICO -> new double[] {0.55, 48};
            case PORCELANATO -> new double[] {0.4, 40};
            case MARMOL -> new double[] {0.7, 64};
            default -> null;
        };
        if (spec != null) {
            phong.setSpecularColor(Color.gray(spec[0]));
            phong.setSpecularPower(spec[1]);
        }
        return phong;
    }

    /** Color de la textura en (u, v) ∈ [0, 1), con u a lo largo de la superficie y v hacia abajo. */
    private static Color paint(SurfaceMaterial m, Color c, double u, double v) {
        return switch (m) {
            case PINTURA -> shade(c, 1 + 0.05 * (fbm(u, v, 16, 3, 1) - 0.5));
            case SATINADO -> shade(c, 1 + 0.03 * (fbm(u, v, 24, 2, 2) - 0.5));
            case PAPEL_TAPIZ -> wallpaper(c, u, v);
            case LADRILLO -> masonry(c, u, v, 12, 4, 0.5, 0.012, Color.web("#cfc8bc"), 3);
            case PIEDRA -> masonry(c, u, v, 6, 3, 0.37, 0.012, shade(c, 0.55), 4);
            case CONCRETO -> concrete(c, u, v);
            case MADERA -> wood(c, u, v);
            case CERAMICO -> tiles(c, u, v, 2, 0.006, shade(c, 0.78), 0.03, 5);
            case PORCELANATO -> tiles(c, u, v, 2, 0.003, shade(c, 0.7), 0.12, 6);
            case MARMOL -> marble(c, u, v);
            case ALFOMBRA -> carpet(c, u, v);
        };
    }

    // ---------------------------------------------------------------- Patrones

    private static Color wallpaper(Color c, double u, double v) {
        int stripes = 8;
        double s = u * stripes, f = s - Math.floor(s);
        boolean light = ((int) Math.floor(s)) % 2 == 0;
        double k = light ? 1.04 : 0.9;
        if (f < 0.03 || f > 0.97) k = 0.82; // filete entre franjas
        // Pequeños rombos en las franjas claras.
        if (light) {
            double du = Math.abs(f - 0.5) * 2, dv = Math.abs(frac(v * 16) - 0.5) * 2;
            if (du + dv < 0.35) k = 0.95;
        }
        return shade(c, k * (1 + 0.04 * (fbm(u, v, 32, 2, 7) - 0.5)));
    }

    /**
     * Ladrillos o bloques de piedra: {@code rows} hileras de {@code cols} piezas,
     * cada hilera desplazada {@code stagger}, separadas por juntas de mortero.
     */
    private static Color masonry(Color c, double u, double v, int rows, int cols, double stagger,
                                 double joint, Color mortar, int seed) {
        double rv = v * rows;
        int row = (int) Math.floor(rv);
        double fv = rv - row;
        double shift = (seed == 4 ? hash(row, 0, seed) : row % 2 * stagger);
        double ru = (u + shift) * cols;
        int col = (int) Math.floor(ru);
        double fu = ru - col;
        col = Math.floorMod(col, cols);
        // Ancho de la junta en cada dirección (las piezas son más anchas que altas).
        double ju = joint * cols, jv = joint * rows;
        double edge = Math.min(Math.min(fu, 1 - fu) / ju, Math.min(fv, 1 - fv) / jv);
        double grain = fbm(u, v, 64, 3, seed) - 0.5;
        if (edge < 1) return shade(mortar, 1 + 0.25 * grain);
        double tone = 0.8 + 0.35 * hash(col, row, seed);
        double k = tone * (1 + 0.35 * grain) * (edge < 2.2 ? 0.88 + 0.12 * (edge - 1) / 1.2 : 1);
        if (seed == 4) k *= 1 + 0.25 * (fbm(u, v, 8, 3, seed + 1) - 0.5);
        return shade(c, k);
    }

    private static Color concrete(Color c, double u, double v) {
        double blotch = fbm(u, v, 4, 4, 11) - 0.5;
        double speck = hash((int) (u * 512), (int) (v * 512), 12) > 0.985 ? -0.18 : 0;
        return shade(c, 1 + 0.22 * blotch + 0.06 * (fbm(u, v, 64, 2, 13) - 0.5) + speck);
    }

    /** Tablas de 15 cm a lo largo de u, con uniones escalonadas, vetas y un tono distinto en cada tabla. */
    private static Color wood(Color c, double u, double v) {
        int planks = 8;
        double pv = v * planks;
        int row = (int) Math.floor(pv);
        double fv = pv - row;
        double pu = u + hash(row, 0, 21); // cada hilera empieza en otro punto
        int piece = (int) Math.floor(pu);
        double fu = pu - piece;
        if (fv < 0.025 || fv > 0.975 || fu < 0.004 || fu > 0.996) return shade(c, 0.5);
        double tone = 0.82 + 0.3 * hash(row, piece, 22);
        double warp = fbm(u, v, 4, 3, 23 + row);
        double rings = Math.sin((fv * 3 + warp * 2.5 + hash(row, piece, 24) * 5) * Math.PI * 2);
        double fine = fbm(u * 0.25, v * 4, 64, 2, 25) - 0.5;
        return shade(c, tone * (1 + 0.1 * rings + 0.12 * fine));
    }

    /** Baldosas cuadradas ({@code n} x {@code n} por textura) con junta. */
    private static Color tiles(Color c, double u, double v, int n, double joint, Color grout, double veining, int seed) {
        double tu = u * n, tv = v * n;
        int iu = (int) Math.floor(tu), iv = (int) Math.floor(tv);
        double fu = tu - iu, fv = tv - iv, j = joint * n;
        if (fu < j || fu > 1 - j || fv < j || fv > 1 - j) return grout;
        double tone = 0.985 + 0.03 * hash(iu, iv, seed);
        double k = tone * (1 + veining * (fbm(u, v, 8, 4, seed + 1) - 0.5));
        // Bisel: el borde de la baldosa un poco más oscuro.
        double edge = Math.min(Math.min(fu, 1 - fu), Math.min(fv, 1 - fv)) - j;
        if (edge < 0.012) k *= 0.93;
        return shade(c, k);
    }

    /** Placas de 60 cm con vetas finas (las frecuencias enteras hacen que la textura no tenga costuras). */
    private static Color marble(Color c, double u, double v) {
        double tu = u * 2, tv = v * 2;
        double fu = tu - Math.floor(tu), fv = tv - Math.floor(tv);
        if (fu < 0.004 || fu > 0.996 || fv < 0.004 || fv > 0.996) return shade(c, 0.78);
        double turb = fbm(u, v, 4, 5, 31);
        double t = Math.sin(2 * Math.PI * (2 * u + v) + turb * 9);
        double vein = Math.pow(1 - Math.abs(t), 10);
        double t2 = Math.sin(2 * Math.PI * (u - 3 * v) + fbm(u, v, 8, 4, 32) * 7);
        double thin = Math.pow(1 - Math.abs(t2), 24);
        double cloud = 0.06 * (fbm(u, v, 4, 3, 33) - 0.5);
        Color vein1 = mix(shade(c, 1 + cloud), shade(c, 0.55), vein * 0.75);
        return mix(vein1, shade(c, 0.7), thin * 0.5);
    }

    private static Color carpet(Color c, double u, double v) {
        double fiber = hash((int) (u * 256), (int) (v * 256), 41) - 0.5;
        return shade(c, 1 + 0.18 * fiber + 0.12 * (fbm(u, v, 8, 3, 42) - 0.5));
    }

    // ---------------------------------------------------------------- Ruido periódico

    /** Número pseudoaleatorio fijo en [0, 1] para cada celda (x, y). */
    private static double hash(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0x7fffffff) / (double) 0x7fffffff;
    }

    /** Ruido suave que se repite cada {@code period} celdas (así la textura no tiene costuras). */
    private static double noise(double x, double y, int period, int seed) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        double fx = x - x0, fy = y - y0;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        int xa = Math.floorMod(x0, period), xb = Math.floorMod(x0 + 1, period);
        int ya = Math.floorMod(y0, period), yb = Math.floorMod(y0 + 1, period);
        double top = lerp(hash(xa, ya, seed), hash(xb, ya, seed), fx);
        double bottom = lerp(hash(xa, yb, seed), hash(xb, yb, seed), fx);
        return lerp(top, bottom, fy);
    }

    /** Suma de varias capas de ruido (de grueso a fino), entre 0 y 1. */
    private static double fbm(double u, double v, int frequency, int octaves, int seed) {
        double sum = 0, amp = 1, total = 0;
        for (int o = 0; o < octaves; o++) {
            int f = frequency << o;
            sum += noise(frac(u) * f, frac(v) * f, f, seed + o * 101) * amp;
            total += amp;
            amp *= 0.5;
        }
        return sum / total;
    }

    private static double frac(double x) {
        return x - Math.floor(x);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static Color shade(Color c, double k) {
        return Color.color(clamp(c.getRed() * k), clamp(c.getGreen() * k), clamp(c.getBlue() * k));
    }

    private static Color mix(Color a, Color b, double t) {
        t = clamp(t);
        return Color.color(lerp(a.getRed(), b.getRed(), t), lerp(a.getGreen(), b.getGreen(), t), lerp(a.getBlue(), b.getBlue(), t));
    }

    private static double clamp(double v) {
        return Math.min(Math.max(v, 0), 1);
    }
}
