package pe.edu.unaj.amoblador3d.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Todo el contenido de un proyecto.
 *
 * <p>Sistema de coordenadas (en metros): el origen es la esquina interior noroeste a nivel
 * del piso; X crece hacia el este, Z hacia el sur e Y hacia arriba. El interior va de
 * x = 0 a ancho, z = 0 a largo, y = 0 a alto.
 */
public final class Project {
    public static final double DEFAULT_AMBIENT = 0.6;

    public RoomDims room = RoomDims.DEFAULT;
    public double ambient = DEFAULT_AMBIENT;
    public Surface floor = Surface.floor();
    public Surface ceiling = Surface.ceiling();
    public Baseboard baseboard = new Baseboard();
    public final Map<WallSide, Surface> walls = new EnumMap<>(WallSide.class);
    public final List<Opening> openings = new ArrayList<>();
    public final List<Light> lights = new ArrayList<>();
    public final List<Furniture> furniture = new ArrayList<>();
    /** Columnas, muros internos y recortes en L. */
    public final List<Column> columns = new ArrayList<>();
    /** Vistas de cámara guardadas por el usuario. */
    public final List<CameraView> views = new ArrayList<>();

    public Project() {
        for (WallSide side : WallSide.values()) walls.put(side, Surface.wall(side));
    }

    /** Piso, las 4 paredes y el techo, en ese orden. */
    public List<Surface> surfaces() {
        List<Surface> all = new ArrayList<>();
        all.add(floor);
        all.addAll(walls.values());
        all.add(ceiling);
        return all;
    }

    public Project copy() {
        Project p = new Project();
        p.room = room;
        p.ambient = ambient;
        p.floor = floor.copy();
        p.ceiling = ceiling.copy();
        p.baseboard = baseboard.copy();
        for (Map.Entry<WallSide, Surface> e : walls.entrySet()) p.walls.put(e.getKey(), e.getValue().copy());
        for (Opening o : openings) p.openings.add(o.copy());
        for (Light l : lights) p.lights.add(l.copy());
        for (Furniture f : furniture) p.furniture.add(f.copy());
        p.views.addAll(views); // CameraView es inmutable
        for (Column c : columns) p.columns.add(c.copy());
        return p;
    }
}
