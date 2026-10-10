package ui.fx;

import dto.EventDto;
import dto.OptionStatsDto;
import dto.OrderBookDto;
import dto.OrderDto;
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
import java.util.function.Function;

final class OrderBookView {

    private OrderBookView() {}

    static VBox buildDashboard(ScreenController controller, EventDto event) {
        Button changeEvent = new Button("← Change event");
        changeEvent.getStyleClass().add("link-button");
        changeEvent.setOnAction(e -> controller.showEventsTable());

        VBox content = new VBox(16);
        content.getChildren().add(changeEvent);
        content.getChildren().add(header(event));

        OrderBookDto orderBook = event.orderBook();
        if (!orderBook.opened()) {
            content.getChildren().add(TradeStatusView.card("Order Book", new Label("Event has not been opened yet.")));
        } else {
            content.getChildren().add(statsPanel(orderBook));
            content.getChildren().add(combinedBookPanel(orderBook));
            content.getChildren().add(participantsPanel(event, orderBook));
        }

        if (event.isClosed()) {
            content.getChildren().add(closedPanel(event));
        }

        return content;
    }

    private static VBox header(EventDto event) {
        OrderBookDto config = event.orderBook();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Description", event.description()});
        rows.add(new String[]{"Status", event.status().toString()});
        rows.add(new String[]{"Market maker", event.marketMaker() == null ? "—" : event.marketMaker()});
        rows.add(new String[]{"Commission",
                event.commissionPercent() + "% (charged " + event.commissionType() + ")"});
        rows.add(new String[]{"Base value (d)", String.valueOf(config.d())});
        rows.add(new String[]{"Auto-mint", config.allowMint() ? "enabled" : "disabled"});
        rows.add(new String[]{"MM initial investment", String.valueOf(config.initial())});
        return TradeStatusView.card("Event #" + event.id() + " · " + event.name(), TradeStatusView.fieldValueGrid(rows));
    }

    private static VBox statsPanel(OrderBookDto orderBook) {
        TableView<OptionStatsDto> table = new TableView<>(FXCollections.observableArrayList(orderBook.stats()));
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(100);

        TableColumn<OptionStatsDto, String> optionCol = new TableColumn<>("Option");
        optionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().optionName()));

        table.getColumns().add(optionCol);
        table.getColumns().add(statColumn("LAST", OptionStatsDto::last));
        table.getColumns().add(statColumn("BID", OptionStatsDto::bid));
        table.getColumns().add(statColumn("ASK", OptionStatsDto::ask));
        table.getColumns().add(statColumn("MID", OptionStatsDto::mid));
        table.getColumns().add(statColumn("SPREAD", OptionStatsDto::spread));
        return TradeStatusView.card("Live Stats", table);
    }

    private static TableColumn<OptionStatsDto, String> statColumn(String title, Function<OptionStatsDto, Double> value) {
        TableColumn<OptionStatsDto, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(fmt(value.apply(data.getValue()))));
        return column;
    }

    // both options' bids/asks combined into one card, so the whole book fits in a single glance
    private static VBox combinedBookPanel(OrderBookDto orderBook) {
        VBox bidsBox = new VBox(6, new Label("Bids"), orderTable(orderBook.bids()));
        VBox asksBox = new VBox(6, new Label("Asks"), orderTable(orderBook.asks()));
        HBox.setHgrow(bidsBox, Priority.ALWAYS);
        HBox.setHgrow(asksBox, Priority.ALWAYS);

        HBox row = new HBox(16, bidsBox, asksBox);
        return TradeStatusView.card("Order Book", row);
    }

    private static TableView<OrderDto> orderTable(List<OrderDto> orders) {
        TableView<OrderDto> table = new TableView<>(FXCollections.observableArrayList(orders));
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(Math.min(160, 40 + orders.size() * 28));

        TableColumn<OrderDto, String> optionCol = new TableColumn<>("Option");
        optionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().optionName()));

        TableColumn<OrderDto, String> ownerCol = new TableColumn<>("Owner");
        ownerCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().ownerName()));

        TableColumn<OrderDto, Number> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().quantity()));

        TableColumn<OrderDto, String> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().price())));

        table.getColumns().addAll(optionCol, ownerCol, qtyCol, priceCol);
        return table;
    }

    private static VBox participantsPanel(EventDto event, OrderBookDto orderBook) {
        List<String> participants = new ArrayList<>(orderBook.holdings().keySet());

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

        for (String optionName : event.optionNames()) {
            TableColumn<String, String> col = new TableColumn<>(optionName + " shares");
            col.setCellValueFactory(data -> new SimpleStringProperty(
                    String.valueOf(orderBook.holdingsOf(data.getValue()).getOrDefault(optionName, 0))));
            table.getColumns().add(col);
        }

        return TradeStatusView.card("Participants", table);
    }

    private static VBox closedPanel(EventDto event) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Winning option", event.winningOption()});
        OrderBookDto orderBook = event.orderBook();
        if (orderBook.opened()) {
            int totalWinningShares = 0;
            for (String name : orderBook.holdings().keySet()) {
                totalWinningShares += orderBook.holdingsOf(name).getOrDefault(event.winningOption(), 0);
            }
            rows.add(new String[]{"Total winning shares", String.valueOf(totalWinningShares)});
        }
        return TradeStatusView.card("🏆 Event Closed", TradeStatusView.fieldValueGrid(rows));
    }

    private static String fmt(Double value) {
        return value == null ? "—" : String.format("%.2f", value);
    }
}
