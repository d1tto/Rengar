package rengar.generator.path;

import rengar.checker.pattern.DisturbType;
import rengar.parser.range.*;
import java.util.*;

public class Path {
    private final List<CharRangeSet> path = new ArrayList<>();
    private DisturbType disturbType = new DisturbType();

    public Path() {}

    public Path(Path other) {
        path.addAll(other.path);
        disturbType = other.disturbType;
    }

    public void setDisturbType(DisturbType type) {
        this.disturbType = type;
    }

    public DisturbType getDisturbType() {
        return disturbType;
    }

    public boolean isDisturbFree() {
        return disturbType.isDisturbFree();
    }

    public Path copy() {
        Path newPath = new Path();
        for (CharRangeSet rangeSet : path) {
            newPath.path.add(rangeSet.copy());
        }
        newPath.disturbType = this.disturbType.copy();
        return newPath;
    }

    public Path slice(int begin) {
        return slice(begin, this.path.size());
    }

    public Path slice(int begin, int end) {
        Path newPath = new Path();
        if (begin >= this.path.size())
            return newPath;
        if (end > this.path.size())
            end = this.path.size();
        for (int i = begin; i < end; i++) {
            newPath.add(this.path.get(i).copy());
        }
        newPath.disturbType = this.disturbType.copy();
        return newPath;
    }

    public List<CharRangeSet> getRangeSets() {
        return path;
    }

    public boolean isEmpty() {
        return path.isEmpty();
    }

    public int getLength() {
        return path.size();
    }

    public boolean hasEmptySet() {
        if (path.isEmpty())
            return true;
        for (CharRangeSet rangeSet : path) {
            if (rangeSet.isEmpty())
                return true;
        }
        return false;
    }

    public void add(CharRangeSet charExpr) {
        path.add(charExpr);
    }

    public void add(int... c) {
        CharRangeSet tmp = new CharRangeSet();
        tmp.addOneChar(c);
        path.add(tmp);
    }

    public void add(Path other) {
        for (CharRangeSet rangeSet : other.path)
            add(rangeSet);
    }

    public static Set<Path> intersect(Set<Path> pathList1, Set<Path> path2List) {
        Set<Path> paths = new HashSet<>();
        for (Path path1 : pathList1) {
            for (Path path2 : path2List) {
                if (path1.getLength() != path2.getLength())
                    continue;
                Path path = path1.intersect(path2);
                if (path.hasEmptySet())
                    continue;
                paths.add(path);
            }
        }
        return paths;
    }

    static public Path intersect(Path... paths) {
        Path newPath = null;
        for (Path path : paths) {
            if (newPath == null) {
                newPath = path;
                continue;
            }
            newPath = newPath.intersect(path);
        }
        return newPath;
    }

    public Path intersect(Path other) {
        assert path.size() == other.path.size();
        Path newPath = new Path();
        for (int i = 0; i < path.size(); i++) {
            CharRangeSet out = path.get(i).and(other.path.get(i));
            newPath.add(out);
        }
        newPath.disturbType = this.disturbType.copy();
        return newPath;
    }

    public int[] genValue() {
        int[] val = new int[path.size()];
        int i = 0;
        for (CharRangeSet rangeSet : path) {
            assert rangeSet.isEmpty();
            val[i++] = rangeSet.getRanges().get(0).begin;
        }
        return val;
    }

    @Override
    public String toString() {
        return path.toString();
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof Path p) {
            return p.path.equals(path);
        }
        return false;
    }
}
