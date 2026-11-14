package pe.edu.unaj.amoblador3d.design;

import java.util.List;
import java.util.function.Supplier;

/**
 * Contrato de todo elemento que se puede editar en el panel de propiedades: título,
 * campos, lectura y escritura de valores y acciones disponibles.
 */
public interface FSHFEditable {
    Object entity();

    String title();

    List<FSHFField> fields();

    Object get(String key);

    String set(String key, Object value);

    default List<Action> actions() {
        return List.of();
    }

    default boolean isEnabled(String key) {
        return true;
    }

    default boolean showDelete() {
        return !(entity() instanceof pe.edu.unaj.amoblador3d.model.FSHFSurface);
    }

    default boolean canDelete() {
        return true;
    }

    record Action(String label, String tooltip, Supplier<String> run, boolean enabled) {
        public Action(String label, String tooltip, Supplier<String> run) {
            this(label, tooltip, run, true);
        }
    }
}
