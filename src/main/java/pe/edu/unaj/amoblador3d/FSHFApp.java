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
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.catalog.FSHFUserModels;
import pe.edu.unaj.amoblador3d.estimate.FSHFPriceList;
import pe.edu.unaj.amoblador3d.persistence.FSHFAppPaths;
import pe.edu.unaj.amoblador3d.persistence.FSHFCatalogRepository;
import pe.edu.unaj.amoblador3d.persistence.FSHFDatabase;
import pe.edu.unaj.amoblador3d.persistence.FSHFDesignRepository;
import pe.edu.unaj.amoblador3d.persistence.FSHFPriceRepository;
import pe.edu.unaj.amoblador3d.persistence.FSHFProjectRepository;
import pe.edu.unaj.amoblador3d.scene.FSHFModelLibrary;
import pe.edu.unaj.amoblador3d.ui.FSHFMainWindow;
import pe.edu.unaj.amoblador3d.util.FSHFAppLog;

/** Arranque: abre la base de datos, carga el catálogo y muestra la ventana principal. */
public final class FSHFApp extends Application {
    private static final Logger LOG = FSHFAppLog.of(FSHFApp.class);
    private FSHFDatabase db;

    /** Versión del .jar (la pone Maven en el manifiesto); "desarrollo" al correr desde el IDE. */
    public static String version() {
        String v = FSHFApp.class.getPackage().getImplementationVersion();
        return v == null ? "desarrollo" : v;
    }

    @Override
    public void start(Stage stage) {
        FSHFAppLog.init(FSHFAppPaths.logsDir());
        FSHFAppLog.catchUncaught(FSHFApp::showUnexpectedError);
        LOG.info(() -> "Inicio de Amoblador3D " + version() + " | Java " + Runtime.version()
                + " | " + System.getProperty("os.name") + " " + System.getProperty("os.arch")
                + " | datos: " + FSHFAppPaths.dataDir());
        try {
            db = new FSHFDatabase(FSHFAppPaths.database());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "No se pudo abrir la base de datos " + FSHFAppPaths.database(), e);
            Alert a = new Alert(Alert.AlertType.ERROR, e.getMessage() + "\n\nArchivo: " + FSHFAppPaths.database(), ButtonType.OK);
            a.setHeaderText("No se pudo abrir la base de datos");
            a.showAndWait();
            Platform.exit();
            return;
        }
        FSHFUserModels userModels = new FSHFUserModels(new FSHFCatalogRepository(db), FSHFAppPaths.modelsDir());
        List<String> warnings = new ArrayList<>();
        FSHFCatalog catalog = new FSHFCatalog();
        catalog.setModels(FSHFMainWindow.loadModels(userModels, warnings));
        FSHFDesignRepository designs = new FSHFDesignRepository(db);
        catalog.setCustom(FSHFMainWindow.loadDesigns(designs, warnings));

        FSHFPriceRepository priceRepository = new FSHFPriceRepository(db);
        FSHFPriceList prices;
        try {
            prices = priceRepository.load();
        } catch (SQLException e) {
            prices = new FSHFPriceList();
            warnings.add("No se pudieron leer tus precios; se usan los referenciales: " + e.getMessage());
        }

        warnings.forEach(w -> LOG.warning(w));

        var icon = FSHFApp.class.getResourceAsStream("/icon.png");
        if (icon != null) stage.getIcons().add(new Image(icon));
        new FSHFMainWindow(stage, catalog, userModels, designs, new FSHFProjectRepository(db), priceRepository, prices,
                        new FSHFModelLibrary(), getHostServices())
                .show(warnings);
    }

    @Override
    public void stop() throws SQLException {
        LOG.info("Cierre de la aplicación");
        try {
            if (db != null) db.close();
        } finally {
            FSHFAppLog.close();
        }
    }

    /** Aviso al usuario cuando ocurre un error que el código no esperaba (ya quedó en el registro). */
    private static void showUnexpectedError(Throwable error) {
        Runnable show = () -> {
            String where = FSHFAppLog.file() == null ? "" : "\n\nDetalle guardado en:\n" + FSHFAppLog.file();
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
