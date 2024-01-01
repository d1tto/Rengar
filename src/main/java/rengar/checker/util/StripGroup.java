package rengar.checker.util;

import rengar.parser.ast.*;

import java.util.HashMap;
import java.util.List;

// eliminate the group expr as much as possible
// for example
//      a((b)) -> ab
//      a((a|b)) -> a(a|b) note: the `()` here is used to guarantee priority
//      a(bbb) -> abbb
//      a(bbb)\1 -> abbbbbb
class StripGroup {
    private static HashMap<Integer, Expr> regularGroupMap = new HashMap<>();
    private static HashMap<String, Expr> namedGroupMap = new HashMap<>();

    public static Expr strip(Expr root) {
        regularGroupMap.clear();
        namedGroupMap.clear();
        replace(root);
        return root;
    }

    private static void replace(Expr root) {
        switch (root) {
            case RegexExpr regexExpr -> replace(regexExpr.getExpr());
            case BranchExpr branchExpr -> {
                for (Expr branch : branchExpr.getBranchs())
                    replace(branch);
            }
            case SequenceExpr seqExpr -> {
                List<Expr> subExprs = seqExpr.getExprs();
                int i = 0;
                while (i < subExprs.size()) {
                    Expr subExpr = subExprs.get(i);
                    if (subExpr instanceof GroupExpr groupExpr) {
                        // depth-first
                        replace(groupExpr.getBody());
                        subExprs.remove(i);
                        Expr outExpr = extractGroupExpr(groupExpr);

                        // record group info
                        switch (groupExpr) {
                            case RegularGroupExpr regularGroupExpr -> {
                                regularGroupMap.put(regularGroupExpr.getIndex(), outExpr);
                            }
                            case NamedGroupExpr namedGroupExpr -> {
                                namedGroupMap.put(namedGroupExpr.getName(), outExpr);
                                regularGroupMap.put(namedGroupExpr.getIndex(), outExpr);
                            }
                            default -> {}
                        }

                        // replace AST node depending on the type of outExpr
                        switch (outExpr) {
                            case GroupExpr outGroupExpr -> {
                                subExprs.add(i, outGroupExpr);
                                i += 1;
                            }
                            case SequenceExpr outSeqExpr -> {
                                for (Expr expr : outSeqExpr.getExprs())
                                    subExprs.add(i++, expr);
                            }
                            default -> {}
                        }
                    } else if (subExpr instanceof BackRefExpr) {
                        subExprs.remove(i);
                        Expr outExpr = switch (subExpr) {
                            case RegularBackRefExpr regularBackRefExpr ->
                                    regularGroupMap.get(regularBackRefExpr.getIndex());
                            case NamedBackRefExpr namedBackRefExpr ->
                                    namedGroupMap.get(namedBackRefExpr.getName());
                            default -> null;
                        };
                        switch (outExpr) {
                            case GroupExpr outGroupExpr -> {
                                subExprs.add(i, outGroupExpr);
                                i += 1;
                            }
                            case SequenceExpr outSeqExpr -> {
                                for (Expr expr : outSeqExpr.getExprs())
                                    subExprs.add(i++, expr);
                            }
                            default -> {}
                        }
                    } else if (subExpr instanceof LoopExpr loopExpr) {
                        if (loopExpr.getBody() instanceof GroupExpr groupExpr) {
                            replace(groupExpr.getBody());
                            switch (groupExpr) {
                                case RegularGroupExpr regularGroupExpr -> {
                                    regularGroupMap.put(regularGroupExpr.getIndex(), groupExpr.getBody());
                                }
                                case NamedGroupExpr namedGroupExpr -> {
                                    namedGroupMap.put(namedGroupExpr.getName(), groupExpr.getBody());
                                    regularGroupMap.put(namedGroupExpr.getIndex(), groupExpr.getBody());
                                }
                                default -> {}
                            }
                        }
                        else if (loopExpr.getBody() instanceof BackRefExpr backRefExpr) {
                            Expr outExpr = switch (backRefExpr) {
                                case RegularBackRefExpr regularBackRefExpr ->
                                        regularGroupMap.get(regularBackRefExpr.getIndex());
                                case NamedBackRefExpr namedBackRefExpr ->
                                        namedGroupMap.get(namedBackRefExpr.getName());
                                default -> null;
                            };
                            loopExpr.setBody(outExpr);
                        }
                        i += 1;
                    } else
                        i += 1;
                }
            }
            default -> {}
        }
    }

    private static Expr extractGroupExpr(GroupExpr groupExpr) {
        RegexExpr regexExpr = (RegexExpr)groupExpr.getBody();
        BranchExpr branchExpr = (BranchExpr)regexExpr.getExpr();
        if (branchExpr.getBranchs().size() > 1)
            return groupExpr;
        return branchExpr.getBranchs().get(0);
    }
}
