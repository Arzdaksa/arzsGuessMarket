package dto;

import java.util.List;
import java.util.Map;

// book contents are empty until the market maker opens the event
public record OrderBookDto(int d, int initial, boolean allowMint, boolean opened,
                           List<OptionStatsDto> stats, List<OrderDto> bids, List<OrderDto> asks,
                           List<FillDto> fills, Map<String, Map<String, Integer>> holdings) {

    public Map<String, Integer> holdingsOf(String userName) {
        return holdings.getOrDefault(userName, Map.of());
    }
}
