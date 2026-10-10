package ui.fx;

import dto.UserDto;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

final class UsersListView {

    private UsersListView() {}

    static VBox build(ScreenController controller, MarketModel model) {
        Label title = new Label("Users");
        title.getStyleClass().add("content-title");
        Label subtitle = new Label("Click a user to see their balance, holdings, and history.");
        subtitle.getStyleClass().add("content-subtitle");

        TableView<UserDto> table = new TableView<>(FXCollections.observableArrayList(model.getUsers()));
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(Math.min(360, 60 + model.getUsers().size() * 32));

        TableColumn<UserDto, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));

        TableColumn<UserDto, String> onlineCol = new TableColumn<>("Connected");
        onlineCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().online() ? "Yes" : "No"));

        TableColumn<UserDto, String> balanceCol = new TableColumn<>("Balance");
        balanceCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().balance())));

        TableColumn<UserDto, String> eventsCol = new TableColumn<>("Events Participated");
        eventsCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().participatedEventIds().size())));

        TableColumn<UserDto, String> mmCol = new TableColumn<>("Market Maker Of");
        mmCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().marketMakerCount() + " event(s)"));

        TableColumn<UserDto, String> lockedCol = new TableColumn<>("Locked");
        lockedCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().locked() ? "Yes" : "No"));

        table.getColumns().addAll(nameCol, onlineCol, balanceCol, eventsCol, mmCol, lockedCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) controller.showUserDetail(selected.name());
        });

        VBox card = TradeStatusView.card("Users (" + model.getUsers().size() + ")", table);
        return new VBox(16, title, subtitle, card);
    }
}
