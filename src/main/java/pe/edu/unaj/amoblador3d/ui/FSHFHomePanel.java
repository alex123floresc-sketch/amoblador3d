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
import pe.edu.unaj.amoblador3d.design.FSHFEditable;
import pe.edu.unaj.amoblador3d.design.FSHFField;
import pe.edu.unaj.amoblador3d.model.FSHFHome;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;
import pe.edu.unaj.amoblador3d.util.FSHFUnits;

final class FSHFHomePanel {
    interface Host {
        FSHFHome home();

        void open(FSHFHome.Space s);

        void rename(FSHFHome.Space s);

        String duplicate(FSHFHome.Space s);

        void delete(FSHFHome.Space s);

        String resize(FSHFHome.Space s, FSHFRoomDims dims);

        String move(FSHFHome.Space s, double x, double z);

        String rotate(FSHFHome.Space s, int degrees);

        String moveToLevel(FSHFHome.Space s, int level);

        int viewLevel();

        void showLevel(int level);

        void addEmpty();

        void frame();

        double total(FSHFHome.Space s);
    }

    private FSHFHomePanel() {}

    static FSHFEditable overview(Host host) {
        return new FSHFEditable() {
            @Override
            public Object entity() {
                return host.home();
            }

            @Override
            public String title() {
                return "Casa completa";
            }

            @Override
            public List<FSHFField> fields() {
                FSHFHome h = host.home();
                List<FSHFField.Option> levels = new ArrayList<>();
                for (int l = 0; l < h.levels(); l++) levels.add(new FSHFField.Option(String.valueOf(l), levelName(l)));
                if (h.levels() < FSHFHome.MAX_LEVELS) {
                    levels.add(new FSHFField.Option(String.valueOf(h.levels()), levelName(h.levels()) + " (nuevo)"));
                }
                return List.of(FSHFField.select("level", "Piso que se ve", levels));
            }

            @Override
            public Object get(String key) {
                return key.equals("level") ? String.valueOf(host.viewLevel()) : null;
            }

            @Override
            public String set(String key, Object value) {
                if (!key.equals("level")) return "Campo desconocido: " + key;
                host.showLevel(Integer.parseInt((String) value));
                return null;
            }

            @Override
            public boolean showDelete() {
                return false;
            }

            @Override
            public List<Action> actions() {
                return List.of(new Action("+ Ambiente vacío", "Agrega un ambiente en el piso que se ve, pegado a la casa", () -> {
                    host.addEmpty();
                    return null;
                }), new Action("⤢ Encuadrar", "Ver la casa completa", () -> {
                    host.frame();
                    return null;
                }));
            }
        };
    }

    static String levelName(int level) {
        String[] names = {"Primer piso", "Segundo piso", "Tercer piso", "Cuarto piso"};
        return level < names.length ? names[level] : "Piso " + (level + 1);
    }

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
        FSHFHome home = host.home();
        boolean levels = home.levels() > 1;
        for (FSHFHome.Space s : home.spaces) {
            FSHFRoomDims r = s.project.room;
            double a = r.width() * r.length(), t = host.total(s);
            area += a;
            total += t;
            Label n = new Label(levels ? "P" + (s.level + 1) + " · " + s.name : s.name);
            n.setMinWidth(40);
            grid.add(n, 0, row);
            grid.add(fixed(FSHFUnits.number(FSHFUnits.round(a)) + " m²"), 1, row);
            grid.add(fixed(FSHFUnits.money(t)), 2, row);
            row++;
        }
        Label totalName = new Label("TOTAL");
        totalName.setMinWidth(40);
        Label totalArea = fixed(FSHFUnits.number(FSHFUnits.round(area)) + " m²");
        Label totalMoney = fixed(FSHFUnits.money(total));
        for (Label l : List.of(totalName, totalArea, totalMoney)) l.getStyleClass().add("home-total");
        grid.add(totalName, 0, row);
        grid.add(totalArea, 1, row);
        grid.add(totalMoney, 2, row);
        long outside = home.exteriorDoors().size();
        Label doors = new Label(home.levels() + (home.levels() == 1 ? " piso" : " pisos") + " · " + outside
                + (outside == 1 ? " puerta da" : " puertas dan") + " al exterior"
                + (outside == 0 ? "\n⚠ Agrega una puerta principal en una pared que dé a la calle." : ""));
        doors.setWrapText(true);
        Label hint = new Label("Haz clic en un ambiente para moverlo, girarlo o cambiar sus medidas; doble clic para "
                + "decorarlo. Al arrastrarlo, sus muros se pegan a los de los demás (Alt: sin imán). Una puerta pegada al "
                + "muro de otro ambiente abre paso entre los dos. El presupuesto de cada ambiente incluye acabados y "
                + "muebles (Proyecto → Metrado y presupuesto).");
        hint.setWrapText(true);
        hint.getStyleClass().add("hint");
        VBox box = new VBox(10, grid, doors, hint);
        box.getStyleClass().add("properties-header");
        return box;
    }

    private static Label fixed(String text) {
        Label l = new Label(text);
        l.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        return l;
    }

    static FSHFEditable space(Host host, FSHFHome.Space s) {
        return new FSHFEditable() {
            @Override
            public Object entity() {
                return s;
            }

            @Override
            public String title() {
                return s.name;
            }

            @Override
            public List<FSHFField> fields() {
                List<FSHFField> f = new ArrayList<>();
                f.add(FSHFField.section("Ubicación en la casa"));
                f.add(FSHFField.number("x", "Esquina X", -200, 200, 0.01, "m"));
                f.add(FSHFField.number("z", "Esquina Z", -200, 200, 0.01, "m"));
                List<FSHFField.Option> turns = new ArrayList<>();
                for (int d = 0; d < 360; d += 90) {
                    turns.add(new FSHFField.Option(String.valueOf(d), d + "°" + (d == 0 ? " (como se dibujó)" : "")));
                }
                f.add(FSHFField.select("rotation", "Giro", turns));
                List<FSHFField.Option> levels = new ArrayList<>();
                int max = Math.min(FSHFHome.MAX_LEVELS, host.home().levels() + 1);
                for (int l = 0; l < max; l++) levels.add(new FSHFField.Option(String.valueOf(l), levelName(l)));
                f.add(FSHFField.select("level", "Piso", levels));
                f.add(FSHFField.section("Medidas de la habitación"));
                f.add(FSHFField.slider("width", "Ancho (X)", FSHFRoomDims.MIN_SIDE, 12, 0.05, "m"));
                f.add(FSHFField.slider("length", "Largo (Z)", FSHFRoomDims.MIN_SIDE, 12, 0.05, "m"));
                f.add(FSHFField.number("height", "Alto", FSHFRoomDims.MIN_HEIGHT, FSHFRoomDims.MAX_HEIGHT, 0.01, "m"));
                f.add(FSHFField.number("wall", "Grosor de muro", FSHFRoomDims.MIN_WALL, FSHFRoomDims.MAX_WALL, 0.01, "m"));
                return f;
            }

            @Override
            public Object get(String key) {
                FSHFRoomDims r = s.project.room;
                return switch (key) {
                    case "x" -> s.x;
                    case "z" -> s.z;
                    case "rotation" -> String.valueOf(s.rotation);
                    case "level" -> String.valueOf(s.level);
                    case "width" -> r.width();
                    case "length" -> r.length();
                    case "height" -> r.height();
                    case "wall" -> r.wallThickness();
                    default -> null;
                };
            }

            @Override
            public String set(String key, Object value) {
                if (key.equals("rotation")) return host.rotate(s, Integer.parseInt((String) value));
                if (key.equals("level")) return host.moveToLevel(s, Integer.parseInt((String) value));
                if (!(value instanceof Double v) || !Double.isFinite(v)) return "Escribe un número.";
                FSHFRoomDims r = s.project.room;
                return switch (key) {
                    case "x" -> host.move(s, v, s.z);
                    case "z" -> host.move(s, s.x, v);
                    case "width" -> host.resize(s, new FSHFRoomDims(v, r.length(), r.height(), r.wallThickness()));
                    case "length" -> host.resize(s, new FSHFRoomDims(r.width(), v, r.height(), r.wallThickness()));
                    case "height" -> host.resize(s, new FSHFRoomDims(r.width(), r.length(), v, r.wallThickness()));
                    case "wall" -> host.resize(s, new FSHFRoomDims(r.width(), r.length(), r.height(), v));
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
                        new Action("⟳ Girar 90°", "Girarlo sobre su centro, en sentido horario",
                                () -> host.rotate(s, (s.rotation + 90) % 360)),
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
