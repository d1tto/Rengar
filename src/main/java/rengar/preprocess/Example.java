package rengar.preprocess;

import rengar.parser.RegexParser;
import rengar.parser.ast.*;
import rengar.parser.exception.PatternSyntaxException;

public class Example {
    public static void main(String[] args) throws PatternSyntaxException {
        String patternStr = "/(?=^[a-z\\x2d\\x5f\\x2f]{95,}\\.php$).*?[a-z]{2,48}\\x2d[a-z]{2,48}\\x2d[a-z]{2,48}\\x2d[a-z]{2,48}\\x2d?\\.php$/\n";
        RegexParser parser = RegexParser.createParser(RegexParser.Language.Java, patternStr);
        RegexExpr regexExpr = parser.parse();
        System.out.println(regexExpr.genString());
        PreprocessPipeline.handle(regexExpr);
        System.out.println(regexExpr.genString());
    }
}
