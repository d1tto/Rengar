package rengar.thirdparty.hunter.cn.ac.ios.JavaScriptRegex;

/**
 * A small demo class that demonstrates how to use the
 * generated rengar.parser classes.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String regex = "[abc[:alnum:]abc]+";
        System.out.println(new JavaScriptRegexBuilder.Tree(regex).toStringASCII());
    }
}
