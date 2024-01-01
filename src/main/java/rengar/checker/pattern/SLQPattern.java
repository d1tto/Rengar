package rengar.checker.pattern;

import rengar.checker.util.RegexUtil;
import rengar.config.GlobalConfig;
import rengar.generator.StringGenerator;
import rengar.generator.path.Path;
import rengar.parser.ast.*;

import java.util.*;

public class SLQPattern extends SpecialPattern {
    private final SequenceExpr prefix; // prefix + loopExpr = attackableExpr
    private final LoopExpr loopExpr;

    public SLQPattern(RegexExpr regexExpr, Expr attackableExpr, SequenceExpr upperLevelExpr,
                      SequenceExpr prefix, LoopExpr loopExpr, int condition) {
        super(regexExpr, attackableExpr, upperLevelExpr);
        this.prefix = prefix;
        this.loopExpr = loopExpr;
        this.condition = condition;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode() ^ loopExpr.hashCode();
        if (prefix != null)
            hash ^= prefix.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o))
            return false;
        SLQPattern pattern = (SLQPattern) o;
        boolean equal = pattern.loopExpr.equals(this.loopExpr);
        if (prefix != null)
            equal &= pattern.prefix.equals(this.prefix);
        return equal;
    }

    @Override
    public String getType() {
        return String.format("POLS_%d", condition);
    } // alias of SLQ

    @Override
    protected int getMaxLength() {
        return GlobalConfig.MaxYStringLengthForSLQ;
    }

    @Override
    protected List<Path> getPhi2Paths() {
        List<Path> paths = null;
        if (prefix == null) {
            paths = StringGenerator.gen(
                    loopExpr,
                    1,
                    -1,
                    false
            ).stream().filter(path -> !path.isEmpty()).toList();
        } else {
            SequenceExpr tmpExpr = new SequenceExpr();

            CharRangeExpr rangeExpr2 = new CharRangeExpr();
            rangeExpr2.addRange(0, Character.MAX_CODE_POINT);
            LoopExpr dotLoop = new LoopExpr(0, -1, LoopExpr.LoopType.Greedy, rangeExpr2);
            if (condition == 1) {
                tmpExpr.addAll(prefix);
                tmpExpr.add(dotLoop);
            } else {
                tmpExpr.add(dotLoop);
                tmpExpr.addAll(prefix);
            }
            int maxLength = Math.max(getYLength(tmpExpr), getYLength(loopExpr));
            Set<Path> path1List = StringGenerator.gen(tmpExpr,
                    RegexUtil.getMaxLoopTimes(tmpExpr, maxLength),
                    maxLength,
                    true
            );
            path1List.add(StringGenerator.quickGen(tmpExpr));
            Set<Path> path2List = StringGenerator.gen(
                    loopExpr,
                    RegexUtil.getMaxLoopTimes(loopExpr, maxLength),
                    maxLength,
                    true
            );
            path2List.add(StringGenerator.quickGen(loopExpr));
            paths = new ArrayList<>();
            for (Path path1 : path1List) {
                for (Path path2 : path2List) {
                    if (path1.getLength() == path2.getLength()) {
                        Path path = path1.intersect(path2);
                        if (path.hasEmptySet())
                            continue;
                        paths.add(path);
                    }
                }
            }
        }
        return paths;
    }
}
