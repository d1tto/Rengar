package rengar.dynamic.validator;

import rengar.checker.attack.*;
import rengar.dynamic.exception.EarlyExitException;
import rengar.dynamic.jdk8.regex.*;

public class Validator {
    private final AttackString attackStr;
    private final Pattern pattern;
    private String type;

    public Validator(String patternStr, AttackString attackStr, String type) {
        this.attackStr = attackStr;
        this.type = type;
        pattern = Pattern.compile(patternStr);
    }

    public boolean isVulnerable() {
        Matcher matcher = pattern.matcher(attackStr.genStr());
        matcher.setEarlyExit();
        try {
            if (type.contains("SLQ")) {
                matcher.find();
            } else {
                matcher.matches();
            }
        } catch (EarlyExitException ignored) {
            return true;
        } catch (StackOverflowError ignored) {
            return false;
        }
        return false;
    }
}
