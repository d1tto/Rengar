package rengar.parser.ast;

public class NonWordBoundaryExpr extends AnchorExpr {
    @Override
    public String genString() {
        return "\\B";
    }

    @Override
    public AnchorExpr copy() {
        return new NonWordBoundaryExpr();
    }
}
