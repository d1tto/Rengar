package rengar.checker.pattern;

import rengar.base.RegexAnalyzer;
import rengar.checker.attack.AttackString;
import rengar.checker.util.RegexUtil;
import rengar.config.GlobalConfig;
import rengar.generator.StringGenerator;
import rengar.generator.path.Path;
import rengar.parser.ast.*;
import rengar.parser.range.CharRangeSet;
import java.util.*;

public class DisturbFreePattern {
    private final List<SequenceExpr> disturbExprs = new LinkedList<>();
    private final List<Path> disturbs = new LinkedList<>();;
    private final ReDoSPattern pattern;

    private List<Path> phi1Paths;
    private List<Path> phi2Paths;
    private List<Path> phi3Paths;
    private List<Path> postfixPaths;

    public String getType() {
        return pattern.getType();
    }

    public DisturbFreePattern(ReDoSPattern pattern, List<SequenceExpr> disturbExprs) {
        this.pattern = pattern;
        this.disturbExprs.addAll(disturbExprs);
        init();
    }

    public DisturbFreePattern(ReDoSPattern pattern) {
        this.pattern = pattern;
        init();
    }

    private void init() {
        for (SequenceExpr disturb : disturbExprs) {
            List<Path> paths = RegexUtil.getPathsOf(disturb);
            disturbs.addAll(paths);
        }
        phi2Paths = pattern.getPhi2Paths();
        //if (pattern instanceof SLQPattern) {
        //    phi1Paths = new LinkedList<>(phi2Paths);
        //} else {
        if (pattern instanceof SLQPattern && !pattern.getPrefixExpr().isEmpty()) {
            // slq pattern that has prefix-expr means that prefix-expr is nullable
            phi1Paths = new LinkedList<>();
        } else {
            phi1Paths = new LinkedList<>(RegexUtil.getPathsOf(pattern.getPrefixExpr()));
        }
        if (!GlobalConfig.option.isIngoreDisturbance()
                && pattern instanceof SLQPattern
                && !disturbs.isEmpty()) {
            CharRangeSet tmp = new CharRangeSet();
            for (Path path : disturbs) {
                if (path.getRangeSets().size() > 0)
                    tmp = tmp.union(path.getRangeSets().get(0));
            }
            Path path = new Path();
            path.add(tmp.negate());
            path.getDisturbType().setType(DisturbType.Type.Case2);
            phi1Paths.add(path);
        }
        if (phi1Paths.isEmpty()) {
            phi1Paths.add(new Path());
        }
        //}
        phi3Paths = RegexUtil.getPathsOf(pattern.getPostfixExpr());
        postfixPaths = getPostfixPaths();
    }

    private CharRangeSet getFirstCharRangeSet(Expr expr) {
        CharRangeSet res;
        switch (expr) {
            case RegexExpr regexExpr -> res = getFirstCharRangeSet(regexExpr.getExpr());
            case BranchExpr branchExpr -> res = getFirstCharRangeSet(branchExpr.get(0));
            case SequenceExpr sequenceExpr -> res = getFirstCharRangeSet(sequenceExpr.get(0));
            case GroupExpr groupExpr -> res = getFirstCharRangeSet(groupExpr.getBody());
            case LoopExpr loopExpr -> res = getFirstCharRangeSet(loopExpr.getBody());
            case SingleCharExpr sce -> {
                res = new CharRangeSet();
                res.addOneChar(sce.getChar());
            }
            case CharRangeExpr rangeExpr -> res = rangeExpr.getRangeSet();
            default -> res = new CharRangeSet();
        }
        return res;
    }

    private CharRangeSet getLastCharRangeSet(Expr expr) {
        CharRangeSet res;
        switch (expr) {
            case RegexExpr regexExpr -> res = getFirstCharRangeSet(regexExpr.getExpr());
            case BranchExpr branchExpr -> res = getFirstCharRangeSet(branchExpr.get(0));
            case SequenceExpr seqExpr -> res = getFirstCharRangeSet(seqExpr.get(seqExpr.getSize() - 1));
            case GroupExpr groupExpr -> res = getFirstCharRangeSet(groupExpr.getBody());
            case LoopExpr loopExpr -> res = getFirstCharRangeSet(loopExpr.getBody());
            case SingleCharExpr sce -> {
                res = new CharRangeSet();
                res.addOneChar(sce.getChar());
            }
            case CharRangeExpr rangeExpr -> res = rangeExpr.getRangeSet();
            default -> res = new CharRangeSet();
        }
        return res;
    }


    private List<Path> getPostfixPaths() {
        List<Path> paths = new LinkedList<>();
        CharRangeSet first = getFirstCharRangeSet(pattern.attackableExpr);
        CharRangeSet last = getLastCharRangeSet(pattern.attackableExpr);
        CharRangeSet lastSetOfY = RegexAnalyzer.getLastSet(pattern.getAttackableExpr());
        CharRangeSet firstSetOfZ = RegexAnalyzer.getFirstSet(pattern.getPostfixExpr());
        CharRangeSet tmp = lastSetOfY.union(firstSetOfZ);
        //if (GlobalConfig.option.isIngoreDisturbance()) {
        //    tmp = tmp.negate();
        //    int postfix = tmp.isEmpty() ? 'A' : tmp.getRanges().get(0).end;
        //    Path path = new Path();
        //    path.add(postfix);
        //    paths.add(path);
        //} else {
            if (pattern instanceof POAPattern) {
                tmp = tmp.union(RegexAnalyzer.getFirstSet(pattern.getAttackableExpr()));
            }
            tmp = tmp.negate();
            int[] postfix = new int[] {
                    'A', '\t', '\n', '\u0000', '\u1111', '\u2222', '\u8888', '\u9999',
                    '\u3333', '\u4444', '\u5555', '\u6666',
            };
            if (!tmp.isEmpty()) {
                Arrays.fill(postfix, tmp.getRanges().get(0).end);
                postfix[1] = '\n'; postfix[3] = '\t';
                postfix[5] = '\u1111'; postfix[7] = '\u2222';
                postfix[9] = '\u5555'; postfix[11] = '\u9999';
            }
            Path newPath = new Path();
            for (int elem : postfix) {
                newPath.add(elem);
            }
            paths.add(newPath);
            if (tmp.isEmpty()) {
                paths.add(new Path());
            }
            if (tmp.isEmpty()) {
                Path infix = StringGenerator.quickGen(pattern.getAttackableExpr());
                Path negInfix = new Path();
                for (CharRangeSet rangeSet : infix.getRangeSets()) {
                    CharRangeSet negSet = rangeSet.negate();
                    if (negSet.isEmpty()) {
                        negInfix.add('\n');
                    } else {
                        negInfix.add(negSet);
                    }
                }
                paths.add(negInfix);
            }
        //}
        Path path = new Path();
        for (int i = 0; i < 3; i++) {
            path.add(last.negate());
            path.add(first.negate());
        }
        paths.add(path);
        return paths;
    }

    private List<Path> freeDisturb(List<Path> disturbPaths, List<Path> paths) {
        List<Path> result = new ArrayList<>(paths);
        for (Path path1 : disturbPaths) {
            for (Path path2 : paths) {
                if (path1.getLength() <= path2.getLength()) {
                    List<Path> newPaths = RegexUtil.getMutexPath(path1, path2);
                    result.addAll(newPaths);
                }
            }
        }
        return result;
    }

    private List<Path> freeDisturb(List<Path> paths) {
        return freeDisturb(this.disturbs, paths);
    }


    private List<Path> handleYPath(List<Path> yPaths) {
        List<Path> result = new ArrayList<>(yPaths);
        for (Path path1 : disturbs) {
            for (Path path2 : yPaths) {
                if (path1.getLength() <= path2.getLength()) {
                    List<Path> newPaths = RegexUtil.getMutexPath(path1, path2);
                    result.addAll(newPaths);
                }
            }
        }
        return result;
    }

    private static class AttackPath {
        Path path = new Path();
        int prefixLen;
        int attackLen;
        int postfixLen;
        public AttackPath(Path prefix, Path attack, Path postfix) {
            path.add(prefix);
            path.add(attack);
            path.add(postfix);
            prefixLen = prefix.getLength();
            attackLen = attack.getLength();
            postfixLen = postfix.getLength();
        }

        public AttackPath(Path path, int prefixLen, int attackLen, int postfixLen) {
            this.path = path.copy();
            this.prefixLen = prefixLen;
            this.attackLen = attackLen;
            this.postfixLen = postfixLen;
        }

        public DisturbType getDisturbType() {
            return path.getDisturbType();
        }

        public void setDisturbType(DisturbType type) {
            this.path.setDisturbType(type);
        }

        public int getAttackLen() {
            return attackLen;
        }

        public int getPostfixLen() {
            return postfixLen;
        }

        public int getPrefixLen() {
            return prefixLen;
        }

        public Path getPath() {
            return path;
        }

        public Path getPrefix() {
            return this.path.slice(0, prefixLen);
        }

        public Path getAttack() {
            return this.path.slice(prefixLen, prefixLen + attackLen);
        }

        public Path getPostfix() {
            return this.path.slice(prefixLen + attackLen);
        }

        private static void judgeDisturbType(AttackPath originAttachPath, AttackPath newAttackPath) {
            Path originPrefix = originAttachPath.getPrefix();
            Path originInfix = originAttachPath.getAttack();
            Path originPostfix = originAttachPath.getPostfix();
            Path newPrefix = newAttackPath.getPrefix();
            Path newInfix = newAttackPath.getAttack();
            Path newPostfix = newAttackPath.getPostfix();
            boolean prefixNotEqual = !originPrefix.getRangeSets().equals(newPrefix.getRangeSets());
            boolean infixNotEqual = !originInfix.getRangeSets().equals(newInfix.getRangeSets());
            boolean postfixNotEqual = !originPostfix.getRangeSets().equals(newPostfix.getRangeSets());
            DisturbType type = new DisturbType();
            type.setType(originAttachPath.getDisturbType());
            if (prefixNotEqual && infixNotEqual && postfixNotEqual) {
                type.setType(DisturbType.Type.Case3);
            } else if (prefixNotEqual && infixNotEqual) {
                type.setType(DisturbType.Type.Case2);
            } else if (prefixNotEqual) {
                type.setType(DisturbType.Type.Case1);
            }
            newAttackPath.setDisturbType(type);
        }

        public static List<AttackPath> freeDisturbForCase123(List<Path> disturbPaths,
                                                   List<AttackPath> attackPaths) {
            if (disturbPaths.isEmpty())
                return attackPaths;
            Set<AttackPath> result = new HashSet<>();
            loop:
            for (Path disturb : disturbPaths) {
                for (AttackPath attackPath : attackPaths) {
                    Path path = attackPath.getPath();
                    if (disturb.getLength() <= path.getLength()) {
                        List<Path> newPaths = RegexUtil.getMutexPath(disturb, path);
                        if (Thread.currentThread().isInterrupted())
                            break loop;
                        if (newPaths.isEmpty()) {
                            AttackPath newAttackPath = new AttackPath(
                                    path,
                                    attackPath.getPrefixLen(),
                                    attackPath.getAttackLen(),
                                    attackPath.getPostfixLen()
                            );
                            result.add(newAttackPath);
                        } else {
                            for (Path newPath : newPaths) {
                                AttackPath newAttackPath = new AttackPath(
                                        newPath,
                                        attackPath.getPrefixLen(),
                                        attackPath.getAttackLen(),
                                        attackPath.getPostfixLen()
                                );
                                judgeDisturbType(attackPath, newAttackPath);
                                result.add(newAttackPath);
                            }
                        }
                    }
                }
            }
            return result.stream().toList();
        }

        private static boolean hasIntersection(String patternType, Path shortPath, Path longPath) {
            assert shortPath.getLength() <= longPath.getLength();
            for (int i = 0; i < longPath.getLength(); i++) {
                boolean has = true;
                if (i + shortPath.getLength() > longPath.getLength())
                    break;
                for (int j = 0; j < shortPath.getLength(); j++) {
                    if (Thread.currentThread().isInterrupted())
                        return false;
                    CharRangeSet rangeSet1 = longPath.getRangeSets().get(i + j);
                    CharRangeSet rangeSet2 = shortPath.getRangeSets().get(j);
                    if (rangeSet1.and(rangeSet2).isEmpty()) {
                        has = false;
                        break;
                    }
                }
                if (has) return true;
            }
            return false;
        }

        public static List<AttackPath> freeDisturbForCase5(ReDoSPattern pattern,
                                                           List<Path> disturbPaths,
                                                           List<AttackPath> attackPaths) {
            if (disturbPaths.isEmpty())
                return attackPaths;
            Set<AttackPath> result = new HashSet<>();
            loop:
            for (Path disturb : disturbPaths) {
                for (AttackPath attackPath : attackPaths) {
                    Path infix = attackPath.getAttack();
                    List<Path> newPaths = RegexUtil.getMutexPath(disturb, infix);
                    if (Thread.currentThread().isInterrupted())
                        break loop;
                    if (newPaths.isEmpty()) {
                        AttackPath newAttackPath = new AttackPath(
                                attackPath.getPrefix(),
                                infix,
                                attackPath.getPostfix()
                        );
                        newAttackPath.setDisturbType(attackPath.getDisturbType());
                        result.add(newAttackPath);
                    } else {
                        for (Path newPath : newPaths) {
                            if (Thread.currentThread().isInterrupted())
                                break loop;
                            AttackPath newAttackPath = new AttackPath(
                                    attackPath.getPrefix(),
                                    newPath,
                                    attackPath.getPostfix()
                            );
                            newAttackPath.setDisturbType(attackPath.getDisturbType());
                            if (hasIntersection(pattern.getType(), disturb, infix)) {
                                SequenceExpr phi3 = pattern.getPostfixExpr();
                                if (!(phi3.get(phi3.getSize() - 1) instanceof EndExpr)) {
                                    DisturbType disturbType = attackPath.getDisturbType().copy();
                                    disturbType.setType(DisturbType.Type.Case5);
                                    newAttackPath.setDisturbType(disturbType);
                                }
                            }
                            result.add(newAttackPath);
                        }
                    }
                }
            }
            return result.stream().toList();
        }

        @Override
        public String toString() {
            return path.toString();
        }
    }

    /**
     * this method try to free disturbance including case1, case2, case3, case 5
     * */
    public List<AttackPath> tryFreeDisturb() {
        List<AttackPath> attackPaths = new LinkedList<>();
        int phi1Times = 0;
        for (Path phi1Path : phi1Paths) {
            phi1Times += 1;
            if (phi1Times >= 5)
                break;
            for (Path phi2Path : phi2Paths) {
                for (Path postfixPath : postfixPaths) {
                    AttackPath attackPath = new AttackPath(phi1Path, phi2Path, postfixPath);
                    attackPath.getDisturbType().setType(phi1Path.getDisturbType());
                    attackPath.getDisturbType().setType(phi2Path.getDisturbType());
                    attackPath.getDisturbType().setType(postfixPath.getDisturbType());
                    attackPaths.add(attackPath);
                }
            }
        }
        if (GlobalConfig.option.isIngoreDisturbance()) {
            return attackPaths;
        } else {
            List<AttackPath> stage1 = AttackPath.freeDisturbForCase123(this.disturbs, attackPaths);
            List<AttackPath> stage2 = AttackPath.freeDisturbForCase5(pattern, this.phi3Paths, stage1);
            return stage2;
        }
    }

    public List<AttackString> generate() {
        Set<AttackString> attackStrings = new HashSet<>();
        List<AttackPath> attackPaths = tryFreeDisturb();
        for (AttackPath attackPath : attackPaths) {
            if (Thread.currentThread().isInterrupted())
                return attackStrings.stream().toList();
            AttackString as = new AttackString();
            as.setDisturbType(attackPath.getDisturbType());
            Path prefix = attackPath.getPrefix();
            Path attack = attackPath.getAttack();
            Path postfix = attackPath.getPostfix();
            as.setPrefix(prefix.genValue());
            int n = pattern.getMaxLength();
            n = n / attack.getLength();
            as.setAttack(attack.genValue(), n);
            as.setPostfix(postfix.genValue());
            attackStrings.add(as);
            if (attackStrings.size() > 120)
                return attackStrings.stream().toList();
        }
        return attackStrings.stream().toList();
    }

    private List<Path> tryFreeDisturbForPhi1() {
        List<Path> freePaths = freeDisturb(phi1Paths);
        if (freePaths.isEmpty()) {
            return phi1Paths;
        }
        return freePaths;
    }

    private List<Path> tryFreeDisturbForPhi2() {
        int phiMinLen;
        if (phi1Paths.isEmpty())
            phiMinLen = 0;
        else {
            phiMinLen = phi1Paths
                    .stream()
                    .min(Comparator.comparingInt(Path::getLength))
                    .get()
                    .getLength();
        }
        List<Path> filterDisturbs = disturbs
                .stream()
                .filter(path -> path.getLength() > phiMinLen)
                .toList();
        List<Path> slicedDisturbs = new LinkedList<>();
        for (Path disturb : filterDisturbs) {
            slicedDisturbs.add(disturb.slice(phiMinLen));
        }
        List<Path> freePaths1 = freeDisturb(slicedDisturbs, phi2Paths);
        if (freePaths1.isEmpty()) {
            freePaths1 = phi2Paths;
        }
        List<Path> freePaths2 = freeDisturb(phi3Paths, freePaths1);
        if (freePaths2.isEmpty()) {
            return freePaths1;
        }
        return freePaths2;
    }

    public List<AttackString> generate1() {
        Set<AttackString> attackStrings = new HashSet<>();
        if (phi2Paths.isEmpty())
            return new ArrayList<>();
        List<Path> phi1Paths = tryFreeDisturbForPhi1();
        Path phi1Path = phi1Paths.isEmpty() ? new Path() : phi1Paths.get(0);
        List<Path> phi2Paths = tryFreeDisturbForPhi2();
        for (Path phi2Path : phi2Paths) {
            for (Path postfixPath : postfixPaths) {
                AttackString as = new AttackString();
                as.setPrefix(phi1Path.genValue());

                as.setAttack(phi2Path.genValue());
                int n = pattern.getMaxLength();
                n = n / phi2Path.getLength();
                as.setAttack(phi2Path.genValue(), n);

                as.setPostfix(postfixPath.genValue());

                attackStrings.add(as);
                if (attackStrings.size() > 120)
                    return attackStrings.stream().toList();
            }
        }
        return attackStrings.stream().toList();
    }

    public List<AttackString> old_generate_for_debug() {
        List<AttackString> attackStrings = new ArrayList<>();
        List<Path> yPaths = handleYPath(pattern.getPhi2Paths());
        for (Path pathY : yPaths) {
            AttackString attackString = new AttackString();
            attackString.setPrefix(StringGenerator.quickGen(pattern.getPrefixExpr()).genValue());

            int n = pattern.getMaxLength();
            n = n / pathY.getLength();
            attackString.setAttack(pathY.genValue(), n);

            CharRangeSet lastSetOfY = RegexAnalyzer.getLastSet(pattern.getAttackableExpr());
            CharRangeSet alphabetOfZ = RegexAnalyzer.getFirstSet(pattern.getPostfixExpr());

            CharRangeSet tmp = lastSetOfY.union(alphabetOfZ);
            if (pattern instanceof POAPattern) {
                tmp = tmp.union(RegexAnalyzer.getFirstSet(pattern.getAttackableExpr()));
            }
            tmp = tmp.negate();
            int[] postfix = new int[]{
                    'A', '\t', '\n', '\u0000',
                    '\u1111', '\u2222', '\u8888', '\u9999',
                    '\u3333', '\u4444', '\u5555', '\u6666',
            };
            if (!tmp.isEmpty()) {
                Arrays.fill(postfix, tmp.getRanges().get(0).end);
                postfix[1] = '\n';
                postfix[3] = '\t';
                postfix[5] = '\u1111';
                postfix[7] = '\u2222';
                postfix[9] = '\u5555';
                postfix[11] = '\u9999';
            }
            attackString.setPostfix(postfix);
            attackStrings.add(attackString);
            if (attackStrings.size() > 100)
                break;
        }
        return attackStrings;
    }

    @Override
    public String toString() {
        return pattern.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof DisturbFreePattern freePattern) {
            return this.pattern.equals(freePattern.pattern)
                    && this.disturbExprs.equals(freePattern.disturbExprs);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.pattern.hashCode() ^ this.disturbs.hashCode();
    }

    public boolean isDuplicate(DisturbFreePattern freePattern) {
        return this.pattern.isDuplicate(freePattern.pattern);
    }
}
