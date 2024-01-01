package rengar.checker;

import rengar.base.*;
import rengar.checker.pattern.EOAPattern;
import rengar.checker.pattern.ReDoSPattern;
import rengar.checker.util.RegexUtil;
import rengar.parser.ast.*;
import rengar.parser.range.*;
import java.util.*;

// EOA -> Exponential Overlapping Adjacent
// for example
//      ^(ab*b*)+c
//      ^(a+b+a+)+c
class EOAPatternReDoSChecker {
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

    // TOP-LEVEL function is used to find Loop-Group structure
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
                if (!(loopBody instanceof GroupExpr groupExpr))
                    break;
                Expr groupBody = groupExpr.getBody();
                secondLevelVisitExpr(groupBody, loopExpr);
                topLevelVisitExpr(groupBody);
            }
            default -> {}
        }
    }

    private void secondLevelVisitExpr(Expr expr, LoopExpr topLoop) throws InterruptedException {
        switch (expr) {
            case RegexExpr regexExpr -> secondLevelVisitExpr(regexExpr.getExpr(), topLoop);
            case SequenceExpr seqExpr -> secondLevelVisitSeqExpr(seqExpr, topLoop);
            case BranchExpr branchExpr -> {
                List<SequenceExpr> branchArr = branchExpr.getBranchs();
                for (Expr branch : branchArr) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    secondLevelVisitExpr(branch, topLoop);
                }
            }
            default -> {}
        }
    }

    private void secondLevelVisitSeqExpr(SequenceExpr seqExpr, LoopExpr topLoop)
            throws InterruptedException {
        List<Expr> subExprs = seqExpr.getExprs();
        if (subExprs.size() < 2)
            return;
        for (int i = 1; i < subExprs.size(); i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            SequenceExpr expr1 = getSubSeqExpr(seqExpr, 0, i - 1);
            SequenceExpr expr2 = getSubSeqExpr(seqExpr, i, subExprs.size() - 1);
            if (!RegexUtil.hasLoopExpr(expr1) || !RegexUtil.hasLoopExpr(expr2))
                continue;
            if (topLoop.getMax() != -1 && topLoop.getMax() < 10)
                continue;
            if (isMeetFirstCondition(expr1, expr2)) {
                patterns.add(new EOAPattern(regexExpr, topLoop, topLoop,
                        seqExpr, expr1, expr2, 1));
            }
            if (isMeetSecondCondition(expr1, expr2)) {
                patterns.add(new EOAPattern(regexExpr, topLoop, topLoop,
                        seqExpr, expr1, expr2,2));
            }
        }
    }

    // (follow-last(𝛽1) ∪ last(𝛽1)) ∩ first(𝛽2) ≠ ∅
    private boolean isMeetSecondCondition(Expr expr1, Expr expr2) {
        CharRangeSet followLastSetOf1 = RegexAnalyzer.getFollowLastSet(expr1);
        CharRangeSet lastSetOf1 = RegexAnalyzer.getLastSet(expr1);
        CharRangeSet firstSetOf2 = RegexAnalyzer.getFirstSet(expr2);

        CharRangeSet union = followLastSetOf1.union(lastSetOf1);
        CharRangeSet and = union.and(firstSetOf2);

        return !and.isEmpty();
    }

    // first(𝛽1) ∩ (follow-last(𝛽2) ∪ last(𝛽2)) ≠ ∅
    private boolean isMeetFirstCondition(Expr expr1, Expr expr2) {
        CharRangeSet firstSetOf1 = RegexAnalyzer.getFirstSet(expr1);
        CharRangeSet followLastSetOf2 = RegexAnalyzer.getFollowLastSet(expr2);
        CharRangeSet lastSetOf2 = RegexAnalyzer.getLastSet(expr2);

        CharRangeSet union = followLastSetOf2.union(lastSetOf2);
        CharRangeSet and = firstSetOf1.and(union);

        return !and.isEmpty();
    }

    private SequenceExpr getSubSeqExpr(SequenceExpr seqExpr, int begin, int end) {
        SequenceExpr out = new SequenceExpr();
        for (int i = begin; i <= end; i++) {
            out.add(seqExpr.getExprs().get(i));
        }
        return out;
    }

}
