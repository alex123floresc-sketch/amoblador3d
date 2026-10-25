package pe.edu.unaj.amoblador3d;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.catalog.UserModels;
import pe.edu.unaj.amoblador3d.persistence.AppPaths;
import pe.edu.unaj.amoblador3d.persistence.CatalogRepository;
import pe.edu.unaj.amoblador3d.persistence.Database;
import pe.edu.unaj.amoblador3d.persistence.ProjectRepository;
import pe.edu.unaj.amoblador3d.scene.ModelLibrary;
import pe.edu.unaj.amoblador3d.ui.MainWindow;

/** Arranque: abre la base de datos, carga el catálogo y muestra la ventana principal. */
public final class App extends Application {
    private Database db;

    @Override
    public void start(Stage stage) {
        try {
            db = new Database(AppPaths.database());
        } catch (SQLException e) {
            Alert a = new Alert(Alert.AlertType.ERROR, e.getMessage() + "\n\nArchivo: " + AppPaths.database(), ButtonType.OK);
            a.setHeaderText("No se pudo abrir la base de datos");
            a.showAndWait();
            Platform.exit();
            return;
        }
        UserModels userModels = new UserModels(new CatalogRepository(db), AppPaths.modelsDir());
        List<String> warnings = new ArrayList<>();
        Catalog catalog = new Catalog();
        catalog.setModels(MainWindow.loadModels(userModels, warnings));

        var icon = App.class.getResourceAsStream("/icon.png");
        if (icon != null) stage.getIcons().add(new Image(icon));
        new MainWindow(stage, catalog, userModels, new ProjectRepository(db), new ModelLibrary(), getHostServices())
                .show(warnings);
    }

    @Override
    public void stop() throws SQLException {
        if (db != null) db.close();
    }
}
