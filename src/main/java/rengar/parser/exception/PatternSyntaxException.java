package rengar.parser.exception;

public class PatternSyntaxException extends Exception {
    private String msg;
    private String patternStr;
    private int index;
    public PatternSyntaxException(String msg, String patternStr, int index) {
        this.msg = msg;
        this.patternStr = patternStr;
        this.index = index;
    }
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(msg);
        sb.append(" near index ");
        sb.append(index - 1);
        sb.append(System.lineSeparator());
        sb.append(patternStr);
        if (index - 1 < patternStr.length()) {
            sb.append(System.lineSeparator());
            for (int i = 0; i < index; i++)
                sb.append(' ');
            sb.append('^');
        }
        return sb.toString();
    }
}