package dto;

public record OrderRequest(String option, Side side, int quantity, double price) {
}
