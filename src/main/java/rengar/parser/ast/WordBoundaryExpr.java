package rengar.parser.ast;

public class WordBoundaryExpr extends AnchorExpr {
    @Override
    public String genString() {
        return "\\b";
    }

    @Override
    public WordBoundaryExpr copy() {
        return new WordBoundaryExpr();
    }
}
