/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
*******************************************************************************
* Copyright (C) 1996-2014, International Business Machines Corporation and
* others. All Rights Reserved.
*******************************************************************************
*/

package jdk.internal.icu.lang;

import jdk.internal.icu.impl.UBiDiProps;
import jdk.internal.icu.impl.UCharacterProperty;
import jdk.internal.icu.text.Normalizer2;
import jdk.internal.icu.text.UTF16;
import jdk.internal.icu.util.VersionInfo;

/**
 * <p>The UCharacter class provides extensions to the
 * <a href="http://java.sun.com/j2se/1.5/docs/api/java/lang/Character.html">
 * java.lang.Character</a> class. These extensions provide support for
 * more Unicode properties and together with the <a href=../text/UTF16.html>UTF16</a>
 * class, provide support for supplementary characters (those with code
 * points above U+FFFF).
 * Each ICU release supports the latest version of Unicode available at that time.
 *
 * <p>Code points are represented in these API using ints. While it would be
 * more convenient in Java to have a separate primitive datatype for them,
 * ints suffice in the meantime.
 *
 * <p>To use this class please add the jar file name icu4j.jar to the
 * class path, since it contains data files which supply the information used
 * by this file.<br>
 * E.g. In Windows <br>
 * <code>set CLASSPATH=%CLASSPATH%;$JAR_FILE_PATH/ucharacter.jar</code>.<br>
 * Otherwise, another method would be to copy the files uprops.dat and
 * unames.icu from the icu4j source subdirectory
 * <i>$ICU4J_SRC/src/com.ibm.icu.impl.data</i> to your class directory
 * <i>$ICU4J_CLASS/com.ibm.icu.impl.data</i>.
 *
 * <p>Aside from the additions for UTF-16 support, and the updated Unicode
 * properties, the main differences between UCharacter and Character are:
 * <ul>
 * <li> UCharacter is not designed to be a char wrapper and does not have
 *      APIs to which involves management of that single char.<br>
 *      These include:
 *      <ul>
 *        <li> char charValue(),
 *        <li> int compareTo(java.lang.Character, java.lang.Character), etc.
 *      </ul>
 * <li> UCharacter does not include Character APIs that are deprecated, nor
 *      does it include the Java-specific character information, such as
 *      boolean isJavaIdentifierPart(char ch).
 * <li> Character maps characters 'A' - 'Z' and 'a' - 'z' to the numeric
 *      values '10' - '35'. UCharacter also does this in digit and
 *      getNumericValue, to adhere to the java semantics of these
 *      methods.  New methods unicodeDigit, and
 *      getUnicodeNumericValue do not treat the above code points
 *      as having numeric values.  This is a semantic change from ICU4J 1.3.1.
 * </ul>
 * <p>
 * Further detail on differences can be determined using the program
 *        <a href=
 * "http://source.icu-project.org/repos/icu/icu4j/trunk/src/com/ibm/icu/dev/test/lang/UCharacterCompare.java">
 *        com.ibm.icu.dev.test.lang.UCharacterCompare</a>
 * </p>
 * <p>
 * In addition to Java compatibility functions, which calculate derived properties,
 * this API provides low-level access to the Unicode Character Database.
 * </p>
 * <p>
 * Unicode assigns each code point (not just assigned character) values for
 * many properties.
 * Most of them are simple boolean flags, or constants from a small enumerated list.
 * For some properties, values are strings or other relatively more complex types.
 * </p>
 * <p>
 * For more information see
 * <a href="http://www.unicode/org/ucd/">"About the Unicode Character Database"</a>
 * (http://www.unicode.org/ucd/)
 * and the <a href="http://www.icu-project.org/userguide/properties.html">ICU
 * User Guide chapter on Properties</a>
 * (http://www.icu-project.org/userguide/properties.html).
 * </p>
 * <p>
 * There are also functions that provide easy migration from C/POSIX functions
 * like isblank(). Their use is generally discouraged because the C/POSIX
 * standards do not define their semantics beyond the ASCII range, which means
 * that different implementations exhibit very different behavior.
 * Instead, Unicode properties should be used directly.
 * </p>
 * <p>
 * There are also only a few, broad C/POSIX character classes, and they tend
 * to be used for conflicting purposes. For example, the "isalpha()" class
 * is sometimes used to determine word boundaries, while a more sophisticated
 * approach would at least distinguish initial letters from continuation
 * characters (the latter including combining marks).
 * (In ICU, BreakIterator is the most sophisticated API for word boundaries.)
 * Another example: There is no "istitle()" class for titlecase characters.
 * </p>
 * <p>
 * ICU 3.4 and later provides API access for all twelve C/POSIX character classes.
 * ICU implements them according to the Standard Recommendations in
 * Annex C: Compatibility Properties of UTS #18 Unicode Regular Expressions
 * (http://www.unicode.org/reports/tr18/#Compatibility_Properties).
 * </p>
 * <p>
 * API access for C/POSIX character classes is as follows:
 * <pre>{@code
 * - alpha:     isUAlphabetic(c) or hasBinaryProperty(c, UProperty.ALPHABETIC)
 * - lower:     isULowercase(c) or hasBinaryProperty(c, UProperty.LOWERCASE)
 * - upper:     isUUppercase(c) or hasBinaryProperty(c, UProperty.UPPERCASE)
 * - punct:     ((1<<getType(c)) & ((1<<DASH_PUNCTUATION)|(1<<START_PUNCTUATION)|
 *               (1<<END_PUNCTUATION)|(1<<CONNECTOR_PUNCTUATION)|(1<<OTHER_PUNCTUATION)|
 *               (1<<INITIAL_PUNCTUATION)|(1<<FINAL_PUNCTUATION)))!=0
 * - digit:     isDigit(c) or getType(c)==DECIMAL_DIGIT_NUMBER
 * - xdigit:    hasBinaryProperty(c, UProperty.POSIX_XDIGIT)
 * - alnum:     hasBinaryProperty(c, UProperty.POSIX_ALNUM)
 * - space:     isUWhiteSpace(c) or hasBinaryProperty(c, UProperty.WHITE_SPACE)
 * - blank:     hasBinaryProperty(c, UProperty.POSIX_BLANK)
 * - cntrl:     getType(c)==CONTROL
 * - graph:     hasBinaryProperty(c, UProperty.POSIX_GRAPH)
 * - print:     hasBinaryProperty(c, UProperty.POSIX_PRINT)
 * }</pre>
 * </p>
 * <p>
 * The C/POSIX character classes are also available in UnicodeSet patterns,
 * using patterns like [:graph:] or \p{graph}.
 * </p>
 *
 * There are several ICU (and Java) whitespace functions.
 * Comparison:<ul>
 * <li> isUWhiteSpace=UCHAR_WHITE_SPACE: Unicode White_Space property;
 *       most of general categories "Z" (separators) + most whitespace ISO controls
 *       (including no-break spaces, but excluding IS1..IS4 and ZWSP)
 * <li> isWhitespace: Java isWhitespace; Z + whitespace ISO controls but excluding no-break spaces
 * <li> isSpaceChar: just Z (including no-break spaces)</ul>
 * </p>
 * <p>
 * This class is not subclassable.
 * </p>
 * @author Syn Wee Quek
 * @stable ICU 2.1
 * @see com.ibm.icu.lang.UCharacterEnums
 */

public final class UCharacter
{

    /**
     * Joining Group constants.
     * @see UProperty#JOINING_GROUP
     * @stable ICU 2.4
     */
    public static interface JoiningGroup
    {
        /**
         * @stable ICU 2.4
         */
        public static final int NO_JOINING_GROUP = 0;
    }

    /**
     * Numeric Type constants.
     * @see UProperty#NUMERIC_TYPE
     * @stable ICU 2.4
     */
    public static interface NumericType
    {
        /**
         * @stable ICU 2.4
         */
        public static final int NONE = 0;
        /**
         * @stable ICU 2.4
         */
        public static final int DECIMAL = 1;
        /**
         * @stable ICU 2.4
         */
        public static final int DIGIT = 2;
        /**
         * @stable ICU 2.4
         */
        public static final int NUMERIC = 3;
        /**
         * @stable ICU 2.4
         */
        public static final int COUNT = 4;
    }

    /**
     * Hangul Syllable Type constants.
     *
     * @see UProperty#HANGUL_SYLLABLE_TYPE
     * @stable ICU 2.6
     */
    public static interface HangulSyllableType
    {
        /**
         * @stable ICU 2.6
         */
        public static final int NOT_APPLICABLE      = 0;   /*[NA]*/ /*See note !!*/
        /**
         * @stable ICU 2.6
         */
        public static final int LEADING_JAMO        = 1;   /*[L]*/
        /**
         * @stable ICU 2.6
         */
        public static final int VOWEL_JAMO          = 2;   /*[V]*/
        /**
         * @stable ICU 2.6
         */
        public static final int TRAILING_JAMO       = 3;   /*[T]*/
        /**
         * @stable ICU 2.6
         */
        public static final int LV_SYLLABLE         = 4;   /*[LV]*/
        /**
         * @stable ICU 2.6
         */
        public static final int LVT_SYLLABLE        = 5;   /*[LVT]*/
        /**
         * @stable ICU 2.6
         */
        public static final int COUNT               = 6;
    }

    // public data members -----------------------------------------------

    /**
     * The lowest Unicode code point value.
     * @stable ICU 2.1
     */
    public static final int MIN_VALUE = UTF16.CODEPOINT_MIN_VALUE;

    /**
     * The highest Unicode code point value (scalar value) according to the
     * Unicode Standard.
     * This is a 21-bit value (21 bits, rounded up).<br>
     * Up-to-date Unicode implementation of java.lang.Character.MAX_VALUE
     * @stable ICU 2.1
     */
    public static final int MAX_VALUE = UTF16.CODEPOINT_MAX_VALUE;

    // public methods ----------------------------------------------------

    /**
     * Returns the numeric value of a decimal digit code point.
     * <br>This method observes the semantics of
     * <code>java.lang.Character.digit()</code>.  Note that this
     * will return positive values for code points for which isDigit
     * returns false, just like java.lang.Character.
     * <br><em>Semantic Change:</em> In release 1.3.1 and
     * prior, this did not treat the European letters as having a
     * digit value, and also treated numeric letters and other numbers as
     * digits.
     * This has been changed to conform to the java semantics.
     * <br>A code point is a valid digit if and only if:
     * <ul>
     *   <li>ch is a decimal digit or one of the european letters, and
     *   <li>the value of ch is less than the specified radix.
     * </ul>
     * @param ch the code point to query
     * @param radix the radix
     * @return the numeric value represented by the code point in the
     * specified radix, or -1 if the code point is not a decimal digit
     * or if its value is too large for the radix
     * @stable ICU 2.1
     */
    public static int digit(int ch, int radix)
    {
        if (2 <= radix && radix <= 36) {
            int value = digit(ch);
            if (value < 0) {
                // ch is not a decimal digit, try latin letters
                value = UCharacterProperty.getEuropeanDigit(ch);
            }
            return (value < radix) ? value : -1;
        } else {
            return -1;  // invalid radix
        }
    }

    /**
     * Returns the numeric value of a decimal digit code point.
     * <br>This is a convenience overload of <code>digit(int, int)</code>
     * that provides a decimal radix.
     * <br><em>Semantic Change:</em> In release 1.3.1 and prior, this
     * treated numeric letters and other numbers as digits.  This has
     * been changed to conform to the java semantics.
     * @param ch the code point to query
     * @return the numeric value represented by the code point,
     * or -1 if the code point is not a decimal digit or if its
     * value is too large for a decimal radix
     * @stable ICU 2.1
     */
    public static int digit(int ch)
    {
        return UCharacterProperty.INSTANCE.digit(ch);
    }

    /**
     * Returns a value indicating a code point's Unicode category.
     * Up-to-date Unicode implementation of java.lang.Character.getType()
     * except for the above mentioned code points that had their category
     * changed.<br>
     * Return results are constants from the interface
     * <a href=UCharacterCategory.html>UCharacterCategory</a><br>
     * <em>NOTE:</em> the UCharacterCategory values are <em>not</em> compatible with
     * those returned by java.lang.Character.getType.  UCharacterCategory values
     * match the ones used in ICU4C, while java.lang.Character type
     * values, though similar, skip the value 17.</p>
     * @param ch code point whose type is to be determined
     * @return category which is a value of UCharacterCategory
     * @stable ICU 2.1
     */
    public static int getType(int ch)
    {
        return UCharacterProperty.INSTANCE.getType(ch);
    }

    /**
     * Returns the Bidirection property of a code point.
     * For example, 0x0041 (letter A) has the LEFT_TO_RIGHT directional
     * property.<br>
     * Result returned belongs to the interface
     * <a href=UCharacterDirection.html>UCharacterDirection</a>
     * @param ch the code point to be determined its direction
     * @return direction constant from UCharacterDirection.
     * @stable ICU 2.1
     */
    public static int getDirection(int ch)
    {
        return UBiDiProps.INSTANCE.getClass(ch);
    }

    /**
     * Maps the specified code point to a "mirror-image" code point.
     * For code points with the "mirrored" property, implementations sometimes
     * need a "poor man's" mapping to another code point such that the default
     * glyph may serve as the mirror-image of the default glyph of the
     * specified code point.<br>
     * This is useful for text conversion to and from codepages with visual
     * order, and for displays without glyph selection capabilities.
     * @param ch code point whose mirror is to be retrieved
     * @return another code point that may serve as a mirror-image substitute,
     *         or ch itself if there is no such mapping or ch does not have the
     *         "mirrored" property
     * @stable ICU 2.1
     */
    public static int getMirror(int ch)
    {
        return UBiDiProps.INSTANCE.getMirror(ch);
    }

    /**
     * Maps the specified character to its paired bracket character.
     * For Bidi_Paired_Bracket_Type!=None, this is the same as getMirror(int).
     * Otherwise c itself is returned.
     * See http://www.unicode.org/reports/tr9/
     *
     * @param c the code point to be mapped
     * @return the paired bracket code point,
     *         or c itself if there is no such mapping
     *         (Bidi_Paired_Bracket_Type=None)
     *
     * @see UProperty#BIDI_PAIRED_BRACKET
     * @see UProperty#BIDI_PAIRED_BRACKET_TYPE
     * @see #getMirror(int)
     * @stable ICU 52
     */
    public static int getBidiPairedBracket(int c) {
        return UBiDiProps.INSTANCE.getPairedBracket(c);
    }

    /**
     * Returns the combining class of the argument codepoint
     * @param ch code point whose combining is to be retrieved
     * @return the combining class of the codepoint
     * @stable ICU 2.1
     */
    public static int getCombiningClass(int ch)
    {
        return Normalizer2.getNFDInstance().getCombiningClass(ch);
    }

    /**
     * Returns the version of Unicode data used.
     * @return the unicode version number used
     * @stable ICU 2.1
     */
    public static VersionInfo getUnicodeVersion()
    {
        return UCharacterProperty.INSTANCE.m_unicodeVersion_;
    }

    /**
     * Returns a code point corresponding to the two UTF16 characters.
     * @param lead the lead char
     * @param trail the trail char
     * @return code point if surrogate characters are valid.
     * @exception IllegalArgumentException thrown when argument characters do
     *            not form a valid codepoint
     * @stable ICU 2.1
     */
    public static int getCodePoint(char lead, char trail)
    {
        if (UTF16.isLeadSurrogate(lead) && UTF16.isTrailSurrogate(trail)) {
            return UCharacterProperty.getRawSupplementary(lead, trail);
        }
        throw new IllegalArgumentException("Illegal surrogate characters");
    }

    /**
     * Returns the "age" of the code point.</p>
     * <p>The "age" is the Unicode version when the code point was first
     * designated (as a non-character or for Private Use) or assigned a
     * character.
     * <p>This can be useful to avoid emitting code points to receiving
     * processes that do not accept newer characters.</p>
     * <p>The data is from the UCD file DerivedAge.txt.</p>
     * @param ch The code point.
     * @return the Unicode version number
     * @stable ICU 2.6
     */
    public static VersionInfo getAge(int ch)
    {
        if (ch < MIN_VALUE || ch > MAX_VALUE) {
            throw new IllegalArgumentException("Codepoint out of bounds");
        }
        return UCharacterProperty.INSTANCE.getAge(ch);
    }

    /**
     * Returns the property value for an Unicode property type of a code point.
     * Also returns binary and mask property values.</p>
     * <p>Unicode, especially in version 3.2, defines many more properties than
     * the original set in UnicodeData.txt.</p>
     * <p>The properties APIs are intended to reflect Unicode properties as
     * defined in the Unicode Character Database (UCD) and Unicode Technical
     * Reports (UTR). For details about the properties see
     * http://www.unicode.org/.</p>
     * <p>For names of Unicode properties see the UCD file PropertyAliases.txt.
     * </p>
     * <pre>
     * Sample usage:
     * int ea = UCharacter.getIntPropertyValue(c, UProperty.EAST_ASIAN_WIDTH);
     * int ideo = UCharacter.getIntPropertyValue(c, UProperty.IDEOGRAPHIC);
     * boolean b = (ideo == 1) ? true : false;
     * </pre>
     * @param ch code point to test.
     * @param type UProperty selector constant, identifies which binary
     *        property to check. Must be
     *        UProperty.BINARY_START &lt;= type &lt; UProperty.BINARY_LIMIT or
     *        UProperty.INT_START &lt;= type &lt; UProperty.INT_LIMIT or
     *        UProperty.MASK_START &lt;= type &lt; UProperty.MASK_LIMIT.
     * @return numeric value that is directly the property value or,
     *         for enumerated properties, corresponds to the numeric value of
     *         the enumerated constant of the respective property value
     *         enumeration type (cast to enum type if necessary).
     *         Returns 0 or 1 (for false / true) for binary Unicode properties.
     *         Returns a bit-mask for mask properties.
     *         Returns 0 if 'type' is out of bounds or if the Unicode version
     *         does not have data for the property at all, or not for this code
     *         point.
     * @see UProperty
     * @see #hasBinaryProperty
     * @see #getIntPropertyMinValue
     * @see #getIntPropertyMaxValue
     * @see #getUnicodeVersion
     * @stable ICU 2.4
     */
     // for BiDiBase.java
    public static int getIntPropertyValue(int ch, int type) {
        return UCharacterProperty.INSTANCE.getIntPropertyValue(ch, type);
    }

    // private constructor -----------------------------------------------

    /**
     * Private constructor to prevent instantiation
     */
    private UCharacter() { }

      /*
       * Copied from UCharacterEnums.java
       */

        /**
         * Character type Mn
         * @stable ICU 2.1
         */
        public static final byte NON_SPACING_MARK        = 6;
        /**
         * Character type Me
         * @stable ICU 2.1
         */
        public static final byte ENCLOSING_MARK          = 7;
        /**
         * Character type Mc
         * @stable ICU 2.1
         */
        public static final byte COMBINING_SPACING_MARK  = 8;
        /**
         * Character type count
         * @stable ICU 2.1
         */
        public static final byte CHAR_CATEGORY_COUNT     = 30;

        /**
         * Directional type R
         * @stable ICU 2.1
         */
        public static final int RIGHT_TO_LEFT              = 1;
        /**
         * Directional type AL
         * @stable ICU 2.1
         */
        public static final int RIGHT_TO_LEFT_ARABIC       = 13;
}
