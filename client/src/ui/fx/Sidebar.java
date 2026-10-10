package ui.fx;

import dto.UserDto;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

final class Sidebar extends VBox {

    enum NavItem { EVENTS, TRADE, MANAGE_EVENT, USERS, CHAT }

    private final Label statusLabel = new Label();
    private final Label userNameLabel = new Label();
    private final Label balanceLabel = new Label();
    private final Button eventsButton;
    private final Button tradeButton;
    private final Button manageEventButton;
    private final Button usersButton;
    private final Button chatButton;

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

        Label userLabel = new Label("LOGGED IN AS");
        userLabel.getStyleClass().add("sidebar-section-label");
        userNameLabel.getStyleClass().add("sidebar-user-name");
        balanceLabel.getStyleClass().add("sidebar-user-balance");
        Button depositButton = navButton("Add funds", e -> controller.handleDeposit());
        depositButton.getStyleClass().add("nav-button-secondary");

        Label workflowLabel = new Label("WORKFLOW");
        workflowLabel.getStyleClass().add("sidebar-section-label");

        eventsButton = navButton("Show all events", e -> controller.showEventsTable());
        tradeButton = navButton("Trade", e -> controller.showTradeChooser());

        Label mmLabel = new Label("MARKET MAKER");
        mmLabel.getStyleClass().add("sidebar-section-label");
        manageEventButton = navButton("Manage my events", e -> controller.showManageEventChooser());
        Button uploadXmlButton = navButton("Upload XML file", e -> controller.handleUploadXml());
        uploadXmlButton.getStyleClass().add("nav-button-secondary");

        Label communityLabel = new Label("COMMUNITY");
        communityLabel.getStyleClass().add("sidebar-section-label");
        usersButton = navButton("View users", e -> controller.showUsersList());
        chatButton = navButton("Chat", e -> controller.showChat());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logoutButton = navButton("Logout", e -> controller.handleLogout());
        logoutButton.getStyleClass().add("nav-button-secondary");
        Button exitButton = navButton("Exit", e -> Platform.exit());
        exitButton.getStyleClass().add("nav-button-danger");

        getChildren().addAll(header, statusLabel, userLabel, userNameLabel, balanceLabel, depositButton,
                workflowLabel, eventsButton, tradeButton,
                mmLabel, manageEventButton, uploadXmlButton,
                communityLabel, usersButton, chatButton,
                spacer, logoutButton, exitButton);
    }

    void setActive(NavItem item) {
        for (Button b : new Button[]{eventsButton, tradeButton, manageEventButton, usersButton, chatButton}) {
            b.getStyleClass().remove("nav-button-active");
        }

        Button active = item == null ? null : switch (item) {
            case EVENTS -> eventsButton;
            case TRADE -> tradeButton;
            case MANAGE_EVENT -> manageEventButton;
            case USERS -> usersButton;
            case CHAT -> chatButton;
        };
        if (active != null) active.getStyleClass().add("nav-button-active");
    }

    // called after every poll: connection state, own balance, and the unread chat count
    void refresh(MarketModel model, boolean connected, int unreadChat) {
        statusLabel.setText(connected
                ? model.getEvents().size() + " event(s) · " + model.getUsers().size() + " user(s)"
                : "Server unreachable — retrying…");

        UserDto me = model.me();
        userNameLabel.setText(model.username());
        balanceLabel.setText(me == null ? "" : "$" + String.format("%.2f", me.balance()) + (me.locked() ? " (locked)" : ""));
        chatButton.setText(unreadChat > 0 ? "Chat (" + unreadChat + " new)" : "Chat");
    }

    private static Button navButton(String label, EventHandler<ActionEvent> action) {
        Button button = new Button(label);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(action);
        return button;
    }
}
