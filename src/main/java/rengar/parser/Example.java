package rengar.parser;

import rengar.parser.ast.RegexExpr;
import rengar.parser.exception.PatternSyntaxException;

public class Example {
    public static void main(String[] args) throws PatternSyntaxException {
        String patternStr = "\\Q";
        RegexParser parser = RegexParser.createParser(RegexParser.Language.Java, patternStr);
        RegexExpr regexExpr = parser.parse();
        System.out.println(regexExpr);
    }
}
