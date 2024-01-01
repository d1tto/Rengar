package rengar.parser.ast;

public class LookaheadExpr extends LookaroundExpr {
    public LookaheadExpr(RegexExpr cond, boolean isNot) {
        super(cond, isNot);
    }

    @Override
    public String genString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(?");
        if (isNot)
            sb.append('!');
        else
            sb.append('=');
        sb.append(cond.genString());
        sb.append(')');
        return sb.toString();
    }

    @Override
    public LookaheadExpr copy() {
        return new LookaheadExpr(cond.copy(), isNot);
    }
}
