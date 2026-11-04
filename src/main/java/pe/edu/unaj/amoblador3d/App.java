package pe.edu.unaj.amoblador3d;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import pe.edu.unaj.amoblador3d.estimate.PriceList;
import pe.edu.unaj.amoblador3d.persistence.AppPaths;
import pe.edu.unaj.amoblador3d.persistence.CatalogRepository;
import pe.edu.unaj.amoblador3d.persistence.Database;
import pe.edu.unaj.amoblador3d.persistence.DesignRepository;
import pe.edu.unaj.amoblador3d.persistence.PriceRepository;
import pe.edu.unaj.amoblador3d.persistence.ProjectRepository;
import pe.edu.unaj.amoblador3d.scene.ModelLibrary;
import pe.edu.unaj.amoblador3d.ui.MainWindow;
import pe.edu.unaj.amoblador3d.util.AppLog;

/** Arranque: abre la base de datos, carga el catálogo y muestra la ventana principal. */
public final class App extends Application {
    private static final Logger LOG = AppLog.of(App.class);
    private Database db;

    /** Versión del .jar (la pone Maven en el manifiesto); "desarrollo" al correr desde el IDE. */
    public static String version() {
        String v = App.class.getPackage().getImplementationVersion();
        return v == null ? "desarrollo" : v;
    }

    @Override
    public void start(Stage stage) {
        AppLog.init(AppPaths.logsDir());
        AppLog.catchUncaught(App::showUnexpectedError);
        LOG.info(() -> "Inicio de Amoblador3D " + version() + " | Java " + Runtime.version()
                + " | " + System.getProperty("os.name") + " " + System.getProperty("os.arch")
                + " | datos: " + AppPaths.dataDir());
        try {
            db = new Database(AppPaths.database());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "No se pudo abrir la base de datos " + AppPaths.database(), e);
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
        DesignRepository designs = new DesignRepository(db);
        catalog.setCustom(MainWindow.loadDesigns(designs, warnings));

        PriceRepository priceRepository = new PriceRepository(db);
        PriceList prices;
        try {
            prices = priceRepository.load();
        } catch (SQLException e) {
            prices = new PriceList();
            warnings.add("No se pudieron leer tus precios; se usan los referenciales: " + e.getMessage());
        }

        warnings.forEach(w -> LOG.warning(w));

        var icon = App.class.getResourceAsStream("/icon.png");
        if (icon != null) stage.getIcons().add(new Image(icon));
        new MainWindow(stage, catalog, userModels, designs, new ProjectRepository(db), priceRepository, prices,
                        new ModelLibrary(), getHostServices())
                .show(warnings);
    }

    @Override
    public void stop() throws SQLException {
        LOG.info("Cierre de la aplicación");
        try {
            if (db != null) db.close();
        } finally {
            AppLog.close();
        }
    }

    /** Aviso al usuario cuando ocurre un error que el código no esperaba (ya quedó en el registro). */
    private static void showUnexpectedError(Throwable error) {
        Runnable show = () -> {
            String where = AppLog.file() == null ? "" : "\n\nDetalle guardado en:\n" + AppLog.file();
            Alert a = new Alert(Alert.AlertType.ERROR,
                    "Ocurrió un error inesperado: " + error + where
                            + "\n\nGuarda tu trabajo; si el problema sigue, envía ese archivo al equipo.",
                    ButtonType.OK);
            a.setHeaderText("Algo salió mal");
            a.show();
        };
        if (Platform.isFxApplicationThread()) show.run();
        else Platform.runLater(show);
    }
}
