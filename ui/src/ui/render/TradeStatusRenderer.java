package ui.render;

import engine.Event;
import engine.Option;
import engine.Trade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ui.render.Layout.*;

// the option-3 trade-status dashboard, plus the option-4 purchase receipt that shares its vocabulary
public final class TradeStatusRenderer {

    private static final int PROBABILITY_BAR_WIDTH = 30;

    private TradeStatusRenderer() {}

    // full read-only status view for one event: header, live prices, account, history, closed state
    public static String dashboard(Event event) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(event)).append("\n\n");
        sb.append(optionsPanel(event)).append("\n\n");
        sb.append(accountPanel(event)).append("\n\n");
        sb.append(historyPanel(event));
        if (event.isClosed()) {
            sb.append("\n\n").append(closedPanel(event));
        }
        return sb.toString();
    }

    private static String header(Event event) {
        String[] headers = {"Field", "Value"};
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Description", event.getDescription()});
        rows.add(new String[]{"Commission",
                event.getCommission().getCommission() + "% (charged " + event.getCommission().getType() + ")"});
        rows.add(new String[]{"LMSR liquidity", "b = " + event.getLmsrData().getB()});
        return table("EVENT #" + event.getID() + " · " + event.getName(), headers, rows, false);
    }

    private static String optionsPanel(Event event) {
        Map<String, Double> probabilities = event.getOptionProbabilities();
        String[] headers = {"Option", "Probability", "Value", "Shares Bought"};
        List<String[]> rows = new ArrayList<>();
        for (Option option : event.getOptions()) {
            double probability = probabilities.getOrDefault(option.getOptionName(), 0.0);
            rows.add(new String[]{
                    option.getOptionName(),
                    formatProbabilityBar(probability),
                    String.format("%.2f", probability),
                    String.valueOf(option.getOptionQuantity())
            });
        }
        return table("CURRENT VALUES", headers, rows, true);
    }

    private static String accountPanel(Event event) {
        String[] headers = {"Field", "Value"};
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Account balance", String.format("%.2f", event.getAccountBalance())});
        rows.add(new String[]{"Commission collected", String.format("%.2f", event.getTotalCommissionCollected())});
        return table("ACCOUNT", headers, rows, false);
    }

    private static String historyPanel(Event event) {
        List<Trade> history = event.getTradeHistory();
        if (history.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(titledTop('┌', '┐', '─', "TRADE HISTORY (0)", WIDTH)).append('\n');
            sb.append('│').append(padRight(" No trades yet.", WIDTH - 2)).append('│').append('\n');
            sb.append(horizontalLine('└', '─', '┘', WIDTH));
            return sb.toString();
        }

        String[] headers = {"Option", "Qty", "Shares Cost", "Commission", "Total Paid"};
        List<String[]> rows = new ArrayList<>();
        for (int i = history.size() - 1; i >= 0; i--) {
            Trade trade = history.get(i);
            rows.add(new String[]{
                    trade.optionName(),
                    String.valueOf(trade.quantity()),
                    String.format("%.2f", trade.sharesCost()),
                    String.format("%.2f", trade.commissionPaid()),
                    String.format("%.2f", trade.totalPaid())
            });
        }
        return table("TRADE HISTORY (" + history.size() + ")", headers, rows, true);
    }

    private static String closedPanel(Event event) {
        String winner = event.getWinningOption();
        int winningShares = event.getOptions().stream()
                .filter(option -> option.getOptionName().equals(winner))
                .mapToInt(Option::getOptionQuantity)
                .findFirst().orElse(0);

        boolean onClose = "on-close".equals(event.getCommission().getType());
        double grossPayout = winningShares;
        double commissionRetained = onClose ? event.getTotalCommissionCollected() : 0;
        double netPaidToWinners = grossPayout - commissionRetained;

        String[] headers = {"Field", "Value"};
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Winning option", winner});
        rows.add(new String[]{"Winning shares", String.valueOf(winningShares)});
        rows.add(new String[]{"Gross payout", String.format("%.2f", grossPayout)});
        rows.add(new String[]{"Commission retained", String.format("%.2f", commissionRetained)});
        rows.add(new String[]{"Net paid to winners", String.format("%.2f", netPaidToWinners)});
        return table("🏆 EVENT CLOSED", headers, rows, false);
    }

    // post-purchase summary for option 4, breaking the bill down into shares cost vs. commission
    public static String tradeReceipt(Trade trade) {
        String[] headers = {"Field", "Value"};
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Option bought", trade.optionName()});
        rows.add(new String[]{"Quantity", String.valueOf(trade.quantity())});
        rows.add(new String[]{"Shares cost", String.format("%.2f", trade.sharesCost())});
        rows.add(new String[]{"Commission paid", String.format("%.2f", trade.commissionPaid())});
        rows.add(new String[]{"Total paid", String.format("%.2f", trade.totalPaid())});
        return table("PURCHASE RECEIPT", headers, rows, false);
    }

    private static String formatProbabilityBar(double probability) {
        int filled = (int) Math.round(probability * PROBABILITY_BAR_WIDTH);
        filled = Math.max(0, Math.min(PROBABILITY_BAR_WIDTH, filled));
        String bar = "█".repeat(filled) + "░".repeat(PROBABILITY_BAR_WIDTH - filled);
        return String.format("%s  %5.1f%%", bar, probability * 100);
    }
}
