package rengar.parser.ast;

// just a wrapper
public class RegexExpr extends Expr {
    private final BranchExpr expr;
    public RegexExpr(BranchExpr expr) {
        this.expr = expr;
    }

    public BranchExpr getExpr() {
        return expr;
    }

    @Override
    public String genString() {
        return expr.genString();
    }

    public RegexExpr copy() {
        return new RegexExpr(expr.copy());
    }
}
