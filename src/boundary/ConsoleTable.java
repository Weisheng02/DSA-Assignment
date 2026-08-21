package boundary;

/**
 * Author: Yeap Wei Sheng
 * Small shared helper that keeps console tables consistent across all UIs.
 */
final class ConsoleTable {

    private ConsoleTable() {
    }

    static void printHeader(String[] headings, int[] widths) {
        printBorder(widths);
        printRow(headings, widths);
        printBorder(widths);
    }

    static void printRow(String[] values, int[] widths) {
        StringBuilder row = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            String value = values != null && i < values.length && values[i] != null ? values[i] : "";
            row.append(' ').append(padOrTrim(value, widths[i])).append(" |");
        }
        System.out.println(row);
    }

    static void printFooter(int[] widths) {
        printBorder(widths);
    }

    private static void printBorder(int[] widths) {
        StringBuilder border = new StringBuilder("+");
        for (int width : widths) {
            for (int i = 0; i < width + 2; i++)
                border.append('-');
            border.append('+');
        }
        System.out.println(border);
    }

    private static String padOrTrim(String value, int width) {
        String text = value.replace('\n', ' ').replace('\r', ' ');
        if (text.length() > width)
            return width <= 2 ? text.substring(0, width) : text.substring(0, width - 2) + "..";
        StringBuilder padded = new StringBuilder(text);
        while (padded.length() < width)
            padded.append(' ');
        return padded.toString();
    }
}
