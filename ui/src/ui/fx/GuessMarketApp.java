package ui.fx;

import engine.GuessMarketEngine;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class GuessMarketApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        GuessMarketEngine engine = new GuessMarketEngine();

        ScreenController controller = new ScreenController(primaryStage, root, engine);
        Sidebar sidebar = new Sidebar(controller);
        controller.attachSidebar(sidebar);

        root.setLeft(sidebar);
        controller.showWelcome();

        Scene scene = new Scene(root, 1000, 680);
        scene.getStylesheets().add(getClass().getResource("theme.css").toExternalForm());

        primaryStage.setTitle("Guess Market");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
