package rengar.preprocess;

import rengar.parser.ast.*;

import java.util.List;

public class FetchLookaround {
    public void handle(RegexExpr regexExpr) {
        RegexExpr copyExpr = regexExpr.copy();
        extract(copyExpr);
        if (!copyExpr.equals(regexExpr))
            regexExpr.getExpr().addAll(copyExpr.getExpr());
    }

    private void extract(Expr expr) {
        switch (expr) {
            case RegexExpr regexExpr -> extract(regexExpr.getExpr());
            case BranchExpr branchExpr -> branchExpr.getBranchs().forEach(this::extract);
            case SequenceExpr seqExpr -> extractSequenceExpr(seqExpr);
            case GroupExpr groupExpr -> extract(groupExpr.getBody());
            case LoopExpr loopExpr -> extract(loopExpr.getBody());
            default -> {}
        }
    }

    private void extractSequenceExpr(SequenceExpr seqExpr) {
        int i = 0;
        while (i < seqExpr.getSize()) {
            Expr subExpr = seqExpr.get(i);
            if (subExpr instanceof LookaroundExpr lookaroundExpr) {
                extract(lookaroundExpr.getCond());
                Expr cond = getCondOf(lookaroundExpr);
                if (cond instanceof SequenceExpr condSequence) {
                    seqExpr.remove(i);
                    seqExpr.insert(i, condSequence);
                    i += condSequence.getSize();
                } else {
                    seqExpr.set(i, cond);
                    i += 1;
                }
            } else if (subExpr instanceof LoopExpr loopExpr) {
                extract(loopExpr.getBody());
                Expr loopBody = loopExpr.getBody();
                if (loopBody instanceof LookaroundExpr) {
                    seqExpr.set(i, loopBody);
                } else
                    i += 1;
            } else {
                extract(subExpr);
                i += 1;
            }
        }
    }

    private Expr getCondOf(LookaroundExpr lookaroundExpr) {
        RegexExpr cond = lookaroundExpr.getCond();
        List<SequenceExpr> branchs = cond.getExpr().getBranchs();
        if (branchs.size() != 1) {
            return new AnonymousGroupExpr(cond);
        } else {
            return branchs.get(0);
        }
    }
}
