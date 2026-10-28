package pe.edu.unaj.amoblador3d.scene;

import java.util.Locale;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import pe.edu.unaj.amoblador3d.catalog.CustomDesign;
import pe.edu.unaj.amoblador3d.catalog.CustomDesign.Paint;
import pe.edu.unaj.amoblador3d.catalog.CustomDesign.Piece;
import pe.edu.unaj.amoblador3d.catalog.CustomDesign.Shape;
import pe.edu.unaj.amoblador3d.model.Finish;
import pe.edu.unaj.amoblador3d.model.Furniture;

/**
 * Nodos 3D de los muebles hechos en el Diseñador ({@link CustomDesign}) y conversión de un
 * mueble paramétrico en piezas editables (para "Diseñar a partir de este mueble").
 *
 * <p>Cada pieza es una forma de 1 x 1 x 1 m escalada a sus medidas: así todas las formas se
 * describen igual (centro en planta, altura de la base y ancho x alto x profundidad).
 */
public final class FurnitureShapes {
    private FurnitureShapes() {}

    /** Colores con que se pinta un diseño: los del mueble colocado o los del propio diseño. */
    public record Colors(String main, String accent, Finish finish) {
        public static Colors of(CustomDesign d) {
            return new Colors(d.color, d.color2, d.finish);
        }

        static Colors of(CustomDesign d, Furniture f) {
            return new Colors(f.color != null ? f.color : d.color, f.color2 != null ? f.color2 : d.color2,
                    f.finish != null ? f.finish : d.finish);
        }
    }

    /** El diseño en sus propias medidas (sin escalar), con el origen en el centro de la base. */
    public static Group design(CustomDesign d, Colors colors) {
        Group g = new Group();
        for (Piece p : d.pieces) g.getChildren().add(piece(p, colors));
        return g;
    }

    /** El diseño escalado a las medidas del mueble colocado (como los modelos 3D). */
    static Group fit(CustomDesign d, Furniture f) {
        CustomDesign.Bounds b = d.bounds();
        double safe = 1e-4;
        Group fitted = new Group(design(d, Colors.of(d, f)));
        fitted.getTransforms().addAll(
                new Scale(f.width / Math.max(b.width(), safe), f.height / Math.max(b.height(), safe), f.depth / Math.max(b.depth(), safe)),
                new Translate(-(b.minX() + b.maxX()) / 2, -b.minY(), -(b.minZ() + b.maxZ()) / 2));
        return fitted;
    }

    /** Una pieza: forma unitaria movida, escalada y (en las barras) acostada. */
    public static Node piece(Piece p, Colors colors) {
        Shape3D shape = switch (p.shape) {
            case BOX -> new Box(1, 1, 1);
            case SPHERE -> new Sphere(0.5, 40);
            case CYLINDER, BAR_X, BAR_Z -> new Cylinder(0.5, 1, 40);
        };
        shape.setMaterial(material(p, colors));
        shape.getTransforms().addAll(new Translate(p.x, p.y + p.height / 2, p.z), new Scale(p.width, p.height, p.depth));
        if (p.shape == Shape.BAR_X) shape.getTransforms().add(new Rotate(90, Rotate.Z_AXIS));
        if (p.shape == Shape.BAR_Z) shape.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
        return shape;
    }

    static Material material(Piece p, Colors colors) {
        String color = switch (p.paint) {
            case MAIN -> colors.main();
            case ACCENT -> colors.accent();
            case OWN -> p.color;
        };
        return Materials.finished(color, p.finish != null ? p.finish : colors.finish());
    }

    // ---------------------------------------------------------------- Paramétrico -> piezas

    /**
     * Convierte un mueble paramétrico (con sus medidas y colores actuales) en un diseño editable.
     * Las piezas del color principal o secundario quedan ligadas a esos colores; el resto
     * (almohadas, tiradores, el televisor...) conservan su color propio.
     *
     * @return null si el mueble no es paramétrico
     */
    public static CustomDesign fromParametric(Furniture f, String name) {
        Group g = ParametricBuilder.build(f);
        if (g == null) return null;
        CustomDesign d = new CustomDesign();
        d.name = name;
        d.color = f.color != null ? f.color.toLowerCase(Locale.ROOT) : "#a0764f";
        d.finish = f.finish != null ? f.finish : ParametricBuilder.defaultFinish(f.type);
        d.color2 = f.color2 != null ? f.color2.toLowerCase(Locale.ROOT) : Materials.darkerHex(d.color, 0.85);
        int n = 1;
        for (Node node : g.getChildren()) {
            Piece p = new Piece();
            if (node instanceof Box b) {
                p.shape = Shape.BOX;
                p.width = b.getWidth();
                p.height = b.getHeight();
                p.depth = b.getDepth();
            } else if (node instanceof Cylinder c) {
                p.shape = Shape.CYLINDER;
                p.width = p.depth = c.getRadius() * 2;
                p.height = c.getHeight();
            } else {
                continue;
            }
            p.x = round(node.getTranslateX());
            p.y = round(node.getTranslateY() - p.height / 2);
            p.z = round(node.getTranslateZ());
            p.width = round(p.width);
            p.height = round(p.height);
            p.depth = round(p.depth);
            if (p.width <= 0 || p.height <= 0 || p.depth <= 0) continue;
            p.name = "Pieza " + n++;
            PhongMaterial m = (PhongMaterial) ((Shape3D) node).getMaterial();
            String hex = Materials.hex(m.getDiffuseColor());
            Finish finish = finishOf(m);
            if (hex.equalsIgnoreCase(d.color)) {
                p.paint = Paint.MAIN;
            } else if (hex.equalsIgnoreCase(d.color2)) {
                p.paint = Paint.ACCENT;
            } else {
                p.paint = Paint.OWN;
                p.color = hex;
            }
            if (finish != d.finish) p.finish = finish;
            d.pieces.add(p);
        }
        return d;
    }

    /** El acabado más parecido al brillo de un material. */
    private static Finish finishOf(PhongMaterial m) {
        Color spec = m.getSpecularColor();
        if (spec == null) return Finish.MATE;
        double s = spec.getBrightness();
        Finish best = Finish.MATE;
        for (Finish f : Finish.values()) {
            if (f == Finish.METALICO) continue;
            if (Math.abs(f.specular() - s) < Math.abs(best.specular() - s)) best = f;
        }
        return s > 0.85 && m.getSpecularPower() <= 32 ? Finish.METALICO : best;
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }
}
