package rengar.parser.ast;

public abstract class Expr {
    // used to cache the result of toString()
    protected String string;

    public abstract String genString();

    @Override
    public String toString() {
        if (string == null)
            string = genString();
        return string;
    }

    public abstract Expr copy();
    @Override
    public boolean equals(Object expr) {
        if (expr == null || this.getClass() != expr.getClass())
            return false;
        if (this == expr)
            return true;
        return genString().equals(((Expr) expr).genString());
    }

    @Override
    public int hashCode() {
        string = genString();
        return string.hashCode();
    }
}