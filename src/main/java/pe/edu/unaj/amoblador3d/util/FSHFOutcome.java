package pe.edu.unaj.amoblador3d.util;

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
