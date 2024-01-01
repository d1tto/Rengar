package rengar.parser.character;

import rengar.parser.ast.CharRangeExpr;
import rengar.parser.range.CharRangeSet;

import java.util.HashMap;

public class UnicodeCategory {
    private static final HashMap<String, CharRangeExpr> map = new HashMap<>();

    public static CharRangeExpr get(String name) {
        return map.get(name);
    }

    private UnicodeCategory(String name, int[]... arr) {
        CharRangeExpr charRangeExpr = new CharRangeExpr();
        charRangeExpr.addRange(arr);
        map.put(name, charRangeExpr);
    }

    private UnicodeCategory(String name, boolean negate, String... others) {
        CharRangeSet rangeSet = new CharRangeSet();
        for (String other : others) {
            rangeSet.addRangeSet(map.get(other).getRangeSet().copy());
        }
        CharRangeExpr tmp = new CharRangeExpr();
        tmp.setRangeSet(rangeSet);
        if (negate)
            tmp.negate();
        map.put(name, tmp);
    }

    private static final UnicodeCategory Sc = new UnicodeCategory("Sc",
            new int[]{0x20A0, 0x20C0});
    private static final UnicodeCategory Lu = new UnicodeCategory("Lu",
            new int[]{0x41, 0x5a}, new int[]{0xc0, 0xd6},
            new int[]{0xd8, 0xde}, new int[]{0x10400, 0x10427},
            new int[]{0x104b0, 0x104d3}, new int[]{0x10c80, 0x10cb2});
    private static final UnicodeCategory Ll = new UnicodeCategory("Ll",
            new int[]{0x61, 0x7a}, new int[]{0xb5, 0xdf}, new int[]{0xe0, 0xf6},
            new int[]{0xf8, 0xff}, new int[]{0x10428, 0x1044f},
            new int[]{0x104d8, 0x104fb}, new int[]{0x10cc0, 0x10cf2},
            new int[]{0x118c0, 0x118df});
    private static final UnicodeCategory N = new UnicodeCategory("N",
            new int[]{0x30, 0x39}, new int[]{0xb2, 0xb3},
            new int[]{0xb9, 0xbc}, new int[]{0xbd, 0xbe},
            new int[]{0x10107, 0x10133}, new int[]{0x10140, 0x10178},
            new int[]{0x1018a, 0x1018b}, new int[]{0x102e1, 0x102fb});
    private static final UnicodeCategory P = new UnicodeCategory("P",
            new int[]{0x21, 0x23}, new int[]{0x25, 0x2a},
            new int[]{0x2c, 0x2f}, new int[]{0x3a, 0x3b},
            new int[]{0x3f, 0x40}, new int[]{0x5b, 0x5d},
            new int[]{0x5f, 0x7b}, new int[]{0x7d, 0xa1},
            new int[]{0xa7, 0xab}, new int[]{0xb6, 0xb7},
            new int[]{0xbb, 0xbf}, new int[]{0x10100, 0x10102},
            new int[]{0x1039f, 0x103d0}, new int[]{0x1056f, 0x10857},
            new int[]{0x1091f, 0x1093f}, new int[]{0x10a50, 0x10a58},
            new int[]{0x10a7f, 0x10af0}, new int[]{0x10af1, 0x10af6},
            new int[]{0x10b39, 0x10b3f}, new int[]{0x10b99, 0x10b9c},
            new int[]{0x10ead, 0x10f55}, new int[]{0x10f56, 0x10f59});
    private static final UnicodeCategory Sm = new UnicodeCategory("Sm",
            new int[]{0x2b, 0x3c}, new int[]{0x3d, 0x3e},
            new int[]{0x7c, 0x7e}, new int[]{0xac, 0xb1},
            new int[]{0xd7, 0xf7}, new int[]{0x1d6c1, 0x1d6db},
            new int[]{0x1d6fb, 0x1d715}, new int[]{0x1d735, 0x1d74f},
            new int[]{0x1d76f, 0x1d789}, new int[]{0x1d7a9, 0x1d7c3});
    private static final UnicodeCategory L = new UnicodeCategory("L",
            new int[]{0x41, 0x5a}, new int[]{0x61, 0x7a}, new int[]{0xaa, 0xb5},
            new int[]{0xba, 0xc0}, new int[]{0xc1, 0xd6}, new int[]{0xd8, 0xf6},
            new int[]{0x10000, 0x1000b}, new int[]{0x1000d, 0x10026},
            new int[]{0x10028, 0x1003a}, new int[]{0x1003c, 0x1003d},
            new int[]{0x1003f, 0x1004d}, new int[]{0x10050, 0x1005d});
    private static final UnicodeCategory Z = new UnicodeCategory("Z",
            new int[]{0x20, 0xa0});
    private static final UnicodeCategory C = new UnicodeCategory("C",
            new int[]{0x0, 0x1f}, new int[]{0x7f, 0x9f}, new int[]{0x110bd, 0x110cd}, new int[]{0x13430, 0x13438});
    private static final UnicodeCategory S = new UnicodeCategory("S",
            new int[]{0x24, 0x2b}, new int[]{0x3c, 0x3e}, new int[]{0x5e, 0x60},
            new int[]{0x7c, 0x7e}, new int[]{0xa2, 0xa6}, new int[]{0xa8, 0xa9},
            new int[]{0xac, 0xae}, new int[]{0xaf, 0xb1}, new int[]{0xb4, 0xb8},
            new int[]{0xd7, 0xf7}, new int[]{0x10137, 0x1013f}, new int[]{0x10179, 0x10189},
            new int[]{0x1018c, 0x1018e}, new int[]{0x10190, 0x1019c},
            new int[]{0x101a0, 0x101d0}, new int[]{0x101d1, 0x101fc},
            new int[]{0x10877, 0x10878}, new int[]{0x10ac8, 0x1173f},
            new int[]{0x11fd5, 0x11ff1}, new int[]{0x16b3c, 0x16b3f});
    private static final UnicodeCategory Po = new UnicodeCategory("Po",
            new int[]{0x21, 0x23}, new int[]{0x25, 0x27}, new int[]{0x2a, 0x2e},
            new int[]{0x2f, 0x3a}, new int[]{0x3b, 0x3f}, new int[]{0x40, 0x5c},
            new int[]{0xa1, 0xa7}, new int[]{0xb6, 0xb7}, new int[]{0x10100, 0x10102},
            new int[]{0x1039f, 0x103d0}, new int[]{0x1056f, 0x10857},
            new int[]{0x1091f, 0x1093f}, new int[]{0x10a50, 0x10a58},
            new int[]{0x10a7f, 0x10af0}, new int[]{0x10af1, 0x10af6},
            new int[]{0x10b39, 0x10b3f});
    private static final UnicodeCategory Ps = new UnicodeCategory("Ps",
            new int[]{0x28, 0x5b});
    private static final UnicodeCategory Pe = new UnicodeCategory("Pe",
            new int[]{0x29, 0x5d});
    private static final UnicodeCategory Nd = new UnicodeCategory("Nd",
            new int[]{0x30, 0x39}, new int[]{0x104a0, 0x104a9});


    private static final UnicodeCategory Space = new UnicodeCategory("Space",
            new int[]{'\n', '\n'}, new int[]{'\r', '\r'},
            new int[]{'\u0085', '\u0085'}, new int[]{'\u2028', '\u2028'},
            new int[]{'\u2029', '\u2029'});
    private static final UnicodeCategory Blank = new UnicodeCategory("Blank",
            false, "Space");
    private static final UnicodeCategory ASCII = new UnicodeCategory("ASCII",
            new int[]{0x00, 0x7F});
    private static final UnicodeCategory Alpha = new UnicodeCategory("Alpha",
            new int[]{'a', 'z'}, new int[]{'A', 'Z'});
    private static final UnicodeCategory Lower = new UnicodeCategory("Lower",
            new int[]{'a', 'z'});
    private static final UnicodeCategory Upper = new UnicodeCategory("Upper",
            new int[]{'A', 'Z'});
    private static final UnicodeCategory Digit = new UnicodeCategory("Digit",
            new int[]{'0', '9'});
    private static final UnicodeCategory Alnum = new UnicodeCategory("Alnum",
            new int[]{'0', '9'}, new int[]{'a', 'z'}, new int[]{'A', 'Z'});
    private static final UnicodeCategory Cntrl = new UnicodeCategory("Cntrl",
            false, "C");
    private static final UnicodeCategory XDigit = new UnicodeCategory("XDigit",
            new int[]{'0', '9'}, new int[]{'a', 'f'});
    private static final UnicodeCategory Punct = new UnicodeCategory("Punct",
            new int[]{'.', '.'}, new int[]{'!', '!'}, new int[]{',', ','},
            new int[]{'?', '?'}, new int[]{'"', '"'}, new int[]{':', ':'},
            new int[]{'\'', '\''});
    private static final UnicodeCategory Graph = new UnicodeCategory("Graph",
            true, "Space", "Cntrl");
    private static final UnicodeCategory Print = new UnicodeCategory("Print",
            true, "Graph", "Blank");
    private static final UnicodeCategory All = new UnicodeCategory("All",
           new int[]{0, Character.MAX_VALUE});
    private static final UnicodeCategory javaJavaIdentifierStart = new UnicodeCategory(
            "javaJavaIdentifierStart",
            new int[]{'_', '_'}, new int[]{'a', 'z'}, new int[]{'A', 'Z'});
    private static final UnicodeCategory javaJavaIdentifierPart = new UnicodeCategory(
            "javaJavaIdentifierPart",
            new int[]{'_', '_'}, new int[]{'a', 'z'}, new int[]{'A', 'Z'},
            new int[]{'0', '9'});
    private static final UnicodeCategory javaLetterOrDigit = new UnicodeCategory(
            "javaLetterOrDigit",
            false, "Alpha", "Digit");
    private static final UnicodeCategory javaUpperCase = new UnicodeCategory(
            "javaUpperCase",
            false, "Upper");
    private static final UnicodeCategory javaLowerCase = new UnicodeCategory(
            "javaLowerCase",
            false, "Lower");
}
