package pe.edu.unaj.amoblador3d.ui;

import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry.Range;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry.Size;
import pe.edu.unaj.amoblador3d.gltf.ModelData;
import pe.edu.unaj.amoblador3d.util.Units;

/**
 * Datos de un modelo importado: nombre, giro para que su frente mire a +Z y medidas reales.
 * Propone las medidas del propio archivo; si el modelo venía en centímetros o pulgadas,
 * aquí se corrigen (con "Mantener proporciones" basta escribir una sola medida).
 */
final class ImportModelDialog {
    record Result(String name, Size size, double rotation) {}

    private ImportModelDialog() {}

    static Optional<Result> show(Window owner, String suggestedName, ModelData data) {
        Dialog<Result> dialog = new Dialog<>();
        Dialogs.prepare(dialog, owner, "Importar modelo 3D", "Agregar modelo al catálogo");
        ButtonType add = new ButtonType("Agregar al catálogo", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(add, ButtonType.CANCEL);

        TextField name = new TextField(suggestedName.length() > 40 ? suggestedName.substring(0, 40) : suggestedName);
        ComboBox<Integer> rotation = new ComboBox<>();
        rotation.getItems().addAll(0, 90, 180, 270);
        rotation.setValue(0);
        TextField width = new TextField(), depth = new TextField(), height = new TextField();
        CheckBox keep = new CheckBox("Mantener proporciones");
        keep.setSelected(true);
        Label sizeInfo = new Label();
        sizeInfo.getStyleClass().add("hint");
        sizeInfo.setWrapText(true);
        Label error = new Label();
        error.getStyleClass().add("error");
        error.setWrapText(true);

        // Medidas del archivo para el giro elegido (las proporciones de referencia).
        double[] base = new double[3];
        Runnable fromModel = () -> {
            ModelData.Box b = data.bounds(rotation.getValue());
            base[0] = Math.max(b.sizeX(), 1e-4);
            base[1] = Math.max(b.sizeZ(), 1e-4);
            base[2] = Math.max(b.sizeY(), 1e-4);
            sizeInfo.setText("Medidas en el archivo: " + Units.number(b.sizeX()) + " x " + Units.number(b.sizeZ())
                    + " x " + Units.number(b.sizeY()) + " (ancho x profundidad x alto). Si están en centímetros, corrígelas.");
            width.setText(Units.number(Units.round(base[0])));
            depth.setText(Units.number(Units.round(base[1])));
            height.setText(Units.number(Units.round(base[2])));
        };
        fromModel.run();
        rotation.valueProperty().addListener((obs, o, n) -> fromModel.run());

        // Con proporciones: al cambiar una medida se recalculan las otras dos.
        TextField[] fields = {width, depth, height};
        boolean[] syncing = {false};
        for (int i = 0; i < 3; i++) {
            int idx = i;
            fields[i].textProperty().addListener((obs, o, n) -> {
                if (syncing[0] || !keep.isSelected() || !fields[idx].isFocused()) return;
                Double v = Units.parse(n);
                if (v == null || v <= 0) return;
                double factor = v / base[idx];
                syncing[0] = true;
                for (int j = 0; j < 3; j++) if (j != idx) fields[j].setText(Units.number(Units.round(base[j] * factor)));
                syncing[0] = false;
            });
        }

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.addRow(0, new Label("Nombre"), name);
        grid.addRow(1, new Label("Giro inicial (°)"), rotation);
        grid.addRow(2, new Label("Ancho (X) m"), width);
        grid.addRow(3, new Label("Profundidad (Z) m"), depth);
        grid.addRow(4, new Label("Alto (Y) m"), height);
        grid.add(keep, 1, 5);
        Label hint = new Label("El frente del mueble debe mirar hacia +Z (hacia abajo en la planta). Si no, cambia el giro inicial.");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);
        VBox box = new VBox(10, sizeInfo, grid, hint, error);
        box.setPadding(new Insets(4, 0, 0, 0));
        box.setPrefWidth(420);
        dialog.getDialogPane().setContent(box);

        // Validar antes de cerrar: si hay un error, el diálogo sigue abierto.
        dialog.getDialogPane().lookupButton(add).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String err = validate(name.getText(), width.getText(), depth.getText(), height.getText());
            if (err != null) {
                error.setText(err);
                e.consume();
            }
        });
        dialog.setResultConverter(b -> b != add ? null : new Result(name.getText().trim(),
                new Size(Units.round(Units.parse(width.getText())), Units.round(Units.parse(depth.getText())),
                        Units.round(Units.parse(height.getText()))),
                rotation.getValue()));
        return dialog.showAndWait();
    }

    private static String validate(String name, String w, String d, String h) {
        if (name == null || name.isBlank()) return "Escribe un nombre.";
        String err = check("El ancho", w, CatalogEntry.MODEL_WIDTH);
        if (err == null) err = check("La profundidad", d, CatalogEntry.MODEL_DEPTH);
        if (err == null) err = check("El alto", h, CatalogEntry.MODEL_HEIGHT);
        return err;
    }

    private static String check(String label, String text, Range range) {
        Double v = Units.parse(text);
        if (v == null) return label + " debe ser un número.";
        return Units.checkRange(label, v, range.min(), range.max());
    }
}
