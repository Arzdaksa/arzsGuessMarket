package dto;

import java.util.List;

// events and users are null when the market version the client already holds is still current;
// newMessages holds only the chat messages after the client's cursor
public record PollResponse(long version, boolean changed, List<EventDto> events, List<UserDto> users,
                           List<ChatMessageDto> newMessages, long chatCursor) {
}
