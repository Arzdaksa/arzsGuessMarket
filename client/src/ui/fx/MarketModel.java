package ui.fx;

import dto.ChatMessageDto;
import dto.EventDto;
import dto.EventStatus;
import dto.PollResponse;
import dto.UserDto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

// the client's copy of the server state, replaced wholesale by each poll that reports a change.
// Mutated only on the JavaFX thread; version and chatCursor are volatile so the network thread can read them.
final class MarketModel {

    private final String username;
    private volatile long version = 0;
    private volatile long chatCursor = 0;
    private List<EventDto> events = List.of();
    private List<UserDto> users = List.of();
    private final ObservableList<ChatMessageDto> chat = FXCollections.observableArrayList();

    MarketModel(String username) {
        this.username = username;
    }

    String username() {return username;}
    long version() {return version;}
    long chatCursor() {return chatCursor;}
    List<EventDto> getEvents() {return events;}
    List<UserDto> getUsers() {return users;}
    ObservableList<ChatMessageDto> chat() {return chat;}

    // returns true when the events/users snapshot was replaced
    boolean applySnapshot(PollResponse response) {
        if (!response.changed() || response.events() == null || response.users() == null) return false;
        events = response.events();
        users = response.users();
        version = response.version();
        return true;
    }

    // returns how many messages were new; overlapping polls can deliver the same message twice
    int applyChat(PollResponse response) {
        int added = 0;
        if (response.newMessages() != null) {
            for (ChatMessageDto message : response.newMessages()) {
                if (message.seq() > chatCursor) {
                    chat.add(message);
                    chatCursor = message.seq();
                    added++;
                }
            }
        }
        return added;
    }

    EventDto event(int id) {
        for (EventDto event : events) {
            if (event.id() == id) return event;
        }
        return null;
    }

    UserDto user(String name) {
        for (UserDto user : users) {
            if (user.name().equals(name)) return user;
        }
        return null;
    }

    UserDto me() {
        return user(username);
    }

    List<EventDto> getActiveEvents() {
        return events.stream().filter(EventDto::isActive).toList();
    }

    // events the logged-in user is market maker of, in the given state
    List<EventDto> myEvents(EventStatus status) {
        return events.stream()
                .filter(e -> e.status() == status && username.equals(e.marketMaker()))
                .toList();
    }
}
