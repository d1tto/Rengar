package rengar.dynamic.profile;

import rengar.config.GlobalConfig;
import rengar.dynamic.exception.EarlyExitException;

import java.util.*;

public class Profile {
    private Map<Integer, Integer> indexToCount = new HashMap<>();
    private int total;

    private final int nodeNumber;
    private final long[] nodeToMatchNumber;

    // used by rengar.dynamic validator
    private boolean earlyExit = false;

    private final List<Suggestion> suggestions = new ArrayList<>();
    private final Set<Integer> posSet = new HashSet<>();

    public Profile(int nodeNumber) {
        this.nodeNumber = nodeNumber;
        this.nodeToMatchNumber = new long[this.nodeNumber];
    }

    public void setEarlyExit() {
        earlyExit = true;
    }

    public int getNodeNumber() {
        return nodeNumber;
    }

    public List<Suggestion> getSuggestions() {
        return suggestions;
    }

    public void suggest(int pos, int c) {
        if (posSet.contains(pos))
            return;
        suggestions.add(new Suggestion(pos, c));
        posSet.add(pos);
    }

    public void mark(int id) throws EarlyExitException {
        total += 1;
        nodeToMatchNumber[id] += 1;
        if (earlyExit && getMatchingStep() > GlobalConfig.MatchingStepUpperBound) {
            throw new EarlyExitException();
        }
    }

    public int getMatchingStep() {
        return total;
    }

    public int coverage() {
        int sum = 0;
        for (int i = 0; i < nodeToMatchNumber.length; i++) {
            if (nodeToMatchNumber[i] != 0)
                sum += 1;
        }
        return sum;
    }

    public void reset() {
        indexToCount.clear();
        total = 0;
        Arrays.fill(nodeToMatchNumber, 0);
        suggestions.clear();
        posSet.clear();
        earlyExit = false;
    }
}
