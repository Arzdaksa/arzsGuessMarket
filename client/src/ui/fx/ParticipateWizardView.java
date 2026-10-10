package ui.fx;

import dto.EventDto;
import dto.EventType;
import dto.TradeDto;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

final class ParticipateWizardView {

    private ParticipateWizardView() {}

    private static final class State {
        EventDto event;
        String option;
    }

    static VBox build(ScreenController controller, MarketModel model) {
        Label title = new Label("Participate in an Event");
        title.getStyleClass().add("content-title");

        VBox stepsBox = new VBox(16);
        renderSteps(stepsBox, new State(), controller, model);

        return new VBox(16, title, stepsBox);
    }

    private static void renderSteps(VBox stepsBox, State state, ScreenController controller, MarketModel model) {
        stepsBox.getChildren().clear();

        if (state.event == null) {
            List<EventDto> lmsrEvents = model.getActiveEvents().stream()
                    .filter(e -> e.type() == EventType.LMSR)
                    .toList();
            stepsBox.getChildren().add(TradeStatusView.card("1. Choose an active event",
                    EventPickerTable.build(lmsrEvents, event -> {
                        state.event = event;
                        renderSteps(stepsBox, state, controller, model);
                    })));
            return;
        }

        stepsBox.getChildren().add(TradeStatusView.breadcrumbChip("Event: " + EventFormat.describe(state.event), () -> {
            state.event = null;
            state.option = null;
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
            renderSteps(stepsBox, state, controller, model);
        }));

        stepsBox.getChildren().add(quantityCard(state, controller));
    }

    private static VBox quantityCard(State state, ScreenController controller) {
        TextField quantityField = new TextField();
        quantityField.setPromptText("Quantity of shares");
        quantityField.setMaxWidth(200);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        Button confirmButton = new Button("Confirm Trade");
        confirmButton.getStyleClass().add("primary-button");
        confirmButton.setDisable(true);

        quantityField.textProperty().addListener((obs, old, val) ->
                updateConfirmState(quantityField, errorLabel, confirmButton));

        confirmButton.setOnAction(e -> {
            int eventId = state.event.id();
            String option = state.option;
            int quantity = Integer.parseInt(quantityField.getText().trim());
            confirmButton.setDisable(true);
            controller.submit("Trade failed",
                    () -> controller.client().trade(eventId, option, quantity),
                    trade -> controller.showTradeReceipt(trade, eventId),
                    () -> confirmButton.setDisable(false));
        });

        VBox content = new VBox(10, quantityField, errorLabel, confirmButton);
        return TradeStatusView.card("3. Enter quantity of shares", content);
    }

    // used by ScreenController to prepend a receipt panel above the refreshed dashboard
    static VBox buildReceipt(TradeDto trade) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Option bought", trade.optionName()});
        rows.add(new String[]{"Quantity", String.valueOf(trade.quantity())});
        rows.add(new String[]{"Shares cost", String.format("%.2f", trade.sharesCost())});
        rows.add(new String[]{"Commission paid", String.format("%.2f", trade.commissionPaid())});
        rows.add(new String[]{"Total paid", String.format("%.2f", trade.totalPaid())});

        return TradeStatusView.card("Purchase Receipt", TradeStatusView.fieldValueGrid(rows));
    }

    private static void updateConfirmState(TextField quantityField, Label errorLabel, Button confirmButton) {
        String text = quantityField.getText() == null ? "" : quantityField.getText().trim();
        if (text.isEmpty()) {
            errorLabel.setText("");
            confirmButton.setDisable(true);
            return;
        }

        try {
            int quantity = Integer.parseInt(text);
            if (quantity <= 0) {
                errorLabel.setText("Quantity must be a positive number.");
                confirmButton.setDisable(true);
            } else {
                errorLabel.setText("");
                confirmButton.setDisable(false);
            }
        } catch (NumberFormatException ex) {
            errorLabel.setText("'" + text + "' is not a valid number.");
            confirmButton.setDisable(true);
        }
    }
}
