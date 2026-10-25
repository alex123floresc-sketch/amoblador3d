package pe.edu.unaj.amoblador3d.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Aviso que aparece unos segundos sobre la vista 3D y se desvanece solo.
 * Para lo que el usuario debe saber pero no tiene que confirmar ("no hay espacio para la cama").
 */
final class Toast {
    enum Kind { INFO, ERROR }

    private final Label label = new Label();
    private final SequentialTransition animation;

    Toast(StackPane host) {
        label.getStyleClass().add("toast");
        label.setWrapText(true);
        label.setMaxWidth(520);
        label.setMouseTransparent(true);
        label.setOpacity(0);
        StackPane.setAlignment(label, Pos.TOP_CENTER);
        StackPane.setMargin(label, new javafx.geometry.Insets(14, 0, 0, 0));
        host.getChildren().add(label);

        FadeTransition in = new FadeTransition(Duration.millis(150), label);
        in.setToValue(1);
        FadeTransition out = new FadeTransition(Duration.millis(400), label);
        out.setToValue(0);
        animation = new SequentialTransition(in, new PauseTransition(Duration.seconds(4)), out);
    }

    void show(String message, Kind kind) {
        label.setText(message);
        label.getStyleClass().remove("toast-error");
        if (kind == Kind.ERROR) label.getStyleClass().add("toast-error");
        animation.stop();
        animation.playFromStart();
    }
}
