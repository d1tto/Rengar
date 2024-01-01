package rengar.generator;

import rengar.generator.path.Path;
import rengar.parser.RegexParser;
import rengar.parser.ast.*;
import rengar.parser.charutil.CharUtil;
import rengar.parser.exception.PatternSyntaxException;
import java.util.*;

public class Example {
    public static void main(String[] args) throws PatternSyntaxException {
        String patternStr = "(\\w\\d)+";
        RegexParser parser = RegexParser.createParser(RegexParser.Language.Java, patternStr);
        RegexExpr regexExpr = parser.parse();
        Set<Path> pathList = StringGenerator.gen(regexExpr, 5,
                5, true);
        for (Path path : pathList) {
            System.out.println(CharUtil.toPrintableString(path.genValue()));
        }
    }
}
