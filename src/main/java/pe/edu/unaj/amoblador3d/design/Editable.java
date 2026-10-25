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

    /** @param run devuelve un mensaje de error o null */
    record Action(String label, String tooltip, Supplier<String> run) {}
}
