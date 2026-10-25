package pe.edu.unaj.amoblador3d.ui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import pe.edu.unaj.amoblador3d.catalog.BuiltinCatalog;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry;
import pe.edu.unaj.amoblador3d.catalog.UserModels;
import pe.edu.unaj.amoblador3d.design.Design;
import pe.edu.unaj.amoblador3d.design.Editable;
import pe.edu.unaj.amoblador3d.design.Editors;
import pe.edu.unaj.amoblador3d.design.History;
import pe.edu.unaj.amoblador3d.gltf.GltfLoader;
import pe.edu.unaj.amoblador3d.gltf.ModelData;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.Light;
import pe.edu.unaj.amoblador3d.model.LightType;
import pe.edu.unaj.amoblador3d.model.Opening;
import pe.edu.unaj.amoblador3d.model.OpeningType;
import pe.edu.unaj.amoblador3d.model.Project;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.model.Surface;
import pe.edu.unaj.amoblador3d.model.SurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.WallSide;
import pe.edu.unaj.amoblador3d.persistence.ProjectFormatException;
import pe.edu.unaj.amoblador3d.persistence.ProjectJson;
import pe.edu.unaj.amoblador3d.persistence.ProjectRepository;
import pe.edu.unaj.amoblador3d.scene.ModelLibrary;
import pe.edu.unaj.amoblador3d.scene.SceneView;
import pe.edu.unaj.amoblador3d.scene.SurfacePreview;
import pe.edu.unaj.amoblador3d.util.Outcome;
import pe.edu.unaj.amoblador3d.util.Units;

/**
 * Ventana principal: menú y barra de herramientas arriba, panel de la habitación y el
 * catálogo a la izquierda, la vista 3D al centro y las propiedades de lo seleccionado a la derecha.
 *
 * <p>Cada acción que cambia el proyecto termina en {@link History#commit()} para poder deshacerla.
 */
public final class MainWindow {
    public static final String STYLESHEET = MainWindow.class.getResource("/ui/app.css").toExternalForm();
    private static final String APP_NAME = "Amoblador3D";
    private static final String UNTITLED = "Sin título";
    private static final FileChooser.ExtensionFilter PROJECT_FILES =
            new FileChooser.ExtensionFilter("Proyecto Amoblador3D (*.amob3d)", "*.amob3d");

    private final Stage stage;
    private final Catalog catalog;
    private final UserModels userModels;
    private final ProjectRepository projects;
    private final ModelLibrary models;
    private final HostServices host;
    private final Design design;
    private final SceneView view;
    private final History history;
    private final PropertiesPanel properties;

    private Long projectId;
    private String projectName = UNTITLED;
    private Object selected;
    private boolean syncing; // evita que los controles reaccionen mientras los actualizamos nosotros

    // Panel izquierdo
    private final TextField roomWidth = new TextField(), roomLength = new TextField();
    private final TextField roomHeight = new TextField(), roomWall = new TextField();
    private final Label roomError = new Label(), roomInfo = new Label();
    private final Slider ambient = new Slider(0, 2, Project.DEFAULT_AMBIENT);
    private final Label ambientValue = new Label();
    private final FlowPane parametricButtons = new FlowPane(6, 6), modelButtons = new FlowPane(6, 6);
    private final ListView<Object> items = new ListView<>();
    private final Button floorButton = new Button("Piso");
    private final java.util.Map<WallSide, Button> wallButtons = new java.util.EnumMap<>(WallSide.class);
    private Toast toast;
    /** Pasos de un movimiento con las flechas: se registran juntos al soltar la tecla. */
    private boolean nudging;

    // Barra superior y de estado
    private final Button undoButton = new Button("↶ Deshacer"), redoButton = new Button("↷ Rehacer");
    private final MenuItem undoItem = new MenuItem("Deshacer"), redoItem = new MenuItem("Rehacer");
    private final ToggleGroup viewGroup = new ToggleGroup();
    private final ToggleButton view3d = new ToggleButton("Vista 3D"), viewPlan = new ToggleButton("Planta 2D");
    private final CheckBox dimensions = new CheckBox("Cotas");
    private final CheckMenuItem dimensionsItem = new CheckMenuItem("Mostrar cotas");
    private final Label status = new Label();
    private final Label selectionInfo = new Label();
    private final PauseTransition statusTimer = new PauseTransition(Duration.seconds(6));

    public MainWindow(Stage stage, Catalog catalog, UserModels userModels, ProjectRepository projects,
                      ModelLibrary models, HostServices host) {
        this.stage = stage;
        this.catalog = catalog;
        this.userModels = userModels;
        this.projects = projects;
        this.models = models;
        this.host = host;

        design = new Design(catalog);
        design.setWarningHandler(this::warn);
        design.loadDefault();
        view = new SceneView(design, models, this::warn);
        history = new History(() -> ProjectJson.write(design.snapshot()), this::restore, this::updateTitle);
        properties = new PropertiesPanel(history::commit, this::delete,
                id -> {
                    SurfaceMaterial m = SurfaceMaterial.fromId(id);
                    return SurfacePreview.of(m, m.defaultColor());
                },
                buildEmptyState());

        view.setOnSelect(this::select);
        view.setOnDragEnd(history::commit);
        design.addListener(changes -> designChanged());

        BorderPane root = new BorderPane();
        root.setTop(new VBox(buildMenu(), buildToolbar()));
        root.setLeft(buildSidebar());
        root.setCenter(buildViewport());
        root.setRight(properties.node());
        root.setBottom(buildStatusBar());

        // Que la ventana quepa en pantallas pequeñas (p. ej. 1366 x 768).
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double w = Math.min(1400, screen.getWidth() * 0.95), h = Math.min(860, screen.getHeight() * 0.92);
        Scene scene = new Scene(root, w, h);
        if (screen.getWidth() < 1300 || screen.getHeight() < 760) stage.setMaximized(true);
        scene.getStylesheets().add(STYLESHEET);
        scene.addEventHandler(KeyEvent.KEY_PRESSED, this::onKey);
        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(600);
        stage.setOnCloseRequest(e -> {
            if (!confirmDiscard()) e.consume();
        });
        scene.addEventHandler(KeyEvent.KEY_RELEASED, e -> {
            if (nudging && e.getCode().isArrowKey()) {
                nudging = false;
                history.flush();
            }
        });

        statusTimer.setOnFinished(e -> status.setText(defaultStatus()));
        status.setText(defaultStatus());
        rebuildCatalogButtons();
        designChanged();
        updateTitle();
    }

    /** Muestra la ventana y encuadra la habitación cuando ya tiene tamaño. */
    public void show(List<String> startupWarnings) {
        stage.show();
        Platform.runLater(() -> {
            view.frameRoom();
            // Corrección: antes el foco empezaba en el campo "Ancho" y R, Supr o las flechas no hacían nada.
            view.node().requestFocus();
        });
        if (!startupWarnings.isEmpty()) warn(String.join("\n", startupWarnings));
    }

    // ---------------------------------------------------------------- Construcción

    private MenuBar buildMenu() {
        Menu file = new Menu("Archivo", null,
                item("Nuevo proyecto", "Shortcut+N", this::newProject),
                item("Abrir…", "Shortcut+O", this::openProject),
                new SeparatorMenuItem(),
                item("Guardar", "Shortcut+S", this::save),
                item("Guardar como…", "Shortcut+Shift+S", this::saveAs),
                new SeparatorMenuItem(),
                item("Importar archivo .amob3d…", null, this::importFile),
                item("Exportar archivo .amob3d…", null, this::exportFile),
                item("Exportar imagen PNG…", "Shortcut+E", this::exportImage),
                new SeparatorMenuItem(),
                item("Importar modelo 3D (.glb/.gltf)…", "Shortcut+I", this::importModel),
                item("Abrir carpeta de modelos importados", null, this::openModelsFolder),
                item("Recargar catálogo", null, this::reloadCatalog),
                new SeparatorMenuItem(),
                item("Salir", null, () -> {
                    if (confirmDiscard()) stage.close();
                }));

        undoItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Z"));
        undoItem.setOnAction(e -> undo());
        redoItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Y"));
        redoItem.setOnAction(e -> redo());
        Menu edit = new Menu("Editar", null, undoItem, redoItem, new SeparatorMenuItem(),
                item("Duplicar mueble", "Shortcut+D", this::duplicateSelected),
                item("Eliminar selección   (Supr)", null, () -> delete(selected)),
                item("Deseleccionar   (Esc)", null, () -> select(null)));

        RadioMenuItem menu3d = new RadioMenuItem("Vista 3D"), menuPlan = new RadioMenuItem("Planta 2D");
        menu3d.setAccelerator(KeyCombination.keyCombination("Shortcut+1"));
        menuPlan.setAccelerator(KeyCombination.keyCombination("Shortcut+2"));
        ToggleGroup menuGroup = new ToggleGroup();
        menu3d.setToggleGroup(menuGroup);
        menuPlan.setToggleGroup(menuGroup);
        menu3d.setSelected(true);
        menu3d.setOnAction(e -> view3d.setSelected(true));
        menuPlan.setOnAction(e -> viewPlan.setSelected(true));
        viewGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == null) return;
            (n == viewPlan ? menuPlan : menu3d).setSelected(true);
        });
        dimensionsItem.setAccelerator(KeyCombination.keyCombination("Shortcut+M"));
        dimensionsItem.setSelected(true);
        dimensionsItem.selectedProperty().bindBidirectional(dimensions.selectedProperty());
        Menu viewMenu = new Menu("Ver", null, menu3d, menuPlan, new SeparatorMenuItem(), dimensionsItem,
                item("Centrar cámara", "Shortcut+0", view::frameRoom),
                new SeparatorMenuItem(),
                item("Pantalla completa", "F11", () -> stage.setFullScreen(!stage.isFullScreen())));

        Menu help = new Menu("Ayuda", null,
                item("Atajos de teclado", "F1", this::showShortcuts),
                item("Acerca de Amoblador3D", null, () -> Dialogs.info(stage, "Acerca de", APP_NAME,
                        "Diseñador de interiores 3D con medidas reales.\nJavaFX + SQLite.\nDatos en: " + userModels.root().getParent())));
        return new MenuBar(file, edit, viewMenu, help);
    }

    private static MenuItem item(String label, String accelerator, Runnable action) {
        MenuItem m = new MenuItem(label);
        if (accelerator != null) m.setAccelerator(KeyCombination.keyCombination(accelerator));
        m.setOnAction(e -> action.run());
        return m;
    }

    private ToolBar buildToolbar() {
        Button newButton = toolButton("Nuevo", "Nuevo proyecto (Ctrl+N)", this::newProject);
        Button openButton = toolButton("Abrir", "Abrir proyecto (Ctrl+O)", this::openProject);
        Button saveButton = toolButton("Guardar", "Guardar (Ctrl+S)", this::save);
        saveButton.getStyleClass().add("primary");
        undoButton.setTooltip(new Tooltip("Deshacer (Ctrl+Z)"));
        undoButton.setOnAction(e -> undo());
        redoButton.setTooltip(new Tooltip("Rehacer (Ctrl+Y)"));
        redoButton.setOnAction(e -> redo());

        undoButton.setFocusTraversable(false);
        redoButton.setFocusTraversable(false);
        view3d.setFocusTraversable(false);
        viewPlan.setFocusTraversable(false);
        dimensions.setFocusTraversable(false);
        view3d.setToggleGroup(viewGroup);
        viewPlan.setToggleGroup(viewGroup);
        view3d.setSelected(true);
        view3d.setTooltip(new Tooltip("Ctrl+1"));
        viewPlan.setTooltip(new Tooltip("Ctrl+2"));
        view3d.getStyleClass().add("segment-left");
        viewPlan.getStyleClass().add("segment-right");
        viewGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == null) {
                o.setSelected(true); // siempre hay una vista elegida
                return;
            }
            view.setMode(n == viewPlan ? SceneView.Mode.PLAN : SceneView.Mode.THREE_D);
            view.frameRoom();
        });
        HBox segmented = new HBox(view3d, viewPlan);

        dimensions.setSelected(true);
        dimensions.setTooltip(new Tooltip("Mostrar u ocultar las medidas (Ctrl+M)"));
        dimensions.selectedProperty().addListener((obs, o, n) -> view.setDimensionsVisible(n));
        Button center = toolButton("Centrar", "Encuadrar la habitación (Ctrl+0)", view::frameRoom);
        Button capture = toolButton("Captura", "Exportar imagen PNG (Ctrl+E)", this::exportImage);

        return new ToolBar(newButton, openButton, saveButton, new Separator(Orientation.VERTICAL),
                undoButton, redoButton, new Separator(Orientation.VERTICAL),
                segmented, dimensions, center, capture);
    }

    /**
     * Los botones no toman el foco al hacer clic: así la barra espaciadora no repite la última
     * acción (antes agregaba otra mesa) y los atajos como R o Supr siguen yendo a la vista 3D.
     */
    private static Button toolButton(String text, String tooltip, Runnable action) {
        Button b = new Button(text);
        b.setTooltip(new Tooltip(tooltip));
        b.setOnAction(e -> action.run());
        b.setFocusTraversable(false);
        return b;
    }

    /** Panel izquierdo en pestañas: antes era una sola columna larga y los muebles quedaban fuera de la vista. */
    private Node buildSidebar() {
        TabPane tabs = new TabPane(
                tab("Muebles", buildCatalog()),
                tab("Habitación", new VBox(14,
                        group("Medidas", buildRoomForm()),
                        group("Paredes y piso", buildSurfaceButtons()),
                        group("Ventanas y puertas", row(
                                smallButton("+ Ventana", "Agrega una ventana en el primer lugar libre", () -> addOpening(OpeningType.WINDOW)),
                                smallButton("+ Puerta", "Agrega una puerta en el primer lugar libre", () -> addOpening(OpeningType.DOOR)))))),
                tab("Luz", buildLightingControls()),
                tab("Lista", buildItemList()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("sidebar-tabs");
        tabs.setPrefWidth(310);
        tabs.setMinWidth(280);
        return tabs;
    }

    private static Tab tab(String title, Node content) {
        VBox box = new VBox(content);
        box.getStyleClass().add("sidebar");
        VBox.setVgrow(content, Priority.ALWAYS);
        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("side-scroll");
        return new Tab(title, scroll);
    }

    private static VBox group(String title, Node content) {
        Label label = new Label(title);
        label.getStyleClass().add("group-title");
        VBox box = new VBox(8, label, content);
        box.getStyleClass().add("group");
        return box;
    }

    /** Vista 3D con los avisos flotantes y los botones de zoom encima. */
    private Node buildViewport() {
        StackPane viewport = new StackPane(view.node());
        viewport.setMinSize(0, 0);
        toast = new Toast(viewport);
        Button zoomIn = toolButton("+", "Acercar (rueda del ratón)", () -> view.zoom(0.8));
        Button zoomOut = toolButton("−", "Alejar (rueda del ratón)", () -> view.zoom(1.25));
        Button center = toolButton("⤢", "Encuadrar la habitación (Ctrl+0)", view::frameRoom);
        VBox controls = new VBox(4, zoomIn, zoomOut, center);
        controls.getStyleClass().add("view-controls");
        controls.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(controls, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(controls, new Insets(0, 12, 12, 0));
        viewport.getChildren().add(controls);
        return viewport;
    }

    /** Lo que muestra el panel derecho cuando no hay nada seleccionado. */
    private Node buildEmptyState() {
        Label intro = new Label("Haz clic en una pared, el piso, una ventana, una luz o un mueble para ver y cambiar sus propiedades.");
        intro.setWrapText(true);
        intro.getStyleClass().add("hint");
        Label quick = new Label("Para empezar");
        quick.getStyleClass().add("subheading");
        Button walls = smallButton("Cambiar el material de las paredes", "Selecciona la pared norte", () -> select(design.wall(WallSide.NORTE)));
        Button floor = smallButton("Cambiar el piso", "Selecciona el piso", () -> select(design.floor()));
        Label tips = new Label("""
                Atajos útiles
                • Arrastra un mueble para moverlo
                • Flechas: mover 5 cm (Shift: 1 cm)
                • R / Shift+R: girar 90°
                • Supr: eliminar · Ctrl+D: duplicar
                • Ctrl+Z / Ctrl+Y: deshacer / rehacer
                • F1: todos los atajos""");
        tips.getStyleClass().add("tips");
        tips.setWrapText(true);
        return new VBox(10, intro, quick, walls, floor, tips);
    }

    private static HBox row(Node... nodes) {
        HBox box = new HBox(6, nodes);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static Button smallButton(String text, String tooltip, Runnable action) {
        Button b = toolButton(text, tooltip, action);
        b.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(b, Priority.ALWAYS);
        return b;
    }

    private Node buildRoomForm() {
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        TextField[] fields = {roomWidth, roomLength, roomHeight, roomWall};
        String[] labels = {"Ancho (X)", "Largo (Z)", "Alto (Y)", "Grosor de muro"};
        for (int i = 0; i < fields.length; i++) {
            fields[i].setPrefColumnCount(6);
            fields[i].setOnAction(e -> applyRoom());
            grid.addRow(i, new Label(labels[i]), fields[i], new Label("m"));
            GridPane.setHgrow(fields[i], Priority.ALWAYS);
        }
        roomError.getStyleClass().add("error");
        roomError.setWrapText(true);
        roomError.setManaged(false);
        roomInfo.getStyleClass().add("hint");
        Button apply = new Button("Aplicar medidas");
        apply.getStyleClass().add("primary");
        apply.setMaxWidth(Double.MAX_VALUE);
        apply.setOnAction(e -> applyRoom());
        return new VBox(8, grid, roomError, apply, roomInfo);
    }

    /** Botones del piso y de cada pared, con una muestra de su material actual. */
    private Node buildSurfaceButtons() {
        floorButton.setOnAction(e -> select(design.floor()));
        floorButton.setTooltip(new Tooltip("Material y color del piso"));
        floorButton.getStyleClass().add("surface-button");
        floorButton.setMaxWidth(Double.MAX_VALUE);
        floorButton.setFocusTraversable(false);
        GridPane walls = new GridPane();
        walls.setHgap(6);
        walls.setVgap(6);
        int i = 0;
        for (WallSide side : WallSide.values()) {
            Button b = new Button(side.label());
            b.setTooltip(new Tooltip("Material y color de la pared " + side.id()));
            b.setOnAction(e -> select(design.wall(side)));
            b.getStyleClass().add("surface-button");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setFocusTraversable(false);
            GridPane.setHgrow(b, Priority.ALWAYS);
            walls.add(b, i % 2, i / 2);
            wallButtons.put(side, b);
            i++;
        }
        Label hint = new Label("También puedes hacer clic en una pared o en el piso de la vista 3D.");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);
        return new VBox(8, floorButton, walls, hint);
    }

    /** Muestra del material de una superficie para su botón. */
    private static Node swatch(Surface s) {
        Rectangle r = new Rectangle(26, 18);
        r.setArcWidth(4);
        r.setArcHeight(4);
        r.setFill(new javafx.scene.paint.ImagePattern(SurfacePreview.of(s.material, s.color), 0, 0, 0.5, 0.5, true));
        r.setStroke(Color.web("#00000066"));
        return r;
    }

    private Node buildLightingControls() {
        ambient.setBlockIncrement(0.05);
        ambient.setMajorTickUnit(0.5);
        ambient.valueProperty().addListener((obs, o, n) -> {
            ambientValue.setText(Units.number(n.doubleValue()));
            if (syncing) return;
            design.setAmbient(Units.round(n.doubleValue()));
            history.markPending();
        });
        // Se registra un solo paso de deshacer al soltar el control.
        ambient.valueChangingProperty().addListener((obs, o, changing) -> {
            if (!changing) history.flush();
        });
        ambient.setOnMouseReleased(e -> history.flush());
        ambient.setOnKeyReleased(e -> history.flush());
        HBox label = row(new Label("Intensidad"), spacer(), ambientValue);
        Label info = new Label("JavaFX ilumina cada objeto con " + Design.MAX_ACTIVE_LIGHTS
                + " luces como máximo. El sol entra por una ventana y dibuja su mancha de luz en el piso.");
        info.getStyleClass().add("hint");
        info.setWrapText(true);
        return new VBox(14,
                group("Luz ambiental", new VBox(6, label, ambient)),
                group("Agregar luces", new VBox(6, row(
                        smallButton("+ Bombilla", "Luz puntual", () -> addLight(LightType.POINT)),
                        smallButton("+ Foco", "Cono de luz dirigido", () -> addLight(LightType.SPOT)),
                        smallButton("+ Sol", "Luz solar que entra por una ventana", () -> addLight(LightType.SUN))), info)));
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private Node buildCatalog() {
        Button importButton = smallButton("Importar modelo .glb/.gltf…", "Agregar un modelo 3D al catálogo (Ctrl+I)", this::importModel);
        Label hint = new Label("Clic para agregar en el lugar libre más cercano al centro. Clic derecho en un modelo importado para quitarlo.");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);
        return new VBox(14,
                group("Paramétricos (medidas y color editables)", parametricButtons),
                group("Modelos 3D", new VBox(8, modelButtons, importButton)),
                hint);
    }

    private Node buildItemList() {
        items.setPlaceholder(new Label("Vacío"));
        items.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : design.label(item));
                setGraphic(empty || item == null ? null : new Circle(5, Color.web(dotColor(item))));
            }
        });
        items.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (!syncing && n != null) select(n);
        });
        Label hint = new Label("Clic para seleccionar · Supr para eliminar");
        hint.getStyleClass().add("hint");
        VBox.setVgrow(items, Priority.ALWAYS);
        return new VBox(8, items, hint);
    }

    /** Color del punto de cada tipo de elemento en la lista. */
    private static String dotColor(Object entity) {
        if (entity instanceof Opening o) return o.type == OpeningType.DOOR ? "#c08457" : "#9fd3ff";
        if (entity instanceof Light l) return l.type == LightType.SUN ? "#ffc845" : "#ffe2b8";
        return "#7ee0a1";
    }

    private Node buildStatusBar() {
        status.getStyleClass().add("status-text");
        selectionInfo.getStyleClass().add("status-text");
        HBox bar = new HBox(12, status, spacer(), selectionInfo);
        bar.getStyleClass().add("status-bar");
        bar.setPadding(new Insets(4, 10, 4, 10));
        return bar;
    }

    private void rebuildCatalogButtons() {
        parametricButtons.getChildren().clear();
        for (CatalogEntry e : catalog.list(CatalogEntry.Kind.PARAMETRIC)) parametricButtons.getChildren().add(catalogButton(e));
        modelButtons.getChildren().clear();
        for (CatalogEntry e : catalog.list(CatalogEntry.Kind.MODEL)) modelButtons.getChildren().add(catalogButton(e));
        if (modelButtons.getChildren().isEmpty()) {
            Label none = new Label("No hay modelos.");
            none.getStyleClass().add("hint");
            modelButtons.getChildren().add(none);
        }
    }

    private Button catalogButton(CatalogEntry e) {
        CatalogEntry.Size s = e.defaults();
        Button b = new Button(e.name() + "\n" + Units.number(s.width()) + " × " + Units.number(s.depth()) + " m");
        b.getStyleClass().add("catalog-button");
        b.setFocusTraversable(false);
        b.setTooltip(new Tooltip("Agregar " + e.name().toLowerCase() + " (" + Units.number(s.width()) + " x "
                + Units.number(s.depth()) + " x " + Units.number(s.height()) + " m)"));
        b.setOnAction(ev -> addFurniture(e.id()));
        if (e.source() == CatalogEntry.Source.USER) {
            MenuItem remove = new MenuItem("Quitar del catálogo…");
            remove.setOnAction(ev -> removeUserModel(e));
            b.setContextMenu(new ContextMenu(remove));
        }
        return b;
    }

    // ---------------------------------------------------------------- Sincronizar con el proyecto

    /** Tras cualquier cambio: actualiza la lista, el panel de la habitación y las propiedades. */
    private void designChanged() {
        syncing = true;
        RoomDims r = design.room();
        // Corrección: antes no se actualizaban si el campo tenía el foco, y tras deshacer quedaba el valor viejo.
        roomWidth.setText(Units.number(r.width()));
        roomLength.setText(Units.number(r.length()));
        roomHeight.setText(Units.number(r.height()));
        roomWall.setText(Units.number(r.wallThickness()));
        roomInfo.setText("Área: " + Units.number(Units.round(r.width() * r.length())) + " m²   ·   Volumen: "
                + Units.number(Units.round(r.width() * r.length() * r.height())) + " m³");
        ambient.setValue(design.ambient());
        ambientValue.setText(Units.number(design.ambient()));

        List<Object> list = new ArrayList<>(design.openings());
        list.addAll(design.lights());
        list.addAll(design.furniture());
        // Solo se reemplaza la lista si cambió qué hay; si no, se refrescan los nombres
        // (antes se reconstruía en cada movimiento al arrastrar y el scroll volvía arriba).
        if (!sameItems(items.getItems(), list)) items.getItems().setAll(list);
        else items.refresh();
        floorButton.setGraphic(swatch(design.floor()));
        for (WallSide side : WallSide.values()) wallButtons.get(side).setGraphic(swatch(design.wall(side)));
        if (selected != null && list.contains(selected)) items.getSelectionModel().select(selected);
        else items.getSelectionModel().clearSelection();
        syncing = false;

        // Si lo seleccionado ya no existe (p. ej. el sol se eliminó con su ventana), deseleccionar.
        if (selected != null && !design.entities().contains(selected)) select(null);
        else properties.refresh();
        updateSelectionInfo();
    }

    private static boolean sameItems(List<Object> a, List<Object> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) if (a.get(i) != b.get(i)) return false;
        return true;
    }

    private void select(Object entity) {
        selected = entity;
        view.setSelected(entity);
        properties.show(entity == null ? null : Editors.of(design, entity, this::duplicate));
        syncing = true;
        if (entity != null && items.getItems().contains(entity)) items.getSelectionModel().select(entity);
        else items.getSelectionModel().clearSelection();
        syncing = false;
        updateSelectionInfo();
    }

    private void updateSelectionInfo() {
        selectionInfo.setText(selected == null ? "Nada seleccionado" : "Seleccionado: " + design.label(selected));
    }

    private void updateTitle() {
        boolean dirty = history.isDirty();
        stage.setTitle(projectName + (dirty ? " •" : "") + " — " + APP_NAME);
        undoButton.setDisable(!history.canUndo());
        undoItem.setDisable(!history.canUndo());
        redoButton.setDisable(!history.canRedo());
        redoItem.setDisable(!history.canRedo());
    }

    // ---------------------------------------------------------------- Mensajes

    private void warn(String message) {
        notify(message);
        if (message.contains("\n")) Dialogs.info(stage, "Aviso", "Aviso", message);
    }

    private void notify(String message) {
        notify(message, Toast.Kind.INFO);
    }

    private void notify(String message, Toast.Kind kind) {
        status.setText(message.replace('\n', ' '));
        statusTimer.playFromStart();
        toast.show(message, kind);
    }

    private String defaultStatus() {
        return "3D: arrastra para girar · botón derecho para mover · rueda para acercar · arrastra un mueble para moverlo";
    }

    /** Muestra el error de una acción o la registra en el historial. */
    private <T> void handle(Outcome<T> outcome, Consumer<T> onOk) {
        if (outcome.failed()) {
            // Aviso flotante en lugar de una ventana que obliga a hacer clic.
            notify(outcome.error(), Toast.Kind.ERROR);
            return;
        }
        history.commit();
        onOk.accept(outcome.value());
    }

    // ---------------------------------------------------------------- Acciones de edición

    private void applyRoom() {
        Double w = Units.parse(roomWidth.getText()), l = Units.parse(roomLength.getText());
        Double h = Units.parse(roomHeight.getText()), t = Units.parse(roomWall.getText());
        String err;
        RoomDims dims = null;
        if (w == null || l == null || h == null || t == null) {
            err = "Escribe todas las medidas como números (por ejemplo 3,5).";
        } else {
            dims = new RoomDims(w, l, h, t);
            err = dims.validate();
        }
        roomError.setText(err == null ? "" : err);
        roomError.setManaged(err != null);
        roomError.setVisible(err != null);
        if (err != null) return;
        view.node().requestFocus(); // los campos dejan de estar "en edición" y se actualizan
        List<String> warnings = design.setRoom(dims);
        history.commit();
        view.frameRoom();
        if (!warnings.isEmpty()) warn(String.join("\n", warnings));
        else notify("Medidas aplicadas.");
    }

    private void addOpening(OpeningType type) {
        handle(design.addOpening(type), this::select);
    }

    private void addLight(LightType type) {
        handle(design.addLight(type), this::select);
    }

    private void addFurniture(String type) {
        handle(design.addFurniture(type), this::select);
    }

    /** Para el botón "Duplicar" del panel de propiedades. Devuelve un error o null. */
    private String duplicate(Furniture f) {
        Outcome<Furniture> out = design.duplicate(f);
        if (out.failed()) return out.error();
        history.commit();
        select(out.value());
        return null;
    }

    private void duplicateSelected() {
        if (selected instanceof Furniture f) {
            String err = duplicate(f);
            if (err != null) notify(err, Toast.Kind.ERROR);
        } else {
            notify("Selecciona un mueble para duplicarlo.", Toast.Kind.ERROR);
        }
    }

    private void delete(Object entity) {
        if (entity == null || entity instanceof Surface) return;
        String label = design.label(entity);
        design.remove(entity);
        history.commit();
        select(null);
        notify("Se eliminó " + label + ".");
    }

    private void rotateSelected(double degrees) {
        if (!(selected instanceof Furniture f)) return;
        String err = design.rotateFurniture(f, degrees);
        if (err != null) notify(err, Toast.Kind.ERROR);
        else history.commit();
    }

    private void undo() {
        commitEdits();
        if (history.undo()) notify("Deshecho.");
    }

    private void redo() {
        commitEdits();
        if (history.redo()) notify("Rehecho.");
    }

    /**
     * Aplica lo que se esté escribiendo en un campo (se aplica al perder el foco).
     * Corrección: antes, escribir un valor y presionar Ctrl+S guardaba el valor anterior.
     */
    private void commitEdits() {
        if (stage.getScene().getFocusOwner() instanceof TextInputControl) view.node().requestFocus();
    }

    /** Aplica una "foto" del historial conservando la selección. */
    private void restore(String json) {
        String id = selected != null ? Design.idOf(selected) : null;
        try {
            design.load(ProjectJson.read(json));
        } catch (ProjectFormatException e) {
            Dialogs.error(stage, "No se pudo deshacer", e.getMessage());
            return;
        }
        select(id == null ? null : design.findById(id));
    }

    private void onKey(KeyEvent e) {
        // Mientras se escribe en un campo, las teclas son del campo.
        if (stage.getScene().getFocusOwner() instanceof TextInputControl) return;
        if (e.isShortcutDown() || e.isAltDown()) return;
        if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) delete(selected);
        else if (e.getCode() == KeyCode.ESCAPE) select(null);
        else if (e.getCode() == KeyCode.R) rotateSelected(e.isShiftDown() ? 90 : -90);
        else if (e.getCode().isArrowKey() && selected instanceof Furniture f) nudge(f, e.getCode(), e.isShiftDown() ? 0.01 : 0.05);
        else return;
        e.consume();
    }

    /** Mueve el mueble con las flechas (arriba = norte). Respeta las mismas reglas que el panel. */
    private void nudge(Furniture f, KeyCode key, double step) {
        Editable editor = Editors.of(design, f, this::duplicate);
        String err = switch (key) {
            case LEFT -> editor.set("x", Units.round(f.x - step));
            case RIGHT -> editor.set("x", Units.round(f.x + step));
            case UP -> editor.set("z", Units.round(f.z - step));
            default -> editor.set("z", Units.round(f.z + step));
        };
        if (err != null) {
            notify(err, Toast.Kind.ERROR);
            return;
        }
        nudging = true;
        history.markPending();
    }

    // ---------------------------------------------------------------- Proyectos (base de datos)

    /** Si hay cambios sin guardar, pregunta. Devuelve false si el usuario canceló. */
    private boolean confirmDiscard() {
        commitEdits();
        if (!history.isDirty()) return true;
        ButtonType answer = Dialogs.unsavedChanges(stage, projectName);
        if (answer == Dialogs.SAVE) return save();
        return answer == Dialogs.DISCARD;
    }

    private void newProject() {
        if (!confirmDiscard()) return;
        select(null);
        design.loadDefault();
        projectId = null;
        projectName = UNTITLED;
        history.reset();
        view.frameRoom();
        notify("Proyecto nuevo.");
    }

    private void openProject() {
        if (!confirmDiscard()) return;
        ProjectsDialog.show(stage, projects, projectId).ifPresent(id -> {
            try {
                Project p = projects.load(id);
                if (p == null) {
                    Dialogs.error(stage, "No se pudo abrir", "El proyecto ya no existe.");
                    return;
                }
                String name = projects.list().stream().filter(s -> s.id() == id).map(ProjectRepository.Summary::name)
                        .findFirst().orElse(UNTITLED);
                loadProject(p, id, name);
            } catch (SQLException | ProjectFormatException e) {
                Dialogs.error(stage, "No se pudo abrir el proyecto", e.getMessage());
            }
        });
    }

    private void loadProject(Project p, Long id, String name) {
        select(null);
        List<String> warnings = design.load(p);
        projectId = id;
        projectName = name;
        history.reset();
        view.frameRoom();
        notify("Abierto: " + name);
        if (!warnings.isEmpty()) warn(String.join("\n", warnings));
    }

    /** Devuelve true si se guardó. */
    private boolean save() {
        commitEdits();
        if (projectId == null) return saveAs();
        return saveTo(projectId, projectName);
    }

    private boolean saveAs() {
        commitEdits();
        String initial = UNTITLED.equals(projectName) ? "Mi habitación" : projectName;
        var name = Dialogs.askText(stage, "Guardar como", "Nombre del proyecto", initial);
        if (name.isEmpty()) return false;
        try {
            Long existing = projects.findByName(name.get());
            if (existing != null && !existing.equals(projectId)
                    && !Dialogs.confirm(stage, "Ya existe \"" + name.get() + "\"", "¿Reemplazarlo con este proyecto?", "Reemplazar")) {
                return false;
            }
            return saveTo(existing, name.get());
        } catch (SQLException e) {
            Dialogs.error(stage, "No se pudo guardar", e.getMessage());
            return false;
        }
    }

    private boolean saveTo(Long id, String name) {
        history.flush();
        try {
            byte[] thumbnail = thumbnail();
            projectId = projects.save(id, name, design.snapshot(), thumbnail);
            projectName = name;
            history.markSaved();
            notify("Guardado: " + name);
            return true;
        } catch (SQLException e) {
            Dialogs.error(stage, "No se pudo guardar", e.getMessage());
            return false;
        }
    }

    /** Miniatura de la vista actual para la lista de proyectos (si falla, se guarda sin ella). */
    private byte[] thumbnail() {
        try {
            double w = Math.max(view.node().getLayoutBounds().getWidth(), 1);
            return Png.encode(view.snapshot(Math.min(1, 240 / w)));
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- Archivos

    private void importFile() {
        if (!confirmDiscard()) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importar proyecto");
        chooser.getExtensionFilters().add(PROJECT_FILES);
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        try {
            Project p = ProjectJson.read(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            String name = file.getName().replaceFirst("\\.amob3d$", "");
            // Un archivo importado es un proyecto nuevo: al guardarlo se pedirá el nombre.
            loadProject(p, null, name);
            history.markSaved();
        } catch (IOException | ProjectFormatException e) {
            Dialogs.error(stage, "No se pudo importar el archivo", e.getMessage());
        }
    }

    private void exportFile() {
        commitEdits();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar proyecto");
        chooser.getExtensionFilters().add(PROJECT_FILES);
        chooser.setInitialFileName(projectName + ".amob3d");
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            Files.writeString(file.toPath(), ProjectJson.write(design.snapshot()), StandardCharsets.UTF_8);
            notify("Exportado: " + file.getName());
        } catch (IOException e) {
            Dialogs.error(stage, "No se pudo exportar", e.getMessage());
        }
    }

    private void exportImage() {
        commitEdits();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar imagen");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen PNG (*.png)", "*.png"));
        chooser.setInitialFileName(projectName + ".png");
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            Files.write(file.toPath(), Png.encode(view.snapshot(2)));
            notify("Imagen guardada: " + file.getName());
        } catch (IOException e) {
            Dialogs.error(stage, "No se pudo guardar la imagen", e.getMessage());
        }
    }

    // ---------------------------------------------------------------- Catálogo

    private void importModel() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importar modelo 3D");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Modelos glTF (*.glb, *.gltf)", "*.glb", "*.gltf"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        UserModels.Imported imported;
        ModelData data;
        try {
            imported = userModels.copyIntoLibrary(file.toPath());
        } catch (IOException e) {
            Dialogs.error(stage, "No se pudo importar el modelo", e.getMessage());
            return;
        }
        try {
            data = GltfLoader.load(imported.path().toUri().toURL());
        } catch (IOException | RuntimeException e) {
            discard(imported);
            Dialogs.error(stage, "No se pudo leer el modelo", e.getMessage());
            return;
        }
        var result = ImportModelDialog.show(stage, imported.name(), data);
        if (result.isEmpty()) {
            discard(imported);
            return;
        }
        try {
            userModels.save(imported, result.get().name(), result.get().size(), result.get().rotation());
        } catch (SQLException e) {
            discard(imported);
            Dialogs.error(stage, "No se pudo guardar el modelo en el catálogo", e.getMessage());
            return;
        }
        reloadCatalog();
        notify("\"" + result.get().name() + "\" se agregó al catálogo.");
    }

    private void discard(UserModels.Imported imported) {
        try {
            userModels.discard(imported);
        } catch (IOException ignored) {
            // Queda una carpeta sin usar; no impide seguir trabajando.
        }
    }

    private void removeUserModel(CatalogEntry e) {
        long used = design.furniture().stream().filter(f -> f.type.equals(e.id())).count();
        String message = used > 0
                ? "Hay " + used + " en la habitación: se mostrarán como cajas rojas hasta que los elimines."
                : "Se borrarán también sus archivos.";
        if (!Dialogs.confirm(stage, "¿Quitar \"" + e.name() + "\" del catálogo?", message, "Quitar")) return;
        try {
            userModels.remove(e.id());
        } catch (SQLException | IOException ex) {
            Dialogs.error(stage, "No se pudo quitar el modelo", ex.getMessage());
            return;
        }
        reloadCatalog();
    }

    private void reloadCatalog() {
        List<String> warnings = new ArrayList<>();
        catalog.setModels(loadModels(userModels, warnings));
        models.clear();
        view.refreshFurniture();
        rebuildCatalogButtons();
        designChanged();
        if (!warnings.isEmpty()) warn(String.join("\n", warnings));
    }

    /** Modelos incluidos + importados. Los problemas se agregan a {@code warnings}. */
    public static List<CatalogEntry> loadModels(UserModels userModels, List<String> warnings) {
        List<CatalogEntry> all = new ArrayList<>(BuiltinCatalog.load(warnings));
        try {
            all.addAll(userModels.load(warnings));
        } catch (SQLException e) {
            warnings.add("No se pudieron leer los modelos importados: " + e.getMessage());
        }
        return all;
    }

    private void openModelsFolder() {
        Path dir = userModels.root();
        try {
            Files.createDirectories(dir);
            host.showDocument(dir.toUri().toString());
        } catch (IOException e) {
            Dialogs.error(stage, "No se pudo abrir la carpeta", e.getMessage());
        }
    }

    private void showShortcuts() {
        Dialogs.info(stage, "Atajos de teclado", "Atajos de teclado", """
                Ctrl+N  Nuevo proyecto        Ctrl+O  Abrir
                Ctrl+S  Guardar               Ctrl+Shift+S  Guardar como
                Ctrl+Z  Deshacer              Ctrl+Y  Rehacer
                Ctrl+D  Duplicar mueble       Supr  Eliminar selección
                R / Shift+R  Girar mueble 90°  Esc  Deseleccionar
                Ctrl+1  Vista 3D              Ctrl+2  Planta 2D
                Ctrl+M  Cotas                 Ctrl+0  Centrar cámara
                Ctrl+E  Exportar imagen       Ctrl+I  Importar modelo 3D

                Ratón (3D): botón izquierdo gira, derecho mueve, rueda acerca.
                Arrastra un mueble para moverlo; clic en una pared o el piso para cambiar su material.""");
    }
}
