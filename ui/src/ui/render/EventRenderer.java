package ui.render;

import engine.Event;

import java.util.ArrayList;
import java.util.List;

// tables and numbered selection lists for events and their options
public final class EventRenderer {

    private EventRenderer() {}

    public static String eventsTable(List<Event> events) {
        String[] headers = {"ID", "Name", "Description", "Commission", "b"};
        List<String[]> rows = new ArrayList<>();
        for (Event event : events) {
            rows.add(new String[]{
                    String.valueOf(event.getID()),
                    event.getName(),
                    event.getDescription(),
                    event.getCommission().getCommission() + "% " + event.getCommission().getType(),
                    String.valueOf(event.getLmsrData().getB())
            });
        }
        return Layout.table("ALL EVENTS (" + events.size() + ")", headers, rows, true);
    }

    public static String eventSelectionList(List<Event> events, String title) {
        String[] headers = {"#", "Name", "ID", "Status"};
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            rows.add(new String[]{
                    String.valueOf(i + 1),
                    event.getName(),
                    String.valueOf(event.getID()),
                    event.isClosed() ? "closed" : "open"
            });
        }
        return Layout.table(title, headers, rows, true);
    }

    public static String optionSelectionList(List<String> optionNames) {
        String[] headers = {"#", "Option"};
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < optionNames.size(); i++) {
            rows.add(new String[]{String.valueOf(i + 1), optionNames.get(i)});
        }
        return Layout.table("CHOOSE AN OPTION", headers, rows, true);
    }
}
