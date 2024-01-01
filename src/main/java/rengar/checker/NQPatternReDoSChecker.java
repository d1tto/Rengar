package rengar.checker;

import rengar.base.*;
import rengar.checker.pattern.NQPattern;
import rengar.checker.pattern.ReDoSPattern;
import rengar.checker.util.RegexUtil;
import rengar.parser.ast.*;
import java.util.*;

// NQ -> Nested Quantifiers
// for example
//      (\d+)*m
//      (ab+c)+   nullable(a) && nullable(c) == true
class NQPatternReDoSChecker {
    private RegexExpr regexExpr;
    private final List<ReDoSPattern> patterns = new ArrayList<>();

    public List<ReDoSPattern> getPatterns() {
        return patterns;
    }

    public void analyse(RegexExpr root) throws InterruptedException {
        this.regexExpr = root;
        RegexUtil.stripGroup(this.regexExpr);
        topLevelVisitExpr(root);
    }

    // TOP-LEVEL function is used to find the first Loop-Group structure
    private void topLevelVisitExpr(Expr expr) throws InterruptedException {
        switch (expr) {
            case RegexExpr regexExpr -> topLevelVisitExpr(regexExpr.getExpr());
            case BranchExpr branchExpr -> {
                for (Expr seqExpr : branchExpr.getBranchs()) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    topLevelVisitExpr(seqExpr);
                }
            }
            case SequenceExpr seqExpr -> {
                for (Expr subExpr : seqExpr.getExprs()) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    topLevelVisitExpr(subExpr);
                }
            }
            case GroupExpr groupExpr -> topLevelVisitExpr(groupExpr.getBody());
            case LoopExpr loopExpr -> {
                Expr loopBody = loopExpr.getBody();
                // we only process the loop-body that is rengar.parser.ast.GroupExpr
                if (!(loopBody instanceof GroupExpr groupExpr))
                    break;
                Expr groupBody = groupExpr.getBody();
                secondLevelVisitExpr(groupBody, loopExpr);
                topLevelVisitExpr(groupBody);
            }
            default -> {}
        }
    }

    // second level function is used to process Loop-Group's body(this is in second level)
    private void secondLevelVisitExpr(Expr expr, LoopExpr topLoop) throws InterruptedException {
        switch (expr) {
            case RegexExpr regexExpr -> secondLevelVisitExpr(regexExpr.getExpr(), topLoop);
            case BranchExpr branchExpr -> {
                for (Expr branch : branchExpr.getBranchs()) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    secondLevelVisitExpr(branch, topLoop);
                }
            }
            case SequenceExpr seqExpr -> secondLevelVisitSeqExpr(seqExpr, topLoop);
            default -> {}
        }
    }

    private void secondLevelVisitSeqExpr(SequenceExpr seqExpr, LoopExpr topLoop)
            throws InterruptedException {
        List<Expr> subExprs = seqExpr.getExprs();
        Expr prev = null, cur, next;
        for (int i = 0; i < subExprs.size(); i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            cur = subExprs.get(i);
            if (i + 1 >= subExprs.size())
                next = null;
            else
                next = subExprs.get(i + 1);
            if (cur instanceof LoopExpr && isSatisfyLoopTimes(topLoop, (LoopExpr)cur)) {
                if (prev == null) {
                    // when prev and next both equal to null, subExprArr.size() == 1
                    if (next == null || RegexAnalyzer.getNullable(next)) {
                        patterns.add(new NQPattern(regexExpr, topLoop,
                                topLoop, (LoopExpr)cur));
                    }
                } else {
                    if (RegexAnalyzer.getNullable(prev)) {
                        if (next == null || RegexAnalyzer.getNullable(next))
                            patterns.add(new NQPattern(regexExpr, topLoop,
                                    topLoop, (LoopExpr)cur));
                    }
                }
            } else if (cur instanceof GroupExpr) {
                topLevelVisitExpr(((GroupExpr)cur).getBody());
            }
            prev = cur;
        }
    }

    private boolean isSatisfyLoopTimes(LoopExpr topLoop, LoopExpr secondLoop) {
        int topMax = topLoop.getMax() == -1 ? 1000 : topLoop.getMax();
        int secondMax = secondLoop.getMax() == -1 ? 1000 : secondLoop.getMax();
        return topMax * secondMax >= 50;
    }

}