package pe.edu.unaj.amoblador3d.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.StringConverter;
import pe.edu.unaj.amoblador3d.design.FSHFEditable;
import pe.edu.unaj.amoblador3d.design.FSHFField;
import pe.edu.unaj.amoblador3d.design.FSHFField.Option;
import pe.edu.unaj.amoblador3d.model.FSHFSurface;
import pe.edu.unaj.amoblador3d.util.FSHFUnits;

final class FSHFPropertiesPanel {
    private static final double SWATCH = 22, PREVIEW = 58;

    private final VBox content = new VBox(12);
    private final ScrollPane root = new ScrollPane(content);
    private final Label title = new Label();
    private final Label subtitle = new Label();
    private final Label error = new Label();
    private final VBox form = new VBox(10);
    private final FlowPane actions = new FlowPane(6, 6);
    private final Button delete = new Button("Eliminar");
    private final Map<String, Node> controls = new LinkedHashMap<>();
    private final Map<String, Slider> sliders = new LinkedHashMap<>();

    private final Runnable onChanged;
    private Runnable onPending = () -> {};
    private Function<Object, Node> header = e -> null;
    private boolean sliding;
    private final Function<String, Image> previews;
    private final Node emptyState;

    private FSHFEditable editable;
    private List<FSHFField> shownFields = List.of();
    private boolean updating;

    FSHFPropertiesPanel(Runnable onChanged, Consumer<Object> onDelete, Function<String, Image> previews, Node emptyState) {
        this.onChanged = onChanged;
        this.previews = previews;
        this.emptyState = emptyState;
        title.getStyleClass().add("panel-title");
        title.setWrapText(true);
        subtitle.getStyleClass().add("hint");
        error.getStyleClass().add("error-box");
        error.setWrapText(true);
        delete.getStyleClass().add("danger");
        delete.setOnAction(e -> {
            if (editable != null) onDelete.accept(editable.entity());
        });
        content.getStyleClass().add("inspector");
        root.setFitToWidth(true);
        root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.getStyleClass().add("side-scroll");
        root.setPrefWidth(300);
        root.setMinWidth(270);
        showEmpty();
    }

    Node node() {
        return root;
    }

    void setOnPending(Runnable r) {
        onPending = r;
    }

    void setHeader(Function<Object, Node> factory) {
        header = factory;
    }

    private void showEmpty() {
        editable = null;
        shownFields = List.of();
        controls.clear();
        sliders.clear();
        title.setText("Propiedades");
        content.getChildren().setAll(title, emptyState);
    }

    void show(FSHFEditable e) {
        if (e == null) {
            showEmpty();
            return;
        }
        editable = e;
        hideError();
        build();
    }

    void refresh() {
        if (editable == null || sliding) return;
        List<FSHFField> fields = editable.fields();
        title.setText(editable.title());
        if (!sameShape(fields, shownFields)) {
            build();
            return;
        }
        shownFields = fields;
        updating = true;
        for (FSHFField f : fields) {
            setControlValue(f, controls.get(f.key()));
            Slider sl = sliders.get(f.key());
            if (sl != null) {
                sl.setMin(f.min());
                sl.setMax(Math.max(f.min(), f.max()));
                if (editable.get(f.key()) instanceof Double d) sl.setValue(d);
            }
        }
        updating = false;
        updateEnabled();
        buildActions();
    }

    private void updateEnabled() {
        for (Map.Entry<String, Node> e : controls.entrySet()) e.getValue().setDisable(!editable.isEnabled(e.getKey()));
        for (Map.Entry<String, Slider> e : sliders.entrySet()) e.getValue().setDisable(!editable.isEnabled(e.getKey()));
        delete.setDisable(!editable.canDelete());
        delete.setTooltip(editable.canDelete() ? null : new Tooltip("Desbloquéalo para poder eliminarlo"));
    }

    private void buildActions() {
        actions.getChildren().clear();
        for (FSHFEditable.Action a : editable.actions()) {
            Button b = new Button(a.label());
            if (a.tooltip() != null && !a.tooltip().isEmpty()) b.setTooltip(new Tooltip(a.tooltip()));
            b.setOnAction(e -> applied(a.run().get()));
            b.setDisable(!a.enabled());
            b.setFocusTraversable(false);
            actions.getChildren().add(b);
        }
        if (editable.showDelete()) actions.getChildren().add(delete);
    }

    private static boolean sameShape(List<FSHFField> a, List<FSHFField> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            FSHFField x = a.get(i), y = b.get(i);
            if (!x.key().equals(y.key()) || x.type() != y.type() || !x.options().equals(y.options())) return false;
        }
        return true;
    }

    private void build() {
        controls.clear();
        sliders.clear();
        shownFields = editable.fields();
        title.setText(editable.title());
        subtitle.setText(editable.entity() instanceof FSHFSurface ? "Elige un material y luego ajusta el color." : "");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        List<Node> blocks = new ArrayList<>();
        int row = 0;
        updating = true;
        for (FSHFField f : shownFields) {
            if (f.type() == FSHFField.Type.SECTION) {
                if (row > 0) {
                    blocks.add(grid);
                    grid = new GridPane();
                    grid.setHgap(8);
                    grid.setVgap(8);
                    row = 0;
                }
                Label section = new Label(f.label().toUpperCase());
                section.getStyleClass().add("section-title");
                section.setMaxWidth(Double.MAX_VALUE);
                controls.put(f.key(), section);
                blocks.add(section);
                continue;
            }
            Node control = createControl(f);
            controls.put(f.key(), control);
            setControlValue(f, control);
            Label label = new Label(f.label());
            label.getStyleClass().add("field-label");

            if (f.type() == FSHFField.Type.GALLERY || (f.type() == FSHFField.Type.COLOR && !f.options().isEmpty())) {
                if (row > 0) {
                    blocks.add(grid);
                    grid = new GridPane();
                    grid.setHgap(8);
                    grid.setVgap(8);
                    row = 0;
                }
                label.getStyleClass().add("subheading");
                blocks.add(new VBox(6, label, control));
                continue;
            }
            Node cell = control;
            if (f.type() == FSHFField.Type.NUMBER && !f.unit().isEmpty()) {
                Label unit = new Label(f.unit());
                unit.getStyleClass().add("unit");
                unit.setMinWidth(18);
                HBox box = new HBox(4, control, unit);
                box.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(control, Priority.ALWAYS);
                cell = box;
            }
            label.setMinWidth(110);
            grid.add(label, 0, row);
            grid.add(cell, 1, row);
            GridPane.setHgrow(cell, Priority.ALWAYS);
            row++;
            if (f.type() == FSHFField.Type.NUMBER && f.slider() && f.max() > f.min()) {
                Slider sl = createSlider(f);
                sliders.put(f.key(), sl);
                grid.add(sl, 0, row, 2, 1);
                GridPane.setHgrow(sl, Priority.ALWAYS);
                row++;
            }
        }
        if (row > 0) blocks.add(grid);
        updating = false;
        form.getChildren().setAll(blocks);
        updateEnabled();
        buildActions();

        List<Node> children = new ArrayList<>(List.of(title));
        Node head = header.apply(editable.entity());
        if (head != null) children.add(head);
        if (!subtitle.getText().isEmpty()) children.add(subtitle);
        children.add(form);
        if (!actions.getChildren().isEmpty()) children.add(actions);
        children.add(error);
        content.getChildren().setAll(children);
    }

    private Slider createSlider(FSHFField f) {
        Slider sl = new Slider(f.min(), f.max(), f.min());
        sl.getStyleClass().add("field-slider");
        sl.setFocusTraversable(false);
        if (editable.get(f.key()) instanceof Double d) sl.setValue(d);
        boolean[] changed = {false};
        sl.valueProperty().addListener((obs, o, n) -> {
            if (updating) return;
            double step = f.step() > 0 ? f.step() : 0.01;
            double v = Math.round(n.doubleValue() / step) * step;
            v = Math.round(v * 1000) / 1000.0;
            if (editable.get(f.key()) instanceof Double cur && Math.abs(cur - v) < 1e-9) return;
            sliding = true;
            String err = editable.set(f.key(), v);
            sliding = false;
            if (err != null) {
                showError(err);
                return;
            }
            hideError();
            changed[0] = true;
            boolean was = updating;
            updating = true;
            for (FSHFField x : shownFields) {
                if (x.type() == FSHFField.Type.NUMBER) setControlValue(x, controls.get(x.key()));
            }
            updating = was;
            onPending.run();
        });
        Runnable finish = () -> {
            if (!changed[0]) return;
            changed[0] = false;
            onChanged.run();
            refresh();
        };
        sl.valueChangingProperty().addListener((obs, o, now) -> {
            if (!now) finish.run();
        });
        sl.setOnMouseReleased(e -> finish.run());
        return sl;
    }

    private Node createControl(FSHFField f) {
        return switch (f.type()) {
            case NUMBER -> {
                TextField t = new TextField();
                t.setPrefColumnCount(6);
                t.setTooltip(new Tooltip("Entre " + FSHFUnits.number(f.min()) + " y " + FSHFUnits.number(f.max()) + " " + f.unit()
                        + "\nEnter para aplicar, Esc para cancelar"));
                t.setOnAction(e -> commitNumber(f, t));
                t.focusedProperty().addListener((obs, was, now) -> {
                    if (!now) commitNumber(f, t);
                });
                t.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ESCAPE) {
                        setControlValue(f, t);
                        hideError();
                        e.consume();
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
            case GALLERY -> {
                TilePane tiles = new TilePane(6, 6);
                tiles.setPrefColumns(3);
                for (Option o : f.options()) {
                    ToggleButton b = new ToggleButton(o.label());
                    b.setUserData(o.value());
                    b.getStyleClass().add("gallery-tile");
                    b.setContentDisplay(ContentDisplay.TOP);
                    b.setWrapText(true);
                    b.setFocusTraversable(false);
                    Image img = previews.apply(o.value());
                    if (img != null) {
                        ImageView view = new ImageView(img);
                        view.setFitWidth(PREVIEW);
                        view.setFitHeight(PREVIEW);
                        view.setSmooth(true);
                        b.setGraphic(view);
                    }
                    b.setOnAction(e -> {
                        if (!updating && !o.value().equals(editable.get(f.key()))) apply(f.key(), o.value());
                        else setControlValue(f, tiles);
                    });
                    tiles.getChildren().add(b);
                }
                yield tiles;
            }
            case COLOR -> {
                ColorPicker p = new ColorPicker();
                p.setMaxWidth(Double.MAX_VALUE);
                p.setOnAction(e -> {
                    if (!updating) apply(f.key(), FSHFColors.toHex(p.getValue()));
                });
                if (f.options().isEmpty()) yield p;
                FlowPane swatches = new FlowPane(5, 5);
                for (Option o : f.options()) {
                    Rectangle r = new Rectangle(SWATCH, SWATCH, Color.web(o.value()));
                    r.setArcWidth(6);
                    r.setArcHeight(6);
                    Button b = new Button(null, r);
                    b.setUserData(o.value());
                    b.getStyleClass().add("swatch");
                    b.setFocusTraversable(false);
                    b.setTooltip(new Tooltip(o.label()));
                    b.setOnAction(e -> apply(f.key(), o.value()));
                    swatches.getChildren().add(b);
                }
                VBox box = new VBox(6, swatches, p);
                box.setUserData(p);
                yield box;
            }
            case CHECKBOX -> {
                CheckBox c = new CheckBox();
                c.selectedProperty().addListener((obs, old, now) -> {
                    if (!updating) apply(f.key(), now);
                });
                yield c;
            }
            case SECTION -> new Label(f.label());
        };
    }

    @SuppressWarnings("unchecked")
    private void setControlValue(FSHFField f, Node control) {
        Object v = editable.get(f.key());
        boolean was = updating;
        updating = true;
        switch (f.type()) {
            case NUMBER -> ((TextField) control).setText(v instanceof Double d ? FSHFUnits.number(d) : "");
            case SELECT -> {
                ComboBox<Option> c = (ComboBox<Option>) control;
                Option selected = null;
                for (Option o : c.getItems()) if (o.value().equals(v)) selected = o;
                c.setValue(selected);
            }
            case GALLERY -> {
                for (Node n : ((TilePane) control).getChildren()) {
                    ((ToggleButton) n).setSelected(n.getUserData().equals(v));
                }
            }
            case COLOR -> {
                ColorPicker p = control instanceof ColorPicker cp ? cp : (ColorPicker) control.getUserData();
                p.setValue(v instanceof String s ? Color.web(s) : Color.WHITE);
                if (control instanceof VBox box) {
                    for (Node n : ((FlowPane) box.getChildren().get(0)).getChildren()) {
                        boolean current = v instanceof String s && s.equalsIgnoreCase((String) n.getUserData());
                        n.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("current"), current);
                    }
                }
            }
            case CHECKBOX -> ((CheckBox) control).setSelected(Boolean.TRUE.equals(v));
            case SECTION -> { }
        }
        updating = was;
    }

    private void commitNumber(FSHFField f, TextField t) {
        if (editable == null || !controls.containsValue(t)) return;
        Double value = FSHFUnits.parse(t.getText());
        Object current = editable.get(f.key());
        if (value != null && current instanceof Double d && FSHFUnits.round(value) == FSHFUnits.round(d)) {
            t.setText(FSHFUnits.number(d));
            return;
        }
        if (value == null) {
            showError(f.label() + " debe ser un número.");
            t.setText(current instanceof Double d ? FSHFUnits.number(d) : "");
            return;
        }
        apply(f.key(), value);
    }

    private void apply(String key, Object value) {
        applied(editable.set(key, value));
    }

    private void applied(String err) {
        if (err != null) {
            showError(err);
            refresh();
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
