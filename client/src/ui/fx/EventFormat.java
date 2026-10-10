package ui.fx;

import dto.EventDto;

final class EventFormat {

    private EventFormat() {}

    // "#id Name [STATE]"
    static String describe(EventDto event) {
        if (event == null) return "";
        return "#" + event.id() + " " + event.name() + " [" + event.status() + "]";
    }
}
