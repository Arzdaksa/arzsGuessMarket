package ui.fx;

import engine.GuessMarketEngine;
import engine.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

final class UsersListView {

    private UsersListView() {}

    static VBox build(ScreenController controller, GuessMarketEngine engine) {
        Label title = new Label("Users");
        title.getStyleClass().add("content-title");

        TableView<User> table = new TableView<>(FXCollections.observableArrayList(engine.getUsers()));
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(Math.min(360, 60 + engine.getUsers().size() * 32));

        TableColumn<User, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<User, String> balanceCol = new TableColumn<>("Balance");
        balanceCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().getBalance())));

        TableColumn<User, String> eventsCol = new TableColumn<>("Events Participated");
        eventsCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().getParticipatedEventIds().size())));

        TableColumn<User, String> mmCol = new TableColumn<>("Market Maker Of");
        mmCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getMarketMakerEventRefs().size() + " event(s)"));

        TableColumn<User, String> lockedCol = new TableColumn<>("Locked");
        lockedCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isLocked() ? "Yes" : "No"));

        table.getColumns().addAll(nameCol, balanceCol, eventsCol, mmCol, lockedCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) controller.showUserDetail(selected);
        });

        VBox card = TradeStatusView.card("Users (" + engine.getUsers().size() + ")", table);
        return new VBox(16, title, card);
    }
}
