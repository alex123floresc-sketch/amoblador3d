package pe.edu.unaj.amoblador3d.scene;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import pe.edu.unaj.amoblador3d.model.FSHFFinish;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;

/**
 * Muebles paramétricos: se construyen con cajas a partir de ancho, profundidad y alto.
 *
 * <p>Regla que cumplen TODOS los muebles (importante para las colisiones):
 * ocupan exactamente x ∈ [-ancho/2, ancho/2], z ∈ [-prof/2, prof/2], y ∈ [0, alto];
 * el origen es el centro de la base y el frente mira hacia +Z (la cabecera de la cama queda en -Z).
 *
 * <p>Cada mueble tiene un color principal, un color secundario (frazada, cojines, frentes...;
 * si es null se calcula a partir del principal) y un acabado que define cuánto brilla.
 *
 * <p>Para agregar un mueble paramétrico nuevo: crea su método aquí, regístralo en
 * {@link #build} y declara sus medidas en {@code Catalog}.
 */
final class FSHFParametricBuilder {
    private FSHFParametricBuilder() {}

    /** Colores y acabado de un mueble. {@code accent} null = color secundario automático. */
    private record Style(String main, String accent, FSHFFinish finish) {
        Material mat() {
            return FSHFMaterials.finished(main, finish);
        }

        Material mat(String color) {
            return FSHFMaterials.finished(color, finish);
        }

        /** El color principal más oscuro (frentes, bases...). */
        Material darker(double factor) {
            return mat(FSHFMaterials.darkerHex(main, factor));
        }

        /** El color secundario elegido, o {@code auto} si es automático. */
        Material accent(String auto) {
            return mat(accent != null ? accent : auto);
        }

        /** El color secundario elegido, o el principal oscurecido en {@code factor}. */
        Material accentDarker(double factor) {
            return accent(FSHFMaterials.darkerHex(main, factor));
        }

        /** El color secundario elegido, o metal cromado si es automático (tiradores, grifería). */
        Material accentOrMetal() {
            return accent != null ? FSHFMaterials.finished(accent, FSHFFinish.METALICO) : FSHFMaterials.METAL;
        }
    }

    /** Acabado típico de cada mueble cuando el usuario no eligió uno. */
    static FSHFFinish defaultFinish(String type) {
        return switch (type) {
            case "refrigeradora", "inodoro", "lavatorio" -> FSHFFinish.BRILLANTE;
            case "sillon", "cama" -> FSHFFinish.MATE;
            default -> FSHFFinish.SATINADO;
        };
    }

    /** Devuelve null si el tipo no es paramétrico. */
    static Group build(FSHFFurniture f) {
        String main = f.color != null ? f.color : "#a0764f";
        Style color = new Style(main, f.color2, f.finish != null ? f.finish : defaultFinish(f.type));
        return switch (f.type) {
            case "cama" -> bed(f.width, f.depth, f.height, color);
            case "mesa" -> table(f.width, f.depth, f.height, color);
            case "silla" -> chair(f.width, f.depth, f.height, color);
            case "closet" -> closet(f.width, f.depth, f.height, color);
            case "escritorio" -> desk(f.width, f.depth, f.height, color);
            case "mesa-noche" -> drawers(f.width, f.depth, f.height, color, 2, 1);
            case "comoda" -> drawers(f.width, f.depth, f.height, color, f.height > 1 ? 4 : 3, f.width >= 1 ? 2 : 1);
            case "sillon" -> sofa(f.width, f.depth, f.height, color);
            case "rack-tv" -> tvUnit(f.width, f.depth, f.height, color);
            case "librero" -> bookshelf(f.width, f.depth, f.height, color);
            case "cocina" -> kitchen(f.width, f.depth, f.height, color);
            case "refrigeradora" -> fridge(f.width, f.depth, f.height, color);
            case "inodoro" -> toilet(f.width, f.depth, f.height, color);
            case "lavatorio" -> washbasin(f.width, f.depth, f.height, color);
            case "escalera" -> straightStair(f.width, f.depth, f.height, color);
            case "escalera-l" -> lStair(f.width, f.depth, f.height, color);
            case "escalera-caracol" -> spiralStair(f.width, f.depth, f.height, color);
            case "baranda" -> railing(f.width, f.depth, f.height, color);
            default -> null;
        };
    }

    /** Caja roja translúcida para muebles que ya no están en el catálogo. */
    static Group missing(FSHFFurniture f) {
        Group g = new Group();
        box(g, FSHFMaterials.MISSING, f.width, f.height, f.depth, 0, f.height / 2, 0);
        return g;
    }

    /** Caja gris mientras se carga un modelo 3D. */
    static Group placeholder(FSHFFurniture f) {
        Group g = new Group();
        box(g, FSHFMaterials.PLACEHOLDER, f.width, f.height, f.depth, 0, f.height / 2, 0);
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

    /** Cilindro vertical de radio r y alto h centrado en (x, y, z). */
    private static void cylinder(Group g, Material m, double r, double h, double x, double y, double z) {
        if (r <= 0 || h <= 0) return;
        Cylinder c = new Cylinder(r, h, 24);
        c.setTranslateX(x);
        c.setTranslateY(y);
        c.setTranslateZ(z);
        c.setMaterial(m);
        g.getChildren().add(c);
    }

    private static Group bed(double w, double d, double h, Style color) {
        Group g = new Group();
        Material wood = color.mat();
        double head = 0.06; // grosor de la cabecera
        double top = Math.min(0.55, h - 0.05); // altura de la superficie del colchón
        double base = top - 0.2;
        double bodyD = d - head, bodyZ = head / 2;
        box(g, wood, w, base, bodyD, 0, base / 2, bodyZ); // base
        box(g, FSHFMaterials.of("#f4f1ea"), w - 0.04, 0.2, bodyD - 0.04, 0, base + 0.1, bodyZ); // colchón
        double blanketD = bodyD * 0.65;
        box(g, color.accent("#5b7fa6"), w, 0.21, blanketD, 0, base + 0.105, d / 2 - blanketD / 2); // frazada
        box(g, wood, w, h, head, 0, h / 2, -d / 2 + head / 2); // cabecera
        int pillows = w >= 1.2 ? 2 : 1;
        double pw = (w - 0.1 * (pillows + 1)) / pillows, ph = Math.min(0.1, h - top);
        for (int i = 0; i < pillows; i++) {
            double x = -w / 2 + 0.1 + pw / 2 + i * (pw + 0.1);
            box(g, FSHFMaterials.of("#ffffff"), pw, ph, 0.35, x, top + ph / 2, -d / 2 + head + 0.03 + 0.175); // almohada
        }
        return g;
    }

    private static Group table(double w, double d, double h, Style color) {
        Group g = new Group();
        Material wood = color.mat();
        double t = 0.04; // grosor del tablero
        box(g, wood, w, t, d, 0, h - t / 2, 0);
        double lx = w / 2 - 0.06, lz = d / 2 - 0.06;
        Material legs = color.accentDarker(1);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) box(g, legs, 0.05, h - t, 0.05, sx * lx, (h - t) / 2, sz * lz); // patas
        }
        return g;
    }

    private static Group chair(double w, double d, double h, Style color) {
        Group g = new Group();
        Material wood = color.mat();
        double seat = Math.min(0.46, h * 0.5), leg = 0.035; // altura del asiento y grosor de las patas
        double lx = w / 2 - leg / 2, lz = d / 2 - leg / 2;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) box(g, wood, leg, seat - 0.04, leg, sx * lx, (seat - 0.04) / 2, sz * lz); // patas
        }
        box(g, color.accentDarker(0.8), w, 0.04, d, 0, seat - 0.02, 0); // asiento
        for (int sx = -1; sx <= 1; sx += 2) box(g, wood, leg, h - seat, leg, sx * lx, seat + (h - seat) / 2, -lz); // postes del respaldo
        box(g, wood, w, 0.18, 0.025, 0, h - 0.09, -d / 2 + 0.0125); // respaldo
        return g;
    }

    private static Group closet(double w, double d, double h, Style color) {
        Group g = new Group();
        box(g, color.mat(), w, h, d - 0.04, 0, h / 2, -0.02); // cuerpo
        int doors = w > 1.6 ? 3 : 2;
        double dw = w / doors;
        Material door = color.accentDarker(0.93);
        for (int i = 0; i < doors; i++) {
            double x = -w / 2 + dw * (i + 0.5);
            box(g, door, dw - 0.006, h - 0.01, 0.02, x, h / 2, d / 2 - 0.03); // puerta
            int side = i < doors / 2.0 ? 1 : -1; // manija hacia el centro del closet
            box(g, FSHFMaterials.METAL, 0.02, 0.25, 0.02, x + side * (dw / 2 - 0.05), h * 0.5, d / 2 - 0.01); // manija
        }
        return g;
    }

    private static Group desk(double w, double d, double h, Style color) {
        Group g = new Group();
        Material wood = color.mat();
        double t = 0.03, under = h - t; // grosor del tablero y espacio bajo él
        box(g, wood, w, t, d, 0, h - t / 2, 0); // tablero
        box(g, wood, 0.03, under, d - 0.02, -w / 2 + 0.015, under / 2, 0); // lateral izquierdo
        double pw = Math.min(0.4, w * 0.35), px = w / 2 - pw / 2; // cajonera
        box(g, wood, pw, under, d - 0.04, px, under / 2, -0.01);
        double drawerH = (under - 0.02) / 3;
        Material front = color.accentDarker(0.85);
        for (int i = 0; i < 3; i++) {
            box(g, front, pw - 0.02, drawerH - 0.01, 0.015, px, 0.01 + drawerH * (i + 0.5), d / 2 - 0.0125); // frente de cajón
        }
        double backW = w - pw - 0.03;
        box(g, wood, backW, 0.3, 0.02, -w / 2 + 0.03 + backW / 2, under - 0.15, -d / 2 + 0.02); // faldón trasero
        return g;
    }

    /**
     * Mueble de cajones (mesa de noche, cómoda): tablero, cuerpo sobre patas y frentes con tirador.
     * Los frentes y tiradores quedan dentro de la profundidad (el tirador llega justo al frente).
     */
    private static Group drawers(double w, double d, double h, Style color, int rows, int cols) {
        Group g = new Group();
        Material wood = color.mat(), front = color.accentDarker(0.9);
        double leg = 0.05, top = 0.03, bodyH = h - top - leg;
        box(g, wood, w, top, d, 0, h - top / 2, 0); // tablero
        box(g, wood, w - 0.02, bodyH, d - 0.03, 0, leg + bodyH / 2, -0.015); // cuerpo
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                box(g, wood, 0.03, leg, 0.03, sx * (w / 2 - 0.03), leg / 2, sz * (d / 2 - 0.035)); // patas
            }
        }
        double rh = bodyH / rows, cw = (w - 0.02) / cols;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double x = -(w - 0.02) / 2 + cw * (j + 0.5), y = leg + rh * (i + 0.5);
                box(g, front, cw - 0.012, rh - 0.012, 0.015, x, y, d / 2 - 0.0175); // frente del cajón
                box(g, FSHFMaterials.METAL, Math.min(0.12, cw * 0.4), 0.015, 0.01, x, y + rh * 0.2, d / 2 - 0.005); // tirador
            }
        }
        return g;
    }

    /** Sofá: base, respaldo, brazos y cojines. El frente (donde uno se sienta) mira a +Z. */
    private static Group sofa(double w, double d, double h, Style color) {
        Group g = new Group();
        Material fabric = color.mat(), dark = color.darker(0.8), cushion = color.accentDarker(0.93);
        double arm = Math.min(0.18, w * 0.12), back = Math.min(0.2, d * 0.25);
        double seat = Math.min(0.45, h - 0.2), base = seat - 0.12;
        box(g, dark, w, base, d, 0, base / 2, 0); // base
        box(g, fabric, w, h - base, back, 0, base + (h - base) / 2, -d / 2 + back / 2); // respaldo
        double armH = Math.min(h, seat + 0.18);
        for (int sx = -1; sx <= 1; sx += 2) box(g, fabric, arm, armH, d, sx * (w / 2 - arm / 2), armH / 2, 0); // brazos
        int n = w - 2 * arm > 1.5 ? 3 : 2;
        double cw = (w - 2 * arm) / n, seatD = d - back;
        double backCushionH = Math.max(0, h - seat - 0.06);
        for (int i = 0; i < n; i++) {
            double x = -w / 2 + arm + cw * (i + 0.5);
            box(g, cushion, cw - 0.02, 0.12, seatD - 0.02, x, base + 0.06, -d / 2 + back + seatD / 2); // cojín del asiento
            box(g, cushion, cw - 0.03, backCushionH, 0.12, x, seat + backCushionH / 2, -d / 2 + back + 0.06); // cojín del respaldo
        }
        return g;
    }

    /** Mueble bajo con televisor encima (el televisor llega exactamente al alto indicado). */
    private static Group tvUnit(double w, double d, double h, Style color) {
        Group g = new Group();
        Material wood = color.mat(), black = FSHFMaterials.of("#111111");
        double leg = 0.05, cab = Math.min(0.5, h * 0.4);
        box(g, wood, w, cab - leg, d, 0, leg + (cab - leg) / 2, 0); // mueble
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                box(g, FSHFMaterials.METAL, 0.03, leg, 0.03, sx * (w / 2 - 0.04), leg / 2, sz * (d / 2 - 0.04)); // patas
            }
        }
        Material door = color.accentDarker(0.8);
        for (int i = 0; i < 2; i++) {
            box(g, door, w / 2 - 0.02, cab - leg - 0.03, 0.004, (i - 0.5) * w / 2, leg + (cab - leg) / 2, d / 2 - 0.002); // puertas
        }
        // Televisor 16:9 con soporte, apoyado sobre el mueble.
        double stand = 0.06, avail = h - cab - stand;
        double tw = Math.min(w - 0.1, avail * 16 / 9), th = tw * 9 / 16;
        box(g, black, 0.3, 0.015, Math.min(0.18, d - 0.05), 0, cab + 0.0075, 0); // base del soporte
        box(g, black, 0.05, h - th - cab, 0.03, 0, cab + (h - th - cab) / 2, -0.03); // cuello
        box(g, black, tw, th, 0.05, 0, h - th / 2, -0.03); // marco
        box(g, FSHFMaterials.shiny(Color.web("#1d2a3a"), 0.9, 64), tw - 0.03, th - 0.03, 0.004, 0, h - th / 2, -0.003); // pantalla
        return g;
    }

    /** Librero: laterales, repisas cada ~35 cm, fondo y libros de colores. */
    private static Group bookshelf(double w, double d, double h, Style color) {
        Group g = new Group();
        Material wood = color.mat();
        double t = 0.02, plinth = 0.06;
        for (int sx = -1; sx <= 1; sx += 2) box(g, wood, t, h, d, sx * (w / 2 - t / 2), h / 2, 0); // laterales
        box(g, wood, w - 2 * t, t, d, 0, h - t / 2, 0); // techo
        box(g, wood, w - 2 * t, plinth, d, 0, plinth / 2, 0); // zócalo
        box(g, color.accentDarker(0.8), w - 2 * t, h - t - plinth, 0.01, 0, plinth + (h - t - plinth) / 2, -d / 2 + 0.005); // fondo
        int shelves = Math.max(0, (int) Math.round((h - plinth - t) / 0.35) - 1);
        double space = (h - plinth - t) / (shelves + 1);
        for (int k = 1; k <= shelves; k++) box(g, wood, w - 2 * t, t, d - 0.01, 0, plinth + k * space, 0.005); // repisas
        String[] colors = {"#8c2f39", "#2f5d8c", "#d9a441", "#3f7d4f", "#e8e2d6", "#5b4a7a", "#c2603f"};
        double bookD = Math.min(0.22, (d - 0.01) * 0.85);
        int c = 0;
        for (int k = 0; k <= shelves; k++) {
            double y0 = plinth + k * space + (k > 0 ? t / 2 : 0);
            double maxH = space - t - 0.03;
            if (maxH < 0.08) continue;
            double x = -w / 2 + t + 0.02, limit = -w / 2 + t + (w - 2 * t) * 0.72; // libros en ~70 % del ancho
            while (x < limit) {
                double bw = 0.025 + (c * 7 % 5) * 0.005, bh = Math.min(maxH, 0.18 + (c * 3 % 4) * 0.025);
                box(g, FSHFMaterials.of(colors[c % colors.length]), bw, bh, bookD, x + bw / 2, y0 + bh / 2, -d / 2 + 0.01 + bookD / 2);
                x += bw + 0.003;
                c++;
            }
        }
        return g;
    }

    /** Mueble bajo de cocina: zócalo, puertas, encimera con lavadero y cocina de 4 hornillas. */
    private static Group kitchen(double w, double d, double h, Style color) {
        Group g = new Group();
        Material body = color.mat(), door = color.darker(0.95);
        Material stone = color.accent("#bdb7ad"), black = FSHFMaterials.of("#1a1a1a");
        double plinth = 0.1, top = 0.038, bodyH = h - 0.04 - plinth;
        box(g, FSHFMaterials.of("#3a3a3a"), w, plinth, d - 0.08, 0, plinth / 2, -0.04); // zócalo
        box(g, body, w, bodyH, d - 0.05, 0, plinth + bodyH / 2, -0.025); // cuerpo
        int n = Math.max(1, (int) Math.round(w / 0.6));
        double dw = w / n;
        for (int i = 0; i < n; i++) {
            double x = -w / 2 + dw * (i + 0.5);
            box(g, door, dw - 0.006, bodyH - 0.01, 0.018, x, plinth + bodyH / 2, d / 2 - 0.041); // puerta
            box(g, FSHFMaterials.METAL, Math.min(0.14, dw * 0.4), 0.015, 0.012, x, plinth + bodyH - 0.06, d / 2 - 0.026); // tirador
        }
        // La encimera queda 2 mm bajo el alto total y el lavadero y las hornillas llegan justo al
        // alto (así sus caras de arriba no se pisan con la de la encimera).
        box(g, stone, w, top, d, 0, h - 0.002 - top / 2, 0); // encimera
        if (w >= 1.2) {
            box(g, FSHFMaterials.shiny(Color.web("#c8ccd0"), 0.9, 60), 0.5, 0.004, 0.38, -w / 4, h - 0.002, 0.02); // lavadero
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    cylinder(g, black, 0.065, 0.004, w / 4 + sx * 0.14, h - 0.002, 0.02 + sz * 0.12); // hornillas
                }
            }
        }
        return g;
    }

    /** Refrigeradora de dos puertas (congelador arriba) con tiradores. */
    private static Group fridge(double w, double d, double h, Style color) {
        Group g = new Group();
        Material body = color.mat(), door = color.darker(0.97), handle = color.accentOrMetal();
        box(g, body, w, h, d - 0.03, 0, h / 2, -0.015); // cuerpo
        double split = h * 0.68;
        box(g, door, w - 0.01, split - 0.015, 0.02, 0, split / 2, d / 2 - 0.02); // puerta de abajo
        box(g, door, w - 0.01, h - split - 0.015, 0.02, 0, split + (h - split) / 2, d / 2 - 0.02); // congelador
        box(g, handle, 0.025, Math.min(0.5, split * 0.45), 0.01, w / 2 - 0.07, split - 0.3, d / 2 - 0.005); // tirador
        box(g, handle, 0.025, Math.min(0.25, (h - split) * 0.5), 0.01, w / 2 - 0.07, split + 0.2, d / 2 - 0.005); // tirador
        return g;
    }

    /** Inodoro: tanque atrás, taza redonda adelante (+Z) y asiento. */
    private static Group toilet(double w, double d, double h, Style color) {
        Group g = new Group();
        Material white = color.mat();
        double tankD = 0.18, seatY = 0.4, r = w / 2;
        double tankFront = -d / 2 + tankD, zc = d / 2 - r; // frente del tanque y centro de la taza
        box(g, white, w, h - 0.38, tankD, 0, 0.38 + (h - 0.38) / 2, -d / 2 + tankD / 2); // tanque
        box(g, white, w * 0.55, seatY - 0.12, Math.max(0.1, zc - tankFront), 0, (seatY - 0.12) / 2, (tankFront + zc) / 2); // pie
        box(g, white, w, 0.12, Math.max(0.01, zc - tankFront), 0, seatY - 0.06, (tankFront + zc) / 2); // unión con el tanque
        cylinder(g, white, r, 0.12, 0, seatY - 0.06, zc); // taza
        cylinder(g, color.accent("#f0f0f0"), r - 0.01, 0.02, 0, seatY + 0.01, zc); // asiento
        return g;
    }

    /** Lavatorio de pedestal con grifo (el grifo llega justo al alto indicado). */
    private static Group washbasin(double w, double d, double h, Style color) {
        Group g = new Group();
        Material white = color.mat();
        double basinH = 0.16, basinTop = h - 0.04;
        cylinder(g, white, Math.min(0.09, w * 0.2), basinTop - basinH, 0, (basinTop - basinH) / 2, -d * 0.05); // pedestal
        box(g, white, w, basinH, d, 0, basinTop - basinH / 2, 0); // lavatorio
        box(g, FSHFMaterials.shiny(Color.web("#cfd6dc"), 0.8, 50), w - 0.1, 0.004, d - 0.14, 0, basinTop, 0.03); // poza
        cylinder(g, color.accentOrMetal(), 0.015, 0.04, 0, h - 0.02, -d / 2 + 0.05); // grifo
        box(g, color.accentOrMetal(), 0.025, 0.015, 0.08, 0, h - 0.0075, -d / 2 + 0.09); // caño
        return g;
    }

    // ---------------------------------------------------------------- Escaleras

    /** Contrapaso ideal (m): la cantidad de peldaños sale del alto. */
    private static final double RISER = 0.18;
    /** Alto de los parantes del pasamanos sobre el peldaño. */
    private static final double POST = 0.85;

    /** Caja girada {@code angle} grados alrededor del eje {@code axis}, con su centro en (x, y, z). */
    private static void turnedBox(Group g, Material m, double w, double h, double d, double x, double y, double z,
                                  double angle, javafx.geometry.Point3D axis) {
        if (w <= 0 || h <= 0 || d <= 0) return;
        Box b = new Box(w, h, d);
        b.setTranslateX(x);
        b.setTranslateY(y);
        b.setTranslateZ(z);
        b.getTransforms().add(new Rotate(angle, axis));
        b.setMaterial(m);
        g.getChildren().add(b);
    }

    /**
     * Pasamanos inclinado de (z0, y0) a (z1, y1) sobre la línea x, con parantes en los extremos.
     * Si no cabe bajo el alto {@code top} (escaleras muy bajas), no se pone.
     */
    private static void handrailZ(Group g, Material m, double x, double z0, double y0, double z1, double y1, double top) {
        if (Math.max(y0, y1) + POST + 0.05 > top || y0 < 0 || y1 < 0) return;
        for (double[] p : new double[][] {{z0, y0}, {z1, y1}}) box(g, m, 0.04, POST, 0.04, x, p[1] + POST / 2, p[0]); // parantes
        // Rotate en X lleva +Z hacia -Y: el ángulo va con el signo cambiado.
        double angle = -Math.toDegrees(Math.atan2(y1 - y0, z1 - z0)), len = Math.hypot(z1 - z0, y1 - y0);
        turnedBox(g, m, 0.05, 0.04, len, x, (y0 + y1) / 2 + POST, (z0 + z1) / 2, angle, Rotate.X_AXIS); // pasamanos
    }

    /** Peldaño macizo de {@code w} × {@code d} (centro en x, z) cuya huella queda a la altura {@code top}. */
    private static void step(Group g, Material body, Material tread, double w, double d, double x, double z, double top) {
        box(g, body, w, top - 0.04, d, x, (top - 0.04) / 2, z); // cuerpo
        box(g, tread, w, 0.04, d, x, top - 0.02, z); // huella
    }

    /**
     * Escalera recta: se sube desde el frente (+Z) hacia atrás (-Z) y el último peldaño llega al
     * alto indicado (el piso de arriba). Pasamanos al lado derecho.
     */
    private static Group straightStair(double w, double d, double h, Style color) {
        Group g = new Group();
        Material wood = color.mat(), body = color.darker(0.85), rail = color.accent("#2f3236");
        int n = Math.max(2, (int) Math.round(h / RISER));
        double rise = h / n, run = d / n;
        for (int i = 0; i < n; i++) step(g, body, wood, w, run, 0, d / 2 - run * (i + 0.5), (i + 1) * rise);
        // El pasamanos sube junto a los peldaños, sin pasar del alto total.
        // Los extremos se separan del borde: en escaleras empinadas el pasamanos inclinado sobresaldría.
        double inset = Math.max(run / 2, 0.03);
        handrailZ(g, rail, w / 2 - 0.03, d / 2 - inset, rise, -d / 2 + inset, Math.max(rise, h - POST - 0.05), h);
        return g;
    }

    /**
     * Escalera en L: un tramo sube por el lado derecho hacia atrás, un descanso en la esquina
     * trasera derecha y un segundo tramo sube hacia la izquierda por el fondo.
     */
    private static Group lStair(double w, double d, double h, Style color) {
        Group g = new Group();
        Material wood = color.mat(), body = color.darker(0.85), rail = color.accent("#2f3236");
        double fw = Math.min(1.0, Math.min(w, d) * 0.5); // ancho de los tramos
        int n = Math.max(4, (int) Math.round(h / RISER));
        double rise = h / n;
        int n1 = n / 2; // el descanso es el peldaño n1
        double run1 = (d - fw) / (n1 - 1), run2 = (w - fw) / (n - n1);
        double xr = w / 2 - fw / 2, zb = -d / 2 + fw / 2; // centro del primer tramo y del segundo
        for (int i = 0; i < n1 - 1; i++) step(g, body, wood, fw, run1, xr, d / 2 - run1 * (i + 0.5), (i + 1) * rise);
        step(g, body, wood, fw, fw, xr, zb, n1 * rise); // descanso
        for (int j = 0; j < n - n1; j++) step(g, body, wood, run2, fw, w / 2 - fw - run2 * (j + 0.5), zb, (n1 + j + 1) * rise);
        // Pasamanos por el lado libre del primer tramo.
        handrailZ(g, rail, w / 2 - fw + 0.03, d / 2 - Math.max(run1 / 2, 0.03), rise, -d / 2 + fw + 0.03, (n1 - 1) * rise, h);
        return g;
    }

    /**
     * Escalera de caracol: poste central y peldaños en abanico (30° cada uno, o más si son pocos,
     * para dar al menos una vuelta) con parantes en la punta. Si la planta no es cuadrada, el
     * caracol se estira hasta llenarla.
     */
    private static Group spiralStair(double w, double d, double h, Style color) {
        Group g = new Group();
        Material steel = color.mat(), tread = color.accent("#b08a5e");
        double r = Math.min(w, d) / 2, pole = 0.05;
        int n = Math.max(4, (int) Math.round(h / RISER));
        double rise = h / n, step = Math.max(30, 360.0 / n);
        cylinder(g, steel, pole, h, 0, h / 2, 0); // poste
        // Ancho del peldaño en la punta, y su largo para que las esquinas no salgan del círculo.
        double tw = Math.min((r - pole) * Math.toRadians(step) * 1.15, 0.5);
        double outer = Math.sqrt(r * r - tw * tw / 4), len = outer - pole;
        for (int i = 0; i < n; i++) {
            double a = Math.toRadians(i * step), mid = pole + len / 2, top = (i + 1) * rise;
            // Peldaño: largo en dirección radial (la Z local, antes de girar).
            turnedBox(g, tread, tw, 0.04, len, Math.sin(a) * mid, top - 0.02, Math.cos(a) * mid, i * step, Rotate.Y_AXIS);
            double ph = Math.min(0.9, h - top);
            cylinder(g, steel, 0.012, ph, Math.sin(a) * (r - 0.03), top + ph / 2, Math.cos(a) * (r - 0.03)); // parante
        }
        if (Math.abs(w - d) < 1e-9) return g;
        Group stretched = new Group(g);
        g.getTransforms().add(new javafx.scene.transform.Scale(w / (2 * r), 1, d / (2 * r)));
        return stretched;
    }

    /** Baranda recta: base, pasamanos arriba, riel abajo y barrotes cada ~12 cm. */
    private static Group railing(double w, double d, double h, Style color) {
        Group g = new Group();
        Material m = color.mat();
        double rail = Math.min(0.05, d);
        box(g, m, w, 0.03, d, 0, 0.015, 0); // base atornillada al piso
        box(g, m, w, 0.05, rail, 0, h - 0.025, 0); // pasamanos
        box(g, m, w, 0.03, rail * 0.6, 0, 0.12, 0); // riel inferior
        int n = Math.max(2, (int) Math.round(w / 0.12));
        for (int i = 0; i <= n; i++) {
            double x = -w / 2 + 0.02 + (w - 0.04) * i / n;
            box(g, m, 0.02, h - 0.08, Math.min(0.02, d), x, 0.03 + (h - 0.08) / 2, 0); // barrote
        }
        return g;
    }
}
