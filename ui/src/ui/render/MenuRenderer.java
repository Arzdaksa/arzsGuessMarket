package ui.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ui.render.Layout.*;

// splash/exit screens, the main menu, and general-purpose prompt/message panels
public final class MenuRenderer {

    public enum PanelStyle { SUCCESS, ERROR, INFO }

    private MenuRenderer() {}

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

    // renders whatever options are currently registered, in insertion order
    public static String mainMenu(Map<String, String> options, boolean loaded, int eventCount, String warning) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        for (Map.Entry<String, String> option : options.entrySet()) {
            lines.add("  [" + option.getKey() + "] " + option.getValue());
        }
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
}
