package ui.fx;

import dto.EventDto;
import dto.OptionDto;
import dto.TradeDto;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class TradeStatusView {

    private TradeStatusView() {}

    static VBox buildDashboard(ScreenController controller, EventDto event) {
        Button changeEvent = new Button("← Change event");
        changeEvent.getStyleClass().add("link-button");
        changeEvent.setOnAction(e -> controller.showEventsTable());

        VBox content = new VBox(16);
        content.getChildren().add(changeEvent);
        content.getChildren().add(infoCard(event));
        content.getChildren().add(optionsTable(event));
        content.getChildren().add(historyTable(event));
        if (event.isClosed()) {
            content.getChildren().add(closedPanel(event));
        }
        return content;
    }

    // description/commission/LMSR-b + account balance/commission-collected, merged into one card
    private static VBox infoCard(EventDto event) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Description", event.description()});
        rows.add(new String[]{"Status", event.status().toString()});
        rows.add(new String[]{"Market maker", event.marketMaker() == null ? "—" : event.marketMaker()});
        rows.add(new String[]{"Commission",
                event.commissionPercent() + "% (charged " + event.commissionType() + ")"});
        if (event.lmsrB() != null) {
            rows.add(new String[]{"LMSR liquidity", "b = " + event.lmsrB()});
        }
        rows.add(new String[]{"Account balance", String.format("%.2f", event.accountBalance())});
        rows.add(new String[]{"Commission collected", String.format("%.2f", event.commissionCollected())});
        return card("Event #" + event.id() + " · " + event.name(), fieldValueGrid(rows));
    }

    private static VBox optionsTable(EventDto event) {
        TableView<OptionDto> table = new TableView<>(FXCollections.observableArrayList(event.options()));
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(Math.min(200, 40 + event.options().size() * 32));

        TableColumn<OptionDto, String> nameCol = new TableColumn<>("Option");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));

        TableColumn<OptionDto, Double> probCol = new TableColumn<>("Probability");
        probCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().probability()).asObject());
        probCol.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar();
            private final Label label = new Label();
            private final HBox box = new HBox(8, bar, label);

            {
                bar.getStyleClass().add("probability-bar");
            }

            @Override
            protected void updateItem(Double probability, boolean empty) {
                super.updateItem(probability, empty);
                if (empty || probability == null) {
                    setGraphic(null);
                } else {
                    bar.setProgress(probability);
                    label.setText(String.format("%.1f%%", probability * 100));
                    setGraphic(box);
                }
            }
        });

        TableColumn<OptionDto, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.format("%.2f", data.getValue().probability())));

        TableColumn<OptionDto, Number> sharesCol = new TableColumn<>("Shares Bought");
        sharesCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().quantity()));

        table.getColumns().addAll(nameCol, probCol, valueCol, sharesCol);

        return card("Current Values", table);
    }

    private static VBox historyTable(EventDto event) {
        List<TradeDto> history = event.trades();
        if (history.isEmpty()) {
            Label empty = new Label("No trades yet.");
            empty.getStyleClass().add("value-label");
            return card("Trade History (0)", empty);
        }

        List<TradeDto> reversed = new ArrayList<>(history);
        Collections.reverse(reversed);

        TableView<TradeDto> table = new TableView<>(FXCollections.observableArrayList(reversed));
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(Math.min(220, 40 + history.size() * 32));

        TableColumn<TradeDto, String> buyerCol = new TableColumn<>("Buyer");
        buyerCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().buyerName()));

        TableColumn<TradeDto, String> optionCol = new TableColumn<>("Option");
        optionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().optionName()));

        TableColumn<TradeDto, Number> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().quantity()));

        TableColumn<TradeDto, String> costCol = new TableColumn<>("Shares Cost");
        costCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().sharesCost())));

        TableColumn<TradeDto, String> commissionCol = new TableColumn<>("Commission");
        commissionCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().commissionPaid())));

        TableColumn<TradeDto, String> totalCol = new TableColumn<>("Total Paid");
        totalCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().totalPaid())));

        table.getColumns().addAll(buyerCol, optionCol, qtyCol, costCol, commissionCol, totalCol);

        return card("Trade History (" + history.size() + ")", table);
    }

    private static VBox closedPanel(EventDto event) {
        String winner = event.winningOption();
        int winningShares = event.options().stream()
                .filter(option -> option.name().equals(winner))
                .mapToInt(OptionDto::quantity)
                .findFirst().orElse(0);

        boolean onClose = "on-close".equals(event.commissionType());
        double grossPayout = winningShares;
        double commissionRetained = onClose ? event.commissionCollected() : 0;
        double netPaidToWinners = grossPayout - commissionRetained;

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Winning option", winner});
        rows.add(new String[]{"Winning shares", String.valueOf(winningShares)});
        rows.add(new String[]{"Gross payout", String.format("%.2f", grossPayout)});
        rows.add(new String[]{"Commission retained", String.format("%.2f", commissionRetained)});
        rows.add(new String[]{"Net paid to winners", String.format("%.2f", netPaidToWinners)});
        return card("🏆 Event Closed", fieldValueGrid(rows));
    }

    // package-private: reused by ParticipateWizardView/CloseEventWizardView for cards and breadcrumbs
    static GridPane fieldValueGrid(List<String[]> rows) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(6);
        for (int i = 0; i < rows.size(); i++) {
            Label field = new Label(rows.get(i)[0] + ":");
            field.getStyleClass().add("field-label");
            Label value = new Label(rows.get(i)[1]);
            value.getStyleClass().add("value-label");
            grid.add(field, 0, i);
            grid.add(value, 1, i);
        }
        return grid;
    }

    static VBox card(String title, Node content) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        VBox box = new VBox(12, titleLabel, content);
        box.getStyleClass().add("card");
        return box;
    }

    static HBox breadcrumbChip(String text, Runnable onChange) {
        Label label = new Label(text);
        label.getStyleClass().add("breadcrumb-chip-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button change = new Button("Change");
        change.getStyleClass().add("link-button");
        change.setOnAction(e -> onChange.run());

        HBox chip = new HBox(label, spacer, change);
        chip.getStyleClass().add("breadcrumb-chip");
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setMaxWidth(Double.MAX_VALUE);
        return chip;
    }
}
