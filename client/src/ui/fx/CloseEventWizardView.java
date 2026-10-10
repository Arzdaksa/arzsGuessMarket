package ui.fx;

import dto.EventDto;
import dto.EventStatus;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

final class CloseEventWizardView {

    private CloseEventWizardView() {}

    private static final class State {
        EventDto event;
        String option;
    }

    static VBox build(ScreenController controller, MarketModel model) {
        Label title = new Label("Close an Event");
        title.getStyleClass().add("content-title");

        VBox stepsBox = new VBox(16);
        renderSteps(stepsBox, new State(), controller, model);

        return new VBox(16, title, stepsBox);
    }

    private static void renderSteps(VBox stepsBox, State state, ScreenController controller, MarketModel model) {
        stepsBox.getChildren().clear();

        if (state.event == null) {
            stepsBox.getChildren().add(TradeStatusView.card("1. Choose one of your active events to close",
                    EventPickerTable.build(model.myEvents(EventStatus.ACTIVE), event -> {
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
            stepsBox.getChildren().add(TradeStatusView.card("2. Choose the winning option",
                    EventPickerTable.buildOptions(state.event.optionNames(), option -> {
                        state.option = option;
                        renderSteps(stepsBox, state, controller, model);
                    })));
            return;
        }

        stepsBox.getChildren().add(TradeStatusView.breadcrumbChip("Winning option: " + state.option, () -> {
            state.option = null;
            renderSteps(stepsBox, state, controller, model);
        }));

        Button closeButton = new Button("Close Event");
        closeButton.getStyleClass().add("primary-button");
        closeButton.setOnAction(e -> {
            boolean confirmed = controller.showConfirm("Close event",
                    "Close event '" + state.event.name() + "' with winning option '" + state.option
                            + "'? This cannot be undone.");
            if (!confirmed) return;

            int eventId = state.event.id();
            String winningOption = state.option;
            closeButton.setDisable(true);
            controller.submit("Close event failed", () -> {
                controller.client().closeEvent(eventId, winningOption);
                return null;
            }, ok -> controller.showEventDashboard(eventId), () -> closeButton.setDisable(false));
        });

        stepsBox.getChildren().add(TradeStatusView.card("3. Confirm", closeButton));
    }
}
