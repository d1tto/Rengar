package rengar.preprocess;

import rengar.parser.ast.*;
import java.util.List;

/**
 * handle \b
 * */
public class HandleWordBoundary {
    public void handle(Expr expr) {
        switch (expr) {
            case RegexExpr regexExpr -> handle(regexExpr.getExpr());
            case BranchExpr branchExpr -> branchExpr.getBranchs().forEach(this::handle);
            case SequenceExpr seqExpr -> handleSequenceExpr(seqExpr);
            case LoopExpr loopExpr -> handle(loopExpr.getBody());
            case GroupExpr groupExpr -> handle(groupExpr.getBody());
            case LookaroundExpr lookaroundExpr -> handle(lookaroundExpr.getCond());
            default -> {}
        }
    }

    private void handleSequenceExpr(SequenceExpr seqExpr) {
        List<Expr> subExprs = seqExpr.getExprs();
        for (int i = 0; i < subExprs.size(); i++) {
            Expr subExpr = subExprs.get(i);
            if (subExpr instanceof WordBoundaryExpr) {

            }
        }
    }
}
