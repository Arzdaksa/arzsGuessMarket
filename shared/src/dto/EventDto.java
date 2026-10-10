package dto;

import java.util.List;

// lmsrB is set for LMSR events, orderBook for Order Book events
public record EventDto(int id, String name, String description, EventType type, EventStatus status,
                       int commissionPercent, String commissionType, String marketMaker,
                       double accountBalance, double commissionCollected, double openCost,
                       String winningOption, List<OptionDto> options, Integer lmsrB,
                       List<TradeDto> trades, OrderBookDto orderBook) {

    public boolean isActive() {return status == EventStatus.ACTIVE;}
    public boolean isInactive() {return status == EventStatus.INACTIVE;}
    public boolean isClosed() {return status == EventStatus.CLOSED;}

    public List<String> optionNames() {
        return options.stream().map(OptionDto::name).toList();
    }
}
