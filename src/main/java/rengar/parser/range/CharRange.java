package rengar.parser.range;

public class CharRange {
    public int begin;
    public int end;
    public CharRange(int begin, int end) {
        this.begin = begin;
        this.end = end;
    }

    public CharRange(int c) {
        this.begin = c;
        this.end = c;
    }

    @Override
    public String toString() {
        if (begin != end)
            return String.format("<0x%x ~ 0x%x>", begin, end);
        else
            return String.format("<0x%x>", begin);
    }

    public CharRange copy() {
        return new CharRange(begin, end);
    }

    public boolean isSingleChar() {
        return begin == end;
    }

    public int getSingleChar() {
        assert begin == end;
        return begin;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(begin) ^ Integer.hashCode(end);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof CharRange cr) {
            return cr.begin == begin && cr.end == end;
        }
        return false;
    }
}