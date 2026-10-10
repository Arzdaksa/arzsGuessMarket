package ui.fx;

import dto.EventDto;
import dto.EventStatus;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

final class OpenEventWizardView {

    private OpenEventWizardView() {}

    private static final class State {
        EventDto event;
    }

    static VBox build(ScreenController controller, MarketModel model) {
        Label title = new Label("Open an Event");
        title.getStyleClass().add("content-title");

        VBox stepsBox = new VBox(16);
        renderSteps(stepsBox, new State(), controller, model);

        return new VBox(16, title, stepsBox);
    }

    private static void renderSteps(VBox stepsBox, State state, ScreenController controller, MarketModel model) {
        stepsBox.getChildren().clear();

        if (state.event == null) {
            stepsBox.getChildren().add(TradeStatusView.card("1. Choose one of your inactive events",
                    EventPickerTable.build(model.myEvents(EventStatus.INACTIVE), event -> {
                        state.event = event;
                        renderSteps(stepsBox, state, controller, model);
                    })));
            return;
        }

        stepsBox.getChildren().add(TradeStatusView.breadcrumbChip("Event: " + EventFormat.describe(state.event), () -> {
            state.event = null;
            renderSteps(stepsBox, state, controller, model);
        }));

        EventDto event = state.event;
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Market maker", event.marketMaker() == null ? "—" : event.marketMaker()});
        rows.add(new String[]{"Cost to open", String.format("%.2f", event.openCost())});

        Button openButton = new Button("Fund & Open");
        openButton.getStyleClass().add("primary-button");
        openButton.setOnAction(e -> {
            openButton.setDisable(true);
            controller.submit("Open event failed", () -> {
                controller.client().openEvent(event.id());
                return null;
            }, ok -> {
                controller.showInfo("Event opened", "'" + event.name() + "' is now active.");
                controller.showEventDashboard(event.id());
            }, () -> openButton.setDisable(false));
        });

        VBox content = new VBox(14, TradeStatusView.fieldValueGrid(rows), openButton);
        stepsBox.getChildren().add(TradeStatusView.card("2. Confirm & Open", content));
    }
}
