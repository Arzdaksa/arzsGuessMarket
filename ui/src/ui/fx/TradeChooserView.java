package ui.fx;

import engine.GuessMarketEngine;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class TradeChooserView {

    private TradeChooserView() {}

    static VBox build(ScreenController controller, GuessMarketEngine engine) {
        Label title = new Label("Trade");
        title.getStyleClass().add("content-title");
        Label subtitle = new Label("Choose which market mechanism to trade in.");
        subtitle.getStyleClass().add("content-subtitle");

        VBox lmsrCard = ChoiceCard.build("LMSR Trade",
                "Buy shares of an option; the price moves automatically with demand.",
                "Choose LMSR", e -> controller.showParticipateWizard());
        VBox obCard = ChoiceCard.build("Order Book Order",
                "Place a buy or sell limit order at a price you choose.",
                "Choose Order Book", e -> controller.showSubmitOrderWizard());

        HBox.setHgrow(lmsrCard, Priority.ALWAYS);
        HBox.setHgrow(obCard, Priority.ALWAYS);
        HBox row = new HBox(16, lmsrCard, obCard);

        return new VBox(16, title, subtitle, row);
    }
}
