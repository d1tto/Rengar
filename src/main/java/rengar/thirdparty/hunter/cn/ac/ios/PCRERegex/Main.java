package rengar.thirdparty.hunter.cn.ac.ios.PCRERegex;

/**
 * A small demo class that demonstrates how to use the
 * generated rengar.parser classes.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String regex = "[abc]";
        System.out.println(new PCREBuilder.Tree(regex).toStringASCII());
    }
}
