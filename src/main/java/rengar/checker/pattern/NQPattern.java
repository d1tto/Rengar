package rengar.checker.pattern;

import rengar.checker.util.RegexUtil;
import rengar.config.GlobalConfig;
import rengar.generator.path.Path;
import rengar.generator.StringGenerator;
import rengar.parser.ast.*;
import java.util.*;

public class NQPattern extends OneCountingPattern {
    private final LoopExpr innerQuantifier;

    public NQPattern(RegexExpr regexExpr, LoopExpr attackableExpr,
                     LoopExpr upperLevelExpr, LoopExpr innerQuantifier) {
        super(regexExpr, attackableExpr, upperLevelExpr);
        this.innerQuantifier = innerQuantifier;
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ innerQuantifier.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o))
            return false;
        NQPattern pattern = (NQPattern) o;
        return pattern.innerQuantifier.equals(this.innerQuantifier);
    }

    @Override
    public String getType() {
        return "EOLS";
    }

    @Override
    protected int getMaxLength() {
        return GlobalConfig.MaxYStringLengthForNQ;
    }

    @Override
    protected List<Path> getPhi2Paths() {
        int maxLength = getYLength(innerQuantifier);
        int maxLoopTimes = RegexUtil.getMaxLoopTimes(innerQuantifier, maxLength);
        List<Path> paths = new ArrayList<>(StringGenerator.gen(
                innerQuantifier,
                maxLoopTimes,
                maxLength,
                true
        ));
        if (maxLoopTimes == 0) {
            paths.add(StringGenerator.quickGen(innerQuantifier));
        }
        paths = paths.stream().filter(path -> !path.isEmpty()).toList();
        return paths;
    }
}
