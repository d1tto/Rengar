package rengar.preprocess;

import rengar.parser.ast.*;
import rengar.parser.ast.LookaroundExpr;
import rengar.parser.range.CharRange;
import rengar.parser.range.CharRangeSet;
import java.util.List;

/**
 * when we process lookaround, the regex mustn't contain back-reference
 * */
public class HandleLookaround {
    public void handle(Expr expr) {
        switch (expr) {
            case RegexExpr regexExpr -> handle(regexExpr.getExpr());
            case BranchExpr branchExpr -> branchExpr.getBranchs().forEach(this::handle);
            case SequenceExpr seqExpr -> handleSequenceExpr(seqExpr);
            case GroupExpr groupExpr -> handle(groupExpr.getBody());
            case LoopExpr loopExpr -> handle(loopExpr.getBody());
            default -> {}
        }
    }

    private void handleSequenceExpr(SequenceExpr seqExpr) {
        // handle lookahead from right to left
        for (int i = seqExpr.getSize() - 1; i >= 0; i--) {
            Expr subExpr = seqExpr.getExprs().get(i);
            if (subExpr instanceof LookaroundExpr lookaround) {
                if (!judge(lookaround.getCond()))
                    continue;
                switch (lookaround) {
                    case LookaheadExpr lookahead -> {
                        if (lookahead.isNot())
                            break;
                        try {
                            handleLookahead(lookahead, i, seqExpr);
                        } catch (Exception ignored) {}
                    }
                    case LookbehindExpr lookbehind -> {

                    }
                    default -> {}
                }
            }
        }
    }

    /**
     * this method is used to judge whether the lookaround can be processed by us
     * @param expr the body of lookaround
     * */
    private boolean judge(Expr expr) {
        boolean ok = true;
        switch (expr) {
            case RegexExpr regexExpr -> ok = judge(regexExpr.getExpr());
            case BranchExpr branchExpr -> {
                List<SequenceExpr> branchs = branchExpr.getBranchs();
                if (branchs.size() > 1)
                    return false;
                ok = judge(branchs.get(0));
            }
            case SequenceExpr seqExpr -> {
                for (Expr subExpr : seqExpr.getExprs()) {
                    ok = judge(subExpr);
                    if (!ok)
                        return false;
                }
            }
            case LoopExpr loopExpr -> ok = judge(loopExpr.getBody());
            case GroupExpr groupExpr -> ok = judge(groupExpr.getBody());
            case LookaroundExpr ignored -> ok = true;
            default -> {}
        }
        return ok;
    }

    private void handleLookahead(LookaheadExpr lookahead, int pos, SequenceExpr seqExpr) throws Exception {
        int unwind = 4;
        SequenceExpr limitSequence = getLimitOf(lookahead);
        List<Expr> subExprs = seqExpr.getExprs();
        boolean stop = false;
        int i = 0;
        int j = pos + 1;
        while (i < limitSequence.getSize()) {
            if (stop || j >= seqExpr.getSize() || i >= limitSequence.getSize())
                break;
            Expr limitExpr = limitSequence.get(i);
            Expr subExpr = subExprs.get(j);
            switch (limitExpr) {
                case CharExpr limitCharExpr -> {
                    CharRangeSet limitRangeSet = getRangeSetOf(limitCharExpr);
                    switch (subExpr) {
                        case SingleCharExpr singleChar -> {
                            int c = singleChar.getChar();
                            if (!contains(limitRangeSet, c)) {
                                throw new Exception();
                            }
                            limitSequence.remove(i);
                            j += 1;
                        }
                        case CharRangeExpr rangeExpr -> {
                            CharRangeSet andSet = rangeExpr.getRangeSet().and(limitRangeSet);
                            // (?=abc)[ab]\w[abc]  ==>  (?=bc)[b]\w[abc]
                            // i point to `b`
                            // j point to `\w`
                            limitSequence.remove(i);
                            seqExpr.set(j, andSet);
                            j += 1;
                        }
                        case LoopExpr loopExpr -> {
                            Expr loopBody = loopExpr.getBody().copy();
                            if (loopBody instanceof GroupExpr groupExpr) {
                                SequenceExpr bodySequence = extractGroupExpr(groupExpr);
                                if (bodySequence == null) {
                                    stop = true;
                                    break;
                                }
                                // (?=a)(abc)+  ==> (?=a)abc(abc)+
                                // i point to `a`
                                // j point to `a`
                                subExprs.remove(j);
                                updateLoop(loopExpr);
                                if (loopExpr.getMax() != 0)
                                    bodySequence.add(loopExpr);
                                seqExpr.insert(j, bodySequence);
                            } else if (loopBody instanceof CharExpr charExpr) {
                                CharRangeSet rangeSet = getRangeSetOf(charExpr);
                                CharRangeSet andSet = rangeSet.and(limitRangeSet);
                                limitSequence.remove(i);
                                seqExpr.insert(j, andSet);
                                j += 1;
                                updateLoop(loopExpr);
                                if (loopExpr.getMax() == 0)
                                    subExprs.remove(j);
                            } else {
                                stop = true;
                            }
                        }
                        default -> stop = true; // maybe group
                    } // switch (subExpr)
                }
                case LoopExpr limitLoopExpr -> {
                    Expr limitLoopBody = limitLoopExpr.getBody().copy();
                    if (limitLoopBody instanceof GroupExpr limitGroup) {
                        SequenceExpr bodySequence = extractGroupExpr(limitGroup);
                        if (bodySequence == null || unwind == 0) {
                            stop = true;
                            break;
                        }
                        limitSequence.remove(i);
                        updateLoop(limitLoopExpr);
                        if (limitLoopExpr.getMax() != 0)
                            bodySequence.add(limitLoopExpr);
                        limitSequence.insert(i, bodySequence);
                        unwind -= 1;
                        break;
                    }
                    CharRangeSet limitRangeSet = getRangeSetOf(limitLoopExpr);
                    int limitMin = limitLoopExpr.getMin();
                    int limitMax = limitLoopExpr.getMax();
                    switch (subExpr) {
                        case SingleCharExpr single -> {
                            if (!contains(limitRangeSet, single.getChar())) {
                                throw new Exception();
                            }
                            updateLoop(limitLoopExpr);
                            if (limitLoopExpr.getMax() == 0)
                                limitSequence.remove(i);
                            j += 1;
                        }
                        case CharRangeExpr rangeExpr -> {
                            CharRangeSet andSet = rangeExpr.getRangeSet().and(limitRangeSet);
                            seqExpr.set(j, andSet);
                            updateLoop(limitLoopExpr);
                            if (limitLoopExpr.getMax() == 0)
                                limitSequence.remove(i);
                            j += 1;
                        }
                        case LoopExpr loopExpr -> {
                            Expr loopBody = loopExpr.getBody().copy();
                            int min = loopExpr.getMin(), max = loopExpr.getMax();
                            if (loopBody instanceof GroupExpr groupExpr) {
                                SequenceExpr bodySequence = extractGroupExpr(groupExpr);
                                if (bodySequence == null || unwind == 0) {
                                    stop = true;
                                    break;
                                }
                                // (?=a)(abc)+  ==> (?=a)abc(abc)+
                                // i point to `a`
                                // j point to `a`
                                seqExpr.remove(j);
                                updateLoop(loopExpr);
                                if (loopExpr.getMax() != 0)
                                    bodySequence.add(loopExpr);
                                seqExpr.insert(j, bodySequence);
                                unwind -= 1;
                            }

                            if (loopBody instanceof CharExpr charExpr) {
                                CharRangeSet andSet = getRangeSetOf(charExpr).and(limitRangeSet);
                                // match failed but the limit loop can be empty
                                if (andSet.isEmpty() && limitMin == 0) {
                                    limitSequence.remove(i);
                                    break;
                                }
                                if (!andSet.isEmpty() && limitMin > 0) {
                                    if (max != -1 && limitMin > max)
                                        throw new Exception();
                                    for (int k = 0; k < limitMin; k++) {
                                        seqExpr.insert(k + j, andSet);
                                    }
                                    j += limitMin;
                                    updateLoop(loopExpr, limitMin);
                                    if (loopExpr.getMax() == 0)
                                        seqExpr.remove(j);
                                    updateLoop(limitLoopExpr, limitMin);
                                    if (limitLoopExpr.getMax() == 0)
                                        limitSequence.remove(i);
                                } else
                                    stop = true;
                            } else {
                                stop = true;
                            }
                        }
                        default -> stop = true;
                    }
                }
                case AnchorExpr anchorExpr -> {
                    limitSequence.remove(i);
                    if (subExpr instanceof AnchorExpr) {
                        j += 1;
                    }
                }
                default -> stop = true;
            } // switch (limitExpr)
        }
        seqExpr.remove(pos);
        if (limitSequence.getSize() != 0) {
            seqExpr.insert(j - 1, lookahead);
        }
    }

    private void updateLoop(LoopExpr loopExpr) {
        updateLoop(loopExpr, 1);
    }

    private void updateLoop(LoopExpr loopExpr, int times) {
        int loopMin = loopExpr.getMin();
        int loopMax = loopExpr.getMax();
        loopMin -= times;
        if (loopMin < 0)
            loopMin = 0;
        if (loopMax != -1)
            loopMax -= times;
        loopExpr.setMin(loopMin);
        loopExpr.setMax(loopMax);
    }

    private CharRangeSet getRangeSetOf(Expr expr) {
        CharRangeSet rangeSet = null;
        switch (expr) {
            case SingleCharExpr single -> {
                rangeSet = new CharRangeSet();
                rangeSet.addOneChar(single.getChar());
            }
            case CharRangeExpr rangeExpr -> {
                rangeSet = rangeExpr.getRangeSet();
            }
            case LoopExpr loopExpr -> rangeSet = getRangeSetOf(loopExpr.getBody());
            default -> {}
        }
        return rangeSet;
    }

    private boolean contains(CharRangeSet rangeSet, int c) {
        for (CharRange range : rangeSet.getRanges()) {
            if (c >= range.begin && c <= range.end)
                return true;
        }
        return false;
    }

    private SequenceExpr extractGroupExpr(GroupExpr groupExpr) {
        if (hasBranchOrLoop(groupExpr))
            return null;
        return groupExpr.getBody().getExpr().getBranchs().get(0);
    }

    private boolean hasBranchOrLoop(Expr expr) {
        boolean has = false;
        switch (expr) {
            case RegexExpr regexExpr -> has = hasBranchOrLoop(regexExpr.getExpr());
            case BranchExpr branchExpr -> {
                if (branchExpr.getBranchs().size() > 1) {
                    has = true;
                    break;
                }
                has = hasBranchOrLoop(branchExpr.getBranchs().get(0));
            }
            case SequenceExpr seqExpr -> {
                seqExpr.getExprs().forEach(this::hasBranchOrLoop);
            }
            case LoopExpr ignored -> has = true;
            case GroupExpr groupExpr -> hasBranchOrLoop(groupExpr.getBody());
            default -> {}
        }
        return has;
    }

    private SequenceExpr getLimitOf(LookaroundExpr lookaroundExpr) {
        List<SequenceExpr> branchs = lookaroundExpr.getCond().getExpr().getBranchs();
        assert branchs.size() == 1;
        return branchs.get(0);
    }

    /**
     * remove the Lookaround that doesn't have body in SequenceExpr
     * */
    private void cleanSequence(SequenceExpr seqExpr) {
        List<Expr> subExprs = seqExpr.getExprs();
        int i = 0;
        while (i < seqExpr.getSize()) {
            Expr subExpr = seqExpr.getExprs().get(i);
            if (subExpr instanceof LookaroundExpr lookaroundExpr) {
                RegexExpr cond = lookaroundExpr.getCond();
                List<SequenceExpr> branchs = cond.getExpr().getBranchs();
                if (branchs.size() == 0) {
                    subExprs.remove(i);
                    continue;
                }
                if (branchs.size() > 1) {
                    i += 1;
                    continue;
                }
                if (branchs.get(0).getSize() == 0) {
                    subExprs.remove(i);
                    continue;
                }
            }
            i += 1;
        }
    }
}
