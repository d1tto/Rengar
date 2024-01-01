package rengar.checker;

import rengar.base.*;
import rengar.checker.pattern.EODPattern;
import rengar.checker.pattern.ReDoSPattern;
import rengar.parser.ast.*;
import rengar.parser.range.*;
import java.util.*;

// EOD -> Exponential Overlapping Disjunction
// for example
//      ([^a]|\w)*
class EODPatternReDoSChecker {
    private RegexExpr regexExpr;
    private final List<ReDoSPattern> patterns = new ArrayList<>();

    public List<ReDoSPattern> getPatterns() {
        return patterns;
    }

    public void analyse(RegexExpr root) throws InterruptedException {
        this.regexExpr = root;
        topLevelVisitExpr(root);
    }

    // TOP-LEVEL function is used to process Loop-Group structure
    private void topLevelVisitExpr(Expr expr) throws InterruptedException {
        switch (expr) {
            case RegexExpr regexExpr -> topLevelVisitExpr(regexExpr.getExpr());
            case BranchExpr branchExpr -> {
                for (SequenceExpr seqExpr : branchExpr) {
                    if (Thread.currentThread().isInterrupted())
                        throw new InterruptedException();
                    topLevelVisitExpr(seqExpr);
                }
            }
            case SequenceExpr seqExpr -> {
                for (Expr subExpr : seqExpr) {
                    if (Thread.currentThread().isInterrupted())
                        throw new InterruptedException();
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

    private void secondLevelVisitExpr(Expr expr, LoopExpr topLoop)
            throws InterruptedException {
        switch (expr) {
            case RegexExpr regexExpr -> secondLevelVisitExpr(regexExpr.getExpr(), topLoop);
            case BranchExpr branchExpr -> secondLevelVisitBranchExpr(branchExpr, topLoop);
            default -> {}
        }
    }

    private void secondLevelVisitBranchExpr(BranchExpr branchExpr, LoopExpr topLoop)
            throws InterruptedException {
        if (topLoop.getMax() != -1 && topLoop.getMax() < 10)
            return;
        List<Result> results = getSatisfiedBranchs(branchExpr);
        for (Result result : results) {
            patterns.add(new EODPattern(
                    regexExpr, topLoop, topLoop,
                    result.branchP, result.branchQ,
                    null, null, result.condition)
            );
        }
        for (SequenceExpr seqExpr : branchExpr) {
            if (Thread.currentThread().isInterrupted())
                throw new InterruptedException();
            secondLevelVisitSequenceExpr(seqExpr, topLoop);
        }
    }

    private void secondLevelVisitSequenceExpr(SequenceExpr seqExpr, LoopExpr topLoop)
            throws InterruptedException {
        for (int i = 0; i < seqExpr.getSize(); i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            Expr subExpr = seqExpr.get(i);
            if (subExpr instanceof GroupExpr groupExpr) {
                List<Result> results = getSatisfiedBranchs(groupExpr.getBody().getExpr());
                if (results.size() == 0)
                    continue;
                SequenceExpr prefix = seqExpr.subSequence(0, i);
                SequenceExpr postfix = seqExpr.subSequence(i + 1, seqExpr.getSize());
                for (Result result : results) {
                    patterns.add(new EODPattern(regexExpr, topLoop, topLoop,
                            result.branchP, result.branchQ,
                            prefix, postfix, result.condition));
                }
            }
        }
    }

    private static class Result {
        SequenceExpr branchP;
        SequenceExpr branchQ;
        int condition;
        public Result(SequenceExpr branchP, SequenceExpr branchQ, int condition) {
            this.branchP = branchP;
            this.branchQ = branchQ;
            this.condition = condition;
        }
    }

    private List<Result> getSatisfiedBranchs(BranchExpr branchExpr)
            throws InterruptedException {
        List<Result> results = new LinkedList<>();
        for (int i = 0; i < branchExpr.getSize(); i++) {
            SequenceExpr branchP = branchExpr.get(i);
            for (int j = i + 1; j < branchExpr.getSize(); j++) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                SequenceExpr branchQ = branchExpr.get(j);
                int condition = isMeetCondition(branchP, branchQ);
                if (condition != -1)
                    results.add(new Result(branchP, branchQ, condition));
            }
        }
        return results;
    }

    private int isMeetCondition(Expr branchP, Expr branchQ) {
        // first(𝛽𝑝) ∩ first(𝛽𝑞) ≠ ∅
        CharRangeSet firstSetOfP = RegexAnalyzer.getFirstSet(branchP);
        CharRangeSet firstSetOfQ = RegexAnalyzer.getFirstSet(branchQ);
        if (!firstSetOfP.and(firstSetOfQ).isEmpty())
            return 1;
        // first(𝛽𝑝) ∩ (follow-last(𝛽𝑞) ∪ last(𝛽𝑞)) ≠ ∅
        CharRangeSet followLastSetOfQ = RegexAnalyzer.getFollowLastSet(branchQ);
        CharRangeSet lastSetOfQ = RegexAnalyzer.getLastSet(branchQ);

        CharRangeSet unionSet = followLastSetOfQ.union(lastSetOfQ);
        CharRangeSet andSet = firstSetOfP.and(unionSet);
        if (!andSet.isEmpty()) {
            // first(𝛽𝑝) ∩ last(𝛽𝑞)
            if (!firstSetOfP.and(lastSetOfQ).isEmpty())
                return 2;
            // first(𝛽𝑝) ∩ follow-last(𝛽𝑞)
            if (!firstSetOfP.and(followLastSetOfQ).isEmpty())
                return 3;
        }
        return -1;
    }
}
