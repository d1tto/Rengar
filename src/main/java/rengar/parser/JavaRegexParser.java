package rengar.parser;

import rengar.parser.ast.*;
import rengar.parser.character.*;
import rengar.parser.exception.*;

import java.util.Locale;

class JavaRegexParser extends RegexParser {
    public JavaRegexParser(String patternStr) {
        super(patternStr);
    }

    @Override
    public RegexExpr parse() throws PatternSyntaxException {
        pos = 0;
        RegexExpr expr = parseExpr();
        if (patternLength != pos)
            throw error("unexpected internal error");
        return expr;
    }
    RegexExpr parseExpr() throws PatternSyntaxException {
        RegexExpr expr;
        expr = new RegexExpr(parseAlternative());
        return expr;
    }

    // Alternative -> Sequence (Sequence '|')*
    BranchExpr parseAlternative() throws PatternSyntaxException {
        BranchExpr branchExpr = new BranchExpr();
        while (true) {
            SequenceExpr joinExpr = parseSequence();
            branchExpr.add(joinExpr);
            if (peek() != '|')
                break;
            read(); // eat |
        }
        return branchExpr;
    }

    // Sequence -> factor*
    SequenceExpr parseSequence() throws PatternSyntaxException {
        SequenceExpr seqExpr = new SequenceExpr();
        LOOP:
        while (true) {
            Expr tmpExpr;
            int c = peek();
            switch (c) {
                case '^':
                    read();
                    tmpExpr = new BeginExpr();
                    break;
                case '$':
                    read();
                    tmpExpr = new EndExpr();
                    break;
                case '.':
                    read();
                    CharRangeExpr charRangeExpr = new CharRangeExpr();
                    charRangeExpr.addOneChar('\n', '\r', '\u0085', '\u2028', '\u2029');
                    charRangeExpr.negate();
                    charRangeExpr.setStr(".");
                    tmpExpr = charRangeExpr;
                    break;
                case '[':
                    read(); // eat [
                    tmpExpr = parseCharClass();
                    break;
                case '(':
                    read(); // eat (
                    tmpExpr = parseGroup();
                    break;
                case '?':
                case '*':
                case '+':
                    read();
                    throw error(String.format("Dangling meta character '%c'", c));
                case '|': // a singer | can work in java regex engine.
                case ')': // parseGroup will call this function, so when we hit ), we need break
                    break LOOP;
                case ']':
                case '}':
                    // interpreting dangling ] and } as normal character
                    tmpExpr = parseSingleCharOrCharRange(false);
                    break;
                case 0:
                    if (isHitEnd())
                        break LOOP; // break while(true)
                default:
                    tmpExpr = parseSingleCharOrCharRange(false);
                    break;
            } // switch (c)
            // parseGroup may be return null
            if (tmpExpr == null)
                continue;
            tmpExpr = parseLoop(tmpExpr);
            seqExpr.add(tmpExpr);
        } // while (true)
        return seqExpr;
    }

    Expr parseLoop(Expr body) throws PatternSyntaxException {
        LoopExpr loopExpr;
        LoopExpr.LoopType type = LoopExpr.LoopType.Greedy;
        int min = 0, max = 0;
        // if we find that this is no loop, we will set `isLoop` to false
        boolean isLoop = true;

        // firstly, we determine the upper and lower bounds of the loop
        // by checking the quantifier
        int c = peek();
        switch (c) {
            case '*' -> {
                read();
                min = 0;
                max = -1;
            }
            case '+' -> {
                read();
                min = 1;
                max = -1;
            }
            case '?' -> {
                read();
                min = 0;
                max = 1;
            }
            case '{' -> {
                read(); // eat {
                if (peek() == '}' || !Character.isDigit(peek()))
                    throw error("illegal repetition");
                min = parseInteger();
                c = peek();
                if (c == ',') {
                    read(); // eat ,
                    c = peek();
                    if (c == '}')
                        max = -1;
                    else
                        max = parseInteger();
                } else
                    max = min;
                read(); // eat }
            }
            default -> isLoop = false;
        }
        // if there is no loop, we just return the argument.
        if (!isLoop)
            return body;
        c = peek();
        switch (c) {
            case '?' -> {
                read();
                type = LoopExpr.LoopType.Lazy;
            }
            case '+' -> {
                read();
                type = LoopExpr.LoopType.Possessive;
            }
        }
        loopExpr = new LoopExpr(min, max, type, body);
        return loopExpr;
    }

    // CharClass -> (OneCharOrCharRange)+
    Expr parseCharClass() throws PatternSyntaxException {
        CharRangeExpr sumExpr = null, curExpr;
        int beginPos = pos - 1;
        boolean isNot = false;
        int c = peek();
        if (c == '^') {
            isNot = true;
            read(); // eat ^
        }
        while (true) {
            c = peek();
            switch (c) {
                case ']':
                    // deal with the special case: AA[]BB
                    if (sumExpr == null)
                        break;
                    read(); // eat ]
                    if (isNot)
                        sumExpr.negate();
                    sumExpr.setStr(patternStr.substring(beginPos, pos));
                    return sumExpr;
                case '[':
                    read(); // eat [
                    curExpr = (CharRangeExpr)parseCharClass();
                    if (sumExpr == null)
                        sumExpr = curExpr;
                    else
                        sumExpr.union(curExpr);
                    continue;
                case 0:
                    if (isHitEnd())
                        throw error("unclosed character class");
                    break;
                default:
                    break;
            } // switch (c)
            CharExpr tmp = (CharExpr) parseSingleCharOrCharRange(true);
            // in character class, we only need OneCharExpr or CharRangeExpr
            if (tmp == null)
                throw error("invalid character class element type");
            curExpr = new CharRangeExpr();
            curExpr.union(tmp);

            if (sumExpr == null)
                sumExpr = curExpr;
            else
                sumExpr.union(curExpr);
        }
    }

    // Parse a single character or a character range in a character class
    // this function return-value's type includes BeginExpr, OneCharExpr, CharRangeExpr...
    Expr parseSingleCharOrCharRange(boolean inClass) throws PatternSyntaxException {
        boolean isNotRange = false;
        Expr resultExpr;
        int beginPos = pos;
        int c = peek();
        // <case 1>. process escape character
        if (c == '\\') {
            read(); // eat \
            c = peek(); // character needed to escape
            if (c == 'P' || c == 'p') {
                // https://www.tutorialspoint.com/character-class-p-javalowercase-java-regex
                boolean isNeg = c == 'P';
                read(); // eat P or p
                resultExpr = parseUnicodeFamily(isNeg);
            } else {
                // normal escape character
                resultExpr = parseEscape(inClass);
                if (!(resultExpr instanceof SingleCharExpr))
                    isNotRange = true;
            }
        } else {
            read(); // move to next char
            resultExpr = new SingleCharExpr(c);
        }
        if (isNotRange)
            return resultExpr;
        // <case 2>. process character class
        if (peek() == '-' && inClass) {
            Expr tmpExpr;
            // this is to process special case: [a-z-]
            // in this case, we treat it as a normal character
            int nextChar = buffer[pos + 1];
            if (nextChar == ']' || nextChar == '[')
                return resultExpr;
            read(); // eat -
            if (peek() == '\\') {
                read(); // eat \
                tmpExpr = parseEscape(inClass);
            } else {
                int endChar = peek();
                read();
                tmpExpr = new SingleCharExpr(endChar);
            }
            if (!(tmpExpr instanceof SingleCharExpr endExpr))
                throw error("invalid character range");
            SingleCharExpr beginExpr = (SingleCharExpr)resultExpr;
            if (beginExpr == null || beginExpr.getChar() > endExpr.getChar()) {
                throw error("invalid character range");
            }
            resultExpr = new CharRangeExpr();
            ((CharRangeExpr)resultExpr).addRange(beginExpr.getChar(), endExpr.getChar());
        }
        return resultExpr;
    }

    CharRangeExpr parseUnicodeFamily(boolean isNeg) throws PatternSyntaxException {
        CharRangeExpr expr = null;
        expect('{', "expect { after \\p or \\P");
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (isHitEnd())
                throw error("unclosed character family");
            int c = peek();
            if (c == '}')
                break;
            sb.append(Character.toString(c));
            read();
        }
        read(); // eat '}'
        if (sb.length() == 0)
            throw error("empty character family");
        String name = sb.toString();

        int i = name.indexOf('=');
        if (i != -1) {
            // property construct \p{name=value}
            String value = name.substring(i + 1);
            name = name.substring(0, i).toLowerCase(Locale.ENGLISH);
            switch (name) {
                case "sc", "script" -> expr = UnicodeScript.get(value);
                case "blk", "block" -> expr = UnicodeBlock.get(value);
                case "gc", "general_category" -> expr = UnicodeCategory.get(value);
                default -> {}
            }
            if (expr == null)
                throw error("unknown unicode property {name=<" + name + ">, "
                        + "value=<" + value + ">}");
        } else {
            if (name.startsWith("In")) {
                // \p{InBlockName}
                name = name.substring(2);
                expr = UnicodeBlock.get(name);
            } else if (name.startsWith("Is")) {
                // \p{IsGeneralCategory} and \p{IsScriptName}
                name = name.substring(2);
                name = name.toUpperCase(Locale.ROOT);
                expr = UnicodeScript.get(name);
                if (expr == null)
                    expr = UnicodeCategory.get(name);
            } else {
                expr = UnicodeCategory.get(name);
            }
            if (expr == null)
                throw error("Unknown character property name {In/Is" + name + "}");
        }
        if (isNeg) {
            expr.negate();
        }
        return expr;
    }

    // deal with \
    Expr parseEscape(boolean inClass) throws PatternSyntaxException {
        // when you enter this function, the '\' has already been eat;
        Expr expr = null;
        int escapeChar = peek();
        switch (escapeChar) {
            case '0' -> {
                read();
                StringBuilder sb = new StringBuilder();
                while (true) {
                    int c = peek();
                    if (!Character.isDigit(c))
                        break;
                    sb.append(Character.toString(c));
                    read();
                }
                if (sb.length() > 3 || sb.length() < 1)
                    throw error("illegal octal escape sequence");
                expr = new SingleCharExpr(Integer.parseInt(sb.toString(), 8));
            }
            case '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                if (inClass)
                    throw error("char class can't contain back-reference");
                int number = parseInteger();
                expr = new RegularBackRefExpr(number);
            }
            case 'A' -> {
                read();
                expr = new BeginExpr();
            }
            case 't' -> {
                read();
                expr = new SingleCharExpr('\t');
            }
            case 'n' -> {
                read();
                expr = new SingleCharExpr('\n');
            }
            case 'r' -> {
                read();
                expr = new SingleCharExpr('\r');
            }
            case 'f' -> {
                read();
                expr = new SingleCharExpr('\f');
            }
            case 'a' -> {
                read();
                expr = new SingleCharExpr('\007');
            }
            case 'e' -> {
                read();
                expr = new SingleCharExpr('\033');
            }
            case 'd' -> {
                read();
                CharRangeExpr rangeExpr = new CharRangeExpr();
                rangeExpr.addRange('0', '9');
                expr = rangeExpr;
            }
            case 'D' -> {
                read();
                CharRangeExpr rangeExpr = new CharRangeExpr();
                rangeExpr.addRange('0', '9');
                rangeExpr.negate();
                expr = rangeExpr;
            }
            case 's' -> {
                read();
                CharRangeExpr rangeExpr = new CharRangeExpr();
                rangeExpr.addOneChar('\t', '\n', 0xb, '\f', '\r', ' ');
                expr = rangeExpr;
            }
            case 'S' -> {
                read();
                CharRangeExpr rangeExpr = new CharRangeExpr();
                rangeExpr.addOneChar('\t', '\n', 0xb, '\f', '\r', ' ');
                rangeExpr.negate();
                expr = rangeExpr;
            }
            case 'w' -> {
                read();
                CharRangeExpr rangeExpr = new CharRangeExpr();
                rangeExpr.addRange(
                        new int[]{'a', 'z'}, new int[]{'A', 'Z'}, new int[]{'0', '9'}
                );
                rangeExpr.addOneChar('_');
                expr = rangeExpr;
            }
            case 'W' -> {
                read();
                CharRangeExpr rangeExpr = new CharRangeExpr();
                rangeExpr.addRange(
                        new int[]{'a', 'z'}, new int[]{'A', 'Z'}, new int[]{'0', '9'}
                );
                rangeExpr.addOneChar('_');
                rangeExpr.negate();
                expr = rangeExpr;
            }
            case 'x' -> {
                // process \xhh and \x{h...h}
                read(); // eat x
                int c = peek(); read();
                if (isHexDigit(c)) {
                    int low = peek(); read();
                    if (isHexDigit(low)) {
                        expr = new SingleCharExpr(
                                hexToNumber(c) * 16 + hexToNumber(low)
                        );
                    } else
                        throw error("illegal hexadecimal escape sequence");
                } else if (c == '{') {
                    int val = 0;
                    while (true) {
                        c = peek(); read();
                        if (!isHexDigit(c))
                            break;
                        val = (val << 4) + hexToNumber(c);
                    }
                    if (c != '}')
                        throw error("unclosed hexadecimal escape sequence");
                    expr = new SingleCharExpr(val);
                }
            }
            case 'u' -> {
                read();
                int val = 0;
                for (int i = 0; i < 4; i++) {
                    int c = peek(); read();
                    if (!isHexDigit(c))
                        throw error("illegal unicode escape sequence");
                    val = (val << 4) + hexToNumber(c);
                }
                expr = new SingleCharExpr(val);
            }
            case 'k' -> {
                read(); // eat k
                expect('<', "expect < after \\k");
                StringBuilder sb = new StringBuilder();
                int firstChar = peek();
                if (!Character.isLetter(firstChar)) {
                    throw error("group name is invalid");
                }
                sb.append(Character.toString(firstChar));
                read(); // eat first char
                while (true) {
                    int c = peek();
                    if (Character.isDigit(c) || Character.isLetter(c)) {
                        sb.append(Character.toString(c));
                        read();
                    } else
                        break;
                }
                expect('>', "named back ref is missing '>'");
                String groupname = sb.toString();
                expr = new NamedBackRefExpr(groupname);
            }
            case 'b' -> {
                read();
                expr = new WordBoundaryExpr();
            }
            case 'B' -> {
                read();
                expr = new NonWordBoundaryExpr();
            }
            default -> {
                read();
                expr = new SingleCharExpr(escapeChar);
            }
        }
        return expr;
    }

    Expr parseGroup() throws PatternSyntaxException {
        Expr resultExpr = null;
        int c = peek();
        if (c == '?') {
            read(); // eat ?
            c = peek();
            switch (c) {
                // (?:X) === X, as a non-capturing group
                case ':' -> {
                    read(); // eat :
                    RegexExpr body = parseExpr();
                    resultExpr = new AnonymousGroupExpr(body);
                }
                case '<' -> {
                    read(); // eat <
                    c = peek();
                    // (?<name>X)	X, as a named-capturing group
                    if (c != '=' && c != '!') {
                        StringBuilder sb = new StringBuilder();
                        int firstChar = peek();
                        if (!Character.isLetter(firstChar)) {
                            throw error("group name is invalid");
                        }
                        sb.append(Character.toString(firstChar));
                        read(); // eat first char
                        while (true) {
                            c = peek();
                            if (Character.isDigit(c) || Character.isLetter(c)) {
                                sb.append(Character.toString(c));
                                read();
                            } else
                                break;
                        }
                        expect('>', "named group is missing '>'");
                        String groupname = sb.toString();
                        int tmpGroupCount = groupCount;
                        groupCount += 1;
                        RegexExpr body = parseExpr();
                        resultExpr = new NamedGroupExpr(body, tmpGroupCount, groupname);
                    } else {
                        // (?<=X)	X, via zero-width positive lookbehind
                        // (?<!X)	X, via zero-width negative lookbehind
                        int tmp = c;
                        read();
                        RegexExpr body = parseExpr();
                        if (tmp == '=')
                            resultExpr = new LookbehindExpr(body, false);
                        else if (tmp == '!')
                            resultExpr = new LookbehindExpr(body, true);
                        else
                            throw error("unknown type");
                    }
                }
                case '=' -> {
                    // (?=X)	X, via zero-width positive lookahead
                    read(); // eat =
                    RegexExpr body = parseExpr();
                    resultExpr = new LookaheadExpr(body, false);
                }
                case '!' -> {
                    // (?!X)	X, via zero-width negative lookahead
                    read(); // eat !
                    RegexExpr body = parseExpr();
                    resultExpr = new LookaheadExpr(body, true);
                }
                case '>' -> {
                    // (?>X)	X, as an independent, non-capturing group
                    read(); // eat >
                    RegexExpr body = parseExpr();
                    resultExpr = new IndependentGroupExpr(body);
                }
                case '$', '@' -> throw error("Unknown group type");
                default -> {
                    // inlined match flags
                    inlineMatchFlagAdd();
                    c = peek();
                    if (c == ')')
                        break;
                    if (c != ':')
                        throw error("unknown inline modifier");
                    read(); // eat :
                    RegexExpr body = parseExpr();
                    resultExpr = new AnonymousGroupExpr(body);
                }
            }
        } else {
            int tmpGroupCount = groupCount;
            groupCount += 1;
            RegexExpr body = parseExpr();
            resultExpr = new RegularGroupExpr(body, tmpGroupCount);
        }
        expect(')', "Unclosed group");
        // when group is inline match flag group and doesn't have body, it will return null
        return resultExpr;
    }

    void inlineMatchFlagAdd() {
        int f = peek();
        while (true) {
            switch (f) {
                case 'i', 'm', 's', 'd', 'u', 'c', 'x', 'U' -> {}
                case '-' -> {
                    read(); // eat -
                    inlineMatchFlagSub();
                    return;
                }
                default -> {
                    return;
                }
            }
            read();
            f = peek();
        }
    }

    void inlineMatchFlagSub() {
        int f = peek();
        while (true) {
            switch (f) {
                case 'i', 'm', 's', 'd', 'u', 'c', 'x', 'U' -> {}
                default -> {
                    return;
                }
            }
            read();
            f = peek();
        }
    }

}