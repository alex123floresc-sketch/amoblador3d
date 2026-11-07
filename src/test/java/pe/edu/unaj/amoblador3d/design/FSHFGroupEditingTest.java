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
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.model.FSHFFootprint;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;
import pe.edu.unaj.amoblador3d.model.FSHFProject;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;
import pe.edu.unaj.amoblador3d.persistence.FSHFProjectJson;
import pe.edu.unaj.amoblador3d.util.FSHFOutcome;

class FSHFGroupEditingTest {
    private FSHFDesign design;
    private final List<String> warnings = new ArrayList<>();

    @BeforeEach
    void setUp() {
        design = new FSHFDesign(new FSHFCatalog());
        design.setWarningHandler(warnings::add);
        FSHFProject p = new FSHFProject();
        p.room = new FSHFRoomDims(5, 4, 2.5, 0.15);
        design.load(p);
    }

    private FSHFFurniture add(String type, double x, double z) {
        FSHFFurniture f = design.addFurniture(type).value();
        assertNull(design.changeFurniture(f, at(f, x, z), true));
        return f;
    }

    private static FSHFFurniture at(FSHFFurniture f, double x, double z) {
        FSHFFurniture c = f.copy();
        c.x = x;
        c.z = z;
        return c;
    }

    @Test
    void elImanPegaElMuebleALaPared() {
        FSHFFurniture table = add("mesa", 2.5, 2);

        List<FSHFSnapper.Guide> guides = design.drag(List.of(table), table, 0.65, 2, true);
        assertNotNull(guides);
        assertEquals(0.6, table.x, 1e-9);
        assertFalse(guides.isEmpty());

        design.drag(List.of(table), table, 0.67, 2, false);
        assertEquals(0.67, table.x, 1e-9);
    }

    @Test
    void elImanPoneUnMuebleJuntoAOtro() {
        FSHFFurniture a = add("mesa", 1.5, 2);
        FSHFFurniture b = add("silla", 3.5, 2);

        design.drag(List.of(b), b, 2.1 + 0.225 + 0.04, 2.3, true);

        assertEquals(2.1 + 0.225, b.x, 0.0051);
        assertFalse(FSHFFootprint.of(a).overlaps(FSHFFootprint.of(b)), "pegados pero sin superponerse");
    }

    @Test
    void elGrupoSeMueveJuntoOno() {
        FSHFFurniture a = add("silla", 1, 1), b = add("silla", 2, 1);
        FSHFFurniture wall = add("closet", 4, 3.6);
        assertNull(design.moveGroup(List.of(a, b), 0.5, 0.5));
        assertEquals(1.5, a.x, 1e-9);
        assertEquals(2.5, b.x, 1e-9);

        String err = design.moveGroup(List.of(a, b), 1.5, 2.1);
        assertNotNull(err);
        assertEquals(1.5, a.x, 1e-9);
        assertEquals(1.5, a.z, 1e-9);
        assertNull(design.checkFurniture(wall, wall));
    }

    @Test
    void alGirarElGrupoSeConservaLaDistribucion() {
        FSHFFurniture a = add("silla", 2, 2), b = add("silla", 3, 2);
        double before = Math.hypot(a.x - b.x, a.z - b.z);
        assertNull(design.rotateGroup(List.of(a, b), 90));
        assertEquals(before, Math.hypot(a.x - b.x, a.z - b.z), 0.011);
        assertEquals(90, a.rotation, 1e-9);

        assertEquals(a.x, b.x, 0.011);
    }

    @Test
    void unMuebleBloqueadoNoSeMueveNiSeElimina() {
        FSHFFurniture bed = add("cama", 2.5, 2);
        design.setLocked(List.of(bed), true);
        assertFalse(design.moveFurniture(bed, 1.5, 2));
        assertNull(design.drag(List.of(bed), bed, 1.5, 2, true));
        assertNotNull(design.rotateFurniture(bed, 90));
        assertNotNull(design.remove(bed));
        assertTrue(design.furniture().contains(bed));
        FSHFEditable editor = FSHFEditors.of(design, bed, f -> null);
        assertFalse(editor.isEnabled("x"));
        assertTrue(editor.isEnabled("locked"));
        assertNull(editor.set("locked", false));
        assertNull(design.remove(bed));
    }

    @Test
    void duplicarUnGrupoConservaComoEstanUbicados() {
        FSHFFurniture a = add("silla", 1, 1), b = add("silla", 1.6, 1);
        FSHFOutcome<List<FSHFFurniture>> out = design.duplicateGroup(List.of(a, b));
        assertFalse(out.failed(), out.error());
        List<FSHFFurniture> copies = out.value();
        assertEquals(0.6, copies.get(1).x - copies.get(0).x, 1e-9);
        assertEquals(copies.get(0).z, copies.get(1).z, 1e-9);
        assertEquals(4, design.furniture().size());
        for (FSHFFurniture f : design.furniture()) assertNull(design.checkFurniture(f, f));
    }

    @Test
    void copiarYPegarEnOtroProyecto() {
        FSHFFurniture bed = add("cama", 2.5, 2);
        design.setLocked(List.of(bed), true);
        String clipboard = FSHFProjectJson.writeClipboard(List.of(bed));

        FSHFDesign other = new FSHFDesign(new FSHFCatalog());
        other.loadDefault();
        List<FSHFFurniture> items = FSHFProjectJson.readClipboard(clipboard);
        assertNotNull(items);
        FSHFOutcome<List<FSHFFurniture>> out = other.paste(items);
        assertFalse(out.failed(), out.error());
        FSHFFurniture pasted = out.value().get(0);
        assertEquals("cama", pasted.type);
        assertFalse(pasted.locked, "lo pegado se puede mover");
        assertNull(other.checkFurniture(pasted, pasted));

        FSHFFurniture again = other.paste(items).value().get(0);
        assertFalse(FSHFFootprint.of(pasted).overlaps(FSHFFootprint.of(again)));
        assertTrue(!pasted.id.equals(again.id));
    }

    @Test
    void elPortapapelesIgnoraTextoAjeno() {
        assertNull(FSHFProjectJson.readClipboard("hola"));
        assertNull(FSHFProjectJson.readClipboard("{\"app\":\"Amoblador3D\",\"version\":1}"));
        assertNull(FSHFProjectJson.readClipboard("{\"app\":\"Amoblador3D\",\"clipboard\":\"furniture\",\"furniture\":[{\"id\":\"x\"}]}"));
    }

    @Test
    void siNoCabeAlPegarSeExplica() {
        FSHFFurniture closet = add("closet", 2.5, 2);
        closet.height = 2.4;
        List<FSHFFurniture> items = FSHFProjectJson.readClipboard(FSHFProjectJson.writeClipboard(List.of(closet)));
        FSHFDesign small = new FSHFDesign(new FSHFCatalog());
        FSHFProject p = new FSHFProject();
        p.room = new FSHFRoomDims(1, 1, 2, 0.15);
        small.load(p);
        FSHFOutcome<List<FSHFFurniture>> out = small.paste(items);
        assertTrue(out.failed());
        assertTrue(out.error().contains("No hay espacio"), out.error());
    }
}
