package pe.edu.unaj.amoblador3d.ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.PickResult;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Shape3D;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.catalog.CustomDesign;
import pe.edu.unaj.amoblador3d.catalog.CustomDesign.Paint;
import pe.edu.unaj.amoblador3d.catalog.CustomDesign.Piece;
import pe.edu.unaj.amoblador3d.catalog.CustomDesign.Shape;
import pe.edu.unaj.amoblador3d.design.Editable;
import pe.edu.unaj.amoblador3d.design.Field;
import pe.edu.unaj.amoblador3d.design.Field.Option;
import pe.edu.unaj.amoblador3d.model.Finish;
import pe.edu.unaj.amoblador3d.scene.FurnitureShapes;
import pe.edu.unaj.amoblador3d.util.Units;

/**
 * Diseñador de muebles: arma un mueble con piezas (cajas, cilindros, esferas y barras), cada
 * una con su posición, medidas, color y acabado, y lo guarda en el catálogo ("Mis diseños").
 *
 * <p>Las medidas se escriben en centímetros. El frente del mueble es el lado que se ve al abrir
 * el diseñador (+Z). Al colocarlo en la habitación, el mueble se puede agrandar o achicar y
 * cambiar de colores como cualquier otro.
 */
final class FurnitureDesigner {
    /** Resultado al guardar: el diseño y si reemplaza al mueble desde el que se abrió. */
    record Saved(CustomDesign design, boolean replace) {}

    /** Guarda el diseño en el catálogo; devuelve un error o null. */
    interface SaveHandler {
        String save(Saved saved);
    }

    private static final double CM = 100;
    private static final int MAX_UNDO = 100;
    private static final Color GRID = Color.web("#ffffff22");

    private final Stage stage = new Stage();
    private final SaveHandler onSave;
    private final Map<String, Supplier<CustomDesign>> templates;
    private CustomDesign design;
    private int selected = -1;
    private boolean dirty;

    private final TextField name = new TextField();
    private final ListView<Piece> list = new ListView<>();
    private final PropertiesPanel pieceProps;
    private final PropertiesPanel designProps;
    private final Label sizeLabel = new Label();
    private final Label error = new Label();
    private final CheckBox replace = new CheckBox();
    private final Deque<CustomDesign> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private CustomDesign committed;
    private boolean updatingList;
    /** Pieza cuyo formulario se está mostrando (si sigue siendo la misma, solo se refrescan los valores). */
    private Piece shownPiece;

    // Vista previa
    private final Group content = new Group();
    private final Group grid = new Group();
    private final List<Node> pieceNodes = new ArrayList<>();
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Rotate yaw = new Rotate(-30, Rotate.Y_AXIS), pitch = new Rotate(-22, Rotate.X_AXIS);
    private final Translate pivot = new Translate(), back = new Translate(0, 0, -4);
    private double lastX, lastY, pressX, pressY;

    /**
     * @param start      diseño inicial (se edita una copia)
     * @param replaceText si no es null, muestra la casilla "reemplazar" con este texto
     * @param templates  "nombre" -> diseño para "Empezar desde"
     */
    private FurnitureDesigner(Window owner, CustomDesign start, String replaceText,
                              Map<String, Supplier<CustomDesign>> templates, SaveHandler onSave) {
        this.onSave = onSave;
        this.templates = templates;
        design = start.copy();
        committed = design.copy();
        selected = design.pieces.isEmpty() ? -1 : 0;

        Label pieceHint = new Label("Selecciona una pieza de la lista o haz clic sobre ella en la vista.");
        pieceHint.setWrapText(true);
        pieceHint.getStyleClass().add("hint");
        pieceProps = new PropertiesPanel(this::committedChange, e -> deletePiece(), id -> null, pieceHint);
        designProps = new PropertiesPanel(this::committedChange, e -> { }, id -> null, new Label());

        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Diseñador de muebles");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("designer");
        root.setTop(buildTop());
        root.setLeft(buildPieceList());
        root.setCenter(buildPreview());
        root.setRight(buildSideTabs());
        root.setBottom(buildBottom(replaceText));

        Scene scene = new Scene(root, 1180, 740);
        scene.getStylesheets().add(MainWindow.STYLESHEET);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);
        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(600);
        stage.setOnCloseRequest(e -> {
            if (!confirmClose()) e.consume();
        });
        refreshAll(true);
        frame();
    }

    /**
     * Abre el diseñador y espera a que se cierre.
     *
     * @param replaceText texto de la casilla "reemplazar el mueble seleccionado" (null = no se muestra)
     */
    static void open(Window owner, CustomDesign start, String replaceText, Map<String, Supplier<CustomDesign>> templates,
                     SaveHandler onSave) {
        new FurnitureDesigner(owner, start, replaceText, templates, onSave).stage.showAndWait();
    }

    // ---------------------------------------------------------------- Construcción

    private Node buildTop() {
        Label nameLabel = new Label("Nombre");
        name.setText(design.name);
        name.setPrefColumnCount(18);
        name.setPromptText("Ej.: Mesa de centro");
        name.textProperty().addListener((obs, o, n) -> {
            design.name = n;
            dirty = true;
        });
        ComboBox<String> start = new ComboBox<>();
        start.setPromptText("Empezar desde…");
        start.getItems().setAll(templates.keySet());
        start.setTooltip(new Tooltip("Reemplaza las piezas por las de una plantilla o un mueble del catálogo"));
        start.setOnAction(e -> {
            String key = start.getValue();
            if (key == null) return;
            start.getSelectionModel().clearSelection();
            if (!design.pieces.isEmpty() && dirty
                    && !Dialogs.confirm(stage, "¿Empezar desde «" + key + "»?", "Se reemplazarán las piezas actuales (puedes deshacerlo con Ctrl+Z).", "Reemplazar")) {
                return;
            }
            CustomDesign t = templates.get(key).get();
            design.pieces.clear();
            for (Piece p : t.pieces) design.pieces.add(p.copy());
            design.color = t.color;
            design.color2 = t.color2;
            design.finish = t.finish;
            selected = design.pieces.isEmpty() ? -1 : 0;
            committedChange();
            frame();
        });
        Button undoButton = button("↶", "Deshacer (Ctrl+Z)", this::undo);
        Button redoButton = button("↷", "Rehacer (Ctrl+Y)", this::redo);
        Button frameButton = button("⤢ Encuadrar", "Ver el mueble completo", this::frame);
        Button front = button("Frente", "Ver el mueble de frente", () -> view(0, -8));
        Button side = button("Lado", "Ver el mueble de costado", () -> view(-90, -8));
        Button top = button("Arriba", "Ver el mueble desde arriba", () -> view(0, -89));
        HBox bar = new HBox(8, nameLabel, name, start, new Separator(Orientation.VERTICAL), undoButton, redoButton,
                new Separator(Orientation.VERTICAL), frameButton, front, side, top);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("tool-bar");
        return bar;
    }

    private Node buildPieceList() {
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Piece p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(pieceName(p) + "  ·  " + p.shape.label().toLowerCase());
                javafx.scene.shape.Rectangle swatch = new javafx.scene.shape.Rectangle(12, 12, Color.web(colorOf(p)));
                swatch.setArcWidth(4);
                swatch.setArcHeight(4);
                swatch.setStroke(Color.web("#00000066"));
                setGraphic(swatch);
            }
        });
        list.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
            if (updatingList) return;
            selected = n.intValue();
            showPiece();
            rebuildPreview();
        });
        list.setPlaceholder(new Label("Sin piezas"));
        VBox.setVgrow(list, Priority.ALWAYS);

        FlowPane add = new FlowPane(6, 6,
                button("+ Caja", "Tablero, lateral, puerta, cojín…", () -> addPiece(Shape.BOX)),
                button("+ Cilindro", "Pata redonda, tablero redondo, maceta…", () -> addPiece(Shape.CYLINDER)),
                button("+ Esfera", "Perilla, adorno, puff…", () -> addPiece(Shape.SPHERE)),
                button("+ Barra ↔", "Barra a lo ancho: colgador, travesaño…", () -> addPiece(Shape.BAR_X)),
                button("+ Barra ↕", "Barra a lo profundo: travesaño lateral…", () -> addPiece(Shape.BAR_Z)));
        FlowPane edit = new FlowPane(6, 6,
                button("Duplicar", "Ctrl+D", this::duplicatePiece),
                button("Espejo", "Copia reflejada al otro lado (ideal para patas y laterales)", this::mirrorPiece),
                button("Eliminar", "Supr", this::deletePiece));
        Label hint = new Label("Medidas en centímetros. El frente del mueble mira hacia ti al abrir el diseñador.");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);
        Label title = new Label("Piezas");
        title.getStyleClass().add("group-title");
        VBox box = new VBox(8, title, add, list, edit, hint);
        box.getStyleClass().add("sidebar");
        box.setPrefWidth(250);
        box.setMinWidth(220);
        return box;
    }

    private Node buildPreview() {
        Group world = new Group(grid, content);
        world.getTransforms().add(new Rotate(180, Rotate.X_AXIS)); // Y hacia arriba, como en la habitación
        AmbientLight ambient = new AmbientLight(Color.gray(0.45));
        PointLight key = new PointLight(Color.gray(0.85));
        key.getTransforms().add(new Translate(-3, -5, -4));
        PointLight fill = new PointLight(Color.gray(0.35));
        fill.getTransforms().add(new Translate(4, -2, 3));
        Group root3d = new Group(world, ambient, key, fill, camera);
        camera.setNearClip(0.01);
        camera.setFarClip(200);
        camera.setFieldOfView(40);
        camera.getTransforms().setAll(pivot, yaw, pitch, back);

        SubScene sub = new SubScene(root3d, 100, 100, true, SceneAntialiasing.BALANCED);
        sub.setFill(Color.TRANSPARENT);
        sub.setCamera(camera);
        StackPane holder = new StackPane();
        holder.getStyleClass().add("designer-preview");
        sub.widthProperty().bind(holder.widthProperty());
        sub.heightProperty().bind(holder.heightProperty());
        Pane subHolder = new Pane(sub);
        subHolder.setMinSize(0, 0);
        sizeLabel.getStyleClass().add("designer-size");
        StackPane.setAlignment(sizeLabel, Pos.TOP_LEFT);
        StackPane.setMargin(sizeLabel, new Insets(10));
        Label help = new Label("Arrastra: girar · Clic derecho + arrastrar: mover · Rueda: acercar · Clic: elegir pieza\n"
                + "Flechas: mover la pieza 1 cm (Shift: 10 cm) · RePág / AvPág: subir o bajar");
        help.getStyleClass().add("designer-help");
        StackPane.setAlignment(help, Pos.BOTTOM_LEFT);
        StackPane.setMargin(help, new Insets(10));
        holder.getChildren().addAll(subHolder, sizeLabel, help);
        holder.setMinSize(0, 0);

        sub.setOnMousePressed(e -> {
            lastX = pressX = e.getSceneX();
            lastY = pressY = e.getSceneY();
            sub.requestFocus();
        });
        sub.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - lastX, dy = e.getSceneY() - lastY;
            lastX = e.getSceneX();
            lastY = e.getSceneY();
            if (e.getButton() == MouseButton.PRIMARY) {
                yaw.setAngle(yaw.getAngle() + dx * 0.4);
                pitch.setAngle(Units.clamp(pitch.getAngle() - dy * 0.4, -89, 30));
            } else {
                // Mover el punto de giro en el plano de la pantalla.
                double k = -back.getZ() / 900;
                double a = Math.toRadians(yaw.getAngle());
                pivot.setX(pivot.getX() - (dx * Math.cos(a)) * k);
                pivot.setZ(pivot.getZ() + (dx * Math.sin(a)) * k);
                pivot.setY(pivot.getY() - dy * k);
            }
        });
        sub.setOnScroll(e -> back.setZ(Units.clamp(back.getZ() * (e.getDeltaY() > 0 ? 0.9 : 1.1), -40, -0.3)));
        sub.setOnMouseReleased(e -> {
            if (Math.hypot(e.getSceneX() - pressX, e.getSceneY() - pressY) > 5 || e.getButton() != MouseButton.PRIMARY) return;
            PickResult pick = e.getPickResult();
            Node n = pick == null ? null : pick.getIntersectedNode();
            int i = n == null ? -1 : pieceNodes.indexOf(n);
            if (i >= 0 || n == null) {
                selected = i;
                syncList();
                showPiece();
                rebuildPreview();
            }
        });
        return holder;
    }

    private Node buildSideTabs() {
        Tab piece = new Tab("Pieza", pieceProps.node());
        Tab furniture = new Tab("Mueble", designProps.node());
        TabPane tabs = new TabPane(piece, furniture);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("sidebar-tabs");
        tabs.setPrefWidth(320);
        designProps.show(new DesignEditor());
        return tabs;
    }

    private Node buildBottom(String replaceText) {
        error.getStyleClass().add("error");
        error.setWrapText(true);
        Button cancel = new Button("Cancelar");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> {
            if (confirmClose()) stage.close();
        });
        Button save = new Button("Guardar en el catálogo");
        save.getStyleClass().add("primary");
        save.setOnAction(e -> save());
        HBox bar = new HBox(10);
        if (replaceText != null) {
            replace.setText(replaceText);
            replace.setSelected(true);
            bar.getChildren().add(replace);
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bar.getChildren().addAll(error, spacer, cancel, save);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 12, 8, 12));
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    private static Button button(String text, String tooltip, Runnable action) {
        Button b = new Button(text);
        b.setTooltip(new Tooltip(tooltip));
        b.setFocusTraversable(false);
        b.setOnAction(e -> action.run());
        return b;
    }

    // ---------------------------------------------------------------- Piezas

    private Piece current() {
        return selected >= 0 && selected < design.pieces.size() ? design.pieces.get(selected) : null;
    }

    private String pieceName(Piece p) {
        return p.name != null && !p.name.isBlank() ? p.name : "Pieza " + (design.pieces.indexOf(p) + 1);
    }

    private String colorOf(Piece p) {
        return switch (p.paint) {
            case MAIN -> design.color;
            case ACCENT -> design.color2;
            case OWN -> p.color;
        };
    }

    /** Una pieza nueva encima de lo que ya hay (o en el piso si es la primera). */
    private void addPiece(Shape shape) {
        if (design.pieces.size() >= CustomDesign.MAX_PIECES) {
            showError("Un mueble puede tener hasta " + CustomDesign.MAX_PIECES + " piezas.");
            return;
        }
        Piece p = new Piece();
        p.shape = shape;
        switch (shape) {
            case BOX -> { p.width = 0.6; p.height = 0.04; p.depth = 0.4; }
            case CYLINDER -> { p.width = 0.05; p.height = 0.7; p.depth = 0.05; }
            case SPHERE -> { p.width = 0.2; p.height = 0.2; p.depth = 0.2; }
            case BAR_X -> { p.width = 0.6; p.height = 0.025; p.depth = 0.025; }
            case BAR_Z -> { p.width = 0.025; p.height = 0.025; p.depth = 0.4; }
        }
        p.y = design.pieces.isEmpty() ? 0 : round(design.bounds().maxY());
        int n = design.pieces.size() + 1;
        p.name = shape.label() + " " + n;
        design.pieces.add(p);
        selected = design.pieces.size() - 1;
        committedChange();
    }

    private void duplicatePiece() {
        Piece p = current();
        if (p == null) return;
        Piece c = p.copy();
        c.x = round(p.x + Math.min(0.1, p.width));
        c.name = pieceName(p) + " (copia)";
        design.pieces.add(selected + 1, c);
        selected++;
        committedChange();
    }

    /** Copia la pieza reflejada respecto del centro del mueble (izquierda <-> derecha). */
    private void mirrorPiece() {
        Piece p = current();
        if (p == null) return;
        CustomDesign.Bounds b = design.bounds();
        double center = (b.minX() + b.maxX()) / 2;
        Piece c = p.copy();
        c.x = round(2 * center - p.x);
        if (Math.abs(c.x - p.x) < 1e-6) { // está en el centro: se refleja de adelante hacia atrás
            double cz = (b.minZ() + b.maxZ()) / 2;
            c.z = round(2 * cz - p.z);
        }
        c.name = pieceName(p) + " (espejo)";
        design.pieces.add(selected + 1, c);
        selected++;
        committedChange();
    }

    private void deletePiece() {
        if (current() == null) return;
        design.pieces.remove(selected);
        selected = Math.min(selected, design.pieces.size() - 1);
        committedChange();
    }

    private void nudge(double dx, double dy, double dz) {
        Piece p = current();
        if (p == null) return;
        p.x = round(Units.clamp(p.x + dx, -CustomDesign.MAX_POS, CustomDesign.MAX_POS));
        p.y = round(Units.clamp(p.y + dy, 0, CustomDesign.MAX_POS));
        p.z = round(Units.clamp(p.z + dz, -CustomDesign.MAX_POS, CustomDesign.MAX_POS));
        committedChange();
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }

    // ---------------------------------------------------------------- Cambios, deshacer y vista

    /** Se llama después de cada cambio: registra el paso de deshacer y actualiza todo. */
    private void committedChange() {
        undo.push(committed);
        if (undo.size() > MAX_UNDO) undo.removeLast();
        redo.clear();
        committed = design.copy();
        dirty = true;
        refreshAll(false);
    }

    private void undo() {
        if (undo.isEmpty()) return;
        redo.push(design.copy());
        restore(undo.pop());
    }

    private void redo() {
        if (redo.isEmpty()) return;
        undo.push(design.copy());
        restore(redo.pop());
    }

    private void restore(CustomDesign d) {
        String currentName = design.name;
        design = d.copy();
        design.name = currentName; // el nombre se escribe aparte y no se deshace
        committed = design.copy();
        selected = Math.min(selected, design.pieces.size() - 1);
        dirty = true;
        refreshAll(true);
    }

    private void refreshAll(boolean rebuildPanels) {
        hideError();
        syncList();
        // Rehacer el formulario solo si cambió la pieza: así no se pierde el foco al escribir medidas.
        if (rebuildPanels || current() != shownPiece) showPiece();
        else pieceProps.refresh();
        designProps.refresh();
        rebuildPreview();
        CustomDesign.Bounds b = design.bounds();
        sizeLabel.setText(design.pieces.isEmpty() ? "Sin piezas"
                : Units.number(b.width() * CM) + " × " + Units.number(b.depth() * CM) + " × " + Units.number(b.height() * CM)
                + " cm  (ancho × prof. × alto) · " + design.pieces.size() + (design.pieces.size() == 1 ? " pieza" : " piezas"));
    }

    private void syncList() {
        updatingList = true;
        list.getItems().setAll(design.pieces);
        if (selected >= 0) {
            list.getSelectionModel().select(selected);
            list.scrollTo(selected);
        } else {
            list.getSelectionModel().clearSelection();
        }
        updatingList = false;
    }

    private void showPiece() {
        Piece p = current();
        shownPiece = p;
        pieceProps.show(p == null ? null : new PieceEditor(p));
    }

    private void rebuildPreview() {
        content.getChildren().clear();
        pieceNodes.clear();
        FurnitureShapes.Colors colors = FurnitureShapes.Colors.of(design);
        for (Piece p : design.pieces) {
            Node n = FurnitureShapes.piece(p, colors);
            pieceNodes.add(n);
            content.getChildren().add(n);
        }
        // Contorno de la pieza elegida.
        Piece p = current();
        if (p != null) {
            Piece outline = p.copy();
            double pad = 0.004;
            outline.width += pad * 2;
            outline.height += pad * 2;
            outline.depth += pad * 2;
            outline.y -= pad;
            Shape3D wire = (Shape3D) FurnitureShapes.piece(outline, colors);
            wire.setDrawMode(DrawMode.LINE);
            PhongMaterial glow = new PhongMaterial(Color.web("#4f9cf9"));
            glow.setSelfIlluminationMap(solid(Color.web("#4f9cf9")));
            wire.setMaterial(glow);
            wire.setMouseTransparent(true);
            content.getChildren().add(wire);
        }
        rebuildGrid();
    }

    private static javafx.scene.image.Image solid(Color c) {
        javafx.scene.image.WritableImage img = new javafx.scene.image.WritableImage(1, 1);
        img.getPixelWriter().setColor(0, 0, c);
        return img;
    }

    /** Piso con cuadrícula de 10 cm alrededor del mueble. */
    private void rebuildGrid() {
        grid.getChildren().clear();
        CustomDesign.Bounds b = design.bounds();
        double half = Math.max(0.6, Math.ceil(Math.max(Math.max(Math.abs(b.minX()), Math.abs(b.maxX())),
                Math.max(Math.abs(b.minZ()), Math.abs(b.maxZ()))) * 10 + 3) / 10);
        Box floor = new Box(half * 2, 0.002, half * 2);
        floor.setTranslateY(-0.002);
        floor.setMaterial(new PhongMaterial(Color.web("#3a4149")));
        floor.setMouseTransparent(true);
        grid.getChildren().add(floor);
        PhongMaterial line = new PhongMaterial(GRID);
        int n = (int) Math.round(half * 10);
        for (int i = -n; i <= n; i++) {
            double t = i % 10 == 0 ? 0.004 : 0.0015;
            Box lx = new Box(half * 2, 0.0005, t);
            lx.setTranslateZ(i / 10.0);
            Box lz = new Box(t, 0.0005, half * 2);
            lz.setTranslateX(i / 10.0);
            for (Box l : List.of(lx, lz)) {
                l.setMaterial(line);
                l.setMouseTransparent(true);
                grid.getChildren().add(l);
            }
        }
        // Flecha del frente (+Z): un triángulo hecho con una caja delgada.
        Box arrow = new Box(0.06, 0.001, 0.12);
        arrow.setTranslateZ(half - 0.08);
        PhongMaterial blue = new PhongMaterial(Color.web("#4f9cf9"));
        blue.setSelfIlluminationMap(solid(Color.web("#4f9cf9")));
        arrow.setMaterial(blue);
        arrow.setMouseTransparent(true);
        grid.getChildren().add(arrow);
    }

    /** Encuadra todo el mueble. */
    private void frame() {
        CustomDesign.Bounds b = design.bounds();
        pivot.setX((b.minX() + b.maxX()) / 2);
        pivot.setY(-(b.minY() + b.maxY()) / 2); // escena de JavaFX: Y hacia abajo
        pivot.setZ(-(b.minZ() + b.maxZ()) / 2);
        double size = Math.max(0.3, Math.max(b.width(), Math.max(b.height(), b.depth())));
        back.setZ(-size * 2.6);
    }

    private void view(double yawDeg, double pitchDeg) {
        yaw.setAngle(yawDeg);
        pitch.setAngle(pitchDeg);
        frame();
    }

    // ---------------------------------------------------------------- Teclado, guardar y cerrar

    private void onKey(KeyEvent e) {
        boolean typing = stage.getScene().getFocusOwner() instanceof TextInputControl;
        if (e.isShortcutDown() && e.getCode() == KeyCode.Z && !typing) {
            undo();
            e.consume();
        } else if (e.isShortcutDown() && e.getCode() == KeyCode.Y && !typing) {
            redo();
            e.consume();
        } else if (e.isShortcutDown() && e.getCode() == KeyCode.D) {
            duplicatePiece();
            e.consume();
        } else if (e.isShortcutDown() && e.getCode() == KeyCode.S) {
            save();
            e.consume();
        } else if (!typing && e.getCode() == KeyCode.DELETE) {
            deletePiece();
            e.consume();
        } else if (!typing && !(stage.getScene().getFocusOwner() instanceof ListView) && !(stage.getScene().getFocusOwner() instanceof ComboBox)) {
            double step = e.isShiftDown() ? 0.1 : 0.01;
            switch (e.getCode()) {
                case LEFT -> nudge(-step, 0, 0);
                case RIGHT -> nudge(step, 0, 0);
                case UP -> nudge(0, 0, -step);
                case DOWN -> nudge(0, 0, step);
                case PAGE_UP -> nudge(0, step, 0);
                case PAGE_DOWN -> nudge(0, -step, 0);
                default -> {
                    return;
                }
            }
            e.consume();
        }
    }

    private void save() {
        design.name = name.getText() == null ? "" : name.getText().trim();
        String err = design.validate();
        if (err == null) err = onSave.save(new Saved(design.copy(), replace.isSelected() && replace.getText() != null
                && !replace.getText().isEmpty()));
        if (err != null) {
            showError(err);
            return;
        }
        dirty = false;
        stage.close();
    }

    private boolean confirmClose() {
        return !dirty || Dialogs.confirm(stage, "¿Cerrar sin guardar?", "Se perderá el diseño de «" + name.getText() + "».",
                "Cerrar sin guardar");
    }

    private void showError(String message) {
        error.setText(message);
        error.setVisible(true);
    }

    private void hideError() {
        error.setText("");
        error.setVisible(false);
    }

    // ---------------------------------------------------------------- Formularios

    private static List<Option> finishOptions(String autoLabel) {
        List<Option> options = new ArrayList<>();
        if (autoLabel != null) options.add(new Option("auto", autoLabel));
        for (Finish f : Finish.values()) options.add(new Option(f.id(), f.label()));
        return options;
    }

    private static final List<Option> COLORS = List.of(
            new Option("#d9c6a5", "Haya"), new Option("#a0764f", "Roble"), new Option("#6d4c33", "Nogal"),
            new Option("#f4f1ea", "Blanco"), new Option("#8a8f94", "Gris"), new Option("#2f3236", "Negro"),
            new Option("#5b7fa6", "Azul"), new Option("#7a9a6e", "Verde"), new Option("#8c2f39", "Vino"),
            new Option("#d9a441", "Mostaza"), new Option("#c0c0c0", "Cromado"), new Option("#b08d57", "Bronce"));

    /** Campos de una pieza (en cm). */
    private final class PieceEditor implements Editable {
        private final Piece p;

        PieceEditor(Piece p) {
            this.p = p;
        }

        @Override
        public Object entity() {
            return p;
        }

        @Override
        public String title() {
            return pieceName(p);
        }

        @Override
        public List<Field> fields() {
            List<Option> shapes = new ArrayList<>();
            for (Shape s : Shape.values()) shapes.add(new Option(s.id(), s.label()));
            List<Option> paints = new ArrayList<>();
            for (Paint x : Paint.values()) paints.add(new Option(x.id(), x.label()));
            double max = CustomDesign.MAX_SIZE * CM, pos = CustomDesign.MAX_POS * CM;
            List<Field> fields = new ArrayList<>(List.of(
                    Field.select("shape", "Forma", shapes),
                    Field.number("width", "Ancho (X)", CustomDesign.MIN_SIZE * CM, max, 0.5, "cm"),
                    Field.number("height", "Alto (Y)", CustomDesign.MIN_SIZE * CM, max, 0.5, "cm"),
                    Field.number("depth", "Profundidad (Z)", CustomDesign.MIN_SIZE * CM, max, 0.5, "cm"),
                    Field.number("x", "Centro en X", -pos, pos, 0.5, "cm"),
                    Field.number("z", "Centro en Z", -pos, pos, 0.5, "cm"),
                    Field.number("y", "Altura de la base", 0, pos, 0.5, "cm"),
                    Field.select("paint", "Pintura", paints)));
            if (p.paint == Paint.OWN) fields.add(Field.color("color", "Color propio", COLORS));
            fields.add(Field.select("finish", "Acabado", finishOptions("Como el mueble")));
            return fields;
        }

        @Override
        public Object get(String key) {
            return switch (key) {
                case "shape" -> p.shape.id();
                case "width" -> p.width * CM;
                case "height" -> p.height * CM;
                case "depth" -> p.depth * CM;
                case "x" -> p.x * CM;
                case "y" -> p.y * CM;
                case "z" -> p.z * CM;
                case "paint" -> p.paint.id();
                case "color" -> p.color;
                case "finish" -> p.finish == null ? "auto" : p.finish.id();
                default -> null;
            };
        }

        @Override
        public String set(String key, Object value) {
            switch (key) {
                case "shape" -> p.shape = Shape.fromId((String) value);
                case "paint" -> p.paint = Paint.fromId((String) value);
                case "color" -> p.color = (String) value;
                case "finish" -> p.finish = Finish.fromId((String) value);
                default -> {
                    if (!(value instanceof Double v)) return "Escribe un número.";
                    for (Field f : fields()) {
                        if (f.key().equals(key) && Units.checkRange(f.label(), v, f.min(), f.max()) != null) {
                            return f.label() + " debe estar entre " + Units.number(f.min()) + " y " + Units.number(f.max()) + " cm.";
                        }
                    }
                    double m = round(v / CM);
                    switch (key) {
                        case "width" -> p.width = m;
                        case "height" -> p.height = m;
                        case "depth" -> p.depth = m;
                        case "x" -> p.x = m;
                        case "y" -> p.y = m;
                        case "z" -> p.z = m;
                        default -> { return "Campo desconocido: " + key; }
                    }
                }
            }
            return null;
        }

        @Override
        public List<Action> actions() {
            return List.of(
                    new Action("Al piso", "Apoyar la pieza en el piso (altura 0)", () -> {
                        p.y = 0;
                        return null;
                    }),
                    new Action("Al centro", "Centrar la pieza en X y Z", () -> {
                        p.x = 0;
                        p.z = 0;
                        return null;
                    }),
                    new Action("Encima", "Poner la pieza sobre la más alta de las demás", () -> {
                        double top = 0;
                        for (Piece o : design.pieces) if (o != p) top = Math.max(top, o.y + o.height);
                        p.y = round(top);
                        return null;
                    }),
                    new Action("Renombrar…", "Nombre de la pieza en la lista", () -> {
                        Dialogs.askText(stage, "Renombrar pieza", "Nombre de la pieza", pieceName(p)).ifPresent(n -> p.name = n);
                        return null;
                    }));
        }
    }

    /** Campos del mueble completo: colores, acabado, categoría y medidas totales. */
    private final class DesignEditor implements Editable {
        @Override
        public Object entity() {
            return design;
        }

        @Override
        public String title() {
            return "Mueble completo";
        }

        @Override
        public List<Field> fields() {
            List<Option> categories = new ArrayList<>();
            for (String c : Catalog.CATEGORIES) if (!c.equals(Catalog.IMPORTED)) categories.add(new Option(c, c));
            return List.of(
                    Field.select("category", "Categoría", categories),
                    Field.color("color", "Color principal", COLORS),
                    Field.color("color2", "Color secundario", COLORS),
                    Field.select("finish", "Acabado", finishOptions(null)),
                    Field.number("width", "Ancho total", 5, CustomDesign.MAX_SIZE * CM, 1, "cm"),
                    Field.number("depth", "Profundidad total", 5, CustomDesign.MAX_SIZE * CM, 1, "cm"),
                    Field.number("height", "Alto total", 5, 400, 1, "cm"));
        }

        @Override
        public Object get(String key) {
            CustomDesign.Bounds b = design.bounds();
            return switch (key) {
                case "category" -> design.category;
                case "color" -> design.color;
                case "color2" -> design.color2;
                case "finish" -> design.finish.id();
                case "width" -> b.width() * CM;
                case "depth" -> b.depth() * CM;
                case "height" -> b.height() * CM;
                default -> null;
            };
        }

        @Override
        public String set(String key, Object value) {
            switch (key) {
                case "category" -> design.category = (String) value;
                case "color" -> design.color = (String) value;
                case "color2" -> design.color2 = (String) value;
                case "finish" -> design.finish = Finish.fromId((String) value);
                default -> {
                    if (design.pieces.isEmpty()) return "Agrega primero alguna pieza.";
                    if (!(value instanceof Double v)) return "Escribe un número.";
                    double max = key.equals("height") ? 400 : CustomDesign.MAX_SIZE * CM;
                    String err = Units.checkRange("La medida", v, 5, max);
                    if (err != null) return err;
                    scaleTo(key, v / CM);
                }
            }
            return null;
        }

        @Override
        public boolean canDelete() {
            return false;
        }

        @Override
        public boolean showDelete() {
            return false;
        }

        @Override
        public List<Action> actions() {
            return List.of(new Action("Centrar todo", "Mover las piezas para que el mueble quede centrado y en el piso", () -> {
                CustomDesign.Bounds b = design.bounds();
                double cx = (b.minX() + b.maxX()) / 2, cz = (b.minZ() + b.maxZ()) / 2;
                for (Piece p : design.pieces) {
                    p.x = round(p.x - cx);
                    p.z = round(p.z - cz);
                    p.y = round(p.y - b.minY());
                }
                return null;
            }), new Action("Intercambiar colores", "El principal pasa a secundario y al revés", () -> {
                String c = design.color;
                design.color = design.color2;
                design.color2 = c;
                return null;
            }));
        }
    }

    /** Escala las piezas en un eje para que el mueble mida {@code size} (m) en ese eje. */
    private void scaleTo(String axis, double size) {
        CustomDesign.Bounds b = design.bounds();
        switch (axis) {
            case "width" -> {
                double k = size / Math.max(b.width(), 1e-6), c = (b.minX() + b.maxX()) / 2;
                for (Piece p : design.pieces) {
                    p.x = round(c + (p.x - c) * k);
                    p.width = round(Math.max(CustomDesign.MIN_SIZE, p.width * k));
                }
            }
            case "depth" -> {
                double k = size / Math.max(b.depth(), 1e-6), c = (b.minZ() + b.maxZ()) / 2;
                for (Piece p : design.pieces) {
                    p.z = round(c + (p.z - c) * k);
                    p.depth = round(Math.max(CustomDesign.MIN_SIZE, p.depth * k));
                }
            }
            case "height" -> {
                double k = size / Math.max(b.height(), 1e-6);
                for (Piece p : design.pieces) {
                    p.y = round(b.minY() + (p.y - b.minY()) * k);
                    p.height = round(Math.max(CustomDesign.MIN_SIZE, p.height * k));
                }
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- Plantillas

    /** Plantillas sencillas para empezar (además de los muebles del catálogo). */
    static Map<String, Supplier<CustomDesign>> basicTemplates() {
        Map<String, Supplier<CustomDesign>> t = new LinkedHashMap<>();
        t.put("Vacío (una caja)", () -> {
            CustomDesign d = new CustomDesign();
            Piece p = new Piece();
            p.name = "Cuerpo";
            p.width = 0.6;
            p.height = 0.6;
            p.depth = 0.4;
            d.pieces.add(p);
            return d;
        });
        t.put("Mesa básica", () -> table(false));
        t.put("Mesa redonda", () -> table(true));
        t.put("Estante abierto", FurnitureDesigner::shelf);
        t.put("Banco", FurnitureDesigner::bench);
        return t;
    }

    private static Piece piece(String name, Shape shape, double x, double y, double z, double w, double h, double d, Paint paint) {
        Piece p = new Piece();
        p.name = name;
        p.shape = shape;
        p.x = x;
        p.y = y;
        p.z = z;
        p.width = w;
        p.height = h;
        p.depth = d;
        p.paint = paint;
        return p;
    }

    private static CustomDesign table(boolean round) {
        CustomDesign d = new CustomDesign();
        d.color2 = "#2f3236";
        if (round) {
            d.pieces.add(piece("Tablero", Shape.CYLINDER, 0, 0.72, 0, 0.9, 0.03, 0.9, Paint.MAIN));
            d.pieces.add(piece("Columna", Shape.CYLINDER, 0, 0.03, 0, 0.08, 0.69, 0.08, Paint.ACCENT));
            d.pieces.add(piece("Base", Shape.CYLINDER, 0, 0, 0, 0.5, 0.03, 0.5, Paint.ACCENT));
            return d;
        }
        d.pieces.add(piece("Tablero", Shape.BOX, 0, 0.72, 0, 1.2, 0.03, 0.7, Paint.MAIN));
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                d.pieces.add(piece("Pata", Shape.BOX, sx * 0.55, 0, sz * 0.3, 0.05, 0.72, 0.05, Paint.ACCENT));
            }
        }
        return d;
    }

    private static CustomDesign shelf() {
        CustomDesign d = new CustomDesign();
        d.color = "#d9c6a5";
        d.color2 = "#2f3236";
        for (int sx = -1; sx <= 1; sx += 2) {
            d.pieces.add(piece("Lateral", Shape.BOX, sx * 0.39, 0, 0, 0.02, 1.2, 0.3, Paint.ACCENT));
        }
        for (int i = 0; i < 4; i++) {
            d.pieces.add(piece("Repisa " + (i + 1), Shape.BOX, 0, 0.05 + i * 0.37, 0, 0.76, 0.025, 0.3, Paint.MAIN));
        }
        return d;
    }

    private static CustomDesign bench() {
        CustomDesign d = new CustomDesign();
        d.color = "#6d4c33";
        d.color2 = "#5b7fa6";
        d.pieces.add(piece("Asiento", Shape.BOX, 0, 0.38, 0, 1.0, 0.04, 0.35, Paint.MAIN));
        d.pieces.add(piece("Cojín", Shape.BOX, 0, 0.42, 0, 0.96, 0.06, 0.33, Paint.ACCENT));
        for (int sx = -1; sx <= 1; sx += 2) {
            d.pieces.add(piece("Pata", Shape.BOX, sx * 0.45, 0, 0, 0.05, 0.38, 0.3, Paint.MAIN));
        }
        d.pieces.add(piece("Travesaño", Shape.BAR_X, 0, 0.12, 0, 0.85, 0.03, 0.03, Paint.MAIN));
        return d;
    }
}
