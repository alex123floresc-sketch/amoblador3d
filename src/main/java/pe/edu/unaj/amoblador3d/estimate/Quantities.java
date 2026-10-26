package pe.edu.unaj.amoblador3d.estimate;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import pe.edu.unaj.amoblador3d.model.Column;
import pe.edu.unaj.amoblador3d.model.Opening;
import pe.edu.unaj.amoblador3d.model.OpeningType;
import pe.edu.unaj.amoblador3d.model.Project;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.model.WallSide;

/**
 * Metrado de la habitación: áreas y longitudes que se necesitan para comprar los acabados.
 * Todas las medidas son interiores (lo que se pinta o se cubre), en metros.
 *
 * <p>Las columnas, muros internos y recortes en L se descuentan del piso y del techo, y tapan
 * parte de las paredes en las que se apoyan; a cambio, sus caras a la vista se pintan (o se
 * enchapan) con su propio acabado. Por eso un recorte en L no cambia el perímetro: lo que quita
 * de una pared lo agrega en sus dos caras.
 *
 * @param floorArea    área del piso (m²), la misma que la del techo
 * @param wallNet      área de cada pared sin ventanas, puertas ni lo que tapan las columnas (m²)
 * @param columnFaces  caras a la vista de cada columna (m²)
 * @param openingsArea área total de ventanas y puertas (m²)
 * @param perimeter    perímetro interior, rodeando las columnas que tocan las paredes (m)
 * @param baseboard    largo del zócalo: perímetro más el contorno de las columnas sueltas, menos las puertas (m)
 * @param volume       volumen interior (m³)
 */
public record Quantities(double floorArea, Map<WallSide, Double> wallNet, List<ColumnFaces> columnFaces,
                         double openingsArea, double perimeter, double baseboard, double volume) {

    /** Caras a la vista de una columna: área a pintar (m²) y largo de su contorno a la vista (m). */
    public record ColumnFaces(Column column, double area, double exposedLength) {}

    /** A esta distancia (m) se considera que una columna toca una pared. */
    private static final double TOUCH = 0.005;

    public static Quantities of(Project p) {
        RoomDims r = p.room;
        double h = r.height();
        Map<WallSide, Double> net = new EnumMap<>(WallSide.class);
        double openings = 0, doors = 0;
        for (WallSide side : WallSide.values()) net.put(side, r.wallLength(side) * h);
        for (Opening o : p.openings) {
            double area = o.width * o.height;
            openings += area;
            net.merge(o.wall, -area, Double::sum);
            if (o.type == OpeningType.DOOR) doors += o.width;
        }

        double floor = r.width() * r.length();
        double perimeter = 2 * (r.width() + r.length());
        double looseOutline = 0; // contorno de las columnas que no tocan ninguna pared (el zócalo las rodea)
        List<ColumnFaces> faces = new ArrayList<>();
        for (Column c : p.columns) {
            double w = c.width, d = c.depth;
            floor -= w * d;
            boolean west = c.minX() <= TOUCH, east = c.maxX() >= r.width() - TOUCH;
            boolean north = c.minZ() <= TOUCH, south = c.maxZ() >= r.length() - TOUCH;
            // Lo que tapa de cada pared y las caras que quedan a la vista.
            double covered = 0, exposed = 0;
            if (west) {
                net.merge(WallSide.OESTE, -d * h, Double::sum);
                covered += d;
            } else {
                exposed += d;
            }
            if (east) {
                net.merge(WallSide.ESTE, -d * h, Double::sum);
                covered += d;
            } else {
                exposed += d;
            }
            if (north) {
                net.merge(WallSide.NORTE, -w * h, Double::sum);
                covered += w;
            } else {
                exposed += w;
            }
            if (south) {
                net.merge(WallSide.SUR, -w * h, Double::sum);
                covered += w;
            } else {
                exposed += w;
            }
            faces.add(new ColumnFaces(c, exposed * h, exposed));
            if (covered > 0) perimeter += exposed - covered;
            else looseOutline += exposed;
        }
        return new Quantities(floor, net, List.copyOf(faces), openings, perimeter,
                Math.max(0, perimeter + looseOutline - doors), floor * h);
    }

    /** Área de las cuatro paredes sin las aberturas ni lo tapado por columnas (m²). */
    public double wallNetTotal() {
        double total = 0;
        for (double a : wallNet.values()) total += a;
        return total;
    }

    /** Área de las caras a la vista de todas las columnas (m²). */
    public double columnFacesTotal() {
        double total = 0;
        for (ColumnFaces f : columnFaces) total += f.area();
        return total;
    }
}
