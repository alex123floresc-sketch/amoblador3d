package pe.edu.unaj.amoblador3d.estimate;

import java.util.EnumMap;
import java.util.Map;
import pe.edu.unaj.amoblador3d.model.Opening;
import pe.edu.unaj.amoblador3d.model.OpeningType;
import pe.edu.unaj.amoblador3d.model.Project;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.model.WallSide;

/**
 * Metrado de la habitación: áreas y longitudes que se necesitan para comprar los acabados.
 * Todas las medidas son interiores (lo que se pinta o se cubre), en metros.
 *
 * @param floorArea      área del piso (m²)
 * @param wallNet        área de cada pared descontando sus ventanas y puertas (m²)
 * @param openingsArea   área total de ventanas y puertas (m²)
 * @param perimeter      perímetro interior (m)
 * @param baseboard      largo del zócalo: el perímetro menos el ancho de las puertas (m)
 * @param volume         volumen interior (m³)
 */
public record Quantities(double floorArea, Map<WallSide, Double> wallNet, double openingsArea,
                         double perimeter, double baseboard, double volume) {

    public static Quantities of(Project p) {
        RoomDims r = p.room;
        Map<WallSide, Double> net = new EnumMap<>(WallSide.class);
        double openings = 0, doors = 0;
        for (WallSide side : WallSide.values()) net.put(side, r.wallLength(side) * r.height());
        for (Opening o : p.openings) {
            double area = o.width * o.height;
            openings += area;
            net.merge(o.wall, -area, Double::sum);
            if (o.type == OpeningType.DOOR) doors += o.width;
        }
        double perimeter = 2 * (r.width() + r.length());
        return new Quantities(r.width() * r.length(), net, openings, perimeter,
                Math.max(0, perimeter - doors), r.width() * r.length() * r.height());
    }

    /** Área de las cuatro paredes sin las aberturas (m²). */
    public double wallNetTotal() {
        double total = 0;
        for (double a : wallNet.values()) total += a;
        return total;
    }
}
