package pe.edu.unaj.amoblador3d.ui;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import pe.edu.unaj.amoblador3d.persistence.ProjectRepository;
import pe.edu.unaj.amoblador3d.persistence.ProjectRepository.Summary;
import pe.edu.unaj.amoblador3d.util.Units;

/** Lista de proyectos guardados en la base de datos: abrir, renombrar o eliminar. */
final class ProjectsDialog {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(Units.LOCALE).withZone(ZoneId.systemDefault());

    private ProjectsDialog() {}

    /**
     * @param currentId el proyecto abierto (no se puede eliminar desde aquí), o null
     * @return el id del proyecto a abrir
     */
    static Optional<Long> show(Window owner, ProjectRepository repo, Long currentId) {
        Dialog<Long> dialog = new Dialog<>();
        Dialogs.prepare(dialog, owner, "Abrir proyecto", "Proyectos guardados");
        ButtonType open = new ButtonType("Abrir", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(open, ButtonType.CANCEL);

        ListView<Summary> list = new ListView<>();
        list.setPrefSize(520, 400);
        list.setCellFactory(v -> new SummaryCell());
        Label empty = new Label("Todavía no hay proyectos guardados.\nUsa Archivo → Guardar (Ctrl+S).");
        empty.getStyleClass().add("hint");
        list.setPlaceholder(empty);

        Label error = new Label();
        error.getStyleClass().add("error");
        Runnable reload = () -> {
            try {
                list.getItems().setAll(repo.list());
                if (!list.getItems().isEmpty()) list.getSelectionModel().select(0);
            } catch (SQLException e) {
                error.setText("No se pudo leer la lista: " + e.getMessage());
            }
        };
        reload.run();

        Button rename = new Button("Renombrar…");
        Button delete = new Button("Eliminar");
        delete.getStyleClass().add("danger");
        rename.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        delete.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        rename.setOnAction(e -> {
            Summary s = list.getSelectionModel().getSelectedItem();
            Dialogs.askText(dialog.getDialogPane().getScene().getWindow(), "Renombrar", "Nuevo nombre del proyecto", s.name())
                    .ifPresent(name -> {
                        try {
                            Long other = repo.findByName(name);
                            if (other != null && other != s.id()) {
                                error.setText("Ya existe un proyecto llamado \"" + name + "\".");
                                return;
                            }
                            repo.rename(s.id(), name);
                            error.setText("");
                            reload.run();
                        } catch (SQLException ex) {
                            error.setText("No se pudo renombrar: " + ex.getMessage());
                        }
                    });
        });
        delete.setOnAction(e -> {
            Summary s = list.getSelectionModel().getSelectedItem();
            if (currentId != null && currentId == s.id()) {
                error.setText("Es el proyecto abierto: crea o abre otro antes de eliminarlo.");
                return;
            }
            Window w = dialog.getDialogPane().getScene().getWindow();
            if (!Dialogs.confirm(w, "¿Eliminar \"" + s.name() + "\"?", "Esta acción no se puede deshacer.", "Eliminar")) return;
            try {
                repo.delete(s.id());
                error.setText("");
                reload.run();
            } catch (SQLException ex) {
                error.setText("No se pudo eliminar: " + ex.getMessage());
            }
        });
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && list.getSelectionModel().getSelectedItem() != null) {
                dialog.setResult(list.getSelectionModel().getSelectedItem().id());
                dialog.close();
            }
        });

        HBox tools = new HBox(8, rename, delete);
        VBox box = new VBox(8, list, tools, error);
        box.setPadding(new Insets(4, 0, 0, 0));
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().lookupButton(open).disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        dialog.setResultConverter(b -> {
            Summary s = list.getSelectionModel().getSelectedItem();
            return b == open && s != null ? s.id() : null;
        });
        return dialog.showAndWait();
    }

    /** Fila con la miniatura, el nombre y la fecha de modificación. */
    private static final class SummaryCell extends ListCell<Summary> {
        @Override
        protected void updateItem(Summary s, boolean empty) {
            super.updateItem(s, empty);
            if (empty || s == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            StackPane thumb = new StackPane();
            thumb.setPrefSize(96, 64);
            thumb.setMinSize(96, 64);
            thumb.getStyleClass().add("thumb");
            if (s.thumbnail() != null) {
                ImageView img = new ImageView(new Image(new ByteArrayInputStream(s.thumbnail())));
                img.setFitWidth(96);
                img.setFitHeight(64);
                img.setPreserveRatio(true);
                thumb.getChildren().add(img);
            }
            Label name = new Label(s.name());
            name.getStyleClass().add("item-name");
            Label date = new Label("Modificado: " + DATE.format(s.updatedAt()));
            date.getStyleClass().add("hint");
            HBox row = new HBox(10, thumb, new VBox(2, name, date));
            row.setAlignment(Pos.CENTER_LEFT);
            setText(null);
            setGraphic(row);
        }
    }
}
