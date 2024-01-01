package rengar.checker.pattern;

import rengar.checker.util.RegexUtil;
import rengar.config.GlobalConfig;
import rengar.generator.StringGenerator;
import rengar.generator.path.Path;
import rengar.parser.ast.*;
import java.util.*;

public class POAPattern extends SpecialPattern {
    private final LoopExpr c1;
    private final SequenceExpr r;
    private final LoopExpr c2;

    public POAPattern(RegexExpr regexExpr, Expr attackableExpr,
                      SequenceExpr upperLevelExpr, int condition,
                      LoopExpr c1, LoopExpr c2, SequenceExpr r) {
        super(regexExpr, attackableExpr, upperLevelExpr);
        this.condition = condition;
        this.c1 = c1;
        this.c2 = c2;
        this.r = r;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode() ^ c1.hashCode() ^ c2.hashCode();
        if (r != null)
            hash ^= r.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o))
            return false;
        POAPattern pattern = (POAPattern) o;
        boolean equal = pattern.c1.equals(this.c1) && pattern.c2.equals(this.c2);
        if (r != null)
            equal &= pattern.r.equals(this.r);
        return equal;
    }

    @Override
    protected List<Path> getPhi2Paths() {
        Set<Path> path1List, path2List;
        switch (condition) {
            case 1, 2 -> {
                int maxLength = Math.max(getYLength(c1), getYLength(c2));
                path1List = StringGenerator.gen(
                        c1,
                        RegexUtil.getMaxLoopTimes(c1, maxLength),
                        maxLength,
                        true
                );
                path2List = StringGenerator.gen(
                        c2,
                        RegexUtil.getMaxLoopTimes(c2, maxLength),
                        maxLength,
                        true
                );
            }
            case 3 -> {
                SequenceExpr tmpExpr = new SequenceExpr();
                tmpExpr.addAll(r);
                tmpExpr.add(c2);

                int maxLength = Math.max(getYLength(c1), getYLength(tmpExpr));
                path1List = StringGenerator.gen(
                        c1,
                        RegexUtil.getMaxLoopTimes(c1, maxLength),
                        maxLength,
                        true
                );
                path2List = StringGenerator.gen(
                        tmpExpr,
                        RegexUtil.getMaxLoopTimes(tmpExpr, maxLength),
                        maxLength,
                        true
                );
            }
            default -> {
                SequenceExpr tmpExpr = new SequenceExpr();
                tmpExpr.add(c1);
                tmpExpr.addAll(r);

                int maxLength = Math.max(getYLength(c2), getYLength(tmpExpr));
                path1List = StringGenerator.gen(
                        tmpExpr,
                        RegexUtil.getMaxLoopTimes(tmpExpr, maxLength),
                        maxLength,
                        true
                );
                path2List = StringGenerator.gen(
                        c2,
                        RegexUtil.getMaxLoopTimes(c2, maxLength),
                        maxLength,
                        true
                );
            }
        }
        List<Path> paths = new ArrayList<>();
        for (Path path1 : path1List) {
            for (Path path2 : path2List) {
                if (path1.getLength() != path2.getLength())
                    continue;
                Path path = path1.intersect(path2);
                if (path.hasEmptySet())
                    continue;
                paths.add(path);
            }
        }
        return paths;
    }

    @Override
    public String getType() {
        return String.format("PTLS_%d", condition);
    } // alias of POA

    @Override
    protected int getMaxLength() {
        return GlobalConfig.MaxYStringLengthForPOA;
    }
}
