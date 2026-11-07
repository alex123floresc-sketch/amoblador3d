package pe.edu.unaj.amoblador3d.ui;

import java.util.List;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import pe.edu.unaj.amoblador3d.design.FSHFTemplates;
import pe.edu.unaj.amoblador3d.model.FSHFColumn;
import pe.edu.unaj.amoblador3d.model.FSHFFootprint;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;
import pe.edu.unaj.amoblador3d.model.FSHFOpening;
import pe.edu.unaj.amoblador3d.model.FSHFProject;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;

final class FSHFTemplatesDialog {
    private static final double PREVIEW_W = 340, PREVIEW_H = 280;

    private FSHFTemplatesDialog() {}

    static Optional<Choice> show(Window owner, List<FSHFTemplates.Template> templates) {
        record Preview(FSHFTemplates.Template template, FSHFProject project) {}
        ListView<Preview> list = new ListView<>();
        for (FSHFTemplates.Template t : templates) list.getItems().add(new Preview(t, t.build().get()));
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Preview item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.template().name());
            }
        });
        list.setPrefWidth(190);

        Canvas canvas = new Canvas(PREVIEW_W, PREVIEW_H);
        Label description = new Label();
        description.setWrapText(true);
        description.setMaxWidth(PREVIEW_W);
        description.getStyleClass().add("hint");
        list.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n == null) return;
            draw(canvas.getGraphicsContext2D(), n.project());
            description.setText(n.template().description());
        });

        Dialog<Choice> dialog = new Dialog<>();
        FSHFDialogs.prepare(dialog, owner, "Nuevo desde plantilla", "Elige una habitación para empezar");
        ButtonType create = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(create, ButtonType.CANCEL);
        HBox content = new HBox(14, list, new VBox(8, canvas, description));
        content.setPadding(new Insets(4));
        dialog.getDialogPane().setContent(content);
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && list.getSelectionModel().getSelectedItem() != null) {
                ((javafx.scene.control.Button) dialog.getDialogPane().lookupButton(create)).fire();
            }
        });
        dialog.setResultConverter(b -> {
            Preview p = list.getSelectionModel().getSelectedItem();
            return b == create && p != null ? new Choice(p.template().name(), p.project()) : null;
        });
        list.getSelectionModel().selectFirst();
        return dialog.showAndWait();
    }

    record Choice(String name, FSHFProject project) {}

    private static void draw(GraphicsContext g, FSHFProject p) {
        FSHFRoomDims r = p.room;
        double margin = 18, t = r.wallThickness();
        double scale = Math.min((PREVIEW_W - 2 * margin) / (r.width() + 2 * t), (PREVIEW_H - 2 * margin) / (r.length() + 2 * t));
        double ox = (PREVIEW_W - (r.width() + 2 * t) * scale) / 2 + t * scale;
        double oz = (PREVIEW_H - (r.length() + 2 * t) * scale) / 2 + t * scale;
        g.setFill(Color.web("#1d2125"));
        g.fillRect(0, 0, PREVIEW_W, PREVIEW_H);
        g.setFill(Color.web("#b9b2a6"));
        g.fillRect(ox - t * scale, oz - t * scale, (r.width() + 2 * t) * scale, (r.length() + 2 * t) * scale);
        g.setFill(Color.web(p.floor.color));
        g.fillRect(ox, oz, r.width() * scale, r.length() * scale);

        for (FSHFOpening o : p.openings) {
            double a0 = o.offset * scale, a1 = (o.offset + o.width) * scale, th = t * scale;
            double x, z, w, h;
            switch (o.wall) {
                case NORTE -> { x = ox + a0; z = oz - th; w = a1 - a0; h = th; }
                case SUR -> { x = ox + a0; z = oz + r.length() * scale; w = a1 - a0; h = th; }
                case OESTE -> { x = ox - th; z = oz + a0; w = th; h = a1 - a0; }
                default -> { x = ox + r.width() * scale; z = oz + a0; w = th; h = a1 - a0; }
            }
            g.setFill(o.type.isDoor() ? Color.WHITE : Color.web("#9fd3ff"));
            g.fillRect(x, z, w, h);
        }

        for (FSHFColumn c : p.columns) {
            g.setFill(Color.web("#b9b2a6"));
            g.fillRect(ox + c.minX() * scale, oz + c.minZ() * scale, c.width * scale, c.depth * scale);
        }
        for (FSHFFurniture f : p.furniture) {
            double[][] c = FSHFFootprint.of(f).corners();
            double[] xs = new double[4], zs = new double[4];
            for (int i = 0; i < 4; i++) {
                xs[i] = ox + c[i][0] * scale;
                zs[i] = oz + c[i][1] * scale;
            }
            g.setFill(f.color != null ? Color.web(f.color) : Color.web("#7ea27a"));
            g.fillPolygon(xs, zs, 4);
            g.setStroke(Color.web("#00000088"));
            g.setLineWidth(1);
            g.strokePolygon(xs, zs, 4);
        }
        g.setFill(Color.web("#e6e8eb"));
        g.fillText(pe.edu.unaj.amoblador3d.util.FSHFUnits.number(r.width()) + " × "
                + pe.edu.unaj.amoblador3d.util.FSHFUnits.number(r.length()) + " m", 6, PREVIEW_H - 6);
    }
}
