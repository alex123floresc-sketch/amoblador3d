package pe.edu.unaj.amoblador3d.ui;

import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;

/** Mensajes y preguntas al usuario, todos con el mismo estilo. */
final class Dialogs {
    static final ButtonType SAVE = new ButtonType("Guardar", ButtonBar.ButtonData.YES);
    static final ButtonType DISCARD = new ButtonType("No guardar", ButtonBar.ButtonData.NO);

    private Dialogs() {}

    static void error(Window owner, String header, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        prepare(a, owner, "Error", header);
        a.showAndWait();
    }

    static void info(Window owner, String title, String header, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        prepare(a, owner, title, header);
        a.showAndWait();
    }

    static boolean confirm(Window owner, String header, String message, String okLabel) {
        ButtonType ok = new ButtonType(okLabel, ButtonBar.ButtonData.OK_DONE);
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ok, ButtonType.CANCEL);
        prepare(a, owner, "Confirmar", header);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ok;
    }

    /** "¿Guardar los cambios?" Devuelve SAVE, DISCARD o CANCEL. */
    static ButtonType unsavedChanges(Window owner, String name) {
        Alert a = new Alert(Alert.AlertType.WARNING, "Si no los guardas, se perderán.", SAVE, DISCARD, ButtonType.CANCEL);
        prepare(a, owner, "Cambios sin guardar", "¿Guardar los cambios de \"" + name + "\"?");
        return a.showAndWait().orElse(ButtonType.CANCEL);
    }

    static Optional<String> askText(Window owner, String title, String header, String initial) {
        TextInputDialog d = new TextInputDialog(initial);
        prepare(d, owner, title, header);
        d.setContentText("Nombre:");
        return d.showAndWait().map(String::trim).filter(s -> !s.isEmpty());
    }

    static void prepare(Dialog<?> d, Window owner, String title, String header) {
        d.initOwner(owner);
        d.setTitle(title);
        d.setHeaderText(header);
        d.getDialogPane().getStylesheets().add(MainWindow.STYLESHEET);
        // Sin esto, JavaFX corta los mensajes largos con "..." en lugar de agrandar la ventana.
        d.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        d.getDialogPane().setMinWidth(420);
        d.setResizable(true);
    }
}
