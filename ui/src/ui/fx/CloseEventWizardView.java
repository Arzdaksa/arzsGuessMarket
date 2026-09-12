package ui.fx;

import engine.Event;
import engine.GuessMarketEngine;
import engine.User;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

final class CloseEventWizardView {

    private CloseEventWizardView() {}

    private static final class State {
        Event event;
        String option;
    }

    static VBox build(ScreenController controller, GuessMarketEngine engine) {
        Label title = new Label("Close an Event");
        title.getStyleClass().add("content-title");

        VBox stepsBox = new VBox(16);
        renderSteps(stepsBox, new State(), controller, engine);

        return new VBox(16, title, stepsBox);
    }

    private static void renderSteps(VBox stepsBox, State state, ScreenController controller, GuessMarketEngine engine) {
        stepsBox.getChildren().clear();

        if (state.event == null) {
            stepsBox.getChildren().add(TradeStatusView.card("1. Choose an active event to close",
                    EventPickerTable.build(engine.getActiveEvents(), event -> {
                        state.event = event;
                        renderSteps(stepsBox, state, controller, engine);
                    })));
            return;
        }

        stepsBox.getChildren().add(TradeStatusView.breadcrumbChip("Event: " + EventFormat.describe(state.event), () -> {
            state.event = null;
            state.option = null;
            renderSteps(stepsBox, state, controller, engine);
        }));

        if (state.option == null) {
            stepsBox.getChildren().add(TradeStatusView.card("2. Choose the winning option",
                    EventPickerTable.buildOptions(state.event.getOptionNames(), option -> {
                        state.option = option;
                        renderSteps(stepsBox, state, controller, engine);
                    })));
            return;
        }

        stepsBox.getChildren().add(TradeStatusView.breadcrumbChip("Winning option: " + state.option, () -> {
            state.option = null;
            renderSteps(stepsBox, state, controller, engine);
        }));

        Button closeButton = new Button("Close Event");
        closeButton.getStyleClass().add("primary-button");
        closeButton.setOnAction(e -> {
            if (!controller.requireCurrentUser()) return;
            User user = controller.getCurrentUser();
            boolean confirmed = controller.showConfirm("Close event",
                    "Close event '" + state.event.getName() + "' with winning option '" + state.option
                            + "'? This cannot be undone.");
            if (!confirmed) return;

            try {
                state.event.closeEvent(user, state.option);
                controller.showEventDashboard(state.event);
            } catch (RuntimeException ex) {
                controller.showError("Close event failed", ex.getMessage());
            }
        });

        stepsBox.getChildren().add(TradeStatusView.card("3. Confirm", closeButton));
    }
}
