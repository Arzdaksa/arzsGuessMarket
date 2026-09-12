package ui.fx;

import engine.Event;
import engine.EventState;
import engine.GuessMarketEngine;
import engine.MethodType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.EnumSet;
import java.util.Set;

final class EventsTableView {

    private EventsTableView() {}

    static VBox build(ScreenController controller, GuessMarketEngine engine) {
        Label title = new Label("All Events");
        title.getStyleClass().add("content-title");
        Label subtitle = new Label("Browse, filter, and click into any event.");
        subtitle.getStyleClass().add("content-subtitle");

        ObservableList<Event> source = FXCollections.observableArrayList(engine.getEvents());
        FilteredList<Event> filtered = new FilteredList<>(source, e -> true);

        Set<MethodType> selectedTypes = EnumSet.allOf(MethodType.class);
        Set<EventState> selectedStates = EnumSet.allOf(EventState.class);
        Set<String> selectedCommissionTypes = new java.util.HashSet<>(Set.of("on-purchase", "on-close"));

        Runnable refreshPredicate = () -> filtered.setPredicate(e ->
                selectedTypes.contains(e.getEventType())
                        && selectedStates.contains(e.getState())
                        && selectedCommissionTypes.contains(e.getCommission().getType()));

        FlowPane typeFilters = toggleRow("Type:",
                toggle("LMSR", true, sel -> toggleSet(selectedTypes, MethodType.LMSR, sel, refreshPredicate)),
                toggle("Order Book", true, sel -> toggleSet(selectedTypes, MethodType.ORDER_BOOK, sel, refreshPredicate)));

        FlowPane statusFilters = toggleRow("Status:",
                toggle("Inactive", true, sel -> toggleSet(selectedStates, EventState.INACTIVE, sel, refreshPredicate)),
                toggle("Active", true, sel -> toggleSet(selectedStates, EventState.ACTIVE, sel, refreshPredicate)),
                toggle("Closed", true, sel -> toggleSet(selectedStates, EventState.CLOSED, sel, refreshPredicate)));

        FlowPane commissionFilters = toggleRow("Commission:",
                toggle("On-purchase", true, sel -> toggleSet(selectedCommissionTypes, "on-purchase", sel, refreshPredicate)),
                toggle("On-close", true, sel -> toggleSet(selectedCommissionTypes, "on-close", sel, refreshPredicate)));

        TableView<Event> table = new TableView<>(filtered);
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(Math.min(420, 60 + engine.getEvents().size() * 32));

        TableColumn<Event, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getID()));

        TableColumn<Event, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Event, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getEventType() == MethodType.LMSR ? "LMSR" : "Order Book"));

        TableColumn<Event, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getState().toString()));

        TableColumn<Event, String> commissionCol = new TableColumn<>("Commission");
        commissionCol.setCellValueFactory(data -> {
            var commission = data.getValue().getCommission();
            return new SimpleStringProperty(commission.getCommission() + "% " + commission.getType());
        });

        TableColumn<Event, String> mmCol = new TableColumn<>("Market Maker");
        mmCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getMarketMaker() == null ? "—" : data.getValue().getMarketMaker().getName()));

        TableColumn<Event, String> accountCol = new TableColumn<>("Event Account");
        accountCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.format("%.2f", data.getValue().getAccountBalance())));

        table.getColumns().addAll(idCol, nameCol, typeCol, statusCol, commissionCol, mmCol, accountCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) controller.showEventDashboard(selected);
        });

        VBox filtersBox = new VBox(8, typeFilters, statusFilters, commissionFilters);
        VBox card = TradeStatusView.card("Events (" + engine.getEvents().size() + ")",
                new VBox(14, filtersBox, table));

        return new VBox(16, title, subtitle, card);
    }

    private static <T> void toggleSet(Set<T> set, T value, boolean selected, Runnable onChange) {
        if (selected) set.add(value); else set.remove(value);
        onChange.run();
    }

    private static ToggleButton toggle(String label, boolean selectedByDefault, java.util.function.Consumer<Boolean> onToggle) {
        ToggleButton button = new ToggleButton(label);
        button.setSelected(selectedByDefault);
        button.selectedProperty().addListener((obs, old, val) -> onToggle.accept(val));
        return button;
    }

    private static FlowPane toggleRow(String label, ToggleButton... buttons) {
        Label rowLabel = new Label(label);
        rowLabel.getStyleClass().add("field-label");
        FlowPane pane = new FlowPane(8, 8);
        pane.getChildren().add(rowLabel);
        pane.getChildren().addAll(buttons);
        return pane;
    }
}
