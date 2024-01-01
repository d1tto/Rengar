package rengar.checker.pattern;

import rengar.parser.ast.*;

abstract class SpecialPattern extends ReDoSPattern {
    public SpecialPattern(RegexExpr regexExpr, Expr attackableExpr,
                          SequenceExpr upperLevelExpr) {
        super(regexExpr, attackableExpr, upperLevelExpr);
    }

    @Override
    public abstract String getType();

    @Override
    protected boolean hasAttackableExpr(Expr expr) {
        boolean has = false;
        switch (expr) {
            case RegexExpr regexExpr -> {
                for (SequenceExpr seqExpr : regexExpr.getExpr().getBranchs())
                    has |= hasAttackableExpr(seqExpr);
            }
            case SequenceExpr seqExpr -> {
                if (seqExpr.equals(upperLevelExpr))
                    return true;
                /**
                 * when string is ^(([^:\/?#]+):)?(\/\/(([^@\/]*)@)?([^\/?#:]*)(:(\d*))?)?([^?#]*)(\?([^#]*))?(#(.*))?
                 * using `seqExpr.toString().equals(attackableExpr.toString())` is not work.
                 * because there exist more than one branches that contain attackableExpr.
                 * using attackableExpr.toString() as mark will remove the branch that we don't want
                 * then it will affect the correctness of StripLoop
                 * */
                for (Expr subExpr : seqExpr.getExprs()) {
                    if (subExpr.toString().contains(upperLevelExpr.toString())) {
                        has = true;
                        break;
                    }
                    has |= hasAttackableExpr(subExpr);
                }
            }
            case GroupExpr groupExpr -> has = hasAttackableExpr(groupExpr.getBody());
            case LoopExpr loopExpr -> has = hasAttackableExpr(loopExpr.getBody());
            default -> {
            }
        }
        return has;
    }
}
