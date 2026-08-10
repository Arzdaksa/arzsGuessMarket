package ui;

import engine.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TerminalRenderer {

    public static final int WIDTH = 80;
    private static final int MIN_COL_WIDTH = 4;
    private static final int MAX_NATURAL_COL_WIDTH = 44;
    private static final int PROBABILITY_BAR_WIDTH = 30;

    public enum PanelStyle { SUCCESS, ERROR, INFO }

    private TerminalRenderer() {}

    // ---- screens ----

    public static String bannerSplash() {
        StringBuilder sb = new StringBuilder();
        sb.append(horizontalLine('╔', '═', '╗', WIDTH)).append('\n');
        sb.append(emptyLine('║', WIDTH)).append('\n');
        sb.append(centeredLine('║', spacedTitle("GUESS MARKET"), WIDTH)).append('\n');
        sb.append(centeredLine('║', "LMSR Prediction Market Console  ·  v1.0", WIDTH)).append('\n');
        sb.append(emptyLine('║', WIDTH)).append('\n');
        sb.append(horizontalLine('╚', '═', '╝', WIDTH));
        return sb.toString();
    }

    public static String exitScreen() {
        StringBuilder sb = new StringBuilder();
        sb.append(horizontalLine('╔', '═', '╗', WIDTH)).append('\n');
        sb.append(centeredLine('║', "Thank you for using Guess Market.", WIDTH)).append('\n');
        sb.append(centeredLine('║', "Goodbye!  ✓", WIDTH)).append('\n');
        sb.append(horizontalLine('╚', '═', '╝', WIDTH));
        return sb.toString();
    }

    public static String mainMenu(boolean loaded, int eventCount, String warning) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("  [1] Load XML file");
        lines.add("  [2] Show all events");
        lines.add("  [3] Show event trade status");
        lines.add("  [4] Participate in event");
        lines.add("  [5] Close event");
        lines.add("  [6] Exit");
        lines.add("");

        String footerText = warning != null
                ? "✗ " + warning
                : "Data status: " + (loaded ? "✓ Loaded — " + eventCount + " event(s)" : "✗ No file loaded");

        int textWidth = WIDTH - 2;
        StringBuilder sb = new StringBuilder();
        sb.append(titledTop('┌', '┐', '─', "MAIN MENU", WIDTH)).append('\n');
        for (String line : lines) {
            sb.append('│').append(padRight(line, textWidth)).append('│').append('\n');
        }
        sb.append(horizontalLine('├', '─', '┤', WIDTH)).append('\n');
        sb.append('│').append(padRight(" " + footerText, textWidth)).append('│').append('\n');
        sb.append(horizontalLine('└', '─', '┘', WIDTH));
        return sb.toString();
    }

    public static String promptBox(String title, String question) {
        int textWidth = WIDTH - 4;
        StringBuilder sb = new StringBuilder();
        sb.append(titledTop('┌', '┐', '─', title, WIDTH)).append('\n');
        for (String wrapped : wrapText(question, textWidth)) {
            sb.append('│').append(' ').append(padRight(wrapped, textWidth)).append(' ').append('│').append('\n');
        }
        sb.append(horizontalLine('└', '─', '┘', WIDTH));
        return sb.toString();
    }

    public static String promptArrow(String label) {
        return "➤ " + label + ": ";
    }

    public static String messagePanel(PanelStyle style, List<String> lines) {
        char tl, tr, bl, br, fill, v;
        String label;
        switch (style) {
            case SUCCESS -> { tl = '╔'; tr = '╗'; bl = '╚'; br = '╝'; fill = '═'; v = '║'; label = "✓ SUCCESS"; }
            case ERROR -> { tl = '┏'; tr = '┓'; bl = '┗'; br = '┛'; fill = '━'; v = '┃'; label = "✗ ERROR"; }
            default -> { tl = '┌'; tr = '┐'; bl = '└'; br = '┘'; fill = '─'; v = '│'; label = "ℹ NOTE"; }
        }

        int textWidth = WIDTH - 6;
        StringBuilder sb = new StringBuilder();
        sb.append(titledTop(tl, tr, fill, label, WIDTH)).append('\n');
        for (String line : lines) {
            for (String wrapped : wrapText(line, textWidth)) {
                sb.append(v).append("  ").append(padRight(wrapped, textWidth)).append("  ").append(v).append('\n');
            }
        }
        sb.append(horizontalLine(bl, fill, br, WIDTH));
        return sb.toString();
    }

    /**
     * showHeader=true renders a header row followed by one separator, then unseparated data rows
     * (list style). showHeader=false renders no header but a separator between every row (card style).
     */
    public static String table(String title, String[] headers, List<String[]> rows, boolean showHeader) {
        int[] colWidths = computeColumnWidths(headers, rows, showHeader);
        int tableWidth = colWidths.length * 3 + 1 + sum(colWidths);

        StringBuilder sb = new StringBuilder();
        sb.append(titledTop('┌', '┐', '─', title, tableWidth)).append('\n');

        if (showHeader) {
            sb.append(dataRow(headers, colWidths));
            sb.append(rowSeparator(colWidths)).append('\n');
            for (String[] row : rows) {
                sb.append(dataRow(row, colWidths));
            }
        } else {
            for (int i = 0; i < rows.size(); i++) {
                sb.append(dataRow(rows.get(i), colWidths));
                if (i < rows.size() - 1) {
                    sb.append(rowSeparator(colWidths)).append('\n');
                }
            }
        }

        sb.append(horizontalLine('└', '─', '┘', tableWidth));
        return sb.toString();
    }

    public static String eventsTable(List<Event> events) {
        String[] headers = {"ID", "Name", "Description", "Commission", "b"};
        List<String[]> rows = new ArrayList<>();
        for (Event event : events) {
            rows.add(new String[]{
                    String.valueOf(event.getID()),
                    event.getName(),
                    event.getDescription(),
                    event.getCommission().getCommission() + "% " + event.getCommission().getType(),
                    String.valueOf(event.getLmsrData().getB())
            });
        }
        return table("ALL EVENTS (" + events.size() + ")", headers, rows, true);
    }

    public static String eventDetailCard(Event event) {
        String[] headers = {"Field", "Value"};
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Name", event.getName()});
        rows.add(new String[]{"Description", event.getDescription()});
        rows.add(new String[]{"Options", formatOptions(event.getGM_options_names())});
        rows.add(new String[]{"Commission",
                event.getCommission().getCommission() + "% (charged " + event.getCommission().getType() + ")"});
        rows.add(new String[]{"LMSR liquidity", "b = " + event.getLmsrData().getB()});
        return table("EVENT #" + event.getID(), headers, rows, false);
    }

    private static String formatOptions(List<String> options) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) sb.append("     ");
            sb.append(i + 1).append(") ").append(options.get(i));
        }
        return sb.toString();
    }

    public static String tradeStatusPanel(Event event) {
        Map<String, Double> probabilities = event.get_options_probabilities();

        String[] headers = {"Option", "Probability"};
        List<String[]> rows = new ArrayList<>();
        for (String name : event.getGM_options_names()) {
            rows.add(new String[]{name, formatProbabilityBar(probabilities.getOrDefault(name, 0.0))});
        }
        return table("TRADE STATUS — EVENT #" + event.getID(), headers, rows, true);
    }

    private static String formatProbabilityBar(double probability) {
        int filled = (int) Math.round(probability * PROBABILITY_BAR_WIDTH);
        filled = Math.max(0, Math.min(PROBABILITY_BAR_WIDTH, filled));
        String bar = "█".repeat(filled) + "░".repeat(PROBABILITY_BAR_WIDTH - filled);
        return String.format("%s  %5.1f%%", bar, probability * 100);
    }

    // ---- table internals ----

    private static int[] computeColumnWidths(String[] headers, List<String[]> rows, boolean showHeader) {
        int colCount = headers.length;
        int[] widths = new int[colCount];
        for (int c = 0; c < colCount; c++) {
            int w = showHeader ? headers[c].length() : 0;
            for (String[] row : rows) {
                if (c < row.length && row[c] != null) {
                    w = Math.max(w, row[c].length());
                }
            }
            widths[c] = Math.max(MIN_COL_WIDTH, Math.min(w, MAX_NATURAL_COL_WIDTH));
        }

        int budget = WIDTH - colCount * 3 - 1;
        while (sum(widths) > budget) {
            int idx = widestIndex(widths);
            if (widths[idx] <= MIN_COL_WIDTH) break;
            widths[idx]--;
        }
        return widths;
    }

    private static int widestIndex(int[] widths) {
        int idx = 0;
        for (int i = 1; i < widths.length; i++) {
            if (widths[i] > widths[idx]) idx = i;
        }
        return idx;
    }

    private static int sum(int[] values) {
        int total = 0;
        for (int v : values) total += v;
        return total;
    }

    private static String dataRow(String[] cells, int[] colWidths) {
        int colCount = colWidths.length;
        List<List<String>> wrappedCells = new ArrayList<>();
        int rowHeight = 1;
        for (int c = 0; c < colCount; c++) {
            String text = c < cells.length && cells[c] != null ? cells[c] : "";
            List<String> wrapped = wrapText(text, colWidths[c]);
            wrappedCells.add(wrapped);
            rowHeight = Math.max(rowHeight, wrapped.size());
        }

        StringBuilder sb = new StringBuilder();
        for (int line = 0; line < rowHeight; line++) {
            sb.append('│');
            for (int c = 0; c < colCount; c++) {
                List<String> wrapped = wrappedCells.get(c);
                String text = line < wrapped.size() ? wrapped.get(line) : "";
                sb.append(' ').append(padRight(text, colWidths[c])).append(' ').append('│');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String rowSeparator(int[] colWidths) {
        StringBuilder sb = new StringBuilder();
        sb.append('├');
        for (int c = 0; c < colWidths.length; c++) {
            sb.append("─".repeat(colWidths[c] + 2));
            sb.append(c == colWidths.length - 1 ? '┤' : '┼');
        }
        return sb.toString();
    }

    // ---- low-level layout helpers ----

    private static String titledTop(char tl, char tr, char fill, String title, int width) {
        String label = " " + title + " ";
        int fillCount = Math.max(1, width - 3 - label.length());
        return tl + String.valueOf(fill) + label + String.valueOf(fill).repeat(fillCount) + tr;
    }

    private static String horizontalLine(char left, char fill, char right, int width) {
        return left + String.valueOf(fill).repeat(width - 2) + right;
    }

    private static String emptyLine(char v, int width) {
        return v + " ".repeat(width - 2) + v;
    }

    private static String centeredLine(char v, String text, int width) {
        return v + centerText(text, width - 2) + v;
    }

    private static String centerText(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        int totalPad = width - s.length();
        int left = totalPad / 2;
        int right = totalPad - left;
        return " ".repeat(left) + s + " ".repeat(right);
    }

    private static String spacedTitle(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') {
                sb.append("  ");
            } else {
                sb.append(c).append(' ');
            }
        }
        return sb.toString().stripTrailing();
    }

    private static String padRight(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }

    private static List<String> wrapText(String text, int width) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            result.add("");
            return result;
        }

        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (line.length() == 0) {
                line.append(word);
            } else if (line.length() + 1 + word.length() <= width) {
                line.append(' ').append(word);
            } else {
                result.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (!line.isEmpty()) result.add(line.toString());
        if (result.isEmpty()) result.add("");

        for (int i = 0; i < result.size(); i++) {
            String l = result.get(i);
            if (l.length() > width) {
                result.set(i, l.substring(0, Math.max(0, width - 1)) + "…");
            }
        }
        return result;
    }
}
