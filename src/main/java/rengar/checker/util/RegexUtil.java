package rengar.checker.util;

import rengar.generator.StringGenerator;
import rengar.generator.path.Path;
import rengar.parser.ast.*;
import rengar.parser.range.CharRangeSet;
import java.util.ArrayList;
import java.util.List;

public class RegexUtil {
    public static RegexExpr flatten(Expr root) throws InterruptedException {
        return new RegexExpr(FlattenRegex.flatten(root));
    }

    public static RegexExpr preprocessForPoaSlq(RegexExpr root) {
        RegexExpr expr;
        try {
            expr = root.copy();
            expr = RegexUtil.flatten(expr);
            RegexUtil.stripGroup(expr);
            expr = ExtractInnerLoop.extract(expr);
        } catch (InterruptedException e) {
            expr = root.copy();
            RegexUtil.stripGroup(expr);
        }
        return expr;
    }

    public static boolean hasLoopExpr(Expr expr) {
        boolean has = false;
        switch (expr) {
            case RegexExpr regexExpr -> has = hasLoopExpr(regexExpr.getExpr());
            case BranchExpr branchExpr -> {
                for (SequenceExpr seqExpr : branchExpr.getBranchs()) {
                    has = hasLoopExpr(seqExpr);
                    if (has)
                        break;
                }
            }
            case SequenceExpr seqExpr -> {
                for (Expr subExpr : seqExpr.getExprs()) {
                    has = hasLoopExpr(subExpr);
                    if (has)
                        break;
                }
            }
            case LoopExpr ignored -> has = true;
            case GroupExpr groupExpr -> has = hasLoopExpr(groupExpr.getBody());
            default -> {}
        }
        return has;
    }

    public static boolean hasLoopExpr(Expr expr, LoopExpr target) {
        boolean has = false;
        switch (expr) {
            case LoopExpr loopExpr -> {
                if (loopExpr.equals(target))
                    has = true;
            }
            case GroupExpr groupExpr -> has = hasLoopExpr(groupExpr.getBody(), target);
            case RegexExpr regexExpr -> {
                for (SequenceExpr seqExpr : regexExpr.getExpr().getBranchs()) {
                    has |= hasLoopExpr(seqExpr, target);
                }
            }
            case SequenceExpr seqExpr -> {
                for (Expr subExpr : seqExpr.getExprs()) {
                    has |= hasLoopExpr(subExpr, target);
                }
            }
            default -> {}
        }
        return has;
    }

    public static void stripGroup(Expr root) {
        StripGroup.strip(root);
    }

    public static void stripLoop(Expr root, Expr breakExpr) {
        StripLoop.strip(root, breakExpr);
    }

    public static SequenceExpr getSubSeqExpr(SequenceExpr seqExpr, int begin, int end) {
        SequenceExpr out = new SequenceExpr();
        for (int i = begin; i <= end; i++) {
            out.add(seqExpr.getExprs().get(i));
        }
        return out;
    }

    /**
     * This function calculates the length of the longest or shortest
     * element produced by the regex
     * @param expr regex expr
     * @param isMin longest or shortest?
     * @return length
     * */
    public static int getRegexElementNumber(Expr expr, boolean isMin) {
        int times = 0;
        switch (expr) {
            case RegexExpr regexExpr -> times = getRegexElementNumber(regexExpr.getExpr(), isMin);
            case BranchExpr branchExpr -> {
                for (SequenceExpr seqExpr : branchExpr.getBranchs()) {
                    int tmp = getRegexElementNumber(seqExpr, isMin);
                    if (tmp > times)
                        times = tmp;
                }
            }
            case SequenceExpr seqExpr -> {
                for (Expr subExpr : seqExpr.getExprs()) {
                    times += getRegexElementNumber(subExpr, isMin);
                }
            }
            case LoopExpr loopExpr -> {
                if (isMin && loopExpr.getMin() == 0)
                    break;
                int loopTimes = isMin ? loopExpr.getMin() : loopExpr.getMax();
                if (loopTimes == -1)
                    loopTimes = 1;
                times = getRegexElementNumber(loopExpr.getBody(), isMin);
                times = loopTimes * times;
            }
            case GroupExpr groupExpr -> times = getRegexElementNumber(groupExpr.getBody(), isMin);
            case CharExpr ignored -> times = 1;
            default -> {}
        }
        return times;
    }

    /**
     * calc the suitable loop times at runtime to guide string generating
     * @param expr the expr used to generating string
     * @param maxLength the max string length
     * */
    public static int getMaxLoopTimes(Expr expr, int maxLength) {
        int max = 300;
        int high = maxLength - getRegexElementNumber(expr, true);
        int low = 0;
        while (low < high) {
            int mid = (high + low + 1) / 2;
            int p = power(expr, mid);
            if (p > max) {
                high = mid - 1;
            }
            else {
                low = mid;
            }
        }
        return low;
    }

    /**
     * calculate the number of strings that can be generated when the number of loops is n
     * @param expr expr used to generate string
     * @param n the loop times
     * @return the number of strings that can be generated
     * */
    private static int power(Expr expr, int n) {
        int sum = 1;
        switch (expr) {
            case RegexExpr regexExpr -> sum = power(regexExpr.getExpr(), n);
            case BranchExpr branchExpr -> {
                for (SequenceExpr seqExpr : branchExpr.getBranchs()) {
                    sum += power(seqExpr, n);
                    // check whether the result overflow
                    if (sum < 0)
                        return Integer.MAX_VALUE;
                }
            }
            case SequenceExpr seqExpr -> {
                for (Expr subExpr : seqExpr.getExprs()) {
                    // check whether the result overflow
                    long tmp = (long)sum * (long)power(subExpr, n);
                    if ((int)tmp != tmp)
                        return Integer.MAX_VALUE;
                    sum = (int)tmp;
                }
            }
            case LoopExpr loopExpr -> {
                int body = power(loopExpr.getBody(), n);
                for (int i = 0; i <= n; i++) {
                    sum += Math.pow(body, i);
                    // check whether the result overflow
                    if (sum < 0)
                        return Integer.MAX_VALUE;
                }
            }
            case GroupExpr groupExpr -> sum  = power(groupExpr.getBody(), n);
            default -> {}
        }
        return sum;
    }

    /**
     * This method is used to get one mutex path.
     * The path doesn't belong to shortPath or longPath
     * */
    public static List<Path> getMutexPath(Path shortPath, Path longPath) {
        assert shortPath.getLength() <= longPath.getLength();
        List<Path> paths = new ArrayList<>();
        for (int k = 0; k <= 1; k++) {
            boolean isOk = false;
            Path newPath = new Path();
            if (k > longPath.getLength())
                return paths;
            if (k != 0)
                newPath.add(longPath.getRangeSets().get(k - 1));
            int index = k;
            int times = (longPath.getLength() - k) / shortPath.getLength();
            for (int time = 0; time < times; time++) {
                for (int i = 0; i < shortPath.getLength(); i++) {
                    if (Thread.currentThread().isInterrupted())
                        return paths;
                    CharRangeSet rangeSet1 = shortPath.getRangeSets().get(i);
                    CharRangeSet rangeSet2 = longPath.getRangeSets().get(index++);
                    rangeSet1 = rangeSet1.negate();
                    CharRangeSet tmp = rangeSet1.and(rangeSet2);
                    if (!tmp.isEmpty()) {
                        isOk = true;
                        newPath.add(tmp);
                    } else
                        newPath.add(rangeSet2);
                }
            }
            if (isOk) {
                for (int j = index; j < longPath.getLength(); j++) {
                    newPath.add(longPath.getRangeSets().get(j));
                }
                paths.add(newPath);
            }
        }
        return paths;
    }

    public static List<Path> getPathsOf(Expr expr) {
        int maxLength = Math.min(RegexUtil.getRegexElementNumber(expr, false) + 5, 50000);
        int maxLoopTimes = RegexUtil.getMaxLoopTimes(expr, maxLength);
        List<Path> paths = new ArrayList<>(StringGenerator.gen(
                expr,
                maxLoopTimes,
                maxLength,
                true
        ));
        paths.add(StringGenerator.quickGen(expr));
        paths = paths.stream().filter(path -> !path.isEmpty()).toList();
        return paths;
    }
}