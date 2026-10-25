package pe.edu.unaj.amoblador3d.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import pe.edu.unaj.amoblador3d.design.Editable;
import pe.edu.unaj.amoblador3d.design.Field;
import pe.edu.unaj.amoblador3d.design.Field.Option;
import pe.edu.unaj.amoblador3d.model.Surface;
import pe.edu.unaj.amoblador3d.util.Units;

/**
 * Panel derecho: dibuja el formulario del elemento seleccionado a partir de sus
 * {@link Field}. El mismo panel sirve para paredes, piso, aberturas, luces y muebles.
 *
 * <p>Los números se aplican al presionar Enter o al salir del campo (Esc deshace lo escrito).
 * Si el cambio no es válido, se muestra el motivo y el campo vuelve al valor anterior.
 */
final class PropertiesPanel {
    private final VBox content = new VBox(10);
    private final ScrollPane root = new ScrollPane(content);
    private final Label title = new Label();
    private final Label error = new Label();
    private final GridPane grid = new GridPane();
    private final FlowPane actions = new FlowPane(6, 6);
    private final Button delete = new Button("Eliminar");
    /** Controles de cada campo, para actualizar sus valores sin rehacer el formulario. */
    private final Map<String, Node> controls = new LinkedHashMap<>();

    private Editable editable;
    private List<Field> shownFields = List.of();
    /** Se llama después de cada cambio aplicado (para registrarlo en deshacer). */
    private final Runnable onChanged;
    private final Consumer<Object> onDelete;
    private boolean updating;

    PropertiesPanel(Runnable onChanged, Consumer<Object> onDelete) {
        this.onChanged = onChanged;
        this.onDelete = onDelete;
        title.getStyleClass().add("panel-title");
        title.setWrapText(true);
        error.getStyleClass().add("error");
        error.setWrapText(true);
        error.setManaged(false);
        error.setVisible(false);
        grid.setHgap(8);
        grid.setVgap(8);
        delete.getStyleClass().add("danger");
        delete.setOnAction(e -> {
            if (editable != null) onDelete.accept(editable.entity());
        });
        content.getStyleClass().add("inspector");
        root.setFitToWidth(true);
        root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.getStyleClass().add("side-scroll");
        root.setPrefWidth(290);
        root.setMinWidth(260);
        showEmpty();
    }

    Node node() {
        return root;
    }

    private void showEmpty() {
        editable = null;
        shownFields = List.of();
        controls.clear();
        Label hint = new Label("Haz clic en una pared, el piso, una ventana, una luz o un mueble para ver y cambiar sus propiedades.");
        hint.setWrapText(true);
        hint.getStyleClass().add("hint");
        title.setText("Propiedades");
        content.getChildren().setAll(title, hint);
    }

    /** Muestra el formulario de un elemento (o el mensaje inicial si es null). */
    void show(Editable e) {
        if (e == null) {
            showEmpty();
            return;
        }
        editable = e;
        hideError();
        build();
    }

    /** Vuelve a leer los valores (tras deshacer, arrastrar, etc.). Rehace el formulario si cambiaron los campos. */
    void refresh() {
        if (editable == null) return;
        List<Field> fields = editable.fields();
        title.setText(editable.title());
        if (!sameShape(fields, shownFields)) {
            build();
            return;
        }
        shownFields = fields;
        updating = true;
        for (Field f : fields) setControlValue(f, controls.get(f.key()));
        updating = false;
    }

    /** Mismos campos, en el mismo orden y con las mismas opciones (los límites pueden cambiar). */
    private static boolean sameShape(List<Field> a, List<Field> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            Field x = a.get(i), y = b.get(i);
            if (!x.key().equals(y.key()) || x.type() != y.type() || !x.options().equals(y.options())) return false;
        }
        return true;
    }

    private void build() {
        controls.clear();
        grid.getChildren().clear();
        shownFields = editable.fields();
        title.setText(editable.title());
        int row = 0;
        updating = true;
        for (Field f : shownFields) {
            Label label = new Label(f.label());
            label.getStyleClass().add("field-label");
            Node control = createControl(f);
            controls.put(f.key(), control);
            setControlValue(f, control);
            Node cell = control;
            if (f.type() == Field.Type.NUMBER && !f.unit().isEmpty()) {
                Label unit = new Label(f.unit());
                unit.getStyleClass().add("unit");
                HBox box = new HBox(4, control, unit);
                box.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(control, Priority.ALWAYS);
                cell = box;
            }
            grid.add(label, 0, row);
            grid.add(cell, 1, row);
            GridPane.setHgrow(cell, Priority.ALWAYS);
            row++;
        }
        updating = false;

        actions.getChildren().clear();
        for (Editable.Action a : editable.actions()) {
            Button b = new Button(a.label());
            if (a.tooltip() != null && !a.tooltip().isEmpty()) b.setTooltip(new Tooltip(a.tooltip()));
            b.setOnAction(e -> applied(a.run().get()));
            actions.getChildren().add(b);
        }
        // Las paredes y el piso no se eliminan.
        if (!(editable.entity() instanceof Surface)) actions.getChildren().add(delete);

        List<Node> children = new ArrayList<>(List.of(title, grid));
        if (!actions.getChildren().isEmpty()) children.add(actions);
        children.add(error);
        content.getChildren().setAll(children);
    }

    private Node createControl(Field f) {
        return switch (f.type()) {
            case NUMBER -> {
                TextField t = new TextField();
                t.setPrefColumnCount(6);
                t.setTooltip(new Tooltip("Entre " + Units.number(f.min()) + " y " + Units.number(f.max()) + " " + f.unit()));
                t.setOnAction(e -> commitNumber(f, t));
                t.focusedProperty().addListener((obs, was, now) -> {
                    if (!now) commitNumber(f, t);
                });
                t.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ESCAPE) {
                        setControlValue(f, t);
                        hideError();
                    }
                });
                yield t;
            }
            case SELECT -> {
                ComboBox<Option> c = new ComboBox<>();
                c.getItems().setAll(f.options());
                c.setMaxWidth(Double.MAX_VALUE);
                c.setConverter(new StringConverter<>() {
                    @Override
                    public String toString(Option o) {
                        return o == null ? "" : o.label();
                    }

                    @Override
                    public Option fromString(String s) {
                        return null;
                    }
                });
                c.valueProperty().addListener((obs, old, now) -> {
                    if (!updating && now != null && !now.value().equals(editable.get(f.key()))) apply(f.key(), now.value());
                });
                yield c;
            }
            case COLOR -> {
                ColorPicker p = new ColorPicker();
                p.setMaxWidth(Double.MAX_VALUE);
                p.setOnAction(e -> {
                    if (!updating) apply(f.key(), Colors.toHex(p.getValue()));
                });
                yield p;
            }
            case CHECKBOX -> {
                CheckBox c = new CheckBox();
                c.selectedProperty().addListener((obs, old, now) -> {
                    if (!updating) apply(f.key(), now);
                });
                yield c;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private void setControlValue(Field f, Node control) {
        Object v = editable.get(f.key());
        switch (f.type()) {
            case NUMBER -> {
                ((TextField) control).setText(v instanceof Double d ? Units.number(d) : "");
            }
            case SELECT -> {
                ComboBox<Option> c = (ComboBox<Option>) control;
                Option selected = null;
                for (Option o : c.getItems()) if (o.value().equals(v)) selected = o;
                c.setValue(selected);
            }
            case COLOR -> {
                ColorPicker p = (ColorPicker) control;
                p.setValue(v instanceof String s ? Color.web(s) : Color.WHITE);
            }
            case CHECKBOX -> ((CheckBox) control).setSelected(Boolean.TRUE.equals(v));
        }
    }

    private void commitNumber(Field f, TextField t) {
        if (editable == null || !controls.containsValue(t)) return;
        Double value = Units.parse(t.getText());
        Object current = editable.get(f.key());
        if (value != null && current instanceof Double d && Units.round(value) == Units.round(d)) {
            t.setText(Units.number(d));
            return;
        }
        if (value == null) {
            showError(f.label() + " debe ser un número.");
            t.setText(current instanceof Double d ? Units.number(d) : "");
            return;
        }
        apply(f.key(), value);
    }

    private void apply(String key, Object value) {
        applied(editable.set(key, value));
    }

    /** @param err mensaje de error o null si el cambio se aplicó */
    private void applied(String err) {
        if (err != null) {
            showError(err);
            refresh(); // vuelve a mostrar los valores que siguen vigentes
            return;
        }
        hideError();
        onChanged.run();
        refresh();
    }

    void showError(String message) {
        error.setText(message);
        error.setManaged(true);
        error.setVisible(true);
    }

    private void hideError() {
        error.setManaged(false);
        error.setVisible(false);
    }
}
