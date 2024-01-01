package rengar.parser.ast;

import rengar.parser.charutil.*;

public class SingleCharExpr extends CharExpr {
    private int c;
    public SingleCharExpr(int c) {
        this.c = c;
    }
    public int getChar() {
        return c;
    }
    @Override
    public SingleCharExpr copy() {
        SingleCharExpr singleCharExpr = new SingleCharExpr(c);
        singleCharExpr.setStr(string);
        return singleCharExpr;
    }

    @Override
    public String genString() {
        return CharUtil.toRegexString(c);
    }
}