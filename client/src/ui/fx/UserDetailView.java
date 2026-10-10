package ui.fx;

import dto.EventDto;
import dto.EventType;
import dto.FillDto;
import dto.OrderBookDto;
import dto.TradeDto;
import dto.UserDto;
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

    static VBox build(ScreenController controller, MarketModel model, UserDto user) {
        Button back = new Button("← Back to users");
        back.getStyleClass().add("link-button");
        back.setOnAction(e -> controller.showUsersList());

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Name", user.name()});
        rows.add(new String[]{"Connected", user.online() ? "Yes" : "No"});
        rows.add(new String[]{"Balance", String.format("%.2f", user.balance())});
        rows.add(new String[]{"Locked", user.locked() ? "Yes" : "No"});
        VBox header = TradeStatusView.card("User Detail", TradeStatusView.fieldValueGrid(rows));

        VBox content = new VBox(16, back, header);

        List<EventDto> participated = model.getEvents().stream()
                .filter(e -> user.participatedEventIds().contains(e.id())
                        || (e.orderBook() != null && e.orderBook().holdings().containsKey(user.name())))
                .toList();

        if (participated.isEmpty()) {
            content.getChildren().add(TradeStatusView.card("Participated Events", new Label("No events participated in yet.")));
        } else {
            for (EventDto event : participated) {
                content.getChildren().add(eventBreakdown(controller, event, user));
            }
        }

        return content;
    }

    private static VBox eventBreakdown(ScreenController controller, EventDto event, UserDto user) {
        Button viewEvent = new Button("View event →");
        viewEvent.getStyleClass().add("link-button");
        viewEvent.setOnAction(e -> controller.showEventDashboard(event.id()));

        VBox body = (event.type() == EventType.LMSR)
                ? lmsrBreakdown(event, user)
                : orderBookBreakdown(event, user);

        VBox content = new VBox(10, viewEvent, body);
        return TradeStatusView.card(EventFormat.describe(event), content);
    }

    private static VBox lmsrBreakdown(EventDto event, UserDto user) {
        List<TradeDto> trades = new ArrayList<>(event.trades().stream()
                .filter(trade -> trade.buyerName().equals(user.name()))
                .toList());
        Collections.reverse(trades);

        VBox box = new VBox(10);
        if (trades.isEmpty()) {
            box.getChildren().add(new Label("No trades yet."));
        } else {
            TableView<TradeDto> table = new TableView<>(FXCollections.observableArrayList(trades));
            table.getStyleClass().add("data-table");
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            table.setPrefHeight(Math.min(180, 40 + trades.size() * 28));

            TableColumn<TradeDto, String> optionCol = new TableColumn<>("Option");
            optionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().optionName()));
            TableColumn<TradeDto, String> qtyCol = new TableColumn<>("Qty");
            qtyCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().quantity())));
            TableColumn<TradeDto, String> priceCol = new TableColumn<>("Price Paid");
            priceCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().totalPaid())));
            table.getColumns().addAll(optionCol, qtyCol, priceCol);
            box.getChildren().add(table);
        }

        double totalCommission = trades.stream().mapToDouble(TradeDto::commissionPaid).sum();
        box.getChildren().add(new Label("Total commission paid: " + String.format("%.2f", totalCommission)));

        if (event.isClosed()) {
            Map<String, Integer> finalShares = new HashMap<>();
            for (TradeDto t : trades) {
                finalShares.merge(t.optionName(), t.quantity(), Integer::sum);
            }
            box.getChildren().add(new Label("Final shares: " + finalShares));
            box.getChildren().add(new Label("Winning option: " + event.winningOption()));
        }

        return box;
    }

    // P/L includes the market maker's initial funding cost, which isn't recorded as a fill
    // but is a real debit against their balance when they open the event
    private static VBox orderBookBreakdown(EventDto event, UserDto user) {
        VBox box = new VBox(10);
        OrderBookDto orderBook = event.orderBook();
        if (!orderBook.opened()) {
            box.getChildren().add(new Label("Event has not been opened yet."));
            return box;
        }

        Map<String, Integer> holdings = orderBook.holdingsOf(user.name());
        box.getChildren().add(new Label("Holdings: " + (holdings.isEmpty() ? "none" : holdings.toString())));

        double tradingAmountPaid = 0;
        double commissionPaid = 0;
        for (FillDto fill : orderBook.fills()) {
            if (fill.buyerName().equals(user.name())) {
                tradingAmountPaid += fill.notional();
                commissionPaid += fill.commission();
            }
        }
        double openingCost = user.name().equals(event.marketMaker()) ? event.openCost() : 0;
        double amountPaid = tradingAmountPaid + openingCost;

        box.getChildren().add(new Label("Amount paid (trading activity): " + String.format("%.2f", tradingAmountPaid)));
        if (openingCost > 0) {
            box.getChildren().add(new Label("Market opening cost (as market maker): " + String.format("%.2f", openingCost)));
        }
        box.getChildren().add(new Label("Commission paid: " + String.format("%.2f", commissionPaid)));

        if (event.isClosed()) {
            int winningShares = holdings.getOrDefault(event.winningOption(), 0);
            double payout = winningShares * (double) orderBook.d();
            double pnl = payout - amountPaid;
            box.getChildren().add(new Label("Winning option: " + event.winningOption()));
            box.getChildren().add(new Label("Realized P/L: " + String.format("%.2f", pnl)));
        }

        return box;
    }
}
