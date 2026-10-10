package dto;

public record OrderDto(int id, String optionName, String ownerName, int quantity, double price) {
}
