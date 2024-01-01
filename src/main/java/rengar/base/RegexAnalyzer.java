package rengar.base;

import rengar.parser.ast.*;
import rengar.parser.range.*;

public class RegexAnalyzer {
    public static boolean getNullable(Expr root) {
        RegexNullable nullable = new RegexNullable(root);
        return nullable.visitExpr(root);
    }

    public static CharRangeSet getFirstSet(Expr root) {
        RegexFirstSet firstSet = new RegexFirstSet(root);
        return firstSet.visitExpr(root).rangeSet;
    }

    public static CharRangeSet getFollowLastSet(Expr root) {
        RegexFollowLastSet followLastSet = new RegexFollowLastSet(root);
        return followLastSet.visitExpr(root);
    }

    public static CharRangeSet getLastSet(Expr root) {
        RegexLastSet lastSet = new RegexLastSet(root);
        return lastSet.visitExpr(root).rangeSet;
    }

    public static CharRangeSet getAlphabet(Expr root) {
        RegexAlphabet alphabet = new RegexAlphabet(root);
        return alphabet.visitExpr(root);
    }
}
