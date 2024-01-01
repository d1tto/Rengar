package rengar.checker.util;

import rengar.parser.ast.*;
import java.util.*;

public class ExtractInnerLoop {
    public static RegexExpr extract(Expr expr) throws InterruptedException {
        BranchExpr branchExpr = new BranchExpr();
        long beginTime = System.currentTimeMillis();
        visitExpr(expr, beginTime).forEach(branchExpr::add);
        return new RegexExpr(branchExpr);
    }

    private static List<SequenceExpr> visitExpr(Expr expr, long beginTime)
            throws InterruptedException {
        List<SequenceExpr> out;
        switch (expr) {
            case RegexExpr regexExpr -> out = visitExpr(regexExpr.getExpr(), beginTime);
            case BranchExpr branchExpr -> {
                out = new ArrayList<>();
                for (SequenceExpr seqExpr : branchExpr) {
                    if (Thread.currentThread().isInterrupted())
                        throw new InterruptedException();
                    out.addAll(visitExpr(seqExpr, beginTime));
                }
            }
            case SequenceExpr seqExpr -> out = visitSeqExpr(seqExpr, beginTime);
            default -> out = new ArrayList<>();
        }
        return out;
    }

    private static ArrayList<SequenceExpr> visitSeqExpr(SequenceExpr seqExpr, long beginTime)
            throws InterruptedException {
        ArrayList<SequenceExpr> out = new ArrayList<>();
        SequenceExpr newBranch = new SequenceExpr();
        processSeqExpr(seqExpr, 0, newBranch, out, beginTime);
        return out;
    }

    private static void processSeqExpr(SequenceExpr seqExpr,
                                       int begin,
                                       SequenceExpr newBranch,
                                       ArrayList<SequenceExpr> out,
                                       long beginTime
    ) throws InterruptedException {
        List<Expr> subExprs = seqExpr.getExprs();
        // this variable is used to pruning
        boolean isHitGroup = false;
        for (int i = begin; i < subExprs.size(); i++) {
            if (Thread.currentThread().isInterrupted())
                throw new InterruptedException();
            if (isHitGroup)
                break;
            Expr subExpr = subExprs.get(i);

            if (subExpr instanceof LoopExpr loopExpr
                    && loopExpr.getBody() instanceof GroupExpr groupExpr
                    && RegexUtil.hasLoopExpr(groupExpr)) {
                isHitGroup = true;
                List<SequenceExpr> choices = visitExpr(groupExpr.getBody(), beginTime);
                choices.removeIf(e -> !RegexUtil.hasLoopExpr(e));
                SequenceExpr tmpSeqExpr = new SequenceExpr();
                tmpSeqExpr.add(loopExpr);
                choices.add(tmpSeqExpr);
                for (SequenceExpr choice : choices) {
                    SequenceExpr backup = newBranch.copy();
                    mergeSeqExpr(newBranch, choice);
                    processSeqExpr(seqExpr, i + 1, newBranch, out, beginTime);
                    newBranch = backup;
                }
            } else {
                newBranch.add(subExpr);
            }
        }
        if (!isHitGroup)
            out.add(newBranch);
    }

    private static void mergeSeqExpr(SequenceExpr self, SequenceExpr add) {
        self.getExprs().addAll(add.getExprs());
    }

    private static List<SequenceExpr> extractGroup(GroupExpr groupExpr) throws InterruptedException {
        return extractGroupHelper(groupExpr.getBody());
    }

    private static List<SequenceExpr> extractGroupHelper(Expr expr) throws InterruptedException {
        List<SequenceExpr> out = new ArrayList<>();
        switch (expr) {
            case RegexExpr regexExpr -> out = extractGroupHelper(regexExpr.getExpr());
            case BranchExpr branchExpr -> {
                for (SequenceExpr seqExpr : branchExpr.getBranchs()) {
                    if (Thread.currentThread().isInterrupted())
                        throw new InterruptedException();
                    if (RegexUtil.hasLoopExpr(seqExpr))
                        out.add(seqExpr);
                }
            }
            default -> {}
        }
        return out;
    }
}
