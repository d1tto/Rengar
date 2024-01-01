package rengar.checker.pattern;

import rengar.parser.ast.*;
import java.util.List;

abstract class OneCountingPattern extends ReDoSPattern {
    public OneCountingPattern(RegexExpr regexExpr, LoopExpr attackableExpr,
                              LoopExpr upperLevelExpr) {
        super(regexExpr, attackableExpr, upperLevelExpr);
    }
    @Override
    protected boolean hasAttackableExpr(Expr expr) {
        boolean has = false;
        switch (expr) {
            case RegexExpr regexExpr -> {
                List<SequenceExpr> branchs = regexExpr.getExpr().getBranchs();
                for (SequenceExpr seqExpr : branchs)
                    has |= hasAttackableExpr(seqExpr);
            }
            case SequenceExpr seqExpr -> {
                for (Expr subExpr : seqExpr.getExprs()) {
                    if (subExpr.equals(attackableExpr)) {
                        has = true;
                        break;
                    }
                    has |= hasAttackableExpr(subExpr);
                }
            }
            case GroupExpr groupExpr -> has = hasAttackableExpr(groupExpr.getBody());
            case LoopExpr loopExpr -> has = hasAttackableExpr(loopExpr.getBody());
            default -> {}
        }
        return has;
    }

    @Override
    public abstract String getType();
}