package rengar.parser.ast;

import rengar.parser.charutil.*;
import rengar.parser.range.*;

public class CharRangeExpr extends CharExpr {
    private CharRangeSet rangeSet = new CharRangeSet();
    private boolean neg = false;

    public void addRange(int begin, int end) {
        rangeSet.addRange(begin, end);
    }

    public void addRange(int[]... arr) {
        rangeSet.addRange(arr);
    }

    public void union(CharExpr expr) {
        if (expr instanceof CharRangeExpr charRangeExpr) {
            rangeSet.addRangeSet(charRangeExpr.rangeSet);
        } else if (expr instanceof SingleCharExpr singleCharExpr) {
            rangeSet.addOneChar(singleCharExpr.getChar());
        }
    }

    public void addOneChar(int... cs) {
        for (int c : cs)
            rangeSet.addOneChar(c);
    }

    public CharRangeSet getRangeSet() {
        return rangeSet;
    }

    public void negate() {
        neg = true;
        rangeSet = rangeSet.negate();
    }

    public boolean isNeg() {
        return neg;
    }

    public CharRangeExpr copy() {
        CharRangeExpr newCharRangeExpr = new CharRangeExpr();
        newCharRangeExpr.rangeSet = rangeSet.copy();
        newCharRangeExpr.neg = neg;
        newCharRangeExpr.setStr(string);
        return newCharRangeExpr;
    }

    public void setRangeSet(CharRangeSet rangeSet) {
        this.rangeSet = rangeSet;
    }

    @Override
    public String genString() {
        if (string != null && string.equals("."))
            return ".";
        CharRangeSet tmp = rangeSet;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        if (neg) {
            sb.append('^');
            tmp = rangeSet.negate();
        }
        for (CharRange range : tmp.getRanges()) {
            if (range.isSingleChar()) {
                int c = range.getSingleChar();
                sb.append(CharUtil.toRegexString(c));
            } else {
                sb.append(CharUtil.toRegexString(range.begin));
                sb.append('-');
                sb.append(CharUtil.toRegexString(range.end));
            }
        }
        sb.append(']');
        return sb.toString();
    }
}
