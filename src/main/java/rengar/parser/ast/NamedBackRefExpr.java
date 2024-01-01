package rengar.parser.ast;

public class NamedBackRefExpr extends BackRefExpr {
    private final String name;

    public NamedBackRefExpr(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String genString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\\k<");
        sb.append(name);
        sb.append('>');
        return sb.toString();
    }

    @Override
    public NamedBackRefExpr copy() {
        return new NamedBackRefExpr(new String(name));
    }
}
