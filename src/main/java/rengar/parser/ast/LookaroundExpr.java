package rengar.parser.ast;

public abstract class LookaroundExpr extends Expr {
    protected RegexExpr cond;
    protected boolean isNot;
    public LookaroundExpr(RegexExpr cond, boolean isNot) {
        this.cond = cond;
        this.isNot = isNot;
    }

    public boolean isNot() {
        return isNot;
    }

    public RegexExpr getCond() {
        return cond;
    }

    @Override
    public abstract LookaroundExpr copy();
}