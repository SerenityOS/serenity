/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *******************************************************************************
 * Copyright (C) 1996-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package jdk.internal.icu.text;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.TreeSet;

import jdk.internal.icu.impl.BMPSet;
import jdk.internal.icu.impl.UCharacterProperty;
import jdk.internal.icu.impl.UnicodeSetStringSpan;
import jdk.internal.icu.impl.Utility;
import jdk.internal.icu.lang.UCharacter;
import jdk.internal.icu.util.OutputInt;
import jdk.internal.icu.util.VersionInfo;

/**
 * A mutable set of Unicode characters and multicharacter strings.
 * Objects of this class represent <em>character classes</em> used
 * in regular expressions. A character specifies a subset of Unicode
 * code points.  Legal code points are U+0000 to U+10FFFF, inclusive.
 *
 * Note: method freeze() will not only make the set immutable, but
 * also makes important methods much higher performance:
 * contains(c), containsNone(...), span(...), spanBack(...) etc.
 * After the object is frozen, any subsequent call that wants to change
 * the object will throw UnsupportedOperationException.
 *
 * <p>The UnicodeSet class is not designed to be subclassed.
 *
 * <p><code>UnicodeSet</code> supports two APIs. The first is the
 * <em>operand</em> API that allows the caller to modify the value of
 * a <code>UnicodeSet</code> object. It conforms to Java 2's
 * <code>java.util.Set</code> interface, although
 * <code>UnicodeSet</code> does not actually implement that
 * interface. All methods of <code>Set</code> are supported, with the
 * modification that they take a character range or single character
 * instead of an <code>Object</code>, and they take a
 * <code>UnicodeSet</code> instead of a <code>Collection</code>.  The
 * operand API may be thought of in terms of boolean logic: a boolean
 * OR is implemented by <code>add</code>, a boolean AND is implemented
 * by <code>retain</code>, a boolean XOR is implemented by
 * <code>complement</code> taking an argument, and a boolean NOT is
 * implemented by <code>complement</code> with no argument.  In terms
 * of traditional set theory function names, <code>add</code> is a
 * union, <code>retain</code> is an intersection, <code>remove</code>
 * is an asymmetric difference, and <code>complement</code> with no
 * argument is a set complement with respect to the superset range
 * <code>MIN_VALUE-MAX_VALUE</code>
 *
 * <p>The second API is the
 * <code>applyPattern()</code>/<code>toPattern()</code> API from the
 * <code>java.text.Format</code>-derived classes.  Unlike the
 * methods that add characters, add categories, and control the logic
 * of the set, the method <code>applyPattern()</code> sets all
 * attributes of a <code>UnicodeSet</code> at once, based on a
 * string pattern.
 *
 * <p><b>Pattern syntax</b></p>
 *
 * Patterns are accepted by the constructors and the
 * <code>applyPattern()</code> methods and returned by the
 * <code>toPattern()</code> method.  These patterns follow a syntax
 * similar to that employed by version 8 regular expression character
 * classes.  Here are some simple examples:
 *
 * <blockquote>
 *   <table>
 *     <tr align="top">
 *       <td nowrap valign="top" align="left"><code>[]</code></td>
 *       <td valign="top">No characters</td>
 *     </tr><tr align="top">
 *       <td nowrap valign="top" align="left"><code>[a]</code></td>
 *       <td valign="top">The character 'a'</td>
 *     </tr><tr align="top">
 *       <td nowrap valign="top" align="left"><code>[ae]</code></td>
 *       <td valign="top">The characters 'a' and 'e'</td>
 *     </tr>
 *     <tr>
 *       <td nowrap valign="top" align="left"><code>[a-e]</code></td>
 *       <td valign="top">The characters 'a' through 'e' inclusive, in Unicode code
 *       point order</td>
 *     </tr>
 *     <tr>
 *       <td nowrap valign="top" align="left"><code>[\\u4E01]</code></td>
 *       <td valign="top">The character U+4E01</td>
 *     </tr>
 *     <tr>
 *       <td nowrap valign="top" align="left"><code>[a{ab}{ac}]</code></td>
 *       <td valign="top">The character 'a' and the multicharacter strings "ab" and
 *       "ac"</td>
 *     </tr>
 *     <tr>
 *       <td nowrap valign="top" align="left"><code>[\p{Lu}]</code></td>
 *       <td valign="top">All characters in the general category Uppercase Letter</td>
 *     </tr>
 *   </table>
 * </blockquote>
 *
 * Any character may be preceded by a backslash in order to remove any special
 * meaning.  White space characters, as defined by the Unicode Pattern_White_Space property, are
 * ignored, unless they are escaped.
 *
 * <p>Property patterns specify a set of characters having a certain
 * property as defined by the Unicode standard.  Both the POSIX-like
 * "[:Lu:]" and the Perl-like syntax "\p{Lu}" are recognized.  For a
 * complete list of supported property patterns, see the User's Guide
 * for UnicodeSet at
 * <a href="http://www.icu-project.org/userguide/unicodeSet.html">
 * http://www.icu-project.org/userguide/unicodeSet.html</a>.
 * Actual determination of property data is defined by the underlying
 * Unicode database as implemented by UCharacter.
 *
 * <p>Patterns specify individual characters, ranges of characters, and
 * Unicode property sets.  When elements are concatenated, they
 * specify their union.  To complement a set, place a '^' immediately
 * after the opening '['.  Property patterns are inverted by modifying
 * their delimiters; "[:^foo]" and "\P{foo}".  In any other location,
 * '^' has no special meaning.
 *
 * <p>Ranges are indicated by placing two a '-' between two
 * characters, as in "a-z".  This specifies the range of all
 * characters from the left to the right, in Unicode order.  If the
 * left character is greater than or equal to the
 * right character it is a syntax error.  If a '-' occurs as the first
 * character after the opening '[' or '[^', or if it occurs as the
 * last character before the closing ']', then it is taken as a
 * literal.  Thus "[a\\-b]", "[-ab]", and "[ab-]" all indicate the same
 * set of three characters, 'a', 'b', and '-'.
 *
 * <p>Sets may be intersected using the {@literal '&'} operator or the asymmetric
 * set difference may be taken using the '-' operator, for example,
 * "{@code [[:L:]&[\\u0000-\\u0FFF]]}" indicates the set of all Unicode letters
 * with values less than 4096.  Operators ({@literal '&'} and '|') have equal
 * precedence and bind left-to-right.  Thus
 * "[[:L:]-[a-z]-[\\u0100-\\u01FF]]" is equivalent to
 * "[[[:L:]-[a-z]]-[\\u0100-\\u01FF]]".  This only really matters for
 * difference; intersection is commutative.
 *
 * <table>
 * <tr valign=top><td nowrap><code>[a]</code><td>The set containing 'a'
 * <tr valign=top><td nowrap><code>[a-z]</code><td>The set containing 'a'
 * through 'z' and all letters in between, in Unicode order
 * <tr valign=top><td nowrap><code>[^a-z]</code><td>The set containing
 * all characters but 'a' through 'z',
 * that is, U+0000 through 'a'-1 and 'z'+1 through U+10FFFF
 * <tr valign=top><td nowrap><code>[[<em>pat1</em>][<em>pat2</em>]]</code>
 * <td>The union of sets specified by <em>pat1</em> and <em>pat2</em>
 * <tr valign=top><td nowrap><code>[[<em>pat1</em>]&amp;[<em>pat2</em>]]</code>
 * <td>The intersection of sets specified by <em>pat1</em> and <em>pat2</em>
 * <tr valign=top><td nowrap><code>[[<em>pat1</em>]-[<em>pat2</em>]]</code>
 * <td>The asymmetric difference of sets specified by <em>pat1</em> and
 * <em>pat2</em>
 * <tr valign=top><td nowrap><code>[:Lu:] or \p{Lu}</code>
 * <td>The set of characters having the specified
 * Unicode property; in
 * this case, Unicode uppercase letters
 * <tr valign=top><td nowrap><code>[:^Lu:] or \P{Lu}</code>
 * <td>The set of characters <em>not</em> having the given
 * Unicode property
 * </table>
 *
 * <p><b>Warning</b>: you cannot add an empty string ("") to a UnicodeSet.</p>
 *
 * <p><b>Formal syntax</b></p>
 *
 * <blockquote>
 *   <table>
 *     <tr align="top">
 *       <td nowrap valign="top" align="right"><code>pattern :=&nbsp; </code></td>
 *       <td valign="top"><code>('[' '^'? item* ']') |
 *       property</code></td>
 *     </tr>
 *     <tr align="top">
 *       <td nowrap valign="top" align="right"><code>item :=&nbsp; </code></td>
 *       <td valign="top"><code>char | (char '-' char) | pattern-expr<br>
 *       </code></td>
 *     </tr>
 *     <tr align="top">
 *       <td nowrap valign="top" align="right"><code>pattern-expr :=&nbsp; </code></td>
 *       <td valign="top"><code>pattern | pattern-expr pattern |
 *       pattern-expr op pattern<br>
 *       </code></td>
 *     </tr>
 *     <tr align="top">
 *       <td nowrap valign="top" align="right"><code>op :=&nbsp; </code></td>
 *       <td valign="top"><code>'&amp;' | '-'<br>
 *       </code></td>
 *     </tr>
 *     <tr align="top">
 *       <td nowrap valign="top" align="right"><code>special :=&nbsp; </code></td>
 *       <td valign="top"><code>'[' | ']' | '-'<br>
 *       </code></td>
 *     </tr>
 *     <tr align="top">
 *       <td nowrap valign="top" align="right"><code>char :=&nbsp; </code></td>
 *       <td valign="top"><em>any character that is not</em><code> special<br>
 *       | ('\\' </code><em>any character</em><code>)<br>
 *       | ('&#92;u' hex hex hex hex)<br>
 *       </code></td>
 *     </tr>
 *     <tr align="top">
 *       <td nowrap valign="top" align="right"><code>hex :=&nbsp; </code></td>
 *       <td valign="top"><em>any character for which
 *       </em><code>Character.digit(c, 16)</code><em>
 *       returns a non-negative result</em></td>
 *     </tr>
 *     <tr>
 *       <td nowrap valign="top" align="right"><code>property :=&nbsp; </code></td>
 *       <td valign="top"><em>a Unicode property set pattern</em></td>
 *     </tr>
 *   </table>
 *   <br>
 *   <table border="1">
 *     <tr>
 *       <td>Legend: <table>
 *         <tr>
 *           <td nowrap valign="top"><code>a := b</code></td>
 *           <td width="20" valign="top">&nbsp; </td>
 *           <td valign="top"><code>a</code> may be replaced by <code>b</code> </td>
 *         </tr>
 *         <tr>
 *           <td nowrap valign="top"><code>a?</code></td>
 *           <td valign="top"></td>
 *           <td valign="top">zero or one instance of <code>a</code><br>
 *           </td>
 *         </tr>
 *         <tr>
 *           <td nowrap valign="top"><code>a*</code></td>
 *           <td valign="top"></td>
 *           <td valign="top">one or more instances of <code>a</code><br>
 *           </td>
 *         </tr>
 *         <tr>
 *           <td nowrap valign="top"><code>a | b</code></td>
 *           <td valign="top"></td>
 *           <td valign="top">either <code>a</code> or <code>b</code><br>
 *           </td>
 *         </tr>
 *         <tr>
 *           <td nowrap valign="top"><code>'a'</code></td>
 *           <td valign="top"></td>
 *           <td valign="top">the literal string between the quotes </td>
 *         </tr>
 *       </table>
 *       </td>
 *     </tr>
 *   </table>
 * </blockquote>
 * <p>To iterate over contents of UnicodeSet, the following are available:
 * <ul><li>{@link #ranges()} to iterate through the ranges</li>
 * <li>{@link #strings()} to iterate through the strings</li>
 * <li>{@link #iterator()} to iterate through the entire contents in a single loop.
 * That method is, however, not particularly efficient, since it "boxes" each code point into a String.
 * </ul>
 * All of the above can be used in <b>for</b> loops.
 * The {@link com.ibm.icu.text.UnicodeSetIterator UnicodeSetIterator} can also be used, but not in <b>for</b> loops.
 * <p>To replace, count elements, or delete spans, see {@link com.ibm.icu.text.UnicodeSetSpanner UnicodeSetSpanner}.
 *
 * @author Alan Liu
 * @stable ICU 2.0
 */
public class UnicodeSet {

    private static final int LOW = 0x000000; // LOW <= all valid values. ZERO for codepoints
    private static final int HIGH = 0x110000; // HIGH > all valid values. 10000 for code units.
    // 110000 for codepoints

    /**
     * Minimum value that can be stored in a UnicodeSet.
     * @stable ICU 2.0
     */
    public static final int MIN_VALUE = LOW;

    /**
     * Maximum value that can be stored in a UnicodeSet.
     * @stable ICU 2.0
     */
    public static final int MAX_VALUE = HIGH - 1;

    private int len;      // length used; list may be longer to minimize reallocs
    private int[] list;   // MUST be terminated with HIGH
    private int[] rangeList; // internal buffer
    private int[] buffer; // internal buffer

    // NOTE: normally the field should be of type SortedSet; but that is missing a public clone!!
    // is not private so that UnicodeSetIterator can get access
    TreeSet<String> strings = new TreeSet<String>();

    /**
     * The pattern representation of this set.  This may not be the
     * most economical pattern.  It is the pattern supplied to
     * applyPattern(), with variables substituted and whitespace
     * removed.  For sets constructed without applyPattern(), or
     * modified using the non-pattern API, this string will be null,
     * indicating that toPattern() must generate a pattern
     * representation from the inversion list.
     */

    private static final int START_EXTRA = 16;         // initial storage. Must be >= 0
    private static final int GROW_EXTRA = START_EXTRA; // extra amount for growth. Must be >= 0

    private static UnicodeSet INCLUSION = null;

    private volatile BMPSet bmpSet; // The set is frozen if bmpSet or stringSpan is not null.
    private volatile UnicodeSetStringSpan stringSpan;

    //----------------------------------------------------------------
    // Public API
    //----------------------------------------------------------------

    /**
     * Constructs an empty set.
     * @stable ICU 2.0
     */
    private UnicodeSet() {
        list = new int[1 + START_EXTRA];
        list[len++] = HIGH;
    }

    /**
     * Constructs a copy of an existing set.
     * @stable ICU 2.0
     */
    private UnicodeSet(UnicodeSet other) {
        set(other);
    }

    /**
     * Constructs a set containing the given range. If <code>end >
     * start</code> then an empty set is created.
     *
     * @param start first character, inclusive, of range
     * @param end last character, inclusive, of range
     * @stable ICU 2.0
     */
    public UnicodeSet(int start, int end) {
        this();
        complement(start, end);
    }

    /**
     * Constructs a set from the given pattern.  See the class description
     * for the syntax of the pattern language.  Whitespace is ignored.
     * @param pattern a string specifying what characters are in the set
     * @exception java.lang.IllegalArgumentException if the pattern contains
     * a syntax error.
     * @stable ICU 2.0
     */
    public UnicodeSet(String pattern) {
        this();
        applyPattern(pattern, null);
    }

    /**
     * Make this object represent the same set as <code>other</code>.
     * @param other a <code>UnicodeSet</code> whose value will be
     * copied to this object
     * @stable ICU 2.0
     */
    public UnicodeSet set(UnicodeSet other) {
        checkFrozen();
        list = other.list.clone();
        len = other.len;
        strings = new TreeSet<String>(other.strings);
        return this;
    }

    /**
     * Returns the number of elements in this set (its cardinality)
     * Note than the elements of a set may include both individual
     * codepoints and strings.
     *
     * @return the number of elements in this set (its cardinality).
     * @stable ICU 2.0
     */
    public int size() {
        int n = 0;
        int count = getRangeCount();
        for (int i = 0; i < count; ++i) {
            n += getRangeEnd(i) - getRangeStart(i) + 1;
        }
        return n + strings.size();
    }

    // for internal use, after checkFrozen has been called
    private UnicodeSet add_unchecked(int start, int end) {
        if (start < MIN_VALUE || start > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(start, 6));
        }
        if (end < MIN_VALUE || end > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(end, 6));
        }
        if (start < end) {
            add(range(start, end), 2, 0);
        } else if (start == end) {
            add(start);
        }
        return this;
    }

    /**
     * Adds the specified character to this set if it is not already
     * present.  If this set already contains the specified character,
     * the call leaves this set unchanged.
     * @stable ICU 2.0
     */
    public final UnicodeSet add(int c) {
        checkFrozen();
        return add_unchecked(c);
    }

    // for internal use only, after checkFrozen has been called
    private final UnicodeSet add_unchecked(int c) {
        if (c < MIN_VALUE || c > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(c, 6));
        }

        // find smallest i such that c < list[i]
        // if odd, then it is IN the set
        // if even, then it is OUT of the set
        int i = findCodePoint(c);

        // already in set?
        if ((i & 1) != 0) return this;

        // HIGH is 0x110000
        // assert(list[len-1] == HIGH);

        // empty = [HIGH]
        // [start_0, limit_0, start_1, limit_1, HIGH]

        // [..., start_k-1, limit_k-1, start_k, limit_k, ..., HIGH]
        //                             ^
        //                             list[i]

        // i == 0 means c is before the first range

        if (c == list[i]-1) {
            // c is before start of next range
            list[i] = c;
            // if we touched the HIGH mark, then add a new one
            if (c == MAX_VALUE) {
                ensureCapacity(len+1);
                list[len++] = HIGH;
            }
            if (i > 0 && c == list[i-1]) {
                // collapse adjacent ranges

                // [..., start_k-1, c, c, limit_k, ..., HIGH]
                //                     ^
                //                     list[i]
                System.arraycopy(list, i+1, list, i-1, len-i-1);
                len -= 2;
            }
        }

        else if (i > 0 && c == list[i-1]) {
            // c is after end of prior range
            list[i-1]++;
            // no need to chcek for collapse here
        }

        else {
            // At this point we know the new char is not adjacent to
            // any existing ranges, and it is not 10FFFF.


            // [..., start_k-1, limit_k-1, start_k, limit_k, ..., HIGH]
            //                             ^
            //                             list[i]

            // [..., start_k-1, limit_k-1, c, c+1, start_k, limit_k, ..., HIGH]
            //                             ^
            //                             list[i]

            // Don't use ensureCapacity() to save on copying.
            // NOTE: This has no measurable impact on performance,
            // but it might help in some usage patterns.
            if (len+2 > list.length) {
                int[] temp = new int[len + 2 + GROW_EXTRA];
                if (i != 0) System.arraycopy(list, 0, temp, 0, i);
                System.arraycopy(list, i, temp, i+2, len-i);
                list = temp;
            } else {
                System.arraycopy(list, i, list, i+2, len-i);
            }

            list[i] = c;
            list[i+1] = c+1;
            len += 2;
        }

        return this;
    }

    /**
     * Adds the specified multicharacter to this set if it is not already
     * present.  If this set already contains the multicharacter,
     * the call leaves this set unchanged.
     * Thus {@code "ch" => {"ch"}}
     * <br><b>Warning: you cannot add an empty string ("") to a UnicodeSet.</b>
     * @param s the source string
     * @return this object, for chaining
     * @stable ICU 2.0
     */
    public final UnicodeSet add(CharSequence s) {
        checkFrozen();
        int cp = getSingleCP(s);
        if (cp < 0) {
            strings.add(s.toString());
        } else {
            add_unchecked(cp, cp);
        }
        return this;
    }

    /**
     * Utility for getting code point from single code point CharSequence.
     * See the public UTF16.getSingleCodePoint()
     * @return a code point IF the string consists of a single one.
     * otherwise returns -1.
     * @param s to test
     */
    private static int getSingleCP(CharSequence s) {
        if (s.length() < 1) {
            throw new IllegalArgumentException("Can't use zero-length strings in UnicodeSet");
        }
        if (s.length() > 2) return -1;
        if (s.length() == 1) return s.charAt(0);

        // at this point, len = 2
        int cp = UTF16.charAt(s, 0);
        if (cp > 0xFFFF) { // is surrogate pair
            return cp;
        }
        return -1;
    }

    /**
     * Complements the specified range in this set.  Any character in
     * the range will be removed if it is in this set, or will be
     * added if it is not in this set.  If {@code end > start}
     * then an empty range is complemented, leaving the set unchanged.
     *
     * @param start first character, inclusive, of range to be removed
     * from this set.
     * @param end last character, inclusive, of range to be removed
     * from this set.
     * @stable ICU 2.0
     */
    public UnicodeSet complement(int start, int end) {
        checkFrozen();
        if (start < MIN_VALUE || start > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(start, 6));
        }
        if (end < MIN_VALUE || end > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(end, 6));
        }
        if (start <= end) {
            xor(range(start, end), 2, 0);
        }
        return this;
    }

    /**
     * Returns true if this set contains the given character.
     * @param c character to be checked for containment
     * @return true if the test condition is met
     * @stable ICU 2.0
     */
    public boolean contains(int c) {
        if (c < MIN_VALUE || c > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(c, 6));
        }
        if (bmpSet != null) {
            return bmpSet.contains(c);
        }
        if (stringSpan != null) {
            return stringSpan.contains(c);
        }

        /*
        // Set i to the index of the start item greater than ch
        // We know we will terminate without length test!
        int i = -1;
        while (true) {
            if (c < list[++i]) break;
        }
         */

        int i = findCodePoint(c);

        return ((i & 1) != 0); // return true if odd
    }

    /**
     * Returns the smallest value i such that c < list[i].  Caller
     * must ensure that c is a legal value or this method will enter
     * an infinite loop.  This method performs a binary search.
     * @param c a character in the range MIN_VALUE..MAX_VALUE
     * inclusive
     * @return the smallest integer i in the range 0..len-1,
     * inclusive, such that c < list[i]
     */
    private final int findCodePoint(int c) {
        /* Examples:
                                           findCodePoint(c)
           set              list[]         c=0 1 3 4 7 8
           ===              ==============   ===========
           []               [110000]         0 0 0 0 0 0
           [\u0000-\u0003]  [0, 4, 110000]   1 1 1 2 2 2
           [\u0004-\u0007]  [4, 8, 110000]   0 0 0 1 1 2
           [:all:]          [0, 110000]      1 1 1 1 1 1
         */

        // Return the smallest i such that c < list[i].  Assume
        // list[len - 1] == HIGH and that c is legal (0..HIGH-1).
        if (c < list[0]) return 0;
        // High runner test.  c is often after the last range, so an
        // initial check for this condition pays off.
        if (len >= 2 && c >= list[len-2]) return len-1;
        int lo = 0;
        int hi = len - 1;
        // invariant: c >= list[lo]
        // invariant: c < list[hi]
        for (;;) {
            int i = (lo + hi) >>> 1;
        if (i == lo) return hi;
            if (c < list[i]) {
                hi = i;
            } else {
                lo = i;
            }
        }
    }

    /**
     * Retains only the elements in this set that are contained in the
     * specified set.  In other words, removes from this set all of
     * its elements that are not contained in the specified set.  This
     * operation effectively modifies this set so that its value is
     * the <i>intersection</i> of the two sets.
     *
     * @param c set that defines which elements this set will retain.
     * @stable ICU 2.0
     */
    public UnicodeSet retainAll(UnicodeSet c) {
        checkFrozen();
        retain(c.list, c.len, 0);
        strings.retainAll(c.strings);
        return this;
    }

    /**
     * Removes all of the elements from this set.  This set will be
     * empty after this call returns.
     * @stable ICU 2.0
     */
    public UnicodeSet clear() {
        checkFrozen();
        list[0] = HIGH;
        len = 1;
        strings.clear();
        return this;
    }

    /**
     * Iteration method that returns the number of ranges contained in
     * this set.
     * @see #getRangeStart
     * @see #getRangeEnd
     * @stable ICU 2.0
     */
    public int getRangeCount() {
        return len/2;
    }

    /**
     * Iteration method that returns the first character in the
     * specified range of this set.
     * @exception ArrayIndexOutOfBoundsException if index is outside
     * the range <code>0..getRangeCount()-1</code>
     * @see #getRangeCount
     * @see #getRangeEnd
     * @stable ICU 2.0
     */
    public int getRangeStart(int index) {
        return list[index*2];
    }

    /**
     * Iteration method that returns the last character in the
     * specified range of this set.
     * @exception ArrayIndexOutOfBoundsException if index is outside
     * the range <code>0..getRangeCount()-1</code>
     * @see #getRangeStart
     * @see #getRangeEnd
     * @stable ICU 2.0
     */
    public int getRangeEnd(int index) {
        return (list[index*2 + 1] - 1);
    }

    //----------------------------------------------------------------
    // Implementation: Pattern parsing
    //----------------------------------------------------------------

    /**
     * Parses the given pattern, starting at the given position.  The character
     * at pattern.charAt(pos.getIndex()) must be '[', or the parse fails.
     * Parsing continues until the corresponding closing ']'.  If a syntax error
     * is encountered between the opening and closing brace, the parse fails.
     * Upon return from a successful parse, the ParsePosition is updated to
     * point to the character following the closing ']', and an inversion
     * list for the parsed pattern is returned.  This method
     * calls itself recursively to parse embedded subpatterns.
     *
     * @param pattern the string containing the pattern to be parsed.  The
     * portion of the string from pos.getIndex(), which must be a '[', to the
     * corresponding closing ']', is parsed.
     * @param pos upon entry, the position at which to being parsing.  The
     * character at pattern.charAt(pos.getIndex()) must be a '['.  Upon return
     * from a successful parse, pos.getIndex() is either the character after the
     * closing ']' of the parsed pattern, or pattern.length() if the closing ']'
     * is the last character of the pattern string.
     * @return an inversion list for the parsed substring
     * of <code>pattern</code>
     * @exception java.lang.IllegalArgumentException if the parse fails.
     */
    private UnicodeSet applyPattern(String pattern,
            ParsePosition pos) {
        if ("[:age=3.2:]".equals(pattern)) {
            checkFrozen();
            VersionInfo version = VersionInfo.getInstance("3.2");
            applyFilter(new VersionFilter(version), UCharacterProperty.SRC_PROPSVEC);
        } else {
            throw new IllegalStateException("UnicodeSet.applyPattern(unexpected pattern "
                          + pattern + ")");
        }

        return this;
    }

    //----------------------------------------------------------------
    // Implementation: Utility methods
    //----------------------------------------------------------------

    private void ensureCapacity(int newLen) {
        if (newLen <= list.length) return;
        int[] temp = new int[newLen + GROW_EXTRA];
        System.arraycopy(list, 0, temp, 0, len);
        list = temp;
    }

    private void ensureBufferCapacity(int newLen) {
        if (buffer != null && newLen <= buffer.length) return;
        buffer = new int[newLen + GROW_EXTRA];
    }

    /**
     * Assumes start <= end.
     */
    private int[] range(int start, int end) {
        if (rangeList == null) {
            rangeList = new int[] { start, end+1, HIGH };
        } else {
            rangeList[0] = start;
            rangeList[1] = end+1;
        }
        return rangeList;
    }

    //----------------------------------------------------------------
    // Implementation: Fundamental operations
    //----------------------------------------------------------------

    // polarity = 0, 3 is normal: x xor y
    // polarity = 1, 2: x xor ~y == x === y

    private UnicodeSet xor(int[] other, int otherLen, int polarity) {
        ensureBufferCapacity(len + otherLen);
        int i = 0, j = 0, k = 0;
        int a = list[i++];
        int b;
        if (polarity == 1 || polarity == 2) {
            b = LOW;
            if (other[j] == LOW) { // skip base if already LOW
                ++j;
                b = other[j];
            }
        } else {
            b = other[j++];
        }
        // simplest of all the routines
        // sort the values, discarding identicals!
        while (true) {
            if (a < b) {
                buffer[k++] = a;
                a = list[i++];
            } else if (b < a) {
                buffer[k++] = b;
                b = other[j++];
            } else if (a != HIGH) { // at this point, a == b
                // discard both values!
                a = list[i++];
                b = other[j++];
            } else { // DONE!
                buffer[k++] = HIGH;
                len = k;
                break;
            }
        }
        // swap list and buffer
        int[] temp = list;
        list = buffer;
        buffer = temp;
        return this;
    }

    // polarity = 0 is normal: x union y
    // polarity = 2: x union ~y
    // polarity = 1: ~x union y
    // polarity = 3: ~x union ~y

    private UnicodeSet add(int[] other, int otherLen, int polarity) {
        ensureBufferCapacity(len + otherLen);
        int i = 0, j = 0, k = 0;
        int a = list[i++];
        int b = other[j++];
        // change from xor is that we have to check overlapping pairs
        // polarity bit 1 means a is second, bit 2 means b is.
        main:
            while (true) {
                switch (polarity) {
                case 0: // both first; take lower if unequal
                    if (a < b) { // take a
                        // Back up over overlapping ranges in buffer[]
                        if (k > 0 && a <= buffer[k-1]) {
                            // Pick latter end value in buffer[] vs. list[]
                            a = max(list[i], buffer[--k]);
                        } else {
                            // No overlap
                            buffer[k++] = a;
                            a = list[i];
                        }
                        i++; // Common if/else code factored out
                        polarity ^= 1;
                    } else if (b < a) { // take b
                        if (k > 0 && b <= buffer[k-1]) {
                            b = max(other[j], buffer[--k]);
                        } else {
                            buffer[k++] = b;
                            b = other[j];
                        }
                        j++;
                        polarity ^= 2;
                    } else { // a == b, take a, drop b
                        if (a == HIGH) break main;
                        // This is symmetrical; it doesn't matter if
                        // we backtrack with a or b. - liu
                        if (k > 0 && a <= buffer[k-1]) {
                            a = max(list[i], buffer[--k]);
                        } else {
                            // No overlap
                            buffer[k++] = a;
                            a = list[i];
                        }
                        i++;
                        polarity ^= 1;
                        b = other[j++]; polarity ^= 2;
                    }
                    break;
                case 3: // both second; take higher if unequal, and drop other
                    if (b <= a) { // take a
                        if (a == HIGH) break main;
                        buffer[k++] = a;
                    } else { // take b
                        if (b == HIGH) break main;
                        buffer[k++] = b;
                    }
                    a = list[i++]; polarity ^= 1;   // factored common code
                    b = other[j++]; polarity ^= 2;
                    break;
                case 1: // a second, b first; if b < a, overlap
                    if (a < b) { // no overlap, take a
                        buffer[k++] = a; a = list[i++]; polarity ^= 1;
                    } else if (b < a) { // OVERLAP, drop b
                        b = other[j++]; polarity ^= 2;
                    } else { // a == b, drop both!
                        if (a == HIGH) break main;
                        a = list[i++]; polarity ^= 1;
                        b = other[j++]; polarity ^= 2;
                    }
                    break;
                case 2: // a first, b second; if a < b, overlap
                    if (b < a) { // no overlap, take b
                        buffer[k++] = b; b = other[j++]; polarity ^= 2;
                    } else  if (a < b) { // OVERLAP, drop a
                        a = list[i++]; polarity ^= 1;
                    } else { // a == b, drop both!
                        if (a == HIGH) break main;
                        a = list[i++]; polarity ^= 1;
                        b = other[j++]; polarity ^= 2;
                    }
                    break;
                }
            }
        buffer[k++] = HIGH;    // terminate
        len = k;
        // swap list and buffer
        int[] temp = list;
        list = buffer;
        buffer = temp;
        return this;
    }

    // polarity = 0 is normal: x intersect y
    // polarity = 2: x intersect ~y == set-minus
    // polarity = 1: ~x intersect y
    // polarity = 3: ~x intersect ~y

    private UnicodeSet retain(int[] other, int otherLen, int polarity) {
        ensureBufferCapacity(len + otherLen);
        int i = 0, j = 0, k = 0;
        int a = list[i++];
        int b = other[j++];
        // change from xor is that we have to check overlapping pairs
        // polarity bit 1 means a is second, bit 2 means b is.
        main:
            while (true) {
                switch (polarity) {
                case 0: // both first; drop the smaller
                    if (a < b) { // drop a
                        a = list[i++]; polarity ^= 1;
                    } else if (b < a) { // drop b
                        b = other[j++]; polarity ^= 2;
                    } else { // a == b, take one, drop other
                        if (a == HIGH) break main;
                        buffer[k++] = a; a = list[i++]; polarity ^= 1;
                        b = other[j++]; polarity ^= 2;
                    }
                    break;
                case 3: // both second; take lower if unequal
                    if (a < b) { // take a
                        buffer[k++] = a; a = list[i++]; polarity ^= 1;
                    } else if (b < a) { // take b
                        buffer[k++] = b; b = other[j++]; polarity ^= 2;
                    } else { // a == b, take one, drop other
                        if (a == HIGH) break main;
                        buffer[k++] = a; a = list[i++]; polarity ^= 1;
                        b = other[j++]; polarity ^= 2;
                    }
                    break;
                case 1: // a second, b first;
                    if (a < b) { // NO OVERLAP, drop a
                        a = list[i++]; polarity ^= 1;
                    } else if (b < a) { // OVERLAP, take b
                        buffer[k++] = b; b = other[j++]; polarity ^= 2;
                    } else { // a == b, drop both!
                        if (a == HIGH) break main;
                        a = list[i++]; polarity ^= 1;
                        b = other[j++]; polarity ^= 2;
                    }
                    break;
                case 2: // a first, b second; if a < b, overlap
                    if (b < a) { // no overlap, drop b
                        b = other[j++]; polarity ^= 2;
                    } else  if (a < b) { // OVERLAP, take a
                        buffer[k++] = a; a = list[i++]; polarity ^= 1;
                    } else { // a == b, drop both!
                        if (a == HIGH) break main;
                        a = list[i++]; polarity ^= 1;
                        b = other[j++]; polarity ^= 2;
                    }
                    break;
                }
            }
        buffer[k++] = HIGH;    // terminate
        len = k;
        // swap list and buffer
        int[] temp = list;
        list = buffer;
        buffer = temp;
        return this;
    }

    private static final int max(int a, int b) {
        return (a > b) ? a : b;
    }

    //----------------------------------------------------------------
    // Generic filter-based scanning code
    //----------------------------------------------------------------

    private static interface Filter {
        boolean contains(int codePoint);
    }

    private static final VersionInfo NO_VERSION = VersionInfo.getInstance(0, 0, 0, 0);

    private static class VersionFilter implements Filter {
        VersionInfo version;
        VersionFilter(VersionInfo version) { this.version = version; }
        public boolean contains(int ch) {
            VersionInfo v = UCharacter.getAge(ch);
            // Reference comparison ok; VersionInfo caches and reuses
            // unique objects.
            return v != NO_VERSION &&
                    v.compareTo(version) <= 0;
        }
    }

    private static synchronized UnicodeSet getInclusions(int src) {
        if (src != UCharacterProperty.SRC_PROPSVEC) {
            throw new IllegalStateException("UnicodeSet.getInclusions(unknown src "+src+")");
        }

        if (INCLUSION == null) {
            UnicodeSet incl = new UnicodeSet();
            UCharacterProperty.INSTANCE.upropsvec_addPropertyStarts(incl);
            INCLUSION = incl;
        }
        return INCLUSION;
    }

    /**
     * Generic filter-based scanning code for UCD property UnicodeSets.
     */
    private UnicodeSet applyFilter(Filter filter, int src) {
        // Logically, walk through all Unicode characters, noting the start
        // and end of each range for which filter.contain(c) is
        // true.  Add each range to a set.
        //
        // To improve performance, use an inclusions set which
        // encodes information about character ranges that are known
        // to have identical properties.
        // getInclusions(src) contains exactly the first characters of
        // same-value ranges for the given properties "source".

        clear();

        int startHasProperty = -1;
        UnicodeSet inclusions = getInclusions(src);
        int limitRange = inclusions.getRangeCount();

        for (int j=0; j<limitRange; ++j) {
            // get current range
            int start = inclusions.getRangeStart(j);
            int end = inclusions.getRangeEnd(j);

            // for all the code points in the range, process
            for (int ch = start; ch <= end; ++ch) {
                // only add to the unicodeset on inflection points --
                // where the hasProperty value changes to false
                if (filter.contains(ch)) {
                    if (startHasProperty < 0) {
                        startHasProperty = ch;
                    }
                } else if (startHasProperty >= 0) {
                    add_unchecked(startHasProperty, ch-1);
                    startHasProperty = -1;
                }
            }
        }
        if (startHasProperty >= 0) {
            add_unchecked(startHasProperty, 0x10FFFF);
        }

        return this;
    }

    /**
     * Is this frozen, according to the Freezable interface?
     *
     * @return value
     * @stable ICU 3.8
     */
    public boolean isFrozen() {
        return (bmpSet != null || stringSpan != null);
    }

    /**
     * Freeze this class, according to the Freezable interface.
     *
     * @return this
     * @stable ICU 4.4
     */
    public UnicodeSet freeze() {
        if (!isFrozen()) {
            // Do most of what compact() does before freezing because
            // compact() will not work when the set is frozen.
            // Small modification: Don't shrink if the savings would be tiny (<=GROW_EXTRA).

            // Delete buffer first to defragment memory less.
            buffer = null;
            if (list.length > (len + GROW_EXTRA)) {
                // Make the capacity equal to len or 1.
                // We don't want to realloc of 0 size.
                int capacity = (len == 0) ? 1 : len;
                int[] oldList = list;
                list = new int[capacity];
                for (int i = capacity; i-- > 0;) {
                    list[i] = oldList[i];
                }
            }

            // Optimize contains() and span() and similar functions.
            if (!strings.isEmpty()) {
                stringSpan = new UnicodeSetStringSpan(this, new ArrayList<String>(strings), UnicodeSetStringSpan.ALL);
            }
            if (stringSpan == null || !stringSpan.needsStringSpanUTF16()) {
                // Optimize for code point spans.
                // There are no strings, or
                // all strings are irrelevant for span() etc. because
                // all of each string's code points are contained in this set.
                // However, fully contained strings are relevant for spanAndCount(),
                // so we create both objects.
                bmpSet = new BMPSet(list, len);
            }
        }
        return this;
    }

    /**
     * Span a string using this UnicodeSet.
     * <p>To replace, count elements, or delete spans, see {@link com.ibm.icu.text.UnicodeSetSpanner UnicodeSetSpanner}.
     * @param s The string to be spanned
     * @param spanCondition The span condition
     * @return the length of the span
     * @stable ICU 4.4
     */
    public int span(CharSequence s, SpanCondition spanCondition) {
        return span(s, 0, spanCondition);
    }

    /**
     * Span a string using this UnicodeSet.
     *   If the start index is less than 0, span will start from 0.
     *   If the start index is greater than the string length, span returns the string length.
     * <p>To replace, count elements, or delete spans, see {@link com.ibm.icu.text.UnicodeSetSpanner UnicodeSetSpanner}.
     * @param s The string to be spanned
     * @param start The start index that the span begins
     * @param spanCondition The span condition
     * @return the string index which ends the span (i.e. exclusive)
     * @stable ICU 4.4
     */
    public int span(CharSequence s, int start, SpanCondition spanCondition) {
        int end = s.length();
        if (start < 0) {
            start = 0;
        } else if (start >= end) {
            return end;
        }
        if (bmpSet != null) {
            // Frozen set without strings, or no string is relevant for span().
            return bmpSet.span(s, start, spanCondition, null);
        }
        if (stringSpan != null) {
            return stringSpan.span(s, start, spanCondition);
        } else if (!strings.isEmpty()) {
            int which = spanCondition == SpanCondition.NOT_CONTAINED ? UnicodeSetStringSpan.FWD_UTF16_NOT_CONTAINED
                    : UnicodeSetStringSpan.FWD_UTF16_CONTAINED;
            UnicodeSetStringSpan strSpan = new UnicodeSetStringSpan(this, new ArrayList<String>(strings), which);
            if (strSpan.needsStringSpanUTF16()) {
                return strSpan.span(s, start, spanCondition);
            }
        }

        return spanCodePointsAndCount(s, start, spanCondition, null);
    }

    /**
     * Same as span() but also counts the smallest number of set elements on any path across the span.
     * <p>To replace, count elements, or delete spans, see {@link com.ibm.icu.text.UnicodeSetSpanner UnicodeSetSpanner}.
     * @param outCount An output-only object (must not be null) for returning the count.
     * @return the limit (exclusive end) of the span
     */
    public int spanAndCount(CharSequence s, int start, SpanCondition spanCondition, OutputInt outCount) {
        if (outCount == null) {
            throw new IllegalArgumentException("outCount must not be null");
        }
        int end = s.length();
        if (start < 0) {
            start = 0;
        } else if (start >= end) {
            return end;
        }
        if (stringSpan != null) {
            // We might also have bmpSet != null,
            // but fully-contained strings are relevant for counting elements.
            return stringSpan.spanAndCount(s, start, spanCondition, outCount);
        } else if (bmpSet != null) {
            return bmpSet.span(s, start, spanCondition, outCount);
        } else if (!strings.isEmpty()) {
            int which = spanCondition == SpanCondition.NOT_CONTAINED ? UnicodeSetStringSpan.FWD_UTF16_NOT_CONTAINED
                    : UnicodeSetStringSpan.FWD_UTF16_CONTAINED;
            which |= UnicodeSetStringSpan.WITH_COUNT;
            UnicodeSetStringSpan strSpan = new UnicodeSetStringSpan(this, new ArrayList<String>(strings), which);
            return strSpan.spanAndCount(s, start, spanCondition, outCount);
        }

        return spanCodePointsAndCount(s, start, spanCondition, outCount);
    }

    private int spanCodePointsAndCount(CharSequence s, int start,
            SpanCondition spanCondition, OutputInt outCount) {
        // Pin to 0/1 values.
        boolean spanContained = (spanCondition != SpanCondition.NOT_CONTAINED);

        int c;
        int next = start;
        int length = s.length();
        int count = 0;
        do {
            c = Character.codePointAt(s, next);
            if (spanContained != contains(c)) {
                break;
            }
            ++count;
            next += Character.charCount(c);
        } while (next < length);
        if (outCount != null) { outCount.value = count; }
        return next;
    }

    /**
     * Span a string backwards (from the fromIndex) using this UnicodeSet.
     * If the fromIndex is less than 0, spanBack will return 0.
     * If fromIndex is greater than the string length, spanBack will start from the string length.
     * <p>To replace, count elements, or delete spans, see {@link com.ibm.icu.text.UnicodeSetSpanner UnicodeSetSpanner}.
     * @param s The string to be spanned
     * @param fromIndex The index of the char (exclusive) that the string should be spanned backwards
     * @param spanCondition The span condition
     * @return The string index which starts the span (i.e. inclusive).
     * @stable ICU 4.4
     */
    public int spanBack(CharSequence s, int fromIndex, SpanCondition spanCondition) {
        if (fromIndex <= 0) {
            return 0;
        }
        if (fromIndex > s.length()) {
            fromIndex = s.length();
        }
        if (bmpSet != null) {
            // Frozen set without strings, or no string is relevant for spanBack().
            return bmpSet.spanBack(s, fromIndex, spanCondition);
        }
        if (stringSpan != null) {
            return stringSpan.spanBack(s, fromIndex, spanCondition);
        } else if (!strings.isEmpty()) {
            int which = (spanCondition == SpanCondition.NOT_CONTAINED)
                    ? UnicodeSetStringSpan.BACK_UTF16_NOT_CONTAINED
                            : UnicodeSetStringSpan.BACK_UTF16_CONTAINED;
            UnicodeSetStringSpan strSpan = new UnicodeSetStringSpan(this, new ArrayList<String>(strings), which);
            if (strSpan.needsStringSpanUTF16()) {
                return strSpan.spanBack(s, fromIndex, spanCondition);
            }
        }

        // Pin to 0/1 values.
        boolean spanContained = (spanCondition != SpanCondition.NOT_CONTAINED);

        int c;
        int prev = fromIndex;
        do {
            c = Character.codePointBefore(s, prev);
            if (spanContained != contains(c)) {
                break;
            }
            prev -= Character.charCount(c);
        } while (prev > 0);
        return prev;
    }

    /**
     * Clone a thawed version of this class, according to the Freezable interface.
     * @return the clone, not frozen
     * @stable ICU 4.4
     */
    public UnicodeSet cloneAsThawed() {
        UnicodeSet result = new UnicodeSet(this);
        assert !result.isFrozen();
        return result;
    }

    // internal function
    private void checkFrozen() {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
    }

    /**
     * Argument values for whether span() and similar functions continue while the current character is contained vs.
     * not contained in the set.
     * <p>
     * The functionality is straightforward for sets with only single code points, without strings (which is the common
     * case):
     * <ul>
     * <li>CONTAINED and SIMPLE work the same.
     * <li>CONTAINED and SIMPLE are inverses of NOT_CONTAINED.
     * <li>span() and spanBack() partition any string the
     * same way when alternating between span(NOT_CONTAINED) and span(either "contained" condition).
     * <li>Using a
     * complemented (inverted) set and the opposite span conditions yields the same results.
     * </ul>
     * When a set contains multi-code point strings, then these statements may not be true, depending on the strings in
     * the set (for example, whether they overlap with each other) and the string that is processed. For a set with
     * strings:
     * <ul>
     * <li>The complement of the set contains the opposite set of code points, but the same set of strings.
     * Therefore, complementing both the set and the span conditions may yield different results.
     * <li>When starting spans
     * at different positions in a string (span(s, ...) vs. span(s+1, ...)) the ends of the spans may be different
     * because a set string may start before the later position.
     * <li>span(SIMPLE) may be shorter than
     * span(CONTAINED) because it will not recursively try all possible paths. For example, with a set which
     * contains the three strings "xy", "xya" and "ax", span("xyax", CONTAINED) will return 4 but span("xyax",
     * SIMPLE) will return 3. span(SIMPLE) will never be longer than span(CONTAINED).
     * <li>With either "contained" condition, span() and spanBack() may partition a string in different ways. For example,
     * with a set which contains the two strings "ab" and "ba", and when processing the string "aba", span() will yield
     * contained/not-contained boundaries of { 0, 2, 3 } while spanBack() will yield boundaries of { 0, 1, 3 }.
     * </ul>
     * Note: If it is important to get the same boundaries whether iterating forward or backward through a string, then
     * either only span() should be used and the boundaries cached for backward operation, or an ICU BreakIterator could
     * be used.
     * <p>
     * Note: Unpaired surrogates are treated like surrogate code points. Similarly, set strings match only on code point
     * boundaries, never in the middle of a surrogate pair.
     *
     * @stable ICU 4.4
     */
    public enum SpanCondition {
        /**
         * Continues a span() while there is no set element at the current position.
         * Increments by one code point at a time.
         * Stops before the first set element (character or string).
         * (For code points only, this is like while contains(current)==false).
         * <p>
         * When span() returns, the substring between where it started and the position it returned consists only of
         * characters that are not in the set, and none of its strings overlap with the span.
         *
         * @stable ICU 4.4
         */
        NOT_CONTAINED,

        /**
         * Spans the longest substring that is a concatenation of set elements (characters or strings).
         * (For characters only, this is like while contains(current)==true).
         * <p>
         * When span() returns, the substring between where it started and the position it returned consists only of set
         * elements (characters or strings) that are in the set.
         * <p>
         * If a set contains strings, then the span will be the longest substring for which there
         * exists at least one non-overlapping concatenation of set elements (characters or strings).
         * This is equivalent to a POSIX regular expression for <code>(OR of each set element)*</code>.
         * (Java/ICU/Perl regex stops at the first match of an OR.)
         *
         * @stable ICU 4.4
         */
        CONTAINED,

        /**
         * Continues a span() while there is a set element at the current position.
         * Increments by the longest matching element at each position.
         * (For characters only, this is like while contains(current)==true).
         * <p>
         * When span() returns, the substring between where it started and the position it returned consists only of set
         * elements (characters or strings) that are in the set.
         * <p>
         * If a set only contains single characters, then this is the same as CONTAINED.
         * <p>
         * If a set contains strings, then the span will be the longest substring with a match at each position with the
         * longest single set element (character or string).
         * <p>
         * Use this span condition together with other longest-match algorithms, such as ICU converters
         * (ucnv_getUnicodeSet()).
         *
         * @stable ICU 4.4
         */
        SIMPLE,
    }

}
