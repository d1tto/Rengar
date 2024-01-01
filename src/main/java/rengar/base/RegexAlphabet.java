package rengar.base;

import rengar.parser.ast.*;
import rengar.parser.range.*;
import java.util.HashMap;

public class RegexAlphabet {
    private Expr root;

    private final HashMap<Integer, CharRangeSet> regularGroupMap = new HashMap<>();
    private final HashMap<String, CharRangeSet> namedGroupMap = new HashMap<>();

    public RegexAlphabet(Expr root) {
        this.root = root;
    }

    public CharRangeSet visitExpr(Expr expr) {
        return switch (expr) {
            case RegexExpr regexExpr -> visitExpr(regexExpr.getExpr());
            case BranchExpr branchExpr -> visitBranchExpr(branchExpr);
            case GroupExpr groupExpr -> visitGroupExpr(groupExpr);
            case BackRefExpr backRefExpr -> visitBackRefExpr(backRefExpr);
            case SequenceExpr seqExpr -> visitSequenceExpr(seqExpr);
            case LoopExpr loopExpr -> visitExpr(loopExpr.getBody());
            case CharExpr charExpr -> visitCharExpr(charExpr);
            default -> new CharRangeSet();
        };
    }

    CharRangeSet visitBranchExpr(BranchExpr branchExpr) {
        CharRangeSet rangeSet = new CharRangeSet();
        for (Expr branch : branchExpr.getBranchs()) {
            rangeSet.addRangeSet(visitExpr(branch));
        }
        return rangeSet;
    }

    CharRangeSet visitGroupExpr(GroupExpr expr) {
        CharRangeSet rangeSet = visitExpr(expr.getBody());
        switch (expr) {
            case RegularGroupExpr regularGroupExpr -> {
                regularGroupMap.put(regularGroupExpr.getIndex(), rangeSet);
            }
            case NamedGroupExpr namedGroupExpr -> {
                namedGroupMap.put(namedGroupExpr.getName(), rangeSet);
                regularGroupMap.put(namedGroupExpr.getIndex(), rangeSet);
            }
            default -> {}
        }
        return rangeSet;
    }

    CharRangeSet visitBackRefExpr(BackRefExpr expr) {
        return switch (expr) {
            case RegularBackRefExpr regularBackRefExpr ->
                    regularGroupMap.get(regularBackRefExpr.getIndex());
            case NamedBackRefExpr namedBackRefExpr ->
                    namedGroupMap.get(namedBackRefExpr.getName());
            default -> new CharRangeSet();
        };
    }

    CharRangeSet visitSequenceExpr(SequenceExpr seqExpr) {
        CharRangeSet rangeSet = new CharRangeSet();
        for (Expr subExpr : seqExpr.getExprs()) {
            rangeSet.addRangeSet(visitExpr(subExpr));
        }
        return rangeSet;
    }

    CharRangeSet visitCharExpr(CharExpr charExpr) {
        CharRangeSet rangeSet = new CharRangeSet();
        switch (charExpr) {
            case SingleCharExpr singleCharExpr -> {
                rangeSet.addOneChar(singleCharExpr.getChar());
            }
            case CharRangeExpr charRangeExpr -> {
                rangeSet.addRangeSet(charRangeExpr.getRangeSet());
            }
            default -> {}
        }
        return rangeSet;
    }

}
