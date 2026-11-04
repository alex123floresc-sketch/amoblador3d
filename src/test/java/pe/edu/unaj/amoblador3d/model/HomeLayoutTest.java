package pe.edu.unaj.amoblador3d.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Ambientes girados, en varios pisos, y puertas que dan de un ambiente a otro o a la calle. */
class HomeLayoutTest {
    private static final double EPS = 1e-9;

    private static Project room(double w, double l) {
        Project p = new Project();
        p.room = new RoomDims(w, l, 2.5, 0.15);
        return p;
    }

    /** Sala de 4 × 3,5 en el origen y un ambiente de 3 × 3,5 pegado muro con muro al este. */
    private static Home twoRooms(int rotation) {
        Home h = Home.of(room(4, 3.5), "Sala");
        h.spaces.get(0).project.openings.add(new Opening("door-1", OpeningType.DOOR, WallSide.ESTE, 1.0, 0.9, 2.1, 0));
        h.spaces.add(new Home.Space("ambiente-2", "Dormitorio", 4.3, 0, room(rotation % 180 == 0 ? 3 : 3.5,
                rotation % 180 == 0 ? 3.5 : 3), rotation, 0));
        return h;
    }

    @Test
    void girarCambiaLaPlantaYSeDeshaceExacto() {
        Project p = room(4, 3);
        for (int r = 0; r < 360; r += 90) {
            Home.Space s = new Home.Space("a", "A", 2, 5, p, r, 0);
            assertEquals(r % 180 == 0 ? 4 : 3, s.planWidth(), EPS);
            double[] plan = s.toPlan(1.2, 0.7);
            // El punto queda dentro de la planta girada y vuelve igual.
            assertTrue(plan[0] >= 2 && plan[0] <= 2 + s.planWidth() && plan[1] >= 5 && plan[1] <= 5 + s.planLength());
            assertArrayEquals(new double[] {1.2, 0.7}, s.toLocal(plan[0], plan[1]), EPS);
        }
        Home.Space quarter = new Home.Space("a", "A", 0, 0, p, 90, 0);
        assertEquals(WallSide.ESTE, quarter.planSide(WallSide.NORTE));
        assertEquals(WallSide.NORTE, quarter.planSide(WallSide.OESTE));
        // A 90° la esquina noroeste del ambiente queda arriba a la derecha de la planta.
        assertArrayEquals(new double[] {3, 0}, quarter.toPlan(0, 0), EPS);
    }

    @Test
    void elGiroSeRedondeaAUnCuarto() {
        assertEquals(90, Home.normalizeRotation(100));
        assertEquals(270, Home.normalizeRotation(-90));
        assertEquals(0, Home.normalizeRotation(360));
        assertEquals(0, Home.normalizeRotation(Double.NaN));
    }

    @Test
    void unaPuertaPegadaAOtroAmbienteLeAbrePaso() {
        Home h = twoRooms(0);
        Home.Space sala = h.spaces.get(0), dorm = h.spaces.get(1);
        List<Home.Link> links = h.links();
        assertEquals(1, links.size());
        Home.Link l = links.get(0);
        assertEquals(dorm, l.to());
        assertEquals(WallSide.OESTE, l.toWall());
        assertEquals(1.0, l.toOffset(), EPS);
        assertEquals(0.9, l.toWidth(), EPS);
        List<Opening> passages = h.passagesInto(dorm);
        assertEquals(1, passages.size());
        assertEquals(OpeningType.PASSAGE, passages.get(0).type);
        assertTrue(h.passagesInto(sala).isEmpty());
        assertTrue(h.exteriorDoors().isEmpty());
        // La pared este de la sala da al dormitorio; las otras tres, a la calle.
        assertEquals(List.of(WallSide.SUR, WallSide.OESTE, WallSide.NORTE), h.exteriorWalls(sala));
    }

    @Test
    void elPasoTambienSeEncuentraSiElVecinoEstaGirado() {
        Home h = twoRooms(180);
        Home.Space dorm = h.spaces.get(1);
        Home.Link l = h.links().get(0);
        assertEquals(dorm, l.to());
        // Girado 180°, su pared este es la que mira al oeste, y se mide desde el otro extremo.
        assertEquals(WallSide.ESTE, l.toWall());
        assertEquals(3.5 - 1.9, l.toOffset(), 1e-6);
        assertFalse(h.exteriorWalls(dorm).contains(WallSide.ESTE));
    }

    @Test
    void siYaHayUnaPuertaDelOtroLadoNoSeAbreOtroVano() {
        Home h = twoRooms(0);
        h.spaces.get(1).project.openings.add(new Opening("door-1", OpeningType.DOOR, WallSide.OESTE, 1.0, 0.9, 2.1, 0));
        assertTrue(h.passagesInto(h.spaces.get(1)).isEmpty());
    }

    @Test
    void unaPuertaQueNoTocaANadieDaAlExterior() {
        Home h = twoRooms(0);
        h.spaces.get(1).z = 10; // lejos
        assertEquals(1, h.exteriorDoors().size());
        assertTrue(h.passagesInto(h.spaces.get(1)).isEmpty());
    }

    @Test
    void losPisosSeApilanYNoChocanEntreSi() {
        Home h = twoRooms(0);
        Home.Space upstairs = new Home.Space("ambiente-3", "Estudio", 0, 0, room(4, 3.5), 0, 1);
        assertNull(h.collision(upstairs, 0, 0), "encima de la sala, en otro piso");
        assertNotNull(h.collision(upstairs, 0, 0, 0, 0, upstairs.project.room), "en el mismo piso, sí choca");
        // Un piso vacío empieza sobre la esquina del de abajo.
        assertArrayEquals(new double[] {0, 0}, h.freeSpot(upstairs.project, 1), EPS);
        h.spaces.add(upstairs);
        assertEquals(2, h.levels());
        assertEquals(2.5 + Home.SLAB, h.elevation(1), EPS);
        // Un ambiente de otro piso no abre paso ni tapa una pared.
        assertEquals(3, h.exteriorWalls(h.spaces.get(0)).size());
    }
}
