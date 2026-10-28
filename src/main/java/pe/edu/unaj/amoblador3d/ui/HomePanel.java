package pe.edu.unaj.amoblador3d.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pe.edu.unaj.amoblador3d.design.Editable;
import pe.edu.unaj.amoblador3d.design.Field;
import pe.edu.unaj.amoblador3d.model.Home;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.util.Units;

/**
 * Panel derecho en la vista "Casa completa": el resumen de la casa (área y presupuesto de cada
 * ambiente) o las propiedades del ambiente elegido (ubicación en la casa y medidas).
 */
final class HomePanel {
    /** Lo que el panel necesita de la ventana principal. */
    interface Host {
        Home home();

        /** Abre el ambiente para decorarlo. */
        void open(Home.Space s);

        void rename(Home.Space s);

        String duplicate(Home.Space s);

        void delete(Home.Space s);

        /** Cambia las medidas de la habitación del ambiente. Devuelve un error o null. */
        String resize(Home.Space s, RoomDims dims);

        /** Mueve el ambiente en el plano de la casa. Devuelve un error o null. */
        String move(Home.Space s, double x, double z);

        void addEmpty();

        void frame();

        /** Total del presupuesto del ambiente (S/). */
        double total(Home.Space s);
    }

    private HomePanel() {}

    /** Resumen de la casa: una fila por ambiente y el total. */
    static Editable overview(Host host) {
        return new Editable() {
            @Override
            public Object entity() {
                return host.home();
            }

            @Override
            public String title() {
                return "Casa completa";
            }

            @Override
            public List<Field> fields() {
                return List.of();
            }

            @Override
            public Object get(String key) {
                return null;
            }

            @Override
            public String set(String key, Object value) {
                return "Campo desconocido: " + key;
            }

            @Override
            public boolean showDelete() {
                return false;
            }

            @Override
            public List<Action> actions() {
                return List.of(new Action("+ Ambiente vacío", "Agrega un ambiente pegado a la casa", () -> {
                    host.addEmpty();
                    return null;
                }), new Action("⤢ Encuadrar", "Ver la casa completa", () -> {
                    host.frame();
                    return null;
                }));
            }
        };
    }

    /** Tabla de ambientes con su área y su presupuesto, y los totales. */
    static Node summary(Host host) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        ColumnConstraints name = new ColumnConstraints();
        name.setHgrow(Priority.ALWAYS);
        ColumnConstraints right = new ColumnConstraints();
        right.setHalignment(HPos.RIGHT);
        grid.getColumnConstraints().addAll(name, right, right);
        String[] head = {"Ambiente", "Área", "Costo"};
        for (int i = 0; i < head.length; i++) {
            Label l = new Label(head[i]);
            l.getStyleClass().add("subheading");
            grid.add(l, i, 0);
        }
        double area = 0, total = 0;
        int row = 1;
        for (Home.Space s : host.home().spaces) {
            RoomDims r = s.project.room;
            double a = r.width() * r.length(), t = host.total(s);
            area += a;
            total += t;
            Label n = new Label(s.name);
            n.setMinWidth(40);
            grid.add(n, 0, row);
            grid.add(fixed(Units.number(Units.round(a)) + " m²"), 1, row);
            grid.add(fixed(Units.money(t)), 2, row);
            row++;
        }
        Label totalName = new Label("TOTAL");
        totalName.setMinWidth(40);
        Label totalArea = fixed(Units.number(Units.round(area)) + " m²");
        Label totalMoney = fixed(Units.money(total));
        for (Label l : List.of(totalName, totalArea, totalMoney)) l.getStyleClass().add("home-total");
        grid.add(totalName, 0, row);
        grid.add(totalArea, 1, row);
        grid.add(totalMoney, 2, row);
        Label hint = new Label("Haz clic en un ambiente para moverlo o cambiar sus medidas; doble clic para decorarlo. "
                + "Al arrastrarlo, sus muros se pegan a los de los demás (Alt: sin imán). El presupuesto de cada ambiente "
                + "incluye acabados y muebles (Proyecto → Metrado y presupuesto).");
        hint.setWrapText(true);
        hint.getStyleClass().add("hint");
        VBox box = new VBox(10, grid, hint);
        box.getStyleClass().add("properties-header");
        return box;
    }

    /** Texto que nunca se corta con "..." (los números se ven completos; se acorta el nombre). */
    private static Label fixed(String text) {
        Label l = new Label(text);
        l.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        return l;
    }

    /** Propiedades de un ambiente en la casa. */
    static Editable space(Host host, Home.Space s) {
        return new Editable() {
            @Override
            public Object entity() {
                return s;
            }

            @Override
            public String title() {
                return s.name;
            }

            @Override
            public List<Field> fields() {
                List<Field> f = new ArrayList<>();
                f.add(Field.section("Ubicación en la casa"));
                f.add(Field.number("x", "Esquina X", -200, 200, 0.01, "m"));
                f.add(Field.number("z", "Esquina Z", -200, 200, 0.01, "m"));
                f.add(Field.section("Medidas de la habitación"));
                f.add(Field.slider("width", "Ancho (X)", RoomDims.MIN_SIDE, 12, 0.05, "m"));
                f.add(Field.slider("length", "Largo (Z)", RoomDims.MIN_SIDE, 12, 0.05, "m"));
                f.add(Field.number("height", "Alto", RoomDims.MIN_HEIGHT, RoomDims.MAX_HEIGHT, 0.01, "m"));
                f.add(Field.number("wall", "Grosor de muro", RoomDims.MIN_WALL, RoomDims.MAX_WALL, 0.01, "m"));
                return f;
            }

            @Override
            public Object get(String key) {
                RoomDims r = s.project.room;
                return switch (key) {
                    case "x" -> s.x;
                    case "z" -> s.z;
                    case "width" -> r.width();
                    case "length" -> r.length();
                    case "height" -> r.height();
                    case "wall" -> r.wallThickness();
                    default -> null;
                };
            }

            @Override
            public String set(String key, Object value) {
                if (!(value instanceof Double v) || !Double.isFinite(v)) return "Escribe un número.";
                RoomDims r = s.project.room;
                return switch (key) {
                    case "x" -> host.move(s, v, s.z);
                    case "z" -> host.move(s, s.x, v);
                    case "width" -> host.resize(s, new RoomDims(v, r.length(), r.height(), r.wallThickness()));
                    case "length" -> host.resize(s, new RoomDims(r.width(), v, r.height(), r.wallThickness()));
                    case "height" -> host.resize(s, new RoomDims(r.width(), r.length(), v, r.wallThickness()));
                    case "wall" -> host.resize(s, new RoomDims(r.width(), r.length(), r.height(), v));
                    default -> "Campo desconocido: " + key;
                };
            }

            @Override
            public boolean showDelete() {
                return false;
            }

            @Override
            public List<Action> actions() {
                boolean several = host.home().spaces.size() > 1;
                return List.of(
                        new Action("✎ Decorar este ambiente", "Abrirlo para poner muebles, luces y acabados (doble clic)", () -> {
                            host.open(s);
                            return null;
                        }),
                        new Action("Renombrar…", "Cambiar el nombre del ambiente", () -> {
                            host.rename(s);
                            return null;
                        }),
                        new Action("Duplicar", "Una copia con los mismos muebles, pegada a la casa", () -> host.duplicate(s)),
                        new Action("Eliminar ambiente", several ? "Quitarlo de la casa" : "La casa debe tener al menos un ambiente",
                                () -> {
                                    host.delete(s);
                                    return null;
                                }, several));
            }
        };
    }
}
