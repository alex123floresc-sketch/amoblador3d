package pe.edu.unaj.amoblador3d.ui;

import java.util.Optional;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;
import pe.edu.unaj.amoblador3d.design.FSHFDesign;
import pe.edu.unaj.amoblador3d.model.FSHFColumn;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;
import pe.edu.unaj.amoblador3d.util.FSHFUnits;

/** "Habitación en L": qué esquina se quita y de qué tamaño. */
final class FSHFNotchDialog {
    record Notch(FSHFColumn.Corner corner, double width, double depth) {}

    private FSHFNotchDialog() {}

    static Optional<Notch> show(Window owner, FSHFRoomDims room) {
        ComboBox<FSHFColumn.Corner> corner = new ComboBox<>();
        corner.getItems().setAll(FSHFColumn.Corner.values());
        corner.setValue(FSHFColumn.Corner.NORESTE);
        corner.setConverter(new StringConverter<>() {
            @Override
            public String toString(FSHFColumn.Corner c) {
                return c == null ? "" : c.label();
            }

            @Override
            public FSHFColumn.Corner fromString(String s) {
                return null;
            }
        });
        TextField width = new TextField(FSHFUnits.number(Math.min(FSHFColumn.Kind.NOTCH.defaultWidth(), room.width() - FSHFDesign.MIN_LEFT)));
        TextField depth = new TextField(FSHFUnits.number(Math.min(FSHFColumn.Kind.NOTCH.defaultDepth(), room.length() - FSHFDesign.MIN_LEFT)));
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.addRow(0, new Label("Esquina"), corner);
        grid.addRow(1, new Label("Ancho a quitar (X)"), width, new Label("m"));
        grid.addRow(2, new Label("Largo a quitar (Z)"), depth, new Label("m"));
        Label hint = new Label("La esquina se rellena con muro (del mismo acabado que las paredes) y se descuenta del piso. "
                + "Las medidas se pueden cambiar después en el panel.");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);
        hint.setMaxWidth(360);
        Label error = new Label();
        error.getStyleClass().add("error");
        error.setWrapText(true);
        error.setMaxWidth(360);

        Dialog<Notch> dialog = new Dialog<>();
        FSHFDialogs.prepare(dialog, owner, "Habitación en L", "Quitar una esquina de la habitación");
        ButtonType ok = new ButtonType("Aplicar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(new VBox(10, grid, hint, error));
        // No se cierra si las medidas no son números válidos.
        dialog.getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            Double w = FSHFUnits.parse(width.getText()), d = FSHFUnits.parse(depth.getText());
            String err = w == null || d == null ? "Escribe las medidas como números (por ejemplo 1,2)."
                    : FSHFUnits.checkRange("El ancho", w, FSHFDesign.MIN_COLUMN, room.width() - FSHFDesign.MIN_LEFT);
            if (err == null) err = FSHFUnits.checkRange("El largo", d, FSHFDesign.MIN_COLUMN, room.length() - FSHFDesign.MIN_LEFT);
            if (err != null) {
                error.setText(err);
                e.consume();
            }
        });
        dialog.setResultConverter(b -> b == ok
                ? new Notch(corner.getValue(), FSHFUnits.parse(width.getText()), FSHFUnits.parse(depth.getText())) : null);
        return dialog.showAndWait();
    }
}
