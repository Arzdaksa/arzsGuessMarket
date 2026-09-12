package ui.fx;

import engine.Event;
import engine.GuessMarketEngine;
import engine.LoadResult;
import engine.MethodType;
import engine.OrderSubmissionResult;
import engine.Trade;
import engine.User;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;

// navigation hub: swaps the BorderPane center per screen and owns the file-dialog actions
final class ScreenController {

    private final Stage stage;
    private final BorderPane root;
    final GuessMarketEngine engine;
    private Sidebar sidebar;
    private User currentUser;

    ScreenController(Stage stage, BorderPane root, GuessMarketEngine engine) {
        this.stage = stage;
        this.root = root;
        this.engine = engine;
    }

    void attachSidebar(Sidebar sidebar) {
        this.sidebar = sidebar;
        sidebar.refreshStatus(engine);
        sidebar.refreshUsers(engine);
    }

    User getCurrentUser() {
        return currentUser;
    }

    void setCurrentUser(User user) {
        this.currentUser = user;
    }

    boolean requireCurrentUser() {
        if (currentUser == null) {
            showInfo("No user selected", "Select a current user in the sidebar first.");
            return false;
        }
        return true;
    }

    void showWelcome() {
        sidebar.setActive(null);
        setContent(WelcomeView.build(engine));
    }

    void showEventsTable() {
        if (!requireLoaded()) return;
        sidebar.setActive(Sidebar.NavItem.EVENTS);
        setContent(EventsTableView.build(this, engine));
    }

    void showTradeStatusPicker() {
        if (!requireLoaded()) return;
        if (engine.getEvents().isEmpty()) {
            showInfo("No events", "No events loaded yet.");
            return;
        }
        sidebar.setActive(Sidebar.NavItem.TRADE_STATUS);
        setContent(TradeStatusView.buildPicker(this, engine.getEvents()));
    }

    void showTradeStatusDashboard(Event event) {
        sidebar.setActive(Sidebar.NavItem.TRADE_STATUS);
        setContent(TradeStatusView.buildDashboard(this, event));
    }

    void showOrderBookDashboard(Event event) {
        sidebar.setActive(Sidebar.NavItem.TRADE_STATUS);
        setContent(OrderBookView.buildDashboard(this, event));
    }

    // routes to the correct dashboard type — used by row-click handlers that don't know the type ahead of time
    void showEventDashboard(Event event) {
        if (event.getEventType() == MethodType.LMSR) {
            showTradeStatusDashboard(event);
        } else {
            showOrderBookDashboard(event);
        }
    }

    void showParticipateWizard() {
        if (!requireLoaded()) return;
        boolean anyLmsrActive = engine.getActiveEvents().stream()
                .anyMatch(e -> e.getEventType() == MethodType.LMSR);
        if (!anyLmsrActive) {
            showInfo("No active events", "No active LMSR events to trade in.");
            return;
        }
        sidebar.setActive(Sidebar.NavItem.PARTICIPATE);
        setContent(ParticipateWizardView.build(this, engine));
    }

    void showTradeReceipt(Trade trade, Event event) {
        sidebar.setActive(Sidebar.NavItem.TRADE_STATUS);
        VBox receipt = ParticipateWizardView.buildReceipt(trade);
        VBox dashboard = TradeStatusView.buildDashboard(this, event);
        dashboard.getChildren().add(0, receipt);
        setContent(dashboard);
    }

    void showSubmitOrderWizard() {
        if (!requireLoaded()) return;
        boolean anyObActive = engine.getActiveEvents().stream()
                .anyMatch(e -> e.getEventType() == MethodType.ORDER_BOOK);
        if (!anyObActive) {
            showInfo("No active events", "No active Order Book events to trade in.");
            return;
        }
        sidebar.setActive(Sidebar.NavItem.SUBMIT_ORDER);
        setContent(SubmitOrderWizardView.build(this, engine));
    }

    void showOrderReceipt(OrderSubmissionResult result, Event event) {
        sidebar.setActive(Sidebar.NavItem.TRADE_STATUS);
        VBox receipt = SubmitOrderWizardView.buildReceipt(result);
        VBox dashboard = OrderBookView.buildDashboard(this, event);
        dashboard.getChildren().add(0, receipt);
        setContent(dashboard);
    }

    void showOpenEventWizard() {
        if (!requireLoaded()) return;
        if (engine.getInactiveEvents().isEmpty()) {
            showInfo("No inactive events", "There are no inactive events to open.");
            return;
        }
        sidebar.setActive(Sidebar.NavItem.OPEN_EVENT);
        setContent(OpenEventWizardView.build(this, engine));
    }

    void showCloseEventWizard() {
        if (!requireLoaded()) return;
        if (engine.getActiveEvents().isEmpty()) {
            showInfo("No active events", "No active events to close.");
            return;
        }
        sidebar.setActive(Sidebar.NavItem.CLOSE_EVENT);
        setContent(CloseEventWizardView.build(this, engine));
    }

    void showUsersList() {
        if (!requireLoaded()) return;
        sidebar.setActive(Sidebar.NavItem.USERS);
        setContent(UsersListView.build(this, engine));
    }

    void showUserDetail(User user) {
        sidebar.setActive(Sidebar.NavItem.USERS);
        setContent(UserDetailView.build(this, user));
    }

    void handleLoadXml() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load XML file");
        File defaultDir = new File("engine/xml_files_dir");
        if (defaultDir.isDirectory()) {
            chooser.setInitialDirectory(defaultDir);
        }
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files", "*.xml"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        Task<LoadResult> loadTask = new Task<>() {
            @Override
            protected LoadResult call() {
                LoadResult result = engine.loadDataFromXML(file.getAbsolutePath());
                // artificial delay so the progress bar is visibly demonstrated, per spec
                for (int i = 0; i <= 20; i++) {
                    updateProgress(i, 20);
                    try {
                        Thread.sleep(75);
                    } catch (InterruptedException ignored) {
                    }
                }
                return result;
            }
        };

        ProgressDialog dialog = new ProgressDialog(stage, "Loading market data…");
        dialog.progressBar().progressProperty().bind(loadTask.progressProperty());

        loadTask.setOnSucceeded(e -> {
            dialog.close();
            LoadResult result = loadTask.getValue();
            currentUser = null;
            sidebar.refreshUsers(engine);
            sidebar.refreshStatus(engine);
            if (result.success()) {
                showInfo("Load XML file", "File loaded successfully.\n" + engine.getEvents().size() + " event(s) available.");
                showEventsTable();
            } else {
                showError("Load XML file", result.message() + "\nFile not loaded.");
            }
        });
        loadTask.setOnFailed(e -> {
            dialog.close();
            showError("Load XML file", String.valueOf(loadTask.getException()));
        });

        dialog.show();
        Thread thread = new Thread(loadTask, "xml-loader");
        thread.setDaemon(true);
        thread.start();
    }

    void handleSaveState() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save system state");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Saved state", "*.ser"));
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        String path = file.getAbsolutePath();
        if (!path.endsWith(".ser")) path = path + ".ser";

        if (engine.saveState(path)) {
            showInfo("Save state", "State successfully saved to " + path + ".");
        } else {
            showError("Save state", "Failed to save state to " + path + ".");
        }
    }

    void handleLoadState() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load system state");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Saved state", "*.ser"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        boolean ok = engine.loadState(file.getAbsolutePath());
        currentUser = null;
        sidebar.refreshUsers(engine);
        sidebar.refreshStatus(engine);
        if (ok) {
            showInfo("Load state", "State successfully loaded from " + file.getAbsolutePath() + ".\n"
                    + engine.getEvents().size() + " event(s) available.");
            showEventsTable();
        } else {
            showError("Load state", "Failed to load state from " + file.getAbsolutePath() + ".");
        }
    }

    private boolean requireLoaded() {
        if (!engine.isLoaded) {
            showInfo("No data loaded", "No data loaded yet. Use \"Load XML file\" first.");
            return false;
        }
        return true;
    }

    private void setContent(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-pane");
        root.setCenter(scroll);
    }

    void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
