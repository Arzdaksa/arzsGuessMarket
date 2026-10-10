package ui.fx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ui.net.ServerClient;

import java.util.List;

public class GuessMarketApp extends Application {

    private Stage stage;
    private Scene scene;
    private String serverUrl;
    private ScreenController controller;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        serverUrl = initialServerUrl();

        scene = new Scene(new StackPane(), 1000, 680);
        scene.getStylesheets().add(getClass().getResource("theme.css").toExternalForm());
        stage.setScene(scene);
        showLogin(null);
        stage.show();
    }

    // first program argument, then -Dgm.server=..., then the local Tomcat default
    private String initialServerUrl() {
        List<String> args = getParameters().getRaw();
        if (!args.isEmpty() && !args.get(0).isBlank()) return args.get(0).trim();
        return System.getProperty("gm.server", ServerClient.DEFAULT_URL);
    }

    private void showLogin(String notice) {
        controller = null;
        stage.setTitle("Guess Market — Login");
        scene.setRoot(LoginView.build(serverUrl, notice, this::showMain));
    }

    private void showMain(ServerClient client, String username) {
        serverUrl = client.baseUrl();
        BorderPane root = new BorderPane();

        controller = new ScreenController(stage, root, client, username, this::showLogin);
        Sidebar sidebar = new Sidebar(controller);
        controller.attachSidebar(sidebar);

        root.setLeft(sidebar);
        controller.showWelcome();

        stage.setTitle("Guess Market — " + username);
        scene.setRoot(root);
        controller.start();
    }

    @Override
    public void stop() {
        if (controller != null) controller.shutdown();
    }
}
