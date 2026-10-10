package ui.fx;

import dto.ChatMessageDto;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// live chat room: every message goes to the server and comes back to all clients through polling
final class ChatView {

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private ChatView() {}

    static VBox build(ScreenController controller, MarketModel model) {
        Label title = new Label("Chat");
        title.getStyleClass().add("content-title");
        Label subtitle = new Label("Talk with everyone connected to the market.");
        subtitle.getStyleClass().add("content-subtitle");

        ListView<ChatMessageDto> messages = new ListView<>(model.chat());
        messages.getStyleClass().add("chat-list");
        messages.setPrefHeight(420);
        messages.setPlaceholder(new Label("No messages yet. Say hello!"));
        messages.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ChatMessageDto message, boolean empty) {
                super.updateItem(message, empty);
                if (empty || message == null) {
                    setText(null);
                    return;
                }
                String who = message.sender().equals(model.username()) ? "you" : message.sender();
                setText("[" + TIME.format(Instant.ofEpochMilli(message.timestamp())) + "] " + who + ": " + message.text());
                setWrapText(true);
                setPrefWidth(0);
            }
        });

        // weak, so the long-lived chat list doesn't keep every ChatView ever opened alive;
        // the ListView's properties hold the strong reference for as long as this view exists
        ListChangeListener<ChatMessageDto> followNewest = change -> scrollToEnd(messages);
        messages.getProperties().put("followNewest", followNewest);
        model.chat().addListener(new WeakListChangeListener<>(followNewest));
        scrollToEnd(messages);

        TextField input = new TextField();
        input.setPromptText("Type a message…");
        HBox.setHgrow(input, Priority.ALWAYS);
        Button send = new Button("Send");
        send.getStyleClass().add("primary-button");

        Label error = new Label();
        error.getStyleClass().add("error-label");

        Runnable sendMessage = () -> {
            String text = input.getText() == null ? "" : input.getText().trim();
            if (text.isEmpty()) return;
            if (text.length() > MAX_MESSAGE_LENGTH) {
                error.setText("Messages are limited to " + MAX_MESSAGE_LENGTH + " characters.");
                return;
            }
            error.setText("");
            input.clear();
            controller.submit("Chat", () -> {
                controller.client().sendChat(text);
                return null;
            }, ok -> {}, () -> input.setText(text));
        };
        send.setOnAction(e -> sendMessage.run());
        input.setOnAction(e -> sendMessage.run());

        VBox card = TradeStatusView.card("Messages", new VBox(10, messages, new HBox(8, input, send), error));
        return new VBox(16, title, subtitle, card);
    }

    private static void scrollToEnd(ListView<ChatMessageDto> messages) {
        if (!messages.getItems().isEmpty()) {
            messages.scrollTo(messages.getItems().size() - 1);
        }
    }
}
