package dto;

public record ChatMessageDto(long seq, String sender, String text, long timestamp) {
}
