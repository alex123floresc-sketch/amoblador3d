package pe.edu.unaj.amoblador3d.ui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Labeled;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.catalog.FSHFUserModels;
import pe.edu.unaj.amoblador3d.design.FSHFDesign;
import pe.edu.unaj.amoblador3d.estimate.FSHFPriceList;
import pe.edu.unaj.amoblador3d.persistence.FSHFAppPaths;
import pe.edu.unaj.amoblador3d.persistence.FSHFCatalogRepository;
import pe.edu.unaj.amoblador3d.persistence.FSHFDatabase;
import pe.edu.unaj.amoblador3d.persistence.FSHFDesignRepository;
import pe.edu.unaj.amoblador3d.persistence.FSHFPriceRepository;
import pe.edu.unaj.amoblador3d.persistence.FSHFProjectRepository;
import pe.edu.unaj.amoblador3d.scene.FSHFModelLibrary;

/**
 * Herramienta para la documentación (no es una prueba): abre la aplicación con una carpeta de datos
 * temporal, recorre sus pantallas con los mismos menús que usa una persona y guarda una captura PNG
 * de cada una.
 *
 * <pre>
 * java -Damoblador.home=C:\temp\capturas -cp (clases + dependencias) pe.edu.unaj.amoblador3d.ui.Capturas$Main documentacion\capturas
 * </pre>
 *
 * Nunca usa la carpeta de datos real: si no se indica {@code amoblador.home}, se detiene.
 */
public final class FSHFCapturas extends Application {
    private static Path out;
    private final Deque<Runnable> steps = new ArrayDeque<>();
    private Stage stage;
    private FSHFMainWindow window;

    /** Punto de entrada aparte (como {@code Launcher}): así arranca con JavaFX en el classpath. */
    public static final class Main {
        public static void main(String[] args) throws Exception {
            if (System.getProperty("amoblador.home") == null) {
                throw new IllegalStateException("Indica -Damoblador.home=<carpeta temporal> para no tocar tus datos reales.");
            }
            out = Path.of(args.length > 0 ? args[0] : "capturas");
            Files.createDirectories(out);
            Application.launch(FSHFCapturas.class, args);
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        FSHFDatabase db = new FSHFDatabase(FSHFAppPaths.database());
        FSHFUserModels userModels = new FSHFUserModels(new FSHFCatalogRepository(db), FSHFAppPaths.modelsDir());
        List<String> warnings = new ArrayList<>();
        FSHFCatalog catalog = new FSHFCatalog();
        catalog.setModels(FSHFMainWindow.loadModels(userModels, warnings));
        FSHFDesignRepository designs = new FSHFDesignRepository(db);
        catalog.setCustom(FSHFMainWindow.loadDesigns(designs, warnings));
        window = new FSHFMainWindow(stage, catalog, userModels, designs, new FSHFProjectRepository(db), new FSHFPriceRepository(db),
                new FSHFPriceList(), new FSHFModelLibrary(), getHostServices());
        stage.setWidth(1440);
        stage.setHeight(900);
        stage.show();

        // 1. Bienvenida → Casa de ejemplo (abre la vista "Casa completa").
        later(() -> menu("Pantalla de bienvenida"));
        later(() -> {
            Window w = dialog();
            shoot(w, "01_bienvenida");
            press(w.getScene().getRoot(), "Casa de ejemplo");
        });
        later(() -> shoot(stage, "02_casa_completa_3d"));
        later(() -> menu("Planta 2D"));
        later(() -> shoot(stage, "03_casa_completa_planta"));
        // 2. Un ambiente: la sala, con un mueble seleccionado.
        later(() -> menu("Casa completa"));
        later(() -> menu("Vista 3D"));
        later(this::selectFirstFurniture);
        later(() -> shoot(stage, "04_ambiente_3d_mueble_seleccionado"));
        later(() -> menu("Planta 2D"));
        later(() -> shoot(stage, "05_planta_con_cotas"));
        later(() -> menu("Recorrido"));
        later(() -> shoot(stage, "06_recorrido"));
        later(() -> menu("Vista 3D"));
        later(() -> tab(1));
        later(() -> shoot(stage, "07_pestana_ambiente"));
        later(() -> tab(2));
        later(() -> check("Modo noche", true));
        later(() -> shoot(stage, "08_luz_modo_noche"));
        later(() -> check("Modo noche", false));
        later(() -> tab(0));
        // 3. Ventanas.
        later(() -> menu("Metrado y presupuesto"));
        later(() -> closeAfterShot("09_presupuesto"));
        later(() -> menu("Diseñador de muebles"));
        later(() -> closeAfterShot("10_disenador_de_muebles"));
        later(() -> menu("Nuevo desde plantilla"));
        later(() -> closeAfterShot("11_plantillas"));
        later(() -> {
            System.out.println("Capturas guardadas en " + out.toAbsolutePath());
            Platform.exit();
        });
        next();
    }

    // ------------------------------------------------------------ pasos

    private void later(Runnable step) {
        steps.add(step);
    }

    /** Cada paso espera 1,5 s: lo suficiente para que la vista 3D termine de dibujarse. */
    private void next() {
        Runnable step = steps.poll();
        if (step == null) return;
        PauseTransition wait = new PauseTransition(Duration.millis(1500));
        wait.setOnFinished(e -> {
            // El paso siguiente se programa ANTES: si este abre una ventana modal, sigue funcionando.
            next();
            // Fuera de la animación: JavaFX no deja abrir ventanas modales desde ella.
            Platform.runLater(() -> {
                try {
                    step.run();
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                }
            });
        });
        wait.play();
    }

    private void menu(String startsWith) {
        MenuBar bar = (MenuBar) stage.getScene().getRoot().lookup(".menu-bar");
        for (Menu m : bar.getMenus()) {
            MenuItem found = find(m.getItems(), startsWith);
            if (found != null) {
                found.fire();
                return;
            }
        }
        throw new IllegalArgumentException("No hay un menú «" + startsWith + "»");
    }

    /** Las casillas del menú no cambian con fire(): se marcan directamente. */
    private void check(String startsWith, boolean on) {
        MenuBar bar = (MenuBar) stage.getScene().getRoot().lookup(".menu-bar");
        for (Menu m : bar.getMenus()) {
            if (find(m.getItems(), startsWith) instanceof javafx.scene.control.CheckMenuItem c) {
                c.setSelected(on);
                return;
            }
        }
        throw new IllegalArgumentException("No hay una casilla «" + startsWith + "»");
    }

    private static MenuItem find(List<MenuItem> items, String startsWith) {
        for (MenuItem i : items) {
            if (i.getText() != null && i.getText().startsWith(startsWith)) return i;
            if (i instanceof Menu sub) {
                MenuItem f = find(sub.getItems(), startsWith);
                if (f != null) return f;
            }
        }
        return null;
    }

    private void tab(int index) {
        ((TabPane) stage.getScene().getRoot().lookup(".sidebar-tabs")).getSelectionModel().select(index);
    }

    private void selectFirstFurniture() {
        try {
            Field f = FSHFMainWindow.class.getDeclaredField("design");
            f.setAccessible(true);
            FSHFDesign design = (FSHFDesign) f.get(window);
            Method select = FSHFMainWindow.class.getDeclaredMethod("select", Object.class);
            select.setAccessible(true);
            select.invoke(window, design.furniture().get(0));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** La ventana abierta encima de la principal (diálogo). */
    private Window dialog() {
        return Window.getWindows().stream().filter(w -> w != stage && w.isShowing()).reduce((a, b) -> b)
                .orElseThrow(() -> new IllegalStateException("No se abrió ninguna ventana"));
    }

    private void closeAfterShot(String name) {
        Window w = dialog();
        shoot(w, name);
        Parent root = w.getScene().getRoot();
        for (String label : List.of("Cancelar", "Cerrar", "Cancel", "Close")) {
            if (press(root, label)) return;
        }
        w.hide();
    }

    private static boolean press(Parent root, String text) {
        for (Node n : root.lookupAll(".button")) {
            if (n instanceof ButtonBase b && contains(b, text) && b.isVisible()) {
                b.fire();
                return true;
            }
        }
        return false;
    }

    private static boolean contains(Node n, String text) {
        if (n instanceof Labeled l && l.getText() != null && l.getText().contains(text)) return true;
        if (n instanceof Button b && b.getGraphic() instanceof Parent p) {
            return p.lookupAll(".label").stream().anyMatch(x -> x instanceof Labeled l && l.getText() != null && l.getText().contains(text));
        }
        return false;
    }

    private static void shoot(Window w, String name) {
        try {
            WritableImage image = w.getScene().snapshot(null);
            Files.write(out.resolve(name + ".png"), FSHFPng.encode(image));
            System.out.println("  " + name + ".png  " + (int) image.getWidth() + "x" + (int) image.getHeight());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo guardar " + name, e);
        }
    }
}
