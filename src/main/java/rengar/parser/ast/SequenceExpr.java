package rengar.parser.ast;

import rengar.parser.range.CharRangeSet;
import rengar.util.Pair;

import java.util.*;

public class SequenceExpr extends Expr implements Iterable<Expr> {
    private final List<Expr> exprs = new ArrayList<>();

    public List<Expr> getExprs() {
        return exprs;
    }

    @Override
    public String genString() {
        StringBuilder sb = new StringBuilder();
        for (Expr subExpr : exprs) {
            sb.append(subExpr.genString());
        }
        return sb.toString();
    }

    @Override
    public SequenceExpr copy() {
        SequenceExpr newSeqExpr = new SequenceExpr();
        for (Expr expr : exprs) {
            newSeqExpr.add(expr.copy());
        }
        return newSeqExpr;
    }

    public boolean isEmpty() {
        return exprs.isEmpty();
    }

    public int getSize() {
        return exprs.size();
    }

    public void add(Expr expr) {
        exprs.add(expr);
    }

    public void addAll(SequenceExpr target) {
        addAll(target.getExprs());
    }

    public void addAll(List<Expr> target) {
        exprs.addAll(target);
    }

    public void remove(int index) {
        assert index < exprs.size();
        exprs.remove(index);
    }

    public Expr get(int index) {
        return exprs.get(index);
    }

    public void set(int index, CharRangeSet rangeSet) {
        CharRangeExpr rangeExpr = new CharRangeExpr();
        rangeExpr.setRangeSet(rangeSet);
        set(index, rangeExpr);
    }

    public void set(int index, int c) {
        set(index, new SingleCharExpr(c));
    }

    public void set(int index, Expr expr) {
        exprs.set(index, expr);
    }

    public void insert(int index, SequenceExpr target) {
        insert(index, target.getExprs());
    }

    public void insert(int index, List<Expr> exprList) {
        for (int i = 0; i < exprList.size(); i++) {
            insert(index + i, exprList.get(i));
        }
    }

    public void insert(int index, int c) {
        insert(index, new SingleCharExpr(c));
    }

    public void insert(int index, CharRangeSet rangeSet) {
        CharRangeExpr rangeExpr = new CharRangeExpr();
        rangeExpr.setRangeSet(rangeSet);
        insert(index, rangeExpr);
    }

    public void insert(int index, Expr expr) {
        exprs.add(index, expr);
    }

    public SequenceExpr subSequence(int begin, int end) {
        return subSequence(this, begin ,end);
    }

    /**
     * find the target in source
     * @param source source sequence expr
     * @param target target expr (Sequence Expr or LoopExpr)
     * @return the start index and end index(exclusive)
     * */
    public static Pair<Integer, Integer> indexOf(SequenceExpr source, Expr target) {
        int begin, end;
        switch (target) {
            case SequenceExpr seqExpr -> {
                begin = Collections.indexOfSubList(source.exprs, seqExpr.exprs);
                end = begin + seqExpr.getExprs().size();
            }
            default -> {
                begin = source.getExprs().indexOf(target);
                end = begin + 1;
            }
        };
        return new Pair<>(begin, end);
    }

    /**
     * get the sub-sequence from SequenceExpr
     * @param seqExpr SequenceExpr
     * @param begin begin index(inclusive)
     * @param end end index(exclusive)
     * @return sub-sequence
     * */
    public static SequenceExpr subSequence(SequenceExpr seqExpr, int begin, int end) {
        List<Expr> subList = seqExpr.getExprs().subList(begin, end);
        SequenceExpr subSeq = new SequenceExpr();
        subList.forEach(subSeq::add);
        return subSeq;
    }

    @Override
    public Iterator<Expr> iterator() {
        return exprs.iterator();
    }
}
