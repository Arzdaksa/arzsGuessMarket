package dto;

import java.util.List;

public record CreateEventRequest(String name, String description, int commissionPercent, String commissionType,
                                 List<String> optionNames, EventType type,
                                 Integer lmsrB, Integer orderBookD, Integer orderBookInitial, boolean allowMint) {
}
