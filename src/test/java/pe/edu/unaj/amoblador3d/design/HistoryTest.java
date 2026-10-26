package pe.edu.unaj.amoblador3d.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HistoryTest {
    private String state = "a";

    @Test
    void deshacerRehacerYCambiosSinGuardar() {
        History h = new History(() -> state, s -> state = s, () -> {});
        assertFalse(h.isDirty());
        state = "b";
        h.commit();
        state = "c";
        h.commit();
        assertTrue(h.isDirty());
        assertTrue(h.undo());
        assertEquals("b", state);
        assertTrue(h.redo());
        assertEquals("c", state);
        h.markSaved();
        assertFalse(h.isDirty());
        h.undo();
        assertTrue(h.isDirty());
    }

    @Test
    void loRecuperadoQuedaSinGuardarHastaGuardarlo() {
        History h = new History(() -> state, s -> state = s, () -> {});
        h.reset();
        assertFalse(h.isDirty());
        h.markUnsaved();
        assertTrue(h.isDirty());
        h.markSaved();
        assertFalse(h.isDirty());
    }

    @Test
    void unCambioNuevoDescartaElRehacer() {
        History h = new History(() -> state, s -> state = s, () -> {});
        state = "b";
        h.commit();
        h.undo();
        state = "x";
        h.commit();
        assertFalse(h.canRedo());
    }

    @Test
    void sinCambiosNoSeRegistraUnPaso() {
        History h = new History(() -> state, s -> state = s, () -> {});
        h.commit();
        assertFalse(h.canUndo());
    }
}
