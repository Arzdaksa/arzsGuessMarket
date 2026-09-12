package ui.fx;

import engine.GuessMarketEngine;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

final class WelcomeView {

    private WelcomeView() {}

    static VBox build(GuessMarketEngine engine) {
        Label title = new Label("Welcome to Guess Market");
        title.getStyleClass().add("content-title");

        Label subtitle = new Label("Use the menu on the left to load data and manage events.");
        subtitle.getStyleClass().add("content-subtitle");

        String hint = engine.isLoaded
                ? engine.getEvents().size() + " event(s) are loaded. Pick a current user in the sidebar, then try "
                        + "\"Show all events\" to browse them."
                : "No data loaded yet. Start with \"Load XML file\" on the left.";
        Label hintLabel = new Label(hint);
        hintLabel.getStyleClass().add("value-label");

        VBox card = TradeStatusView.card("Getting Started", hintLabel);

        return new VBox(16, title, subtitle, card);
    }
}
