package rengar.parser.ast;

public abstract class GroupExpr extends Expr {
    private final RegexExpr body;
    public GroupExpr(RegexExpr body) {
        this.body = body;
    }
    public RegexExpr getBody() {
        return body;
    }

    public String genString() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(body.genString());
        sb.append(')');
        return sb.toString();
    }

    @Override
    public abstract GroupExpr copy();
}