package rengar.parser.ast;

public class RegularBackRefExpr extends BackRefExpr {
    private final int index;
    public RegularBackRefExpr(int index) {
        this.index = index;
    }
    public int getIndex() {
        return index;
    }

    @Override
    public String genString() {
        return String.format("\\%d", index);
    }

    @Override
    public RegularBackRefExpr copy() {
        return new RegularBackRefExpr(index);
    }
}
