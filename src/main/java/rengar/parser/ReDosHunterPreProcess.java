package rengar.parser;

import rengar.dynamic.jdk8.regex.Pattern;
import rengar.thirdparty.hunter.cn.ac.ios.TreeNode.TreeNode;

import static rengar.thirdparty.hunter.cn.ac.ios.TreeNode.Utils.createReDoSTree;

public class ReDosHunterPreProcess {
    public static String process(String patternStr) {
        try {
            //RegexBean divideRegexByFlagsBean = FlagsUtils.divideRegexByFlags(patternStr);
            //patternStr = divideRegexByFlagsBean.getRegex();
            Pattern.compile(patternStr);
            return patternStr;
        } catch (Exception e) {
            try {
                TreeNode ReDoSTree = createReDoSTree(patternStr);
                ReDoSTree.deleteAnnotation();
                ReDoSTree.deleteGroupName();
                ReDoSTree.addBackslashBeforeSomeCharacters();
                ReDoSTree.rewriteUnicodeNumberInBracketNode();
                ReDoSTree.reWriteBackspace();
                ReDoSTree.rewriteIllegalBarSymbol();
                ReDoSTree.rewriteSpecialBackslashCharacterForDifferentLanguage("java");
                return ReDoSTree.getData();
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}