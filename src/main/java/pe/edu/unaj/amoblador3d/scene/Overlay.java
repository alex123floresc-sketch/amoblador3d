package pe.edu.unaj.amoblador3d.scene;

import java.util.List;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import pe.edu.unaj.amoblador3d.design.Design;
import pe.edu.unaj.amoblador3d.design.Snapper;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.Light;
import pe.edu.unaj.amoblador3d.model.LightType;
import pe.edu.unaj.amoblador3d.model.Opening;
import pe.edu.unaj.amoblador3d.model.OpeningType;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.model.Surface;
import pe.edu.unaj.amoblador3d.util.Units;

/**
 * Capa 2D dibujada encima de la vista 3D: cotas (estilo plano de arquitectura), contorno de
 * lo seleccionado, símbolos de puertas y ventanas en planta y la dirección del sol.
 * Se dibuja encima de todo, así las cotas se ven aunque haya paredes delante.
 */
final class Overlay {
    private static final Color DIM = Color.web("#ffd166");
    private static final Color SELECT = Color.web("#4f9cf9");
    private static final Color MEASURE = Color.web("#ff6b9a");
    private static final Color GUIDE = Color.web("#ff4fd8");
    private static final Color LOCKED = Color.web("#f0a04b");
    private static final Font LABEL_FONT = Font.font("System", FontWeight.BOLD, 12);

    final Canvas canvas = new Canvas();
    private GraphicsContext g;
    private ViewCamera cam;
    private Color outlineColor = SELECT;

    void draw(ViewCamera cam, Design design, LightView lights, List<Object> selection, boolean showDimensions) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        drawOn(gc, cam, design, lights, selection, showDimensions);
    }

    /** Dibuja en otro lienzo (para exportar imágenes más grandes que la pantalla). */
    void drawOn(GraphicsContext gc, ViewCamera cam, Design design, LightView lights, List<Object> selection,
                boolean showDimensions) {
        this.cam = cam;
        g = gc;
        RoomDims r = design.room();
        boolean plan = cam.mode == ViewCamera.Mode.PLAN;

        if (plan) for (Opening o : design.openings()) drawPlanSymbol(r, o);
        if (!plan) {
            g.setStroke(Color.web("#ffc845"));
            g.setLineWidth(2);
            for (LightView.SunInfo s : lights.suns()) arrow(s.helper(), s.windowCenter());
        }
        if (showDimensions) {
            double gap = r.wallThickness() + 0.4; // separación de las cotas respecto al interior
            dimension(p(0, 0.01, r.length()), p(r.width(), 0.01, r.length()), p(0, 0, 1), gap); // ancho, fuera del muro sur
            dimension(p(r.width(), 0.01, 0), p(r.width(), 0.01, r.length()), p(1, 0, 0), gap); // largo, fuera del muro este
            if (!plan) dimension(p(0, 0, r.length()), p(0, r.height(), r.length()), p(-1, 0, 0), gap); // alto
        }
        if (selection.size() == 1) {
            drawSelection(r, lights, selection.get(0), plan);
        } else {
            // Varios muebles: solo el contorno de cada uno (las cotas de todos serían ilegibles).
            for (Object o : selection) if (o instanceof Furniture f) furnitureOutline(f);
        }
    }

    /** Guías del imán: líneas punteadas sobre el piso que muestran a qué se pegó el mueble. */
    void drawGuides(List<Snapper.Guide> guides) {
        if (guides.isEmpty()) return;
        g.setStroke(GUIDE);
        g.setLineWidth(3);
        g.setLineDashes(10, 6);
        for (Snapper.Guide gd : guides) line(p(gd.x1(), 0.02, gd.z1()), p(gd.x2(), 0.02, gd.z2()));
        g.setLineDashes();
    }

    /** Rectángulo de selección (coordenadas de pantalla). */
    void drawBand(double x0, double y0, double x1, double y1) {
        double x = Math.min(x0, x1), y = Math.min(y0, y1), w = Math.abs(x1 - x0), h = Math.abs(y1 - y0);
        g.setFill(Color.rgb(79, 156, 249, 0.15));
        g.fillRect(x, y, w, h);
        g.setStroke(SELECT);
        g.setLineWidth(1.2);
        g.setLineDashes(5, 3);
        g.strokeRect(x, y, w, h);
        g.setLineDashes();
    }

    /** Contorno de un mueble (naranja si está bloqueado). */
    private void furnitureOutline(Furniture f) {
        double a = Math.toRadians(f.rotation), c = Math.cos(a), s = Math.sin(a);
        outlineColor = f.locked ? LOCKED : SELECT;
        outline(f.width, f.height, f.depth, (lx, ly, lz) -> p(f.x + lx * c + lz * s, ly, f.z - lx * s + lz * c));
        outlineColor = SELECT;
    }

    /** Línea de la herramienta de medir entre dos puntos del piso, con su distancia. */
    void drawMeasure(Point3D a, Point3D b) {
        Point3D lift = p(0, 0.02, 0); // un poco sobre el piso para que no lo tape en 3D
        Point2D sa = cam.project(a.add(lift));
        if (sa != null) {
            g.setFill(MEASURE);
            g.fillOval(sa.getX() - 4, sa.getY() - 4, 8, 8);
        }
        if (b == null) return;
        g.setStroke(MEASURE);
        g.setLineWidth(2.2);
        g.setLineDashes(8, 5);
        line(a.add(lift), b.add(lift));
        g.setLineDashes();
        Point2D sb = cam.project(b.add(lift));
        if (sb != null) g.fillOval(sb.getX() - 4, sb.getY() - 4, 8, 8);
        if (a.distance(b) > 0.005) label(a.midpoint(b).add(lift), Units.meters(a.distance(b)));
    }

    private static Point3D p(double x, double y, double z) {
        return new Point3D(x, y, z);
    }

    // ---------------------------------------------------------------- Selección

    private void drawSelection(RoomDims r, LightView lights, Object selected, boolean plan) {
        if (selected instanceof Furniture f) {
            double a = Math.toRadians(f.rotation), c = Math.cos(a), s = Math.sin(a);
            // Esquinas locales del mueble llevadas al mundo (mismo giro que javafx Rotate en Y).
            java.util.function.BiFunction<Double, Double, double[]> world = (lx, lz) ->
                    new double[] {f.x + lx * c + lz * s, f.z - lx * s + lz * c};
            furnitureOutline(f);
            double hw = f.width / 2, hd = f.depth / 2;
            double[] fl = world.apply(-hw, hd), fr = world.apply(hw, hd), br = world.apply(hw, -hd);
            double[] front = world.apply(0.0, 1.0), side = world.apply(1.0, 0.0), left = world.apply(-1.0, 0.0);
            Point3D frontDir = p(front[0] - f.x, 0, front[1] - f.z), sideDir = p(side[0] - f.x, 0, side[1] - f.z);
            dimension(p(fl[0], 0.01, fl[1]), p(fr[0], 0.01, fr[1]), frontDir, 0.15); // ancho
            dimension(p(br[0], 0.01, br[1]), p(fr[0], 0.01, fr[1]), sideDir, 0.15); // profundidad
            if (!plan) dimension(p(fl[0], 0, fl[1]), p(fl[0], f.height, fl[1]), p(left[0] - f.x, 0, left[1] - f.z), 0.15); // alto
        } else if (selected instanceof Opening o) {
            WallFrame wall = WallFrame.of(o.wall, r);
            double a0 = o.offset - 0.02, y0 = o.sill - 0.02, d0 = -0.03;
            double w = o.width + 0.04, h = o.height + 0.04, d = r.wallThickness() + 0.06;
            outline(w, h, d, (lx, ly, lz) -> wall.point(a0 + lx + w / 2, y0 + ly, d0 + lz + d / 2));
        } else if (selected instanceof Surface s && s.isFloor()) {
            outline(r.width(), 0.02, r.length(), (lx, ly, lz) -> p(r.width() / 2 + lx, ly, r.length() / 2 + lz));
        } else if (selected instanceof Surface s) {
            WallFrame wall = WallFrame.of(s.wallSide(), r);
            double len = r.wallLength(s.wallSide()), t = r.wallThickness();
            outline(len, r.height(), t, (lx, ly, lz) -> wall.point(len / 2 + lx, ly, t / 2 + lz));
        } else if (selected instanceof Light l) {
            Point3D c = lights.helperPosition(l);
            if (c == null) return;
            outline(0.22, 0.22, 0.22, (lx, ly, lz) -> c.add(lx, ly - 0.11, lz));
            if (l.type == LightType.SPOT) {
                g.setStroke(DIM);
                g.setLineWidth(1.5);
                g.setLineDashes(6, 4);
                line(c, p(l.targetX, 0, l.targetZ));
                g.setLineDashes();
            }
        }
    }

    private interface Corner {
        Point3D at(double lx, double ly, double lz);
    }

    /** Contorno de una caja: x ∈ [-w/2, w/2], y ∈ [0, h], z ∈ [-d/2, d/2] (en coordenadas locales). */
    private void outline(double w, double h, double d, Corner corner) {
        Point3D[] v = new Point3D[8];
        int i = 0;
        for (double y : new double[] {0, h}) {
            for (double[] xz : new double[][] {{-w / 2, -d / 2}, {w / 2, -d / 2}, {w / 2, d / 2}, {-w / 2, d / 2}}) {
                v[i++] = corner.at(xz[0], y, xz[1]);
            }
        }
        g.setStroke(outlineColor);
        g.setLineWidth(2);
        for (int k = 0; k < 4; k++) {
            line(v[k], v[(k + 1) % 4]);
            line(v[k + 4], v[(k + 1) % 4 + 4]);
            line(v[k], v[k + 4]);
        }
    }

    // ---------------------------------------------------------------- Planta

    /** Ventana: línea por el centro del muro. Puerta: hoja abierta 90° y arco de giro. */
    private void drawPlanSymbol(RoomDims r, Opening o) {
        WallFrame wall = WallFrame.of(o.wall, r);
        double y = r.height() + 0.01, t = r.wallThickness(), x0 = o.offset, x1 = x0 + o.width;
        double[] xs = new double[4], ys = new double[4];
        Point3D[] quad = {wall.point(x0, y, 0), wall.point(x1, y, 0), wall.point(x1, y, t), wall.point(x0, y, t)};
        for (int i = 0; i < 4; i++) {
            Point2D s = cam.project(quad[i]);
            if (s == null) return;
            xs[i] = s.getX();
            ys[i] = s.getY();
        }
        g.setFill(o.type == OpeningType.DOOR ? Color.WHITE : Color.web("#9fd3ff"));
        g.fillPolygon(xs, ys, 4);
        g.setStroke(Color.web("#333333"));
        g.setLineWidth(1.2);
        if (o.type == OpeningType.WINDOW) {
            line(wall.point(x0, y, t / 2), wall.point(x1, y, t / 2));
        } else {
            Point3D prev = wall.point(x0, y, 0);
            line(prev, wall.point(x0, y, -o.width)); // hoja abierta
            prev = wall.point(x0, y, -o.width);
            for (int i = 1; i <= 24; i++) {
                double a = Math.PI / 2 * (i / 24.0);
                Point3D next = wall.point(x0 + o.width * Math.sin(a), y, -o.width * Math.cos(a));
                line(prev, next);
                prev = next;
            }
        }
    }

    private void arrow(Point3D from, Point3D to) {
        Point2D a = cam.project(from), b = cam.project(to);
        if (a == null || b == null) return;
        g.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
        double angle = Math.atan2(b.getY() - a.getY(), b.getX() - a.getX());
        for (double side : new double[] {-0.45, 0.45}) {
            g.strokeLine(b.getX(), b.getY(), b.getX() - 10 * Math.cos(angle + side), b.getY() - 10 * Math.sin(angle + side));
        }
    }

    // ---------------------------------------------------------------- Cotas

    /**
     * Cota entre dos puntos: línea principal, líneas de extensión, marcas diagonales en los
     * extremos y una etiqueta con el valor.
     *
     * @param offsetDir dirección hacia donde se separa la cota del objeto
     * @param offset    separación en metros
     */
    private void dimension(Point3D start, Point3D end, Point3D offsetDir, double offset) {
        Point3D n = offsetDir.normalize();
        Point3D a = start.add(n.multiply(offset)), b = end.add(n.multiply(offset));
        Point3D dir = b.subtract(a).normalize();
        Point3D tick = dir.add(n).normalize().multiply(0.08);
        Point3D extra = n.multiply(0.1); // la línea de extensión sobrepasa un poco la cota
        g.setStroke(DIM);
        g.setLineWidth(1.3);
        line(a, b);
        line(start, a.add(extra));
        line(end, b.add(extra));
        line(a.subtract(tick), a.add(tick));
        line(b.subtract(tick), b.add(tick));
        label(a.midpoint(b), Units.meters(start.distance(end)));
    }

    private void line(Point3D from, Point3D to) {
        Point2D a = cam.project(from), b = cam.project(to);
        if (a != null && b != null) g.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
    }

    private void label(Point3D at, String text) {
        Point2D s = cam.project(at);
        if (s == null) return;
        Text measure = new Text(text);
        measure.setFont(LABEL_FONT);
        double w = measure.getLayoutBounds().getWidth() + 10, h = 18;
        g.setFill(Color.rgb(255, 255, 255, 0.92));
        g.fillRoundRect(s.getX() - w / 2, s.getY() - h / 2, w, h, 6, 6);
        g.setFill(Color.web("#1a1a1a"));
        g.setFont(LABEL_FONT);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(text, s.getX(), s.getY());
    }
}
