package ui;

import javafx.application.Application;
import ui.fx.GuessMarketApp;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        // ConsoleUI console = new ConsoleUI();
        // console.start();

        Application.launch(GuessMarketApp.class, args);
    }
}
