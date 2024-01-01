package rengar.preprocess;

import rengar.parser.ast.*;
import rengar.parser.range.CharRangeSet;

public class CommonPreprocess {
    public void handle(RegexExpr regexExpr) {
        RegexExpr backup = regexExpr.copy();
        replace(backup);
        regexExpr.getExpr().addAll(backup.getExpr());
    }

    private void replace(Expr expr) {
        switch (expr) {
            case RegexExpr regexExpr -> replace(regexExpr.getExpr());
            case BranchExpr branchExpr -> branchExpr.forEach(this::replace);
            case SequenceExpr seqExpr -> seqExpr.forEach(this::replace);
            case LoopExpr loopExpr -> {
                Expr loopBody = loopExpr.getBody();
                replace(loopBody);
                if (loopBody instanceof GroupExpr groupExpr) {
                    Expr result = tryMerge(groupExpr);
                    if (result != groupExpr) {
                        loopExpr.setBody(result);
                    }
                }
            }
            case GroupExpr groupExpr -> {
                RegexExpr regexExpr = groupExpr.getBody();
                replace(regexExpr);
                Expr result = tryMerge(groupExpr);
                if (result != groupExpr) {
                    SequenceExpr newSequence = new SequenceExpr();
                    newSequence.add(result);
                    regexExpr.getExpr().getBranchs().clear();
                    regexExpr.getExpr().getBranchs().add(newSequence);
                }
            }
            case LookaroundExpr lookaroundExpr -> replace(lookaroundExpr.getCond());
            default -> {}
        }
    }

    private Expr tryMerge(GroupExpr groupExpr) {
        BranchExpr branchExpr = groupExpr.getBody().getExpr();
        if (branchExpr.getSize() < 2)
            return groupExpr;
        CharRangeSet rangeSet = new CharRangeSet();
        for (SequenceExpr branch : branchExpr) {
            if (branch.getSize() != 1)
                return groupExpr;
            Expr subExpr = branch.get(0);
            if (!(subExpr instanceof CharExpr charExpr))
                return groupExpr;
            switch (charExpr) {
                case SingleCharExpr sc -> rangeSet.addOneChar(sc.getChar());
                case CharRangeExpr cr -> rangeSet.addRangeSet(cr.getRangeSet());
                default -> {}
            }
        }
        CharRangeExpr rangeExpr = new CharRangeExpr();
        rangeExpr.setRangeSet(rangeSet);
        return rangeExpr;
    }
}
