package rengar.checker.util;

import rengar.parser.ast.*;
import java.util.ArrayList;
import java.util.List;

class FlattenRegex {
    public static BranchExpr flatten(Expr root) throws InterruptedException {
        long beginTime = System.currentTimeMillis();
        BranchExpr branchExpr = new BranchExpr();
        ArrayList<SequenceExpr> seqExprArr = visitExpr(root, beginTime);
        for (SequenceExpr seqExpr : seqExprArr) {
            branchExpr.add(seqExpr);
        }
        return branchExpr;
    }

    private static ArrayList<SequenceExpr> visitExpr(Expr expr, long beginTime)
            throws InterruptedException {
        return switch (expr) {
            case RegexExpr regexExpr -> visitExpr(regexExpr.getExpr(), beginTime);
            case BranchExpr branchExpr -> visitBranchExpr(branchExpr, beginTime);
            case SequenceExpr seqExpr -> visitSeqExpr(seqExpr, beginTime);
            default -> new ArrayList<>();
        };
    }

    private static ArrayList<SequenceExpr> visitBranchExpr(BranchExpr branchExpr, long beginTime)
            throws InterruptedException {
        ArrayList<SequenceExpr> outSeqExprArr = new ArrayList<>();
        for (Expr branch : branchExpr.getBranchs()) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            outSeqExprArr.addAll(visitExpr(branch, beginTime));
        }
        return outSeqExprArr;
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
        int first = findFirstExprContainingLoop(seqExpr);
        int last = findLastExprContainingLoop(seqExpr);
        // seqExpr doesn't have rengar.parser.ast.LoopExpr, we don't need to flatten
        if (first == -1 || last == -1) {
            out.add(seqExpr);
            return;
        }

        List<Expr> subExprs = seqExpr.getExprs();
        // this variable is used to pruning
        boolean isHitGroup = false;
        for (int i = begin; i < subExprs.size(); i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            if (isHitGroup)
                break;
            Expr subExpr = subExprs.get(i);

            if (subExpr instanceof GroupExpr groupExpr && i >= first && i <= last) {
                isHitGroup = true;
                ArrayList<SequenceExpr> choiceArr = visitExpr(groupExpr.getBody(), beginTime);
                for (SequenceExpr choice : choiceArr) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
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

    private static int findFirstExprContainingLoop(SequenceExpr seqExpr) throws InterruptedException {
        for (int i = 0; i < seqExpr.getExprs().size(); i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            Expr subExpr = seqExpr.getExprs().get(i);
            if (hasLoopExpr(subExpr))
                return i;
        }
        return -1;
    }

    private static int findLastExprContainingLoop(SequenceExpr seqExpr) throws InterruptedException {
        for (int i = seqExpr.getExprs().size() - 1; i >= 0; i--) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            Expr subExpr = seqExpr.getExprs().get(i);
            if (hasLoopExpr(subExpr))
                return i;
        }
        return -1;
    }

    private static boolean hasLoopExpr(Expr expr) {
        boolean has = false;
        switch (expr) {
            case LoopExpr loopExpr -> {
                has = true;
            }
            case GroupExpr groupExpr -> has = hasLoopExpr(groupExpr.getBody());
            case RegexExpr regexExpr -> {
                List<SequenceExpr> seqExprs = regexExpr.getExpr().getBranchs();
                ArrayList<SequenceExpr> loops = new ArrayList<>();
                for (SequenceExpr seqExpr : seqExprs) {
                    if (hasLoopExpr(seqExpr)) {
                        loops.add(seqExpr);
                        has = true;
                    }
                }
                if (loops.size() != 0) {
                    seqExprs.clear();
                    seqExprs.addAll(loops);
                }
            }
            case SequenceExpr seqExpr -> {
                for (Expr subExpr : seqExpr.getExprs()) {
                    has |= hasLoopExpr(subExpr);
                }
            }
            default -> {}
        }
        return has;
    }
}