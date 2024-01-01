package rengar.checker;

import rengar.checker.pattern.DisturbFreePattern;
import rengar.checker.pattern.ReDoSPattern;
import rengar.config.GlobalConfig;
import rengar.parser.RegexParser;
import rengar.parser.ast.BranchExpr;
import rengar.parser.ast.RegexExpr;
import rengar.parser.ast.SequenceExpr;
import rengar.parser.exception.PatternSyntaxException;
import rengar.preprocess.PreprocessPipeline;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DisturbFreeChecker {
    private final Set<DisturbFreePattern> freePatterns = new HashSet<>();

    private boolean hasBranch = false;
    public List<DisturbFreePattern> getFreePatterns() {
        return freePatterns.stream().toList();
    }

    public DisturbFreeChecker(String patternStr, RegexParser.Language lanType) throws PatternSyntaxException {
        RegexParser parser = RegexParser.createParser(lanType, patternStr);
        RegexExpr regexExpr = parser.parse();
        analyse(regexExpr);
    }

    private List<ReDoSPattern> getPatternsOf(RegexExpr regexExpr) {
        ReDoSChecker checker = new ReDoSChecker(regexExpr);
        checker.analyse();
        return checker.getPatterns();
    }

    public boolean hasBranch() {
        return hasBranch;
    }
    public void analyse(RegexExpr root) {
        Future<Void> future = GlobalConfig.executor.submit(() -> {
            RegexExpr regexExpr = root.copy();
            hasBranch = regexExpr.getExpr().getSize() != 1;
            PreprocessPipeline.handle(regexExpr);
            Queue<SequenceExpr> queue = new LinkedList<>(regexExpr.getExpr().getBranchs());

            List<ReDoSPattern> patterns = getPatternsOf(regexExpr);
            for (ReDoSPattern pattern : patterns) {
                if (Thread.currentThread().isInterrupted())
                    break;
                DisturbFreePattern freePattern = new DisturbFreePattern(pattern);
                freePatterns.add(freePattern);
            }

            if (!GlobalConfig.option.isIngoreDisturbance()) {
                for (int i = 0; i < queue.size(); i++) {
                    if (Thread.currentThread().isInterrupted())
                        break;
                    SequenceExpr curSeqExpr = queue.poll();
                    RegexExpr wrapper = createWrapper(curSeqExpr);
                    patterns = getPatternsOf(wrapper);
                    for (ReDoSPattern pattern : patterns) {
                        DisturbFreePattern freePattern = new DisturbFreePattern(
                                pattern, new LinkedList<>(queue));
                        freePatterns.add(freePattern);
                    }
                    queue.add(curSeqExpr);
                }
            }
            return null;
        });
        try {
            future.get(GlobalConfig.option.getStaticTimeout(), TimeUnit.SECONDS);
        } catch (Exception ignored) {
            future.cancel(true);
        }
    }

    private RegexExpr createWrapper(SequenceExpr seqExpr) {
        RegexExpr wrapper = new RegexExpr(new BranchExpr());
        wrapper.getExpr().add(seqExpr);
        return wrapper;
    }
}
