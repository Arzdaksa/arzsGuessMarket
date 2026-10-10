package dto;

// probability is the LMSR price of the option; 0 for Order Book events
public record OptionDto(String name, int quantity, double probability) {
}
