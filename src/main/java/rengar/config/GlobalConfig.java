package rengar.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GlobalConfig {
    public static final int MatchingStepUpperBound = 100000;
    public static final int MaxYStringLengthForNQ = 45;
    public static final int MaxYStringLengthForEOD = 40;
    public static final int MaxYStringLengthForEOA = 120;
    public static final int MaxYStringLengthForSLQ = 10000;
    public static final int MaxYStringLengthForPOA = 15000;
    public static Option option = new Option();
    public static ExecutorService executor = Executors.newCachedThreadPool();
}
