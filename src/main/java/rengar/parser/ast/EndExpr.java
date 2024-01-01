package rengar.parser.ast;

// $
public class EndExpr extends AnchorExpr {
    @Override
    public String genString() {
        return "$";
    }

    @Override
    public EndExpr copy() {
        return new EndExpr();
    }
}