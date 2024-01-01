package rengar.parser.character;

import rengar.parser.ast.CharRangeExpr;

import java.util.HashMap;

public class UnicodeScript {
    private static final HashMap<String, CharRangeExpr> map = new HashMap<>();

    public static CharRangeExpr get(String name) {
        return map.get(name);
    }

    private UnicodeScript(String name, int[]... arr) {
        CharRangeExpr charRangeExpr = new CharRangeExpr();
        charRangeExpr.addRange(arr);
        map.put(name, charRangeExpr);
    }

    private static final UnicodeScript LATIN = new UnicodeScript("LATIN",
            new int[]{0x41, 0x5a}, new int[]{0x61, 0x7a}, new int[]{0xaa, 0xaa},
            new int[]{0xba, 0xba}, new int[]{0xc0, 0xd6}, new int[]{0xd8, 0xf6},
            new int[]{0xf8, 0x2b8}, new int[]{0x2e0, 0x2e4});
    private static final UnicodeScript GREEK = new UnicodeScript("GREEK",
            new int[]{0x370, 0x373});
}
