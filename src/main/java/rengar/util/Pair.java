package rengar.util;

public class Pair<L,R> {
    private L left;
    private R right;

    public Pair(L left, R right) {
        assert left != null;
        assert right != null;

        this.left = left;
        this.right = right;
    }

    public Pair() {}

    public void setLeft(L left) {
        this.left = left;
    }

    public void setRight(R right) {
        this.right = right;
    }

    public L getLeft() { return left; }

    public R getRight() { return right; }

    @Override
    public int hashCode() { return left.hashCode() ^ right.hashCode(); }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o instanceof Pair<?, ?> pair) {
            return this.left.equals(pair.getLeft()) &&
                    this.right.equals(pair.getRight());
        }
        return false;
    }
}