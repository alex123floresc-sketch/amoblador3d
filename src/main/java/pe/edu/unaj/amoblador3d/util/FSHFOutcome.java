package pe.edu.unaj.amoblador3d.util;

/**
 * Resultado de una operación que puede fallar con un mensaje para el usuario
 * (por ejemplo, "No hay espacio libre para la cama").
 */
public record FSHFOutcome<T>(T value, String error) {
    public static <T> FSHFOutcome<T> ok(T value) {
        return new FSHFOutcome<>(value, null);
    }

    public static <T> FSHFOutcome<T> fail(String error) {
        return new FSHFOutcome<>(null, error);
    }

    public boolean failed() {
        return error != null;
    }
}
