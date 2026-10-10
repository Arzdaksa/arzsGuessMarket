package ui.render;

import java.util.ArrayList;
import java.util.List;

public final class Layout {

    public static final int WIDTH = 80;
    private static final int MIN_COL_WIDTH = 4;
    private static final int MAX_NATURAL_COL_WIDTH = 44;

    private Layout() {}

    // showHeader=true: header row + one separator, then unseparated rows (list style); showHeader=false: a separator between every row instead (card style)
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

    public static String titledTop(char tl, char tr, char fill, String title, int width) {
        String label = " " + title + " ";
        int fillCount = Math.max(1, width - 3 - label.length());
        return tl + String.valueOf(fill) + label + String.valueOf(fill).repeat(fillCount) + tr;
    }

    public static String horizontalLine(char left, char fill, char right, int width) {
        return left + String.valueOf(fill).repeat(width - 2) + right;
    }

    public static String emptyLine(char v, int width) {
        return v + " ".repeat(width - 2) + v;
    }

    public static String centeredLine(char v, String text, int width) {
        return v + centerText(text, width - 2) + v;
    }

    private static String centerText(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        int totalPad = width - s.length();
        int left = totalPad / 2;
        int right = totalPad - left;
        return " ".repeat(left) + s + " ".repeat(right);
    }

    public static String spacedTitle(String s) {
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

    public static String padRight(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }

    public static List<String> wrapText(String text, int width) {
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
