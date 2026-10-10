package dto;

import java.util.List;

public record UserDto(String name, double balance, boolean locked, boolean online,
                      int marketMakerCount, List<Integer> participatedEventIds) {
}
