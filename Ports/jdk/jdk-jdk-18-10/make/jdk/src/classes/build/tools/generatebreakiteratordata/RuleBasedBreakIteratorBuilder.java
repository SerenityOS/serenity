/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package build.tools.generatebreakiteratordata;

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import java.util.zip.CRC32;
import sun.text.CompactByteArray;

/**
 * This class has the job of constructing a RuleBasedBreakIterator from a
 * textual description. A Builder is constructed by GenerateBreakIteratorData,
 * which uses it to construct the iterator itself and then throws it away.
 * <p>The construction logic is separated out into its own class for two primary
 * reasons:
 * <ul>
 * <li>The construction logic is quite sophisticated and large. Separating
 * it out into its own class means the code must only be loaded into memory
 * while a RuleBasedBreakIterator is being constructed, and can be purged after
 * that.
 * <li>There is a fair amount of state that must be maintained throughout the
 * construction process that is not needed by the iterator after construction.
 * Separating this state out into another class prevents all of the functions
 * that construct the iterator from having to have really long parameter lists,
 * (hopefully) contributing to readability and maintainability.
 * </ul>
 * <p>
 * It'd be really nice if this could be an independent class rather than an
 * inner class, because that would shorten the source file considerably, but
 * making Builder an inner class of RuleBasedBreakIterator allows it direct
 * access to RuleBasedBreakIterator's private members, which saves us from
 * having to provide some kind of "back door" to the Builder class that could
 * then also be used by other classes.
 */
class RuleBasedBreakIteratorBuilder {

    /**
     * A token used as a character-category value to identify ignore characters
     */
    protected static final byte IGNORE = -1;

    /**
     * Tables that indexes from character values to character category numbers
     */
    private CompactByteArray charCategoryTable = null;
    private SupplementaryCharacterData supplementaryCharCategoryTable = null;

    /**
     * The table of state transitions used for forward iteration
     */
    private short[] stateTable = null;

    /**
     * The table of state transitions used to sync up the iterator with the
     * text in backwards and random-access iteration
     */
    private short[] backwardsStateTable = null;

    /**
     * A list of flags indicating which states in the state table are accepting
     * ("end") states
     */
    private boolean[] endStates = null;

    /**
     * A list of flags indicating which states in the state table are
     * lookahead states (states which turn lookahead on and off)
     */
    private boolean[] lookaheadStates = null;

    /**
     * A table for additional data. May be used by a subclass of
     * RuleBasedBreakIterator.
     */
    private byte[] additionalData = null;

    /**
     * The number of character categories (and, thus, the number of columns in
     * the state tables)
     */
    private int numCategories;

    /**
     * A temporary holding place used for calculating the character categories.
     * This object contains CharSet objects.
     */
    protected Vector<CharSet> categories = null;

    /**
     * A table used to map parts of regexp text to lists of character
     * categories, rather than having to figure them out from scratch each time
     */
    protected Hashtable<String, Object> expressions = null;

    /**
     * A temporary holding place for the list of ignore characters
     */
    protected CharSet ignoreChars = null;

    /**
     * A temporary holding place where the forward state table is built
     */
    protected Vector<short[]> tempStateTable = null;

    /**
     * A list of all the states that have to be filled in with transitions to
     * the next state that is created.  Used when building the state table from
     * the regular expressions.
     */
    protected Vector<Integer> decisionPointList = null;

    /**
     * A stack for holding decision point lists.  This is used to handle nested
     * parentheses and braces in regexps.
     */
    protected Stack<Vector<Integer>> decisionPointStack = null;

    /**
     * A list of states that loop back on themselves.  Used to handle .*?
     */
    protected Vector<Integer> loopingStates = null;

    /**
     * Looping states actually have to be backfilled later in the process
     * than everything else.  This is where a the list of states to backfill
     * is accumulated.  This is also used to handle .*?
     */
    protected Vector<Integer> statesToBackfill = null;

    /**
     * A list mapping pairs of state numbers for states that are to be combined
     * to the state number of the state representing their combination.  Used
     * in the process of making the state table deterministic to prevent
     * infinite recursion.
     */
    protected Vector<int[]> mergeList = null;

    /**
     * A flag that is used to indicate when the list of looping states can
     * be reset.
     */
    protected boolean clearLoopingStates = false;

    /**
     * A bit mask used to indicate a bit in the table's flags column that marks
     * a state as an accepting state.
     */
    protected static final int END_STATE_FLAG = 0x8000;

    /**
     * A bit mask used to indicate a bit in the table's flags column that marks
     * a state as one the builder shouldn't loop to any looping states
     */
    protected static final int DONT_LOOP_FLAG = 0x4000;

    /**
     * A bit mask used to indicate a bit in the table's flags column that marks
     * a state as a lookahead state.
     */
    protected static final int LOOKAHEAD_STATE_FLAG = 0x2000;

    /**
     * A bit mask representing the union of the mask values listed above.
     * Used for clearing or masking off the flag bits.
     */
    protected static final int ALL_FLAGS = END_STATE_FLAG
                                         | LOOKAHEAD_STATE_FLAG
                                         | DONT_LOOP_FLAG;

    /**
     * This is the main function for setting up the BreakIterator's tables. It
     * just vectors different parts of the job off to other functions.
     */
    public RuleBasedBreakIteratorBuilder(String description) {
        Vector<String> tempRuleList = buildRuleList(description);
        buildCharCategories(tempRuleList);
        buildStateTable(tempRuleList);
        buildBackwardsStateTable(tempRuleList);
    }

    /**
     * Thus function has three main purposes:
     * <ul><li>Perform general syntax checking on the description, so the rest
     * of the build code can assume that it's parsing a legal description.
     * <li>Split the description into separate rules
     * <li>Perform variable-name substitutions (so that no one else sees
     * variable names)
     * </ul>
     */
    private Vector<String> buildRuleList(String description) {
        // invariants:
        // - parentheses must be balanced: ()[]{}<>
        // - nothing can be nested inside <>
        // - nothing can be nested inside [] except more []s
        // - pairs of ()[]{}<> must not be empty
        // - ; can only occur at the outer level
        // - | can only appear inside ()
        // - only one = or / can occur in a single rule
        // - = and / cannot both occur in the same rule
        // - <> can only occur on the left side of a = expression
        //   (because we'll perform substitutions to eliminate them other places)
        // - the left-hand side of a = expression can only be a single character
        //   (possibly with \) or text inside <>
        // - the right-hand side of a = expression must be enclosed in [] or ()
        // - * may not occur at the beginning of a rule, nor may it follow
        //   =, /, (, (, |, }, ;, or *
        // - ? may only follow *
        // - the rule list must contain at least one / rule
        // - no rule may be empty
        // - all printing characters in the ASCII range except letters and digits
        //   are reserved and must be preceded by \
        // - ! may only occur at the beginning of a rule

        // set up a vector to contain the broken-up description (each entry in the
        // vector is a separate rule) and a stack for keeping track of opening
        // punctuation
        Vector<String> tempRuleList = new Vector<>();
        Stack<Character> parenStack = new Stack<>();

        int p = 0;
        int ruleStart = 0;
        int c = '\u0000';
        int lastC = '\u0000';
        int lastOpen = '\u0000';
        boolean haveEquals = false;
        boolean havePipe = false;
        boolean sawVarName = false;
        final String charsThatCantPrecedeAsterisk = "=/{(|}*;\u0000";

        // if the description doesn't end with a semicolon, tack a semicolon onto the end
        if (description.length() != 0 &&
            description.codePointAt(description.length() - 1) != ';') {
            description = description + ";";
        }

        // for each character, do...
        while (p < description.length()) {
            c = description.codePointAt(p);

            switch (c) {
                // if the character is a backslash, skip the character that follows it
                // (it'll get treated as a literal character)
                case '\\':
                    ++p;
                    break;

                // if the character is opening punctuation, verify that no nesting
                // rules are broken, and push the character onto the stack
                case '{':
                case '<':
                case '[':
                case '(':
                    if (lastOpen == '<') {
                        error("Can't nest brackets inside <>", p, description);
                    }
                    if (lastOpen == '[' && c != '[') {
                        error("Can't nest anything in [] but []", p, description);
                    }

                    // if we see < anywhere except on the left-hand side of =,
                    // we must be seeing a variable name that was never defined
                    if (c == '<' && (haveEquals || havePipe)) {
                        error("Unknown variable name", p, description);
                    }

                    lastOpen = c;
                    parenStack.push(Character.valueOf((char)c));
                    if (c == '<') {
                        sawVarName = true;
                    }
                    break;

                // if the character is closing punctuation, verify that it matches the
                // last opening punctuation we saw, and that the brackets contain
                // something, then pop the stack
                case '}':
                case '>':
                case ']':
                case ')':
                    char expectedClose = '\u0000';
                    switch (lastOpen) {
                        case '{':
                            expectedClose = '}';
                            break;
                        case '[':
                            expectedClose = ']';
                            break;
                        case '(':
                            expectedClose = ')';
                            break;
                        case '<':
                            expectedClose = '>';
                            break;
                    }
                    if (c != expectedClose) {
                        error("Unbalanced parentheses", p, description);
                    }
                    if (lastC == lastOpen) {
                        error("Parens don't contain anything", p, description);
                    }
                    parenStack.pop();
                    if (!parenStack.empty()) {
                        lastOpen = parenStack.peek().charValue();
                    }
                    else {
                        lastOpen = '\u0000';
                    }

                    break;

                // if the character is an asterisk, make sure it occurs in a place
                // where an asterisk can legally go
                case '*':
                    if (charsThatCantPrecedeAsterisk.indexOf(lastC) != -1) {
                        error("Misplaced asterisk", p, description);
                    }
                    break;

                // if the character is a question mark, make sure it follows an asterisk
                case '?':
                    if (lastC != '*') {
                        error("Misplaced ?", p, description);
                    }
                    break;

                // if the character is an equals sign, make sure we haven't seen another
                // equals sign or a slash yet
                case '=':
                    if (haveEquals || havePipe) {
                        error("More than one = or / in rule", p, description);
                    }
                    haveEquals = true;
                    break;

                // if the character is a slash, make sure we haven't seen another slash
                // or an equals sign yet
                case '/':
                    if (haveEquals || havePipe) {
                        error("More than one = or / in rule", p, description);
                    }
                    if (sawVarName) {
                        error("Unknown variable name", p, description);
                    }
                    havePipe = true;
                    break;

                // if the character is an exclamation point, make sure it occurs only
                // at the beginning of a rule
                case '!':
                    if (lastC != ';' && lastC != '\u0000') {
                        error("! can only occur at the beginning of a rule", p, description);
                    }
                    break;

                // we don't have to do anything special on a period
                case '.':
                    break;

                // if the character is a syntax character that can only occur
                // inside [], make sure that it does in fact only occur inside [].
                case '^':
                case '-':
                case ':':
                    if (lastOpen != '[' && lastOpen != '<') {
                        error("Illegal character", p, description);
                    }
                    break;

                // if the character is a semicolon, do the following...
                case ';':
                    // make sure the rule contains something and that there are no
                    // unbalanced parentheses or brackets
                    if (lastC == ';' || lastC == '\u0000') {
                        error("Empty rule", p, description);
                    }
                    if (!parenStack.empty()) {
                        error("Unbalanced parenheses", p, description);
                    }

                    if (parenStack.empty()) {
                        // if the rule contained an = sign, call processSubstitution()
                        // to replace the substitution name with the substitution text
                        // wherever it appears in the description
                        if (haveEquals) {
                            description = processSubstitution(description.substring(ruleStart,
                                            p), description, p + 1);
                        }
                        else {
                            // otherwise, check to make sure the rule doesn't reference
                            // any undefined substitutions
                            if (sawVarName) {
                                error("Unknown variable name", p, description);
                            }

                            // then add it to tempRuleList
                            tempRuleList.addElement(description.substring(ruleStart, p));
                        }

                        // and reset everything to process the next rule
                        ruleStart = p + 1;
                        haveEquals = havePipe = sawVarName = false;
                    }
                    break;

                // if the character is a vertical bar, check to make sure that it
                // occurs inside a () expression and that the character that precedes
                // it isn't also a vertical bar
                case '|':
                    if (lastC == '|') {
                        error("Empty alternative", p, description);
                    }
                    if (parenStack.empty() || lastOpen != '(') {
                        error("Misplaced |", p, description);
                    }
                    break;

                // if the character is anything else (escaped characters are
                // skipped and don't make it here), it's an error
                default:
                    if (c >= ' ' && c < '\u007f' && !Character.isLetter((char)c)
                        && !Character.isDigit((char)c)) {
                        error("Illegal character", p, description);
                    }
                    if (c >= Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                        ++p;
                    }
                    break;
            }
            lastC = c;
            ++p;
        }
        if (tempRuleList.size() == 0) {
            error("No valid rules in description", p, description);
        }
        return tempRuleList;
    }

    /**
     * This function performs variable-name substitutions.  First it does syntax
     * checking on the variable-name definition.  If it's syntactically valid, it
     * then goes through the remainder of the description and does a simple
     * find-and-replace of the variable name with its text.  (The variable text
     * must be enclosed in either [] or () for this to work.)
     */
    protected String processSubstitution(String substitutionRule, String description,
                    int startPos) {
        // isolate out the text on either side of the equals sign
        String replace;
        String replaceWith;
        int equalPos = substitutionRule.indexOf('=');
        replace = substitutionRule.substring(0, equalPos);
        replaceWith = substitutionRule.substring(equalPos + 1);

        // check to see whether the substitution name is something we've declared
        // to be "special".  For RuleBasedBreakIterator itself, this is "<ignore>".
        // This function takes care of any extra processing that has to be done
        // with "special" substitution names.
        handleSpecialSubstitution(replace, replaceWith, startPos, description);

        // perform various other syntax checks on the rule
        if (replaceWith.length() == 0) {
            error("Nothing on right-hand side of =", startPos, description);
        }
        if (replace.length() == 0) {
            error("Nothing on left-hand side of =", startPos, description);
        }
        if (replace.length() == 2 && replace.charAt(0) != '\\') {
            error("Illegal left-hand side for =", startPos, description);
        }
        if (replace.length() >= 3 && replace.charAt(0) != '<' &&
            replace.codePointBefore(equalPos) != '>') {
            error("Illegal left-hand side for =", startPos, description);
        }
        if (!(replaceWith.charAt(0) == '[' &&
              replaceWith.charAt(replaceWith.length() - 1) == ']') &&
            !(replaceWith.charAt(0) == '(' &&
              replaceWith.charAt(replaceWith.length() - 1) == ')')) {
            error("Illegal right-hand side for =", startPos, description);
        }

        // now go through the rest of the description (which hasn't been broken up
        // into separate rules yet) and replace every occurrence of the
        // substitution name with the substitution body
        StringBuffer result = new StringBuffer();
        result.append(description.substring(0, startPos));
        int lastPos = startPos;
        int pos = description.indexOf(replace, startPos);
        while (pos != -1) {
            result.append(description.substring(lastPos, pos));
            result.append(replaceWith);
            lastPos = pos + replace.length();
            pos = description.indexOf(replace, lastPos);
        }
        result.append(description.substring(lastPos));
        return result.toString();
    }

    /**
     * This function defines a protocol for handling substitution names that
     * are "special," i.e., that have some property beyond just being
     * substitutions.  At the RuleBasedBreakIterator level, we have one
     * special substitution name, "<ignore>".  Subclasses can override this
     * function to add more.  Any special processing that has to go on beyond
     * that which is done by the normal substitution-processing code is done
     * here.
     */
    protected void handleSpecialSubstitution(String replace, String replaceWith,
                int startPos, String description) {
        // if we get a definition for a substitution called "ignore", it defines
        // the ignore characters for the iterator.  Check to make sure the expression
        // is a [] expression, and if it is, parse it and store the characters off
        // to the side.
        if (replace.equals("<ignore>")) {
            if (replaceWith.charAt(0) == '(') {
                error("Ignore group can't be enclosed in (", startPos, description);
            }
            ignoreChars = CharSet.parseString(replaceWith);
        }
    }

    /**
     * This function builds the character category table.  On entry,
     * tempRuleList is a vector of break rules that has had variable names substituted.
     * On exit, the charCategoryTable data member has been initialized to hold the
     * character category table, and tempRuleList's rules have been munged to contain
     * character category numbers everywhere a literal character or a [] expression
     * originally occurred.
     */
    @SuppressWarnings("fallthrough")
    protected void buildCharCategories(Vector<String> tempRuleList) {
        int bracketLevel = 0;
        int p = 0;
        int lineNum = 0;

        // build hash table of every literal character or [] expression in the rule list
        // and use CharSet.parseString() to derive a CharSet object representing the
        // characters each refers to
        expressions = new Hashtable<>();
        while (lineNum < tempRuleList.size()) {
            String line = tempRuleList.elementAt(lineNum);
            p = 0;
            while (p < line.length()) {
                int c = line.codePointAt(p);
                switch (c) {
                    // skip over all syntax characters except [
                    case '{': case '}': case '(': case ')': case '*': case '.':
                    case '/': case '|': case ';': case '?': case '!':
                        break;

                    // for [, find the matching ] (taking nested [] pairs into account)
                    // and add the whole expression to the expression list
                    case '[':
                        int q = p + 1;
                        ++bracketLevel;
                        while (q < line.length() && bracketLevel != 0) {
                            c = line.codePointAt(q);
                            switch (c) {
                            case '\\':
                                q++;
                                break;
                            case '[':
                                ++bracketLevel;
                                break;
                            case ']':
                                --bracketLevel;
                                break;
                            }
                            q = q + Character.charCount(c);
                        }
                        if (expressions.get(line.substring(p, q)) == null) {
                            expressions.put(line.substring(p, q), CharSet.parseString(line.substring(p, q)));
                        }
                        p = q - 1;
                        break;

                    // for \ sequences, just move to the next character and treat
                    // it as a single character
                    case '\\':
                        ++p;
                        c = line.codePointAt(p);
                        // DON'T break; fall through into "default" clause

                    // for an isolated single character, add it to the expression list
                    default:
                        expressions.put(line.substring(p, p + 1), CharSet.parseString(line.substring(p, p + 1)));
                        break;
                }
                p += Character.charCount(line.codePointAt(p));
            }
            ++lineNum;
        }
        // dump CharSet's internal expression cache
        CharSet.releaseExpressionCache();

        // create the temporary category table (which is a vector of CharSet objects)
        categories = new Vector<>();
        if (ignoreChars != null) {
            categories.addElement(ignoreChars);
        }
        else {
            categories.addElement(new CharSet());
        }
        ignoreChars = null;

        // this is a hook to allow subclasses to add categories on their own
        mungeExpressionList(expressions);

        // Derive the character categories.  Go through the existing character categories
        // looking for overlap.  Any time there's overlap, we create a new character
        // category for the characters that overlapped and remove them from their original
        // category.  At the end, any characters that are left in the expression haven't
        // been mentioned in any category, so another new category is created for them.
        // For example, if the first expression is [abc], then a, b, and c will be placed
        // into a single character category.  If the next expression is [bcd], we will first
        // remove b and c from their existing category (leaving a behind), create a new
        // category for b and c, and then create another new category for d (which hadn't
        // been mentioned in the previous expression).
        // At no time should a character ever occur in more than one character category.

        // for each expression in the expressions list, do...
        for (Enumeration<Object> iter = expressions.elements(); iter.hasMoreElements(); ) {
            // initialize the working char set to the chars in the current expression
            CharSet e = (CharSet)iter.nextElement();

            // for each category in the category list, do...
            for (int j = categories.size() - 1; !e.empty() && j > 0; j--) {

                // if there's overlap between the current working set of chars
                // and the current category...
                CharSet that = categories.elementAt(j);
                if (!that.intersection(e).empty()) {

                    // add a new category for the characters that were in the
                    // current category but not in the working char set
                    CharSet temp = that.difference(e);
                    if (!temp.empty()) {
                        categories.addElement(temp);
                    }

                    // remove those characters from the working char set and replace
                    // the current category with the characters that it did
                    // have in common with the current working char set
                    temp = e.intersection(that);
                    e = e.difference(that);
                    if (!temp.equals(that)) {
                        categories.setElementAt(temp, j);
                    }
                }
            }

            // if there are still characters left in the working char set,
            // add a new category containing them
            if (!e.empty()) {
                categories.addElement(e);
            }
        }

        // we have the ignore characters stored in position 0.  Make an extra pass through
        // the character category list and remove anything from the ignore list that shows
        // up in some other category
        CharSet allChars = new CharSet();
        for (int i = 1; i < categories.size(); i++) {
            allChars = allChars.union(categories.elementAt(i));
        }
        CharSet ignoreChars = categories.elementAt(0);
        ignoreChars = ignoreChars.difference(allChars);
        categories.setElementAt(ignoreChars, 0);

        // now that we've derived the character categories, go back through the expression
        // list and replace each CharSet object with a String that represents the
        // character categories that expression refers to.  The String is encoded: each
        // character is a character category number (plus 0x100 to avoid confusing them
        // with syntax characters in the rule grammar)

        for (Enumeration<String> iter = expressions.keys(); iter.hasMoreElements(); ) {
            String key = iter.nextElement();
            CharSet cs = (CharSet)expressions.get(key);
            StringBuffer cats = new StringBuffer();

            // for each category...
            for (int j = 0; j < categories.size(); j++) {

                // if the current expression contains characters in that category...
                CharSet temp = cs.intersection(categories.elementAt(j));
                if (!temp.empty()) {

                    // then add the encoded category number to the String for this
                    // expression
                    cats.append((char)(0x100 + j));
                    if (temp.equals(cs)) {
                        break;
                    }
                }
            }

            // once we've finished building the encoded String for this expression,
            // replace the CharSet object with it
            expressions.put(key, cats.toString());
        }

        // and finally, we turn the temporary category table into a permanent category
        // table, which is a CompactByteArray. (we skip category 0, which by definition
        // refers to all characters not mentioned specifically in the rules)
        charCategoryTable = new CompactByteArray((byte)0);
        supplementaryCharCategoryTable = new SupplementaryCharacterData((byte)0);

        // for each category...
        for (int i = 0; i < categories.size(); i++) {
            CharSet chars = categories.elementAt(i);

            // go through the character ranges in the category one by one...
            Enumeration<int[]> enum_ = chars.getChars();
            while (enum_.hasMoreElements()) {
                int[] range = enum_.nextElement();

                // and set the corresponding elements in the CompactArray accordingly
                if (i != 0) {
                    if (range[0] < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                        if (range[1] < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                            charCategoryTable.setElementAt((char)range[0], (char)range[1], (byte)i);
                        } else {
                            charCategoryTable.setElementAt((char)range[0], (char)0xFFFF, (byte)i);
                            supplementaryCharCategoryTable.appendElement(Character.MIN_SUPPLEMENTARY_CODE_POINT, range[1], (byte)i);
                        }
                    } else {
                        supplementaryCharCategoryTable.appendElement(range[0], range[1], (byte)i);
                    }
                }

                // (category 0 is special-- it's the hiding place for the ignore
                // characters, whose real category number in the CompactArray is
                // -1 [this is because category 0 contains all characters not
                // specifically mentioned anywhere in the rules] )
                else {
                    if (range[0] < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                        if (range[1] < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                            charCategoryTable.setElementAt((char)range[0], (char)range[1], IGNORE);
                        } else {
                            charCategoryTable.setElementAt((char)range[0], (char)0xFFFF, IGNORE);
                            supplementaryCharCategoryTable.appendElement(Character.MIN_SUPPLEMENTARY_CODE_POINT, range[1], IGNORE);
                        }
                    } else {
                        supplementaryCharCategoryTable.appendElement(range[0], range[1], IGNORE);
                    }
                }
            }
        }

        // once we've populated the CompactArray, compact it
        charCategoryTable.compact();

        // And, complete the category table for supplementary characters
        supplementaryCharCategoryTable.complete();

        // initialize numCategories
        numCategories = categories.size();
    }

    protected void mungeExpressionList(Hashtable<String, Object> expressions) {
        // empty in the parent class.  This function provides a hook for subclasses
        // to mess with the character category table.
    }

    /**
     * This is the function that builds the forward state table.  Most of the real
     * work is done in parseRule(), which is called once for each rule in the
     * description.
     */
    private void buildStateTable(Vector<String> tempRuleList) {
        // initialize our temporary state table, and fill it with two states:
        // state 0 is a dummy state that allows state 1 to be the starting state
        // and 0 to represent "stop".  State 1 is added here to seed things
        // before we start parsing
        tempStateTable = new Vector<>();
        tempStateTable.addElement(new short[numCategories + 1]);
        tempStateTable.addElement(new short[numCategories + 1]);

        // call parseRule() for every rule in the rule list (except those which
        // start with !, which are actually backwards-iteration rules)
        for (int i = 0; i < tempRuleList.size(); i++) {
            String rule = tempRuleList.elementAt(i);
            if (rule.charAt(0) != '!') {
                parseRule(rule, true);
            }
        }

        // finally, use finishBuildingStateTable() to minimize the number of
        // states in the table and perform some other cleanup work
        finishBuildingStateTable(true);
    }

    /**
     * This is where most of the work really happens.  This routine parses a single
     * rule in the rule description, adding and modifying states in the state
     * table according to the new expression.  The state table is kept deterministic
     * throughout the whole operation, although some ugly postprocessing is needed
     * to handle the *? token.
     */
    private void parseRule(String rule, boolean forward) {
        // algorithm notes:
        //   - The basic idea here is to read successive character-category groups
        //   from the input string.  For each group, you create a state and point
        //   the appropriate entries in the previous state to it.  This produces a
        //   straight line from the start state to the end state.  The {}, *, and (|)
        //   idioms produce branches in this straight line.  These branches (states
        //   that can transition to more than one other state) are called "decision
        //   points."  A list of decision points is kept.  This contains a list of
        //   all states that can transition to the next state to be created.  For a
        //   straight line progression, the only thing in the decision-point list is
        //   the current state.  But if there's a branch, the decision-point list
        //   will contain all of the beginning points of the branch when the next
        //   state to be created represents the end point of the branch.  A stack is
        //   used to save decision point lists in the presence of nested parentheses
        //   and the like.  For example, when a { is encountered, the current decision
        //   point list is saved on the stack and restored when the corresponding }
        //   is encountered.  This way, after the } is read, the decision point list
        //   will contain both the state right before the } _and_ the state before
        //   the whole {} expression.  Both of these states can transition to the next
        //   state after the {} expression.
        //   - one complication arises when we have to stamp a transition value into
        //   an array cell that already contains one.  The updateStateTable() and
        //   mergeStates() functions handle this case.  Their basic approach is to
        //   create a new state that combines the two states that conflict and point
        //   at it when necessary.  This happens recursively, so if the merged states
        //   also conflict, they're resolved in the same way, and so on.  There are
        //   a number of tests aimed at preventing infinite recursion.
        //   - another complication arises with repeating characters.  It's somewhat
        //   ambiguous whether the user wants a greedy or non-greedy match in these cases.
        //   (e.g., whether "[a-z]*abc" means the SHORTEST sequence of letters ending in
        //   "abc" or the LONGEST sequence of letters ending in "abc".  We've adopted
        //   the *? to mean "shortest" and * by itself to mean "longest".  (You get the
        //   same result with both if there's no overlap between the repeating character
        //   group and the group immediately following it.)  Handling the *? token is
        //   rather complicated and involves keeping track of whether a state needs to
        //   be merged (as described above) or merely overwritten when you update one of
        //   its cells, and copying the contents of a state that loops with a *? token
        //   into some of the states that follow it after the rest of the table-building
        //   process is complete ("backfilling").
        // implementation notes:
        //   - This function assumes syntax checking has been performed on the input string
        //   prior to its being passed in here.  It assumes that parentheses are
        //   balanced, all literal characters are enclosed in [] and turned into category
        //   numbers, that there are no illegal characters or character sequences, and so
        //   on.  Violation of these invariants will lead to undefined behavior.
        //   - It'd probably be better to use linked lists rather than Vector and Stack
        //   to maintain the decision point list and stack.  I went for simplicity in
        //   this initial implementation.  If performance is critical enough, we can go
        //   back and fix this later.
        //   -There are a number of important limitations on the *? token.  It does not work
        //   right when followed by a repeating character sequence (e.g., ".*?(abc)*")
        //   (although it does work right when followed by a single repeating character).
        //   It will not always work right when nested in parentheses or braces (although
        //   sometimes it will).  It also will not work right if the group of repeating
        //   characters and the group of characters that follows overlap partially
        //   (e.g., "[a-g]*?[e-j]").  None of these capabilites was deemed necessary for
        //   describing breaking rules we know about, so we left them out for
        //   expeditiousness.
        //   - Rules such as "[a-z]*?abc;" will be treated the same as "[a-z]*?aa*bc;"--
        //   that is, if the string ends in "aaaabc", the break will go before the first
        //   "a" rather than the last one.  Both of these are limitations in the design
        //   of RuleBasedBreakIterator and not limitations of the rule parser.

        int p = 0;
        int currentState = 1;   // don't use state number 0; 0 means "stop"
        int lastState = currentState;
        String pendingChars = "";

        decisionPointStack = new Stack<>();
        decisionPointList = new Vector<>();
        loopingStates = new Vector<>();
        statesToBackfill = new Vector<>();

        short[] state;
        boolean sawEarlyBreak = false;

        // if we're adding rules to the backward state table, mark the initial state
        // as a looping state
        if (!forward) {
            loopingStates.addElement(Integer.valueOf(1));
        }

        // put the current state on the decision point list before we start
        decisionPointList.addElement(Integer.valueOf(currentState)); // we want currentState to
                                                                 // be 1 here...
        currentState = tempStateTable.size() - 1;   // but after that, we want it to be
                                                    // 1 less than the state number of the next state
        while (p < rule.length()) {
            int c = rule.codePointAt(p);
            clearLoopingStates = false;

            // this section handles literal characters, escaped characters (which are
            // effectively literal characters too), the . token, and [] expressions
            if (c == '['
                || c == '\\'
                || Character.isLetter(c)
                || Character.isDigit(c)
                || c < ' '
                || c == '.'
                || c >= '\u007f') {

                // if we're not on a period, isolate the expression and look up
                // the corresponding category list
                if (c != '.') {
                    int q = p;

                    // if we're on a backslash, the expression is the character
                    // after the backslash
                    if (c == '\\') {
                        q = p + 2;
                        ++p;
                    }

                    // if we're on an opening bracket, scan to the closing bracket
                    // to isolate the expression
                    else if (c == '[') {
                        int bracketLevel = 1;

                        q += Character.charCount(rule.codePointAt(q));
                        while (bracketLevel > 0) {
                            c = rule.codePointAt(q);
                            if (c == '[') {
                                ++bracketLevel;
                            }
                            else if (c == ']') {
                                --bracketLevel;
                            }
                            else if (c == '\\') {
                                c = rule.codePointAt(++q);
                            }
                            q += Character.charCount(c);
                        }
                    }

                    // otherwise, the expression is just the character itself
                    else {
                        q = p + Character.charCount(c);
                    }

                    // look up the category list for the expression and store it
                    // in pendingChars
                    pendingChars = (String)expressions.get(rule.substring(p, q));

                    // advance the current position past the expression
                    p = q - Character.charCount(rule.codePointBefore(q));
                }

                // if the character we're on is a period, we end up down here
                else {
                    int rowNum = decisionPointList.lastElement().intValue();
                    state = tempStateTable.elementAt(rowNum);

                    // if the period is followed by an asterisk, then just set the current
                    // state to loop back on itself
                    if (p + 1 < rule.length() && rule.charAt(p + 1) == '*' && state[0] != 0) {
                        decisionPointList.addElement(Integer.valueOf(state[0]));
                        pendingChars = "";
                        ++p;
                    }

                    // otherwise, fabricate a category list ("pendingChars") with
                    // every category in it
                    else {
                        StringBuffer temp = new StringBuffer();
                        for (int i = 0; i < numCategories; i++)
                            temp.append((char)(i + 0x100));
                        pendingChars = temp.toString();
                    }
                }

                // we'll end up in here for all expressions except for .*, which is
                // special-cased above
                if (pendingChars.length() != 0) {

                    // if the expression is followed by an asterisk, then push a copy
                    // of the current desicion point list onto the stack (this is
                    // the same thing we do on an opening brace)
                    if (p + 1 < rule.length() && rule.charAt(p + 1) == '*') {
                        @SuppressWarnings("unchecked")
                        Vector<Integer> clone = (Vector<Integer>)decisionPointList.clone();
                        decisionPointStack.push(clone);
                    }

                    // create a new state, add it to the list of states to backfill
                    // if we have looping states to worry about, set its "don't make
                    // me an accepting state" flag if we've seen a slash, and add
                    // it to the end of the state table
                    int newState = tempStateTable.size();
                    if (loopingStates.size() != 0) {
                        statesToBackfill.addElement(Integer.valueOf(newState));
                    }
                    state = new short[numCategories + 1];
                    if (sawEarlyBreak) {
                        state[numCategories] = DONT_LOOP_FLAG;
                    }
                    tempStateTable.addElement(state);

                    // update everybody in the decision point list to point to
                    // the new state (this also performs all the reconciliation
                    // needed to make the table deterministic), then clear the
                    // decision point list
                    updateStateTable(decisionPointList, pendingChars, (short)newState);
                    decisionPointList.removeAllElements();

                    // add all states created since the last literal character we've
                    // seen to the decision point list
                    lastState = currentState;
                    do {
                        ++currentState;
                        decisionPointList.addElement(Integer.valueOf(currentState));
                    } while (currentState + 1 < tempStateTable.size());
                }
            }

            // a { marks the beginning of an optional run of characters.  Push a
            // copy of the current decision point list onto the stack.  This saves
            // it, preventing it from being affected by whatever's inside the parentheses.
            // This decision point list is restored when a } is encountered.
            else if (c == '{') {
                @SuppressWarnings("unchecked")
                Vector<Integer> clone = (Vector<Integer>)decisionPointList.clone();
                decisionPointStack.push(clone);
            }

            // a } marks the end of an optional run of characters.  Pop the last decision
            // point list off the stack and merge it with the current decision point list.
            // a * denotes a repeating character or group (* after () is handled separately
            // below).  In addition to restoring the decision point list, modify the
            // current state to point to itself on the appropriate character categories.
            else if (c == '}' || c == '*') {
                // when there's a *, update the current state to loop back on itself
                // on the character categories that caused us to enter this state
                if (c == '*') {
                    for (int i = lastState + 1; i < tempStateTable.size(); i++) {
                        Vector<Integer> temp = new Vector<>();
                        temp.addElement(Integer.valueOf(i));
                        updateStateTable(temp, pendingChars, (short)(lastState + 1));
                    }
                }

                // pop the top element off the decision point stack and merge
                // it with the current decision point list (this causes the divergent
                // paths through the state table to come together again on the next
                // new state)
                Vector<Integer> temp = decisionPointStack.pop();
                for (int i = 0; i < decisionPointList.size(); i++)
                    temp.addElement(decisionPointList.elementAt(i));
                decisionPointList = temp;
            }

            // a ? after a * modifies the behavior of * in cases where there is overlap
            // between the set of characters that repeat and the characters which follow.
            // Without the ?, all states following the repeating state, up to a state which
            // is reached by a character that doesn't overlap, will loop back into the
            // repeating state.  With the ?, the mark states following the *? DON'T loop
            // back into the repeating state.  Thus, "[a-z]*xyz" will match the longest
            // sequence of letters that ends in "xyz," while "[a-z]*? will match the
            // _shortest_ sequence of letters that ends in "xyz".
            // We use extra bookkeeping to achieve this effect, since everything else works
            // according to the "longest possible match" principle.  The basic principle
            // is that transitions out of a looping state are written in over the looping
            // value instead of being reconciled, and that we copy the contents of the
            // looping state into empty cells of all non-terminal states that follow the
            // looping state.
            else if (c == '?') {
                setLoopingStates(decisionPointList, decisionPointList);
            }

            // a ( marks the beginning of a sequence of characters.  Parentheses can either
            // contain several alternative character sequences (i.e., "(ab|cd|ef)"), or
            // they can contain a sequence of characters that can repeat (i.e., "(abc)*").  Thus,
            // A () group can have multiple entry and exit points.  To keep track of this,
            // we reserve TWO spots on the decision-point stack.  The top of the stack is
            // the list of exit points, which becomes the current decision point list when
            // the ) is reached.  The next entry down is the decision point list at the
            // beginning of the (), which becomes the current decision point list at every
            // entry point.
            // In addition to keeping track of the exit points and the active decision
            // points before the ( (i.e., the places from which the () can be entered),
            // we need to keep track of the entry points in case the expression loops
            // (i.e., is followed by *).  We do that by creating a dummy state in the
            // state table and adding it to the decision point list (BEFORE it's duplicated
            // on the stack).  Nobody points to this state, so it'll get optimized out
            // at the end.  It exists only to hold the entry points in case the ()
            // expression loops.
            else if (c == '(') {

                // add a new state to the state table to hold the entry points into
                // the () expression
                tempStateTable.addElement(new short[numCategories + 1]);

                // we have to adjust lastState and currentState to account for the
                // new dummy state
                lastState = currentState;
                ++currentState;

                // add the current state to the decision point list (add it at the
                // BEGINNING so we can find it later)
                decisionPointList.insertElementAt(Integer.valueOf(currentState), 0);

                // finally, push a copy of the current decision point list onto the
                // stack (this keeps track of the active decision point list before
                // the () expression), followed by an empty decision point list
                // (this will hold the exit points)
                @SuppressWarnings("unchecked")
                Vector<Integer> clone = (Vector<Integer>)decisionPointList.clone();
                decisionPointStack.push(clone);
                decisionPointStack.push(new Vector<Integer>());
            }

            // a | separates alternative character sequences in a () expression.  When
            // a | is encountered, we add the current decision point list to the exit-point
            // list, and restore the decision point list to its state prior to the (.
            else if (c == '|') {

                // pick out the top two decision point lists on the stack
                Vector<Integer> oneDown = decisionPointStack.pop();
                Vector<Integer> twoDown = decisionPointStack.peek();
                decisionPointStack.push(oneDown);

                // append the current decision point list to the list below it
                // on the stack (the list of exit points), and restore the
                // current decision point list to its state before the () expression
                for (int i = 0; i < decisionPointList.size(); i++)
                    oneDown.addElement(decisionPointList.elementAt(i));
                @SuppressWarnings("unchecked")
                Vector<Integer> clone = (Vector<Integer>)twoDown.clone();
                decisionPointList = clone;
            }

            // a ) marks the end of a sequence of characters.  We do one of two things
            // depending on whether the sequence repeats (i.e., whether the ) is followed
            // by *):  If the sequence doesn't repeat, then the exit-point list is merged
            // with the current decision point list and the decision point list from before
            // the () is thrown away.  If the sequence does repeat, then we fish out the
            // state we were in before the ( and copy all of its forward transitions
            // (i.e., every transition added by the () expression) into every state in the
            // exit-point list and the current decision point list.  The current decision
            // point list is then merged with both the exit-point list AND the saved version
            // of the decision point list from before the ().  Then we throw out the *.
            else if (c == ')') {

                // pull the exit point list off the stack, merge it with the current
                // decision point list, and make the merged version the current
                // decision point list
                Vector<Integer> exitPoints = decisionPointStack.pop();
                for (int i = 0; i < decisionPointList.size(); i++)
                    exitPoints.addElement(decisionPointList.elementAt(i));
                decisionPointList = exitPoints;

                // if the ) isn't followed by a *, then all we have to do is throw
                // away the other list on the decision point stack, and we're done
                if (p + 1 >= rule.length() || rule.charAt(p + 1) != '*') {
                    decisionPointStack.pop();
                }

                // but if the sequence repeats, we have a lot more work to do...
                else {

                    // now exitPoints and decisionPointList have to point to equivalent
                    // vectors, but not the SAME vector
                    @SuppressWarnings("unchecked")
                    Vector<Integer> clone = (Vector<Integer>)decisionPointList.clone();
                    exitPoints = clone;

                    // pop the original decision point list off the stack
                    Vector<Integer> temp = decisionPointStack.pop();

                    // we squirreled away the row number of our entry point list
                    // at the beginning of the original decision point list.  Fish
                    // that state number out and retrieve the entry point list
                    int tempStateNum = temp.firstElement().intValue();
                    short[] tempState = tempStateTable.elementAt(tempStateNum);

                    // merge the original decision point list with the current
                    // decision point list
                    for (int i = 0; i < decisionPointList.size(); i++)
                        temp.addElement(decisionPointList.elementAt(i));
                    decisionPointList = temp;

                    // finally, copy every forward reference from the entry point
                    // list into every state in the new decision point list
                    for (int i = 0; i < tempState.length; i++) {
                        if (tempState[i] > tempStateNum) {
                            updateStateTable(exitPoints,
                                             Character.valueOf((char)(i + 0x100)).toString(),
                                             tempState[i]);
                        }
                    }

                    // update lastState and currentState, and throw away the *
                    lastState = currentState;
                    currentState = tempStateTable.size() - 1;
                    ++p;
                }
            }

            // a / marks the position where the break is to go if the character sequence
            // matches this rule.  We update the flag word of every state on the decision
            // point list to mark them as ending states, and take note of the fact that
            // we've seen the slash
            else if (c == '/') {
                sawEarlyBreak = true;
                for (int i = 0; i < decisionPointList.size(); i++) {
                    state = tempStateTable.elementAt(decisionPointList.
                                    elementAt(i).intValue());
                    state[numCategories] |= LOOKAHEAD_STATE_FLAG;
                }
            }

            // if we get here without executing any of the above clauses, we have a
            // syntax error.  However, for now we just ignore the offending character
            // and move on

            // clearLoopingStates is a signal back from updateStateTable() that we've
            // transitioned to a state that won't loop back to the current looping
            // state.  (In other words, we've gotten to a point where we can no longer
            // go back into a *? we saw earlier.)  Clear out the list of looping states
            // and backfill any states that need to be backfilled.
            if (clearLoopingStates) {
                setLoopingStates(null, decisionPointList);
            }

            // advance to the next character, now that we've processed the current
            // character
            p += Character.charCount(c);
        }

        // this takes care of backfilling any states that still need to be backfilled
        setLoopingStates(null, decisionPointList);

        // when we reach the end of the string, we do a postprocessing step to mark the
        // end states.  The decision point list contains every state that can transition
        // to the end state-- that is, every state that is the last state in a sequence
        // that matches the rule.  All of these states are considered "mark states"
        // or "accepting states"-- that is, states that cause the position returned from
        // next() to be updated.  A mark state represents a possible break position.
        // This allows us to look ahead and remember how far the rule matched
        // before following the new branch (see next() for more information).
        // The temporary state table has an extra "flag column" at the end where this
        // information is stored.  We mark the end states by setting a flag in their
        // flag column.
        // Now if we saw the / in the rule, then everything after it is lookahead
        // material and the break really goes where the slash is.  In this case,
        // we mark these states as BOTH accepting states and lookahead states.  This
        // signals that these states cause the break position to be updated to the
        // position of the slash rather than the current break position.
        for (int i = 0; i < decisionPointList.size(); i++) {
            int rowNum = decisionPointList.elementAt(i).intValue();
            state = tempStateTable.elementAt(rowNum);
            state[numCategories] |= END_STATE_FLAG;
            if (sawEarlyBreak) {
                state[numCategories] |= LOOKAHEAD_STATE_FLAG;
            }
        }
    }


    /**
     * Update entries in the state table, and merge states when necessary to keep
     * the table deterministic.
     * @param rows The list of rows that need updating (the decision point list)
     * @param pendingChars A character category list, encoded in a String.  This is the
     * list of the columns that need updating.
     * @param newValue Update the cells specfied above to contain this value
     */
    private void updateStateTable(Vector<Integer> rows,
                                  String pendingChars,
                                  short newValue) {
        // create a dummy state that has the specified row number (newValue) in
        // the cells that need to be updated (those specified by pendingChars)
        // and 0 in the other cells
        short[] newValues = new short[numCategories + 1];
        for (int i = 0; i < pendingChars.length(); i++)
            newValues[(int)(pendingChars.charAt(i)) - 0x100] = newValue;

        // go through the list of rows to update, and update them by calling
        // mergeStates() to merge them the the dummy state we created
        for (int i = 0; i < rows.size(); i++) {
            mergeStates(rows.elementAt(i).intValue(), newValues, rows);
        }
    }

    /**
     * The real work of making the state table deterministic happens here.  This function
     * merges a state in the state table (specified by rowNum) with a state that is
     * passed in (newValues).  The basic process is to copy the nonzero cells in newStates
     * into the state in the state table (we'll call that oldValues).  If there's a
     * collision (i.e., if the same cell has a nonzero value in both states, and it's
     * not the SAME value), then we have to reconcile the collision.  We do this by
     * creating a new state, adding it to the end of the state table, and using this
     * function recursively to merge the original two states into a single, combined
     * state.  This process may happen recursively (i.e., each successive level may
     * involve collisions).  To prevent infinite recursion, we keep a log of merge
     * operations.  Any time we're merging two states we've merged before, we can just
     * supply the row number for the result of that merge operation rather than creating
     * a new state just like it.
     * @param rowNum The row number in the state table of the state to be updated
     * @param newValues The state to merge it with.
     * @param rowsBeingUpdated A copy of the list of rows passed to updateStateTable()
     * (itself a copy of the decision point list from parseRule()).  Newly-created
     * states get added to the decision point list if their "parents" were on it.
     */
    private void mergeStates(int rowNum,
                             short[] newValues,
                             Vector<Integer> rowsBeingUpdated) {
        short[] oldValues = tempStateTable.elementAt(rowNum);
        boolean isLoopingState = loopingStates.contains(Integer.valueOf(rowNum));

        // for each of the cells in the rows we're reconciling, do...
        for (int i = 0; i < oldValues.length; i++) {

            // if they contain the same value, we don't have to do anything
            if (oldValues[i] == newValues[i]) {
                continue;
            }

            // if oldValues is a looping state and the state the current cell points to
            // is too, then we can just stomp over the current value of that cell (and
            // set the clear-looping-states flag if necessary)
            else if (isLoopingState && loopingStates.contains(Integer.valueOf(oldValues[i]))) {
                if (newValues[i] != 0) {
                    if (oldValues[i] == 0) {
                        clearLoopingStates = true;
                    }
                    oldValues[i] = newValues[i];
                }
            }

            // if the current cell in oldValues is 0, copy in the corresponding value
            // from newValues
            else if (oldValues[i] == 0) {
                oldValues[i] = newValues[i];
            }

            // the last column of each row is the flag column.  Take care to merge the
            // flag words correctly
            else if (i == numCategories) {
                oldValues[i] = (short)((newValues[i] & ALL_FLAGS) | oldValues[i]);
            }

            // if both newValues and oldValues have a nonzero value in the current
            // cell, and it isn't the same value both places...
            else if (oldValues[i] != 0 && newValues[i] != 0) {

                // look up this pair of cell values in the merge list.  If it's
                // found, update the cell in oldValues to point to the merged state
                int combinedRowNum = searchMergeList(oldValues[i], newValues[i]);
                if (combinedRowNum != 0) {
                    oldValues[i] = (short)combinedRowNum;
                }

                // otherwise, we have to reconcile them...
                else {
                    // copy our row numbers into variables to make things easier
                    int oldRowNum = oldValues[i];
                    int newRowNum = newValues[i];
                    combinedRowNum = tempStateTable.size();

                    // add this pair of row numbers to the merge list (create it first
                    // if we haven't created the merge list yet)
                    if (mergeList == null) {
                        mergeList = new Vector<>();
                    }
                    mergeList.addElement(new int[] { oldRowNum, newRowNum, combinedRowNum });

                    // create a new row to represent the merged state, and copy the
                    // contents of oldRow into it, then add it to the end of the
                    // state table and update the original row (oldValues) to point
                    // to the new, merged, state
                    short[] newRow = new short[numCategories + 1];
                    short[] oldRow = tempStateTable.elementAt(oldRowNum);
                    System.arraycopy(oldRow, 0, newRow, 0, numCategories + 1);
                    tempStateTable.addElement(newRow);
                    oldValues[i] = (short)combinedRowNum;

                    // if the decision point list contains either of the parent rows,
                    // update it to include the new row as well
                    if ((decisionPointList.contains(Integer.valueOf(oldRowNum))
                            || decisionPointList.contains(Integer.valueOf(newRowNum)))
                        && !decisionPointList.contains(Integer.valueOf(combinedRowNum))
                    ) {
                        decisionPointList.addElement(Integer.valueOf(combinedRowNum));
                    }

                    // do the same thing with the list of rows being updated
                    if ((rowsBeingUpdated.contains(Integer.valueOf(oldRowNum))
                            || rowsBeingUpdated.contains(Integer.valueOf(newRowNum)))
                        && !rowsBeingUpdated.contains(Integer.valueOf(combinedRowNum))
                    ) {
                        decisionPointList.addElement(Integer.valueOf(combinedRowNum));
                    }
                    // now (groan) do the same thing for all the entries on the
                    // decision point stack
                    for (int k = 0; k < decisionPointStack.size(); k++) {
                        Vector<Integer> dpl = decisionPointStack.elementAt(k);
                        if ((dpl.contains(Integer.valueOf(oldRowNum))
                                || dpl.contains(Integer.valueOf(newRowNum)))
                            && !dpl.contains(Integer.valueOf(combinedRowNum))
                        ) {
                            dpl.addElement(Integer.valueOf(combinedRowNum));
                        }
                    }

                    // FINALLY (puff puff puff), call mergeStates() recursively to copy
                    // the row referred to by newValues into the new row and resolve any
                    // conflicts that come up at that level
                    mergeStates(combinedRowNum, tempStateTable.elementAt(
                                    newValues[i]), rowsBeingUpdated);
                }
            }
        }
        return;
    }

    /**
     * The merge list is a list of pairs of rows that have been merged somewhere in
     * the process of building this state table, along with the row number of the
     * row containing the merged state.  This function looks up a pair of row numbers
     * and returns the row number of the row they combine into.  (It returns 0 if
     * this pair of rows isn't in the merge list.)
     */
    private int searchMergeList(int a, int b) {
        // if there is no merge list, there obviously isn't anything in it
        if (mergeList == null) {
            return 0;
        }

        // otherwise, for each element in the merge list...
        else {
            int[] entry;
            for (int i = 0; i < mergeList.size(); i++) {
                entry = mergeList.elementAt(i);

                // we have a hit if the two row numbers match the two row numbers
                // in the beginning of the entry (the two that combine), in either
                // order
                if ((entry[0] == a && entry[1] == b) || (entry[0] == b && entry[1] == a)) {
                    return entry[2];
                }

                // we also have a hit if one of the two row numbers matches the marged
                // row number and the other one matches one of the original row numbers
                if ((entry[2] == a && (entry[0] == b || entry[1] == b))) {
                    return entry[2];
                }
                if ((entry[2] == b && (entry[0] == a || entry[1] == a))) {
                    return entry[2];
                }
            }
            return 0;
        }
    }

    /**
     * This function is used to update the list of current loooping states (i.e.,
     * states that are controlled by a *? construct).  It backfills values from
     * the looping states into unpopulated cells of the states that are currently
     * marked for backfilling, and then updates the list of looping states to be
     * the new list
     * @param newLoopingStates The list of new looping states
     * @param endStates The list of states to treat as end states (states that
     * can exit the loop).
     */
    private void setLoopingStates(Vector<Integer> newLoopingStates,
                                  Vector<Integer> endStates) {

        // if the current list of looping states isn't empty, we have to backfill
        // values from the looping states into the states that are waiting to be
        // backfilled
        if (!loopingStates.isEmpty()) {
            int loopingState = loopingStates.lastElement().intValue();
            int rowNum;

            // don't backfill into an end state OR any state reachable from an end state
            // (since the search for reachable states is recursive, it's split out into
            // a separate function, eliminateBackfillStates(), below)
            for (int i = 0; i < endStates.size(); i++) {
                eliminateBackfillStates(endStates.elementAt(i).intValue());
            }

            // we DON'T actually backfill the states that need to be backfilled here.
            // Instead, we MARK them for backfilling.  The reason for this is that if
            // there are multiple rules in the state-table description, the looping
            // states may have some of their values changed by a succeeding rule, and
            // this wouldn't be reflected in the backfilled states.  We mark a state
            // for backfilling by putting the row number of the state to copy from
            // into the flag cell at the end of the row
            for (int i = 0; i < statesToBackfill.size(); i++) {
                rowNum = statesToBackfill.elementAt(i).intValue();
                short[] state = tempStateTable.elementAt(rowNum);
                state[numCategories] =
                    (short)((state[numCategories] & ALL_FLAGS) | loopingState);
            }
            statesToBackfill.removeAllElements();
            loopingStates.removeAllElements();
        }

        if (newLoopingStates != null) {
            @SuppressWarnings("unchecked")
            Vector<Integer> clone = (Vector<Integer>)newLoopingStates.clone();
            loopingStates = clone;
        }
    }

    /**
     * This removes "ending states" and states reachable from them from the
     * list of states to backfill.
     * @param The row number of the state to remove from the backfill list
     */
    private void eliminateBackfillStates(int baseState) {

        // don't do anything unless this state is actually in the backfill list...
        if (statesToBackfill.contains(Integer.valueOf(baseState))) {

            // if it is, take it out
            statesToBackfill.removeElement(Integer.valueOf(baseState));

            // then go through and recursively call this function for every
            // state that the base state points to
            short[] state = tempStateTable.elementAt(baseState);
            for (int i = 0; i < numCategories; i++) {
                if (state[i] != 0) {
                    eliminateBackfillStates(state[i]);
                }
            }
        }
    }

    /**
     * This function completes the backfilling process by actually doing the
     * backfilling on the states that are marked for it
     */
    private void backfillLoopingStates() {
        short[] state;
        short[] loopingState = null;
        int loopingStateRowNum = 0;
        int fromState;

        // for each state in the state table...
        for (int i = 0; i < tempStateTable.size(); i++) {
            state = tempStateTable.elementAt(i);

            // check the state's flag word to see if it's marked for backfilling
            // (it's marked for backfilling if any bits other than the two high-order
            // bits are set-- if they are, then the flag word, minus the two high bits,
            // is the row number to copy from)
            fromState = state[numCategories] & ~ALL_FLAGS;
            if (fromState > 0) {

                // load up the state to copy from (if we haven't already)
                if (fromState != loopingStateRowNum) {
                    loopingStateRowNum = fromState;
                    loopingState = tempStateTable.elementAt(loopingStateRowNum);
                }

                // clear out the backfill part of the flag word
                state[numCategories] &= ALL_FLAGS;

                // then fill all zero cells in the current state with values
                // from the corresponding cells of the fromState
                for (int j = 0; j < state.length; j++) {
                    if (state[j] == 0) {
                        state[j] = loopingState[j];
                    }
                    else if (state[j] == DONT_LOOP_FLAG) {
                        state[j] = 0;
                    }
                }
            }
        }
    }

    /**
     * This function completes the state-table-building process by doing several
     * postprocessing steps and copying everything into its final resting place
     * in the iterator itself
     * @param forward True if we're working on the forward state table
     */
    private void finishBuildingStateTable(boolean forward) {
        // start by backfilling the looping states
        backfillLoopingStates();

        int[] rowNumMap = new int[tempStateTable.size()];
        Stack<Integer> rowsToFollow = new Stack<>();
        rowsToFollow.push(Integer.valueOf(1));
        rowNumMap[1] = 1;

        // determine which states are no longer reachable from the start state
        // (the reachable states will have their row numbers in the row number
        // map, and the nonreachable states will have zero in the row number map)
        while (rowsToFollow.size() != 0) {
            int rowNum = rowsToFollow.pop().intValue();
            short[] row = tempStateTable.elementAt(rowNum);

            for (int i = 0; i < numCategories; i++) {
                if (row[i] != 0) {
                    if (rowNumMap[row[i]] == 0) {
                        rowNumMap[row[i]] = row[i];
                        rowsToFollow.push(Integer.valueOf(row[i]));
                    }
                }
            }
        }

        boolean madeChange;
        int newRowNum;

        // algorithm for minimizing the number of states in the table adapted from
        // Aho & Ullman, "Principles of Compiler Design"
        // The basic idea here is to organize the states into classes.  When we're done,
        // all states in the same class can be considered identical and all but one eliminated.

        // initially assign states to classes based on the number of populated cells they
        // contain (the class number is the number of populated cells)
        int[] stateClasses = new int[tempStateTable.size()];
        int nextClass = numCategories + 1;
        short[] state1, state2;
        for (int i = 1; i < stateClasses.length; i++) {
            if (rowNumMap[i] == 0) {
                continue;
            }
            state1 = tempStateTable.elementAt(i);
            for (int j = 0; j < numCategories; j++) {
                if (state1[j] != 0) {
                    ++stateClasses[i];
                }
            }
            if (stateClasses[i] == 0) {
                stateClasses[i] = nextClass;
            }
        }
        ++nextClass;

        // then, for each class, elect the first member of that class as that class's
        // "representative".  For each member of the class, compare it to the "representative."
        // If there's a column position where the state being tested transitions to a
        // state in a DIFFERENT class from the class where the "representative" transitions,
        // then move the state into a new class.  Repeat this process until no new classes
        // are created.
        int currentClass;
        int lastClass;
        boolean split;

        do {
            currentClass = 1;
            lastClass = nextClass;
            while (currentClass < nextClass) {
                split = false;
                state1 = state2 = null;
                for (int i = 0; i < stateClasses.length; i++) {
                    if (stateClasses[i] == currentClass) {
                        if (state1 == null) {
                            state1 = tempStateTable.elementAt(i);
                        }
                        else {
                            state2 = tempStateTable.elementAt(i);
                            for (int j = 0; j < state2.length; j++) {
                                if ((j == numCategories && state1[j] != state2[j] && forward)
                                        || (j != numCategories && stateClasses[state1[j]]
                                        != stateClasses[state2[j]])) {
                                    stateClasses[i] = nextClass;
                                    split = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (split) {
                    ++nextClass;
                }
                ++currentClass;
            }
        } while (lastClass != nextClass);

        // at this point, all of the states in a class except the first one (the
        //"representative") can be eliminated, so update the row-number map accordingly
        int[] representatives = new int[nextClass];
        for (int i = 1; i < stateClasses.length; i++)
            if (representatives[stateClasses[i]] == 0) {
                representatives[stateClasses[i]] = i;
            }
            else {
                rowNumMap[i] = representatives[stateClasses[i]];
            }

        // renumber all remaining rows...
        // first drop all that are either unreferenced or not a class representative
        for (int i = 1; i < rowNumMap.length; i++) {
            if (rowNumMap[i] != i) {
                tempStateTable.setElementAt(null, i);
            }
        }

        // then calculate everybody's new row number and update the row
        // number map appropriately (the first pass updates the row numbers
        // of all the class representatives [the rows we're keeping], and the
        // second pass updates the cross references for all the rows that
        // are being deleted)
        newRowNum = 1;
        for (int i = 1; i < rowNumMap.length; i++) {
            if (tempStateTable.elementAt(i) != null) {
                rowNumMap[i] = newRowNum++;
            }
        }
        for (int i = 1; i < rowNumMap.length; i++) {
            if (tempStateTable.elementAt(i) == null) {
                rowNumMap[i] = rowNumMap[rowNumMap[i]];
            }
        }

        // allocate the permanent state table, and copy the remaining rows into it
        // (adjusting all the cell values, of course)

        // this section does that for the forward state table
        if (forward) {
            endStates = new boolean[newRowNum];
            lookaheadStates = new boolean[newRowNum];
            stateTable = new short[newRowNum * numCategories];
            int p = 0;
            int p2 = 0;
            for (int i = 0; i < tempStateTable.size(); i++) {
                short[] row = tempStateTable.elementAt(i);
                if (row == null) {
                    continue;
                }
                for (int j = 0; j < numCategories; j++) {
                    stateTable[p] = (short)(rowNumMap[row[j]]);
                    ++p;
                }
                endStates[p2] = ((row[numCategories] & END_STATE_FLAG) != 0);
                lookaheadStates[p2] = ((row[numCategories] & LOOKAHEAD_STATE_FLAG) != 0);
                ++p2;
            }
        }

        // and this section does it for the backward state table
        else {
            backwardsStateTable = new short[newRowNum * numCategories];
            int p = 0;
            for (int i = 0; i < tempStateTable.size(); i++) {
                short[] row = tempStateTable.elementAt(i);
                if (row == null) {
                    continue;
                }
                for (int j = 0; j < numCategories; j++) {
                    backwardsStateTable[p] = (short)(rowNumMap[row[j]]);
                    ++p;
                }
            }
        }
    }

    /**
     * This function builds the backward state table from the forward state
     * table and any additional rules (identified by the ! on the front)
     * supplied in the description
     */
    private void buildBackwardsStateTable(Vector<String> tempRuleList) {

        // create the temporary state table and seed it with two rows (row 0
        // isn't used for anything, and we have to create row 1 (the initial
        // state) before we can do anything else
        tempStateTable = new Vector<>();
        tempStateTable.addElement(new short[numCategories + 1]);
        tempStateTable.addElement(new short[numCategories + 1]);

        // although the backwards state table is built automatically from the forward
        // state table, there are some situations (the default sentence-break rules,
        // for example) where this doesn't yield enough stop states, causing a dramatic
        // drop in performance.  To help with these cases, the user may supply
        // supplemental rules that are added to the backward state table.  These have
        // the same syntax as the normal break rules, but begin with '!' to distinguish
        // them from normal break rules
        for (int i = 0; i < tempRuleList.size(); i++) {
            String rule = tempRuleList.elementAt(i);
            if (rule.charAt(0) == '!') {
                parseRule(rule.substring(1), false);
            }
        }
        backfillLoopingStates();

        // Backwards iteration is qualitatively different from forwards iteration.
        // This is because backwards iteration has to be made to operate from no context
        // at all-- the user should be able to ask BreakIterator for the break position
        // immediately on either side of some arbitrary offset in the text.  The
        // forward iteration table doesn't let us do that-- it assumes complete
        // information on the context, which means starting from the beginning of the
        // document.
        // The way we do backward and random-access iteration is to back up from the
        // current (or user-specified) position until we see something we're sure is
        // a break position (it may not be the last break position immediately
        // preceding our starting point, however).  Then we roll forward from there to
        // locate the actual break position we're after.
        // This means that the backwards state table doesn't have to identify every
        // break position, allowing the building algorithm to be much simpler.  Here,
        // we use a "pairs" approach, scanning the forward-iteration state table for
        // pairs of character categories we ALWAYS break between, and building a state
        // table from that information.  No context is required-- all this state table
        // looks at is a pair of adjacent characters.

        // It's possible that the user has supplied supplementary rules (see above).
        // This has to be done first to keep parseRule() and friends from becoming
        // EVEN MORE complicated.  The automatically-generated states are appended
        // onto the end of the state table, and then the two sets of rules are
        // stitched together at the end.  Take note of the row number of the
        // first row of the auromatically-generated part.
        int backTableOffset = tempStateTable.size();
        if (backTableOffset > 2) {
            ++backTableOffset;
        }

        // the automatically-generated part of the table models a two-dimensional
        // array where the two dimensions represent the two characters we're currently
        // looking at.  To model this as a state table, we actually need one additional
        // row to represent the initial state.  It gets populated with the row numbers
        // of the other rows (in order).
        for (int i = 0; i < numCategories + 1; i++)
            tempStateTable.addElement(new short[numCategories + 1]);

        short[] state = tempStateTable.elementAt(backTableOffset - 1);
        for (int i = 0; i < numCategories; i++)
            state[i] = (short)(i + backTableOffset);

        // scavenge the forward state table for pairs of character categories
        // that always have a break between them.  The algorithm is as follows:
        // Look down each column in the state table.  For each nonzero cell in
        // that column, look up the row it points to.  For each nonzero cell in
        // that row, populate a cell in the backwards state table: the row number
        // of that cell is the number of the column we were scanning (plus the
        // offset that locates this sub-table), and the column number of that cell
        // is the column number of the nonzero cell we just found.  This cell is
        // populated with its own column number (adjusted according to the actual
        // location of the sub-table).  This process will produce a state table
        // whose behavior is the same as looking up successive pairs of characters
        // in an array of Booleans to determine whether there is a break.
        int numRows = stateTable.length / numCategories;
        for (int column = 0; column < numCategories; column++) {
            for (int row = 0; row < numRows; row++) {
                int nextRow = lookupState(row, column);
                if (nextRow != 0) {
                    for (int nextColumn = 0; nextColumn < numCategories; nextColumn++) {
                        int cellValue = lookupState(nextRow, nextColumn);
                        if (cellValue != 0) {
                            state = tempStateTable.elementAt(nextColumn +
                                            backTableOffset);
                            state[column] = (short)(column + backTableOffset);
                        }
                    }
                }
            }
        }

        // if the user specified some backward-iteration rules with the ! token,
        // we have to merge the resulting state table with the auto-generated one
        // above.  First copy the populated cells from row 1 over the populated
        // cells in the auto-generated table.  Then copy values from row 1 of the
        // auto-generated table into all of the the unpopulated cells of the
        // rule-based table.
        if (backTableOffset > 1) {

            // for every row in the auto-generated sub-table, if a cell is
            // populated that is also populated in row 1 of the rule-based
            // sub-table, copy the value from row 1 over the value in the
            // auto-generated sub-table
            state = tempStateTable.elementAt(1);
            for (int i = backTableOffset - 1; i < tempStateTable.size(); i++) {
                short[] state2 = tempStateTable.elementAt(i);
                for (int j = 0; j < numCategories; j++) {
                    if (state[j] != 0 && state2[j] != 0) {
                        state2[j] = state[j];
                    }
                }
            }

            // now, for every row in the rule-based sub-table that is not
            // an end state, fill in all unpopulated cells with the values
            // of the corresponding cells in the first row of the auto-
            // generated sub-table.
            state = tempStateTable.elementAt(backTableOffset - 1);
            for (int i = 1; i < backTableOffset - 1; i++) {
                short[] state2 = tempStateTable.elementAt(i);
                if ((state2[numCategories] & END_STATE_FLAG) == 0) {
                    for (int j = 0; j < numCategories; j++) {
                        if (state2[j] == 0) {
                            state2[j] = state[j];
                        }
                    }
                }
            }
        }

        // finally, clean everything up and copy it into the actual BreakIterator
        // by calling finishBuildingStateTable()
        finishBuildingStateTable(false);
    }

    /**
     * Given a current state and a character category, looks up the
     * next state to transition to in the state table.
     */
    protected int lookupState(int state, int category) {
        return stateTable[state * numCategories + category];
    }

    /**
     * Throws an IllegalArgumentException representing a syntax error in the rule
     * description.  The exception's message contains some debugging information.
     * @param message A message describing the problem
     * @param position The position in the description where the problem was
     * discovered
     * @param context The string containing the error
     */
    protected void error(String message, int position, String context) {
        throw new IllegalArgumentException("Parse error at position (" + position + "): " + message + "\n" +
                context.substring(0, position) + " -here- " + context.substring(position));
    }

    void makeFile(String filename) {
        writeTables(filename);
    }

    /**
     * Magic number for the BreakIterator data file format.
     */
    private static final byte[] LABEL = {
        (byte)'B', (byte)'I', (byte)'d', (byte)'a', (byte)'t', (byte)'a',
        (byte)'\0'
    };

    /**
     * Version number of the dictionary that was read in.
     */
    private static final byte[] supportedVersion = { (byte)1 };

    /**
     * Header size in byte count
     */
     private static final int HEADER_LENGTH = 36;

    /**
     * Array length of indices for BMP characters
     */
     private static final int BMP_INDICES_LENGTH = 512;

    /**
     * Read datafile. The datafile's format is as follows:
     * <pre>
     *   BreakIteratorData {
     *       u1           magic[7];
     *       u1           version;
     *       u4           totalDataSize;
     *       header_info  header;
     *       body         value;
     *   }
     * </pre>
     * <code>totalDataSize</code> is the summation of the size of
     * <code>header_info</code> and <code>body</code> in byte count.
     * <p>
     * In <code>header</code>, each field except for checksum implies the
     * length of each field. Since <code>BMPdataLength</code> is a fixed-length
     *  data(512 entries), its length isn't included in <code>header</code>.
     * <code>checksum</code> is a CRC32 value of all in <code>body</code>.
     * <pre>
     *   header_info {
     *       u4           stateTableLength;
     *       u4           backwardsStateTableLength;
     *       u4           endStatesLength;
     *       u4           lookaheadStatesLength;
     *       u4           BMPdataLength;
     *       u4           nonBMPdataLength;
     *       u4           additionalDataLength;
     *       u8           checksum;
     *   }
     * </pre>
     * <p>
     *
     * Finally, <code>BMPindices</code> and <code>BMPdata</code> are set to
     * <code>charCategoryTable</code>. <code>nonBMPdata</code> is set to
     * <code>supplementaryCharCategoryTable</code>.
     * <pre>
     *   body {
     *       u2           stateTable[stateTableLength];
     *       u2           backwardsStateTable[backwardsStateTableLength];
     *       u1           endStates[endStatesLength];
     *       u1           lookaheadStates[lookaheadStatesLength];
     *       u2           BMPindices[512];
     *       u1           BMPdata[BMPdataLength];
     *       u4           nonBMPdata[numNonBMPdataLength];
     *       u1           additionalData[additionalDataLength];
     *   }
     * </pre>
     */
    protected void writeTables(String datafile) {
        final String filename;
        final String outputDir;
        String tmpbuf = GenerateBreakIteratorData.getOutputDirectory();

        if (tmpbuf.equals("")) {
            filename = datafile;
            outputDir = "";
        } else {
            char sep = File.separatorChar;
            if (sep == '/') {
                outputDir = tmpbuf;
            } else if (sep == '\\') {
                outputDir = tmpbuf.replaceAll("/", "\\\\");
            } else {
                outputDir = tmpbuf.replaceAll("/", String.valueOf(sep));
            }

            filename = outputDir + sep + datafile;
        }

        try {
            if (!outputDir.equals("")) {
                new File(outputDir).mkdirs();
            }
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename));

            byte[] BMPdata = charCategoryTable.getStringArray();
            short[] BMPindices = charCategoryTable.getIndexArray();
            int[] nonBMPdata = supplementaryCharCategoryTable.getArray();

            if (BMPdata.length <= 0) {
                throw new InternalError("Wrong BMP data length(" + BMPdata.length + ")");
            }
            if (BMPindices.length != BMP_INDICES_LENGTH) {
                throw new InternalError("Wrong BMP indices length(" + BMPindices.length + ")");
            }
            if (nonBMPdata.length <= 0) {
                throw new InternalError("Wrong non-BMP data length(" + nonBMPdata.length + ")");
            }

            int len;

            /* Compute checksum */
            CRC32 crc32 = new CRC32();
            len = stateTable.length;
            for (int i = 0; i < len; i++) {
                crc32.update(stateTable[i]);
            }
            len = backwardsStateTable.length;
            for (int i = 0; i < len; i++) {
                crc32.update(backwardsStateTable[i]);
            }
            crc32.update(toByteArray(endStates));
            crc32.update(toByteArray(lookaheadStates));
            for (int i = 0; i < BMP_INDICES_LENGTH; i++) {
                crc32.update(BMPindices[i]);
            }
            crc32.update(BMPdata);
            len = nonBMPdata.length;
            for (int i = 0; i < len; i++) {
                crc32.update(nonBMPdata[i]);
            }
            if (additionalData != null) {
                len = additionalData.length;
                for (int i = 0; i < len; i++) {
                    crc32.update(additionalData[i]);
                }
            }

            /* First, write magic, version, and totalDataSize. */
            len = HEADER_LENGTH +
                  (stateTable.length + backwardsStateTable.length) * 2 +
                  endStates.length + lookaheadStates.length + 1024 +
                  BMPdata.length + nonBMPdata.length * 4 +
                  ((additionalData == null) ? 0 : additionalData.length);
            out.write(LABEL);
            out.write(supportedVersion);
            out.write(toByteArray(len));

            /* Write header_info. */
            out.write(toByteArray(stateTable.length));
            out.write(toByteArray(backwardsStateTable.length));
            out.write(toByteArray(endStates.length));
            out.write(toByteArray(lookaheadStates.length));
            out.write(toByteArray(BMPdata.length));
            out.write(toByteArray(nonBMPdata.length));
            if (additionalData == null) {
                out.write(toByteArray(0));
            } else {
                out.write(toByteArray(additionalData.length));
            }
            out.write(toByteArray(crc32.getValue()));

            /* Write stateTable[numCategories * numRows] */
            len = stateTable.length;
            for (int i = 0; i < len; i++) {
                out.write(toByteArray(stateTable[i]));
            }

            /* Write backwardsStateTable[numCategories * numRows] */
            len = backwardsStateTable.length;
            for (int i = 0; i < len; i++) {
                out.write(toByteArray(backwardsStateTable[i]));
            }

            /* Write endStates[numRows] */
            out.write(toByteArray(endStates));

            /* Write lookaheadStates[numRows] */
            out.write(toByteArray(lookaheadStates));

            for (int i = 0; i < BMP_INDICES_LENGTH; i++) {
                out.write(toByteArray(BMPindices[i]));
            }
            BMPindices = null;
            out.write(BMPdata);
            BMPdata = null;

            /* Write a category table for non-BMP characters. */
            len = nonBMPdata.length;
            for (int i = 0; i < len; i++) {
                out.write(toByteArray(nonBMPdata[i]));
            }
            nonBMPdata = null;

            /* Write additional data */
            if (additionalData != null) {
                out.write(additionalData);
            }

            out.close();
        }
        catch (Exception e) {
            throw new InternalError(e.toString());
        }
    }

    byte[] toByteArray(short val) {
        byte[] buf = new byte[2];
        buf[0] = (byte)((val>>>8) & 0xFF);
        buf[1] = (byte)(val & 0xFF);
        return buf;
    }

    byte[] toByteArray(int val) {
        byte[] buf = new byte[4];
        buf[0] = (byte)((val>>>24) & 0xFF);
        buf[1] = (byte)((val>>>16) & 0xFF);
        buf[2] = (byte)((val>>>8) & 0xFF);
        buf[3] = (byte)(val & 0xFF);
        return buf;
    }

    byte[] toByteArray(long val) {
        byte[] buf = new byte[8];
        buf[0] = (byte)((val>>>56) & 0xff);
        buf[1] = (byte)((val>>>48) & 0xff);
        buf[2] = (byte)((val>>>40) & 0xff);
        buf[3] = (byte)((val>>>32) & 0xff);
        buf[4] = (byte)((val>>>24) & 0xff);
        buf[5] = (byte)((val>>>16) & 0xff);
        buf[6] = (byte)((val>>>8) & 0xff);
        buf[7] = (byte)(val & 0xff);
        return buf;
    }

    byte[] toByteArray(boolean[] data) {
        byte[] buf = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            buf[i] = data[i] ? (byte)1 : (byte)0;
        }
        return buf;
    }

    void setAdditionalData(byte[] data) {
        additionalData = data;
    }
}
