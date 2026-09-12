package ui.fx;

import engine.Event;
import engine.Order;
import engine.OrderBook;
import engine.OrderBookConfig;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

final class OrderBookView {

    private OrderBookView() {}

    static VBox buildDashboard(ScreenController controller, Event event) {
        Button changeEvent = new Button("← Change event");
        changeEvent.getStyleClass().add("link-button");
        changeEvent.setOnAction(e -> controller.showEventsTable());

        VBox content = new VBox(16);
        content.getChildren().add(changeEvent);
        content.getChildren().add(header(event));

        OrderBook orderBook = event.getOrderBook();
        if (orderBook == null) {
            content.getChildren().add(TradeStatusView.card("Order Book", new Label("Event has not been opened yet.")));
        } else {
            content.getChildren().add(statsPanel(event, orderBook));
            content.getChildren().add(combinedBookPanel(event, orderBook));
            content.getChildren().add(participantsPanel(event, orderBook));
        }

        if (event.isClosed()) {
            content.getChildren().add(closedPanel(event));
        }

        return content;
    }

    private static VBox header(Event event) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Description", event.getDescription()});
        rows.add(new String[]{"Commission",
                event.getCommission().getCommission() + "% (charged " + event.getCommission().getType() + ")"});
        OrderBookConfig config = event.getOrderBookConfig();
        if (config != null) {
            rows.add(new String[]{"Base value (d)", String.valueOf(config.getD())});
            rows.add(new String[]{"Auto-mint", config.isAllowMint() ? "enabled" : "disabled"});
            rows.add(new String[]{"MM initial investment", String.valueOf(config.getInitial())});
        }
        return TradeStatusView.card("Event #" + event.getID() + " · " + event.getName(), TradeStatusView.fieldValueGrid(rows));
    }

    private static VBox statsPanel(Event event, OrderBook orderBook) {
        TableView<String> table = new TableView<>(FXCollections.observableArrayList(event.getOptionNames()));
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(100);

        TableColumn<String, String> optionCol = new TableColumn<>("Option");
        optionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));

        TableColumn<String, String> lastCol = new TableColumn<>("LAST");
        lastCol.setCellValueFactory(data -> new SimpleStringProperty(fmt(orderBook.getLast(data.getValue()))));

        TableColumn<String, String> bidCol = new TableColumn<>("BID");
        bidCol.setCellValueFactory(data -> new SimpleStringProperty(fmt(orderBook.getBestBid(data.getValue()))));

        TableColumn<String, String> askCol = new TableColumn<>("ASK");
        askCol.setCellValueFactory(data -> new SimpleStringProperty(fmt(orderBook.getBestAsk(data.getValue()))));

        TableColumn<String, String> midCol = new TableColumn<>("MID");
        midCol.setCellValueFactory(data -> new SimpleStringProperty(fmt(orderBook.getMid(data.getValue()))));

        TableColumn<String, String> spreadCol = new TableColumn<>("SPREAD");
        spreadCol.setCellValueFactory(data -> new SimpleStringProperty(fmt(orderBook.getSpread(data.getValue()))));

        table.getColumns().addAll(optionCol, lastCol, bidCol, askCol, midCol, spreadCol);
        return TradeStatusView.card("Live Stats", table);
    }

    // both options' bids/asks combined into one card, so the whole book fits in a single glance
    private static VBox combinedBookPanel(Event event, OrderBook orderBook) {
        List<Order> allBids = new ArrayList<>();
        List<Order> allAsks = new ArrayList<>();
        for (String optionName : event.getOptionNames()) {
            allBids.addAll(orderBook.getBids(optionName));
            allAsks.addAll(orderBook.getAsks(optionName));
        }

        VBox bidsBox = new VBox(6, new Label("Bids"), orderTable(allBids));
        VBox asksBox = new VBox(6, new Label("Asks"), orderTable(allAsks));
        HBox.setHgrow(bidsBox, Priority.ALWAYS);
        HBox.setHgrow(asksBox, Priority.ALWAYS);

        HBox row = new HBox(16, bidsBox, asksBox);
        return TradeStatusView.card("Order Book", row);
    }

    private static TableView<Order> orderTable(List<Order> orders) {
        TableView<Order> table = new TableView<>(FXCollections.observableArrayList(orders));
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(Math.min(160, 40 + orders.size() * 28));

        TableColumn<Order, String> optionCol = new TableColumn<>("Option");
        optionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOptionName()));

        TableColumn<Order, String> ownerCol = new TableColumn<>("Owner");
        ownerCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOwnerName()));

        TableColumn<Order, Number> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getQuantity()));

        TableColumn<Order, String> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().getPrice())));

        table.getColumns().addAll(optionCol, ownerCol, qtyCol, priceCol);
        return table;
    }

    private static VBox participantsPanel(Event event, OrderBook orderBook) {
        List<String> optionNames = event.getOptionNames();
        List<String> participants = new ArrayList<>(orderBook.getParticipantNames());

        if (participants.isEmpty()) {
            return TradeStatusView.card("Participants", new Label("No participants yet."));
        }

        TableView<String> table = new TableView<>(FXCollections.observableArrayList(participants));
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(Math.min(200, 40 + participants.size() * 28));

        TableColumn<String, String> nameCol = new TableColumn<>("Participant");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        table.getColumns().add(nameCol);

        for (String optionName : optionNames) {
            TableColumn<String, String> col = new TableColumn<>(optionName + " shares");
            col.setCellValueFactory(data -> new SimpleStringProperty(
                    String.valueOf(orderBook.getHoldings(data.getValue()).getOrDefault(optionName, 0))));
            table.getColumns().add(col);
        }

        return TradeStatusView.card("Participants", table);
    }

    private static VBox closedPanel(Event event) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Winning option", event.getWinningOption()});
        OrderBook orderBook = event.getOrderBook();
        if (orderBook != null) {
            int totalWinningShares = 0;
            for (String name : orderBook.getParticipantNames()) {
                totalWinningShares += orderBook.getHoldings(name).getOrDefault(event.getWinningOption(), 0);
            }
            rows.add(new String[]{"Total winning shares", String.valueOf(totalWinningShares)});
        }
        return TradeStatusView.card("🏆 Event Closed", TradeStatusView.fieldValueGrid(rows));
    }

    private static String fmt(Double value) {
        return value == null ? "—" : String.format("%.2f", value);
    }
}
