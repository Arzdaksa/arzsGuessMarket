package ui.fx;

import engine.Event;
import engine.GuessMarketEngine;
import engine.MethodType;
import engine.OrderSide;
import engine.OrderSubmissionResult;
import engine.User;
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
        Event event;
        String option;
        OrderSide side;
    }

    static VBox build(ScreenController controller, GuessMarketEngine engine) {
        Label title = new Label("Submit an Order");
        title.getStyleClass().add("content-title");

        VBox stepsBox = new VBox(16);
        renderSteps(stepsBox, new State(), controller, engine);

        return new VBox(16, title, stepsBox);
    }

    private static void renderSteps(VBox stepsBox, State state, ScreenController controller, GuessMarketEngine engine) {
        stepsBox.getChildren().clear();

        if (state.event == null) {
            List<Event> obEvents = engine.getActiveEvents().stream()
                    .filter(e -> e.getEventType() == MethodType.ORDER_BOOK)
                    .toList();
            stepsBox.getChildren().add(TradeStatusView.card("1. Choose an active Order Book event",
                    EventPickerTable.build(obEvents, event -> {
                        state.event = event;
                        renderSteps(stepsBox, state, controller, engine);
                    })));
            return;
        }

        stepsBox.getChildren().add(TradeStatusView.breadcrumbChip("Event: " + EventFormat.describe(state.event), () -> {
            state.event = null;
            state.option = null;
            state.side = null;
            renderSteps(stepsBox, state, controller, engine);
        }));

        if (state.option == null) {
            stepsBox.getChildren().add(TradeStatusView.card("2. Choose an option",
                    EventPickerTable.buildOptions(state.event.getOptionNames(), option -> {
                        state.option = option;
                        renderSteps(stepsBox, state, controller, engine);
                    })));
            return;
        }

        stepsBox.getChildren().add(TradeStatusView.breadcrumbChip("Option: " + state.option, () -> {
            state.option = null;
            state.side = null;
            renderSteps(stepsBox, state, controller, engine);
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
                state.side = OrderSide.BID;
                renderSteps(stepsBox, state, controller, engine);
            });
            sell.setOnAction(e -> {
                state.side = OrderSide.ASK;
                renderSteps(stepsBox, state, controller, engine);
            });

            HBox row = new HBox(12, buy, sell);
            stepsBox.getChildren().add(TradeStatusView.card("3. Buy or Sell", row));
            return;
        }

        stepsBox.getChildren().add(TradeStatusView.breadcrumbChip(
                "Side: " + (state.side == OrderSide.BID ? "Buy" : "Sell"), () -> {
                    state.side = null;
                    renderSteps(stepsBox, state, controller, engine);
                }));

        stepsBox.getChildren().add(quantityPriceCard(state, controller));
    }

    private static VBox quantityPriceCard(State state, ScreenController controller) {
        double maxPrice = state.event.getOrderBookConfig().getD() - 0.01;

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
            if (!controller.requireCurrentUser()) return;
            User user = controller.getCurrentUser();
            int quantity = Integer.parseInt(quantityField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());
            try {
                OrderSubmissionResult result = state.event.getOrderBook()
                        .submitOrder(user, state.option, state.side, quantity, price);
                controller.showOrderReceipt(result, state.event);
            } catch (RuntimeException ex) {
                controller.showError("Order failed", ex.getMessage());
            }
        });

        HBox fields = new HBox(12, quantityField, priceField);
        VBox content = new VBox(10, fields, errorLabel, confirmButton);
        return TradeStatusView.card("4. Quantity & Price", content);
    }

    static VBox buildReceipt(OrderSubmissionResult result) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Filled quantity", String.valueOf(result.filledQuantity())});
        rows.add(new String[]{"Resting quantity", String.valueOf(result.restingQuantity())});
        rows.add(new String[]{"Fills produced", String.valueOf(result.newFills().size())});
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
