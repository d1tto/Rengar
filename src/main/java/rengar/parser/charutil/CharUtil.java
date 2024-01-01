package rengar.parser.charutil;

public class CharUtil {
    /**
     * this method is used to print the string regex compiler can receive
     * by escaping special characters in regex.
     * */
    public static String toRegexString(int codePoint) {
        if (Character.isLetterOrDigit(codePoint))
            return Character.toString(codePoint);
        String res;
        switch (codePoint) {
            case '\r' -> res = "\\r";
            case '\n' -> res = "\\n";
            case '\t' -> res = "\\t";
            case '\f' -> res = "\\f";
            case '\\' -> res = "\\\\";
            case '\'' -> res = "\\'";
            case '\"' -> res = "\\\"";
            case '(' -> res = "\\(";
            case ')' -> res = "\\)";
            case '[' -> res = "\\[";
            case '{' -> res = "\\{";
            case '}' -> res = "\\}";
            case '*' -> res = "\\*";
            case '+' -> res = "\\+";
            case '?' -> res = "\\?";
            case '.' -> res = "\\.";
            case '^' -> res = "\\^";
            case '$' -> res = "\\$";
            case '|' -> res = "\\|";
            default -> {
                if (codePoint >= 32 && codePoint < 127) {
                    res = Character.toString(codePoint);
                    break;
                }
                if (codePoint <= Character.MAX_VALUE) {
                    String tmp = Integer.toHexString(codePoint);
                    tmp = "0".repeat(4 - tmp.length()) + tmp;
                    res = "\\u" + tmp;
                } else {
                    res = String.format("\\x{%s}", Integer.toHexString(codePoint));
                }
            }
        }
        return res;
    }

    /**
     * this method used to print string in printable way
     * */
    public static String toPrintableString(int codePoint) {
        if (Character.isLetterOrDigit(codePoint))
            return Character.toString(codePoint);
        String res;
        switch (codePoint) {
            case '\r' -> res = "\\r";
            case '\n' -> res = "\\n";
            case '\t' -> res = "\\t";
            case '\f' -> res = "\\f";
            case '\"' -> res = "\"";
            default -> {
                if (codePoint >= 32 && codePoint < 127) {
                    res = Character.toString(codePoint);
                    break;
                }
                if (codePoint <= Character.MAX_VALUE) {
                    String tmp = Integer.toHexString(codePoint);
                    tmp = "0".repeat(4 - tmp.length()) + tmp;
                    res = "\\u" + tmp;
                } else {
                    res = String.format("\\x{%s}", Integer.toHexString(codePoint));
                }
            }
        }
        return res;
    }

    public static String toRegexString(int[] ints) {
        if (ints == null || ints.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int c : ints) {
            sb.append(toRegexString(c));
        }
        return sb.toString();
    }

    public static String toPrintableString(int[] ints) {
        if (ints == null || ints.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int c : ints) {
            sb.append(toPrintableString(c));
        }
        return sb.toString();
    }

    public static String toString(int[] ints) {
        if (ints == null || ints.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int c : ints)
            sb.append(Character.toString(c));
        return sb.toString();
    }
}
