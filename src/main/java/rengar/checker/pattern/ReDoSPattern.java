package rengar.checker.pattern;

import rengar.base.*;
import rengar.generator.*;
import rengar.generator.path.*;
import rengar.checker.util.*;
import rengar.parser.ast.*;
import rengar.parser.range.*;
import rengar.checker.attack.*;
import rengar.util.Pair;
import java.util.*;

public abstract class ReDoSPattern {
    protected RegexExpr regexExpr;
    protected SequenceExpr prefixExpr;
    protected Expr attackableExpr;
    protected SequenceExpr postfixExpr;
    protected Expr upperLevelExpr;
    protected int condition;

    public ReDoSPattern(RegexExpr regexExpr, Expr attackableExpr,
                        Expr upperLevelExpr) {
        this.regexExpr = regexExpr.copy();
        this.attackableExpr = attackableExpr;
        this.upperLevelExpr = upperLevelExpr;
        split();
    }

    public SequenceExpr getPrefixExpr() {
        return prefixExpr;
    }

    public Expr getAttackableExpr() {
        return attackableExpr;
    }

    public SequenceExpr getPostfixExpr() {
        return postfixExpr;
    }

    private void split() {
        SequenceExpr targetSequence = getTargetSequence(regexExpr);
        RegexUtil.stripLoop(targetSequence, upperLevelExpr);
        RegexUtil.stripGroup(targetSequence);

        Pair<Integer, Integer> pair = SequenceExpr.indexOf(targetSequence, attackableExpr);
        int begin = pair.getLeft(), end = pair.getRight();
        prefixExpr = SequenceExpr.subSequence(targetSequence, 0, begin);
        postfixExpr = SequenceExpr.subSequence(targetSequence, end, targetSequence.getSize());
    }

    /**
     * get the sequence containing attackable expr.
     * */
    protected SequenceExpr getTargetSequence(RegexExpr regexExpr) {
        remove(regexExpr);
        return regexExpr.getExpr().getBranchs().get(0);
    }

    /**
     * remove the branch that doesn't contain the attackable expr
     * */
    protected void remove(Expr expr) {
        switch (expr) {
            case RegexExpr regexExpr -> remove(regexExpr.getExpr());
            case BranchExpr branchExpr -> {
                List<SequenceExpr> branchs = branchExpr.getBranchs();
                for (SequenceExpr seqExpr : branchs) {
                    remove(seqExpr);
                    if (hasAttackableExpr(seqExpr)) {
                        branchs.clear();
                        branchs.add(seqExpr);
                        break;
                    }
                }
            }
            case SequenceExpr seqExpr -> seqExpr.getExprs().forEach(this::remove);
            case GroupExpr groupExpr -> remove(groupExpr.getBody());
            case LoopExpr loopExpr -> remove(loopExpr.getBody());
            default -> {}
        }
    }

    protected abstract boolean hasAttackableExpr(Expr sequenceExpr);

    public List<AttackString> generate() {
        List<AttackString> attackStrings = new ArrayList<>();
        for (Path pathY : getPhi2Paths()) {
            AttackString attackString = new AttackString();
            attackString.setPrefix(StringGenerator.quickGen(prefixExpr).genValue());

            int n = getMaxLength();
            n = n / pathY.getLength();
            attackString.setAttack(pathY.genValue(), n);

            CharRangeSet lastSetOfY = RegexAnalyzer.getLastSet(attackableExpr);
            CharRangeSet alphabetOfZ = RegexAnalyzer.getFirstSet(postfixExpr);

            CharRangeSet tmp = lastSetOfY.union(alphabetOfZ);
            if (this instanceof POAPattern) {
                tmp = tmp.union(RegexAnalyzer.getFirstSet(attackableExpr));
            }
            tmp = tmp.negate();
            int[] postfix = new int[]{
                    'A', '\t', '\n', '\u0000',
                    '\u1111', '\u2222', '\u8888', '\u9999',
                    '\u3333', '\u4444', '\u5555', '\u6666',
            };
            if (!tmp.isEmpty()) {
                Arrays.fill(postfix, tmp.getRanges().get(0).end);
                postfix[1] = '\n';
                postfix[3] = '\t';
                postfix[5] = '\u1111';
                postfix[7] = '\u2222';
                postfix[9] = '\u5555';
                postfix[11] = '\u9999';
            }
            attackString.setPostfix(postfix);
            attackStrings.add(attackString);
            if (attackStrings.size() > 40)
                break;
        }
        return attackStrings;
    }

    protected abstract List<Path> getPhi2Paths();

    protected int getYLength(Expr expr) {
        return Math.min(RegexUtil.getRegexElementNumber(expr, false) + 5, 50000);
    }

    public abstract String getType();

    /**
     * the max length of x + y * n + z
     * */
    protected abstract int getMaxLength();

    @Override
    public int hashCode() {
        return regexExpr.hashCode() ^ prefixExpr.hashCode() ^ attackableExpr.hashCode()
                ^ postfixExpr.hashCode() ^ getType().hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Type: <%s>\n", getType()));
        sb.append("\t");
        sb.append(regexExpr);
        sb.append("\n");
        sb.append(String.format("\tprefix string is %s\n", prefixExpr.toString()));
        sb.append(String.format("\tattackable string is %s\n", attackableExpr.toString()));
        sb.append(String.format("\tpostfix string is %s", postfixExpr.toString()));
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o.getClass() != this.getClass())
            return false;
        ReDoSPattern pattern = (ReDoSPattern) o;
        return pattern.attackableExpr.equals(this.attackableExpr)
                && pattern.prefixExpr.equals(this.prefixExpr)
                && pattern.postfixExpr.equals(this.postfixExpr)
                && pattern.getType().equals(this.getType());
    }

    public boolean isDuplicate(ReDoSPattern pattern) {
        return this.equals(pattern);
    }
}