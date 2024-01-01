package rengar.base;

import rengar.parser.ast.*;
import rengar.parser.range.*;
import java.util.HashMap;
import java.util.List;

public class RegexLastSet {
    Expr root;

    HashMap<Integer, Private> regularGroupMap = new HashMap<>();
    HashMap<String, Private> namedGroupMap = new HashMap<>();

    public RegexLastSet(Expr root) {
        this.root = root;
    }

    public void debug() {
        Private data = visitExpr(root);
        System.out.println(data);
    }

    static class Private {
        public boolean isEmptyOk;
        public CharRangeSet rangeSet = new CharRangeSet();

        @Override
        public String toString() {
            return String.format("canBeEmpty: %b, lastCharSet: %s",
                    isEmptyOk,
                    rangeSet.toString()
            );
        }
    }

    Private visitExpr(Expr expr) {
        return switch (expr) {
            case RegexExpr regexExpr -> visitExpr(regexExpr.getExpr());
            case SingleCharExpr singleCharExpr -> visitOneCharExpr(singleCharExpr);
            case CharRangeExpr charRangeExpr -> visitCharRangeExpr(charRangeExpr);
            case BranchExpr branchExpr -> visitBranchExpr(branchExpr);
            case SequenceExpr seqExpr -> visitSequenceExpr(seqExpr);
            case LoopExpr loopExpr -> visitLoopExpr(loopExpr);
            case GroupExpr groupExpr -> visitGroupExpr(groupExpr);
            case BackRefExpr backRefExpr -> visitBackRefExpr(backRefExpr);
            default -> visitExprDefault(expr);
        };
    }

    Private visitExprDefault(Expr expr) {
        Private data = new Private();
        data.isEmptyOk = true;
        return data;
    }

    Private visitOneCharExpr(SingleCharExpr expr) {
        Private data = new Private();
        data.isEmptyOk = false;
        data.rangeSet.addOneChar(expr.getChar());
        return data;
    }

    Private visitCharRangeExpr(CharRangeExpr expr) {
        Private data = new Private();
        data.isEmptyOk = false;
        data.rangeSet.addRangeSet(expr.getRangeSet());
        return data;
    }

    Private visitBranchExpr(BranchExpr expr) {
        Private data = new Private();

        boolean isEmptyOk = false;
        for (Expr body : expr.getBranchs()) {
            Private bodyData = visitExpr(body);
            if (bodyData.isEmptyOk)
                isEmptyOk = true;
            data.rangeSet.addRangeSet(bodyData.rangeSet);
        }

        data.isEmptyOk = isEmptyOk;
        return data;
    }

    Private visitSequenceExpr(SequenceExpr expr) {
        Private data = new Private();
        boolean prevIsEmptyOk = true;
        // the only difference from the first set. we traverse the sequence backwards.
        List<Expr> subExprs = expr.getExprs();
        for (int i = subExprs.size() - 1; i >= 0; i--) {
            Expr subExpr = subExprs.get(i);
            Private subExprData = visitExpr(subExpr);
            if (prevIsEmptyOk)
                data.rangeSet.addRangeSet(subExprData.rangeSet);
            else
                break;
            if (subExprData.isEmptyOk)
                prevIsEmptyOk = true;
            else
                prevIsEmptyOk = false;
        }
        data.isEmptyOk = prevIsEmptyOk;
        return data;
    }

    Private visitLoopExpr(LoopExpr expr) {
        Private data = new Private();
        data.isEmptyOk = false;
        if (expr.getMin() == 0)
            data.isEmptyOk = true;
        Private loopBodyData = visitExpr(expr.getBody());
        data.rangeSet.addRangeSet(loopBodyData.rangeSet);
        return data;
    }

    Private visitGroupExpr(GroupExpr expr) {
        Private data = visitExpr(expr.getBody());
        switch (expr) {
            case RegularGroupExpr regularGroupExpr -> {
                regularGroupMap.put(regularGroupExpr.getIndex(), data);
            }
            case NamedGroupExpr namedGroupExpr -> {
                namedGroupMap.put(namedGroupExpr.getName(), data);
                regularGroupMap.put(namedGroupExpr.getIndex(), data);
            }
            default -> {}
        }
        return data;
    }

    Private visitBackRefExpr(BackRefExpr expr) {
        return switch (expr) {
            case RegularBackRefExpr regularBackRefExpr ->
                    regularGroupMap.get(regularBackRefExpr.getIndex());
            case NamedBackRefExpr namedBackRefExpr ->
                    namedGroupMap.get(namedBackRefExpr.getName());
            default -> null;
        };
    }
}
