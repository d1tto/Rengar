package rengar.parser.ast;

public abstract class BackRefExpr extends Expr {
    @Override
    public abstract BackRefExpr copy();
}