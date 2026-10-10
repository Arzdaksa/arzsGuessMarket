package ui.fx;

import dto.EventDto;
import dto.EventType;
import dto.OrderResultDto;
import dto.Side;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

final class SubmitOrderWizardView {

    private SubmitOrderWizardView() {}

    private static final class State {
        EventDto event;
        String option;
        Side side;
    }

    static VBox build(ScreenController controller, MarketModel model) {
        Label title = new Label("Submit an Order");
        title.getStyleClass().add("content-title");

        VBox stepsBox = new VBox(16);
        renderSteps(stepsBox, new State(), controller, model);

        return new VBox(16, title, stepsBox);
    }

    private static void renderSteps(VBox stepsBox, State state, ScreenController controller, MarketModel model) {
        stepsBox.getChildren().clear();

        if (state.event == null) {
            List<EventDto> obEvents = model.getActiveEvents().stream()
                    .filter(e -> e.type() == EventType.ORDER_BOOK)
                    .toList();
            stepsBox.getChildren().add(TradeStatusView.card("1. Choose an active Order Book event",
                    EventPickerTable.build(obEvents, event -> {
                        state.event = event;
                        renderSteps(stepsBox, state, controller, model);
                    })));
            return;
        }

        stepsBox.getChildren().add(TradeStatusView.breadcrumbChip("Event: " + EventFormat.describe(state.event), () -> {
            state.event = null;
            state.option = null;
            state.side = null;
            renderSteps(stepsBox, state, controller, model);
        }));

        if (state.option == null) {
            stepsBox.getChildren().add(TradeStatusView.card("2. Choose an option",
                    EventPickerTable.buildOptions(state.event.optionNames(), option -> {
                        state.option = option;
                        renderSteps(stepsBox, state, controller, model);
                    })));
            return;
        }

        stepsBox.getChildren().add(TradeStatusView.breadcrumbChip("Option: " + state.option, () -> {
            state.option = null;
            state.side = null;
            renderSteps(stepsBox, state, controller, model);
        }));

        if (state.side == null) {
            ToggleGroup group = new ToggleGroup();
            ToggleButton buy = new ToggleButton("Buy (Bid)");
            buy.getStyleClass().add("buy-toggle");
            buy.setToggleGroup(group);
            ToggleButton sell = new ToggleButton("Sell (Ask)");
            sell.getStyleClass().add("sell-toggle");
            sell.setToggleGroup(group);

            buy.setOnAction(e -> {
                state.side = Side.BID;
                renderSteps(stepsBox, state, controller, model);
            });
            sell.setOnAction(e -> {
                state.side = Side.ASK;
                renderSteps(stepsBox, state, controller, model);
            });

            HBox row = new HBox(12, buy, sell);
            stepsBox.getChildren().add(TradeStatusView.card("3. Buy or Sell", row));
            return;
        }

        stepsBox.getChildren().add(TradeStatusView.breadcrumbChip(
                "Side: " + (state.side == Side.BID ? "Buy" : "Sell"), () -> {
                    state.side = null;
                    renderSteps(stepsBox, state, controller, model);
                }));

        stepsBox.getChildren().add(quantityPriceCard(state, controller));
    }

    private static VBox quantityPriceCard(State state, ScreenController controller) {
        double maxPrice = state.event.orderBook().d() - 0.01;

        TextField quantityField = new TextField();
        quantityField.setPromptText("Quantity");
        quantityField.setMaxWidth(160);

        TextField priceField = new TextField();
        priceField.setPromptText("Price (0 - " + maxPrice + ")");
        priceField.setMaxWidth(160);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        Button confirmButton = new Button("Submit Order");
        confirmButton.getStyleClass().add("primary-button");
        confirmButton.setDisable(true);

        Runnable validate = () -> updateConfirmState(quantityField, priceField, maxPrice, errorLabel, confirmButton);
        quantityField.textProperty().addListener((obs, old, val) -> validate.run());
        priceField.textProperty().addListener((obs, old, val) -> validate.run());

        confirmButton.setOnAction(e -> {
            int eventId = state.event.id();
            String option = state.option;
            Side side = state.side;
            int quantity = Integer.parseInt(quantityField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());
            confirmButton.setDisable(true);
            controller.submit("Order failed",
                    () -> controller.client().submitOrder(eventId, option, side, quantity, price),
                    result -> controller.showOrderReceipt(result, eventId),
                    () -> confirmButton.setDisable(false));
        });

        HBox fields = new HBox(12, quantityField, priceField);
        VBox content = new VBox(10, fields, errorLabel, confirmButton);
        return TradeStatusView.card("4. Quantity & Price", content);
    }

    static VBox buildReceipt(OrderResultDto result) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Filled quantity", String.valueOf(result.filledQuantity())});
        rows.add(new String[]{"Resting quantity", String.valueOf(result.restingQuantity())});
        rows.add(new String[]{"Fills produced", String.valueOf(result.fillCount())});
        return TradeStatusView.card("Order Result", TradeStatusView.fieldValueGrid(rows));
    }

    private static void updateConfirmState(TextField quantityField, TextField priceField, double maxPrice,
                                            Label errorLabel, Button confirmButton) {
        String qtyText = quantityField.getText() == null ? "" : quantityField.getText().trim();
        String priceText = priceField.getText() == null ? "" : priceField.getText().trim();
        if (qtyText.isEmpty() || priceText.isEmpty()) {
            errorLabel.setText("");
            confirmButton.setDisable(true);
            return;
        }
        try {
            int quantity = Integer.parseInt(qtyText);
            double price = Double.parseDouble(priceText);
            if (quantity <= 0) {
                errorLabel.setText("Quantity must be a positive number.");
                confirmButton.setDisable(true);
            } else if (price <= 0 || price > maxPrice) {
                errorLabel.setText("Price must be between 0 and " + maxPrice + ".");
                confirmButton.setDisable(true);
            } else {
                errorLabel.setText("");
                confirmButton.setDisable(false);
            }
        } catch (NumberFormatException ex) {
            errorLabel.setText("Enter valid numbers for quantity and price.");
            confirmButton.setDisable(true);
        }
    }
}
