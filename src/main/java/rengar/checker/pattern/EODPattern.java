package rengar.checker.pattern;

import rengar.checker.util.RegexUtil;
import rengar.config.GlobalConfig;
import rengar.generator.StringGenerator;
import rengar.generator.path.Path;
import rengar.parser.ast.*;
import java.util.*;

public class EODPattern extends OneCountingPattern {
    private final SequenceExpr prefix, postfix;
    private final SequenceExpr branchP;
    private final SequenceExpr branchQ;

    public EODPattern(RegexExpr regexExpr, LoopExpr attackableExpr, LoopExpr upperLevelExpr,
                      SequenceExpr branchP, SequenceExpr branchQ,
                      SequenceExpr prefix, SequenceExpr postfix, int condition) {
        super(regexExpr, attackableExpr, upperLevelExpr);
        this.branchP = branchP;
        this.branchQ = branchQ;
        this.prefix = prefix;
        this.postfix = postfix;
        this.condition = condition;
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ branchP.hashCode() ^ branchQ.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o))
            return false;
        EODPattern pattern = (EODPattern) o;
        return pattern.branchP.equals(this.branchP)
                && pattern.branchQ.equals(this.branchQ);
    }

    @Override
    public String getType() {
        return "EOLS";
    }

    @Override
    protected int getMaxLength() {
        return GlobalConfig.MaxYStringLengthForEOD;
    }

    @Override
    protected List<Path> getPhi2Paths() {
        Set<Path> paths = new HashSet<>();
        // add branchP
        int maxLength = getYLength(branchP);
        paths.addAll(StringGenerator.gen(branchP,
                RegexUtil.getMaxLoopTimes(branchP, maxLength),
                maxLength,
                true)
        );
        // add branchQ
        maxLength = getYLength(branchQ);
        paths.addAll(StringGenerator.gen(branchQ,
                RegexUtil.getMaxLoopTimes(branchQ, maxLength),
                maxLength,
                true)
        );
        // add branchP + branchQ
        SequenceExpr tmpExpr = new SequenceExpr();
        tmpExpr.addAll(branchQ);
        tmpExpr.addAll(branchP);
        maxLength = getYLength(tmpExpr);
        paths.addAll(StringGenerator.gen(
                tmpExpr,
                RegexUtil.getMaxLoopTimes(tmpExpr, maxLength),
                maxLength,
                true
        ));
        // add branchQ + branchP
        tmpExpr = new SequenceExpr();
        tmpExpr.addAll(branchP);
        tmpExpr.addAll(branchQ);
        maxLength = getYLength(tmpExpr);
        paths.addAll(StringGenerator.gen(
                tmpExpr,
                RegexUtil.getMaxLoopTimes(tmpExpr, maxLength),
                maxLength,
                true
        ));
        // r1* & r2* & r
        try {
            CharRangeExpr anyExpr = new CharRangeExpr();
            anyExpr.addRange(0, Character.MAX_CODE_POINT);
            SequenceExpr r1star = new SequenceExpr();
            r1star.add(anyExpr);
            SequenceExpr r2star = new SequenceExpr();
            r2star.add(anyExpr);
            maxLength = Math.max(getYLength(r1star), getYLength(r2star));
            maxLength = Math.max(maxLength, getYLength(attackableExpr));
            Set<Path> path1List = StringGenerator.gen(
                    r1star,
                    RegexUtil.getMaxLoopTimes(r1star, maxLength),
                    maxLength,
                    true
            );
            Set<Path> path2List = StringGenerator.gen(
                    r2star,
                    RegexUtil.getMaxLoopTimes(r2star, maxLength),
                    maxLength,
                    true
            );
            Set<Path> path3List = StringGenerator.gen(
                    attackableExpr,
                    RegexUtil.getMaxLoopTimes(attackableExpr, maxLength),
                    maxLength,
                    true
            );
            Set<Path> tmpPaths = Path.intersect(path1List, path2List);
            paths.addAll(Path.intersect(tmpPaths, path3List));
        } catch (Exception | Error ignored) {}

        if (prefix != null && postfix != null) {
            Path prefixPath = StringGenerator.quickGen(prefix);
            Path postfixPath = StringGenerator.quickGen(postfix);
            Set<Path> results = new HashSet<>();
            for (Path path : paths) {
                Path tmp = new Path();
                tmp.add(prefixPath);
                tmp.add(path);
                tmp.add(postfixPath);
                results.add(tmp);
            }
            return results.stream().filter(path -> !path.isEmpty()).toList();
        }
        return paths.stream().filter(path -> !path.isEmpty()).toList();
    }
}
