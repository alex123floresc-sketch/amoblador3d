package pe.edu.unaj.amoblador3d.scene;

import javafx.scene.Group;
import javafx.scene.paint.Material;
import javafx.scene.shape.Box;
import pe.edu.unaj.amoblador3d.model.Furniture;

/**
 * Muebles paramétricos: se construyen con cajas a partir de ancho, profundidad y alto.
 *
 * <p>Regla que cumplen TODOS los muebles (importante para las colisiones):
 * ocupan exactamente x ∈ [-ancho/2, ancho/2], z ∈ [-prof/2, prof/2], y ∈ [0, alto];
 * el origen es el centro de la base y el frente mira hacia +Z (la cabecera de la cama queda en -Z).
 *
 * <p>Para agregar un mueble paramétrico nuevo: crea su método aquí, regístralo en
 * {@link #build} y declara sus medidas en {@code Catalog}.
 */
final class ParametricBuilder {
    private ParametricBuilder() {}

    /** Devuelve null si el tipo no es paramétrico. */
    static Group build(Furniture f) {
        String color = f.color != null ? f.color : "#a0764f";
        return switch (f.type) {
            case "cama" -> bed(f.width, f.depth, f.height, color);
            case "mesa" -> table(f.width, f.depth, f.height, color);
            case "silla" -> chair(f.width, f.depth, f.height, color);
            case "closet" -> closet(f.width, f.depth, f.height, color);
            case "escritorio" -> desk(f.width, f.depth, f.height, color);
            default -> null;
        };
    }

    /** Caja roja translúcida para muebles que ya no están en el catálogo. */
    static Group missing(Furniture f) {
        Group g = new Group();
        box(g, Materials.MISSING, f.width, f.height, f.depth, 0, f.height / 2, 0);
        return g;
    }

    /** Caja gris mientras se carga un modelo 3D. */
    static Group placeholder(Furniture f) {
        Group g = new Group();
        box(g, Materials.PLACEHOLDER, f.width, f.height, f.depth, 0, f.height / 2, 0);
        return g;
    }

    /** Agrega una caja (w x h x d) centrada en (x, y, z). */
    private static void box(Group g, Material m, double w, double h, double d, double x, double y, double z) {
        if (w <= 0 || h <= 0 || d <= 0) return;
        Box b = new Box(w, h, d);
        b.setTranslateX(x);
        b.setTranslateY(y);
        b.setTranslateZ(z);
        b.setMaterial(m);
        g.getChildren().add(b);
    }

    private static Group bed(double w, double d, double h, String color) {
        Group g = new Group();
        Material wood = Materials.of(color);
        double head = 0.06; // grosor de la cabecera
        double top = Math.min(0.55, h - 0.05); // altura de la superficie del colchón
        double base = top - 0.2;
        double bodyD = d - head, bodyZ = head / 2;
        box(g, wood, w, base, bodyD, 0, base / 2, bodyZ); // base
        box(g, Materials.of("#f4f1ea"), w - 0.04, 0.2, bodyD - 0.04, 0, base + 0.1, bodyZ); // colchón
        double blanketD = bodyD * 0.65;
        box(g, Materials.of("#5b7fa6"), w, 0.21, blanketD, 0, base + 0.105, d / 2 - blanketD / 2); // frazada
        box(g, wood, w, h, head, 0, h / 2, -d / 2 + head / 2); // cabecera
        int pillows = w >= 1.2 ? 2 : 1;
        double pw = (w - 0.1 * (pillows + 1)) / pillows, ph = Math.min(0.1, h - top);
        for (int i = 0; i < pillows; i++) {
            double x = -w / 2 + 0.1 + pw / 2 + i * (pw + 0.1);
            box(g, Materials.of("#ffffff"), pw, ph, 0.35, x, top + ph / 2, -d / 2 + head + 0.03 + 0.175); // almohada
        }
        return g;
    }

    private static Group table(double w, double d, double h, String color) {
        Group g = new Group();
        Material wood = Materials.of(color);
        double t = 0.04; // grosor del tablero
        box(g, wood, w, t, d, 0, h - t / 2, 0);
        double lx = w / 2 - 0.06, lz = d / 2 - 0.06;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) box(g, wood, 0.05, h - t, 0.05, sx * lx, (h - t) / 2, sz * lz); // patas
        }
        return g;
    }

    private static Group chair(double w, double d, double h, String color) {
        Group g = new Group();
        Material wood = Materials.of(color);
        double seat = Math.min(0.46, h * 0.5), leg = 0.035; // altura del asiento y grosor de las patas
        double lx = w / 2 - leg / 2, lz = d / 2 - leg / 2;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) box(g, wood, leg, seat - 0.04, leg, sx * lx, (seat - 0.04) / 2, sz * lz); // patas
        }
        box(g, Materials.darker(color, 0.8), w, 0.04, d, 0, seat - 0.02, 0); // asiento
        for (int sx = -1; sx <= 1; sx += 2) box(g, wood, leg, h - seat, leg, sx * lx, seat + (h - seat) / 2, -lz); // postes del respaldo
        box(g, wood, w, 0.18, 0.025, 0, h - 0.09, -d / 2 + 0.0125); // respaldo
        return g;
    }

    private static Group closet(double w, double d, double h, String color) {
        Group g = new Group();
        box(g, Materials.of(color), w, h, d - 0.04, 0, h / 2, -0.02); // cuerpo
        int doors = w > 1.6 ? 3 : 2;
        double dw = w / doors;
        Material door = Materials.darker(color, 0.93);
        for (int i = 0; i < doors; i++) {
            double x = -w / 2 + dw * (i + 0.5);
            box(g, door, dw - 0.006, h - 0.01, 0.02, x, h / 2, d / 2 - 0.03); // puerta
            int side = i < doors / 2.0 ? 1 : -1; // manija hacia el centro del closet
            box(g, Materials.METAL, 0.02, 0.25, 0.02, x + side * (dw / 2 - 0.05), h * 0.5, d / 2 - 0.01); // manija
        }
        return g;
    }

    private static Group desk(double w, double d, double h, String color) {
        Group g = new Group();
        Material wood = Materials.of(color);
        double t = 0.03, under = h - t; // grosor del tablero y espacio bajo él
        box(g, wood, w, t, d, 0, h - t / 2, 0); // tablero
        box(g, wood, 0.03, under, d - 0.02, -w / 2 + 0.015, under / 2, 0); // lateral izquierdo
        double pw = Math.min(0.4, w * 0.35), px = w / 2 - pw / 2; // cajonera
        box(g, wood, pw, under, d - 0.04, px, under / 2, -0.01);
        double drawerH = (under - 0.02) / 3;
        Material front = Materials.darker(color, 0.85);
        for (int i = 0; i < 3; i++) {
            box(g, front, pw - 0.02, drawerH - 0.01, 0.015, px, 0.01 + drawerH * (i + 0.5), d / 2 - 0.0125); // frente de cajón
        }
        double backW = w - pw - 0.03;
        box(g, wood, backW, 0.3, 0.02, -w / 2 + 0.03 + backW / 2, under - 0.15, -d / 2 + 0.02); // faldón trasero
        return g;
    }
}
