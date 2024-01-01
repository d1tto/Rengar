package rengar.dynamic.profile;

import rengar.parser.charutil.CharUtil;

public class Suggestion {
    public int pos;
    public int c;
    public Suggestion(int pos, int c) {
        this.pos = pos;
        this.c = c;
    }

    @Override
    public String toString() {
        return String.format("(%d -> %s)", pos, CharUtil.toPrintableString(c));
    }
}
