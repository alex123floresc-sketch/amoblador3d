package pe.edu.unaj.amoblador3d.persistence;

/** El archivo o los datos guardados no tienen la forma esperada. El mensaje es para el usuario. */
public class ProjectFormatException extends Exception {
    public ProjectFormatException(String message) {
        super(message);
    }
}
