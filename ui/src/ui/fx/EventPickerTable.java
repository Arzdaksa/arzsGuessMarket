package ui.fx;

import engine.Event;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.function.Consumer;

// shared row-click selection tables used by the trade-status/participate/close-event screens
final class EventPickerTable {

    private EventPickerTable() {}

    static TableView<Event> build(List<Event> events, Consumer<Event> onSelect) {
        TableView<Event> table = new TableView<>(FXCollections.observableArrayList(events));
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(Math.min(240, 40 + events.size() * 32));

        TableColumn<Event, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getID()));

        TableColumn<Event, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Event, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isClosed() ? "closed" : "open"));

        table.getColumns().addAll(idCol, nameCol, statusCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) onSelect.accept(selected);
        });

        return table;
    }

    static TableView<String> buildOptions(List<String> optionNames, Consumer<String> onSelect) {
        TableView<String> table = new TableView<>(FXCollections.observableArrayList(optionNames));
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(Math.min(200, 40 + optionNames.size() * 32));

        TableColumn<String, String> optionCol = new TableColumn<>("Option");
        optionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        table.getColumns().add(optionCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) onSelect.accept(selected);
        });

        return table;
    }
}
