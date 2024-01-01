package rengar.parser.ast;

// (?:X)
public class AnonymousGroupExpr extends GroupExpr {
    public AnonymousGroupExpr(RegexExpr body) {
        super(body);
    }

    @Override
    public String genString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(?:");
        sb.append(getBody().genString());
        sb.append(')');
        return sb.toString();
    }

    @Override
    public AnonymousGroupExpr copy() {
        return new AnonymousGroupExpr(getBody().copy());
    }
}