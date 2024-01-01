package rengar.preprocess;

import rengar.parser.ast.*;

import java.util.List;

/**
 * there is no back-reference in the regex we need process
 * */
public class HandleGroup {
    public void handle(Expr expr) {
        switch (expr) {
            case RegexExpr regexExpr -> handle(regexExpr.getExpr());
            case BranchExpr branchExpr -> branchExpr.getBranchs().forEach(this::handle);
            case SequenceExpr seqExpr -> handleSequenceExpr(seqExpr);
            case GroupExpr groupExpr -> handle(groupExpr.getBody());
            case LoopExpr loopExpr -> handleLoopExpr(loopExpr);
            case LookaroundExpr lookaroundExpr -> handle(lookaroundExpr.getCond());
            default -> {}
        }
    }

    private void handleLoopExpr(LoopExpr loopExpr) {
        handle(loopExpr.getBody());
        Expr loopBody = loopExpr.getBody();
        if (loopBody instanceof GroupExpr groupExpr) {
            List<SequenceExpr> branchs = groupExpr.getBody().getExpr().getBranchs();
            if (branchs.size() != 1)
                return;
            SequenceExpr branch = branchs.get(0);
            if (branch.getSize() != 1)
                return;
            Expr subExpr = branch.getExprs().get(0);
            if (subExpr instanceof CharExpr) {
                loopExpr.setBody(subExpr);
            }
        }
    }

    private void handleSequenceExpr(SequenceExpr seqExpr) {
        int i = 0;
        while (i < seqExpr.getSize()) {
            Expr subExpr = seqExpr.get(i);
            if (subExpr instanceof GroupExpr groupExpr) {
                handle(groupExpr);
                Expr body = extract(groupExpr);
                if (body instanceof SequenceExpr bodySequence) {
                    seqExpr.remove(i);
                    seqExpr.insert(i, bodySequence);
                    i += bodySequence.getSize();
                    continue;
                }
            } else
                handle(subExpr);
            i += 1;
        }
    }

    private Expr extract(GroupExpr groupExpr) {
        RegexExpr body = groupExpr.getBody();
        List<SequenceExpr> branchs = body.getExpr().getBranchs();
        // if the number of branch in the body is more than one, we can't extract
        if (branchs.size() > 1)
            return groupExpr;
        return branchs.get(0);
    }
}
