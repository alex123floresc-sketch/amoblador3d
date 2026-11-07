package pe.edu.unaj.amoblador3d.estimate;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import pe.edu.unaj.amoblador3d.model.FSHFColumn;
import pe.edu.unaj.amoblador3d.model.FSHFOpening;
import pe.edu.unaj.amoblador3d.model.FSHFProject;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;
import pe.edu.unaj.amoblador3d.model.FSHFWallSide;

public record FSHFQuantities(double floorArea, Map<FSHFWallSide, Double> wallNet, List<ColumnFaces> columnFaces,
                         double openingsArea, double perimeter, double baseboard, double volume) {
    public record ColumnFaces(FSHFColumn column, double area, double exposedLength) {}

    private static final double TOUCH = 0.005;

    public static FSHFQuantities of(FSHFProject p) {
        FSHFRoomDims r = p.room;
        double h = r.height();
        Map<FSHFWallSide, Double> net = new EnumMap<>(FSHFWallSide.class);
        double openings = 0, doors = 0;
        for (FSHFWallSide side : FSHFWallSide.values()) net.put(side, r.wallLength(side) * h);
        for (FSHFOpening o : p.openings) {
            double area = o.width * o.height;
            openings += area;
            net.merge(o.wall, -area, Double::sum);
            if (o.type.isDoor()) doors += o.width;
        }

        double floor = r.width() * r.length();
        double perimeter = 2 * (r.width() + r.length());
        double looseOutline = 0;
        List<ColumnFaces> faces = new ArrayList<>();
        for (FSHFColumn c : p.columns) {
            double w = c.width, d = c.depth;
            floor -= w * d;
            boolean west = c.minX() <= TOUCH, east = c.maxX() >= r.width() - TOUCH;
            boolean north = c.minZ() <= TOUCH, south = c.maxZ() >= r.length() - TOUCH;

            double covered = 0, exposed = 0;
            if (west) {
                net.merge(FSHFWallSide.OESTE, -d * h, Double::sum);
                covered += d;
            } else {
                exposed += d;
            }
            if (east) {
                net.merge(FSHFWallSide.ESTE, -d * h, Double::sum);
                covered += d;
            } else {
                exposed += d;
            }
            if (north) {
                net.merge(FSHFWallSide.NORTE, -w * h, Double::sum);
                covered += w;
            } else {
                exposed += w;
            }
            if (south) {
                net.merge(FSHFWallSide.SUR, -w * h, Double::sum);
                covered += w;
            } else {
                exposed += w;
            }
            faces.add(new ColumnFaces(c, exposed * h, exposed));
            if (covered > 0) perimeter += exposed - covered;
            else looseOutline += exposed;
        }
        return new FSHFQuantities(floor, net, List.copyOf(faces), openings, perimeter,
                Math.max(0, perimeter + looseOutline - doors), floor * h);
    }

    public double wallNetTotal() {
        double total = 0;
        for (double a : wallNet.values()) total += a;
        return total;
    }

    public double columnFacesTotal() {
        double total = 0;
        for (ColumnFaces f : columnFaces) total += f.area();
        return total;
    }
}
