package ui.fx;

import engine.Event;
import engine.User;

final class EventFormat {

    private EventFormat() {}

    // "#id Name [STATE]"
    static String describe(Event event) {
        if (event == null) return "";
        return "#" + event.getID() + " " + event.getName() + " [" + event.getState() + "]";
    }

    // "Name — $balance", with a "(locked)" suffix if applicable
    static String describe(User user) {
        if (user == null) return "";
        String suffix = user.isLocked() ? " (locked)" : "";
        return user.getName() + " — $" + String.format("%.2f", user.getBalance()) + suffix;
    }
}
