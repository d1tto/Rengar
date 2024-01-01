package rengar.checker;

import rengar.base.*;
import rengar.checker.pattern.POAPattern;
import rengar.checker.pattern.ReDoSPattern;
import rengar.checker.util.*;
import rengar.config.GlobalConfig;
import rengar.parser.ast.*;
import rengar.parser.range.*;
import java.util.*;

// POA -> Polynomial Overlapping Adjacent
class POAPatternReDoSChecker {
    private RegexExpr regexExpr;
    private final List<ReDoSPattern> patterns = new ArrayList<>();
    public List<ReDoSPattern> getPatterns() {
        return patterns;
    }

    public void analyse(RegexExpr preprocess) throws InterruptedException {
        regexExpr = preprocess.copy();
        topLevelVisitExpr(regexExpr);
    }

    private void topLevelVisitExpr(Expr root)
            throws InterruptedException {
        switch (root) {
            case RegexExpr regexExpr -> topLevelVisitExpr(regexExpr.getExpr());
            case BranchExpr branchExpr -> {
                for (Expr branch : branchExpr.getBranchs()) {
                    if (Thread.currentThread().isInterrupted())
                        throw new InterruptedException();
                    topLevelVisitExpr(branch);
                }
            }
            case SequenceExpr seqExpr -> secondLevelVisitSeqExpr(seqExpr);
            case GroupExpr groupExpr -> topLevelVisitExpr(groupExpr.getBody());
            case LoopExpr loopExpr -> topLevelVisitExpr(loopExpr.getBody());
            default -> {}
        }
    }

    private int times = 0;
    private void secondLevelVisitSeqExpr(SequenceExpr seqExpr)
            throws InterruptedException {
        class LoopInfo {
            final int index;
            final LoopExpr loopExpr;
            public LoopInfo(int index, LoopExpr loopExpr) {
                this.index = index;
                this.loopExpr = loopExpr;
            }
        }
        List<LoopInfo> loopInfoList = new ArrayList<>();

        List<Expr> subExprs = seqExpr.getExprs();
        for (int i = 0; i < subExprs.size(); i++) {
            if (Thread.currentThread().isInterrupted())
                throw new InterruptedException();
            Expr expr = subExprs.get(i);
            if (expr instanceof LoopExpr loopExpr) {
                int max = loopExpr.getMax();
                if (max != -1 && max <= 50)
                    continue;
                topLevelVisitExpr(loopExpr);
                loopInfoList.add(new LoopInfo(i, loopExpr));
            }
        }

        class Pair {
            final LoopInfo info1;
            final LoopInfo info2;
            final int distance;
            public Pair(LoopInfo info1, LoopInfo info2, int distance) {
                this.info1 = info1;
                this.info2 = info2;
                this.distance = distance;
            }
        }

        List<Pair> pairs = new ArrayList<>();
        for (int i = 0; i < loopInfoList.size(); i++) {
            for (int j = i + 1; j < loopInfoList.size(); j++) {
                LoopInfo info1 = loopInfoList.get(i);
                LoopInfo info2 = loopInfoList.get(j);
                pairs.add(new Pair(info1, info2, info2.index - info1.index));
            }
        }
        pairs.sort(Comparator.comparingInt(info -> info.distance));

        for (Pair value : pairs) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            times += 1;
            if (times > 100)
                break;
            int indexOfCur = value.info1.index;
            LoopExpr curLoopExpr = value.info1.loopExpr;

            int indexOfOther = value.info2.index;
            LoopExpr otherLoopExpr = value.info2.loopExpr;

            boolean isOk = false;
            int condition = 0;
            SequenceExpr expr2 = null;
            // adjacent
            if (indexOfOther - indexOfCur == 1) {
                if (isMeetAdjacentCondition(curLoopExpr, otherLoopExpr)) {
                    condition = 1;
                    isOk = true;
                }
            } else {
                expr2 = RegexUtil.getSubSeqExpr(seqExpr,
                        indexOfCur + 1, indexOfOther - 1);
                if (RegexAnalyzer.getNullable(expr2)) {
                    if (isMeetAdjacentCondition(curLoopExpr, otherLoopExpr)) {
                        condition = 2;
                        isOk = true;
                    }
                } else {
                    if (GlobalConfig.option.isWeakPatternCheck()) {
                        if (isMeetNotAdjacentCondition1(curLoopExpr, expr2, otherLoopExpr)
                                && isMeetNotAdjacentCondition2(curLoopExpr, expr2, otherLoopExpr)) {
                            condition = 4;
                            isOk = true;
                        }
                    } else {
                        if (isMeetNotAdjacentCondition1(curLoopExpr, expr2, otherLoopExpr)) {
                            condition = 3;
                            isOk = true;
                        }
                        if (isMeetNotAdjacentCondition2(curLoopExpr, expr2, otherLoopExpr)) {
                            condition = 4;
                            isOk = true;
                        }
                    }
                }
            }
            if (isOk) {
                SequenceExpr attack = RegexUtil.getSubSeqExpr(seqExpr, indexOfCur, indexOfOther);
                patterns.add(new POAPattern(
                        regexExpr,
                        attack,
                        seqExpr, condition, curLoopExpr, otherLoopExpr, expr2));

            }
        }
    }

    // first(𝛽1) ∩ first(𝛽2) ≠ ∅ and last(𝛽1) ∩ last(𝛽2) ≠ ∅
    private boolean isMeetAdjacentCondition(LoopExpr loopExpr1, LoopExpr loopExpr2) {
        CharRangeSet firstSetOf1 = RegexAnalyzer.getFirstSet(loopExpr1);
        CharRangeSet firstSetOf2 = RegexAnalyzer.getFirstSet(loopExpr2);
        if (firstSetOf1.and(firstSetOf2).isEmpty())
            return false;
        CharRangeSet lastSetOf1 = RegexAnalyzer.getLastSet(loopExpr1);
        CharRangeSet lastSetOf2 = RegexAnalyzer.getLastSet(loopExpr2);
        return !lastSetOf1.and(lastSetOf2).isEmpty();
    }

    private int isMeetNotAdjacentCondition(
            LoopExpr loopExpr1, Expr Expr2, LoopExpr loopExpr3) {
        CharRangeSet firstSetOf1 = RegexAnalyzer.getFirstSet(loopExpr1);
        CharRangeSet firstSetOf2 = RegexAnalyzer.getFirstSet(Expr2);
        CharRangeSet firstSetOf3 = RegexAnalyzer.getFirstSet(loopExpr3);
        if (!firstSetOf1.and(firstSetOf2).and(firstSetOf3).isEmpty())
            return 1;
        CharRangeSet lastSetOf1 = RegexAnalyzer.getLastSet(loopExpr1);
        CharRangeSet lastSetOf2 = RegexAnalyzer.getLastSet(Expr2);
        CharRangeSet lastSetOf3 = RegexAnalyzer.getLastSet(loopExpr3);
        if (!lastSetOf1.and(lastSetOf2).and(lastSetOf3).isEmpty())
            return 2;
        return -1;
    }

    private boolean isMeetNotAdjacentCondition1(
            LoopExpr loopExpr1, Expr Expr2, LoopExpr loopExpr3) {
        CharRangeSet firstSetOf1 = RegexAnalyzer.getFirstSet(loopExpr1);
        CharRangeSet firstSetOf2 = RegexAnalyzer.getFirstSet(Expr2);
        CharRangeSet firstSetOf3 = RegexAnalyzer.getFirstSet(loopExpr3);
        return !firstSetOf1.and(firstSetOf2).and(firstSetOf3).isEmpty();
    }

    private boolean isMeetNotAdjacentCondition2(
            LoopExpr loopExpr1, Expr Expr2, LoopExpr loopExpr3) {
        CharRangeSet lastSetOf1 = RegexAnalyzer.getLastSet(loopExpr1);
        CharRangeSet lastSetOf2 = RegexAnalyzer.getLastSet(Expr2);
        CharRangeSet lastSetOf3 = RegexAnalyzer.getLastSet(loopExpr3);
        return !lastSetOf1.and(lastSetOf2).and(lastSetOf3).isEmpty();
    }
}