package pe.edu.unaj.amoblador3d.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
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
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Labeled;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.catalog.FSHFUserModels;
import pe.edu.unaj.amoblador3d.design.FSHFDesign;
import pe.edu.unaj.amoblador3d.estimate.FSHFEstimate;
import pe.edu.unaj.amoblador3d.estimate.FSHFPriceList;
import pe.edu.unaj.amoblador3d.gltf.FSHFGltfLoader;
import pe.edu.unaj.amoblador3d.model.FSHFProject;
import pe.edu.unaj.amoblador3d.persistence.FSHFAppPaths;
import pe.edu.unaj.amoblador3d.persistence.FSHFCatalogRepository;
import pe.edu.unaj.amoblador3d.persistence.FSHFDatabase;
import pe.edu.unaj.amoblador3d.persistence.FSHFDesignRepository;
import pe.edu.unaj.amoblador3d.persistence.FSHFPriceRepository;
import pe.edu.unaj.amoblador3d.persistence.FSHFProjectRepository;
import pe.edu.unaj.amoblador3d.report.FSHFProjectReport;
import pe.edu.unaj.amoblador3d.scene.FSHFModelLibrary;
import pe.edu.unaj.amoblador3d.scene.FSHFSceneView;

public final class FSHFCapturas extends Application {
    private static Path out;
    private final Deque<Runnable> steps = new ArrayDeque<>();
    private Stage stage;
    private FSHFMainWindow window;
    private FSHFCatalog catalog;
    private final FSHFPriceList prices = new FSHFPriceList();

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
        catalog = new FSHFCatalog();
        catalog.setModels(FSHFMainWindow.loadModels(userModels, warnings));
        FSHFDesignRepository designs = new FSHFDesignRepository(db);
        catalog.setCustom(FSHFMainWindow.loadDesigns(designs, warnings));
        window = new FSHFMainWindow(stage, catalog, userModels, designs, new FSHFProjectRepository(db), new FSHFPriceRepository(db),
                prices, new FSHFModelLibrary(), getHostServices());
        stage.setWidth(1440);
        stage.setHeight(900);
        stage.show();

        later(() -> shoot(stage, "12_ambiente_nuevo"));
        later(() -> annotateMainEmpty("12a_ambiente_nuevo_partes"));

        later(() -> menu("Pantalla de bienvenida"));
        later(() -> {
            Window w = dialog();
            shoot(w, "01_bienvenida");
            annotateWelcome(w, "01a_bienvenida_partes");
            press(w.getScene().getRoot(), "Casa de ejemplo");
        });
        later(() -> shoot(stage, "02_casa_completa_3d"));
        later(() -> annotateHouse("02a_casa_completa_partes"));
        later(() -> menu("Planta 2D"));
        later(() -> shoot(stage, "03_casa_completa_planta"));

        later(() -> menu("Casa completa"));
        later(() -> menu("Vista 3D"));
        later(this::selectFirstFurniture);
        later(() -> {
            shoot(stage, "04_ambiente_3d_mueble_seleccionado");
            annotateMain("04a_ventana_principal_partes");
        });
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
        later(() -> tab(3));
        waitForToast();
        later(() -> shoot(stage, "13_pestana_lista"));
        later(() -> tab(0));
        later(() -> menu("Seleccionar todos los muebles"));
        later(() -> shoot(stage, "14_seleccion_multiple"));
        later(() -> menu("Deseleccionar"));

        later(() -> menu("Metrado y presupuesto"));
        later(() -> {
            Window w = dialog();
            annotateEstimate(w, "09a_presupuesto_partes");
            closeAfterShot("09_presupuesto");
        });
        later(() -> menu("Diseñador de muebles"));
        later(this::startDesignerFromTemplate);
        later(() -> {
            Window w = dialog();
            shoot(w, "10_disenador_de_muebles");
            annotateDesigner(w, "10a_disenador_partes");
            w.hide();
        });
        later(() -> menu("Nuevo desde plantilla"));
        later(() -> closeAfterShot("11_plantillas"));

        later(this::pdfPages);
        later(this::openImportDialog);
        later(() -> closeAfterShot("16_importar_modelo"));
        later(this::saveExample);
        later(() -> menu("Abrir…"));
        later(() -> closeAfterShot("17_abrir_proyecto"));
        later(() -> menu("Atajos de teclado"));
        later(() -> closeAfterShot("18_atajos_de_teclado"));
        later(() -> menu("Nuevo proyecto"));
        later(this::fillRoomUntilFull);
        later(() -> shoot(stage, "19_mensaje_sin_espacio"));
        later(() -> {
            System.out.println("Capturas guardadas en " + out.toAbsolutePath());
            Platform.exit();
        });
        next();
    }

    private void waitForToast() {
        for (int i = 0; i < 3; i++) later(() -> { });
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

    private Object field(String name) {
        try {
            Field f = FSHFMainWindow.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(window);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Object call(String name, Class<?>[] types, Object... args) {
        try {
            Method m = FSHFMainWindow.class.getDeclaredMethod(name, types);
            m.setAccessible(true);
            return m.invoke(window, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void selectFirstFurniture() {
        FSHFDesign design = (FSHFDesign) field("design");
        call("select", new Class<?>[] {Object.class}, design.furniture().get(0));
    }

    private void startDesignerFromTemplate() {
        Window w = dialog();
        for (Node n : w.getScene().getRoot().lookupAll(".combo-box")) {
            if (n instanceof ComboBox<?> c && "Empezar desde…".equals(c.getPromptText())) {
                @SuppressWarnings("unchecked")
                ComboBox<String> start = (ComboBox<String>) c;
                String choice = start.getItems().stream().filter(s -> s.startsWith("Copia de: Escritorio")).findFirst()
                        .orElse(start.getItems().stream().filter(s -> s.startsWith("Estante")).findFirst().orElse(start.getItems().get(0)));
                start.setValue(choice);
                return;
            }
        }
    }

    private void pdfPages() {
        try {
            FSHFDesign design = (FSHFDesign) field("design");
            FSHFSceneView view = (FSHFSceneView) field("view");
            FSHFProject p = design.snapshot();
            Path pdf = FSHFAppPaths.dataDir().resolve("capturas.pdf");
            new FSHFProjectReport("Sala", p, FSHFEstimate.of(p, catalog, prices),
                    FSHFPng.encode(view.planImage(2)), FSHFPng.encode(view.viewImage(2)), design::label).write(pdf);
            try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
                PDFRenderer renderer = new PDFRenderer(doc);
                int pages = Math.min(3, doc.getNumberOfPages());
                List<BufferedImage> images = new ArrayList<>();
                for (int i = 0; i < pages; i++) images.add(renderer.renderImageWithDPI(i, 72));
                int gap = 24;
                int w = images.stream().mapToInt(BufferedImage::getWidth).sum() + gap * (pages + 1);
                int h = images.stream().mapToInt(BufferedImage::getHeight).max().orElse(0) + gap * 2;
                BufferedImage sheet = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = sheet.createGraphics();
                g.setColor(new Color(0x5a5f66));
                g.fillRect(0, 0, w, h);
                int x = gap;
                for (BufferedImage img : images) {
                    g.drawImage(img, x, gap, null);
                    x += img.getWidth() + gap;
                }
                g.dispose();
                ImageIO.write(sheet, "png", out.resolve("15_informe_pdf.png").toFile());
                System.out.println("  15_informe_pdf.png  " + w + "x" + h + " (" + doc.getNumberOfPages() + " páginas)");
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        }
    }

    private void openImportDialog() {
        try {
            var data = FSHFGltfLoader.load(FSHFMainWindow.class.getResource("/assets/models/sofa.glb"));
            FSHFImportModelDialog.show(stage, "sofa", data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void saveExample() {
        call("saveTo", new Class<?>[] {Long.class, String.class}, null, "Casa de ejemplo");
    }

    private void fillRoomUntilFull() {
        FSHFDesign design = (FSHFDesign) field("design");
        for (int i = 0; i < 40; i++) {
            int before = design.furniture().size();
            call("addFurniture", new Class<?>[] {String.class}, "closet");
            if (design.furniture().size() == before) return;
        }
    }

    private Window dialog() {
        return Window.getWindows().stream().filter(w -> w != stage && w.isShowing()).reduce((a, b) -> b)
                .orElseThrow(() -> new IllegalStateException("No se abrió ninguna ventana"));
    }

    private void closeAfterShot(String name) {
        Window w = dialog();
        shoot(w, name);
        Parent root = w.getScene().getRoot();
        for (String label : List.of("Cancelar", "Cerrar", "Aceptar", "Cancel", "Close", "OK")) {
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

    private record Mark(Node node, double fx, double fy, double dx, double dy) {
    }

    private static Mark at(Node n, double fx, double fy, double dx, double dy) {
        return new Mark(n, fx, fy, dx, dy);
    }

    private static Mark topRight(Node n) {
        return new Mark(n, 1, 0, 0, 0);
    }

    private static Mark insideTopRight(Node n) {
        return new Mark(n, 1, 0, -22, 22);
    }

    private static Mark afterRight(Node n) {
        return new Mark(n, 1, 0.5, 18, 0);
    }

    private static Mark beforeLeft(Node n) {
        return new Mark(n, 0, 0.5, -16, 0);
    }

    private static Node visible(Window w, String selector) {
        return w.getScene().getRoot().lookupAll(selector).stream().filter(Node::isVisible).findFirst()
                .orElseThrow(() -> new IllegalStateException("No se encontró " + selector));
    }

    private static Node largest(Window w, String selector) {
        return w.getScene().getRoot().lookupAll(selector).stream().filter(Node::isVisible)
                .max(java.util.Comparator.comparingDouble(n -> n.getBoundsInLocal().getWidth() * n.getBoundsInLocal().getHeight()))
                .orElseThrow(() -> new IllegalStateException("No se encontró " + selector));
    }

    private static Node button(Window w, String text) {
        return w.getScene().getRoot().lookupAll(".button").stream().filter(n -> n.isVisible() && contains(n, text)).findFirst()
                .orElseThrow(() -> new IllegalStateException("No se encontró el botón " + text));
    }

    private static Node labeled(Window w, String text) {
        return w.getScene().getRoot().lookupAll("*").stream()
                .filter(n -> n.isVisible() && !(n instanceof ButtonBase) && n instanceof Labeled l && l.getText() != null && l.getText().startsWith(text)).findFirst()
                .orElseThrow(() -> new IllegalStateException("No se encontró «" + text + "»"));
    }

    private static void annotate(Window w, String name, List<Mark> marks) {
        WritableImage base = w.getScene().snapshot(null);
        Pane layer = new Pane(new ImageView(base));
        int number = 1;
        for (Mark m : marks) {
            Bounds b = m.node().localToScene(m.node().getBoundsInLocal());
            double r = base.getWidth() > 1100 ? 20 : 14;
            double cx = b.getMinX() + m.fx() * b.getWidth() + m.dx();
            double cy = b.getMinY() + m.fy() * b.getHeight() + m.dy();
            cx = Math.max(r + 1, Math.min(base.getWidth() - r - 1, cx));
            cy = Math.max(r + 1, Math.min(base.getHeight() - r - 1, cy));
            Circle circle = new Circle(r, Paint.valueOf("#d32f2f"));
            circle.setStroke(Paint.valueOf("#ffffff"));
            circle.setStrokeWidth(r / 5.5);
            Text label = new Text(String.valueOf(number++));
            label.setFont(Font.font("Segoe UI", FontWeight.BOLD, r * 1.08));
            label.setFill(Paint.valueOf("#ffffff"));
            StackPane badge = new StackPane(circle, label);
            badge.setPrefSize(2 * r + 3, 2 * r + 3);
            badge.relocate(cx - r - 1.5, cy - r - 1.5);
            layer.getChildren().add(badge);
        }
        layer.setPrefSize(base.getWidth(), base.getHeight());
        layer.resize(base.getWidth(), base.getHeight());
        layer.applyCss();
        layer.layout();
        try {
            WritableImage image = layer.snapshot(null, null);
            Files.write(out.resolve(name + ".png"), FSHFPng.encode(image));
            System.out.println("  " + name + ".png  (" + marks.size() + " marcas)");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo guardar " + name, e);
        }
    }

    private void annotateMain(String name) {
        Node spaceBar = visible(stage, ".space-bar");
        annotate(stage, name, List.of(
                at(visible(stage, ".menu-bar"), 0, 0.5, 420, 0),
                at(visible(stage, ".tool-bar"), 0, 0.5, 1300, 0),
                at(visible(stage, ".sidebar-tabs"), 0, 0, 284, 18),
                at(visible(stage, ".category-chips"), 1, 1, -18, -14),
                insideTopRight(visible(stage, ".catalog-card")),
                at(spaceBar, 1, 0.5, -24, 0),
                at(spaceBar, 0, 1, 26, 28),
                beforeLeft(visible(stage, ".floating-bar")),
                beforeLeft(visible(stage, ".view-controls")),
                at(visible(stage, ".properties-header"), 1, 0, -18, -36),
                at(visible(stage, ".status-bar"), 0.62, 0.5, 0, 0)));
    }

    private void annotateMainEmpty(String name) {
        annotate(stage, name, List.of(
                topRight(visible(stage, ".space-tab")),
                topRight(visible(stage, ".space-add")),
                at(visible(stage, ".space-bar"), 0, 1, 26, 28),
                insideTopRight(visible(stage, ".step")),
                at(button(stage, "Empezar desde una plantilla"), 1, 0.5, -22, 0),
                insideTopRight(visible(stage, ".tips"))));
    }

    private void annotateHouse(String name) {
        annotate(stage, name, List.of(
                topRight(visible(stage, ".house-toggle")),
                topRight(visible(stage, ".space-tab")),
                topRight(visible(stage, ".space-add")),
                at(labeled(stage, "Sala"), 0, 0.5, -16, 0),
                at(visible(stage, ".home-total"), 0, 0.5, -14, 0),
                topRight(button(stage, "Ambiente vacío"))));
    }

    private static void annotateWelcome(Window w, String name) {
        List<Mark> marks = new ArrayList<>();
        marks.add(afterRight(visible(w, ".welcome-title")));
        w.getScene().getRoot().lookupAll(".welcome-card").stream().filter(Node::isVisible).forEach(n -> marks.add(insideTopRight(n)));
        marks.add(afterRight(visible(w, ".check-box")));
        marks.add(at(button(w, "Seguir con"), 0, 0.5, -16, 0));
        annotate(w, name, marks);
    }

    private static void annotateEstimate(Window w, String name) {
        annotate(w, name, List.of(
                afterRight(visible(w, ".hint")),
                at(visible(w, ".table-view"), 1, 1, -26, -26),
                at(labeled(w, "P. unitario"), 1, 0.5, 6, 0),
                topRight(button(w, "Precios referenciales")),
                topRight(button(w, "Exportar CSV")),
                beforeLeft(visible(w, ".estimate-total")),
                topRight(button(w, "Informe PDF"))));
    }

    private static void annotateDesigner(Window w, String name) {
        annotate(w, name, List.of(
                topRight(visible(w, ".text-field")),
                topRight(visible(w, ".combo-box")),
                topRight(button(w, "Arriba")),
                topRight(button(w, "+ Esfera")),
                insideTopRight(largest(w, ".list-view")),
                topRight(button(w, "Espejo")),
                afterRight(visible(w, ".designer-size")),
                afterRight(visible(w, ".designer-help")),
                at(visible(w, ".tab-pane"), 1, 0, -22, 20),
                at(button(w, "Guardar en el catálogo"), 0, 0.5, -14, 0)));
    }
}
