package rengar.base;

import rengar.parser.ast.*;

import java.util.HashMap;

/**
 * nullable(r) = true when ε ∈ L(r) otherwise nullable(r) = false
 **/
public class RegexNullable {
    private Expr root;

    private HashMap<Integer, Boolean> regularGroupMap = new HashMap<>();
    private HashMap<String, Boolean> namedGroupMap = new HashMap<>();

    public RegexNullable(Expr root) {
        this.root = root;
    }

    // the return value meaning is whether the expression can be null
    public boolean visitExpr(Expr expr) {
        return switch (expr) {
            case RegexExpr regexExpr -> visitExpr(regexExpr.getExpr());
            case BranchExpr branchExpr -> visitBranchExpr(branchExpr);
            case GroupExpr groupExpr -> visitGroupExpr(groupExpr);
            case BackRefExpr backRefExpr -> visitBackRefExpr(backRefExpr);
            case SequenceExpr seqExpr -> visitSequenceExpr(seqExpr);
            case LoopExpr loopExpr -> visitLoopExpr(loopExpr);
            case LookaroundExpr ignored -> true;
            default -> false;
        };
    }

    boolean visitBranchExpr(BranchExpr expr) {
        boolean isNullable = false;
        for (Expr subExpr : expr.getBranchs()) {
            if (visitExpr(subExpr)) {
                isNullable = true;
                break;
            }
        }
        return isNullable;
    }

    boolean visitGroupExpr(GroupExpr expr) {
        boolean isNullable = visitExpr(expr.getBody());
        switch (expr) {
            case RegularGroupExpr regularGroupExpr -> {
                regularGroupMap.put(regularGroupExpr.getIndex(), isNullable);
            }
            case NamedGroupExpr namedGroupExpr -> {
                namedGroupMap.put(namedGroupExpr.getName(), isNullable);
                regularGroupMap.put(namedGroupExpr.getIndex(), isNullable);
            }
            default -> {}
        }
        return isNullable;
    }

    boolean visitBackRefExpr(BackRefExpr expr) {
        Boolean is = switch (expr) {
            case RegularBackRefExpr regularBackRefExpr ->
                regularGroupMap.get(regularBackRefExpr.getIndex());
            case NamedBackRefExpr namedBackRefExpr ->
                namedGroupMap.get(namedBackRefExpr.getName());
            default -> false;
        };
        if (is == null)
            return false;
        return is;
    }

    boolean visitSequenceExpr(SequenceExpr expr) {
        boolean isNullable = true;
        for (Expr subExpr : expr.getExprs()) {
            if (!visitExpr(subExpr)) {
                isNullable = false;
                break;
            }
        }
        return isNullable;
    }

    boolean visitLoopExpr(LoopExpr expr) {
        if (expr.getMin() == 0)
            return true;
        return visitExpr(expr.getBody());
    }
}
