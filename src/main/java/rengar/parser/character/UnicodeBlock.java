package rengar.parser.character;

import rengar.parser.ast.CharRangeExpr;
import java.util.HashMap;

public class UnicodeBlock {
    private static final HashMap<String, CharRangeExpr> map = new HashMap<>();

    public static CharRangeExpr get(String name) {
        return map.get(name);
    }

    private UnicodeBlock(String name, int[]... arr) {
        CharRangeExpr charRangeExpr = new CharRangeExpr();
        charRangeExpr.addRange(arr);
        map.put(name, charRangeExpr);
    }

    private static final UnicodeBlock BASIC_LATIN =
            new UnicodeBlock("BASIC_LATIN", new int[]{0, 0x7f});

    private static final UnicodeBlock LATIN_1_SUPPLEMENT =
            new UnicodeBlock("LATIN-1SUPPLEMENT", new int[]{0x80, 0xff});

    private static final UnicodeBlock LATIN_EXTENDED_A =
            new UnicodeBlock("LATINEXTENDED-A", new int[]{0x100, 0x17f});

    private static final UnicodeBlock LATIN_EXTENDED_B =
            new UnicodeBlock("LATINEXTENDED-B", new int[]{0x180, 0x24f});

    private static final UnicodeBlock IPA_EXTENSIONS =
            new UnicodeBlock("IPAEXTENSIONS", new int[]{0x250, 0x2AF});

    private static final UnicodeBlock SPACING_MODIFIER_LETTERS =
            new UnicodeBlock("SPACINGMODIFIERLETTERS", new int[]{0x2b0, 0x2ff});

    private static final UnicodeBlock COMBINING_DIACRITICAL_MARKS =
            new UnicodeBlock("COMBININGDIACRITICALMARKS", new int[]{0x300, 0x36f});

    public static final UnicodeBlock GREEK =
            new UnicodeBlock("GREEK", new int[]{0x370, 0x3ff});
}
