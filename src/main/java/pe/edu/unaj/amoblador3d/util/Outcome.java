package pe.edu.unaj.amoblador3d.util;

/**
 * Resultado de una operación que puede fallar con un mensaje para el usuario
 * (por ejemplo, "No hay espacio libre para la cama").
 */
public record Outcome<T>(T value, String error) {
    public static <T> Outcome<T> ok(T value) {
        return new Outcome<>(value, null);
    }

    public static <T> Outcome<T> fail(String error) {
        return new Outcome<>(null, error);
    }

    public boolean failed() {
        return error != null;
    }
}
