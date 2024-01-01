package rengar.parser.ast;

// ^
public class BeginExpr extends AnchorExpr {
    @Override
    public String genString() {
        return "^";
    }

    @Override
    public BeginExpr copy() {
        return new BeginExpr();
    }
}
