package pe.edu.unaj.amoblador3d.ui;

import java.io.ByteArrayInputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import pe.edu.unaj.amoblador3d.persistence.ProjectRepository;

/**
 * Pantalla de bienvenida: cómo empezar (vacío, plantilla, casa de ejemplo) y los proyectos
 * recientes con su miniatura. Se puede desactivar con "Mostrar al iniciar".
 */
final class WelcomeDialog {
    /** Qué eligió el usuario. {@code projectId} solo en RECENT. */
    record Choice(Kind kind, Long projectId) {}

    enum Kind { NEW, TEMPLATE, EXAMPLE, OPEN, RECENT }

    private static final String SHOW_KEY = "bienvenida.mostrar";
    private static final Preferences PREFS = Preferences.userNodeForPackage(WelcomeDialog.class);

    private WelcomeDialog() {}

    static boolean enabled() {
        return PREFS.getBoolean(SHOW_KEY, true);
    }

    static Optional<Choice> show(Window owner, List<ProjectRepository.Summary> recent) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Bienvenido a Amoblador3D");
        Choice[] result = {null};

        Label title = new Label("¿Qué quieres hacer hoy?");
        title.getStyleClass().add("welcome-title");
        Label subtitle = new Label("Diseña y amuebla tu casa en 3D con medidas reales.");
        subtitle.getStyleClass().add("welcome-subtitle");

        TilePane start = new TilePane(12, 12);
        start.setPrefColumns(4);
        start.getChildren().addAll(
                card("＋", "Habitación vacía", "Empieza de cero con una habitación de 4 × 3,5 m", () -> {
                    result[0] = new Choice(Kind.NEW, null);
                    stage.close();
                }),
                card("▦", "Desde una plantilla", "Dormitorio, sala, cocina, baño… ya amueblados", () -> {
                    result[0] = new Choice(Kind.TEMPLATE, null);
                    stage.close();
                }),
                card("🏠", "Casa de ejemplo", "Sala, dormitorio, cocina y baño listos para recorrer y cambiar", () -> {
                    result[0] = new Choice(Kind.EXAMPLE, null);
                    stage.close();
                }),
                card("📂", "Abrir proyecto", "Todos tus proyectos guardados", () -> {
                    result[0] = new Choice(Kind.OPEN, null);
                    stage.close();
                }));

        VBox content = new VBox(14, title, subtitle, start);
        if (!recent.isEmpty()) {
            Label recentTitle = new Label("RECIENTES");
            recentTitle.getStyleClass().add("section-title");
            recentTitle.setMaxWidth(Double.MAX_VALUE);
            HBox recents = new HBox(12);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
            for (ProjectRepository.Summary s : recent.subList(0, Math.min(4, recent.size()))) {
                ImageView img = new ImageView();
                img.setFitWidth(150);
                img.setFitHeight(95);
                img.setPreserveRatio(true);
                if (s.thumbnail() != null) img.setImage(new Image(new ByteArrayInputStream(s.thumbnail())));
                StackPane pic = new StackPane(img);
                pic.setPrefSize(150, 95);
                pic.setMinSize(150, 95);
                pic.getStyleClass().add("card-picture");
                Label name = new Label(s.name());
                name.getStyleClass().add("card-name");
                Label when = new Label(fmt.format(s.updatedAt()));
                when.getStyleClass().add("card-size");
                VBox box = new VBox(5, pic, name, when);
                box.setAlignment(Pos.CENTER_LEFT);
                Button b = new Button(null, box);
                b.getStyleClass().add("catalog-card");
                b.setOnAction(e -> {
                    result[0] = new Choice(Kind.RECENT, s.id());
                    stage.close();
                });
                recents.getChildren().add(b);
            }
            content.getChildren().addAll(recentTitle, recents);
        }

        CheckBox show = new CheckBox("Mostrar al iniciar");
        show.setSelected(enabled());
        show.selectedProperty().addListener((obs, o, n) -> PREFS.putBoolean(SHOW_KEY, n));
        Button skip = new Button("Seguir con la habitación de siempre");
        skip.setOnAction(e -> stage.close());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottom = new HBox(10, show, spacer, skip);
        bottom.setAlignment(Pos.CENTER_LEFT);

        BorderPane root = new BorderPane(content);
        root.setBottom(bottom);
        BorderPane.setMargin(bottom, new Insets(16, 0, 0, 0));
        root.setPadding(new Insets(26, 30, 20, 30));
        root.getStyleClass().add("welcome");
        Scene scene = new Scene(root);
        scene.getStylesheets().add(MainWindow.STYLESHEET);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) stage.close();
        });
        stage.setScene(scene);
        stage.setResizable(false);
        stage.showAndWait();
        return Optional.ofNullable(result[0]);
    }

    /** Tarjeta grande con un ícono, un título y una explicación corta. */
    private static Button card(String icon, String title, String text, Runnable action) {
        Label i = new Label(icon);
        i.getStyleClass().add("welcome-icon");
        Label t = new Label(title);
        t.getStyleClass().add("card-name");
        Label d = new Label(text);
        d.getStyleClass().add("card-size");
        d.setWrapText(true);
        d.setMaxWidth(165);
        VBox box = new VBox(8, i, t, d);
        box.setAlignment(Pos.TOP_LEFT);
        box.setPrefSize(165, 125);
        Button b = new Button(null, box);
        b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        b.getStyleClass().addAll("catalog-card", "welcome-card");
        b.setOnAction(e -> action.run());
        return b;
    }
}
