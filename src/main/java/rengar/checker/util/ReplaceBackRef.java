package rengar.checker.util;

import rengar.parser.ast.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReplaceBackRef {
    private final Map<Integer, RegexExpr> indexToRegexExpr = new HashMap<>();
    private final Map<String, RegexExpr> nameToRegexExpr = new HashMap<>();

    public void replace(Expr expr) {
        initRecord(expr);
        replaceHelper(expr);
    }

    private void replaceHelper(Expr expr) {
        switch (expr) {
            case RegexExpr regexExpr -> replaceHelper(regexExpr.getExpr());
            case BranchExpr branchExpr -> branchExpr.getBranchs().forEach(this::replaceHelper);
            case SequenceExpr seqExpr -> {
                List<Expr> subExprs = seqExpr.getExprs();
                for (int i = 0; i < subExprs.size(); i++) {
                    Expr subExpr = subExprs.get(i);
                    if (subExpr instanceof BackRefExpr backRefExpr) {
                        RegexExpr body = getRefOf(backRefExpr);
                        subExprs.set(i, new AnonymousGroupExpr(body));
                    } else if (subExpr instanceof LoopExpr loopExpr) {
                        if (loopExpr.getBody() instanceof BackRefExpr backRefExpr) {
                            RegexExpr body = getRefOf(backRefExpr);
                            loopExpr.setBody(body);
                        } else
                            replaceHelper(loopExpr);
                    } else {
                        replaceHelper(subExpr);
                    }
                }
            }
            case GroupExpr groupExpr -> replaceHelper(groupExpr.getBody());
            case LoopExpr loopExpr -> replaceHelper(loopExpr.getBody());
            case LookaroundExpr lookaroundExpr -> replaceHelper(lookaroundExpr.getCond());
            default -> {}
        }
    }

    private RegexExpr getRefOf(BackRefExpr backRefExpr) {
        return switch (backRefExpr) {
            case NamedBackRefExpr nbre -> nameToRegexExpr.get(nbre.getName());
            case RegularBackRefExpr rbre -> indexToRegexExpr.get(rbre.getIndex());
            default -> null;
        };
    }

    private void initRecord(Expr expr) {
        switch (expr) {
            case RegexExpr regexExpr -> initRecord(regexExpr.getExpr());
            case BranchExpr branchExpr -> branchExpr.getBranchs().forEach(this::initRecord);
            case SequenceExpr seqExpr -> seqExpr.getExprs().forEach(this::initRecord);
            case GroupExpr groupExpr -> {
                switch (groupExpr) {
                    case RegularGroupExpr rge -> {
                        indexToRegexExpr.put(rge.getIndex(), rge.getBody());
                    }
                    case NamedGroupExpr nge -> {
                        nameToRegexExpr.put(nge.getName(), nge.getBody());
                        indexToRegexExpr.put(nge.getIndex(), nge.getBody());
                    }
                    default -> {}
                }
                initRecord(groupExpr.getBody());
            }
            case LoopExpr loopExpr -> initRecord(loopExpr.getBody());
            case LookaroundExpr lookaroundExpr -> initRecord(lookaroundExpr.getCond());
            default -> {}
        }
    }
}
