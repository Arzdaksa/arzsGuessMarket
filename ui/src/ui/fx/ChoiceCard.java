package ui.fx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// a single "pick this option" card used by the Trade and Manage-Event chooser screens
final class ChoiceCard {

    private ChoiceCard() {}

    static VBox build(String heading, String description, String buttonLabel, EventHandler<ActionEvent> action) {
        Label headingLabel = new Label(heading);
        headingLabel.getStyleClass().add("card-title");
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("content-subtitle");
        descLabel.setWrapText(true);

        Button button = new Button(buttonLabel);
        button.getStyleClass().add("primary-button");
        button.setOnAction(action);

        VBox content = new VBox(10, headingLabel, descLabel, button);
        content.getStyleClass().add("card");
        return content;
    }
}
