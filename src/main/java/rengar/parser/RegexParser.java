package rengar.parser;

import rengar.parser.ast.*;
import rengar.parser.exception.*;

public abstract class RegexParser {
    protected String patternStr;
    protected int buffer[];
    protected int patternLength; // number of element of buffer. not buffer size.

    protected int groupCount; // number of groups that have been parsed

    protected int pos = 0; // current position in buffer

    public enum Language {
        Java, Python, JavaScript
    }

    public static RegexParser createParser(Language lanType, String patternStr) {
        return switch (lanType) {
            case Java -> new JavaRegexParser(patternStr);
            case Python -> null;
            case JavaScript -> null;
        };
    }

    protected RegexParser(String patternStr) {
        this.patternStr = patternStr;
        patternLength = patternStr.length();
        buffer = new int[patternLength + 2];
        int c, count = 0;
        for (int i = 0; i < patternLength; i += Character.charCount(c)) {
            c = patternStr.codePointAt(i);
            buffer[count++] = c;
        }
        patternLength = count;
        pos = 0;
        groupCount = 1;
    }

    public abstract RegexExpr parse() throws PatternSyntaxException;

    protected boolean isHitEnd() {
        return pos >= patternLength;
    }

    protected boolean isHitEnd(int index) {
        return index >= patternLength;
    }

    protected int peek() {
        if (isHitEnd())
            return 0;
        return buffer[pos];
    }

    protected void read() {
        pos++;
    }

    protected void unread() {
        pos--;
    }

    protected PatternSyntaxException error(String msg) {
        return new PatternSyntaxException(msg, patternStr, pos);
    }

    protected void expect(int c, String msg) throws PatternSyntaxException {
        int p = peek();
        if (p != c)
            throw error(msg);
        else
            read();
    }

    protected int parseInteger() {
        StringBuilder numStr = new StringBuilder();
        while (Character.isDigit(peek())) {
            numStr.append(Character.toString(peek()));
            read();
        }
        return Integer.parseInt(numStr.toString());
    }

    protected boolean isHexDigit(int c) {
        if (Character.isDigit(c))
            return true;
        if (c >= 'a' && c <= 'f')
            return true;
        if (c >= 'A' && c <= 'F')
            return true;
        return false;
    }

    protected int hexToNumber(int c) {
        if (Character.isDigit(c))
            return c - '0';
        if (c >= 'a' && c <= 'f')
            return c - 'a' + 10;
        if (c >= 'A' && c <= 'F')
            return c - 'A' + 10;
        return -1;
    }
}