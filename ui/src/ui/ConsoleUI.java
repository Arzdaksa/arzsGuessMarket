package ui;

import engine.Event;
import engine.GuessMarketEngine;
import engine.LoadResult;

import java.util.List;
import java.util.Scanner;

public class ConsoleUI {
    private final GuessMarketEngine engine;
    private final Scanner scanner;

    public ConsoleUI() {
        this.engine = new GuessMarketEngine();
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println(TerminalRenderer.bannerSplash());

        boolean isRunning = true;
        String warning = null;

        while (isRunning) {
            System.out.println(TerminalRenderer.mainMenu(engine.isLoaded, engine.getEvents().size(), warning));
            warning = null;
            System.out.print(TerminalRenderer.promptArrow("Enter your choice (1-6)"));
            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> handleLoad();
                case "2" -> handleShowAll();
                case "3" -> handleShowById();
                case "6" -> {
                    System.out.println(TerminalRenderer.exitScreen());
                    isRunning = false;
                }
                default -> warning = "'" + choice + "' is not a valid option. Please choose 1-6.";
            }
        }
    }

    private void handleLoad() {
        System.out.println(TerminalRenderer.promptBox("LOAD XML FILE", "Enter full XML file path:"));
        System.out.print(TerminalRenderer.promptArrow("Path"));
        String path = scanner.nextLine();

        LoadResult result = engine.loadDataFromXML(path);
        if (result.success()) {
            System.out.println(TerminalRenderer.messagePanel(TerminalRenderer.PanelStyle.SUCCESS, List.of(
                    "File loaded successfully.",
                    engine.getEvents().size() + " event(s) available. Use option 2 to view them."
            )));
        } else {
            System.out.println(TerminalRenderer.messagePanel(TerminalRenderer.PanelStyle.ERROR, List.of(
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
        System.out.println(TerminalRenderer.eventsTable(engine.getEvents()));
    }

    private void handleShowById() {
        if (!engine.isLoaded) {
            printNoDataLoaded();
            return;
        }

        System.out.println(TerminalRenderer.promptBox("EVENT LOOKUP", "Enter Event ID to inspect:"));
        System.out.print(TerminalRenderer.promptArrow("Event ID"));
        String input = scanner.nextLine();

        int id;
        try {
            id = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println(TerminalRenderer.messagePanel(TerminalRenderer.PanelStyle.ERROR, List.of(
                    "'" + input + "' is not a valid numeric ID."
            )));
            return;
        }

        Event event = engine.getEventByID(id);
        if (event == null) {
            System.out.println(TerminalRenderer.messagePanel(TerminalRenderer.PanelStyle.ERROR, List.of(
                    "No event found with ID " + id + ".",
                    "Use option 2 to see all available event IDs."
            )));
        } else {
            System.out.println(TerminalRenderer.eventDetailCard(event));
            System.out.println(TerminalRenderer.tradeStatusPanel(event));
        }
    }

    private void printNoDataLoaded() {
        System.out.println(TerminalRenderer.messagePanel(TerminalRenderer.PanelStyle.INFO, List.of(
                "No data loaded yet.",
                "Use option 1 to load an XML file first."
        )));
    }
}
