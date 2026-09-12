package ui.fx;

import engine.User;
import javafx.util.StringConverter;

final class FormatConverters {

    private FormatConverters() {}

    static StringConverter<User> userConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(User user) {
                return EventFormat.describe(user);
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        };
    }
}
