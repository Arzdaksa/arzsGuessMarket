package ui.fx;

import engine.GuessMarketEngine;
import engine.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

// persistent left navigation, built once and never rebuilt on navigation
final class Sidebar extends VBox {

    enum NavItem { EVENTS, TRADE_STATUS, PARTICIPATE, SUBMIT_ORDER, OPEN_EVENT, CLOSE_EVENT, USERS }

    private final Label statusLabel = new Label();
    private final ComboBox<User> userCombo = new ComboBox<>();
    private final Button eventsButton;
    private final Button tradeStatusButton;
    private final Button participateButton;
    private final Button submitOrderButton;
    private final Button openEventButton;
    private final Button closeEventButton;
    private final Button usersButton;

    Sidebar(ScreenController controller) {
        getStyleClass().add("sidebar");

        Label title = new Label("Guess Market");
        title.getStyleClass().add("sidebar-title");
        Label subtitle = new Label("Prediction Markets");
        subtitle.getStyleClass().add("sidebar-subtitle");
        VBox header = new VBox(2, title, subtitle);
        header.getStyleClass().add("sidebar-header");
        header.setOnMouseClicked(e -> controller.showWelcome());

        statusLabel.getStyleClass().add("sidebar-status");

        Label userLabel = new Label("CURRENT USER");
        userLabel.getStyleClass().add("sidebar-section-label");
        userCombo.getStyleClass().add("user-switcher");
        userCombo.setPromptText("Select a user…");
        userCombo.setMaxWidth(Double.MAX_VALUE);
        userCombo.setConverter(FormatConverters.userConverter());
        userCombo.valueProperty().addListener((obs, old, val) -> controller.setCurrentUser(val));

        Label workflowLabel = new Label("WORKFLOW");
        workflowLabel.getStyleClass().add("sidebar-section-label");

        eventsButton = navButton("Show all events", e -> controller.showEventsTable());
        tradeStatusButton = navButton("Show event trade status", e -> controller.showTradeStatusPicker());
        participateButton = navButton("Participate in event", e -> controller.showParticipateWizard());
        submitOrderButton = navButton("Submit order (Order Book)", e -> controller.showSubmitOrderWizard());

        Label mmLabel = new Label("MARKET MAKER");
        mmLabel.getStyleClass().add("sidebar-section-label");
        openEventButton = navButton("Open event", e -> controller.showOpenEventWizard());
        closeEventButton = navButton("Close event", e -> controller.showCloseEventWizard());

        Label accountsLabel = new Label("ACCOUNTS");
        accountsLabel.getStyleClass().add("sidebar-section-label");
        usersButton = navButton("View users", e -> controller.showUsersList());

        Label dataLabel = new Label("DATA");
        dataLabel.getStyleClass().add("sidebar-section-label");

        Button loadXmlButton = navButton("Load XML file", e -> controller.handleLoadXml());
        loadXmlButton.getStyleClass().add("nav-button-secondary");
        Button saveStateButton = navButton("Save system state (Bonus)", e -> controller.handleSaveState());
        saveStateButton.getStyleClass().add("nav-button-secondary");
        Button loadStateButton = navButton("Load system state (Bonus)", e -> controller.handleLoadState());
        loadStateButton.getStyleClass().add("nav-button-secondary");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button exitButton = navButton("Exit", e -> Platform.exit());
        exitButton.getStyleClass().add("nav-button-danger");

        getChildren().addAll(header, statusLabel, userLabel, userCombo,
                workflowLabel, eventsButton, tradeStatusButton, participateButton, submitOrderButton,
                mmLabel, openEventButton, closeEventButton,
                accountsLabel, usersButton,
                dataLabel, loadXmlButton, saveStateButton, loadStateButton,
                spacer, exitButton);
    }

    void setActive(NavItem item) {
        for (Button b : new Button[]{eventsButton, tradeStatusButton, participateButton, submitOrderButton,
                openEventButton, closeEventButton, usersButton}) {
            b.getStyleClass().remove("nav-button-active");
        }

        Button active = item == null ? null : switch (item) {
            case EVENTS -> eventsButton;
            case TRADE_STATUS -> tradeStatusButton;
            case PARTICIPATE -> participateButton;
            case SUBMIT_ORDER -> submitOrderButton;
            case OPEN_EVENT -> openEventButton;
            case CLOSE_EVENT -> closeEventButton;
            case USERS -> usersButton;
        };
        if (active != null) active.getStyleClass().add("nav-button-active");
    }

    void refreshStatus(GuessMarketEngine engine) {
        statusLabel.setText(engine.isLoaded
                ? engine.getEvents().size() + " event(s) loaded"
                : "No data loaded");
    }

    // repopulates the current-user combo after a (re)load; clears the current selection
    void refreshUsers(GuessMarketEngine engine) {
        userCombo.setItems(FXCollections.observableArrayList(engine.getUsers()));
        userCombo.setValue(null);
    }

    private static Button navButton(String label, EventHandler<ActionEvent> action) {
        Button button = new Button(label);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(action);
        return button;
    }
}
