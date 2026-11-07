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
public final class FSHFProject {
    public static final double DEFAULT_AMBIENT = 0.6;

    public FSHFRoomDims room = FSHFRoomDims.DEFAULT;
    public double ambient = DEFAULT_AMBIENT;
    public FSHFSurface floor = FSHFSurface.floor();
    public FSHFSurface ceiling = FSHFSurface.ceiling();
    public FSHFBaseboard baseboard = new FSHFBaseboard();
    public final Map<FSHFWallSide, FSHFSurface> walls = new EnumMap<>(FSHFWallSide.class);
    public final List<FSHFOpening> openings = new ArrayList<>();
    public final List<FSHFLight> lights = new ArrayList<>();
    public final List<FSHFFurniture> furniture = new ArrayList<>();
    /** Columnas, muros internos y recortes en L. */
    public final List<FSHFColumn> columns = new ArrayList<>();
    /** Vistas de cámara guardadas por el usuario. */
    public final List<FSHFCameraView> views = new ArrayList<>();

    public FSHFProject() {
        for (FSHFWallSide side : FSHFWallSide.values()) walls.put(side, FSHFSurface.wall(side));
    }

    /** Piso, las 4 paredes y el techo, en ese orden. */
    public List<FSHFSurface> surfaces() {
        List<FSHFSurface> all = new ArrayList<>();
        all.add(floor);
        all.addAll(walls.values());
        all.add(ceiling);
        return all;
    }

    public FSHFProject copy() {
        FSHFProject p = new FSHFProject();
        p.room = room;
        p.ambient = ambient;
        p.floor = floor.copy();
        p.ceiling = ceiling.copy();
        p.baseboard = baseboard.copy();
        for (Map.Entry<FSHFWallSide, FSHFSurface> e : walls.entrySet()) p.walls.put(e.getKey(), e.getValue().copy());
        for (FSHFOpening o : openings) p.openings.add(o.copy());
        for (FSHFLight l : lights) p.lights.add(l.copy());
        for (FSHFFurniture f : furniture) p.furniture.add(f.copy());
        p.views.addAll(views); // FSHFCameraView es inmutable
        for (FSHFColumn c : columns) p.columns.add(c.copy());
        return p;
    }
}
