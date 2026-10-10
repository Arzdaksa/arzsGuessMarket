package ui.fx;

import dto.UserDto;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

final class WelcomeView {

    private WelcomeView() {}

    static VBox build(MarketModel model) {
        Label title = new Label("Welcome to Guess Market, " + model.username());
        title.getStyleClass().add("content-title");

        Label subtitle = new Label("Everything you see is live from the server and shared with every connected user.");
        subtitle.getStyleClass().add("content-subtitle");

        UserDto me = model.me();
        String funds = me != null && me.balance() <= 0
                ? "Your balance is empty: use \"Add funds\" on the left before trading. "
                : "";
        String hint = model.getEvents().isEmpty()
                ? funds + "No events yet. Use \"Upload XML file\" to add events with you as their market maker."
                : funds + model.getEvents().size() + " event(s) are available. Try \"Show all events\" to browse them.";
        Label hintLabel = new Label(hint);
        hintLabel.getStyleClass().add("value-label");
        hintLabel.setWrapText(true);

        VBox card = TradeStatusView.card("Getting Started", hintLabel);

        return new VBox(16, title, subtitle, card);
    }
}
