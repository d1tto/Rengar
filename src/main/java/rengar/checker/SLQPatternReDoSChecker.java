package rengar.checker;

import rengar.base.*;
import rengar.checker.pattern.ReDoSPattern;
import rengar.checker.pattern.SLQPattern;
import rengar.config.GlobalConfig;
import rengar.parser.ast.*;
import rengar.parser.range.*;
import java.util.*;

// SLQ -> Starting with a Large Quantifier
// for example
//      a+$
//      b?a+$
//      b?(c?a+)$
class SLQPatternReDoSChecker {
    private RegexExpr regexExpr;
    private final List<ReDoSPattern> patterns = new ArrayList<>();
    public List<ReDoSPattern> getPatterns() {
        return patterns;
    }

    private long beginTime;

    public void analyse(RegexExpr preprocess) throws InterruptedException {
        beginTime = System.currentTimeMillis();
        regexExpr = preprocess.copy();
        topLevelVisitExpr(regexExpr);
    }

    private boolean isTimeout() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - beginTime >= 5 * 1000)
            return true;
        return false;
    }

    private int times = 0;
    private void topLevelVisitExpr(Expr root)
            throws InterruptedException {
        switch (root) {
            case RegexExpr regexExpr -> topLevelVisitExpr(regexExpr.getExpr());
            case BranchExpr branchExpr -> {
                for (Expr branch : branchExpr.getBranchs()) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    topLevelVisitExpr(branch);
                }
            }
            case SequenceExpr seqExpr -> {
                List<Expr> subExprs = seqExpr.getExprs();
                for (int i = 0; i < subExprs.size(); i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    Expr subExpr = subExprs.get(i);
                    if (!isLargeQuantifier(subExpr))
                        continue;
                    times += 1;
                    if (times > 50)
                        break;
                    // postfix expr is not exist
                    if (i + 1 >= subExprs.size())
                        continue;
                    // check whether postfix expr can be nullable
                    // because the $ is not nullable in my implementation,
                    // so we don't need to care about it
                    boolean isNullable = true;
                    for (int j = i + 1; j < subExprs.size(); j++) {
                        Expr postfixExpr = subExprs.get(j);
                        if (postfixExpr instanceof LookaheadExpr) {
                            isNullable = false;
                            continue;
                        }
                        if (!RegexAnalyzer.getNullable(subExprs.get(j)))
                            isNullable = false;
                    }
                    if (isNullable)
                        continue;
                    if (i == 0) {
                        patterns.add(new SLQPattern(regexExpr, subExpr, seqExpr,
                                null, (LoopExpr)subExpr,0));
                    } else {
                        // if we enter here, it means that there exist a prefix expr.
                        // we should check whether prefix expr can be nullable
                        boolean isOk = true;
                        for (int j = 0; j < i; j++) {
                            if (!RegexAnalyzer.getNullable(subExprs.get(j)))
                                isOk = false;
                        }
                        boolean hasBegin = false;
                        for (int j = 0; j < i; j++) {
                            if (hasBeginAnchor(subExprs.get(j))) {
                                hasBegin = true;
                                break;
                            }
                        }
                        if (hasBegin)
                            continue;
                        if (isOk) {
                            patterns.add(new SLQPattern(regexExpr, subExpr, seqExpr,
                                    null, (LoopExpr)subExpr, 0));
                            continue;
                        }
                        if (!GlobalConfig.option.isWeakPatternCheck()) {
                            // if prefix expr can't be nullable, we check the condition:
                            // first(𝛽1) ∩ first(𝛽2) ≠ ∅ or last(𝛽1) ∩ last(𝛽2)
                            SequenceExpr prefixExpr = new SequenceExpr();
                            for (int j = 0; j < i; j++)
                                prefixExpr.add(subExprs.get(j));
                            int condition = isMeetCondition(prefixExpr, subExpr);
                            if (condition != -1)
                                isOk = true;
                            if (!hasAlphabetIntersection(prefixExpr, subExpr))
                                isOk = false;
                            if (isOk) {
                                SequenceExpr attackExpr = new SequenceExpr();
                                for (int j = 0; j <= i; j++)
                                    attackExpr.add(subExprs.get(j));
                                patterns.add(new SLQPattern(regexExpr, attackExpr, seqExpr,
                                        prefixExpr, (LoopExpr)subExpr, condition));
                            }
                        }
                    }
                }
            }
            default -> {}
        }
    }

    private boolean isLargeQuantifier(Expr expr) {
        if (expr instanceof LoopExpr loopExpr) {
            return loopExpr.getMax() == -1 || loopExpr.getMax() >= 50;
        }
        return false;
    }

    private int isMeetCondition(Expr expr1, Expr expr2) {
        // first(𝛽1) ∩ first(𝛽2) ≠ ∅
        CharRangeSet firstSetOf1 = RegexAnalyzer.getFirstSet(expr1);
        CharRangeSet firstSetOf2 = RegexAnalyzer.getFirstSet(expr2);
        CharRangeSet and = firstSetOf1.and(firstSetOf2);
        if (!and.isEmpty())
            return 1;
        // last(𝛽1) ∩ last(𝛽2)
        CharRangeSet lastSetOf1 = RegexAnalyzer.getLastSet(expr1);
        CharRangeSet lastSetOf2 = RegexAnalyzer.getLastSet(expr2);
        and = lastSetOf1.and(lastSetOf2);
        if (!and.isEmpty())
            return 2;
        return -1;
    }

    private boolean hasAlphabetIntersection(Expr left, Expr right) {
        CharRangeSet alphabetOfLeft = RegexAnalyzer.getAlphabet(left);
        CharRangeSet alphabetOfRight = RegexAnalyzer.getAlphabet(right);
        return !alphabetOfLeft.and(alphabetOfRight).isEmpty();
    }

    private boolean hasBeginAnchor(Expr expr) {
        boolean has = false;
        switch (expr) {
            case RegexExpr regexExpr -> has = hasBeginAnchor(regexExpr.getExpr());
            case BranchExpr branchExpr -> {
                has = true;
                for (SequenceExpr seqExpr : branchExpr.getBranchs()) {
                    if (!hasBeginAnchor(seqExpr)) {
                        has = false;
                        break;
                    }
                }
            }
            case SequenceExpr seqExpr -> {
                for (Expr subExpr : seqExpr.getExprs()) {
                    if (hasBeginAnchor(subExpr)) {
                        has = true;
                        break;
                    }
                }
            }
            case LoopExpr loopExpr -> {
                if (loopExpr.getMin() == 0)
                    break;
                has = hasBeginAnchor(loopExpr.getBody());
            }
            case GroupExpr groupExpr -> has = hasBeginAnchor(groupExpr.getBody());
            case BeginExpr ignored -> has = true;
            default -> {}
        }
        return has;
    }
}