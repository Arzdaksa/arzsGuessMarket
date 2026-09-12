package ui.fx;

import engine.Event;
import engine.GuessMarketEngine;
import engine.User;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

final class OpenEventWizardView {

    private OpenEventWizardView() {}

    private static final class State {
        Event event;
    }

    static VBox build(ScreenController controller, GuessMarketEngine engine) {
        Label title = new Label("Open an Event");
        title.getStyleClass().add("content-title");

        VBox stepsBox = new VBox(16);
        renderSteps(stepsBox, new State(), controller, engine);

        return new VBox(16, title, stepsBox);
    }

    private static void renderSteps(VBox stepsBox, State state, ScreenController controller, GuessMarketEngine engine) {
        stepsBox.getChildren().clear();

        if (state.event == null) {
            stepsBox.getChildren().add(TradeStatusView.card("1. Choose an inactive event",
                    EventPickerTable.build(engine.getInactiveEvents(), event -> {
                        state.event = event;
                        renderSteps(stepsBox, state, controller, engine);
                    })));
            return;
        }

        stepsBox.getChildren().add(TradeStatusView.breadcrumbChip("Event: " + EventFormat.describe(state.event), () -> {
            state.event = null;
            renderSteps(stepsBox, state, controller, engine);
        }));

        Event event = state.event;
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Market maker", event.getMarketMaker() == null ? "—" : event.getMarketMaker().getName()});
        rows.add(new String[]{"Cost to open", String.format("%.2f", event.previewOpenCost())});

        Button openButton = new Button("Fund & Open");
        openButton.getStyleClass().add("primary-button");
        openButton.setOnAction(e -> {
            if (!controller.requireCurrentUser()) return;
            User user = controller.getCurrentUser();
            try {
                event.open(user);
                controller.showInfo("Event opened", "'" + event.getName() + "' is now active.");
                controller.showEventDashboard(event);
            } catch (RuntimeException ex) {
                controller.showError("Open event failed", ex.getMessage());
            }
        });

        VBox content = new VBox(14, TradeStatusView.fieldValueGrid(rows), openButton);
        stepsBox.getChildren().add(TradeStatusView.card("2. Confirm & Open", content));
    }
}
