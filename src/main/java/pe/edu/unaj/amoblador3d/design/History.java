package pe.edu.unaj.amoblador3d.design;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Deshacer / rehacer con "fotos" (snapshots) del proyecto.
 *
 * <p>Después de cada cambio se guarda el proyecto completo como texto JSON. Deshacer = volver a
 * cargar la foto anterior. Es más simple y robusto que programar la operación inversa de cada
 * acción, y como un proyecto pesa pocos KB, guardar 100 fotos no es problema.
 */
public final class History {
    private static final int MAX_STEPS = 100;

    private final Supplier<String> capture;
    private final Consumer<String> restore;
    private final Runnable onChange;
    private final List<String> stack = new ArrayList<>();
    private int index;
    private String saved;
    private boolean pending;

    /**
     * @param capture  devuelve el estado actual como texto
     * @param restore  aplica un estado guardado
     * @param onChange se llama cuando cambian deshacer/rehacer/cambios sin guardar
     */
    public History(Supplier<String> capture, Consumer<String> restore, Runnable onChange) {
        this.capture = capture;
        this.restore = restore;
        this.onChange = onChange;
        start();
    }

    private void start() {
        stack.clear();
        stack.add(capture.get());
        index = 0;
        saved = stack.get(0);
        pending = false;
    }

    /** Empieza un historial nuevo (al abrir o crear un proyecto). */
    public void reset() {
        start();
        onChange.run();
    }

    /** Registra el estado actual como un paso nuevo (si cambió algo). */
    public void commit() {
        pending = false;
        String snapshot = capture.get();
        if (snapshot.equals(stack.get(index))) return;
        stack.subList(index + 1, stack.size()).clear(); // un cambio nuevo descarta el "rehacer"
        stack.add(snapshot);
        if (stack.size() > MAX_STEPS) stack.remove(0);
        index = stack.size() - 1;
        onChange.run();
    }

    /**
     * Indica que hay un cambio que se registrará en breve (quien llama programa el
     * {@link #commit()}); sirve para agrupar cambios seguidos, como al elegir un color.
     */
    public void markPending() {
        pending = true;
    }

    /** Si hay un cambio pendiente, lo registra ya. */
    public void flush() {
        if (pending) commit();
    }

    public boolean undo() {
        flush();
        if (index == 0) return false;
        index--;
        restore.accept(stack.get(index));
        onChange.run();
        return true;
    }

    public boolean redo() {
        flush();
        if (index >= stack.size() - 1) return false;
        index++;
        restore.accept(stack.get(index));
        onChange.run();
        return true;
    }

    /** Marca el estado actual como "guardado". */
    public void markSaved() {
        flush();
        saved = stack.get(index);
        onChange.run();
    }

    public boolean isDirty() {
        return pending || !stack.get(index).equals(saved);
    }

    public boolean canUndo() {
        return index > 0 || pending;
    }

    public boolean canRedo() {
        return index < stack.size() - 1;
    }
}
