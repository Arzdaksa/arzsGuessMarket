package ui.fx;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ui.net.ServerClient;

import java.util.function.BiConsumer;

// the first screen: a unique user name is all it takes to join; the server refuses a name already in use
final class LoginView {

    private LoginView() {}

    static StackPane build(String serverUrl, String notice, BiConsumer<ServerClient, String> onLoggedIn) {
        Label title = new Label("Guess Market");
        title.getStyleClass().add("content-title");
        Label subtitle = new Label("Choose a user name to join the market.");
        subtitle.getStyleClass().add("content-subtitle");

        Label nameLabel = new Label("User name");
        nameLabel.getStyleClass().add("field-label");
        TextField nameField = new TextField();
        nameField.setPromptText("e.g. alice");

        Label serverLabel = new Label("Server");
        serverLabel.getStyleClass().add("field-label");
        TextField serverField = new TextField(serverUrl);

        Label message = new Label(notice == null ? "" : notice);
        message.getStyleClass().add("error-label");
        message.setWrapText(true);

        Button loginButton = new Button("Login");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setDefaultButton(true);
        loginButton.setMaxWidth(Double.MAX_VALUE);

        loginButton.setOnAction(e -> {
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (name.isEmpty()) {
                message.setText("Enter a user name.");
                return;
            }
            ServerClient client;
            try {
                client = new ServerClient(serverField.getText());
            } catch (IllegalArgumentException ex) {
                message.setText("Enter a valid server address, e.g. " + ServerClient.DEFAULT_URL);
                return;
            }

            loginButton.setDisable(true);
            message.setText("");
            Thread thread = new Thread(() -> {
                try {
                    String accepted = client.login(name);
                    Platform.runLater(() -> onLoggedIn.accept(client, accepted));
                } catch (RuntimeException ex) {
                    Platform.runLater(() -> {
                        message.setText(ex.getMessage());
                        loginButton.setDisable(false);
                    });
                }
            }, "guess-market-login");
            thread.setDaemon(true);
            thread.start();
        });

        VBox card = new VBox(10, title, subtitle, nameLabel, nameField, serverLabel, serverField, message, loginButton);
        card.getStyleClass().addAll("card", "login-card");
        card.setMaxWidth(360);
        card.setMaxHeight(VBox.USE_PREF_SIZE);

        StackPane pane = new StackPane(card);
        pane.setAlignment(Pos.CENTER);
        pane.getStyleClass().add("login-pane");
        Platform.runLater(nameField::requestFocus);
        return pane;
    }
}
