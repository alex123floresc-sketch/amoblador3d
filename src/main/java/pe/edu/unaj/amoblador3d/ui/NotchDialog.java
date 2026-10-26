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
import pe.edu.unaj.amoblador3d.design.Design;
import pe.edu.unaj.amoblador3d.model.Column;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.util.Units;

/** "Habitación en L": qué esquina se quita y de qué tamaño. */
final class NotchDialog {
    record Notch(Column.Corner corner, double width, double depth) {}

    private NotchDialog() {}

    static Optional<Notch> show(Window owner, RoomDims room) {
        ComboBox<Column.Corner> corner = new ComboBox<>();
        corner.getItems().setAll(Column.Corner.values());
        corner.setValue(Column.Corner.NORESTE);
        corner.setConverter(new StringConverter<>() {
            @Override
            public String toString(Column.Corner c) {
                return c == null ? "" : c.label();
            }

            @Override
            public Column.Corner fromString(String s) {
                return null;
            }
        });
        TextField width = new TextField(Units.number(Math.min(Column.Kind.NOTCH.defaultWidth(), room.width() - Design.MIN_LEFT)));
        TextField depth = new TextField(Units.number(Math.min(Column.Kind.NOTCH.defaultDepth(), room.length() - Design.MIN_LEFT)));
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
        Dialogs.prepare(dialog, owner, "Habitación en L", "Quitar una esquina de la habitación");
        ButtonType ok = new ButtonType("Aplicar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(new VBox(10, grid, hint, error));
        // No se cierra si las medidas no son números válidos.
        dialog.getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            Double w = Units.parse(width.getText()), d = Units.parse(depth.getText());
            String err = w == null || d == null ? "Escribe las medidas como números (por ejemplo 1,2)."
                    : Units.checkRange("El ancho", w, Design.MIN_COLUMN, room.width() - Design.MIN_LEFT);
            if (err == null) err = Units.checkRange("El largo", d, Design.MIN_COLUMN, room.length() - Design.MIN_LEFT);
            if (err != null) {
                error.setText(err);
                e.consume();
            }
        });
        dialog.setResultConverter(b -> b == ok
                ? new Notch(corner.getValue(), Units.parse(width.getText()), Units.parse(depth.getText())) : null);
        return dialog.showAndWait();
    }
}
