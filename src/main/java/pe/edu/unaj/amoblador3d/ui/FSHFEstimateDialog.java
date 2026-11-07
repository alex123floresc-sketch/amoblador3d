package pe.edu.unaj.amoblador3d.ui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.function.Supplier;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import pe.edu.unaj.amoblador3d.estimate.FSHFEstimate;
import pe.edu.unaj.amoblador3d.estimate.FSHFPriceList;
import pe.edu.unaj.amoblador3d.estimate.FSHFQuantities;
import pe.edu.unaj.amoblador3d.persistence.FSHFPriceRepository;
import pe.edu.unaj.amoblador3d.util.FSHFUnits;

final class FSHFEstimateDialog {
    private final Stage stage = new Stage();
    private final Supplier<FSHFEstimate> compute;
    private final FSHFPriceList prices;
    private final FSHFPriceRepository repository;
    private final Runnable onExportPdf;
    private final String projectName;
    private final TableView<FSHFEstimate.Line> table = new TableView<>();
    private final Label summary = new Label(), total = new Label(), error = new Label();
    private FSHFEstimate estimate;

    static void show(Window owner, String projectName, Supplier<FSHFEstimate> compute, FSHFPriceList prices,
                     FSHFPriceRepository repository, Runnable onExportPdf) {
        new FSHFEstimateDialog(owner, projectName, compute, prices, repository, onExportPdf).stage.showAndWait();
    }

    private FSHFEstimateDialog(Window owner, String projectName, Supplier<FSHFEstimate> compute, FSHFPriceList prices,
                           FSHFPriceRepository repository, Runnable onExportPdf) {
        this.compute = compute;
        this.prices = prices;
        this.repository = repository;
        this.onExportPdf = onExportPdf;
        this.projectName = projectName;

        TableColumn<FSHFEstimate.Line, String> section = column("Rubro", 100, l -> l.section().label());
        TableColumn<FSHFEstimate.Line, String> concept = column("Concepto", 330, l -> l.detail().isEmpty()
                ? l.concept() : l.concept() + "\n" + l.detail());
        TableColumn<FSHFEstimate.Line, String> qty = column("Cantidad", 80, l -> FSHFUnits.number(l.quantity()));
        TableColumn<FSHFEstimate.Line, String> unit = column("Unidad", 60, FSHFEstimate.Line::unit);
        TableColumn<FSHFEstimate.Line, FSHFEstimate.Line> price = new TableColumn<>("P. unitario");
        price.setPrefWidth(120);
        price.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        price.setCellFactory(c -> new PriceCell());
        price.setSortable(false);
        TableColumn<FSHFEstimate.Line, String> subtotal = column("Subtotal", 110, l -> FSHFUnits.money(l.subtotal()));
        concept.setCellFactory(c -> new WrappingCell());
        unit.setStyle("-fx-alignment: CENTER-LEFT;");
        section.setStyle("-fx-alignment: CENTER-LEFT;");
        qty.setStyle("-fx-alignment: CENTER-RIGHT;");
        subtotal.setStyle("-fx-alignment: CENTER-RIGHT;");
        table.getColumns().addAll(java.util.List.of(section, concept, qty, unit, price, subtotal));
        table.setPlaceholder(new Label("No hay nada que presupuestar."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setEditable(true);
        VBox.setVgrow(table, Priority.ALWAYS);

        Label heading = new Label("Metrado y presupuesto");
        heading.getStyleClass().add("panel-title");
        summary.getStyleClass().add("hint");
        summary.setWrapText(true);
        Label hint = new Label("Haz doble clic en un precio para cambiarlo. Los precios se guardan para todos los proyectos. "
                + "Son referenciales (Lima) y no incluyen mano de obra.");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);
        error.getStyleClass().add("error");
        error.setManaged(false);
        total.getStyleClass().add("estimate-total");

        Button reset = new Button("Precios referenciales");
        reset.setTooltip(new Tooltip("Descartar los precios que cambiaste"));
        reset.setOnAction(e -> resetPrices());
        Button csv = new Button("Exportar CSV…");
        csv.setTooltip(new Tooltip("Para abrirlo en Excel"));
        csv.setOnAction(e -> exportCsv());
        Button pdf = new Button("Informe PDF…");
        pdf.getStyleClass().add("primary");
        pdf.setTooltip(new Tooltip("Planos, muebles y presupuesto en un PDF (Ctrl+Shift+P)"));
        pdf.setOnAction(e -> onExportPdf.run());
        Button close = new Button("Cerrar");
        close.setCancelButton(true);
        close.setOnAction(e -> stage.close());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(8, reset, csv, spacer, total, pdf, close);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(10, heading, summary, table, error, hint, buttons);
        body.setPadding(new Insets(16));
        body.getStyleClass().add("inspector");
        Scene scene = new Scene(new BorderPane(body), 900, 600);
        scene.getStylesheets().add(FSHFMainWindow.STYLESHEET);
        stage.setScene(scene);
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Presupuesto — " + projectName);
        stage.setMinWidth(700);
        stage.setMinHeight(420);
        recompute();
    }

    private static TableColumn<FSHFEstimate.Line, String> column(String title, double width,
                                                            java.util.function.Function<FSHFEstimate.Line, String> value) {
        TableColumn<FSHFEstimate.Line, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setSortable(false);
        c.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        return c;
    }

    private void recompute() {
        estimate = compute.get();
        table.getItems().setAll(estimate.lines());
        FSHFQuantities q = estimate.quantities();
        summary.setText("Piso: " + FSHFUnits.number(FSHFUnits.round(q.floorArea())) + " m²   ·   Paredes (sin aberturas): "
                + FSHFUnits.number(FSHFUnits.round(q.wallNetTotal())) + " m²   ·   Ventanas y puertas: "
                + FSHFUnits.number(FSHFUnits.round(q.openingsArea())) + " m²   ·   Perímetro: " + FSHFUnits.meters(q.perimeter()));
        total.setText("Total: " + FSHFUnits.money(estimate.total()));
    }

    private void showError(String message) {
        error.setText(message == null ? "" : message);
        error.setManaged(message != null);
        error.setVisible(message != null);
    }

    private void setPrice(FSHFPriceList.Key key, double value) {
        try {
            prices.set(key, FSHFUnits.round(value));
            repository.save(key, FSHFUnits.round(value));
            showError(null);
        } catch (IllegalArgumentException | SQLException e) {
            showError("No se pudo guardar el precio: " + e.getMessage());
        }
        recompute();
    }

    private void resetPrices() {
        if (!FSHFDialogs.confirm(stage, "¿Volver a los precios referenciales?", "Se descartarán todos los precios que cambiaste.",
                "Restablecer")) {
            return;
        }
        try {
            repository.resetAll();
            prices.resetAll();
            showError(null);
        } catch (SQLException e) {
            showError("No se pudieron restablecer los precios: " + e.getMessage());
        }
        recompute();
    }

    private void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar presupuesto");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        chooser.setInitialFileName(projectName + " - presupuesto.csv");
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        StringBuilder sb = new StringBuilder("﻿Rubro;Concepto;Detalle;Cantidad;Unidad;Precio unitario;Subtotal\r\n");
        for (FSHFEstimate.Line l : estimate.lines()) {
            sb.append(csv(l.section().label())).append(';').append(csv(l.concept())).append(';').append(csv(l.detail()))
                    .append(';').append(decimal(l.quantity())).append(';').append(csv(l.unit()))
                    .append(';').append(decimal(l.unitPrice())).append(';').append(decimal(l.subtotal())).append("\r\n");
        }
        sb.append(";TOTAL;;;;;").append(decimal(estimate.total())).append("\r\n");
        try {
            Files.writeString(file.toPath(), sb, StandardCharsets.UTF_8);
        } catch (IOException e) {
            showError("No se pudo exportar: " + e.getMessage());
        }
    }

    private static String csv(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private static String decimal(double v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v).replace('.', ',');
    }

    private static final class WrappingCell extends TableCell<FSHFEstimate.Line, String> {
        private final javafx.scene.text.Text text = new javafx.scene.text.Text();

        WrappingCell() {
            text.getStyleClass().add("wrapping-cell-text");
            text.wrappingWidthProperty().bind(widthProperty().subtract(12));
            setPrefHeight(Region.USE_COMPUTED_SIZE);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            text.setText(empty || item == null ? "" : item);
            setGraphic(empty || item == null ? null : text);
        }
    }

    private final class PriceCell extends TableCell<FSHFEstimate.Line, FSHFEstimate.Line> {
        private final TextField field = new TextField();

        PriceCell() {
            setStyle("-fx-alignment: CENTER-RIGHT;");
            field.setOnAction(e -> apply());
            field.focusedProperty().addListener((obs, o, focused) -> {
                if (!focused && isEditing()) apply();
            });
            field.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) cancelEdit();
            });
            setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && getItem() != null) startEdit();
            });
        }

        @Override
        public void startEdit() {
            if (getItem() == null) return;
            super.startEdit();
            field.setText(FSHFUnits.number(getItem().unitPrice()));
            setText(null);
            setGraphic(field);
            field.requestFocus();
            field.selectAll();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            show(getItem());
        }

        private void apply() {
            FSHFEstimate.Line line = getItem();
            Double value = FSHFUnits.parse(field.getText());
            super.cancelEdit();
            if (line == null) return;
            if (value == null || value < 0) {
                showError("Escribe el precio como un número positivo (por ejemplo 65,50).");
                show(line);
                return;
            }
            setPrice(line.priceKey(), value);
        }

        @Override
        protected void updateItem(FSHFEstimate.Line item, boolean empty) {
            super.updateItem(item, empty);
            if (!isEditing()) show(empty ? null : item);
        }

        private void show(FSHFEstimate.Line item) {
            setGraphic(null);
            if (item == null) {
                setText(null);
                setTooltip(null);
                return;
            }
            boolean custom = prices.isCustom(item.priceKey());
            setText(FSHFUnits.money(item.unitPrice()) + (custom ? "  ✎" : ""));
            setTooltip(new Tooltip(custom ? "Precio cambiado por ti (referencial: "
                    + FSHFUnits.money(FSHFPriceList.defaultPrice(item.priceKey())) + ")" : "Precio referencial · doble clic para cambiarlo"));
        }
    }
}
