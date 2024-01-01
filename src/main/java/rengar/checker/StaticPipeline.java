package rengar.checker;

import rengar.checker.attack.AttackString;
import rengar.checker.pattern.*;
import rengar.config.GlobalConfig;
import rengar.dynamic.validator.Validator;
import rengar.parser.ReDosHunterPreProcess;
import rengar.parser.RegexParser;
import rengar.parser.exception.PatternSyntaxException;
import rengar.util.Pair;
import java.text.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StaticPipeline {
    public static class Result {
        public enum State {
            SyntaxError, Vulnerable, InternalBug, Normal
        }

        public DisturbType disturbType;
        public long runningTime;
        public State state;
        public List<Pair<DisturbFreePattern, AttackString>> attacks = new LinkedList<>();

        public void add(DisturbFreePattern newPattern, AttackString newAttackString) {
            boolean isOK = true;
            for (Pair<DisturbFreePattern, AttackString> pair : attacks) {
                DisturbFreePattern pattern = pair.getLeft();
                AttackString attackString = pair.getRight();
                if (attackString.equals(newAttackString)
                        && attackString.getDisturbType() == newAttackString.getDisturbType()) {
                    isOK = false;
                    break;
                }
                if (pattern.isDuplicate(newPattern)) {
                    isOK = false;
                    break;
                }
            }
            if (isOK) {
                attacks.add(new Pair<>(newPattern, newAttackString));
            }
        }
    }

    public static Result runWithTimeOut(String patternStr, RegexParser.Language language, boolean findAll) {
        Future<Result> future = GlobalConfig.executor.submit(
                () -> StaticPipeline.run(patternStr, language, findAll)
        );
        try {
            return future.get(GlobalConfig.option.getTotalTimeout(), TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ignored) {
            future.cancel(true);
            return null;
        }
    }

    public static Result run(String patternStr, RegexParser.Language language, boolean findAll) {
        if (!GlobalConfig.option.isQuiet())
            System.out.printf("%s pattern string %s\n", getCurrentDate(), patternStr);
        long startTime = System.currentTimeMillis();
        Result result = new Result();
        result.state = Result.State.Normal;

        if (!GlobalConfig.option.isDisablePreprocess()) {
            // step 1. rengar.preprocess regex string
            patternStr = ReDosHunterPreProcess.process(patternStr);
            if (patternStr == null) {
                result.state = Result.State.SyntaxError;
                result.runningTime = System.currentTimeMillis() - startTime;
                return result;
            }
            if (!GlobalConfig.option.isQuiet())
                System.out.printf("ReDosHunter rengar.preprocess result %s\n", patternStr);
        }
        // step 2. detect ReDoS pattern
        DisturbFreeChecker checker;
        try {
            checker = new DisturbFreeChecker(patternStr, language);
            //rengar.checker.analyse();
        } catch (PatternSyntaxException e) {
            if (!GlobalConfig.option.isQuiet())
                System.out.println(e);
            result.state = Result.State.SyntaxError;
            result.runningTime = System.currentTimeMillis() - startTime;
            return result;
        } catch (Exception | StackOverflowError | OutOfMemoryError e) {
            if (!GlobalConfig.option.isQuiet())
                System.out.println(e);
            result.state = Result.State.InternalBug;
            result.runningTime = System.currentTimeMillis() - startTime;
            return result;
        }

        List<DisturbFreePattern> patternList = checker.getFreePatterns();
        for (DisturbFreePattern pattern : patternList) {
            if (Thread.currentThread().isInterrupted())
                return null;
            try {
                AttackString attackStr = handleReDoSPattern(patternStr, pattern, result);
                if (attackStr != null) {
                    result.state = Result.State.Vulnerable;
                    result.add(pattern, attackStr);
                    if (!findAll)
                        break;
                }
            } catch (PatternSyntaxException ignored) {}
        }

        long endTime = System.currentTimeMillis();
        result.runningTime = endTime - startTime;
        if (!GlobalConfig.option.isQuiet())
            System.out.printf(
                    "%s. It takes %f seconds\n",
                    getCurrentDate(), (double)result.runningTime / 1000);
        DisturbType type = new DisturbType();
        for (Pair<DisturbFreePattern, AttackString> pair : result.attacks) {
            AttackString as = pair.getRight();
            type.setType(as.getDisturbType());
        }
        if (!checker.hasBranch()) {
            type.getTypes().remove(DisturbType.Type.Case1);
            type.getTypes().remove(DisturbType.Type.Case2);
            type.getTypes().remove(DisturbType.Type.Case3);
            if (type.getTypes().isEmpty()) {
                type.getTypes().add(DisturbType.Type.None);
            }
        }
        result.disturbType = type;
        return result;
    }

    private static <T> List<T> tryPopN(int n, List<T> lists) {
        List<T> results = new LinkedList<>();
        while (results.size() != n && !lists.isEmpty()) {
            T elem = lists.get(0);
            lists.remove(0);
            results.add(elem);
        }
        return results;
    }

    private static AttackString handleReDoSPattern(String patternStr,
                                                   DisturbFreePattern pattern,
                                              Result result)
            throws PatternSyntaxException {
        if (!GlobalConfig.option.isQuiet())
            System.out.println(pattern);
        List<AttackString> attackStrList;
        try {
            attackStrList = pattern.generate();
        } catch (Exception | Error ignored) {
            if (!GlobalConfig.option.isQuiet())
                System.out.println("ERROR");
            return null;
        }

        for (AttackString attackStr : attackStrList) {
            if (Thread.currentThread().isInterrupted())
                break;
            if (!GlobalConfig.option.isQuiet())
                System.out.printf("try %s ", attackStr.genReadableStr());
            try {
                Validator validator = new Validator(patternStr, attackStr, pattern.getType());
                if (validator.isVulnerable()) {
                    if (!GlobalConfig.option.isQuiet())
                        System.out.println("SUCCESS");
                    return attackStr;
                }
                else {
                    if (!GlobalConfig.option.isQuiet())
                        System.out.println("FAILED");
                }
            } catch (rengar.dynamic.jdk8.regex.PatternSyntaxException e) {
                if (!GlobalConfig.option.isQuiet())
                    System.out.println("SYNTAX ERROR");
            }
        }
        return null;
    }

    public static String getCurrentDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        return formatter.format(date);
    }
}
