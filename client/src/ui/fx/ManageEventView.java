package ui.fx;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class ManageEventView {

    private ManageEventView() {}

    static VBox build(ScreenController controller) {
        Label title = new Label("Manage My Events");
        title.getStyleClass().add("content-title");
        Label subtitle = new Label("Market makers open and close their own events here.");
        subtitle.getStyleClass().add("content-subtitle");

        VBox openCard = ChoiceCard.build("Open an Event",
                "Fund an inactive event you're the market maker for and start trading.",
                "Open an Event", e -> controller.showOpenEventWizard());
        VBox closeCard = ChoiceCard.build("Close an Event",
                "Resolve an active event you're the market maker for and pay out winners.",
                "Close an Event", e -> controller.showCloseEventWizard());
        VBox createCard = ChoiceCard.build("Create an Event (Bonus)",
                "Define a new event by hand instead of uploading XML; you become its market maker.",
                "Create an Event", e -> controller.showCreateEventWizard());

        HBox.setHgrow(openCard, Priority.ALWAYS);
        HBox.setHgrow(closeCard, Priority.ALWAYS);
        HBox row = new HBox(16, openCard, closeCard);

        return new VBox(16, title, subtitle, row, createCard);
    }
}
