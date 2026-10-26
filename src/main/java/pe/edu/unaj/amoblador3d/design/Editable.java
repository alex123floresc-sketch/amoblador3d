package pe.edu.unaj.amoblador3d.design;

import java.util.List;
import java.util.function.Supplier;

/** Lo que el panel de propiedades necesita saber de la entidad seleccionada. */
public interface Editable {
    Object entity();

    String title();

    List<Field> fields();

    /** Valor actual: Double (número), String (lista o color) o Boolean (casilla). */
    Object get(String key);

    /** Aplica un cambio. Devuelve un mensaje de error o null si se aplicó. */
    String set(String key, Object value);

    /** Botones extra (girar, duplicar...). */
    default List<Action> actions() {
        return List.of();
    }

    /** false si el campo se muestra pero no se puede cambiar (p. ej. un mueble bloqueado). */
    default boolean isEnabled(String key) {
        return true;
    }

    /** Si se muestra el botón "Eliminar" habilitado. */
    default boolean canDelete() {
        return true;
    }

    /**
     * @param run     devuelve un mensaje de error o null
     * @param enabled false: el botón se muestra deshabilitado
     */
    record Action(String label, String tooltip, Supplier<String> run, boolean enabled) {
        public Action(String label, String tooltip, Supplier<String> run) {
            this(label, tooltip, run, true);
        }
    }
}
