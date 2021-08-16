/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 *
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 2002 - All Rights Reserved
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 */

package sun.text;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.MissingResourceException;
import sun.text.CompactByteArray;
import sun.text.SupplementaryCharacterData;

/**
 * <p>A subclass of BreakIterator whose behavior is specified using a list of rules.</p>
 *
 * <p>There are two kinds of rules, which are separated by semicolons: <i>substitutions</i>
 * and <i>regular expressions.</i></p>
 *
 * <p>A substitution rule defines a name that can be used in place of an expression. It
 * consists of a name, which is a string of characters contained in angle brackets, an equals
 * sign, and an expression. (There can be no whitespace on either side of the equals sign.)
 * To keep its syntactic meaning intact, the expression must be enclosed in parentheses or
 * square brackets. A substitution is visible after its definition, and is filled in using
 * simple textual substitution. Substitution definitions can contain other substitutions, as
 * long as those substitutions have been defined first. Substitutions are generally used to
 * make the regular expressions (which can get quite complex) shorted and easier to read.
 * They typically define either character categories or commonly-used subexpressions.</p>
 *
 * <p>There is one special substitution.&nbsp; If the description defines a substitution
 * called &quot;&lt;ignore&gt;&quot;, the expression must be a [] expression, and the
 * expression defines a set of characters (the &quot;<em>ignore characters</em>&quot;) that
 * will be transparent to the BreakIterator.&nbsp; A sequence of characters will break the
 * same way it would if any ignore characters it contains are taken out.&nbsp; Break
 * positions never occur befoer ignore characters.</p>
 *
 * <p>A regular expression uses a subset of the normal Unix regular-expression syntax, and
 * defines a sequence of characters to be kept together. With one significant exception, the
 * iterator uses a longest-possible-match algorithm when matching text to regular
 * expressions. The iterator also treats descriptions containing multiple regular expressions
 * as if they were ORed together (i.e., as if they were separated by |).</p>
 *
 * <p>The special characters recognized by the regular-expression parser are as follows:</p>
 *
 * <blockquote>
 *   <table border="1" width="100%">
 *     <tr>
 *       <td width="6%">*</td>
 *       <td width="94%">Specifies that the expression preceding the asterisk may occur any number
 *       of times (including not at all).</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">{}</td>
 *       <td width="94%">Encloses a sequence of characters that is optional.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">()</td>
 *       <td width="94%">Encloses a sequence of characters.&nbsp; If followed by *, the sequence
 *       repeats.&nbsp; Otherwise, the parentheses are just a grouping device and a way to delimit
 *       the ends of expressions containing |.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">|</td>
 *       <td width="94%">Separates two alternative sequences of characters.&nbsp; Either one
 *       sequence or the other, but not both, matches this expression.&nbsp; The | character can
 *       only occur inside ().</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">.</td>
 *       <td width="94%">Matches any character.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">*?</td>
 *       <td width="94%">Specifies a non-greedy asterisk.&nbsp; *? works the same way as *, except
 *       when there is overlap between the last group of characters in the expression preceding the
 *       * and the first group of characters following the *.&nbsp; When there is this kind of
 *       overlap, * will match the longest sequence of characters that match the expression before
 *       the *, and *? will match the shortest sequence of characters matching the expression
 *       before the *?.&nbsp; For example, if you have &quot;xxyxyyyxyxyxxyxyxyy&quot; in the text,
 *       &quot;x[xy]*x&quot; will match through to the last x (i.e., &quot;<strong>xxyxyyyxyxyxxyxyx</strong>yy&quot;,
 *       but &quot;x[xy]*?x&quot; will only match the first two xes (&quot;<strong>xx</strong>yxyyyxyxyxxyxyxyy&quot;).</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">[]</td>
 *       <td width="94%">Specifies a group of alternative characters.&nbsp; A [] expression will
 *       match any single character that is specified in the [] expression.&nbsp; For more on the
 *       syntax of [] expressions, see below.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">/</td>
 *       <td width="94%">Specifies where the break position should go if text matches this
 *       expression.&nbsp; (e.g., &quot;[a-z]&#42;/[:Zs:]*[1-0]&quot; will match if the iterator sees a run
 *       of letters, followed by a run of whitespace, followed by a digit, but the break position
 *       will actually go before the whitespace).&nbsp; Expressions that don't contain / put the
 *       break position at the end of the matching text.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">\</td>
 *       <td width="94%">Escape character.&nbsp; The \ itself is ignored, but causes the next
 *       character to be treated as literal character.&nbsp; This has no effect for many
 *       characters, but for the characters listed above, this deprives them of their special
 *       meaning.&nbsp; (There are no special escape sequences for Unicode characters, or tabs and
 *       newlines; these are all handled by a higher-level protocol.&nbsp; In a Java string,
 *       &quot;\n&quot; will be converted to a literal newline character by the time the
 *       regular-expression parser sees it.&nbsp; Of course, this means that \ sequences that are
 *       visible to the regexp parser must be written as \\ when inside a Java string.)&nbsp; All
 *       characters in the ASCII range except for letters, digits, and control characters are
 *       reserved characters to the parser and must be preceded by \ even if they currently don't
 *       mean anything.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">!</td>
 *       <td width="94%">If ! appears at the beginning of a regular expression, it tells the regexp
 *       parser that this expression specifies the backwards-iteration behavior of the iterator,
 *       and not its normal iteration behavior.&nbsp; This is generally only used in situations
 *       where the automatically-generated backwards-iteration brhavior doesn't produce
 *       satisfactory results and must be supplemented with extra client-specified rules.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%"><em>(all others)</em></td>
 *       <td width="94%">All other characters are treated as literal characters, which must match
 *       the corresponding character(s) in the text exactly.</td>
 *     </tr>
 *   </table>
 * </blockquote>
 *
 * <p>Within a [] expression, a number of other special characters can be used to specify
 * groups of characters:</p>
 *
 * <blockquote>
 *   <table border="1" width="100%">
 *     <tr>
 *       <td width="6%">-</td>
 *       <td width="94%">Specifies a range of matching characters.&nbsp; For example
 *       &quot;[a-p]&quot; matches all lowercase Latin letters from a to p (inclusive).&nbsp; The -
 *       sign specifies ranges of continuous Unicode numeric values, not ranges of characters in a
 *       language's alphabetical order: &quot;[a-z]&quot; doesn't include capital letters, nor does
 *       it include accented letters such as a-umlaut.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">::</td>
 *       <td width="94%">A pair of colons containing a one- or two-letter code matches all
 *       characters in the corresponding Unicode category.&nbsp; The two-letter codes are the same
 *       as the two-letter codes in the Unicode database (for example, &quot;[:Sc::Sm:]&quot;
 *       matches all currency symbols and all math symbols).&nbsp; Specifying a one-letter code is
 *       the same as specifying all two-letter codes that begin with that letter (for example,
 *       &quot;[:L:]&quot; matches all letters, and is equivalent to
 *       &quot;[:Lu::Ll::Lo::Lm::Lt:]&quot;).&nbsp; Anything other than a valid two-letter Unicode
 *       category code or a single letter that begins a Unicode category code is illegal within
 *       colons.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">[]</td>
 *       <td width="94%">[] expressions can nest.&nbsp; This has no effect, except when used in
 *       conjunction with the ^ token.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">^</td>
 *       <td width="94%">Excludes the character (or the characters in the [] expression) following
 *       it from the group of characters.&nbsp; For example, &quot;[a-z^p]&quot; matches all Latin
 *       lowercase letters except p.&nbsp; &quot;[:L:^[&#92;u4e00-&#92;u9fff]]&quot; matches all letters
 *       except the Han ideographs.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%"><em>(all others)</em></td>
 *       <td width="94%">All other characters are treated as literal characters.&nbsp; (For
 *       example, &quot;[aeiou]&quot; specifies just the letters a, e, i, o, and u.)</td>
 *     </tr>
 *   </table>
 * </blockquote>
 *
 * <p>For a more complete explanation, see <a
 * href="http://www.ibm.com/java/education/boundaries/boundaries.html">http://www.ibm.com/java/education/boundaries/boundaries.html</a>.
 * &nbsp; For examples, see the resource data (which is annotated).</p>
 *
 * @author Richard Gillam
 */
public class RuleBasedBreakIterator extends BreakIterator {

    /**
     * A token used as a character-category value to identify ignore characters
     */
    protected static final byte IGNORE = -1;

    /**
     * The state number of the starting state
     */
    private static final short START_STATE = 1;

    /**
     * The state-transition value indicating "stop"
     */
    private static final short STOP_STATE = 0;

    /**
     * Magic number for the BreakIterator data file format.
     */
    static final byte[] LABEL = {
        (byte)'B', (byte)'I', (byte)'d', (byte)'a', (byte)'t', (byte)'a',
        (byte)'\0'
    };
    static final int    LABEL_LENGTH = LABEL.length;

    /**
     * Version number of the dictionary that was read in.
     */
    static final byte supportedVersion = 1;

    /**
     * An array length of indices for BMP characters
     */
    private static final int BMP_INDICES_LENGTH = 512;

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
     * The character iterator through which this BreakIterator accesses the text
     */
    private CharacterIterator text = null;

    /**
     * A CRC32 value of all data in datafile
     */
    private long checksum;

    //=======================================================================
    // constructors
    //=======================================================================

    /**
     * Constructs a RuleBasedBreakIterator using the given rule data.
     *
     * @throws MissingResourceException if the rule data is invalid or corrupted
     */
    public RuleBasedBreakIterator(String ruleFile, byte[] ruleData) {
        ByteBuffer bb = ByteBuffer.wrap(ruleData);
        try {
            validateRuleData(ruleFile, bb);
            setupTables(ruleFile, bb);
        } catch (BufferUnderflowException bue) {
            MissingResourceException e;
            e = new MissingResourceException("Corrupted rule data file", ruleFile, "");
            e.initCause(bue);
            throw e;
        }
    }

    /**
     * Initializes the fields with the given rule data.
     * The data format is as follows:
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
     *
     * @throws BufferUnderflowException if the end-of-data is reached before
     *                                  setting up all the tables
     */
    private void setupTables(String ruleFile, ByteBuffer bb) {
        /* Read header_info. */
        int stateTableLength = bb.getInt();
        int backwardsStateTableLength = bb.getInt();
        int endStatesLength = bb.getInt();
        int lookaheadStatesLength = bb.getInt();
        int BMPdataLength = bb.getInt();
        int nonBMPdataLength = bb.getInt();
        int additionalDataLength = bb.getInt();
        checksum = bb.getLong();

        /* Read stateTable[numCategories * numRows] */
        stateTable = new short[stateTableLength];
        for (int i = 0; i < stateTableLength; i++) {
            stateTable[i] = bb.getShort();
        }

        /* Read backwardsStateTable[numCategories * numRows] */
        backwardsStateTable = new short[backwardsStateTableLength];
        for (int i = 0; i < backwardsStateTableLength; i++) {
            backwardsStateTable[i] = bb.getShort();
        }

        /* Read endStates[numRows] */
        endStates = new boolean[endStatesLength];
        for (int i = 0; i < endStatesLength; i++) {
            endStates[i] = bb.get() == 1;
        }

        /* Read lookaheadStates[numRows] */
        lookaheadStates = new boolean[lookaheadStatesLength];
        for (int i = 0; i < lookaheadStatesLength; i++) {
            lookaheadStates[i] = bb.get() == 1;
        }

        /* Read a category table and indices for BMP characters. */
        short[] temp1 = new short[BMP_INDICES_LENGTH];  // BMPindices
        for (int i = 0; i < BMP_INDICES_LENGTH; i++) {
            temp1[i] = bb.getShort();
        }
        byte[] temp2 = new byte[BMPdataLength];  // BMPdata
        bb.get(temp2);
        charCategoryTable = new CompactByteArray(temp1, temp2);

        /* Read a category table for non-BMP characters. */
        int[] temp3 = new int[nonBMPdataLength];
        for (int i = 0; i < nonBMPdataLength; i++) {
            temp3[i] = bb.getInt();
        }
        supplementaryCharCategoryTable = new SupplementaryCharacterData(temp3);

        /* Read additional data */
        if (additionalDataLength > 0) {
            additionalData = new byte[additionalDataLength];
            bb.get(additionalData);
        }
        assert bb.position() == bb.limit();

        /* Set numCategories */
        numCategories = stateTable.length / endStates.length;
    }

    /**
     * Validates the magic number, version, and the length of the given data.
     *
     * @throws BufferUnderflowException if the end-of-data is reached while
     *                                  validating data
     * @throws MissingResourceException if valification failed
     */
    void validateRuleData(String ruleFile, ByteBuffer bb) {
        /* Verify the magic number. */
        for (int i = 0; i < LABEL_LENGTH; i++) {
            if (bb.get() != LABEL[i]) {
                throw new MissingResourceException("Wrong magic number",
                                                   ruleFile, "");
            }
        }

        /* Verify the version number. */
        byte version = bb.get();
        if (version != supportedVersion) {
            throw new MissingResourceException("Unsupported version(" + version + ")",
                                               ruleFile, "");
        }

        // Check the length of the rest of data
        int len = bb.getInt();
        if (bb.position() + len != bb.limit()) {
            throw new MissingResourceException("Wrong data length",
                                               ruleFile, "");
        }
    }

    byte[] getAdditionalData() {
        return additionalData;
    }

    void setAdditionalData(byte[] b) {
        additionalData = b;
    }

    //=======================================================================
    // boilerplate
    //=======================================================================
    /**
     * Clones this iterator.
     * @return A newly-constructed RuleBasedBreakIterator with the same
     * behavior as this one.
     */
    @Override
    public Object clone() {
        RuleBasedBreakIterator result = (RuleBasedBreakIterator) super.clone();
        if (text != null) {
            result.text = (CharacterIterator) text.clone();
        }
        return result;
    }

    /**
     * Returns true if both BreakIterators are of the same class, have the same
     * rules, and iterate over the same text.
     */
    @Override
    public boolean equals(Object that) {
        try {
            if (that == null) {
                return false;
            }

            RuleBasedBreakIterator other = (RuleBasedBreakIterator) that;
            if (checksum != other.checksum) {
                return false;
            }
            if (text == null) {
                return other.text == null;
            } else {
                return text.equals(other.text);
            }
        }
        catch(ClassCastException e) {
            return false;
        }
    }

    /**
     * Returns text
     */
    @Override
    public String toString() {
        return "[checksum=0x" + Long.toHexString(checksum) + ']';
    }

    /**
     * Compute a hashcode for this BreakIterator
     * @return A hash code
     */
    @Override
    public int hashCode() {
        return (int)checksum;
    }

    //=======================================================================
    // BreakIterator overrides
    //=======================================================================

    /**
     * Sets the current iteration position to the beginning of the text.
     * (i.e., the CharacterIterator's starting offset).
     * @return The offset of the beginning of the text.
     */
    @Override
    public int first() {
        CharacterIterator t = getText();

        t.first();
        return t.getIndex();
    }

    /**
     * Sets the current iteration position to the end of the text.
     * (i.e., the CharacterIterator's ending offset).
     * @return The text's past-the-end offset.
     */
    @Override
    public int last() {
        CharacterIterator t = getText();

        // I'm not sure why, but t.last() returns the offset of the last character,
        // rather than the past-the-end offset
        t.setIndex(t.getEndIndex());
        return t.getIndex();
    }

    /**
     * Advances the iterator either forward or backward the specified number of steps.
     * Negative values move backward, and positive values move forward.  This is
     * equivalent to repeatedly calling next() or previous().
     * @param n The number of steps to move.  The sign indicates the direction
     * (negative is backwards, and positive is forwards).
     * @return The character offset of the boundary position n boundaries away from
     * the current one.
     */
    @Override
    public int next(int n) {
        int result = current();
        while (n > 0) {
            result = handleNext();
            --n;
        }
        while (n < 0) {
            result = previous();
            ++n;
        }
        return result;
    }

    /**
     * Advances the iterator to the next boundary position.
     * @return The position of the first boundary after this one.
     */
    @Override
    public int next() {
        return handleNext();
    }

    private int cachedLastKnownBreak = BreakIterator.DONE;

    /**
     * Advances the iterator backwards, to the last boundary preceding this one.
     * @return The position of the last boundary position preceding this one.
     */
    @Override
    public int previous() {
        // if we're already sitting at the beginning of the text, return DONE
        CharacterIterator text = getText();
        if (current() == text.getBeginIndex()) {
            return BreakIterator.DONE;
        }

        // set things up.  handlePrevious() will back us up to some valid
        // break position before the current position (we back our internal
        // iterator up one step to prevent handlePrevious() from returning
        // the current position), but not necessarily the last one before
        // where we started
        int start = current();
        int lastResult = cachedLastKnownBreak;
        if (lastResult >= start || lastResult <= BreakIterator.DONE) {
            getPrevious();
            lastResult = handlePrevious();
        } else {
            //it might be better to check if handlePrevious() give us closer
            //safe value but handlePrevious() is slow too
            //So, this has to be done carefully
            text.setIndex(lastResult);
        }
        int result = lastResult;

        // iterate forward from the known break position until we pass our
        // starting point.  The last break position before the starting
        // point is our return value
        while (result != BreakIterator.DONE && result < start) {
            lastResult = result;
            result = handleNext();
        }

        // set the current iteration position to be the last break position
        // before where we started, and then return that value
        text.setIndex(lastResult);
        cachedLastKnownBreak = lastResult;
        return lastResult;
    }

    /**
     * Returns previous character
     */
    private int getPrevious() {
        char c2 = text.previous();
        if (Character.isLowSurrogate(c2) &&
            text.getIndex() > text.getBeginIndex()) {
            char c1 = text.previous();
            if (Character.isHighSurrogate(c1)) {
                return Character.toCodePoint(c1, c2);
            } else {
                text.next();
            }
        }
        return (int)c2;
    }

    /**
     * Returns current character
     */
    int getCurrent() {
        char c1 = text.current();
        if (Character.isHighSurrogate(c1) &&
            text.getIndex() < text.getEndIndex()) {
            char c2 = text.next();
            text.previous();
            if (Character.isLowSurrogate(c2)) {
                return Character.toCodePoint(c1, c2);
            }
        }
        return (int)c1;
    }

    /**
     * Returns the count of next character.
     */
    private int getCurrentCodePointCount() {
        char c1 = text.current();
        if (Character.isHighSurrogate(c1) &&
            text.getIndex() < text.getEndIndex()) {
            char c2 = text.next();
            text.previous();
            if (Character.isLowSurrogate(c2)) {
                return 2;
            }
        }
        return 1;
    }

    /**
     * Returns next character
     */
    int getNext() {
        int index = text.getIndex();
        int endIndex = text.getEndIndex();
        if (index == endIndex ||
            (index += getCurrentCodePointCount()) >= endIndex) {
            return CharacterIterator.DONE;
        }
        text.setIndex(index);
        return getCurrent();
    }

    /**
     * Returns the position of next character.
     */
    private int getNextIndex() {
        int index = text.getIndex() + getCurrentCodePointCount();
        int endIndex = text.getEndIndex();
        if (index > endIndex) {
            return endIndex;
        } else {
            return index;
        }
    }

    /**
     * Throw IllegalArgumentException unless begin <= offset < end.
     */
    protected static final void checkOffset(int offset, CharacterIterator text) {
        if (offset < text.getBeginIndex() || offset > text.getEndIndex()) {
            throw new IllegalArgumentException("offset out of bounds");
        }
    }

    /**
     * Sets the iterator to refer to the first boundary position following
     * the specified position.
     * @offset The position from which to begin searching for a break position.
     * @return The position of the first break after the current position.
     */
    @Override
    public int following(int offset) {

        CharacterIterator text = getText();
        checkOffset(offset, text);

        // Set our internal iteration position (temporarily)
        // to the position passed in.  If this is the _beginning_ position,
        // then we can just use next() to get our return value
        text.setIndex(offset);
        if (offset == text.getBeginIndex()) {
            cachedLastKnownBreak = handleNext();
            return cachedLastKnownBreak;
        }

        // otherwise, we have to sync up first.  Use handlePrevious() to back
        // us up to a known break position before the specified position (if
        // we can determine that the specified position is a break position,
        // we don't back up at all).  This may or may not be the last break
        // position at or before our starting position.  Advance forward
        // from here until we've passed the starting position.  The position
        // we stop on will be the first break position after the specified one.
        int result = cachedLastKnownBreak;
        if (result >= offset || result <= BreakIterator.DONE) {
            result = handlePrevious();
        } else {
            //it might be better to check if handlePrevious() give us closer
            //safe value but handlePrevious() is slow too
            //So, this has to be done carefully
            text.setIndex(result);
        }
        while (result != BreakIterator.DONE && result <= offset) {
            result = handleNext();
        }
        cachedLastKnownBreak = result;
        return result;
    }

    /**
     * Sets the iterator to refer to the last boundary position before the
     * specified position.
     * @offset The position to begin searching for a break from.
     * @return The position of the last boundary before the starting position.
     */
    @Override
    public int preceding(int offset) {
        // if we start by updating the current iteration position to the
        // position specified by the caller, we can just use previous()
        // to carry out this operation
        CharacterIterator text = getText();
        checkOffset(offset, text);
        text.setIndex(offset);
        return previous();
    }

    /**
     * Returns true if the specified position is a boundary position.  As a side
     * effect, leaves the iterator pointing to the first boundary position at
     * or after "offset".
     * @param offset the offset to check.
     * @return True if "offset" is a boundary position.
     */
    @Override
    public boolean isBoundary(int offset) {
        CharacterIterator text = getText();
        checkOffset(offset, text);
        if (offset == text.getBeginIndex()) {
            return true;
        }

        // to check whether this is a boundary, we can use following() on the
        // position before the specified one and return true if the position we
        // get back is the one the user specified
        else {
            return following(offset - 1) == offset;
        }
    }

    /**
     * Returns the current iteration position.
     * @return The current iteration position.
     */
    @Override
    public int current() {
        return getText().getIndex();
    }

    /**
     * Return a CharacterIterator over the text being analyzed.  This version
     * of this method returns the actual CharacterIterator we're using internally.
     * Changing the state of this iterator can have undefined consequences.  If
     * you need to change it, clone it first.
     * @return An iterator over the text being analyzed.
     */
    @Override
    public CharacterIterator getText() {
        // The iterator is initialized pointing to no text at all, so if this
        // function is called while we're in that state, we have to fudge an
        // iterator to return.
        if (text == null) {
            text = new StringCharacterIterator("");
        }
        return text;
    }

    /**
     * Set the iterator to analyze a new piece of text.  This function resets
     * the current iteration position to the beginning of the text.
     * @param newText An iterator over the text to analyze.
     */
    @Override
    public void setText(CharacterIterator newText) {
        // Test iterator to see if we need to wrap it in a SafeCharIterator.
        // The correct behavior for CharacterIterators is to allow the
        // position to be set to the endpoint of the iterator.  Many
        // CharacterIterators do not uphold this, so this is a workaround
        // to permit them to use this class.
        int end = newText.getEndIndex();
        boolean goodIterator;
        try {
            newText.setIndex(end);  // some buggy iterators throw an exception here
            goodIterator = newText.getIndex() == end;
        }
        catch(IllegalArgumentException e) {
            goodIterator = false;
        }

        if (goodIterator) {
            text = newText;
        }
        else {
            text = new SafeCharIterator(newText);
        }
        text.first();

        cachedLastKnownBreak = BreakIterator.DONE;
    }


    //=======================================================================
    // implementation
    //=======================================================================

    /**
     * This method is the actual implementation of the next() method.  All iteration
     * vectors through here.  This method initializes the state machine to state 1
     * and advances through the text character by character until we reach the end
     * of the text or the state machine transitions to state 0.  We update our return
     * value every time the state machine passes through a possible end state.
     */
    protected int handleNext() {
        // if we're already at the end of the text, return DONE.
        CharacterIterator text = getText();
        if (text.getIndex() == text.getEndIndex()) {
            return BreakIterator.DONE;
        }

        // no matter what, we always advance at least one character forward
        int result = getNextIndex();
        int lookaheadResult = 0;

        // begin in state 1
        int state = START_STATE;
        int category;
        int c = getCurrent();

        // loop until we reach the end of the text or transition to state 0
        while (c != CharacterIterator.DONE && state != STOP_STATE) {

            // look up the current character's character category (which tells us
            // which column in the state table to look at)
            category = lookupCategory(c);

            // if the character isn't an ignore character, look up a state
            // transition in the state table
            if (category != IGNORE) {
                state = lookupState(state, category);
            }

            // if the state we've just transitioned to is a lookahead state,
            // (but not also an end state), save its position.  If it's
            // both a lookahead state and an end state, update the break position
            // to the last saved lookup-state position
            if (lookaheadStates[state]) {
                if (endStates[state]) {
                    result = lookaheadResult;
                }
                else {
                    lookaheadResult = getNextIndex();
                }
            }

            // otherwise, if the state we've just transitioned to is an accepting
            // state, update the break position to be the current iteration position
            else {
                if (endStates[state]) {
                    result = getNextIndex();
                }
            }

            c = getNext();
        }

        // if we've run off the end of the text, and the very last character took us into
        // a lookahead state, advance the break position to the lookahead position
        // (the theory here is that if there are no characters at all after the lookahead
        // position, that always matches the lookahead criteria)
        if (c == CharacterIterator.DONE && lookaheadResult == text.getEndIndex()) {
            result = lookaheadResult;
        }

        text.setIndex(result);
        return result;
    }

    /**
     * This method backs the iterator back up to a "safe position" in the text.
     * This is a position that we know, without any context, must be a break position.
     * The various calling methods then iterate forward from this safe position to
     * the appropriate position to return.  (For more information, see the description
     * of buildBackwardsStateTable() in RuleBasedBreakIterator.Builder.)
     */
    protected int handlePrevious() {
        CharacterIterator text = getText();
        int state = START_STATE;
        int category = 0;
        int lastCategory = 0;
        int c = getCurrent();

        // loop until we reach the beginning of the text or transition to state 0
        while (c != CharacterIterator.DONE && state != STOP_STATE) {

            // save the last character's category and look up the current
            // character's category
            lastCategory = category;
            category = lookupCategory(c);

            // if the current character isn't an ignore character, look up a
            // state transition in the backwards state table
            if (category != IGNORE) {
                state = lookupBackwardState(state, category);
            }

            // then advance one character backwards
            c = getPrevious();
        }

        // if we didn't march off the beginning of the text, we're either one or two
        // positions away from the real break position.  (One because of the call to
        // previous() at the end of the loop above, and another because the character
        // that takes us into the stop state will always be the character BEFORE
        // the break position.)
        if (c != CharacterIterator.DONE) {
            if (lastCategory != IGNORE) {
                getNext();
                getNext();
            }
            else {
                getNext();
            }
        }
        return text.getIndex();
    }

    /**
     * Looks up a character's category (i.e., its category for breaking purposes,
     * not its Unicode category)
     */
    protected int lookupCategory(int c) {
        if (c < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            return charCategoryTable.elementAt((char)c);
        } else {
            return supplementaryCharCategoryTable.getValue(c);
        }
    }

    /**
     * Given a current state and a character category, looks up the
     * next state to transition to in the state table.
     */
    protected int lookupState(int state, int category) {
        return stateTable[state * numCategories + category];
    }

    /**
     * Given a current state and a character category, looks up the
     * next state to transition to in the backwards state table.
     */
    protected int lookupBackwardState(int state, int category) {
        return backwardsStateTable[state * numCategories + category];
    }

    /*
     * This class exists to work around a bug in incorrect implementations
     * of CharacterIterator, which incorrectly handle setIndex(endIndex).
     * This iterator relies only on base.setIndex(n) where n is less than
     * endIndex.
     *
     * One caveat:  if the base iterator's begin and end indices change
     * the change will not be reflected by this wrapper.  Does that matter?
     */
    // TODO: Review this class to see if it's still required.
    private static final class SafeCharIterator implements CharacterIterator,
                                                           Cloneable {

        private CharacterIterator base;
        private int rangeStart;
        private int rangeLimit;
        private int currentIndex;

        SafeCharIterator(CharacterIterator base) {
            this.base = base;
            this.rangeStart = base.getBeginIndex();
            this.rangeLimit = base.getEndIndex();
            this.currentIndex = base.getIndex();
        }

        @Override
        public char first() {
            return setIndex(rangeStart);
        }

        @Override
        public char last() {
            return setIndex(rangeLimit - 1);
        }

        @Override
        public char current() {
            if (currentIndex < rangeStart || currentIndex >= rangeLimit) {
                return DONE;
            }
            else {
                return base.setIndex(currentIndex);
            }
        }

        @Override
        public char next() {

            currentIndex++;
            if (currentIndex >= rangeLimit) {
                currentIndex = rangeLimit;
                return DONE;
            }
            else {
                return base.setIndex(currentIndex);
            }
        }

        @Override
        public char previous() {

            currentIndex--;
            if (currentIndex < rangeStart) {
                currentIndex = rangeStart;
                return DONE;
            }
            else {
                return base.setIndex(currentIndex);
            }
        }

        @Override
        public char setIndex(int i) {

            if (i < rangeStart || i > rangeLimit) {
                throw new IllegalArgumentException("Invalid position");
            }
            currentIndex = i;
            return current();
        }

        @Override
        public int getBeginIndex() {
            return rangeStart;
        }

        @Override
        public int getEndIndex() {
            return rangeLimit;
        }

        @Override
        public int getIndex() {
            return currentIndex;
        }

        @Override
        public Object clone() {

            SafeCharIterator copy = null;
            try {
                copy = (SafeCharIterator) super.clone();
            }
            catch(CloneNotSupportedException e) {
                throw new Error("Clone not supported: " + e);
            }

            CharacterIterator copyOfBase = (CharacterIterator) base.clone();
            copy.base = copyOfBase;
            return copy;
        }
    }
}
