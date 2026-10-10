package ui.fx;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

final class ProgressDialog {

    private final Stage stage;
    private final ProgressBar progressBar = new ProgressBar(0);

    ProgressDialog(Stage owner, String message) {
        Label label = new Label(message);
        label.getStyleClass().add("value-label");
        progressBar.setPrefWidth(280);

        VBox box = new VBox(14, label, progressBar);
        box.getStyleClass().add("card");

        stage = new Stage(StageStyle.UNDECORATED);
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        Scene scene = new Scene(box);
        if (owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
    }

    ProgressBar progressBar() {return progressBar;}

    void show() {stage.show();}
    void close() {stage.close();}
}
