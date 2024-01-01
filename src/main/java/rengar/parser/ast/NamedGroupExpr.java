package rengar.parser.ast;

public class NamedGroupExpr extends GroupExpr {
    private final String name;
    private final int index;
    public NamedGroupExpr(RegexExpr body, int index, String name) {
        super(body);
        this.name = name;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String genString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(?<");
        sb.append(name);
        sb.append(">");
        sb.append(getBody().genString());
        sb.append(')');
        return sb.toString();
    }

    @Override
    public NamedGroupExpr copy() {
        return new NamedGroupExpr(getBody().copy(), index, new String(name));
    }
}
