package ui.fx;

import engine.Event;
import engine.GuessMarketEngine;
import engine.MethodType;
import engine.OrderBookTrade;
import engine.Trade;
import engine.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class UserDetailView {

    private UserDetailView() {}

    static VBox build(ScreenController controller, User user) {
        Button back = new Button("← Back to users");
        back.getStyleClass().add("link-button");
        back.setOnAction(e -> controller.showUsersList());

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Name", user.getName()});
        rows.add(new String[]{"Balance", String.format("%.2f", user.getBalance())});
        rows.add(new String[]{"Locked", user.isLocked() ? "Yes" : "No"});
        VBox header = TradeStatusView.card("User Detail", TradeStatusView.fieldValueGrid(rows));

        VBox content = new VBox(16, back, header);

        GuessMarketEngine engine = controller.engine;
        List<Event> participated = engine.getEvents().stream()
                .filter(e -> user.getParticipatedEventIds().contains(e.getID())
                        || (e.getOrderBook() != null && e.getOrderBook().getParticipantNames().contains(user.getName())))
                .toList();

        if (participated.isEmpty()) {
            content.getChildren().add(TradeStatusView.card("Participated Events", new Label("No events participated in yet.")));
        } else {
            for (Event event : participated) {
                content.getChildren().add(eventBreakdown(controller, event, user));
            }
        }

        return content;
    }

    private static VBox eventBreakdown(ScreenController controller, Event event, User user) {
        Button viewEvent = new Button("View event →");
        viewEvent.getStyleClass().add("link-button");
        viewEvent.setOnAction(e -> controller.showEventDashboard(event));

        VBox body = (event.getEventType() == MethodType.LMSR)
                ? lmsrBreakdown(event, user)
                : orderBookBreakdown(event, user);

        VBox content = new VBox(10, viewEvent, body);
        return TradeStatusView.card(EventFormat.describe(event), content);
    }

    private static VBox lmsrBreakdown(Event event, User user) {
        List<Trade> trades = new ArrayList<>(event.getTradeHistoryForUser(user.getName()));
        Collections.reverse(trades);

        VBox box = new VBox(10);
        if (trades.isEmpty()) {
            box.getChildren().add(new Label("No trades yet."));
        } else {
            TableView<Trade> table = new TableView<>(FXCollections.observableArrayList(trades));
            table.getStyleClass().add("data-table");
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            table.setPrefHeight(Math.min(180, 40 + trades.size() * 28));

            TableColumn<Trade, String> optionCol = new TableColumn<>("Option");
            optionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().optionName()));
            TableColumn<Trade, String> qtyCol = new TableColumn<>("Qty");
            qtyCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().quantity())));
            TableColumn<Trade, String> priceCol = new TableColumn<>("Price Paid");
            priceCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().totalPaid())));
            table.getColumns().addAll(optionCol, qtyCol, priceCol);
            box.getChildren().add(table);
        }

        double totalCommission = trades.stream().mapToDouble(Trade::commissionPaid).sum();
        box.getChildren().add(new Label("Total commission paid: " + String.format("%.2f", totalCommission)));

        if (event.isClosed()) {
            Map<String, Integer> finalShares = new HashMap<>();
            for (Trade t : trades) {
                finalShares.merge(t.optionName(), t.quantity(), Integer::sum);
            }
            box.getChildren().add(new Label("Final shares: " + finalShares));
            box.getChildren().add(new Label("Winning option: " + event.getWinningOption()));
        }

        return box;
    }

    private static VBox orderBookBreakdown(Event event, User user) {
        VBox box = new VBox(10);
        if (event.getOrderBook() == null) {
            box.getChildren().add(new Label("Event has not been opened yet."));
            return box;
        }

        Map<String, Integer> holdings = event.getOrderBook().getHoldings(user.getName());
        box.getChildren().add(new Label("Holdings: " + (holdings.isEmpty() ? "none" : holdings.toString())));

        double amountPaid = 0;
        double commissionPaid = 0;
        for (OrderBookTrade fill : event.getOrderBook().getFills()) {
            if (fill.buyerName().equals(user.getName())) {
                amountPaid += fill.notional();
                commissionPaid += fill.commission();
            }
        }
        box.getChildren().add(new Label("Amount paid (trading activity): " + String.format("%.2f", amountPaid)));
        box.getChildren().add(new Label("Commission paid: " + String.format("%.2f", commissionPaid)));

        if (event.isClosed()) {
            int winningShares = holdings.getOrDefault(event.getWinningOption(), 0);
            double payout = winningShares * (double) event.getOrderBookConfig().getD();
            double pnl = payout - amountPaid;
            box.getChildren().add(new Label("Winning option: " + event.getWinningOption()));
            box.getChildren().add(new Label("Realized P/L: " + String.format("%.2f", pnl)));
        }

        return box;
    }
}
