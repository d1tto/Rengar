package rengar.parser.ast;

// (?<=X)
public class LookbehindExpr extends LookaroundExpr {
    public LookbehindExpr(RegexExpr cond, boolean isNot) {
        super(cond, isNot);
    }

    @Override
    public String genString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(?<");
        if (isNot)
            sb.append('!');
        else
            sb.append('=');
        sb.append(cond.genString());
        sb.append(')');
        return sb.toString();
    }

    @Override
    public LookbehindExpr copy() {
        return new LookbehindExpr(cond.copy(), isNot);
    }
}