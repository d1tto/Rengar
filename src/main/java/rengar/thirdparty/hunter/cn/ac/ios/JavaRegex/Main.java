package rengar.thirdparty.hunter.cn.ac.ios.JavaRegex;

/**
 * A small demo class that demonstrates how to use the
 * generated rengar.parser classes.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String regex = "abc";
        System.out.println(new JavaRegexBuilder.Tree(regex).toStringASCII());
    }
}
