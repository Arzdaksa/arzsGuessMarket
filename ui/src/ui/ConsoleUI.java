package ui;

import engine.GuessMarketEngine;

import java.util.Scanner;

public class ConsoleUI {
    private final GuessMarketEngine engine;
    private final Scanner scanner;

    public ConsoleUI() {
        this.engine = new GuessMarketEngine();
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        boolean isRunning = true;

        while (isRunning) {
            printMenu();
            System.out.print("Please enter your choice (1-6): ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    System.out.print("Enter full XML file path: ");
                    String path = scanner.nextLine();
                    this.engine.loadDataFromXML(path);
                    break;
                case "2":
                    if (this.engine.isLoaded) this.engine.printAllEvents();
                    else System.out.println("----- Message -----\n* No data loaded!");
                    break;
                case "3":
                    if (this.engine.isLoaded) {
                        System.out.println("Please enter Event ID: ");
                        String eventID = scanner.nextLine();

                    } else System.out.println("----- Message -----\n* No data loaded!");

                    break;
                case "6":
                    System.out.println("----- Message -----\n* Exiting Guess Market. Goodbye!");
                    isRunning = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n--- Guess Market Menu ---");
        System.out.println("1. Load XML file");
        System.out.println("2. Show all events");
        System.out.println("3. Show event trade status");
        System.out.println("4. Participate in event");
        System.out.println("5. Close event");
        System.out.println("6. Exit");
    }
}
