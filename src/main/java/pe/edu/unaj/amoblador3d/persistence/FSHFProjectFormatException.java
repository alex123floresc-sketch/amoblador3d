package pe.edu.unaj.amoblador3d.persistence;

/**
 * Error que indica que un archivo de proyecto (.amob3d) no tiene el formato esperado.
 */
public class FSHFProjectFormatException extends Exception {
    private static final long serialVersionUID = 1L;

    public FSHFProjectFormatException(String message) {
        super(message);
    }
}
