package pe.edu.unaj.amoblador3d.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.model.Footprint;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.Project;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.persistence.ProjectJson;
import pe.edu.unaj.amoblador3d.util.Outcome;

/** Imán, grupos de muebles, bloqueo y copiar/pegar. */
class GroupEditingTest {
    private Design design;
    private final List<String> warnings = new ArrayList<>();

    /** Habitación vacía de 5 x 4 m (sin puertas, para que no estorbe su zona de giro). */
    @BeforeEach
    void setUp() {
        design = new Design(new Catalog());
        design.setWarningHandler(warnings::add);
        Project p = new Project();
        p.room = new RoomDims(5, 4, 2.5, 0.15);
        design.load(p);
    }

    private Furniture add(String type, double x, double z) {
        Furniture f = design.addFurniture(type).value();
        assertNull(design.changeFurniture(f, at(f, x, z), true));
        return f;
    }

    private static Furniture at(Furniture f, double x, double z) {
        Furniture c = f.copy();
        c.x = x;
        c.z = z;
        return c;
    }

    @Test
    void elImanPegaElMuebleALaPared() {
        Furniture table = add("mesa", 2.5, 2); // 1.2 x 0.8
        // Se suelta a 5 cm de la pared oeste: el borde queda en x = 0.05 -> se pega a x = 0.
        List<Snapper.Guide> guides = design.drag(List.of(table), table, 0.65, 2, true);
        assertNotNull(guides);
        assertEquals(0.6, table.x, 1e-9);
        assertFalse(guides.isEmpty());
        // Sin imán se queda donde se soltó.
        design.drag(List.of(table), table, 0.67, 2, false);
        assertEquals(0.67, table.x, 1e-9);
    }

    @Test
    void elImanPoneUnMuebleJuntoAOtro() {
        Furniture a = add("mesa", 1.5, 2); // x de 0.9 a 2.1
        Furniture b = add("silla", 3.5, 2); // 0.45 de ancho
        // La silla se suelta con su borde izquierdo a 4 cm del borde derecho de la mesa.
        design.drag(List.of(b), b, 2.1 + 0.225 + 0.04, 2.3, true);
        // Las posiciones van al centímetro: con 0.45 m de ancho queda a lo sumo a 5 mm.
        assertEquals(2.1 + 0.225, b.x, 0.0051);
        assertFalse(Footprint.of(a).overlaps(Footprint.of(b)), "pegados pero sin superponerse");
    }

    @Test
    void elGrupoSeMueveJuntoOno() {
        Furniture a = add("silla", 1, 1), b = add("silla", 2, 1);
        Furniture wall = add("closet", 4, 3.6);
        assertNull(design.moveGroup(List.of(a, b), 0.5, 0.5));
        assertEquals(1.5, a.x, 1e-9);
        assertEquals(2.5, b.x, 1e-9);
        // Si uno chocaría, no se mueve ninguno.
        String err = design.moveGroup(List.of(a, b), 1.5, 2.1);
        assertNotNull(err);
        assertEquals(1.5, a.x, 1e-9);
        assertEquals(1.5, a.z, 1e-9);
        assertNull(design.checkFurniture(wall, wall));
    }

    @Test
    void alGirarElGrupoSeConservaLaDistribucion() {
        Furniture a = add("silla", 2, 2), b = add("silla", 3, 2);
        double before = Math.hypot(a.x - b.x, a.z - b.z);
        assertNull(design.rotateGroup(List.of(a, b), 90));
        assertEquals(before, Math.hypot(a.x - b.x, a.z - b.z), 0.011);
        assertEquals(90, a.rotation, 1e-9);
        // Al girar 90° quedan uno detrás del otro (misma X).
        assertEquals(a.x, b.x, 0.011);
    }

    @Test
    void unMuebleBloqueadoNoSeMueveNiSeElimina() {
        Furniture bed = add("cama", 2.5, 2);
        design.setLocked(List.of(bed), true);
        assertFalse(design.moveFurniture(bed, 1.5, 2));
        assertNull(design.drag(List.of(bed), bed, 1.5, 2, true));
        assertNotNull(design.rotateFurniture(bed, 90));
        assertNotNull(design.remove(bed));
        assertTrue(design.furniture().contains(bed));
        Editable editor = Editors.of(design, bed, f -> null);
        assertFalse(editor.isEnabled("x"));
        assertTrue(editor.isEnabled("locked"));
        assertNull(editor.set("locked", false));
        assertNull(design.remove(bed));
    }

    @Test
    void duplicarUnGrupoConservaComoEstanUbicados() {
        Furniture a = add("silla", 1, 1), b = add("silla", 1.6, 1);
        Outcome<List<Furniture>> out = design.duplicateGroup(List.of(a, b));
        assertFalse(out.failed(), out.error());
        List<Furniture> copies = out.value();
        assertEquals(0.6, copies.get(1).x - copies.get(0).x, 1e-9);
        assertEquals(copies.get(0).z, copies.get(1).z, 1e-9);
        assertEquals(4, design.furniture().size());
        for (Furniture f : design.furniture()) assertNull(design.checkFurniture(f, f));
    }

    @Test
    void copiarYPegarEnOtroProyecto() {
        Furniture bed = add("cama", 2.5, 2);
        design.setLocked(List.of(bed), true);
        String clipboard = ProjectJson.writeClipboard(List.of(bed));

        Design other = new Design(new Catalog());
        other.loadDefault();
        List<Furniture> items = ProjectJson.readClipboard(clipboard);
        assertNotNull(items);
        Outcome<List<Furniture>> out = other.paste(items);
        assertFalse(out.failed(), out.error());
        Furniture pasted = out.value().get(0);
        assertEquals("cama", pasted.type);
        assertFalse(pasted.locked, "lo pegado se puede mover");
        assertNull(other.checkFurniture(pasted, pasted));
        // Pegar otra vez no lo pone encima del primero.
        Furniture again = other.paste(items).value().get(0);
        assertFalse(Footprint.of(pasted).overlaps(Footprint.of(again)));
        assertTrue(!pasted.id.equals(again.id));
    }

    @Test
    void elPortapapelesIgnoraTextoAjeno() {
        assertNull(ProjectJson.readClipboard("hola"));
        assertNull(ProjectJson.readClipboard("{\"app\":\"Amoblador3D\",\"version\":1}"));
        assertNull(ProjectJson.readClipboard("{\"app\":\"Amoblador3D\",\"clipboard\":\"furniture\",\"furniture\":[{\"id\":\"x\"}]}"));
    }

    @Test
    void siNoCabeAlPegarSeExplica() {
        Furniture closet = add("closet", 2.5, 2);
        closet.height = 2.4;
        List<Furniture> items = ProjectJson.readClipboard(ProjectJson.writeClipboard(List.of(closet)));
        Design small = new Design(new Catalog());
        Project p = new Project();
        p.room = new RoomDims(1, 1, 2, 0.15); // más bajo que el closet
        small.load(p);
        Outcome<List<Furniture>> out = small.paste(items);
        assertTrue(out.failed());
        assertTrue(out.error().contains("No hay espacio"), out.error());
    }
}
