package ui.fx;

import dto.EventDto;
import dto.EventStatus;
import dto.EventType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

final class EventsTableView {

    private EventsTableView() {}

    // kept by the ScreenController so the chosen filters survive the live refreshes that rebuild this screen
    static final class Filters {
        final Set<EventType> types = EnumSet.allOf(EventType.class);
        final Set<EventStatus> statuses = EnumSet.allOf(EventStatus.class);
        final Set<String> commissionTypes = new HashSet<>(Set.of("on-purchase", "on-close"));

        boolean accepts(EventDto event) {
            return types.contains(event.type())
                    && statuses.contains(event.status())
                    && commissionTypes.contains(event.commissionType());
        }
    }

    static VBox build(ScreenController controller, MarketModel model) {
        Label title = new Label("All Events");
        title.getStyleClass().add("content-title");
        Label subtitle = new Label("Browse, filter, and click into any event. Updates live.");
        subtitle.getStyleClass().add("content-subtitle");

        Filters filters = controller.eventFilters;
        ObservableList<EventDto> source = FXCollections.observableArrayList(model.getEvents());
        FilteredList<EventDto> filtered = new FilteredList<>(source, filters::accepts);

        Runnable refreshPredicate = () -> filtered.setPredicate(filters::accepts);

        FlowPane typeFilters = toggleRow("Type:",
                toggle("LMSR", filters.types, EventType.LMSR, refreshPredicate),
                toggle("Order Book", filters.types, EventType.ORDER_BOOK, refreshPredicate));

        FlowPane statusFilters = toggleRow("Status:",
                toggle("Inactive", filters.statuses, EventStatus.INACTIVE, refreshPredicate),
                toggle("Active", filters.statuses, EventStatus.ACTIVE, refreshPredicate),
                toggle("Closed", filters.statuses, EventStatus.CLOSED, refreshPredicate));

        FlowPane commissionFilters = toggleRow("Commission:",
                toggle("On-purchase", filters.commissionTypes, "on-purchase", refreshPredicate),
                toggle("On-close", filters.commissionTypes, "on-close", refreshPredicate));

        SortedList<EventDto> sorted = new SortedList<>(filtered);
        TableView<EventDto> table = new TableView<>(sorted);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(Math.min(420, 60 + model.getEvents().size() * 32));
        table.setPlaceholder(new Label("No events yet. Upload an XML file to add some."));

        TableColumn<EventDto, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().id()));

        TableColumn<EventDto, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));

        TableColumn<EventDto, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().type() == EventType.LMSR ? "LMSR" : "Order Book"));

        TableColumn<EventDto, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status().toString()));

        TableColumn<EventDto, String> commissionCol = new TableColumn<>("Commission");
        commissionCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().commissionPercent() + "% " + data.getValue().commissionType()));

        TableColumn<EventDto, String> mmCol = new TableColumn<>("Market Maker");
        mmCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().marketMaker() == null ? "—" : data.getValue().marketMaker()));

        TableColumn<EventDto, String> accountCol = new TableColumn<>("Event Account");
        accountCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.format("%.2f", data.getValue().accountBalance())));

        table.getColumns().addAll(idCol, nameCol, typeCol, statusCol, commissionCol, mmCol, accountCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) controller.showEventDashboard(selected.id());
        });

        VBox filtersBox = new VBox(8, typeFilters, statusFilters, commissionFilters);
        VBox card = TradeStatusView.card("Events (" + model.getEvents().size() + ")",
                new VBox(14, filtersBox, table));

        return new VBox(16, title, subtitle, card);
    }

    private static <T> ToggleButton toggle(String label, Set<T> selected, T value, Runnable onChange) {
        ToggleButton button = new ToggleButton(label);
        button.setSelected(selected.contains(value));
        button.selectedProperty().addListener((obs, old, isSelected) -> {
            if (isSelected) selected.add(value); else selected.remove(value);
            onChange.run();
        });
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
