package rengar.parser.ast;

public abstract class CharExpr extends Expr {
    public void setStr(String string) {
        this.string = string;
    }
    @Override
    public abstract CharExpr copy();
}