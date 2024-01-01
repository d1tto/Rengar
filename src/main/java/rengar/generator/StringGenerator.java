package rengar.generator;

import rengar.generator.path.Path;
import rengar.parser.ast.*;
import java.util.*;
import rengar.parser.RegexParser;
import rengar.parser.exception.PatternSyntaxException;

public class StringGenerator {
    public static int[] gen(String patternStr)
            throws PatternSyntaxException {
        return gen(patternStr, RegexParser.Language.Java, 1).get(0);
    }
    public static int[] gen(String patternStr, RegexParser.Language type)
            throws PatternSyntaxException {
        return gen(patternStr, type, 1).get(0);
    }
    public static List<int[]> gen(String patternStr, RegexParser.Language type, int loopMax)
            throws PatternSyntaxException {
        RegexParser parser = RegexParser.createParser(type, patternStr);
        RegexExpr regexExpr = parser.parse();
        Set<Path> paths = gen(regexExpr, loopMax, -1, false);
        List<int[]> strings = new ArrayList<>();
        paths.forEach(path -> strings.add(path.genValue()));
        return strings;
    }
    public static Set<Path> gen(Expr expr) {
        return gen(expr, 1, -1, false);
    }

    /**
     * this function is used to generate Path for regex
     * @param expr the Expr that needs to rengar.generator Path
     * @param loopMax max loop times of the LoopExpr in expr
     * @param specificLen if you need generate the Path that has specific length, we set it.
     *                    Otherwise, we set it to -1.
     * @param range range is enable when specificLen is not equal to -1. when range is true,
     *              we add all Path that it's length is less than or equal to specificLen
     * @return the Path we generated
     * */
    public static Set<Path> gen(Expr expr, int loopMax, int specificLen, boolean range) {
        Set<Path> paths = new HashSet<>();
        switch (expr) {
            case CharExpr charExpr -> {
                Path path = new Path();
                switch (charExpr) {
                    case SingleCharExpr s -> path.add(s.getChar());
                    case CharRangeExpr c -> path.add(c.getRangeSet());
                    default -> {}
                }
                paths.add(path);
            }
            case GroupExpr groupExpr -> paths = gen(groupExpr.getBody(), loopMax, specificLen, range);
            case SequenceExpr seqExpr -> {
                Path prefix = new Path();
                genSequenceExpr(prefix, paths, seqExpr, 0, loopMax, specificLen, range);
            }
            case BranchExpr branchExpr -> {
                for (SequenceExpr branch : branchExpr.getBranchs())
                    paths.addAll(gen(branch, loopMax, specificLen, range));
            }
            case RegexExpr regexExpr -> paths = (gen(regexExpr.getExpr(), loopMax, specificLen, range));
            case LoopExpr loopExpr -> {
                int preLoopMax = loopMax;
                // edge case (\d{3, 40})+
                if (loopExpr.getMin() > loopMax)
                    loopMax = loopExpr.getMin();
                if (loopExpr.getMin() == loopExpr.getMax()) {
                    loopMax = loopExpr.getMax();
                } else {
                    loopMax = Math.min(
                            loopMax,
                            loopExpr.getMax() == -1 ? Integer.MAX_VALUE : loopExpr.getMax()
                    );
                }
                if (loopExpr.getMin() > loopMax) {
                    paths.add(quickGen(loopExpr));
                } else {
                    for (int times = loopExpr.getMin(); times <= loopMax; times++) {
                        Set<Path> choices = permutation(
                                gen(loopExpr.getBody(), preLoopMax, specificLen, true),
                                times,
                                specificLen
                        );
                        if (specificLen > 0) {
                            if (range)
                                choices.removeIf(path -> path.getLength() > specificLen);
                            else
                                choices.removeIf(path -> path.getLength() != specificLen);
                        }
                        paths.addAll(choices);
                    }
                }
            }
            default -> {}
        }
        return paths;
    }

    static void genSequenceExpr(Path prefix, Set<Path> out, SequenceExpr seqExpr,
                                int begin, int loopMax, int specificLen, boolean range) {
        boolean pass = false;
        for (int i = begin; i < seqExpr.getExprs().size(); i++) {
            // early exit. used to prune
            if (pass || (specificLen > 0 && prefix.getLength() > specificLen))
                break;
            Expr subExpr = seqExpr.getExprs().get(i);
            switch (subExpr) {
                case LoopExpr loopExpr -> {
                    int tmp = loopExpr.getMax();
                    loopMax = Math.min(loopMax, tmp == -1 ? Integer.MAX_VALUE : tmp);
                    if (loopMax == 0)
                        break;
                    if (loopExpr.getMin() > 0)
                        pass = true;
                    if (loopExpr.getMin() > loopMax) {
                        Path backup = new Path(prefix);
                        prefix.add(quickGen(loopExpr));
                        genSequenceExpr(prefix, out, seqExpr, i + 1, loopMax, specificLen, range);
                        prefix = backup;
                    } else {
                        Set<Path> tmpOut = gen(loopExpr.getBody(), loopMax, -1, false);
                        tmpOut.removeIf(Path::isEmpty);
                        for (int times = loopExpr.getMin(); times <= loopMax; times++) {
                            // `specificLen - prefix.getLength()` used to limit the choice's length
                            Set<Path> choices = permutation(tmpOut, times, specificLen - prefix.getLength());
                            for (Path choice : choices) {
                                // used to backtracking
                                Path backup = new Path(prefix);
                                prefix.add(choice);
                                genSequenceExpr(prefix, out, seqExpr, i + 1, loopMax, specificLen, range);
                                prefix = backup;
                            }
                        }
                    }
                }
                case CharExpr charExpr -> {
                    switch (charExpr) {
                        case SingleCharExpr s -> prefix.add(s.getChar());
                        case CharRangeExpr c -> prefix.add(c.getRangeSet());
                        default -> {}
                    }
                }
                case GroupExpr groupExpr -> {
                    pass = true;
                    Set<Path> choices = gen(groupExpr.getBody(), loopMax, -1, false);
                    choices.removeIf(Path::isEmpty);
                    for (Path choice : choices) {
                        // used to backtracking
                        Path backup = new Path(prefix);
                        prefix.add(choice);
                        if (loopMax == 0)
                            break;
                        genSequenceExpr(prefix, out, seqExpr, i + 1, loopMax, specificLen, range);
                        prefix = backup;
                    }
                }
                default -> {}
            }
        }
        if (!pass) {
            if (specificLen < 0)
                out.add(prefix);
            if (specificLen > 0) {
                if (!range && prefix.getLength() == specificLen)
                    out.add(prefix);
                if (range && prefix.getLength() <= specificLen)
                    out.add(prefix);
            }
        }
    }

    /**
     * full permutation
     * @param choices elements that are used to do permutation
     * @param loopMax the number of selected elements
     * @param maxLength the result's length can't be greater than maxLength
     * */
    private static Set<Path> permutation(Set<Path> choices, int loopMax,
                                         int maxLength) {
        Set<Path> out = new HashSet<>();
        Path prefix = new Path();
        try {
            permutationHelper(choices, out, prefix, 0, loopMax, maxLength);
        } catch (Exception ignore){}
        return out;
    }

    private static void permutationHelper(Set<Path> choices, Set<Path> out,
                                          Path prefix, int index, int loopMax,
                                          int maxLength) throws Exception {
        // early exit. used to prune
        if (maxLength > 0 && prefix.getLength() > maxLength) {
            return;
        }
        if (out.size() > 50)
            throw new Exception();
        if (index >= loopMax) {
            out.add(prefix);
            return;
        }
        for (Path path : choices) {
            Path backup = new Path(prefix);
            prefix.add(path);
            permutationHelper(choices, out, prefix, index + 1, loopMax,
                    maxLength);
            prefix = backup;
        }
    }

    public static Path quickGen(Expr expr) {
        Path path = new Path();
        switch (expr) {
            case RegexExpr regexExpr -> path = quickGen(regexExpr.getExpr());
            case BranchExpr branchExpr -> path = quickGen(branchExpr.getBranchs().get(0));
            case SequenceExpr seqExpr -> {
                for (Expr subExpr : seqExpr.getExprs()) {
                    path.add(quickGen(subExpr));
                }
            }
            case LoopExpr loopExpr -> {
                if (loopExpr.getMin() == 0)
                    break;
                Path bodyPath = quickGen(loopExpr.getBody());
                for (int i = 0; i < loopExpr.getMin(); i++) {
                    path.add(bodyPath);
                }
            }
            case GroupExpr groupExpr -> path = quickGen(groupExpr.getBody());
            case SingleCharExpr singleCharExpr -> path.add(singleCharExpr.getChar());
            case CharRangeExpr charRangeExpr -> path.add(charRangeExpr.getRangeSet());
            default -> {}
        }
        return path;
    }

    public static Set<Path> quickGen(Expr expr, int loopTimes) {
        Set<Path> paths = new HashSet<>();
        switch (expr) {
            case CharExpr charExpr -> {
                Path path = new Path();
                switch (charExpr) {
                    case SingleCharExpr s -> path.add(s.getChar());
                    case CharRangeExpr c -> path.add(c.getRangeSet());
                    default -> {}
                }
                paths.add(path);
            }
            case GroupExpr groupExpr -> paths = quickGen(groupExpr.getBody(), loopTimes);
            case SequenceExpr seqExpr -> {
                Path prefix = new Path();
                quickGenSequence(prefix, paths, seqExpr, 0, loopTimes);
            }
            case BranchExpr branchExpr -> {
                for (SequenceExpr branch : branchExpr.getBranchs())
                    paths.addAll(quickGen(branch, loopTimes));
            }
            case RegexExpr regexExpr -> paths = (quickGen(regexExpr.getExpr(), loopTimes));
            case LoopExpr loopExpr -> {
                int preLoopTimes = loopTimes;
                // edge case (\d{3, 40})+
                if (loopExpr.getMin() > loopTimes)
                    loopTimes = loopExpr.getMin();
                if (loopExpr.getMin() == loopExpr.getMax()) {
                    loopTimes = loopExpr.getMax();
                } else {
                    loopTimes = Math.min(
                            loopTimes,
                            loopExpr.getMax() == -1 ? Integer.MAX_VALUE : loopExpr.getMax()
                    );
                }
                Set<Path> choices = quickGen(loopExpr.getBody(), preLoopTimes);
                for (Path choice : choices) {
                    Path tmp = new Path();
                    for (int i = 0; i < loopTimes; i++) {
                        tmp.add(choice);
                    }
                    paths.add(tmp);
                }
            }
            default -> {}
        }
        return paths;
    }

    private static void quickGenSequence(Path prefix, Set<Path> out, SequenceExpr seqExpr,
                                         int begin, int loopTimes) {
        boolean isOk = true;
        for (int i = begin; i < seqExpr.getExprs().size(); i++) {
            if (!isOk)
                break;
            Expr subExpr = seqExpr.getExprs().get(i);
            Set<Path> choices = quickGen(subExpr, loopTimes);
            if (choices.size() != 0)
                isOk = false;
            for (Path choice : choices) {
                Path backup = new Path(prefix);
                prefix.add(choice);
                quickGenSequence(prefix, out, seqExpr, i + 1, loopTimes);
                prefix = backup;
            }
        }
        if (isOk)
            out.add(prefix);
    }
}
