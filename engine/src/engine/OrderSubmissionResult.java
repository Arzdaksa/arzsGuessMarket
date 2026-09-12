package engine;

import java.io.Serializable;
import java.util.List;

public record OrderSubmissionResult(int filledQuantity, int restingQuantity, List<OrderBookTrade> newFills)
        implements Serializable {
}
