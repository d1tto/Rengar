package rengar.checker.pattern;

import rengar.checker.util.RegexUtil;
import rengar.config.GlobalConfig;
import rengar.generator.StringGenerator;
import rengar.generator.path.Path;
import rengar.parser.ast.*;

import java.util.List;
import java.util.Set;

public class EOAPattern extends OneCountingPattern {
    private final SequenceExpr body;
    private final SequenceExpr expr1;
    private final SequenceExpr expr2;
    public EOAPattern(RegexExpr regexExpr, LoopExpr attackableExpr, LoopExpr upperLevelExpr,
                      SequenceExpr body, SequenceExpr expr1, SequenceExpr expr2, int condition) {
        super(regexExpr, attackableExpr, upperLevelExpr);
        this.body = body;
        this.expr1 = expr1;
        this.expr2 = expr2;
        this.condition = condition;
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ body.hashCode() ^ expr1.hashCode() ^ expr2.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o))
            return false;
        EOAPattern pattern = (EOAPattern) o;
        return pattern.body.equals(this.body)
                && pattern.expr1.equals(this.expr1)
                && pattern.expr2.equals(this.expr2);
    }

    @Override
    public String getType() {
        return "EOLS";
    }

    @Override
    protected int getMaxLength() {
        return GlobalConfig.MaxYStringLengthForEOA;
    }

    @Override
    protected List<Path> getPhi2Paths() {
        int maxLength = getYLength(body);
        Set<Path> paths = StringGenerator.gen(body,
                RegexUtil.getMaxLoopTimes(body, maxLength),
                maxLength,
                true
        );
        int loopTimes = 1;
        if (condition == 1) {
            int ub1 = 0, ub2 = 0;
            for (int i = 0; i < expr1.getSize(); i++) {
                ub1 = helper(expr1.get(i));
                if (ub1 != -1)
                    break;
            }
            for (int i = expr2.getSize() - 1; i >= 0; i--) {
                ub2 = helper(expr2.get(i));
                if (ub2 != -1)
                    break;
            }
            loopTimes += ub1 + ub2;
        } else {
            int ub1 = 0, ub2 = 0;
            for (int i = 0; i < expr2.getSize(); i++) {
                ub1 = helper(expr2.get(i));
                if (ub1 != -1)
                    break;
            }
            for (int i = expr1.getSize() - 1; i >= 0; i--) {
                ub2 = helper(expr1.get(i));
                if (ub2 != -1)
                    break;
            }
            loopTimes += ub1 + ub2;
        }
        paths.addAll(StringGenerator.quickGen(body, loopTimes));
        return paths.stream().filter(path -> !path.isEmpty()).toList();
    }

    private int helper(Expr expr) {
        int upperbound = -1;
        switch (expr) {
            case RegexExpr regexExpr -> upperbound = helper(regexExpr.getExpr());
            case BranchExpr branchExpr -> {
                for (SequenceExpr branch : branchExpr) {
                    upperbound = Math.max(upperbound, helper(branch));
                }
            }
            case SequenceExpr seqExpr -> {
                for (Expr subExpr : seqExpr) {
                    upperbound = Math.max(upperbound, helper(subExpr));
                }
            }
            case LoopExpr loopExpr -> upperbound = loopExpr.getMin();
            case GroupExpr groupExpr -> upperbound = helper(groupExpr.getBody());
            default -> {}
        }
        return upperbound;
    }

    @Override
    public boolean isDuplicate(ReDoSPattern pattern) {
        if (!super.equals(pattern))
            return false;
        if (pattern instanceof EOAPattern eoaPattern) {
            return this.body.equals(eoaPattern.body);
        }
        return false;
    }
}
