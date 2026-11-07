package pe.edu.unaj.amoblador3d.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalogEntry;
import pe.edu.unaj.amoblador3d.scene.FSHFThumbnails;
import pe.edu.unaj.amoblador3d.util.FSHFUnits;

/**
 * Pestaña "Muebles" del panel izquierdo: buscador, categorías y tarjetas con la miniatura de cada
 * mueble. Clic en una tarjeta para agregar el mueble, arrastrarla para ponerlo donde se suelte.
 *
 * <p>Antes vivía dentro de {@link FSHFMainWindow}; separado, la ventana solo recibe lo que el usuario
 * pidió a través de {@link Actions}.
 */
final class FSHFCatalogPanel {

    /** Lo que el catálogo le pide a la ventana principal. */
    interface Actions {
        void add(String type);

        void importModel();

        void openDesigner();

        void editDesign(FSHFCatalogEntry e);

        void duplicateDesign(FSHFCatalogEntry e);

        void removeDesign(FSHFCatalogEntry e);

        void removeUserModel(FSHFCatalogEntry e);

        /** Empezó a arrastrarse un mueble: avisar en la barra de estado. */
        void dragStarted(FSHFCatalogEntry e);

        /** Devolver el teclado a la vista 3D (Esc en el buscador). */
        void focusView();
    }

    /** Prefijo de lo que se arrastra desde el catálogo hasta la vista 3D. */
    private static final String DRAG_PREFIX = "amoblador3d:mueble:";

    private final FSHFCatalog catalog;
    private final FSHFThumbnails thumbnails;
    private final Actions actions;
    private final TextField search = new TextField();
    private final VBox groups = new VBox(14);
    private final FlowPane chips = new FlowPane();
    private final Node node;
    /** Categoría elegida (null = todas). */
    private String category;

    FSHFCatalogPanel(FSHFCatalog catalog, FSHFThumbnails thumbnails, Actions actions) {
        this.catalog = catalog;
        this.thumbnails = thumbnails;
        this.actions = actions;
        this.node = build();
    }

    Node node() {
        return node;
    }

    /** Id del mueble que se está arrastrando desde el catálogo, o null si es otra cosa. */
    static String draggedType(Dragboard board) {
        String s = board.getString();
        return s != null && s.startsWith(DRAG_PREFIX) ? s.substring(DRAG_PREFIX.length()) : null;
    }

    private Node build() {
        Button importButton = FSHFMainWindow.smallButton("Importar modelo .glb/.gltf…",
                "Agregar un modelo 3D al catálogo (Ctrl+I)", actions::importModel);
        Button designButton = FSHFMainWindow.smallButton("✎  Diseñar un mueble nuevo",
                "Crea tu propio mueble con piezas: medidas, colores y acabados a tu gusto (Ctrl+Shift+D)",
                actions::openDesigner);
        designButton.getStyleClass().addAll("primary", "big-action");
        Label hint = new Label("Haz clic en un mueble para agregarlo, o arrástralo hasta el lugar de la habitación donde lo "
                + "quieres. Clic derecho en tus diseños («✎») o modelos importados para editarlos o quitarlos.");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);
        search.setPromptText("🔍  Buscar mueble…");
        search.setTooltip(new Tooltip("Escribe parte del nombre (Esc para borrar)"));
        search.textProperty().addListener((obs, o, n) -> rebuild());
        search.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                search.clear();
                actions.focusView();
                e.consume();
            }
        });
        chips.setHgap(5);
        chips.setVgap(5);
        chips.getStyleClass().add("category-chips");
        return new VBox(10, designButton, search, chips, groups, importButton, hint);
    }

    /** Vuelve a dibujar las categorías y las tarjetas (al buscar o al cambiar el catálogo). */
    void rebuild() {
        String query = search.getText();
        // Chips: "Todos" y cada categoría que tiene algo, con cuántos muebles hay.
        chips.getChildren().clear();
        ToggleGroup group = new ToggleGroup();
        List<String> available = new ArrayList<>();
        for (String c : FSHFCatalog.CATEGORIES) if (!catalog.search(c, "").isEmpty()) available.add(c);
        if (category != null && !available.contains(category)) category = null;
        List<String> chipNames = new ArrayList<>();
        chipNames.add(null);
        chipNames.addAll(available);
        for (String c : chipNames) {
            int count = c == null ? 0 : catalog.search(c, "").size();
            ToggleButton chip = new ToggleButton(c == null ? "Todos" : c + "  " + count);
            chip.getStyleClass().add("chip");
            chip.setToggleGroup(group);
            chip.setFocusTraversable(false);
            chip.setSelected(Objects.equals(c, category));
            chip.setOnAction(e -> {
                category = c;
                rebuild();
            });
            chips.getChildren().add(chip);
        }

        groups.getChildren().clear();
        for (String c : FSHFCatalog.CATEGORIES) {
            if (category != null && !c.equals(category)) continue;
            List<FSHFCatalogEntry> entries = catalog.search(c, query);
            if (entries.isEmpty()) continue;
            // Dos columnas que ocupan todo el ancho (con o sin barra de desplazamiento).
            GridPane cards = new GridPane();
            cards.setHgap(8);
            cards.setVgap(8);
            for (int i = 0; i < 2; i++) {
                ColumnConstraints col = new ColumnConstraints();
                col.setPercentWidth(50);
                cards.getColumnConstraints().add(col);
            }
            for (int i = 0; i < entries.size(); i++) {
                Button card = card(entries.get(i));
                card.setMaxWidth(Double.MAX_VALUE);
                cards.add(card, i % 2, i / 2);
            }
            Label title = new Label(c.toUpperCase());
            title.getStyleClass().add("section-title");
            title.setMaxWidth(Double.MAX_VALUE);
            groups.getChildren().add(new VBox(8, title, cards));
        }
        if (groups.getChildren().isEmpty()) {
            Label none = new Label("Ningún mueble se llama «" + query.trim() + "»"
                    + (category != null ? " en " + category + "." : "."));
            none.getStyleClass().add("hint");
            none.setWrapText(true);
            groups.getChildren().add(none);
        }
    }

    /** Tarjeta de un mueble: miniatura 3D, nombre y medidas. Clic para agregar, arrastrar para ubicar. */
    private Button card(FSHFCatalogEntry e) {
        FSHFCatalogEntry.Size s = e.defaults();
        boolean model = e.kind() == FSHFCatalogEntry.Kind.MODEL, custom = e.kind() == FSHFCatalogEntry.Kind.CUSTOM;
        ImageView image = new ImageView();
        image.setFitWidth(FSHFThumbnails.WIDTH);
        image.setFitHeight(FSHFThumbnails.HEIGHT);
        image.setPreserveRatio(true);
        image.setSmooth(true);
        Image thumb = thumbnails.of(e, image::setImage);
        if (thumb != null) image.setImage(thumb);
        StackPane picture = new StackPane(image);
        picture.setMinSize(FSHFThumbnails.WIDTH, FSHFThumbnails.HEIGHT);
        picture.setPrefSize(FSHFThumbnails.WIDTH, FSHFThumbnails.HEIGHT);
        picture.getStyleClass().add("card-picture");
        if (model || custom) {
            Label badge = new Label(model ? "3D" : "✎ MÍO");
            badge.getStyleClass().addAll("card-badge", custom ? "badge-custom" : "badge-model");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            picture.getChildren().add(badge);
        }
        Label name = new Label(e.name());
        name.getStyleClass().add("card-name");
        Label size = new Label(FSHFUnits.number(s.width()) + " × " + FSHFUnits.number(s.depth()) + " × " + FSHFUnits.number(s.height()) + " m");
        size.getStyleClass().add("card-size");
        VBox content = new VBox(4, picture, name, size);
        content.setAlignment(Pos.CENTER);
        Button card = new Button(null, content);
        card.getStyleClass().add("catalog-card");
        if (custom) card.getStyleClass().add("catalog-custom");
        card.setFocusTraversable(false);
        card.setTooltip(new Tooltip("Clic: agregar " + e.name().toLowerCase() + "\nArrastrar: ponerlo donde lo sueltes"
                + (model ? "\nModelo 3D: trae sus propios colores; se puede teñir."
                        : custom ? "\nDiseñado por ti. Clic derecho para editarlo." : "\nMedidas, colores y acabado editables.")));
        card.setOnAction(ev -> actions.add(e.id()));
        // Se agranda un poco al pasar el ratón.
        ScaleTransition grow = new ScaleTransition(Duration.millis(120), card);
        card.setOnMouseEntered(ev -> {
            grow.stop();
            grow.setToX(1.04);
            grow.setToY(1.04);
            grow.playFromStart();
        });
        card.setOnMouseExited(ev -> {
            grow.stop();
            grow.setToX(1);
            grow.setToY(1);
            grow.playFromStart();
        });
        card.setOnDragDetected(ev -> {
            Dragboard db = card.startDragAndDrop(TransferMode.COPY);
            ClipboardContent dragged = new ClipboardContent();
            dragged.putString(DRAG_PREFIX + e.id());
            db.setContent(dragged);
            if (image.getImage() != null) db.setDragView(image.snapshot(transparentSnapshot(), null));
            actions.dragStarted(e);
            ev.consume();
        });
        if (custom) {
            MenuItem edit = new MenuItem("Editar diseño…");
            edit.setOnAction(ev -> actions.editDesign(e));
            MenuItem copy = new MenuItem("Duplicar diseño");
            copy.setOnAction(ev -> actions.duplicateDesign(e));
            MenuItem remove = new MenuItem("Quitar del catálogo…");
            remove.setOnAction(ev -> actions.removeDesign(e));
            card.setContextMenu(new ContextMenu(edit, copy, new SeparatorMenuItem(), remove));
        } else if (e.source() == FSHFCatalogEntry.Source.USER) {
            MenuItem remove = new MenuItem("Quitar del catálogo…");
            remove.setOnAction(ev -> actions.removeUserModel(e));
            card.setContextMenu(new ContextMenu(remove));
        }
        return card;
    }

    private static SnapshotParameters transparentSnapshot() {
        SnapshotParameters p = new SnapshotParameters();
        p.setFill(Color.TRANSPARENT);
        return p;
    }
}
