package ui;

import engine.Event;
import engine.GuessMarketEngine;
import engine.LoadResult;
import engine.Trade;
import ui.render.EventRenderer;
import ui.render.MenuRenderer;
import ui.render.MenuRenderer.PanelStyle;
import ui.render.TradeStatusRenderer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ConsoleUI {
    private final GuessMarketEngine engine;
    private final Scanner scanner;
    private final LinkedHashMap<String, MenuOption> menuOptions;
    private boolean isRunning;

    private record MenuOption(String label, Runnable action) {}

    public ConsoleUI() {
        this.engine = new GuessMarketEngine();
        this.scanner = new Scanner(System.in);
        this.menuOptions = new LinkedHashMap<>();
        menuOptions.put("1", new MenuOption("Load XML file", this::handleLoad));
        menuOptions.put("2", new MenuOption("Show all events", this::handleShowAll));
        menuOptions.put("3", new MenuOption("Show event trade status", this::handleTradeStatus));
        menuOptions.put("4", new MenuOption("Participate in event", this::handleParticipate));
        menuOptions.put("5", new MenuOption("Close event", this::handleCloseEvent));
        menuOptions.put("6", new MenuOption("Exit", this::handleExit));
    }

    public void start() {
        System.out.println(MenuRenderer.bannerSplash());

        isRunning = true;
        String warning = null;

        while (isRunning) {
            System.out.println(MenuRenderer.mainMenu(labelsByKey(), engine.isLoaded, engine.getEvents().size(), warning));
            warning = null;
            System.out.print(MenuRenderer.promptArrow("Enter your choice (" + keyRange() + ")"));
            String choice = scanner.nextLine();

            MenuOption selected = menuOptions.get(choice);
            if (selected != null) {
                selected.action().run();
            } else {
                warning = "'" + choice + "' is not a valid option. Please choose " + keyRange() + ".";
            }
        }
    }

    private void handleLoad() {
        System.out.println(MenuRenderer.promptBox("LOAD XML FILE", "Enter full XML file path:"));
        System.out.print(MenuRenderer.promptArrow("Path"));
        String path = scanner.nextLine();

        LoadResult result = engine.loadDataFromXML(path);
        if (result.success()) {
            System.out.println(MenuRenderer.messagePanel(PanelStyle.SUCCESS, List.of(
                    "File loaded successfully.",
                    engine.getEvents().size() + " event(s) available. Use option 2 to view them."
            )));
        } else {
            System.out.println(MenuRenderer.messagePanel(PanelStyle.ERROR, List.of(
                    result.message(),
                    "File not loaded."
            )));
        }
    }

    private void handleShowAll() {
        if (!engine.isLoaded) {
            printNoDataLoaded();
            return;
        }
        System.out.println(EventRenderer.eventsTable(engine.getEvents()));
    }

    private void handleTradeStatus() {
        if (!engine.isLoaded) {
            printNoDataLoaded();
            return;
        }

        List<Event> events = engine.getEvents();
        if (events.isEmpty()) {
            System.out.println(MenuRenderer.messagePanel(PanelStyle.INFO, List.of("No events loaded yet.")));
            return;
        }

        System.out.println(EventRenderer.eventSelectionList(events, "SELECT AN EVENT"));
        Event event = selectEventBySerial(events);
        if (event == null) return;

        System.out.println(TradeStatusRenderer.dashboard(event));
    }

    private void handleParticipate() {
        if (!engine.isLoaded) {
            printNoDataLoaded();
            return;
        }

        List<Event> activeEvents = engine.getActiveEvents();
        if (activeEvents.isEmpty()) {
            System.out.println(MenuRenderer.messagePanel(PanelStyle.INFO, List.of("No active events to trade in.")));
            return;
        }

        System.out.println(EventRenderer.eventSelectionList(activeEvents, "SELECT AN ACTIVE EVENT"));
        Event event = selectEventBySerial(activeEvents);
        if (event == null) return;

        System.out.println(TradeStatusRenderer.dashboard(event));

        String optionName = selectOptionBySerial(event);
        if (optionName == null) return;

        Integer quantity = promptPositiveInt("Quantity of shares");
        if (quantity == null) return;

        Trade trade = event.executeTrade(optionName, quantity);

        System.out.println(TradeStatusRenderer.tradeReceipt(trade));
        System.out.println(TradeStatusRenderer.dashboard(event));
    }

    private void handleCloseEvent() {
        if (!engine.isLoaded) {
            printNoDataLoaded();
            return;
        }

        List<Event> activeEvents = engine.getActiveEvents();
        if (activeEvents.isEmpty()) {
            System.out.println(MenuRenderer.messagePanel(PanelStyle.INFO, List.of("No active events to close.")));
            return;
        }

        System.out.println(EventRenderer.eventSelectionList(activeEvents, "SELECT AN ACTIVE EVENT"));
        Event event = selectEventBySerial(activeEvents);
        if (event == null) return;

        System.out.println(TradeStatusRenderer.dashboard(event));

        String winningOption = selectOptionBySerial(event);
        if (winningOption == null) return;

        event.closeEvent(winningOption);

        System.out.println(TradeStatusRenderer.dashboard(event));
    }

    private void handleExit() {
        System.out.println(MenuRenderer.exitScreen());
        isRunning = false;
    }

    private Event selectEventBySerial(List<Event> events) {
        Integer serial = promptInt("Event # (1-" + events.size() + ")");
        if (serial == null) return null;
        if (serial < 1 || serial > events.size()) {
            System.out.println(MenuRenderer.messagePanel(PanelStyle.ERROR, List.of(
                    serial + " is out of range. Choose between 1 and " + events.size() + "."
            )));
            return null;
        }
        return events.get(serial - 1);
    }

    private String selectOptionBySerial(Event event) {
        List<String> optionNames = event.getOptionNames();
        System.out.println(EventRenderer.optionSelectionList(optionNames));

        Integer serial = promptInt("Option # (1-" + optionNames.size() + ")");
        if (serial == null) return null;
        if (serial < 1 || serial > optionNames.size()) {
            System.out.println(MenuRenderer.messagePanel(PanelStyle.ERROR, List.of(
                    serial + " is out of range. Choose between 1 and " + optionNames.size() + "."
            )));
            return null;
        }
        return optionNames.get(serial - 1);
    }

    private Integer promptInt(String label) {
        System.out.print(MenuRenderer.promptArrow(label));
        String input = scanner.nextLine();
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            System.out.println(MenuRenderer.messagePanel(PanelStyle.ERROR, List.of(
                    "'" + input + "' is not a valid number."
            )));
            return null;
        }
    }

    private Integer promptPositiveInt(String label) {
        Integer value = promptInt(label);
        if (value == null) return null;
        if (value <= 0) {
            System.out.println(MenuRenderer.messagePanel(PanelStyle.ERROR, List.of(
                    "Quantity must be a positive number."
            )));
            return null;
        }
        return value;
    }

    private String keyRange() {
        List<String> keys = new ArrayList<>(menuOptions.keySet());
        return keys.get(0) + "-" + keys.get(keys.size() - 1);
    }

    private Map<String, String> labelsByKey() {
        Map<String, String> labels = new LinkedHashMap<>();
        menuOptions.forEach((key, option) -> labels.put(key, option.label()));
        return labels;
    }

    private void printNoDataLoaded() {
        System.out.println(MenuRenderer.messagePanel(PanelStyle.INFO, List.of(
                "No data loaded yet.",
                "Use option 1 to load an XML file first."
        )));
    }
}
