package rengar.base;

import rengar.parser.ast.*;
import rengar.parser.range.*;
import java.util.*;
import java.util.HashMap;

public class RegexFollowLastSet {
    Expr root;

    HashMap<Integer, CharRangeSet> regularGroupMap = new HashMap<>();

    HashMap<String, CharRangeSet> namedGroupMap = new HashMap<>();

    public RegexFollowLastSet(Expr root) {
        this.root = root;
    }

    public void debug() {
        CharRangeSet data = visitExpr(root);
        System.out.println(data);
    }

    CharRangeSet visitExpr(Expr expr) {
        return switch (expr) {
            case RegexExpr regexExpr -> visitExpr(regexExpr.getExpr());
            case BranchExpr branchExpr -> visitBranchExpr(branchExpr);
            case SequenceExpr seqExpr -> visitSequenceExpr(seqExpr);
            case LoopExpr loopExpr -> visitLoopExpr(loopExpr);
            case GroupExpr groupExpr -> visitGroupExpr(groupExpr);
            case BackRefExpr backRefExpr -> visitBackRefExpr(backRefExpr);
            default -> new CharRangeSet();
        };
    }

    CharRangeSet visitSequenceExpr(SequenceExpr expr) {
        List<Expr> subExprs = expr.getExprs();

        Expr curExpr = null, nextExpr = null;
        CharRangeSet prevFollowLastSet = new CharRangeSet();
        CharRangeSet curFollowLastSet = new CharRangeSet();

        if (subExprs.size() == 1) {
            prevFollowLastSet = visitExpr(subExprs.get(0));
        }

        for (int i = 0; i < subExprs.size() - 1; i += 2) {
            curExpr = subExprs.get(i);
            nextExpr = subExprs.get(i + 1);
            boolean nextExprIsNullable = RegexAnalyzer.getNullable(nextExpr);
            if (nextExprIsNullable) {
                curFollowLastSet.addRangeSet(prevFollowLastSet);
                CharRangeSet nextExprFirstSet = RegexAnalyzer.getFirstSet(nextExpr);
                curFollowLastSet.addRangeSet(nextExprFirstSet);
                curFollowLastSet.addRangeSet(visitExpr(nextExpr));
            } else {
                curFollowLastSet.addRangeSet(visitExpr(nextExpr));
            }
            prevFollowLastSet = curFollowLastSet;
            curFollowLastSet = new CharRangeSet();
        }
        return prevFollowLastSet;
    }

    CharRangeSet visitBranchExpr(BranchExpr expr) {
        CharRangeSet data = new CharRangeSet();
        for (Expr branch : expr.getBranchs()) {
            CharRangeSet branchData = visitExpr(branch);
            data.addRangeSet(branchData);
        }
        return data;
    }

    CharRangeSet visitGroupExpr(GroupExpr expr) {
        CharRangeSet data = visitExpr(expr.getBody());
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

    CharRangeSet visitBackRefExpr(BackRefExpr expr) {
        CharRangeSet rangeSet = switch (expr) {
            case RegularBackRefExpr regularBackRefExpr ->
                    regularGroupMap.get(regularBackRefExpr.getIndex());
            case NamedBackRefExpr namedBackRefExpr ->
                    namedGroupMap.get(namedBackRefExpr.getName());
            default -> null;
        };
        if (rangeSet == null)
            return new CharRangeSet();
        return rangeSet;
    }

    CharRangeSet visitLoopExpr(LoopExpr expr) {
        CharRangeSet data = new CharRangeSet();
        Expr loopBodyExpr = expr.getBody();
        CharRangeSet loopBodyData = visitExpr(loopBodyExpr);
        data.addRangeSet(loopBodyData);
        // * or +
        if (expr.getMax() == -1) {
            CharRangeSet firstSet = RegexAnalyzer.getFirstSet(loopBodyExpr);
            data.addRangeSet(firstSet);
        }
        return data;
    }

}
