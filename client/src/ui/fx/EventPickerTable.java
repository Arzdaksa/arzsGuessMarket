package ui.fx;

import dto.EventDto;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.function.Consumer;

final class EventPickerTable {

    private EventPickerTable() {}

    static TableView<EventDto> build(List<EventDto> events, Consumer<EventDto> onSelect) {
        TableView<EventDto> table = new TableView<>(FXCollections.observableArrayList(events));
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(Math.min(240, 40 + events.size() * 32));

        TableColumn<EventDto, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().id()));

        TableColumn<EventDto, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));

        TableColumn<EventDto, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status().toString()));

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
