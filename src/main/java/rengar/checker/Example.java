package rengar.checker;

import rengar.config.GlobalConfig;
import rengar.parser.RegexParser;

public class Example {
    public static void main(String[] args) throws InterruptedException {
        String s = "(a+)+";
        StaticPipeline.Result result = StaticPipeline.run(s, RegexParser.Language.Java, false);
        GlobalConfig.executor.shutdownNow();
    }
}
