package rengar.preprocess;

import rengar.config.GlobalConfig;
import rengar.parser.ast.RegexExpr;

/**
 * step 1. handle back-reference <br>
 * step 2. remove group
 * step 3. handle \b <br>
 * step 4. extract lookaround and the result will serve as a new branch
 * step 5. handle part lookaround <br>
 * */

public class PreprocessPipeline {
    public static void handle(RegexExpr expr) {
        CommonPreprocess commonPreprocess = new CommonPreprocess();
        commonPreprocess.handle(expr);
        if (!GlobalConfig.option.isDisablePreprocess()) {
            // step 1
            HandleBackReference backref = new HandleBackReference();
            backref.handle(expr);
        }
        // step 2
        HandleGroup group = new HandleGroup();
        group.handle(expr);
        if (!GlobalConfig.option.isDisablePreprocess()) {
            // step 3
            HandleWordBoundary wordBoundary = new HandleWordBoundary();
            wordBoundary.handle(expr);
            // step 4
            FetchLookaround fetchLookaround = new FetchLookaround();
            fetchLookaround.handle(expr);
            // step 5
            HandleLookaround handleLookaround = new HandleLookaround();
            handleLookaround.handle(expr);
        }
    }
}
