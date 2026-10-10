package ui.fx;

import dto.EventDto;
import dto.EventStatus;
import dto.EventType;
import dto.OrderResultDto;
import dto.PollResponse;
import dto.TradeDto;
import dto.UploadResultDto;
import dto.UserDto;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ui.net.ServerClient;
import ui.net.ServerException;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

// navigation hub for one logged-in session: swaps the BorderPane center per screen, runs every
// server request on a single background thread, and polls the server to keep the screen live
final class ScreenController {

    // the automatic pull: how often the client asks the server for changes
    private static final long POLL_INTERVAL_MS = 1000;

    @FunctionalInterface
    interface ServerAction<T> {
        T run() throws Exception;
    }

    private final Stage stage;
    private final BorderPane root;
    private final ServerClient client;
    private final MarketModel model;
    private final Consumer<String> onSessionEnded;
    final EventsTableView.Filters eventFilters = new EventsTableView.Filters();

    // one thread for polls and actions alike, so responses are applied in the order they were requested
    private final ScheduledExecutorService network = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "guess-market-network");
        thread.setDaemon(true);
        return thread;
    });

    private Sidebar sidebar;
    // rebuilds the current screen from the latest model; null on screens that hold user input
    private Runnable refresher;
    private boolean refreshing;
    private boolean chatOpen;
    private boolean sessionEnded;
    private int unreadChat;

    ScreenController(Stage stage, BorderPane root, ServerClient client, String username, Consumer<String> onSessionEnded) {
        this.stage = stage;
        this.root = root;
        this.client = client;
        this.model = new MarketModel(username);
        this.onSessionEnded = onSessionEnded;
    }

    void attachSidebar(Sidebar sidebar) {
        this.sidebar = sidebar;
        sidebar.refresh(model, true, 0);
    }

    MarketModel model() {return model;}
    ServerClient client() {return client;}

    // ---------- live polling and server actions ----------

    void start() {
        network.scheduleWithFixedDelay(this::pollOnce, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void pollOnce() {
        try {
            PollResponse response = client.poll(model.version(), model.chatCursor());
            Platform.runLater(() -> applyPoll(response));
        } catch (RuntimeException e) {
            Platform.runLater(() -> onPollFailed(e));
        }
    }

    private void applyPoll(PollResponse response) {
        if (sessionEnded) return;
        // the first poll delivers the chat history from before this login, which doesn't count as unread
        boolean history = model.version() == 0;
        boolean changed = model.applySnapshot(response);
        int newMessages = model.applyChat(response);
        if (!chatOpen && !history) unreadChat += newMessages;
        sidebar.refresh(model, true, unreadChat);
        if (changed) refreshCurrentScreen();
    }

    private void onPollFailed(RuntimeException e) {
        if (sessionEnded) return;
        if (e instanceof ServerException && ((ServerException) e).isUnauthorized()) {
            endSession("Your session ended. Please log in again.");
        } else {
            sidebar.refresh(model, false, unreadChat);
        }
    }

    private void refreshCurrentScreen() {
        if (refresher == null) return;
        refreshing = true;
        try {
            refresher.run();
        } finally {
            refreshing = false;
        }
    }

    <T> void submit(String errorTitle, ServerAction<T> action, Consumer<T> onSuccess) {
        submit(errorTitle, action, onSuccess, () -> {});
    }

    // runs the action off the JavaFX thread, pulls the resulting state straight away, then reports back on it
    <T> void submit(String errorTitle, ServerAction<T> action, Consumer<T> onSuccess, Runnable onFailure) {
        network.execute(() -> {
            T result;
            try {
                result = action.run();
            } catch (Exception e) {
                Platform.runLater(() -> {
                    onFailure.run();
                    onActionFailed(errorTitle, e);
                });
                return;
            }
            PollResponse fresh = null;
            try {
                fresh = client.poll(model.version(), model.chatCursor());
            } catch (RuntimeException ignored) {
                // the action itself succeeded; the next scheduled poll will catch the screen up
            }
            PollResponse state = fresh;
            Platform.runLater(() -> {
                if (sessionEnded) return;
                if (state != null) applyPoll(state);
                onSuccess.accept(result);
            });
        });
    }

    private void onActionFailed(String title, Exception e) {
        if (sessionEnded) return;
        if (e instanceof ServerException && ((ServerException) e).isUnauthorized()) {
            endSession("Your session ended. Please log in again.");
            return;
        }
        showError(title, e.getMessage() == null ? e.toString() : e.getMessage());
    }

    void handleLogout() {
        network.execute(() -> {
            try {
                client.logout();
            } catch (RuntimeException ignored) {
                // the server drops the session by itself once polling stops
            }
            Platform.runLater(() -> endSession(null));
        });
    }

    private void endSession(String notice) {
        if (sessionEnded) return;
        sessionEnded = true;
        network.shutdownNow();
        onSessionEnded.accept(notice);
    }

    // called when the window closes: frees the user name on the server right away
    void shutdown() {
        if (sessionEnded) return;
        sessionEnded = true;
        network.shutdownNow();
        try {
            client.logout();
        } catch (RuntimeException ignored) {
        }
    }

    // ---------- screens ----------

    void showWelcome() {
        sidebar.setActive(null);
        setContent(WelcomeView.build(model), this::showWelcome);
    }

    void showEventsTable() {
        sidebar.setActive(Sidebar.NavItem.EVENTS);
        setContent(EventsTableView.build(this, model), this::showEventsTable);
    }

    void showTradeChooser() {
        sidebar.setActive(Sidebar.NavItem.TRADE);
        setContent(TradeChooserView.build(this), null);
    }

    void showManageEventChooser() {
        sidebar.setActive(Sidebar.NavItem.MANAGE_EVENT);
        setContent(ManageEventView.build(this), null);
    }

    // routes to the correct dashboard type — used by row-click handlers that don't know the type ahead of time
    void showEventDashboard(int eventId) {
        EventDto event = model.event(eventId);
        if (event == null) {
            showEventsTable();
            return;
        }
        sidebar.setActive(Sidebar.NavItem.EVENTS);
        setContent(dashboard(event), () -> showEventDashboard(eventId));
    }

    private VBox dashboard(EventDto event) {
        return event.type() == EventType.LMSR
                ? TradeStatusView.buildDashboard(this, event)
                : OrderBookView.buildDashboard(this, event);
    }

    void showParticipateWizard() {
        boolean anyLmsrActive = model.getActiveEvents().stream().anyMatch(e -> e.type() == EventType.LMSR);
        if (!anyLmsrActive) {
            showInfo("No active events", "No active LMSR events to trade in.");
            return;
        }
        sidebar.setActive(Sidebar.NavItem.TRADE);
        setContent(ParticipateWizardView.build(this, model), null);
    }

    void showTradeReceipt(TradeDto trade, int eventId) {
        showReceipt(ParticipateWizardView.buildReceipt(trade), eventId, () -> showTradeReceipt(trade, eventId));
    }

    void showSubmitOrderWizard() {
        boolean anyObActive = model.getActiveEvents().stream().anyMatch(e -> e.type() == EventType.ORDER_BOOK);
        if (!anyObActive) {
            showInfo("No active events", "No active Order Book events to trade in.");
            return;
        }
        sidebar.setActive(Sidebar.NavItem.TRADE);
        setContent(SubmitOrderWizardView.build(this, model), null);
    }

    void showOrderReceipt(OrderResultDto result, int eventId) {
        showReceipt(SubmitOrderWizardView.buildReceipt(result), eventId, () -> showOrderReceipt(result, eventId));
    }

    // the receipt stays pinned above the dashboard while the dashboard below keeps refreshing
    private void showReceipt(VBox receipt, int eventId, Runnable refresher) {
        EventDto event = model.event(eventId);
        if (event == null) {
            showEventsTable();
            return;
        }
        sidebar.setActive(Sidebar.NavItem.EVENTS);
        VBox dashboard = dashboard(event);
        dashboard.getChildren().add(0, receipt);
        setContent(dashboard, refresher);
    }

    void showOpenEventWizard() {
        if (model.myEvents(EventStatus.INACTIVE).isEmpty()) {
            showInfo("No inactive events", "You have no inactive events to open. "
                    + "Upload an XML file to become the market maker of its events.");
            return;
        }
        sidebar.setActive(Sidebar.NavItem.MANAGE_EVENT);
        setContent(OpenEventWizardView.build(this, model), null);
    }

    void showCloseEventWizard() {
        if (model.myEvents(EventStatus.ACTIVE).isEmpty()) {
            showInfo("No active events", "You have no active events to close.");
            return;
        }
        sidebar.setActive(Sidebar.NavItem.MANAGE_EVENT);
        setContent(CloseEventWizardView.build(this, model), null);
    }

    void showCreateEventWizard() {
        sidebar.setActive(Sidebar.NavItem.MANAGE_EVENT);
        setContent(CreateEventWizardView.build(this), null);
    }

    void showUsersList() {
        sidebar.setActive(Sidebar.NavItem.USERS);
        setContent(UsersListView.build(this, model), this::showUsersList);
    }

    void showUserDetail(String userName) {
        UserDto user = model.user(userName);
        if (user == null) {
            showUsersList();
            return;
        }
        sidebar.setActive(Sidebar.NavItem.USERS);
        setContent(UserDetailView.build(this, model, user), () -> showUserDetail(userName));
    }

    // the chat list is bound to the model and updates itself, so the screen is never rebuilt under the input box
    void showChat() {
        sidebar.setActive(Sidebar.NavItem.CHAT);
        setContent(ChatView.build(this, model), null);
        chatOpen = true;
        unreadChat = 0;
        sidebar.refresh(model, true, 0);
    }

    // ---------- sidebar actions ----------

    void handleUploadXml() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Upload XML file");
        File defaultDir = new File("engine/xml_files_dir");
        if (defaultDir.isDirectory()) {
            chooser.setInitialDirectory(defaultDir);
        }
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files", "*.xml"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) uploadXml(file);
    }

    void uploadXml(File file) {
        ProgressDialog dialog = new ProgressDialog(stage, "Uploading market data…");
        dialog.show();
        this.<UploadResultDto>submit("Upload XML file", () -> client.uploadXml(file.toPath()), result -> {
            dialog.close();
            showInfo("Upload XML file", result.addedEvents().size() + " event(s) added, with you as market maker:\n"
                    + String.join("\n", result.addedEvents()));
            showEventsTable();
        }, dialog::close);
    }

    void handleDeposit() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.initOwner(stage);
        dialog.setTitle("Add funds");
        dialog.setHeaderText(null);
        dialog.setContentText("Amount to add to your balance:");
        Optional<String> input = dialog.showAndWait();
        if (input.isEmpty() || input.get().isBlank()) return;

        double amount;
        try {
            amount = Double.parseDouble(input.get().trim());
        } catch (NumberFormatException e) {
            showError("Add funds", "'" + input.get().trim() + "' is not a valid amount.");
            return;
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            showError("Add funds", "Amount must be greater than 0.");
            return;
        }
        submit("Add funds", () -> {
            client.deposit(amount);
            return null;
        }, ok -> {});
    }

    private void setContent(Node content, Runnable refresher) {
        double scrollPosition = 0;
        if (refreshing && root.getCenter() instanceof ScrollPane) {
            scrollPosition = ((ScrollPane) root.getCenter()).getVvalue();
        }
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-pane");
        root.setCenter(scroll);
        scroll.setVvalue(scrollPosition);
        this.refresher = refresher;
        this.chatOpen = false;
    }

    void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
