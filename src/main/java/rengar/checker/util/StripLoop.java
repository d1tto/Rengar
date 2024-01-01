package rengar.checker.util;

import rengar.parser.ast.*;
import java.util.List;

class StripLoop {
    public static Expr strip(Expr root, Expr breakExpr) {
        replace(root, breakExpr);
        return root;
    }
    private static void replace(Expr root, Expr breakExpr) {
        switch (root) {
            case RegexExpr regexExpr -> replace(regexExpr.getExpr(), breakExpr);
            case BranchExpr branchExpr -> {
                for (Expr branch : branchExpr.getBranchs())
                    replace(branch, breakExpr);
            }
            case SequenceExpr seqExpr -> {
                if (seqExpr.equals(breakExpr))
                    return;
                List<Expr> subExprs = seqExpr.getExprs();
                for (int i = 0; i < subExprs.size(); i++) {
                    Expr subExpr = subExprs.get(i);
                    if (subExpr.equals(breakExpr))
                        continue;
                    if (subExpr instanceof LoopExpr loopExpr) {
                        if (loopExpr.getBody() instanceof GroupExpr groupExpr) {
                            replace(groupExpr.getBody(), breakExpr);
                            subExprs.set(i, groupExpr);
                            replace(groupExpr, breakExpr);
                        }
                    } else
                        replace(subExpr, breakExpr);
                }
            }
            case GroupExpr groupExpr -> replace(groupExpr.getBody(), breakExpr);
            default -> {}
        }
    }
}