package dto;

// any price is null while that side of the book is empty
public record OptionStatsDto(String optionName, Double last, Double bid, Double ask, Double mid, Double spread) {
}
