package rengar.parser.ast;

public class RegularGroupExpr extends GroupExpr {
    private final int index;
    public RegularGroupExpr(RegexExpr body, int index) {
        super(body);
        this.index = index;
    }
    public int getIndex() {
        return index;
    }
    @Override
    public RegularGroupExpr copy() {
        return new RegularGroupExpr(getBody().copy(), index);
    }
}
