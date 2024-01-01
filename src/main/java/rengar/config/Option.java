package rengar.config;

public class Option {
    /**
     * DisablePreprocess
     *   - disable back-reference, lookaround process
     *   - disable ReDoSHunter rengar.preprocess
     * */
    private boolean disablePreprocess = false;
    /**
     * WeakPatternCheck
     *   - POA pattern.
     *   - SLQ pattern.
     * */
    private boolean weakPatternCheck = false;
    private boolean ingoreDisturbance = false;
    private boolean multipleVulnerabilityMode = false;
    private boolean quiet = false;
    private int staticTimeout = 10;
    private int totalTimeout = 15;
    private int threadNumber = 6;

    public boolean isDisablePreprocess() {
        return disablePreprocess;
    }

    public boolean isWeakPatternCheck() {
        return weakPatternCheck;
    }

    public boolean isIngoreDisturbance() {
        return ingoreDisturbance;
    }

    public boolean isMultipleVulnerabilityMode() {
        return multipleVulnerabilityMode;
    }

    public int getStaticTimeout() {
        return staticTimeout;
    }

    public int getTotalTimeout() {
        return totalTimeout;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public int getThreadNumber() {
        return threadNumber;
    }

    public void disablePreprocess() {
        this.disablePreprocess = true;
    }

    public void weakPatternCheck() {
        this.weakPatternCheck = true;
    }

    public void ignoreDisturbance() {
        this.ingoreDisturbance = true;
    }

    public void multipleVulnerabilityMode() {
        this.multipleVulnerabilityMode = true;
    }

    public void setStaticTimeout(int time) {
        staticTimeout = time;
    }

    public void setTotalTimeout(int time) {
        totalTimeout = time;
    }

    public void setThreadNumber(int number) {
        threadNumber = number;
    }

    public void quiet() {
        this.quiet = true;
    }
}
