package pe.edu.unaj.amoblador3d.ui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Labeled;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalogEntry;
import pe.edu.unaj.amoblador3d.catalog.FSHFUserModels;
import pe.edu.unaj.amoblador3d.design.FSHFDesign;
import pe.edu.unaj.amoblador3d.estimate.FSHFEstimate;
import pe.edu.unaj.amoblador3d.estimate.FSHFPriceList;
import pe.edu.unaj.amoblador3d.gltf.FSHFGltfLoader;
import pe.edu.unaj.amoblador3d.gltf.FSHFModelData;
import pe.edu.unaj.amoblador3d.model.FSHFHome;
import pe.edu.unaj.amoblador3d.model.FSHFProject;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;
import pe.edu.unaj.amoblador3d.persistence.FSHFAppPaths;
import pe.edu.unaj.amoblador3d.persistence.FSHFCatalogRepository;
import pe.edu.unaj.amoblador3d.persistence.FSHFDatabase;
import pe.edu.unaj.amoblador3d.persistence.FSHFDesignRepository;
import pe.edu.unaj.amoblador3d.persistence.FSHFHomeJson;
import pe.edu.unaj.amoblador3d.persistence.FSHFPriceRepository;
import pe.edu.unaj.amoblador3d.persistence.FSHFProjectRepository;
import pe.edu.unaj.amoblador3d.report.FSHFProjectReport;
import pe.edu.unaj.amoblador3d.scene.FSHFModelLibrary;
import pe.edu.unaj.amoblador3d.scene.FSHFSceneView;
import pe.edu.unaj.amoblador3d.util.FSHFAppLog;

public final class FSHFComprobacionSistema extends Application {
    private final Deque<Runnable> steps = new ArrayDeque<>();
    private final Map<String, String> results = new LinkedHashMap<>();
    private Stage stage;
    private FSHFMainWindow window;
    private FSHFCatalog catalog;
    private FSHFUserModels userModels;
    private FSHFProjectRepository projects;
    private FSHFDatabase db;

    public static final class Main {
        public static void main(String[] args) {
            if (System.getProperty("amoblador.home") == null) {
                throw new IllegalStateException("Indica -Damoblador.home=<carpeta temporal> para no tocar tus datos reales.");
            }
            Application.launch(FSHFComprobacionSistema.class, args);
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        FSHFAppLog.init(FSHFAppPaths.logsDir());
        db = new FSHFDatabase(FSHFAppPaths.database());
        userModels = new FSHFUserModels(new FSHFCatalogRepository(db), FSHFAppPaths.modelsDir());
        List<String> warnings = new ArrayList<>();
        catalog = new FSHFCatalog();
        catalog.setModels(FSHFMainWindow.loadModels(userModels, warnings));
        FSHFDesignRepository designs = new FSHFDesignRepository(db);
        catalog.setCustom(FSHFMainWindow.loadDesigns(designs, warnings));
        projects = new FSHFProjectRepository(db);
        window = new FSHFMainWindow(stage, catalog, userModels, designs, projects, new FSHFPriceRepository(db),
                new FSHFPriceList(), new FSHFModelLibrary(), getHostServices());
        stage.setWidth(1366);
        stage.setHeight(768);
        stage.show();

        later(() -> check("PM-01", "Abrir la pantalla de bienvenida", () -> {
            menu("Pantalla de bienvenida");
            return true;
        }));
        later(() -> check("PM-01", "Abrir la pantalla de bienvenida", () -> {
            Window w = dialog();
            boolean ok = w != null && press(w.getScene().getRoot(), "Casa de ejemplo");
            return ok;
        }));
        later(() -> check("PM-02", "Casa de ejemplo con sala, dormitorio, cocina y baño", () -> {
            FSHFHome home = (FSHFHome) get("home");
            String names = home.spaces.stream().map(s -> s.name.toLowerCase(Locale.ROOT)).reduce("", (a, b) -> a + "|" + b);
            return home.spaces.size() == 4 && names.contains("sala") && names.contains("dormitorio")
                    && names.contains("cocina") && names.contains("baño");
        }));
        later(() -> menu("Casa completa"));
        later(() -> menu("Recorrido"));
        later(() -> check("PM-05", "En el recorrido no se atraviesan las paredes", () -> {
            FSHFDesign design = (FSHFDesign) get("design");
            FSHFSceneView view = (FSHFSceneView) get("view");
            Method walkable = FSHFSceneView.class.getDeclaredMethod("walkable", Point3D.class, Point3D.class);
            walkable.setAccessible(true);
            FSHFRoomDims r = design.room();
            Point3D eye = new Point3D(r.width() / 2, 1.6, r.length() / 2);

            for (int i = 0; i < 200; i++) eye = (Point3D) walkable.invoke(view, eye, eye.add(-0.1, 0, 0));
            return eye.getX() > 0.1;
        }));
        later(() -> menu("Vista 3D"));
        later(() -> check("PM-06", "Modo noche", () -> {
            checkItem("Modo noche", true);
            boolean on = ((FSHFSceneView) get("view")).isNight();
            checkItem("Modo noche", false);
            return on && !((FSHFSceneView) get("view")).isNight();
        }));
        later(() -> check("PM-08", "Informe PDF con planos y presupuesto", () -> {
            FSHFDesign design = (FSHFDesign) get("design");
            FSHFSceneView view = (FSHFSceneView) get("view");
            FSHFProject p = design.snapshot();
            Path pdf = FSHFAppPaths.dataDir().resolve("comprobacion.pdf");
            new FSHFProjectReport("Comprobación", p, FSHFEstimate.of(p, catalog, (FSHFPriceList) get("prices")),
                    FSHFPng.encode(view.planImage(2)), FSHFPng.encode(view.viewImage(2)), design::label).write(pdf);
            byte[] head = Files.readAllBytes(pdf);
            return head.length > 10_000 && new String(head, 0, 5).equals("%PDF-");
        }));
        later(() -> check("PM-09", "Guardar, cerrar y volver a abrir el proyecto", () -> {
            Method saveTo = FSHFMainWindow.class.getDeclaredMethod("saveTo", Long.class, String.class);
            saveTo.setAccessible(true);
            Method currentHome = FSHFMainWindow.class.getDeclaredMethod("currentHome");
            currentHome.setAccessible(true);
            String before = FSHFHomeJson.write((FSHFHome) currentHome.invoke(window));
            if (!(boolean) saveTo.invoke(window, null, "Comprobación del sistema")) return false;
            long id = projects.findByName("Comprobación del sistema");
            menu("Nuevo proyecto");
            Method open = FSHFMainWindow.class.getDeclaredMethod("openById", long.class);
            open.setAccessible(true);
            open.invoke(window, id);
            String after = FSHFHomeJson.write((FSHFHome) currentHome.invoke(window));
            byte[] thumb = projects.list().stream().filter(s -> s.id() == id).findFirst().orElseThrow().thumbnail();
            return before.equals(after) && thumb != null && thumb.length > 0;
        }));
        later(() -> check("PM-11", "Importar un modelo .glb al catálogo", () -> {
            Path tmp = Files.createTempFile("sofa", ".glb");
            try (var in = FSHFMainWindow.class.getResourceAsStream("/assets/models/sofa.glb")) {
                Files.write(tmp, in.readAllBytes());
            }
            Path named = tmp.resolveSibling("sofa-importado.glb");
            Files.move(tmp, named, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            FSHFUserModels.Imported imp = userModels.copyIntoLibrary(named);
            FSHFModelData.Box box = FSHFGltfLoader.load(imp.path().toUri().toURL()).bounds(0);
            userModels.save(imp, "Sofá importado", new FSHFCatalogEntry.Size(box.sizeX(), box.sizeZ(), box.sizeY()), 0);
            Method reload = FSHFMainWindow.class.getDeclaredMethod("reloadCatalog");
            reload.setAccessible(true);
            reload.invoke(window);
            FSHFCatalogEntry e = catalog.get(imp.id());
            return e != null && Math.abs(e.defaults().width() - box.sizeX()) < 1e-6;
        }));
        later(() -> check("PM-14", "Registro de errores en la carpeta logs", () ->
                Files.exists(FSHFAppPaths.logsDir().resolve("amoblador3d-0.log"))));
        later(() -> {
            System.out.println();
            System.out.println("RESULTADOS");
            results.forEach((id, r) -> System.out.println("  " + id + "  " + r));
            Platform.exit();
        });
        next();
    }

    private void check(String id, String name, Check c) {
        try {
            boolean ok = c.run();
            if (!ok || !results.containsKey(id) || results.get(id).startsWith("PASA")) {
                results.put(id, (ok ? "PASA   " : "FALLA  ") + name);
            }
        } catch (Throwable t) {
            Throwable cause = t instanceof java.lang.reflect.InvocationTargetException ite ? ite.getCause() : t;
            results.put(id, "FALLA  " + name + " (" + cause + ")");
            cause.printStackTrace();
        }
    }

    @FunctionalInterface
    private interface Check {
        boolean run() throws Exception;
    }

    private void later(Runnable step) {
        steps.add(step);
    }

    private void next() {
        Runnable step = steps.poll();
        if (step == null) return;
        PauseTransition wait = new PauseTransition(Duration.millis(1500));
        wait.setOnFinished(e -> {
            next();
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

    private Object get(String field) throws ReflectiveOperationException {
        Field f = FSHFMainWindow.class.getDeclaredField(field);
        f.setAccessible(true);
        return f.get(window);
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

    private void checkItem(String startsWith, boolean on) {
        MenuBar bar = (MenuBar) stage.getScene().getRoot().lookup(".menu-bar");
        for (Menu m : bar.getMenus()) {
            if (find(m.getItems(), startsWith) instanceof CheckMenuItem c) {
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

    private Window dialog() {
        return Window.getWindows().stream().filter(w -> w != stage && w.isShowing()).reduce((a, b) -> b).orElse(null);
    }

    private static boolean press(Parent root, String text) {
        for (Node n : root.lookupAll(".button")) {
            if (n instanceof ButtonBase b && b.isVisible() && mentions(b, text)) {
                b.fire();
                return true;
            }
        }
        return false;
    }

    private static boolean mentions(Node n, String text) {
        if (n instanceof Labeled l && l.getText() != null && l.getText().contains(text)) return true;
        return n instanceof Parent p && p.lookupAll(".label").stream()
                .anyMatch(x -> x instanceof Labeled l && l.getText() != null && l.getText().contains(text));
    }
}
