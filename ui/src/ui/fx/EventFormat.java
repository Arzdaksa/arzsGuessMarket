package ui.fx;

import engine.Event;
import engine.User;

// shared display text for Event/User across breadcrumbs, combo boxes, and table columns
final class EventFormat {

    private EventFormat() {}

    static String describe(Event event) {
        if (event == null) return "";
        return "#" + event.getID() + " " + event.getName() + " [" + event.getState() + "]";
    }

    static String describe(User user) {
        if (user == null) return "";
        String suffix = user.isLocked() ? " (locked)" : "";
        return user.getName() + " — $" + String.format("%.2f", user.getBalance()) + suffix;
    }
}
