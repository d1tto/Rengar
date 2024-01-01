package rengar.parser.range;

import java.util.*;

public class CharRangeSet {
    private final List<CharRange> ranges = new ArrayList<>();

    private boolean neg = false;

    public List<CharRange> getRanges() {
        return ranges;
    }

    // this function used to process [^XXX] by
    // calculating the supplementary set of the CharRangeSet
    public CharRangeSet negate() {
        sortRangeOrderByBegin();
        CharRangeSet rangeSet = new CharRangeSet();
        int prevEnd = 0;
        for (int i = 0; i < ranges.size(); i++) {
            CharRange curRange = ranges.get(i);
            // special processing is required for the first `CharRange`
            if (prevEnd == 0 && curRange.begin != 0) {
                rangeSet.addRange(prevEnd, curRange.begin - 1);
            }
            if (i + 1 >= ranges.size()) {
                if (curRange.end != Character.MAX_CODE_POINT)
                    rangeSet.addRange(curRange.end + 1, Character.MAX_CODE_POINT);
                break;
            }
            CharRange nextRange = ranges.get(i + 1);
            int begin = curRange.end + 1, end = nextRange.begin - 1;
            if (begin <= end)
                rangeSet.addRange(begin, end);
            prevEnd = nextRange.end;
        }
        rangeSet.neg = true;
        return rangeSet;
    }

    public boolean isNeg() {
        return neg;
    }

    // add a new CharRange
    public void addRange(int begin, int end) {
        ranges.add(new CharRange(begin, end));
        rangeMerge();
    }

    public void addRange(int[]... arr) {
        for (int[] ints : arr) {
            addRange(ints[0], ints[1]);
        }
    }

    // add OneChar
    public void addOneChar(int... cs) {
        for (int c : cs)
            this.addRange(c, c);
    }

    public void addRangeSet(CharRangeSet rangeSet) {
        for (CharRange range : rangeSet.ranges) {
            ranges.add(new CharRange(range.begin, range.end));
        }
        rangeMerge();
    }

    // calculate the union set
    public CharRangeSet union(CharRangeSet rangeSet) {
        CharRangeSet newRangeSet = copy();
        for (CharRange range : rangeSet.ranges) {
            newRangeSet.ranges.add(new CharRange(range.begin, range.end));
        }
        newRangeSet.rangeMerge();
        return newRangeSet;
    }

    // calculate the and set
    public CharRangeSet and(CharRangeSet rangeSet) {
        CharRangeSet newRangeSet = new CharRangeSet();
        for (int i = 0; i < ranges.size(); i++) {
            CharRange curRange = ranges.get(i);
            int curBegin = curRange.begin;
            int curEnd = curRange.end;
            for (int j = 0; j < rangeSet.ranges.size(); j++) {
                CharRange otherRange = rangeSet.ranges.get(j);
                int otherBegin = otherRange.begin;
                int otherEnd = otherRange.end;
                if (curEnd >= otherBegin && curEnd <= otherEnd) {
                    newRangeSet.addRange(
                            Math.max(curBegin, otherBegin),
                            Math.min(curEnd, otherEnd)
                    );
                }
                if (curBegin >= otherBegin && curBegin <= otherEnd) {
                    newRangeSet.addRange(
                            Math.max(curBegin, otherBegin),
                            Math.min(curEnd, otherEnd)
                    );
                }
                if (otherBegin >= curEnd && otherEnd <= curEnd) {
                    newRangeSet.addRange(
                            Math.max(otherBegin, curBegin),
                            Math.min(otherEnd, curEnd)
                    );
                }
                if (curBegin >= otherBegin && curEnd <= otherEnd) {
                    newRangeSet.addRange(curBegin, curEnd);
                }
                if (otherBegin >= curBegin && otherEnd <= curEnd) {
                    newRangeSet.addRange(otherBegin, otherEnd);
                }
            }
        }
        return newRangeSet;
    }

    public boolean isEmpty() {
        return ranges.isEmpty();
    }

    @Override
    public String toString() {
        return ranges.toString();
    }

    private void sortRangeOrderByBegin() {
        ranges.sort((o1, o2) -> o1.begin - o2.begin);
    }

    private void rangeMerge() {
        // firstly, we sort the range set order by range.begin
        sortRangeOrderByBegin();

        for (int i = 0; i < ranges.size(); i++) {
            int begin = ranges.get(i).begin;
            int end = ranges.get(i).end;
            for (int j = i + 1; j < ranges.size(); j++) {
                int nextBegin = ranges.get(j).begin;
                int nextEnd = ranges.get(j).end;
                if (nextBegin <= end) {
                    end = Math.max(nextEnd, end);
                    ranges.remove(j);
                    ranges.set(i, new CharRange(begin, end));
                    j--;
                }
            }
        }
    }

    public CharRangeSet copy() {
        CharRangeSet newRangeSet = new CharRangeSet();
        for (CharRange range : this.ranges) {
            newRangeSet.addRange(range.begin, range.end);
        }
        newRangeSet.neg = neg;
        return newRangeSet;
    }

    public boolean isSubSet(CharRangeSet otherRangeSet) {
        boolean is = true;
        for (CharRange range : ranges) {
            boolean cur = false;
            for (CharRange otherRange : otherRangeSet.ranges) {
                if (range.begin >= otherRange.begin && range.end <= otherRange.end) {
                    cur = true;
                    break;
                }
            }
            if (!cur) {
                is = false;
                break;
            }
        }
        return is;
    }

    public boolean isSingleChar() {
        if (ranges.size() == 1) {
            CharRange range = ranges.get(0);
            return range.isSingleChar();
        }
        return false;
    }

    public int getSingleChar() {
        assert isSingleChar();
        return ranges.get(0).getSingleChar();
    }

    @Override
    public int hashCode() {
        return ranges.hashCode() ^ Boolean.hashCode(neg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof CharRangeSet rangeSet) {
            return rangeSet.ranges.equals(ranges) && rangeSet.neg == neg;
        }
        return false;
    }
}
