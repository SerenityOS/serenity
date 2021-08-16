/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import jdk.internal.misc.CDS;
import jdk.internal.vm.annotation.IntrinsicCandidate;

import java.lang.constant.Constable;
import java.lang.constant.DynamicConstantDesc;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.lang.constant.ConstantDescs.BSM_EXPLICIT_CAST;
import static java.lang.constant.ConstantDescs.CD_char;
import static java.lang.constant.ConstantDescs.CD_int;
import static java.lang.constant.ConstantDescs.DEFAULT_NAME;

/**
 * The {@code Character} class wraps a value of the primitive
 * type {@code char} in an object. An object of class
 * {@code Character} contains a single field whose type is
 * {@code char}.
 * <p>
 * In addition, this class provides a large number of static methods for
 * determining a character's category (lowercase letter, digit, etc.)
 * and for converting characters from uppercase to lowercase and vice
 * versa.
 *
 * <h2><a id="conformance">Unicode Conformance</a></h2>
 * <p>
 * The fields and methods of class {@code Character} are defined in terms
 * of character information from the Unicode Standard, specifically the
 * <i>UnicodeData</i> file that is part of the Unicode Character Database.
 * This file specifies properties including name and category for every
 * assigned Unicode code point or character range. The file is available
 * from the Unicode Consortium at
 * <a href="http://www.unicode.org">http://www.unicode.org</a>.
 * <p>
 * Character information is based on the Unicode Standard, version 13.0.
 * <p>
 * The Java platform has supported different versions of the Unicode
 * Standard over time. Upgrades to newer versions of the Unicode Standard
 * occurred in the following Java releases, each indicating the new version:
 * <table class="striped">
 * <caption style="display:none">Shows Java releases and supported Unicode versions</caption>
 * <thead>
 * <tr><th scope="col">Java release</th>
 *     <th scope="col">Unicode version</th></tr>
 * </thead>
 * <tbody>
 * <tr><td>Java SE 15</td>
 *     <td>Unicode 13.0</td></tr>
 * <tr><td>Java SE 13</td>
 *     <td>Unicode 12.1</td></tr>
 * <tr><td>Java SE 12</td>
 *     <td>Unicode 11.0</td></tr>
 * <tr><td>Java SE 11</td>
 *     <td>Unicode 10.0</td></tr>
 * <tr><td>Java SE 9</td>
 *     <td>Unicode 8.0</td></tr>
 * <tr><td>Java SE 8</td>
 *     <td>Unicode 6.2</td></tr>
 * <tr><td>Java SE 7</td>
 *     <td>Unicode 6.0</td></tr>
 * <tr><td>Java SE 5.0</td>
 *     <td>Unicode 4.0</td></tr>
 * <tr><td>Java SE 1.4</td>
 *     <td>Unicode 3.0</td></tr>
 * <tr><td>JDK 1.1</td>
 *     <td>Unicode 2.0</td></tr>
 * <tr><td>JDK 1.0.2</td>
 *     <td>Unicode 1.1.5</td></tr>
 * </tbody>
 * </table>
 * Variations from these base Unicode versions, such as recognized appendixes,
 * are documented elsewhere.
 * <h2><a id="unicode">Unicode Character Representations</a></h2>
 *
 * <p>The {@code char} data type (and therefore the value that a
 * {@code Character} object encapsulates) are based on the
 * original Unicode specification, which defined characters as
 * fixed-width 16-bit entities. The Unicode Standard has since been
 * changed to allow for characters whose representation requires more
 * than 16 bits.  The range of legal <em>code point</em>s is now
 * U+0000 to U+10FFFF, known as <em>Unicode scalar value</em>.
 * (Refer to the <a
 * href="http://www.unicode.org/reports/tr27/#notation"><i>
 * definition</i></a> of the U+<i>n</i> notation in the Unicode
 * Standard.)
 *
 * <p><a id="BMP">The set of characters from U+0000 to U+FFFF</a> is
 * sometimes referred to as the <em>Basic Multilingual Plane (BMP)</em>.
 * <a id="supplementary">Characters</a> whose code points are greater
 * than U+FFFF are called <em>supplementary character</em>s.  The Java
 * platform uses the UTF-16 representation in {@code char} arrays and
 * in the {@code String} and {@code StringBuffer} classes. In
 * this representation, supplementary characters are represented as a pair
 * of {@code char} values, the first from the <em>high-surrogates</em>
 * range, (&#92;uD800-&#92;uDBFF), the second from the
 * <em>low-surrogates</em> range (&#92;uDC00-&#92;uDFFF).
 *
 * <p>A {@code char} value, therefore, represents Basic
 * Multilingual Plane (BMP) code points, including the surrogate
 * code points, or code units of the UTF-16 encoding. An
 * {@code int} value represents all Unicode code points,
 * including supplementary code points. The lower (least significant)
 * 21 bits of {@code int} are used to represent Unicode code
 * points and the upper (most significant) 11 bits must be zero.
 * Unless otherwise specified, the behavior with respect to
 * supplementary characters and surrogate {@code char} values is
 * as follows:
 *
 * <ul>
 * <li>The methods that only accept a {@code char} value cannot support
 * supplementary characters. They treat {@code char} values from the
 * surrogate ranges as undefined characters. For example,
 * {@code Character.isLetter('\u005CuD840')} returns {@code false}, even though
 * this specific value if followed by any low-surrogate value in a string
 * would represent a letter.
 *
 * <li>The methods that accept an {@code int} value support all
 * Unicode characters, including supplementary characters. For
 * example, {@code Character.isLetter(0x2F81A)} returns
 * {@code true} because the code point value represents a letter
 * (a CJK ideograph).
 * </ul>
 *
 * <p>In the Java SE API documentation, <em>Unicode code point</em> is
 * used for character values in the range between U+0000 and U+10FFFF,
 * and <em>Unicode code unit</em> is used for 16-bit
 * {@code char} values that are code units of the <em>UTF-16</em>
 * encoding. For more information on Unicode terminology, refer to the
 * <a href="http://www.unicode.org/glossary/">Unicode Glossary</a>.
 *
 * <p>This is a <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>
 * class; programmers should treat instances that are
 * {@linkplain #equals(Object) equal} as interchangeable and should not
 * use instances for synchronization, or unpredictable behavior may
 * occur. For example, in a future release, synchronization may fail.
 *
 * @author  Lee Boynton
 * @author  Guy Steele
 * @author  Akira Tanaka
 * @author  Martin Buchholz
 * @author  Ulf Zibis
 * @since   1.0
 */
@jdk.internal.ValueBased
public final
class Character implements java.io.Serializable, Comparable<Character>, Constable {
    /**
     * The minimum radix available for conversion to and from strings.
     * The constant value of this field is the smallest value permitted
     * for the radix argument in radix-conversion methods such as the
     * {@code digit} method, the {@code forDigit} method, and the
     * {@code toString} method of class {@code Integer}.
     *
     * @see     Character#digit(char, int)
     * @see     Character#forDigit(int, int)
     * @see     Integer#toString(int, int)
     * @see     Integer#valueOf(String)
     */
    public static final int MIN_RADIX = 2;

    /**
     * The maximum radix available for conversion to and from strings.
     * The constant value of this field is the largest value permitted
     * for the radix argument in radix-conversion methods such as the
     * {@code digit} method, the {@code forDigit} method, and the
     * {@code toString} method of class {@code Integer}.
     *
     * @see     Character#digit(char, int)
     * @see     Character#forDigit(int, int)
     * @see     Integer#toString(int, int)
     * @see     Integer#valueOf(String)
     */
    public static final int MAX_RADIX = 36;

    /**
     * The constant value of this field is the smallest value of type
     * {@code char}, {@code '\u005Cu0000'}.
     *
     * @since   1.0.2
     */
    public static final char MIN_VALUE = '\u0000';

    /**
     * The constant value of this field is the largest value of type
     * {@code char}, {@code '\u005CuFFFF'}.
     *
     * @since   1.0.2
     */
    public static final char MAX_VALUE = '\uFFFF';

    /**
     * The {@code Class} instance representing the primitive type
     * {@code char}.
     *
     * @since   1.1
     */
    @SuppressWarnings("unchecked")
    public static final Class<Character> TYPE = (Class<Character>) Class.getPrimitiveClass("char");

    /*
     * Normative general types
     */

    /*
     * General character types
     */

    /**
     * General category "Cn" in the Unicode specification.
     * @since   1.1
     */
    public static final byte UNASSIGNED = 0;

    /**
     * General category "Lu" in the Unicode specification.
     * @since   1.1
     */
    public static final byte UPPERCASE_LETTER = 1;

    /**
     * General category "Ll" in the Unicode specification.
     * @since   1.1
     */
    public static final byte LOWERCASE_LETTER = 2;

    /**
     * General category "Lt" in the Unicode specification.
     * @since   1.1
     */
    public static final byte TITLECASE_LETTER = 3;

    /**
     * General category "Lm" in the Unicode specification.
     * @since   1.1
     */
    public static final byte MODIFIER_LETTER = 4;

    /**
     * General category "Lo" in the Unicode specification.
     * @since   1.1
     */
    public static final byte OTHER_LETTER = 5;

    /**
     * General category "Mn" in the Unicode specification.
     * @since   1.1
     */
    public static final byte NON_SPACING_MARK = 6;

    /**
     * General category "Me" in the Unicode specification.
     * @since   1.1
     */
    public static final byte ENCLOSING_MARK = 7;

    /**
     * General category "Mc" in the Unicode specification.
     * @since   1.1
     */
    public static final byte COMBINING_SPACING_MARK = 8;

    /**
     * General category "Nd" in the Unicode specification.
     * @since   1.1
     */
    public static final byte DECIMAL_DIGIT_NUMBER = 9;

    /**
     * General category "Nl" in the Unicode specification.
     * @since   1.1
     */
    public static final byte LETTER_NUMBER = 10;

    /**
     * General category "No" in the Unicode specification.
     * @since   1.1
     */
    public static final byte OTHER_NUMBER = 11;

    /**
     * General category "Zs" in the Unicode specification.
     * @since   1.1
     */
    public static final byte SPACE_SEPARATOR = 12;

    /**
     * General category "Zl" in the Unicode specification.
     * @since   1.1
     */
    public static final byte LINE_SEPARATOR = 13;

    /**
     * General category "Zp" in the Unicode specification.
     * @since   1.1
     */
    public static final byte PARAGRAPH_SEPARATOR = 14;

    /**
     * General category "Cc" in the Unicode specification.
     * @since   1.1
     */
    public static final byte CONTROL = 15;

    /**
     * General category "Cf" in the Unicode specification.
     * @since   1.1
     */
    public static final byte FORMAT = 16;

    /**
     * General category "Co" in the Unicode specification.
     * @since   1.1
     */
    public static final byte PRIVATE_USE = 18;

    /**
     * General category "Cs" in the Unicode specification.
     * @since   1.1
     */
    public static final byte SURROGATE = 19;

    /**
     * General category "Pd" in the Unicode specification.
     * @since   1.1
     */
    public static final byte DASH_PUNCTUATION = 20;

    /**
     * General category "Ps" in the Unicode specification.
     * @since   1.1
     */
    public static final byte START_PUNCTUATION = 21;

    /**
     * General category "Pe" in the Unicode specification.
     * @since   1.1
     */
    public static final byte END_PUNCTUATION = 22;

    /**
     * General category "Pc" in the Unicode specification.
     * @since   1.1
     */
    public static final byte CONNECTOR_PUNCTUATION = 23;

    /**
     * General category "Po" in the Unicode specification.
     * @since   1.1
     */
    public static final byte OTHER_PUNCTUATION = 24;

    /**
     * General category "Sm" in the Unicode specification.
     * @since   1.1
     */
    public static final byte MATH_SYMBOL = 25;

    /**
     * General category "Sc" in the Unicode specification.
     * @since   1.1
     */
    public static final byte CURRENCY_SYMBOL = 26;

    /**
     * General category "Sk" in the Unicode specification.
     * @since   1.1
     */
    public static final byte MODIFIER_SYMBOL = 27;

    /**
     * General category "So" in the Unicode specification.
     * @since   1.1
     */
    public static final byte OTHER_SYMBOL = 28;

    /**
     * General category "Pi" in the Unicode specification.
     * @since   1.4
     */
    public static final byte INITIAL_QUOTE_PUNCTUATION = 29;

    /**
     * General category "Pf" in the Unicode specification.
     * @since   1.4
     */
    public static final byte FINAL_QUOTE_PUNCTUATION = 30;

    /**
     * Error flag. Use int (code point) to avoid confusion with U+FFFF.
     */
    static final int ERROR = 0xFFFFFFFF;


    /**
     * Undefined bidirectional character type. Undefined {@code char}
     * values have undefined directionality in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_UNDEFINED = -1;

    /**
     * Strong bidirectional character type "L" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_LEFT_TO_RIGHT = 0;

    /**
     * Strong bidirectional character type "R" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT = 1;

    /**
     * Strong bidirectional character type "AL" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC = 2;

    /**
     * Weak bidirectional character type "EN" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_EUROPEAN_NUMBER = 3;

    /**
     * Weak bidirectional character type "ES" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR = 4;

    /**
     * Weak bidirectional character type "ET" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR = 5;

    /**
     * Weak bidirectional character type "AN" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_ARABIC_NUMBER = 6;

    /**
     * Weak bidirectional character type "CS" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_COMMON_NUMBER_SEPARATOR = 7;

    /**
     * Weak bidirectional character type "NSM" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_NONSPACING_MARK = 8;

    /**
     * Weak bidirectional character type "BN" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_BOUNDARY_NEUTRAL = 9;

    /**
     * Neutral bidirectional character type "B" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_PARAGRAPH_SEPARATOR = 10;

    /**
     * Neutral bidirectional character type "S" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_SEGMENT_SEPARATOR = 11;

    /**
     * Neutral bidirectional character type "WS" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_WHITESPACE = 12;

    /**
     * Neutral bidirectional character type "ON" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_OTHER_NEUTRALS = 13;

    /**
     * Strong bidirectional character type "LRE" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING = 14;

    /**
     * Strong bidirectional character type "LRO" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE = 15;

    /**
     * Strong bidirectional character type "RLE" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING = 16;

    /**
     * Strong bidirectional character type "RLO" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE = 17;

    /**
     * Weak bidirectional character type "PDF" in the Unicode specification.
     * @since 1.4
     */
    public static final byte DIRECTIONALITY_POP_DIRECTIONAL_FORMAT = 18;

    /**
     * Weak bidirectional character type "LRI" in the Unicode specification.
     * @since 9
     */
    public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_ISOLATE = 19;

    /**
     * Weak bidirectional character type "RLI" in the Unicode specification.
     * @since 9
     */
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_ISOLATE = 20;

    /**
     * Weak bidirectional character type "FSI" in the Unicode specification.
     * @since 9
     */
    public static final byte DIRECTIONALITY_FIRST_STRONG_ISOLATE = 21;

    /**
     * Weak bidirectional character type "PDI" in the Unicode specification.
     * @since 9
     */
    public static final byte DIRECTIONALITY_POP_DIRECTIONAL_ISOLATE = 22;

    /**
     * The minimum value of a
     * <a href="http://www.unicode.org/glossary/#high_surrogate_code_unit">
     * Unicode high-surrogate code unit</a>
     * in the UTF-16 encoding, constant {@code '\u005CuD800'}.
     * A high-surrogate is also known as a <i>leading-surrogate</i>.
     *
     * @since 1.5
     */
    public static final char MIN_HIGH_SURROGATE = '\uD800';

    /**
     * The maximum value of a
     * <a href="http://www.unicode.org/glossary/#high_surrogate_code_unit">
     * Unicode high-surrogate code unit</a>
     * in the UTF-16 encoding, constant {@code '\u005CuDBFF'}.
     * A high-surrogate is also known as a <i>leading-surrogate</i>.
     *
     * @since 1.5
     */
    public static final char MAX_HIGH_SURROGATE = '\uDBFF';

    /**
     * The minimum value of a
     * <a href="http://www.unicode.org/glossary/#low_surrogate_code_unit">
     * Unicode low-surrogate code unit</a>
     * in the UTF-16 encoding, constant {@code '\u005CuDC00'}.
     * A low-surrogate is also known as a <i>trailing-surrogate</i>.
     *
     * @since 1.5
     */
    public static final char MIN_LOW_SURROGATE  = '\uDC00';

    /**
     * The maximum value of a
     * <a href="http://www.unicode.org/glossary/#low_surrogate_code_unit">
     * Unicode low-surrogate code unit</a>
     * in the UTF-16 encoding, constant {@code '\u005CuDFFF'}.
     * A low-surrogate is also known as a <i>trailing-surrogate</i>.
     *
     * @since 1.5
     */
    public static final char MAX_LOW_SURROGATE  = '\uDFFF';

    /**
     * The minimum value of a Unicode surrogate code unit in the
     * UTF-16 encoding, constant {@code '\u005CuD800'}.
     *
     * @since 1.5
     */
    public static final char MIN_SURROGATE = MIN_HIGH_SURROGATE;

    /**
     * The maximum value of a Unicode surrogate code unit in the
     * UTF-16 encoding, constant {@code '\u005CuDFFF'}.
     *
     * @since 1.5
     */
    public static final char MAX_SURROGATE = MAX_LOW_SURROGATE;

    /**
     * The minimum value of a
     * <a href="http://www.unicode.org/glossary/#supplementary_code_point">
     * Unicode supplementary code point</a>, constant {@code U+10000}.
     *
     * @since 1.5
     */
    public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x010000;

    /**
     * The minimum value of a
     * <a href="http://www.unicode.org/glossary/#code_point">
     * Unicode code point</a>, constant {@code U+0000}.
     *
     * @since 1.5
     */
    public static final int MIN_CODE_POINT = 0x000000;

    /**
     * The maximum value of a
     * <a href="http://www.unicode.org/glossary/#code_point">
     * Unicode code point</a>, constant {@code U+10FFFF}.
     *
     * @since 1.5
     */
    public static final int MAX_CODE_POINT = 0X10FFFF;

    /**
     * Returns an {@link Optional} containing the nominal descriptor for this
     * instance.
     *
     * @return an {@link Optional} describing the {@linkplain Character} instance
     * @since 15
     */
    @Override
    public Optional<DynamicConstantDesc<Character>> describeConstable() {
        return Optional.of(DynamicConstantDesc.ofNamed(BSM_EXPLICIT_CAST, DEFAULT_NAME, CD_char, (int) value));
    }

    /**
     * Instances of this class represent particular subsets of the Unicode
     * character set.  The only family of subsets defined in the
     * {@code Character} class is {@link Character.UnicodeBlock}.
     * Other portions of the Java API may define other subsets for their
     * own purposes.
     *
     * @since 1.2
     */
    public static class Subset  {

        private String name;

        /**
         * Constructs a new {@code Subset} instance.
         *
         * @param  name  The name of this subset
         * @throws NullPointerException if name is {@code null}
         */
        protected Subset(String name) {
            if (name == null) {
                throw new NullPointerException("name");
            }
            this.name = name;
        }

        /**
         * Compares two {@code Subset} objects for equality.
         * This method returns {@code true} if and only if
         * {@code this} and the argument refer to the same
         * object; since this method is {@code final}, this
         * guarantee holds for all subclasses.
         */
        public final boolean equals(Object obj) {
            return (this == obj);
        }

        /**
         * Returns the standard hash code as defined by the
         * {@link Object#hashCode} method.  This method
         * is {@code final} in order to ensure that the
         * {@code equals} and {@code hashCode} methods will
         * be consistent in all subclasses.
         */
        public final int hashCode() {
            return super.hashCode();
        }

        /**
         * Returns the name of this subset.
         */
        public final String toString() {
            return name;
        }
    }

    // See http://www.unicode.org/Public/UNIDATA/Blocks.txt
    // for the latest specification of Unicode Blocks.

    /**
     * A family of character subsets representing the character blocks in the
     * Unicode specification. Character blocks generally define characters
     * used for a specific script or purpose. A character is contained by
     * at most one Unicode block.
     *
     * @since 1.2
     */
    public static final class UnicodeBlock extends Subset {
        /**
         * 684 - the expected number of entities
         * 0.75 - the default load factor of HashMap
         */
        private static final int NUM_ENTITIES = 684;
        private static Map<String, UnicodeBlock> map =
                new HashMap<>((int)(NUM_ENTITIES / 0.75f + 1.0f));

        /**
         * Creates a UnicodeBlock with the given identifier name.
         * This name must be the same as the block identifier.
         */
        private UnicodeBlock(String idName) {
            super(idName);
            map.put(idName, this);
        }

        /**
         * Creates a UnicodeBlock with the given identifier name and
         * alias name.
         */
        private UnicodeBlock(String idName, String alias) {
            this(idName);
            map.put(alias, this);
        }

        /**
         * Creates a UnicodeBlock with the given identifier name and
         * alias names.
         */
        private UnicodeBlock(String idName, String... aliases) {
            this(idName);
            for (String alias : aliases)
                map.put(alias, this);
        }

        /**
         * Constant for the "Basic Latin" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock  BASIC_LATIN =
            new UnicodeBlock("BASIC_LATIN",
                             "BASIC LATIN",
                             "BASICLATIN");

        /**
         * Constant for the "Latin-1 Supplement" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock LATIN_1_SUPPLEMENT =
            new UnicodeBlock("LATIN_1_SUPPLEMENT",
                             "LATIN-1 SUPPLEMENT",
                             "LATIN-1SUPPLEMENT");

        /**
         * Constant for the "Latin Extended-A" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock LATIN_EXTENDED_A =
            new UnicodeBlock("LATIN_EXTENDED_A",
                             "LATIN EXTENDED-A",
                             "LATINEXTENDED-A");

        /**
         * Constant for the "Latin Extended-B" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock LATIN_EXTENDED_B =
            new UnicodeBlock("LATIN_EXTENDED_B",
                             "LATIN EXTENDED-B",
                             "LATINEXTENDED-B");

        /**
         * Constant for the "IPA Extensions" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock IPA_EXTENSIONS =
            new UnicodeBlock("IPA_EXTENSIONS",
                             "IPA EXTENSIONS",
                             "IPAEXTENSIONS");

        /**
         * Constant for the "Spacing Modifier Letters" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock SPACING_MODIFIER_LETTERS =
            new UnicodeBlock("SPACING_MODIFIER_LETTERS",
                             "SPACING MODIFIER LETTERS",
                             "SPACINGMODIFIERLETTERS");

        /**
         * Constant for the "Combining Diacritical Marks" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock COMBINING_DIACRITICAL_MARKS =
            new UnicodeBlock("COMBINING_DIACRITICAL_MARKS",
                             "COMBINING DIACRITICAL MARKS",
                             "COMBININGDIACRITICALMARKS");

        /**
         * Constant for the "Greek and Coptic" Unicode character block.
         * <p>
         * This block was previously known as the "Greek" block.
         *
         * @since 1.2
         */
        public static final UnicodeBlock GREEK =
            new UnicodeBlock("GREEK",
                             "GREEK AND COPTIC",
                             "GREEKANDCOPTIC");

        /**
         * Constant for the "Cyrillic" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock CYRILLIC =
            new UnicodeBlock("CYRILLIC");

        /**
         * Constant for the "Armenian" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock ARMENIAN =
            new UnicodeBlock("ARMENIAN");

        /**
         * Constant for the "Hebrew" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock HEBREW =
            new UnicodeBlock("HEBREW");

        /**
         * Constant for the "Arabic" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock ARABIC =
            new UnicodeBlock("ARABIC");

        /**
         * Constant for the "Devanagari" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock DEVANAGARI =
            new UnicodeBlock("DEVANAGARI");

        /**
         * Constant for the "Bengali" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock BENGALI =
            new UnicodeBlock("BENGALI");

        /**
         * Constant for the "Gurmukhi" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock GURMUKHI =
            new UnicodeBlock("GURMUKHI");

        /**
         * Constant for the "Gujarati" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock GUJARATI =
            new UnicodeBlock("GUJARATI");

        /**
         * Constant for the "Oriya" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock ORIYA =
            new UnicodeBlock("ORIYA");

        /**
         * Constant for the "Tamil" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock TAMIL =
            new UnicodeBlock("TAMIL");

        /**
         * Constant for the "Telugu" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock TELUGU =
            new UnicodeBlock("TELUGU");

        /**
         * Constant for the "Kannada" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock KANNADA =
            new UnicodeBlock("KANNADA");

        /**
         * Constant for the "Malayalam" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock MALAYALAM =
            new UnicodeBlock("MALAYALAM");

        /**
         * Constant for the "Thai" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock THAI =
            new UnicodeBlock("THAI");

        /**
         * Constant for the "Lao" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock LAO =
            new UnicodeBlock("LAO");

        /**
         * Constant for the "Tibetan" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock TIBETAN =
            new UnicodeBlock("TIBETAN");

        /**
         * Constant for the "Georgian" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock GEORGIAN =
            new UnicodeBlock("GEORGIAN");

        /**
         * Constant for the "Hangul Jamo" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock HANGUL_JAMO =
            new UnicodeBlock("HANGUL_JAMO",
                             "HANGUL JAMO",
                             "HANGULJAMO");

        /**
         * Constant for the "Latin Extended Additional" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock LATIN_EXTENDED_ADDITIONAL =
            new UnicodeBlock("LATIN_EXTENDED_ADDITIONAL",
                             "LATIN EXTENDED ADDITIONAL",
                             "LATINEXTENDEDADDITIONAL");

        /**
         * Constant for the "Greek Extended" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock GREEK_EXTENDED =
            new UnicodeBlock("GREEK_EXTENDED",
                             "GREEK EXTENDED",
                             "GREEKEXTENDED");

        /**
         * Constant for the "General Punctuation" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock GENERAL_PUNCTUATION =
            new UnicodeBlock("GENERAL_PUNCTUATION",
                             "GENERAL PUNCTUATION",
                             "GENERALPUNCTUATION");

        /**
         * Constant for the "Superscripts and Subscripts" Unicode character
         * block.
         * @since 1.2
         */
        public static final UnicodeBlock SUPERSCRIPTS_AND_SUBSCRIPTS =
            new UnicodeBlock("SUPERSCRIPTS_AND_SUBSCRIPTS",
                             "SUPERSCRIPTS AND SUBSCRIPTS",
                             "SUPERSCRIPTSANDSUBSCRIPTS");

        /**
         * Constant for the "Currency Symbols" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock CURRENCY_SYMBOLS =
            new UnicodeBlock("CURRENCY_SYMBOLS",
                             "CURRENCY SYMBOLS",
                             "CURRENCYSYMBOLS");

        /**
         * Constant for the "Combining Diacritical Marks for Symbols" Unicode
         * character block.
         * <p>
         * This block was previously known as "Combining Marks for Symbols".
         * @since 1.2
         */
        public static final UnicodeBlock COMBINING_MARKS_FOR_SYMBOLS =
            new UnicodeBlock("COMBINING_MARKS_FOR_SYMBOLS",
                             "COMBINING DIACRITICAL MARKS FOR SYMBOLS",
                             "COMBININGDIACRITICALMARKSFORSYMBOLS",
                             "COMBINING MARKS FOR SYMBOLS",
                             "COMBININGMARKSFORSYMBOLS");

        /**
         * Constant for the "Letterlike Symbols" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock LETTERLIKE_SYMBOLS =
            new UnicodeBlock("LETTERLIKE_SYMBOLS",
                             "LETTERLIKE SYMBOLS",
                             "LETTERLIKESYMBOLS");

        /**
         * Constant for the "Number Forms" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock NUMBER_FORMS =
            new UnicodeBlock("NUMBER_FORMS",
                             "NUMBER FORMS",
                             "NUMBERFORMS");

        /**
         * Constant for the "Arrows" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock ARROWS =
            new UnicodeBlock("ARROWS");

        /**
         * Constant for the "Mathematical Operators" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock MATHEMATICAL_OPERATORS =
            new UnicodeBlock("MATHEMATICAL_OPERATORS",
                             "MATHEMATICAL OPERATORS",
                             "MATHEMATICALOPERATORS");

        /**
         * Constant for the "Miscellaneous Technical" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock MISCELLANEOUS_TECHNICAL =
            new UnicodeBlock("MISCELLANEOUS_TECHNICAL",
                             "MISCELLANEOUS TECHNICAL",
                             "MISCELLANEOUSTECHNICAL");

        /**
         * Constant for the "Control Pictures" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock CONTROL_PICTURES =
            new UnicodeBlock("CONTROL_PICTURES",
                             "CONTROL PICTURES",
                             "CONTROLPICTURES");

        /**
         * Constant for the "Optical Character Recognition" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock OPTICAL_CHARACTER_RECOGNITION =
            new UnicodeBlock("OPTICAL_CHARACTER_RECOGNITION",
                             "OPTICAL CHARACTER RECOGNITION",
                             "OPTICALCHARACTERRECOGNITION");

        /**
         * Constant for the "Enclosed Alphanumerics" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock ENCLOSED_ALPHANUMERICS =
            new UnicodeBlock("ENCLOSED_ALPHANUMERICS",
                             "ENCLOSED ALPHANUMERICS",
                             "ENCLOSEDALPHANUMERICS");

        /**
         * Constant for the "Box Drawing" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock BOX_DRAWING =
            new UnicodeBlock("BOX_DRAWING",
                             "BOX DRAWING",
                             "BOXDRAWING");

        /**
         * Constant for the "Block Elements" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock BLOCK_ELEMENTS =
            new UnicodeBlock("BLOCK_ELEMENTS",
                             "BLOCK ELEMENTS",
                             "BLOCKELEMENTS");

        /**
         * Constant for the "Geometric Shapes" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock GEOMETRIC_SHAPES =
            new UnicodeBlock("GEOMETRIC_SHAPES",
                             "GEOMETRIC SHAPES",
                             "GEOMETRICSHAPES");

        /**
         * Constant for the "Miscellaneous Symbols" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock MISCELLANEOUS_SYMBOLS =
            new UnicodeBlock("MISCELLANEOUS_SYMBOLS",
                             "MISCELLANEOUS SYMBOLS",
                             "MISCELLANEOUSSYMBOLS");

        /**
         * Constant for the "Dingbats" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock DINGBATS =
            new UnicodeBlock("DINGBATS");

        /**
         * Constant for the "CJK Symbols and Punctuation" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock CJK_SYMBOLS_AND_PUNCTUATION =
            new UnicodeBlock("CJK_SYMBOLS_AND_PUNCTUATION",
                             "CJK SYMBOLS AND PUNCTUATION",
                             "CJKSYMBOLSANDPUNCTUATION");

        /**
         * Constant for the "Hiragana" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock HIRAGANA =
            new UnicodeBlock("HIRAGANA");

        /**
         * Constant for the "Katakana" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock KATAKANA =
            new UnicodeBlock("KATAKANA");

        /**
         * Constant for the "Bopomofo" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock BOPOMOFO =
            new UnicodeBlock("BOPOMOFO");

        /**
         * Constant for the "Hangul Compatibility Jamo" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock HANGUL_COMPATIBILITY_JAMO =
            new UnicodeBlock("HANGUL_COMPATIBILITY_JAMO",
                             "HANGUL COMPATIBILITY JAMO",
                             "HANGULCOMPATIBILITYJAMO");

        /**
         * Constant for the "Kanbun" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock KANBUN =
            new UnicodeBlock("KANBUN");

        /**
         * Constant for the "Enclosed CJK Letters and Months" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock ENCLOSED_CJK_LETTERS_AND_MONTHS =
            new UnicodeBlock("ENCLOSED_CJK_LETTERS_AND_MONTHS",
                             "ENCLOSED CJK LETTERS AND MONTHS",
                             "ENCLOSEDCJKLETTERSANDMONTHS");

        /**
         * Constant for the "CJK Compatibility" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock CJK_COMPATIBILITY =
            new UnicodeBlock("CJK_COMPATIBILITY",
                             "CJK COMPATIBILITY",
                             "CJKCOMPATIBILITY");

        /**
         * Constant for the "CJK Unified Ideographs" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS =
            new UnicodeBlock("CJK_UNIFIED_IDEOGRAPHS",
                             "CJK UNIFIED IDEOGRAPHS",
                             "CJKUNIFIEDIDEOGRAPHS");

        /**
         * Constant for the "Hangul Syllables" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock HANGUL_SYLLABLES =
            new UnicodeBlock("HANGUL_SYLLABLES",
                             "HANGUL SYLLABLES",
                             "HANGULSYLLABLES");

        /**
         * Constant for the "Private Use Area" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock PRIVATE_USE_AREA =
            new UnicodeBlock("PRIVATE_USE_AREA",
                             "PRIVATE USE AREA",
                             "PRIVATEUSEAREA");

        /**
         * Constant for the "CJK Compatibility Ideographs" Unicode character
         * block.
         * @since 1.2
         */
        public static final UnicodeBlock CJK_COMPATIBILITY_IDEOGRAPHS =
            new UnicodeBlock("CJK_COMPATIBILITY_IDEOGRAPHS",
                             "CJK COMPATIBILITY IDEOGRAPHS",
                             "CJKCOMPATIBILITYIDEOGRAPHS");

        /**
         * Constant for the "Alphabetic Presentation Forms" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock ALPHABETIC_PRESENTATION_FORMS =
            new UnicodeBlock("ALPHABETIC_PRESENTATION_FORMS",
                             "ALPHABETIC PRESENTATION FORMS",
                             "ALPHABETICPRESENTATIONFORMS");

        /**
         * Constant for the "Arabic Presentation Forms-A" Unicode character
         * block.
         * @since 1.2
         */
        public static final UnicodeBlock ARABIC_PRESENTATION_FORMS_A =
            new UnicodeBlock("ARABIC_PRESENTATION_FORMS_A",
                             "ARABIC PRESENTATION FORMS-A",
                             "ARABICPRESENTATIONFORMS-A");

        /**
         * Constant for the "Combining Half Marks" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock COMBINING_HALF_MARKS =
            new UnicodeBlock("COMBINING_HALF_MARKS",
                             "COMBINING HALF MARKS",
                             "COMBININGHALFMARKS");

        /**
         * Constant for the "CJK Compatibility Forms" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock CJK_COMPATIBILITY_FORMS =
            new UnicodeBlock("CJK_COMPATIBILITY_FORMS",
                             "CJK COMPATIBILITY FORMS",
                             "CJKCOMPATIBILITYFORMS");

        /**
         * Constant for the "Small Form Variants" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock SMALL_FORM_VARIANTS =
            new UnicodeBlock("SMALL_FORM_VARIANTS",
                             "SMALL FORM VARIANTS",
                             "SMALLFORMVARIANTS");

        /**
         * Constant for the "Arabic Presentation Forms-B" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock ARABIC_PRESENTATION_FORMS_B =
            new UnicodeBlock("ARABIC_PRESENTATION_FORMS_B",
                             "ARABIC PRESENTATION FORMS-B",
                             "ARABICPRESENTATIONFORMS-B");

        /**
         * Constant for the "Halfwidth and Fullwidth Forms" Unicode character
         * block.
         * @since 1.2
         */
        public static final UnicodeBlock HALFWIDTH_AND_FULLWIDTH_FORMS =
            new UnicodeBlock("HALFWIDTH_AND_FULLWIDTH_FORMS",
                             "HALFWIDTH AND FULLWIDTH FORMS",
                             "HALFWIDTHANDFULLWIDTHFORMS");

        /**
         * Constant for the "Specials" Unicode character block.
         * @since 1.2
         */
        public static final UnicodeBlock SPECIALS =
            new UnicodeBlock("SPECIALS");

        /**
         * @deprecated
         * Instead of {@code SURROGATES_AREA}, use {@link #HIGH_SURROGATES},
         * {@link #HIGH_PRIVATE_USE_SURROGATES}, and {@link #LOW_SURROGATES}.
         * These constants match the block definitions of the Unicode Standard.
         * The {@link #of(char)} and {@link #of(int)} methods return the
         * standard constants.
         */
        @Deprecated(since="1.5")
        public static final UnicodeBlock SURROGATES_AREA =
            new UnicodeBlock("SURROGATES_AREA");

        /**
         * Constant for the "Syriac" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock SYRIAC =
            new UnicodeBlock("SYRIAC");

        /**
         * Constant for the "Thaana" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock THAANA =
            new UnicodeBlock("THAANA");

        /**
         * Constant for the "Sinhala" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock SINHALA =
            new UnicodeBlock("SINHALA");

        /**
         * Constant for the "Myanmar" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock MYANMAR =
            new UnicodeBlock("MYANMAR");

        /**
         * Constant for the "Ethiopic" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock ETHIOPIC =
            new UnicodeBlock("ETHIOPIC");

        /**
         * Constant for the "Cherokee" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock CHEROKEE =
            new UnicodeBlock("CHEROKEE");

        /**
         * Constant for the "Unified Canadian Aboriginal Syllabics" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS =
            new UnicodeBlock("UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS",
                             "UNIFIED CANADIAN ABORIGINAL SYLLABICS",
                             "UNIFIEDCANADIANABORIGINALSYLLABICS");

        /**
         * Constant for the "Ogham" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock OGHAM =
            new UnicodeBlock("OGHAM");

        /**
         * Constant for the "Runic" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock RUNIC =
            new UnicodeBlock("RUNIC");

        /**
         * Constant for the "Khmer" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock KHMER =
            new UnicodeBlock("KHMER");

        /**
         * Constant for the "Mongolian" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock MONGOLIAN =
            new UnicodeBlock("MONGOLIAN");

        /**
         * Constant for the "Braille Patterns" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock BRAILLE_PATTERNS =
            new UnicodeBlock("BRAILLE_PATTERNS",
                             "BRAILLE PATTERNS",
                             "BRAILLEPATTERNS");

        /**
         * Constant for the "CJK Radicals Supplement" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock CJK_RADICALS_SUPPLEMENT =
            new UnicodeBlock("CJK_RADICALS_SUPPLEMENT",
                             "CJK RADICALS SUPPLEMENT",
                             "CJKRADICALSSUPPLEMENT");

        /**
         * Constant for the "Kangxi Radicals" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock KANGXI_RADICALS =
            new UnicodeBlock("KANGXI_RADICALS",
                             "KANGXI RADICALS",
                             "KANGXIRADICALS");

        /**
         * Constant for the "Ideographic Description Characters" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock IDEOGRAPHIC_DESCRIPTION_CHARACTERS =
            new UnicodeBlock("IDEOGRAPHIC_DESCRIPTION_CHARACTERS",
                             "IDEOGRAPHIC DESCRIPTION CHARACTERS",
                             "IDEOGRAPHICDESCRIPTIONCHARACTERS");

        /**
         * Constant for the "Bopomofo Extended" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock BOPOMOFO_EXTENDED =
            new UnicodeBlock("BOPOMOFO_EXTENDED",
                             "BOPOMOFO EXTENDED",
                             "BOPOMOFOEXTENDED");

        /**
         * Constant for the "CJK Unified Ideographs Extension A" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A =
            new UnicodeBlock("CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A",
                             "CJK UNIFIED IDEOGRAPHS EXTENSION A",
                             "CJKUNIFIEDIDEOGRAPHSEXTENSIONA");

        /**
         * Constant for the "Yi Syllables" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock YI_SYLLABLES =
            new UnicodeBlock("YI_SYLLABLES",
                             "YI SYLLABLES",
                             "YISYLLABLES");

        /**
         * Constant for the "Yi Radicals" Unicode character block.
         * @since 1.4
         */
        public static final UnicodeBlock YI_RADICALS =
            new UnicodeBlock("YI_RADICALS",
                             "YI RADICALS",
                             "YIRADICALS");

        /**
         * Constant for the "Cyrillic Supplement" Unicode character block.
         * This block was previously known as the "Cyrillic Supplementary" block.
         * @since 1.5
         */
        public static final UnicodeBlock CYRILLIC_SUPPLEMENTARY =
            new UnicodeBlock("CYRILLIC_SUPPLEMENTARY",
                             "CYRILLIC SUPPLEMENTARY",
                             "CYRILLICSUPPLEMENTARY",
                             "CYRILLIC SUPPLEMENT",
                             "CYRILLICSUPPLEMENT");

        /**
         * Constant for the "Tagalog" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock TAGALOG =
            new UnicodeBlock("TAGALOG");

        /**
         * Constant for the "Hanunoo" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock HANUNOO =
            new UnicodeBlock("HANUNOO");

        /**
         * Constant for the "Buhid" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock BUHID =
            new UnicodeBlock("BUHID");

        /**
         * Constant for the "Tagbanwa" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock TAGBANWA =
            new UnicodeBlock("TAGBANWA");

        /**
         * Constant for the "Limbu" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock LIMBU =
            new UnicodeBlock("LIMBU");

        /**
         * Constant for the "Tai Le" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock TAI_LE =
            new UnicodeBlock("TAI_LE",
                             "TAI LE",
                             "TAILE");

        /**
         * Constant for the "Khmer Symbols" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock KHMER_SYMBOLS =
            new UnicodeBlock("KHMER_SYMBOLS",
                             "KHMER SYMBOLS",
                             "KHMERSYMBOLS");

        /**
         * Constant for the "Phonetic Extensions" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock PHONETIC_EXTENSIONS =
            new UnicodeBlock("PHONETIC_EXTENSIONS",
                             "PHONETIC EXTENSIONS",
                             "PHONETICEXTENSIONS");

        /**
         * Constant for the "Miscellaneous Mathematical Symbols-A" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A =
            new UnicodeBlock("MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A",
                             "MISCELLANEOUS MATHEMATICAL SYMBOLS-A",
                             "MISCELLANEOUSMATHEMATICALSYMBOLS-A");

        /**
         * Constant for the "Supplemental Arrows-A" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock SUPPLEMENTAL_ARROWS_A =
            new UnicodeBlock("SUPPLEMENTAL_ARROWS_A",
                             "SUPPLEMENTAL ARROWS-A",
                             "SUPPLEMENTALARROWS-A");

        /**
         * Constant for the "Supplemental Arrows-B" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock SUPPLEMENTAL_ARROWS_B =
            new UnicodeBlock("SUPPLEMENTAL_ARROWS_B",
                             "SUPPLEMENTAL ARROWS-B",
                             "SUPPLEMENTALARROWS-B");

        /**
         * Constant for the "Miscellaneous Mathematical Symbols-B" Unicode
         * character block.
         * @since 1.5
         */
        public static final UnicodeBlock MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B =
            new UnicodeBlock("MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B",
                             "MISCELLANEOUS MATHEMATICAL SYMBOLS-B",
                             "MISCELLANEOUSMATHEMATICALSYMBOLS-B");

        /**
         * Constant for the "Supplemental Mathematical Operators" Unicode
         * character block.
         * @since 1.5
         */
        public static final UnicodeBlock SUPPLEMENTAL_MATHEMATICAL_OPERATORS =
            new UnicodeBlock("SUPPLEMENTAL_MATHEMATICAL_OPERATORS",
                             "SUPPLEMENTAL MATHEMATICAL OPERATORS",
                             "SUPPLEMENTALMATHEMATICALOPERATORS");

        /**
         * Constant for the "Miscellaneous Symbols and Arrows" Unicode character
         * block.
         * @since 1.5
         */
        public static final UnicodeBlock MISCELLANEOUS_SYMBOLS_AND_ARROWS =
            new UnicodeBlock("MISCELLANEOUS_SYMBOLS_AND_ARROWS",
                             "MISCELLANEOUS SYMBOLS AND ARROWS",
                             "MISCELLANEOUSSYMBOLSANDARROWS");

        /**
         * Constant for the "Katakana Phonetic Extensions" Unicode character
         * block.
         * @since 1.5
         */
        public static final UnicodeBlock KATAKANA_PHONETIC_EXTENSIONS =
            new UnicodeBlock("KATAKANA_PHONETIC_EXTENSIONS",
                             "KATAKANA PHONETIC EXTENSIONS",
                             "KATAKANAPHONETICEXTENSIONS");

        /**
         * Constant for the "Yijing Hexagram Symbols" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock YIJING_HEXAGRAM_SYMBOLS =
            new UnicodeBlock("YIJING_HEXAGRAM_SYMBOLS",
                             "YIJING HEXAGRAM SYMBOLS",
                             "YIJINGHEXAGRAMSYMBOLS");

        /**
         * Constant for the "Variation Selectors" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock VARIATION_SELECTORS =
            new UnicodeBlock("VARIATION_SELECTORS",
                             "VARIATION SELECTORS",
                             "VARIATIONSELECTORS");

        /**
         * Constant for the "Linear B Syllabary" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock LINEAR_B_SYLLABARY =
            new UnicodeBlock("LINEAR_B_SYLLABARY",
                             "LINEAR B SYLLABARY",
                             "LINEARBSYLLABARY");

        /**
         * Constant for the "Linear B Ideograms" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock LINEAR_B_IDEOGRAMS =
            new UnicodeBlock("LINEAR_B_IDEOGRAMS",
                             "LINEAR B IDEOGRAMS",
                             "LINEARBIDEOGRAMS");

        /**
         * Constant for the "Aegean Numbers" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock AEGEAN_NUMBERS =
            new UnicodeBlock("AEGEAN_NUMBERS",
                             "AEGEAN NUMBERS",
                             "AEGEANNUMBERS");

        /**
         * Constant for the "Old Italic" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock OLD_ITALIC =
            new UnicodeBlock("OLD_ITALIC",
                             "OLD ITALIC",
                             "OLDITALIC");

        /**
         * Constant for the "Gothic" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock GOTHIC =
            new UnicodeBlock("GOTHIC");

        /**
         * Constant for the "Ugaritic" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock UGARITIC =
            new UnicodeBlock("UGARITIC");

        /**
         * Constant for the "Deseret" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock DESERET =
            new UnicodeBlock("DESERET");

        /**
         * Constant for the "Shavian" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock SHAVIAN =
            new UnicodeBlock("SHAVIAN");

        /**
         * Constant for the "Osmanya" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock OSMANYA =
            new UnicodeBlock("OSMANYA");

        /**
         * Constant for the "Cypriot Syllabary" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock CYPRIOT_SYLLABARY =
            new UnicodeBlock("CYPRIOT_SYLLABARY",
                             "CYPRIOT SYLLABARY",
                             "CYPRIOTSYLLABARY");

        /**
         * Constant for the "Byzantine Musical Symbols" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock BYZANTINE_MUSICAL_SYMBOLS =
            new UnicodeBlock("BYZANTINE_MUSICAL_SYMBOLS",
                             "BYZANTINE MUSICAL SYMBOLS",
                             "BYZANTINEMUSICALSYMBOLS");

        /**
         * Constant for the "Musical Symbols" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock MUSICAL_SYMBOLS =
            new UnicodeBlock("MUSICAL_SYMBOLS",
                             "MUSICAL SYMBOLS",
                             "MUSICALSYMBOLS");

        /**
         * Constant for the "Tai Xuan Jing Symbols" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock TAI_XUAN_JING_SYMBOLS =
            new UnicodeBlock("TAI_XUAN_JING_SYMBOLS",
                             "TAI XUAN JING SYMBOLS",
                             "TAIXUANJINGSYMBOLS");

        /**
         * Constant for the "Mathematical Alphanumeric Symbols" Unicode
         * character block.
         * @since 1.5
         */
        public static final UnicodeBlock MATHEMATICAL_ALPHANUMERIC_SYMBOLS =
            new UnicodeBlock("MATHEMATICAL_ALPHANUMERIC_SYMBOLS",
                             "MATHEMATICAL ALPHANUMERIC SYMBOLS",
                             "MATHEMATICALALPHANUMERICSYMBOLS");

        /**
         * Constant for the "CJK Unified Ideographs Extension B" Unicode
         * character block.
         * @since 1.5
         */
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B =
            new UnicodeBlock("CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B",
                             "CJK UNIFIED IDEOGRAPHS EXTENSION B",
                             "CJKUNIFIEDIDEOGRAPHSEXTENSIONB");

        /**
         * Constant for the "CJK Compatibility Ideographs Supplement" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT =
            new UnicodeBlock("CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT",
                             "CJK COMPATIBILITY IDEOGRAPHS SUPPLEMENT",
                             "CJKCOMPATIBILITYIDEOGRAPHSSUPPLEMENT");

        /**
         * Constant for the "Tags" Unicode character block.
         * @since 1.5
         */
        public static final UnicodeBlock TAGS =
            new UnicodeBlock("TAGS");

        /**
         * Constant for the "Variation Selectors Supplement" Unicode character
         * block.
         * @since 1.5
         */
        public static final UnicodeBlock VARIATION_SELECTORS_SUPPLEMENT =
            new UnicodeBlock("VARIATION_SELECTORS_SUPPLEMENT",
                             "VARIATION SELECTORS SUPPLEMENT",
                             "VARIATIONSELECTORSSUPPLEMENT");

        /**
         * Constant for the "Supplementary Private Use Area-A" Unicode character
         * block.
         * @since 1.5
         */
        public static final UnicodeBlock SUPPLEMENTARY_PRIVATE_USE_AREA_A =
            new UnicodeBlock("SUPPLEMENTARY_PRIVATE_USE_AREA_A",
                             "SUPPLEMENTARY PRIVATE USE AREA-A",
                             "SUPPLEMENTARYPRIVATEUSEAREA-A");

        /**
         * Constant for the "Supplementary Private Use Area-B" Unicode character
         * block.
         * @since 1.5
         */
        public static final UnicodeBlock SUPPLEMENTARY_PRIVATE_USE_AREA_B =
            new UnicodeBlock("SUPPLEMENTARY_PRIVATE_USE_AREA_B",
                             "SUPPLEMENTARY PRIVATE USE AREA-B",
                             "SUPPLEMENTARYPRIVATEUSEAREA-B");

        /**
         * Constant for the "High Surrogates" Unicode character block.
         * This block represents codepoint values in the high surrogate
         * range: U+D800 through U+DB7F
         *
         * @since 1.5
         */
        public static final UnicodeBlock HIGH_SURROGATES =
            new UnicodeBlock("HIGH_SURROGATES",
                             "HIGH SURROGATES",
                             "HIGHSURROGATES");

        /**
         * Constant for the "High Private Use Surrogates" Unicode character
         * block.
         * This block represents codepoint values in the private use high
         * surrogate range: U+DB80 through U+DBFF
         *
         * @since 1.5
         */
        public static final UnicodeBlock HIGH_PRIVATE_USE_SURROGATES =
            new UnicodeBlock("HIGH_PRIVATE_USE_SURROGATES",
                             "HIGH PRIVATE USE SURROGATES",
                             "HIGHPRIVATEUSESURROGATES");

        /**
         * Constant for the "Low Surrogates" Unicode character block.
         * This block represents codepoint values in the low surrogate
         * range: U+DC00 through U+DFFF
         *
         * @since 1.5
         */
        public static final UnicodeBlock LOW_SURROGATES =
            new UnicodeBlock("LOW_SURROGATES",
                             "LOW SURROGATES",
                             "LOWSURROGATES");

        /**
         * Constant for the "Arabic Supplement" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock ARABIC_SUPPLEMENT =
            new UnicodeBlock("ARABIC_SUPPLEMENT",
                             "ARABIC SUPPLEMENT",
                             "ARABICSUPPLEMENT");

        /**
         * Constant for the "NKo" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock NKO =
            new UnicodeBlock("NKO");

        /**
         * Constant for the "Samaritan" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock SAMARITAN =
            new UnicodeBlock("SAMARITAN");

        /**
         * Constant for the "Mandaic" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock MANDAIC =
            new UnicodeBlock("MANDAIC");

        /**
         * Constant for the "Ethiopic Supplement" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock ETHIOPIC_SUPPLEMENT =
            new UnicodeBlock("ETHIOPIC_SUPPLEMENT",
                             "ETHIOPIC SUPPLEMENT",
                             "ETHIOPICSUPPLEMENT");

        /**
         * Constant for the "Unified Canadian Aboriginal Syllabics Extended"
         * Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS_EXTENDED =
            new UnicodeBlock("UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS_EXTENDED",
                             "UNIFIED CANADIAN ABORIGINAL SYLLABICS EXTENDED",
                             "UNIFIEDCANADIANABORIGINALSYLLABICSEXTENDED");

        /**
         * Constant for the "New Tai Lue" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock NEW_TAI_LUE =
            new UnicodeBlock("NEW_TAI_LUE",
                             "NEW TAI LUE",
                             "NEWTAILUE");

        /**
         * Constant for the "Buginese" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock BUGINESE =
            new UnicodeBlock("BUGINESE");

        /**
         * Constant for the "Tai Tham" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock TAI_THAM =
            new UnicodeBlock("TAI_THAM",
                             "TAI THAM",
                             "TAITHAM");

        /**
         * Constant for the "Balinese" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock BALINESE =
            new UnicodeBlock("BALINESE");

        /**
         * Constant for the "Sundanese" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock SUNDANESE =
            new UnicodeBlock("SUNDANESE");

        /**
         * Constant for the "Batak" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock BATAK =
            new UnicodeBlock("BATAK");

        /**
         * Constant for the "Lepcha" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock LEPCHA =
            new UnicodeBlock("LEPCHA");

        /**
         * Constant for the "Ol Chiki" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock OL_CHIKI =
            new UnicodeBlock("OL_CHIKI",
                             "OL CHIKI",
                             "OLCHIKI");

        /**
         * Constant for the "Vedic Extensions" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock VEDIC_EXTENSIONS =
            new UnicodeBlock("VEDIC_EXTENSIONS",
                             "VEDIC EXTENSIONS",
                             "VEDICEXTENSIONS");

        /**
         * Constant for the "Phonetic Extensions Supplement" Unicode character
         * block.
         * @since 1.7
         */
        public static final UnicodeBlock PHONETIC_EXTENSIONS_SUPPLEMENT =
            new UnicodeBlock("PHONETIC_EXTENSIONS_SUPPLEMENT",
                             "PHONETIC EXTENSIONS SUPPLEMENT",
                             "PHONETICEXTENSIONSSUPPLEMENT");

        /**
         * Constant for the "Combining Diacritical Marks Supplement" Unicode
         * character block.
         * @since 1.7
         */
        public static final UnicodeBlock COMBINING_DIACRITICAL_MARKS_SUPPLEMENT =
            new UnicodeBlock("COMBINING_DIACRITICAL_MARKS_SUPPLEMENT",
                             "COMBINING DIACRITICAL MARKS SUPPLEMENT",
                             "COMBININGDIACRITICALMARKSSUPPLEMENT");

        /**
         * Constant for the "Glagolitic" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock GLAGOLITIC =
            new UnicodeBlock("GLAGOLITIC");

        /**
         * Constant for the "Latin Extended-C" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock LATIN_EXTENDED_C =
            new UnicodeBlock("LATIN_EXTENDED_C",
                             "LATIN EXTENDED-C",
                             "LATINEXTENDED-C");

        /**
         * Constant for the "Coptic" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock COPTIC =
            new UnicodeBlock("COPTIC");

        /**
         * Constant for the "Georgian Supplement" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock GEORGIAN_SUPPLEMENT =
            new UnicodeBlock("GEORGIAN_SUPPLEMENT",
                             "GEORGIAN SUPPLEMENT",
                             "GEORGIANSUPPLEMENT");

        /**
         * Constant for the "Tifinagh" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock TIFINAGH =
            new UnicodeBlock("TIFINAGH");

        /**
         * Constant for the "Ethiopic Extended" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock ETHIOPIC_EXTENDED =
            new UnicodeBlock("ETHIOPIC_EXTENDED",
                             "ETHIOPIC EXTENDED",
                             "ETHIOPICEXTENDED");

        /**
         * Constant for the "Cyrillic Extended-A" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock CYRILLIC_EXTENDED_A =
            new UnicodeBlock("CYRILLIC_EXTENDED_A",
                             "CYRILLIC EXTENDED-A",
                             "CYRILLICEXTENDED-A");

        /**
         * Constant for the "Supplemental Punctuation" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock SUPPLEMENTAL_PUNCTUATION =
            new UnicodeBlock("SUPPLEMENTAL_PUNCTUATION",
                             "SUPPLEMENTAL PUNCTUATION",
                             "SUPPLEMENTALPUNCTUATION");

        /**
         * Constant for the "CJK Strokes" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock CJK_STROKES =
            new UnicodeBlock("CJK_STROKES",
                             "CJK STROKES",
                             "CJKSTROKES");

        /**
         * Constant for the "Lisu" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock LISU =
            new UnicodeBlock("LISU");

        /**
         * Constant for the "Vai" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock VAI =
            new UnicodeBlock("VAI");

        /**
         * Constant for the "Cyrillic Extended-B" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock CYRILLIC_EXTENDED_B =
            new UnicodeBlock("CYRILLIC_EXTENDED_B",
                             "CYRILLIC EXTENDED-B",
                             "CYRILLICEXTENDED-B");

        /**
         * Constant for the "Bamum" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock BAMUM =
            new UnicodeBlock("BAMUM");

        /**
         * Constant for the "Modifier Tone Letters" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock MODIFIER_TONE_LETTERS =
            new UnicodeBlock("MODIFIER_TONE_LETTERS",
                             "MODIFIER TONE LETTERS",
                             "MODIFIERTONELETTERS");

        /**
         * Constant for the "Latin Extended-D" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock LATIN_EXTENDED_D =
            new UnicodeBlock("LATIN_EXTENDED_D",
                             "LATIN EXTENDED-D",
                             "LATINEXTENDED-D");

        /**
         * Constant for the "Syloti Nagri" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock SYLOTI_NAGRI =
            new UnicodeBlock("SYLOTI_NAGRI",
                             "SYLOTI NAGRI",
                             "SYLOTINAGRI");

        /**
         * Constant for the "Common Indic Number Forms" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock COMMON_INDIC_NUMBER_FORMS =
            new UnicodeBlock("COMMON_INDIC_NUMBER_FORMS",
                             "COMMON INDIC NUMBER FORMS",
                             "COMMONINDICNUMBERFORMS");

        /**
         * Constant for the "Phags-pa" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock PHAGS_PA =
            new UnicodeBlock("PHAGS_PA",
                             "PHAGS-PA");

        /**
         * Constant for the "Saurashtra" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock SAURASHTRA =
            new UnicodeBlock("SAURASHTRA");

        /**
         * Constant for the "Devanagari Extended" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock DEVANAGARI_EXTENDED =
            new UnicodeBlock("DEVANAGARI_EXTENDED",
                             "DEVANAGARI EXTENDED",
                             "DEVANAGARIEXTENDED");

        /**
         * Constant for the "Kayah Li" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock KAYAH_LI =
            new UnicodeBlock("KAYAH_LI",
                             "KAYAH LI",
                             "KAYAHLI");

        /**
         * Constant for the "Rejang" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock REJANG =
            new UnicodeBlock("REJANG");

        /**
         * Constant for the "Hangul Jamo Extended-A" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock HANGUL_JAMO_EXTENDED_A =
            new UnicodeBlock("HANGUL_JAMO_EXTENDED_A",
                             "HANGUL JAMO EXTENDED-A",
                             "HANGULJAMOEXTENDED-A");

        /**
         * Constant for the "Javanese" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock JAVANESE =
            new UnicodeBlock("JAVANESE");

        /**
         * Constant for the "Cham" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock CHAM =
            new UnicodeBlock("CHAM");

        /**
         * Constant for the "Myanmar Extended-A" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock MYANMAR_EXTENDED_A =
            new UnicodeBlock("MYANMAR_EXTENDED_A",
                             "MYANMAR EXTENDED-A",
                             "MYANMAREXTENDED-A");

        /**
         * Constant for the "Tai Viet" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock TAI_VIET =
            new UnicodeBlock("TAI_VIET",
                             "TAI VIET",
                             "TAIVIET");

        /**
         * Constant for the "Ethiopic Extended-A" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock ETHIOPIC_EXTENDED_A =
            new UnicodeBlock("ETHIOPIC_EXTENDED_A",
                             "ETHIOPIC EXTENDED-A",
                             "ETHIOPICEXTENDED-A");

        /**
         * Constant for the "Meetei Mayek" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock MEETEI_MAYEK =
            new UnicodeBlock("MEETEI_MAYEK",
                             "MEETEI MAYEK",
                             "MEETEIMAYEK");

        /**
         * Constant for the "Hangul Jamo Extended-B" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock HANGUL_JAMO_EXTENDED_B =
            new UnicodeBlock("HANGUL_JAMO_EXTENDED_B",
                             "HANGUL JAMO EXTENDED-B",
                             "HANGULJAMOEXTENDED-B");

        /**
         * Constant for the "Vertical Forms" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock VERTICAL_FORMS =
            new UnicodeBlock("VERTICAL_FORMS",
                             "VERTICAL FORMS",
                             "VERTICALFORMS");

        /**
         * Constant for the "Ancient Greek Numbers" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock ANCIENT_GREEK_NUMBERS =
            new UnicodeBlock("ANCIENT_GREEK_NUMBERS",
                             "ANCIENT GREEK NUMBERS",
                             "ANCIENTGREEKNUMBERS");

        /**
         * Constant for the "Ancient Symbols" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock ANCIENT_SYMBOLS =
            new UnicodeBlock("ANCIENT_SYMBOLS",
                             "ANCIENT SYMBOLS",
                             "ANCIENTSYMBOLS");

        /**
         * Constant for the "Phaistos Disc" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock PHAISTOS_DISC =
            new UnicodeBlock("PHAISTOS_DISC",
                             "PHAISTOS DISC",
                             "PHAISTOSDISC");

        /**
         * Constant for the "Lycian" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock LYCIAN =
            new UnicodeBlock("LYCIAN");

        /**
         * Constant for the "Carian" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock CARIAN =
            new UnicodeBlock("CARIAN");

        /**
         * Constant for the "Old Persian" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock OLD_PERSIAN =
            new UnicodeBlock("OLD_PERSIAN",
                             "OLD PERSIAN",
                             "OLDPERSIAN");

        /**
         * Constant for the "Imperial Aramaic" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock IMPERIAL_ARAMAIC =
            new UnicodeBlock("IMPERIAL_ARAMAIC",
                             "IMPERIAL ARAMAIC",
                             "IMPERIALARAMAIC");

        /**
         * Constant for the "Phoenician" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock PHOENICIAN =
            new UnicodeBlock("PHOENICIAN");

        /**
         * Constant for the "Lydian" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock LYDIAN =
            new UnicodeBlock("LYDIAN");

        /**
         * Constant for the "Kharoshthi" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock KHAROSHTHI =
            new UnicodeBlock("KHAROSHTHI");

        /**
         * Constant for the "Old South Arabian" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock OLD_SOUTH_ARABIAN =
            new UnicodeBlock("OLD_SOUTH_ARABIAN",
                             "OLD SOUTH ARABIAN",
                             "OLDSOUTHARABIAN");

        /**
         * Constant for the "Avestan" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock AVESTAN =
            new UnicodeBlock("AVESTAN");

        /**
         * Constant for the "Inscriptional Parthian" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock INSCRIPTIONAL_PARTHIAN =
            new UnicodeBlock("INSCRIPTIONAL_PARTHIAN",
                             "INSCRIPTIONAL PARTHIAN",
                             "INSCRIPTIONALPARTHIAN");

        /**
         * Constant for the "Inscriptional Pahlavi" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock INSCRIPTIONAL_PAHLAVI =
            new UnicodeBlock("INSCRIPTIONAL_PAHLAVI",
                             "INSCRIPTIONAL PAHLAVI",
                             "INSCRIPTIONALPAHLAVI");

        /**
         * Constant for the "Old Turkic" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock OLD_TURKIC =
            new UnicodeBlock("OLD_TURKIC",
                             "OLD TURKIC",
                             "OLDTURKIC");

        /**
         * Constant for the "Rumi Numeral Symbols" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock RUMI_NUMERAL_SYMBOLS =
            new UnicodeBlock("RUMI_NUMERAL_SYMBOLS",
                             "RUMI NUMERAL SYMBOLS",
                             "RUMINUMERALSYMBOLS");

        /**
         * Constant for the "Brahmi" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock BRAHMI =
            new UnicodeBlock("BRAHMI");

        /**
         * Constant for the "Kaithi" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock KAITHI =
            new UnicodeBlock("KAITHI");

        /**
         * Constant for the "Cuneiform" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock CUNEIFORM =
            new UnicodeBlock("CUNEIFORM");

        /**
         * Constant for the "Cuneiform Numbers and Punctuation" Unicode
         * character block.
         * @since 1.7
         */
        public static final UnicodeBlock CUNEIFORM_NUMBERS_AND_PUNCTUATION =
            new UnicodeBlock("CUNEIFORM_NUMBERS_AND_PUNCTUATION",
                             "CUNEIFORM NUMBERS AND PUNCTUATION",
                             "CUNEIFORMNUMBERSANDPUNCTUATION");

        /**
         * Constant for the "Egyptian Hieroglyphs" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock EGYPTIAN_HIEROGLYPHS =
            new UnicodeBlock("EGYPTIAN_HIEROGLYPHS",
                             "EGYPTIAN HIEROGLYPHS",
                             "EGYPTIANHIEROGLYPHS");

        /**
         * Constant for the "Bamum Supplement" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock BAMUM_SUPPLEMENT =
            new UnicodeBlock("BAMUM_SUPPLEMENT",
                             "BAMUM SUPPLEMENT",
                             "BAMUMSUPPLEMENT");

        /**
         * Constant for the "Kana Supplement" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock KANA_SUPPLEMENT =
            new UnicodeBlock("KANA_SUPPLEMENT",
                             "KANA SUPPLEMENT",
                             "KANASUPPLEMENT");

        /**
         * Constant for the "Ancient Greek Musical Notation" Unicode character
         * block.
         * @since 1.7
         */
        public static final UnicodeBlock ANCIENT_GREEK_MUSICAL_NOTATION =
            new UnicodeBlock("ANCIENT_GREEK_MUSICAL_NOTATION",
                             "ANCIENT GREEK MUSICAL NOTATION",
                             "ANCIENTGREEKMUSICALNOTATION");

        /**
         * Constant for the "Counting Rod Numerals" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock COUNTING_ROD_NUMERALS =
            new UnicodeBlock("COUNTING_ROD_NUMERALS",
                             "COUNTING ROD NUMERALS",
                             "COUNTINGRODNUMERALS");

        /**
         * Constant for the "Mahjong Tiles" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock MAHJONG_TILES =
            new UnicodeBlock("MAHJONG_TILES",
                             "MAHJONG TILES",
                             "MAHJONGTILES");

        /**
         * Constant for the "Domino Tiles" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock DOMINO_TILES =
            new UnicodeBlock("DOMINO_TILES",
                             "DOMINO TILES",
                             "DOMINOTILES");

        /**
         * Constant for the "Playing Cards" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock PLAYING_CARDS =
            new UnicodeBlock("PLAYING_CARDS",
                             "PLAYING CARDS",
                             "PLAYINGCARDS");

        /**
         * Constant for the "Enclosed Alphanumeric Supplement" Unicode character
         * block.
         * @since 1.7
         */
        public static final UnicodeBlock ENCLOSED_ALPHANUMERIC_SUPPLEMENT =
            new UnicodeBlock("ENCLOSED_ALPHANUMERIC_SUPPLEMENT",
                             "ENCLOSED ALPHANUMERIC SUPPLEMENT",
                             "ENCLOSEDALPHANUMERICSUPPLEMENT");

        /**
         * Constant for the "Enclosed Ideographic Supplement" Unicode character
         * block.
         * @since 1.7
         */
        public static final UnicodeBlock ENCLOSED_IDEOGRAPHIC_SUPPLEMENT =
            new UnicodeBlock("ENCLOSED_IDEOGRAPHIC_SUPPLEMENT",
                             "ENCLOSED IDEOGRAPHIC SUPPLEMENT",
                             "ENCLOSEDIDEOGRAPHICSUPPLEMENT");

        /**
         * Constant for the "Miscellaneous Symbols And Pictographs" Unicode
         * character block.
         * @since 1.7
         */
        public static final UnicodeBlock MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS =
            new UnicodeBlock("MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS",
                             "MISCELLANEOUS SYMBOLS AND PICTOGRAPHS",
                             "MISCELLANEOUSSYMBOLSANDPICTOGRAPHS");

        /**
         * Constant for the "Emoticons" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock EMOTICONS =
            new UnicodeBlock("EMOTICONS");

        /**
         * Constant for the "Transport And Map Symbols" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock TRANSPORT_AND_MAP_SYMBOLS =
            new UnicodeBlock("TRANSPORT_AND_MAP_SYMBOLS",
                             "TRANSPORT AND MAP SYMBOLS",
                             "TRANSPORTANDMAPSYMBOLS");

        /**
         * Constant for the "Alchemical Symbols" Unicode character block.
         * @since 1.7
         */
        public static final UnicodeBlock ALCHEMICAL_SYMBOLS =
            new UnicodeBlock("ALCHEMICAL_SYMBOLS",
                             "ALCHEMICAL SYMBOLS",
                             "ALCHEMICALSYMBOLS");

        /**
         * Constant for the "CJK Unified Ideographs Extension C" Unicode
         * character block.
         * @since 1.7
         */
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C =
            new UnicodeBlock("CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C",
                             "CJK UNIFIED IDEOGRAPHS EXTENSION C",
                             "CJKUNIFIEDIDEOGRAPHSEXTENSIONC");

        /**
         * Constant for the "CJK Unified Ideographs Extension D" Unicode
         * character block.
         * @since 1.7
         */
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D =
            new UnicodeBlock("CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D",
                             "CJK UNIFIED IDEOGRAPHS EXTENSION D",
                             "CJKUNIFIEDIDEOGRAPHSEXTENSIOND");

        /**
         * Constant for the "Arabic Extended-A" Unicode character block.
         * @since 1.8
         */
        public static final UnicodeBlock ARABIC_EXTENDED_A =
            new UnicodeBlock("ARABIC_EXTENDED_A",
                             "ARABIC EXTENDED-A",
                             "ARABICEXTENDED-A");

        /**
         * Constant for the "Sundanese Supplement" Unicode character block.
         * @since 1.8
         */
        public static final UnicodeBlock SUNDANESE_SUPPLEMENT =
            new UnicodeBlock("SUNDANESE_SUPPLEMENT",
                             "SUNDANESE SUPPLEMENT",
                             "SUNDANESESUPPLEMENT");

        /**
         * Constant for the "Meetei Mayek Extensions" Unicode character block.
         * @since 1.8
         */
        public static final UnicodeBlock MEETEI_MAYEK_EXTENSIONS =
            new UnicodeBlock("MEETEI_MAYEK_EXTENSIONS",
                             "MEETEI MAYEK EXTENSIONS",
                             "MEETEIMAYEKEXTENSIONS");

        /**
         * Constant for the "Meroitic Hieroglyphs" Unicode character block.
         * @since 1.8
         */
        public static final UnicodeBlock MEROITIC_HIEROGLYPHS =
            new UnicodeBlock("MEROITIC_HIEROGLYPHS",
                             "MEROITIC HIEROGLYPHS",
                             "MEROITICHIEROGLYPHS");

        /**
         * Constant for the "Meroitic Cursive" Unicode character block.
         * @since 1.8
         */
        public static final UnicodeBlock MEROITIC_CURSIVE =
            new UnicodeBlock("MEROITIC_CURSIVE",
                             "MEROITIC CURSIVE",
                             "MEROITICCURSIVE");

        /**
         * Constant for the "Sora Sompeng" Unicode character block.
         * @since 1.8
         */
        public static final UnicodeBlock SORA_SOMPENG =
            new UnicodeBlock("SORA_SOMPENG",
                             "SORA SOMPENG",
                             "SORASOMPENG");

        /**
         * Constant for the "Chakma" Unicode character block.
         * @since 1.8
         */
        public static final UnicodeBlock CHAKMA =
            new UnicodeBlock("CHAKMA");

        /**
         * Constant for the "Sharada" Unicode character block.
         * @since 1.8
         */
        public static final UnicodeBlock SHARADA =
            new UnicodeBlock("SHARADA");

        /**
         * Constant for the "Takri" Unicode character block.
         * @since 1.8
         */
        public static final UnicodeBlock TAKRI =
            new UnicodeBlock("TAKRI");

        /**
         * Constant for the "Miao" Unicode character block.
         * @since 1.8
         */
        public static final UnicodeBlock MIAO =
            new UnicodeBlock("MIAO");

        /**
         * Constant for the "Arabic Mathematical Alphabetic Symbols" Unicode
         * character block.
         * @since 1.8
         */
        public static final UnicodeBlock ARABIC_MATHEMATICAL_ALPHABETIC_SYMBOLS =
            new UnicodeBlock("ARABIC_MATHEMATICAL_ALPHABETIC_SYMBOLS",
                             "ARABIC MATHEMATICAL ALPHABETIC SYMBOLS",
                             "ARABICMATHEMATICALALPHABETICSYMBOLS");

        /**
         * Constant for the "Combining Diacritical Marks Extended" Unicode
         * character block.
         * @since 9
         */
        public static final UnicodeBlock COMBINING_DIACRITICAL_MARKS_EXTENDED =
            new UnicodeBlock("COMBINING_DIACRITICAL_MARKS_EXTENDED",
                             "COMBINING DIACRITICAL MARKS EXTENDED",
                             "COMBININGDIACRITICALMARKSEXTENDED");

        /**
         * Constant for the "Myanmar Extended-B" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock MYANMAR_EXTENDED_B =
            new UnicodeBlock("MYANMAR_EXTENDED_B",
                             "MYANMAR EXTENDED-B",
                             "MYANMAREXTENDED-B");

        /**
         * Constant for the "Latin Extended-E" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock LATIN_EXTENDED_E =
            new UnicodeBlock("LATIN_EXTENDED_E",
                             "LATIN EXTENDED-E",
                             "LATINEXTENDED-E");

        /**
         * Constant for the "Coptic Epact Numbers" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock COPTIC_EPACT_NUMBERS =
            new UnicodeBlock("COPTIC_EPACT_NUMBERS",
                             "COPTIC EPACT NUMBERS",
                             "COPTICEPACTNUMBERS");

        /**
         * Constant for the "Old Permic" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock OLD_PERMIC =
            new UnicodeBlock("OLD_PERMIC",
                             "OLD PERMIC",
                             "OLDPERMIC");

        /**
         * Constant for the "Elbasan" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock ELBASAN =
            new UnicodeBlock("ELBASAN");

        /**
         * Constant for the "Caucasian Albanian" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock CAUCASIAN_ALBANIAN =
            new UnicodeBlock("CAUCASIAN_ALBANIAN",
                             "CAUCASIAN ALBANIAN",
                             "CAUCASIANALBANIAN");

        /**
         * Constant for the "Linear A" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock LINEAR_A =
            new UnicodeBlock("LINEAR_A",
                             "LINEAR A",
                             "LINEARA");

        /**
         * Constant for the "Palmyrene" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock PALMYRENE =
            new UnicodeBlock("PALMYRENE");

        /**
         * Constant for the "Nabataean" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock NABATAEAN =
            new UnicodeBlock("NABATAEAN");

        /**
         * Constant for the "Old North Arabian" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock OLD_NORTH_ARABIAN =
            new UnicodeBlock("OLD_NORTH_ARABIAN",
                             "OLD NORTH ARABIAN",
                             "OLDNORTHARABIAN");

        /**
         * Constant for the "Manichaean" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock MANICHAEAN =
            new UnicodeBlock("MANICHAEAN");

        /**
         * Constant for the "Psalter Pahlavi" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock PSALTER_PAHLAVI =
            new UnicodeBlock("PSALTER_PAHLAVI",
                             "PSALTER PAHLAVI",
                             "PSALTERPAHLAVI");

        /**
         * Constant for the "Mahajani" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock MAHAJANI =
            new UnicodeBlock("MAHAJANI");

        /**
         * Constant for the "Sinhala Archaic Numbers" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock SINHALA_ARCHAIC_NUMBERS =
            new UnicodeBlock("SINHALA_ARCHAIC_NUMBERS",
                             "SINHALA ARCHAIC NUMBERS",
                             "SINHALAARCHAICNUMBERS");

        /**
         * Constant for the "Khojki" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock KHOJKI =
            new UnicodeBlock("KHOJKI");

        /**
         * Constant for the "Khudawadi" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock KHUDAWADI =
            new UnicodeBlock("KHUDAWADI");

        /**
         * Constant for the "Grantha" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock GRANTHA =
            new UnicodeBlock("GRANTHA");

        /**
         * Constant for the "Tirhuta" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock TIRHUTA =
            new UnicodeBlock("TIRHUTA");

        /**
         * Constant for the "Siddham" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock SIDDHAM =
            new UnicodeBlock("SIDDHAM");

        /**
         * Constant for the "Modi" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock MODI =
            new UnicodeBlock("MODI");

        /**
         * Constant for the "Warang Citi" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock WARANG_CITI =
            new UnicodeBlock("WARANG_CITI",
                             "WARANG CITI",
                             "WARANGCITI");

        /**
         * Constant for the "Pau Cin Hau" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock PAU_CIN_HAU =
            new UnicodeBlock("PAU_CIN_HAU",
                             "PAU CIN HAU",
                             "PAUCINHAU");

        /**
         * Constant for the "Mro" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock MRO =
            new UnicodeBlock("MRO");

        /**
         * Constant for the "Bassa Vah" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock BASSA_VAH =
            new UnicodeBlock("BASSA_VAH",
                             "BASSA VAH",
                             "BASSAVAH");

        /**
         * Constant for the "Pahawh Hmong" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock PAHAWH_HMONG =
            new UnicodeBlock("PAHAWH_HMONG",
                             "PAHAWH HMONG",
                             "PAHAWHHMONG");

        /**
         * Constant for the "Duployan" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock DUPLOYAN =
            new UnicodeBlock("DUPLOYAN");

        /**
         * Constant for the "Shorthand Format Controls" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock SHORTHAND_FORMAT_CONTROLS =
            new UnicodeBlock("SHORTHAND_FORMAT_CONTROLS",
                             "SHORTHAND FORMAT CONTROLS",
                             "SHORTHANDFORMATCONTROLS");

        /**
         * Constant for the "Mende Kikakui" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock MENDE_KIKAKUI =
            new UnicodeBlock("MENDE_KIKAKUI",
                             "MENDE KIKAKUI",
                             "MENDEKIKAKUI");

        /**
         * Constant for the "Ornamental Dingbats" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock ORNAMENTAL_DINGBATS =
            new UnicodeBlock("ORNAMENTAL_DINGBATS",
                             "ORNAMENTAL DINGBATS",
                             "ORNAMENTALDINGBATS");

        /**
         * Constant for the "Geometric Shapes Extended" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock GEOMETRIC_SHAPES_EXTENDED =
            new UnicodeBlock("GEOMETRIC_SHAPES_EXTENDED",
                             "GEOMETRIC SHAPES EXTENDED",
                             "GEOMETRICSHAPESEXTENDED");

        /**
         * Constant for the "Supplemental Arrows-C" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock SUPPLEMENTAL_ARROWS_C =
            new UnicodeBlock("SUPPLEMENTAL_ARROWS_C",
                             "SUPPLEMENTAL ARROWS-C",
                             "SUPPLEMENTALARROWS-C");

        /**
         * Constant for the "Cherokee Supplement" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock CHEROKEE_SUPPLEMENT =
            new UnicodeBlock("CHEROKEE_SUPPLEMENT",
                             "CHEROKEE SUPPLEMENT",
                             "CHEROKEESUPPLEMENT");

        /**
         * Constant for the "Hatran" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock HATRAN =
            new UnicodeBlock("HATRAN");

        /**
         * Constant for the "Old Hungarian" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock OLD_HUNGARIAN =
            new UnicodeBlock("OLD_HUNGARIAN",
                             "OLD HUNGARIAN",
                             "OLDHUNGARIAN");

        /**
         * Constant for the "Multani" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock MULTANI =
            new UnicodeBlock("MULTANI");

        /**
         * Constant for the "Ahom" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock AHOM =
            new UnicodeBlock("AHOM");

        /**
         * Constant for the "Early Dynastic Cuneiform" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock EARLY_DYNASTIC_CUNEIFORM =
            new UnicodeBlock("EARLY_DYNASTIC_CUNEIFORM",
                             "EARLY DYNASTIC CUNEIFORM",
                             "EARLYDYNASTICCUNEIFORM");

        /**
         * Constant for the "Anatolian Hieroglyphs" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock ANATOLIAN_HIEROGLYPHS =
            new UnicodeBlock("ANATOLIAN_HIEROGLYPHS",
                             "ANATOLIAN HIEROGLYPHS",
                             "ANATOLIANHIEROGLYPHS");

        /**
         * Constant for the "Sutton SignWriting" Unicode character block.
         * @since 9
         */
        public static final UnicodeBlock SUTTON_SIGNWRITING =
            new UnicodeBlock("SUTTON_SIGNWRITING",
                             "SUTTON SIGNWRITING",
                             "SUTTONSIGNWRITING");

        /**
         * Constant for the "Supplemental Symbols and Pictographs" Unicode
         * character block.
         * @since 9
         */
        public static final UnicodeBlock SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS =
            new UnicodeBlock("SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS",
                             "SUPPLEMENTAL SYMBOLS AND PICTOGRAPHS",
                             "SUPPLEMENTALSYMBOLSANDPICTOGRAPHS");

        /**
         * Constant for the "CJK Unified Ideographs Extension E" Unicode
         * character block.
         * @since 9
         */
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E =
            new UnicodeBlock("CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E",
                             "CJK UNIFIED IDEOGRAPHS EXTENSION E",
                             "CJKUNIFIEDIDEOGRAPHSEXTENSIONE");

        /**
         * Constant for the "Syriac Supplement" Unicode
         * character block.
         * @since 11
         */
        public static final UnicodeBlock SYRIAC_SUPPLEMENT =
            new UnicodeBlock("SYRIAC_SUPPLEMENT",
                             "SYRIAC SUPPLEMENT",
                             "SYRIACSUPPLEMENT");

        /**
         * Constant for the "Cyrillic Extended-C" Unicode
         * character block.
         * @since 11
         */
        public static final UnicodeBlock CYRILLIC_EXTENDED_C =
            new UnicodeBlock("CYRILLIC_EXTENDED_C",
                             "CYRILLIC EXTENDED-C",
                             "CYRILLICEXTENDED-C");

        /**
         * Constant for the "Osage" Unicode
         * character block.
         * @since 11
         */
        public static final UnicodeBlock OSAGE =
            new UnicodeBlock("OSAGE");

        /**
         * Constant for the "Newa" Unicode
         * character block.
         * @since 11
         */
        public static final UnicodeBlock NEWA =
            new UnicodeBlock("NEWA");

        /**
         * Constant for the "Mongolian Supplement" Unicode
         * character block.
         * @since 11
         */
        public static final UnicodeBlock MONGOLIAN_SUPPLEMENT =
            new UnicodeBlock("MONGOLIAN_SUPPLEMENT",
                             "MONGOLIAN SUPPLEMENT",
                             "MONGOLIANSUPPLEMENT");

        /**
         * Constant for the "Marchen" Unicode
         * character block.
         * @since 11
         */
        public static final UnicodeBlock MARCHEN =
            new UnicodeBlock("MARCHEN");

        /**
         * Constant for the "Ideographic Symbols and Punctuation" Unicode
         * character block.
         * @since 11
         */
        public static final UnicodeBlock IDEOGRAPHIC_SYMBOLS_AND_PUNCTUATION =
            new UnicodeBlock("IDEOGRAPHIC_SYMBOLS_AND_PUNCTUATION",
                             "IDEOGRAPHIC SYMBOLS AND PUNCTUATION",
                             "IDEOGRAPHICSYMBOLSANDPUNCTUATION");

        /**
         * Constant for the "Tangut" Unicode
         * character block.
         * @since 11
         */
        public static final UnicodeBlock TANGUT =
            new UnicodeBlock("TANGUT");

        /**
         * Constant for the "Tangut Components" Unicode
         * character block.
         * @since 11
         */
        public static final UnicodeBlock TANGUT_COMPONENTS =
            new UnicodeBlock("TANGUT_COMPONENTS",
                             "TANGUT COMPONENTS",
                             "TANGUTCOMPONENTS");

        /**
         * Constant for the "Kana Extended-A" Unicode
         * character block.
         * @since 11
         */
        public static final UnicodeBlock KANA_EXTENDED_A =
            new UnicodeBlock("KANA_EXTENDED_A",
                             "KANA EXTENDED-A",
                             "KANAEXTENDED-A");
        /**
         * Constant for the "Glagolitic Supplement" Unicode
         * character block.
         * @since 11
         */
        public static final UnicodeBlock GLAGOLITIC_SUPPLEMENT =
            new UnicodeBlock("GLAGOLITIC_SUPPLEMENT",
                             "GLAGOLITIC SUPPLEMENT",
                             "GLAGOLITICSUPPLEMENT");
        /**
         * Constant for the "Adlam" Unicode
         * character block.
         * @since 11
         */
        public static final UnicodeBlock ADLAM =
            new UnicodeBlock("ADLAM");

        /**
         * Constant for the "Masaram Gondi" Unicode
         * character block.
         * @since 11
         */
        public static final UnicodeBlock MASARAM_GONDI =
            new UnicodeBlock("MASARAM_GONDI",
                             "MASARAM GONDI",
                             "MASARAMGONDI");

        /**
         * Constant for the "Zanabazar Square" Unicode
         * character block.
         * @since 11
         */
        public static final UnicodeBlock ZANABAZAR_SQUARE =
            new UnicodeBlock("ZANABAZAR_SQUARE",
                             "ZANABAZAR SQUARE",
                             "ZANABAZARSQUARE");

        /**
         * Constant for the "Nushu" Unicode
         * character block.
         * @since 11
         */
        public static final UnicodeBlock NUSHU =
            new UnicodeBlock("NUSHU");

        /**
         * Constant for the "Soyombo" Unicode
         * character block.
         * @since 11
         */
        public static final UnicodeBlock SOYOMBO =
            new UnicodeBlock("SOYOMBO");

        /**
         * Constant for the "Bhaiksuki" Unicode
         * character block.
         * @since 11
         */
        public static final UnicodeBlock BHAIKSUKI =
            new UnicodeBlock("BHAIKSUKI");

        /**
         * Constant for the "CJK Unified Ideographs Extension F" Unicode
         * character block.
         * @since 11
         */
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F =
            new UnicodeBlock("CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F",
                             "CJK UNIFIED IDEOGRAPHS EXTENSION F",
                             "CJKUNIFIEDIDEOGRAPHSEXTENSIONF");
        /**
         * Constant for the "Georgian Extended" Unicode
         * character block.
         * @since 12
         */
        public static final UnicodeBlock GEORGIAN_EXTENDED =
            new UnicodeBlock("GEORGIAN_EXTENDED",
                             "GEORGIAN EXTENDED",
                             "GEORGIANEXTENDED");

        /**
         * Constant for the "Hanifi Rohingya" Unicode
         * character block.
         * @since 12
         */
        public static final UnicodeBlock HANIFI_ROHINGYA =
            new UnicodeBlock("HANIFI_ROHINGYA",
                             "HANIFI ROHINGYA",
                             "HANIFIROHINGYA");

        /**
         * Constant for the "Old Sogdian" Unicode
         * character block.
         * @since 12
         */
        public static final UnicodeBlock OLD_SOGDIAN =
            new UnicodeBlock("OLD_SOGDIAN",
                             "OLD SOGDIAN",
                             "OLDSOGDIAN");

        /**
         * Constant for the "Sogdian" Unicode
         * character block.
         * @since 12
         */
        public static final UnicodeBlock SOGDIAN =
            new UnicodeBlock("SOGDIAN");

        /**
         * Constant for the "Dogra" Unicode
         * character block.
         * @since 12
         */
        public static final UnicodeBlock DOGRA =
            new UnicodeBlock("DOGRA");

        /**
         * Constant for the "Gunjala Gondi" Unicode
         * character block.
         * @since 12
         */
        public static final UnicodeBlock GUNJALA_GONDI =
            new UnicodeBlock("GUNJALA_GONDI",
                             "GUNJALA GONDI",
                             "GUNJALAGONDI");

        /**
         * Constant for the "Makasar" Unicode
         * character block.
         * @since 12
         */
        public static final UnicodeBlock MAKASAR =
            new UnicodeBlock("MAKASAR");

        /**
         * Constant for the "Medefaidrin" Unicode
         * character block.
         * @since 12
         */
        public static final UnicodeBlock MEDEFAIDRIN =
            new UnicodeBlock("MEDEFAIDRIN");

        /**
         * Constant for the "Mayan Numerals" Unicode
         * character block.
         * @since 12
         */
        public static final UnicodeBlock MAYAN_NUMERALS =
            new UnicodeBlock("MAYAN_NUMERALS",
                             "MAYAN NUMERALS",
                             "MAYANNUMERALS");

        /**
         * Constant for the "Indic Siyaq Numbers" Unicode
         * character block.
         * @since 12
         */
        public static final UnicodeBlock INDIC_SIYAQ_NUMBERS =
            new UnicodeBlock("INDIC_SIYAQ_NUMBERS",
                             "INDIC SIYAQ NUMBERS",
                             "INDICSIYAQNUMBERS");

        /**
         * Constant for the "Chess Symbols" Unicode
         * character block.
         * @since 12
         */
        public static final UnicodeBlock CHESS_SYMBOLS =
            new UnicodeBlock("CHESS_SYMBOLS",
                             "CHESS SYMBOLS",
                             "CHESSSYMBOLS");

        /**
         * Constant for the "Elymaic" Unicode
         * character block.
         * @since 13
         */
        public static final UnicodeBlock ELYMAIC =
            new UnicodeBlock("ELYMAIC");

        /**
         * Constant for the "Nandinagari" Unicode
         * character block.
         * @since 13
         */
        public static final UnicodeBlock NANDINAGARI =
            new UnicodeBlock("NANDINAGARI");

        /**
         * Constant for the "Tamil Supplement" Unicode
         * character block.
         * @since 13
         */
        public static final UnicodeBlock TAMIL_SUPPLEMENT =
            new UnicodeBlock("TAMIL_SUPPLEMENT",
                             "TAMIL SUPPLEMENT",
                             "TAMILSUPPLEMENT");

        /**
         * Constant for the "Egyptian Hieroglyph Format Controls" Unicode
         * character block.
         * @since 13
         */
        public static final UnicodeBlock EGYPTIAN_HIEROGLYPH_FORMAT_CONTROLS =
            new UnicodeBlock("EGYPTIAN_HIEROGLYPH_FORMAT_CONTROLS",
                             "EGYPTIAN HIEROGLYPH FORMAT CONTROLS",
                             "EGYPTIANHIEROGLYPHFORMATCONTROLS");

        /**
         * Constant for the "Small Kana Extension" Unicode
         * character block.
         * @since 13
         */
        public static final UnicodeBlock SMALL_KANA_EXTENSION =
            new UnicodeBlock("SMALL_KANA_EXTENSION",
                             "SMALL KANA EXTENSION",
                             "SMALLKANAEXTENSION");

        /**
         * Constant for the "Nyiakeng Puachue Hmong" Unicode
         * character block.
         * @since 13
         */
        public static final UnicodeBlock NYIAKENG_PUACHUE_HMONG =
            new UnicodeBlock("NYIAKENG_PUACHUE_HMONG",
                             "NYIAKENG PUACHUE HMONG",
                             "NYIAKENGPUACHUEHMONG");

        /**
         * Constant for the "Wancho" Unicode
         * character block.
         * @since 13
         */
        public static final UnicodeBlock WANCHO =
            new UnicodeBlock("WANCHO");

        /**
         * Constant for the "Ottoman Siyaq Numbers" Unicode
         * character block.
         * @since 13
         */
        public static final UnicodeBlock OTTOMAN_SIYAQ_NUMBERS =
            new UnicodeBlock("OTTOMAN_SIYAQ_NUMBERS",
                             "OTTOMAN SIYAQ NUMBERS",
                             "OTTOMANSIYAQNUMBERS");

        /**
         * Constant for the "Symbols and Pictographs Extended-A" Unicode
         * character block.
         * @since 13
         */
        public static final UnicodeBlock SYMBOLS_AND_PICTOGRAPHS_EXTENDED_A =
            new UnicodeBlock("SYMBOLS_AND_PICTOGRAPHS_EXTENDED_A",
                             "SYMBOLS AND PICTOGRAPHS EXTENDED-A",
                             "SYMBOLSANDPICTOGRAPHSEXTENDED-A");

        /**
         * Constant for the "Yezidi" Unicode
         * character block.
         * @since 15
         */
        public static final UnicodeBlock YEZIDI =
            new UnicodeBlock("YEZIDI");

        /**
         * Constant for the "Chorasmian" Unicode
         * character block.
         * @since 15
         */
        public static final UnicodeBlock CHORASMIAN =
            new UnicodeBlock("CHORASMIAN");

        /**
         * Constant for the "Dives Akuru" Unicode
         * character block.
         * @since 15
         */
        public static final UnicodeBlock DIVES_AKURU =
            new UnicodeBlock("DIVES_AKURU",
                             "DIVES AKURU",
                             "DIVESAKURU");

        /**
         * Constant for the "Lisu Supplement" Unicode
         * character block.
         * @since 15
         */
        public static final UnicodeBlock LISU_SUPPLEMENT =
            new UnicodeBlock("LISU_SUPPLEMENT",
                             "LISU SUPPLEMENT",
                             "LISUSUPPLEMENT");

        /**
         * Constant for the "Khitan Small Script" Unicode
         * character block.
         * @since 15
         */
        public static final UnicodeBlock KHITAN_SMALL_SCRIPT =
            new UnicodeBlock("KHITAN_SMALL_SCRIPT",
                             "KHITAN SMALL SCRIPT",
                             "KHITANSMALLSCRIPT");

        /**
         * Constant for the "Tangut Supplement" Unicode
         * character block.
         * @since 15
         */
        public static final UnicodeBlock TANGUT_SUPPLEMENT =
            new UnicodeBlock("TANGUT_SUPPLEMENT",
                             "TANGUT SUPPLEMENT",
                             "TANGUTSUPPLEMENT");

        /**
         * Constant for the "Symbols for Legacy Computing" Unicode
         * character block.
         * @since 15
         */
        public static final UnicodeBlock SYMBOLS_FOR_LEGACY_COMPUTING =
            new UnicodeBlock("SYMBOLS_FOR_LEGACY_COMPUTING",
                             "SYMBOLS FOR LEGACY COMPUTING",
                             "SYMBOLSFORLEGACYCOMPUTING");

        /**
         * Constant for the "CJK Unified Ideographs Extension G" Unicode
         * character block.
         * @since 15
         */
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_G =
            new UnicodeBlock("CJK_UNIFIED_IDEOGRAPHS_EXTENSION_G",
                             "CJK UNIFIED IDEOGRAPHS EXTENSION G",
                             "CJKUNIFIEDIDEOGRAPHSEXTENSIONG");

        private static final int[] blockStarts = {
            0x0000,   // 0000..007F; Basic Latin
            0x0080,   // 0080..00FF; Latin-1 Supplement
            0x0100,   // 0100..017F; Latin Extended-A
            0x0180,   // 0180..024F; Latin Extended-B
            0x0250,   // 0250..02AF; IPA Extensions
            0x02B0,   // 02B0..02FF; Spacing Modifier Letters
            0x0300,   // 0300..036F; Combining Diacritical Marks
            0x0370,   // 0370..03FF; Greek and Coptic
            0x0400,   // 0400..04FF; Cyrillic
            0x0500,   // 0500..052F; Cyrillic Supplement
            0x0530,   // 0530..058F; Armenian
            0x0590,   // 0590..05FF; Hebrew
            0x0600,   // 0600..06FF; Arabic
            0x0700,   // 0700..074F; Syriac
            0x0750,   // 0750..077F; Arabic Supplement
            0x0780,   // 0780..07BF; Thaana
            0x07C0,   // 07C0..07FF; NKo
            0x0800,   // 0800..083F; Samaritan
            0x0840,   // 0840..085F; Mandaic
            0x0860,   // 0860..086F; Syriac Supplement
            0x0870,   //             unassigned
            0x08A0,   // 08A0..08FF; Arabic Extended-A
            0x0900,   // 0900..097F; Devanagari
            0x0980,   // 0980..09FF; Bengali
            0x0A00,   // 0A00..0A7F; Gurmukhi
            0x0A80,   // 0A80..0AFF; Gujarati
            0x0B00,   // 0B00..0B7F; Oriya
            0x0B80,   // 0B80..0BFF; Tamil
            0x0C00,   // 0C00..0C7F; Telugu
            0x0C80,   // 0C80..0CFF; Kannada
            0x0D00,   // 0D00..0D7F; Malayalam
            0x0D80,   // 0D80..0DFF; Sinhala
            0x0E00,   // 0E00..0E7F; Thai
            0x0E80,   // 0E80..0EFF; Lao
            0x0F00,   // 0F00..0FFF; Tibetan
            0x1000,   // 1000..109F; Myanmar
            0x10A0,   // 10A0..10FF; Georgian
            0x1100,   // 1100..11FF; Hangul Jamo
            0x1200,   // 1200..137F; Ethiopic
            0x1380,   // 1380..139F; Ethiopic Supplement
            0x13A0,   // 13A0..13FF; Cherokee
            0x1400,   // 1400..167F; Unified Canadian Aboriginal Syllabics
            0x1680,   // 1680..169F; Ogham
            0x16A0,   // 16A0..16FF; Runic
            0x1700,   // 1700..171F; Tagalog
            0x1720,   // 1720..173F; Hanunoo
            0x1740,   // 1740..175F; Buhid
            0x1760,   // 1760..177F; Tagbanwa
            0x1780,   // 1780..17FF; Khmer
            0x1800,   // 1800..18AF; Mongolian
            0x18B0,   // 18B0..18FF; Unified Canadian Aboriginal Syllabics Extended
            0x1900,   // 1900..194F; Limbu
            0x1950,   // 1950..197F; Tai Le
            0x1980,   // 1980..19DF; New Tai Lue
            0x19E0,   // 19E0..19FF; Khmer Symbols
            0x1A00,   // 1A00..1A1F; Buginese
            0x1A20,   // 1A20..1AAF; Tai Tham
            0x1AB0,   // 1AB0..1AFF; Combining Diacritical Marks Extended
            0x1B00,   // 1B00..1B7F; Balinese
            0x1B80,   // 1B80..1BBF; Sundanese
            0x1BC0,   // 1BC0..1BFF; Batak
            0x1C00,   // 1C00..1C4F; Lepcha
            0x1C50,   // 1C50..1C7F; Ol Chiki
            0x1C80,   // 1C80..1C8F; Cyrillic Extended-C
            0x1C90,   // 1C90..1CBF; Georgian Extended
            0x1CC0,   // 1CC0..1CCF; Sundanese Supplement
            0x1CD0,   // 1CD0..1CFF; Vedic Extensions
            0x1D00,   // 1D00..1D7F; Phonetic Extensions
            0x1D80,   // 1D80..1DBF; Phonetic Extensions Supplement
            0x1DC0,   // 1DC0..1DFF; Combining Diacritical Marks Supplement
            0x1E00,   // 1E00..1EFF; Latin Extended Additional
            0x1F00,   // 1F00..1FFF; Greek Extended
            0x2000,   // 2000..206F; General Punctuation
            0x2070,   // 2070..209F; Superscripts and Subscripts
            0x20A0,   // 20A0..20CF; Currency Symbols
            0x20D0,   // 20D0..20FF; Combining Diacritical Marks for Symbols
            0x2100,   // 2100..214F; Letterlike Symbols
            0x2150,   // 2150..218F; Number Forms
            0x2190,   // 2190..21FF; Arrows
            0x2200,   // 2200..22FF; Mathematical Operators
            0x2300,   // 2300..23FF; Miscellaneous Technical
            0x2400,   // 2400..243F; Control Pictures
            0x2440,   // 2440..245F; Optical Character Recognition
            0x2460,   // 2460..24FF; Enclosed Alphanumerics
            0x2500,   // 2500..257F; Box Drawing
            0x2580,   // 2580..259F; Block Elements
            0x25A0,   // 25A0..25FF; Geometric Shapes
            0x2600,   // 2600..26FF; Miscellaneous Symbols
            0x2700,   // 2700..27BF; Dingbats
            0x27C0,   // 27C0..27EF; Miscellaneous Mathematical Symbols-A
            0x27F0,   // 27F0..27FF; Supplemental Arrows-A
            0x2800,   // 2800..28FF; Braille Patterns
            0x2900,   // 2900..297F; Supplemental Arrows-B
            0x2980,   // 2980..29FF; Miscellaneous Mathematical Symbols-B
            0x2A00,   // 2A00..2AFF; Supplemental Mathematical Operators
            0x2B00,   // 2B00..2BFF; Miscellaneous Symbols and Arrows
            0x2C00,   // 2C00..2C5F; Glagolitic
            0x2C60,   // 2C60..2C7F; Latin Extended-C
            0x2C80,   // 2C80..2CFF; Coptic
            0x2D00,   // 2D00..2D2F; Georgian Supplement
            0x2D30,   // 2D30..2D7F; Tifinagh
            0x2D80,   // 2D80..2DDF; Ethiopic Extended
            0x2DE0,   // 2DE0..2DFF; Cyrillic Extended-A
            0x2E00,   // 2E00..2E7F; Supplemental Punctuation
            0x2E80,   // 2E80..2EFF; CJK Radicals Supplement
            0x2F00,   // 2F00..2FDF; Kangxi Radicals
            0x2FE0,   //             unassigned
            0x2FF0,   // 2FF0..2FFF; Ideographic Description Characters
            0x3000,   // 3000..303F; CJK Symbols and Punctuation
            0x3040,   // 3040..309F; Hiragana
            0x30A0,   // 30A0..30FF; Katakana
            0x3100,   // 3100..312F; Bopomofo
            0x3130,   // 3130..318F; Hangul Compatibility Jamo
            0x3190,   // 3190..319F; Kanbun
            0x31A0,   // 31A0..31BF; Bopomofo Extended
            0x31C0,   // 31C0..31EF; CJK Strokes
            0x31F0,   // 31F0..31FF; Katakana Phonetic Extensions
            0x3200,   // 3200..32FF; Enclosed CJK Letters and Months
            0x3300,   // 3300..33FF; CJK Compatibility
            0x3400,   // 3400..4DBF; CJK Unified Ideographs Extension A
            0x4DC0,   // 4DC0..4DFF; Yijing Hexagram Symbols
            0x4E00,   // 4E00..9FFF; CJK Unified Ideographs
            0xA000,   // A000..A48F; Yi Syllables
            0xA490,   // A490..A4CF; Yi Radicals
            0xA4D0,   // A4D0..A4FF; Lisu
            0xA500,   // A500..A63F; Vai
            0xA640,   // A640..A69F; Cyrillic Extended-B
            0xA6A0,   // A6A0..A6FF; Bamum
            0xA700,   // A700..A71F; Modifier Tone Letters
            0xA720,   // A720..A7FF; Latin Extended-D
            0xA800,   // A800..A82F; Syloti Nagri
            0xA830,   // A830..A83F; Common Indic Number Forms
            0xA840,   // A840..A87F; Phags-pa
            0xA880,   // A880..A8DF; Saurashtra
            0xA8E0,   // A8E0..A8FF; Devanagari Extended
            0xA900,   // A900..A92F; Kayah Li
            0xA930,   // A930..A95F; Rejang
            0xA960,   // A960..A97F; Hangul Jamo Extended-A
            0xA980,   // A980..A9DF; Javanese
            0xA9E0,   // A9E0..A9FF; Myanmar Extended-B
            0xAA00,   // AA00..AA5F; Cham
            0xAA60,   // AA60..AA7F; Myanmar Extended-A
            0xAA80,   // AA80..AADF; Tai Viet
            0xAAE0,   // AAE0..AAFF; Meetei Mayek Extensions
            0xAB00,   // AB00..AB2F; Ethiopic Extended-A
            0xAB30,   // AB30..AB6F; Latin Extended-E
            0xAB70,   // AB70..ABBF; Cherokee Supplement
            0xABC0,   // ABC0..ABFF; Meetei Mayek
            0xAC00,   // AC00..D7AF; Hangul Syllables
            0xD7B0,   // D7B0..D7FF; Hangul Jamo Extended-B
            0xD800,   // D800..DB7F; High Surrogates
            0xDB80,   // DB80..DBFF; High Private Use Surrogates
            0xDC00,   // DC00..DFFF; Low Surrogates
            0xE000,   // E000..F8FF; Private Use Area
            0xF900,   // F900..FAFF; CJK Compatibility Ideographs
            0xFB00,   // FB00..FB4F; Alphabetic Presentation Forms
            0xFB50,   // FB50..FDFF; Arabic Presentation Forms-A
            0xFE00,   // FE00..FE0F; Variation Selectors
            0xFE10,   // FE10..FE1F; Vertical Forms
            0xFE20,   // FE20..FE2F; Combining Half Marks
            0xFE30,   // FE30..FE4F; CJK Compatibility Forms
            0xFE50,   // FE50..FE6F; Small Form Variants
            0xFE70,   // FE70..FEFF; Arabic Presentation Forms-B
            0xFF00,   // FF00..FFEF; Halfwidth and Fullwidth Forms
            0xFFF0,   // FFF0..FFFF; Specials
            0x10000,  // 10000..1007F; Linear B Syllabary
            0x10080,  // 10080..100FF; Linear B Ideograms
            0x10100,  // 10100..1013F; Aegean Numbers
            0x10140,  // 10140..1018F; Ancient Greek Numbers
            0x10190,  // 10190..101CF; Ancient Symbols
            0x101D0,  // 101D0..101FF; Phaistos Disc
            0x10200,  //               unassigned
            0x10280,  // 10280..1029F; Lycian
            0x102A0,  // 102A0..102DF; Carian
            0x102E0,  // 102E0..102FF; Coptic Epact Numbers
            0x10300,  // 10300..1032F; Old Italic
            0x10330,  // 10330..1034F; Gothic
            0x10350,  // 10350..1037F; Old Permic
            0x10380,  // 10380..1039F; Ugaritic
            0x103A0,  // 103A0..103DF; Old Persian
            0x103E0,  //               unassigned
            0x10400,  // 10400..1044F; Deseret
            0x10450,  // 10450..1047F; Shavian
            0x10480,  // 10480..104AF; Osmanya
            0x104B0,  // 104B0..104FF; Osage
            0x10500,  // 10500..1052F; Elbasan
            0x10530,  // 10530..1056F; Caucasian Albanian
            0x10570,  //               unassigned
            0x10600,  // 10600..1077F; Linear A
            0x10780,  //               unassigned
            0x10800,  // 10800..1083F; Cypriot Syllabary
            0x10840,  // 10840..1085F; Imperial Aramaic
            0x10860,  // 10860..1087F; Palmyrene
            0x10880,  // 10880..108AF; Nabataean
            0x108B0,  //               unassigned
            0x108E0,  // 108E0..108FF; Hatran
            0x10900,  // 10900..1091F; Phoenician
            0x10920,  // 10920..1093F; Lydian
            0x10940,  //               unassigned
            0x10980,  // 10980..1099F; Meroitic Hieroglyphs
            0x109A0,  // 109A0..109FF; Meroitic Cursive
            0x10A00,  // 10A00..10A5F; Kharoshthi
            0x10A60,  // 10A60..10A7F; Old South Arabian
            0x10A80,  // 10A80..10A9F; Old North Arabian
            0x10AA0,  //               unassigned
            0x10AC0,  // 10AC0..10AFF; Manichaean
            0x10B00,  // 10B00..10B3F; Avestan
            0x10B40,  // 10B40..10B5F; Inscriptional Parthian
            0x10B60,  // 10B60..10B7F; Inscriptional Pahlavi
            0x10B80,  // 10B80..10BAF; Psalter Pahlavi
            0x10BB0,  //               unassigned
            0x10C00,  // 10C00..10C4F; Old Turkic
            0x10C50,  //               unassigned
            0x10C80,  // 10C80..10CFF; Old Hungarian
            0x10D00,  // 10D00..10D3F; Hanifi Rohingya
            0x10D40,  //               unassigned
            0x10E60,  // 10E60..10E7F; Rumi Numeral Symbols
            0x10E80,  // 10E80..10EBF; Yezidi
            0x10EC0,  //               unassigned
            0x10F00,  // 10F00..10F2F; Old Sogdian
            0x10F30,  // 10F30..10F6F; Sogdian
            0x10F70,  //               unassigned
            0x10FB0,  // 10FB0..10FDF; Chorasmian
            0x10FE0,  // 10FE0..10FFF; Elymaic
            0x11000,  // 11000..1107F; Brahmi
            0x11080,  // 11080..110CF; Kaithi
            0x110D0,  // 110D0..110FF; Sora Sompeng
            0x11100,  // 11100..1114F; Chakma
            0x11150,  // 11150..1117F; Mahajani
            0x11180,  // 11180..111DF; Sharada
            0x111E0,  // 111E0..111FF; Sinhala Archaic Numbers
            0x11200,  // 11200..1124F; Khojki
            0x11250,  //               unassigned
            0x11280,  // 11280..112AF; Multani
            0x112B0,  // 112B0..112FF; Khudawadi
            0x11300,  // 11300..1137F; Grantha
            0x11380,  //               unassigned
            0x11400,  // 11400..1147F; Newa
            0x11480,  // 11480..114DF; Tirhuta
            0x114E0,  //               unassigned
            0x11580,  // 11580..115FF; Siddham
            0x11600,  // 11600..1165F; Modi
            0x11660,  // 11660..1167F; Mongolian Supplement
            0x11680,  // 11680..116CF; Takri
            0x116D0,  //               unassigned
            0x11700,  // 11700..1173F; Ahom
            0x11740,  //               unassigned
            0x11800,  // 11800..1184F; Dogra
            0x11850,  //               unassigned
            0x118A0,  // 118A0..118FF; Warang Citi
            0x11900,  // 11900..1195F; Dives Akuru
            0x11960,  //               unassigned
            0x119A0,  // 119A0..119FF; Nandinagari
            0x11A00,  // 11A00..11A4F; Zanabazar Square
            0x11A50,  // 11A50..11AAF; Soyombo
            0x11AB0,  //               unassigned
            0x11AC0,  // 11AC0..11AFF; Pau Cin Hau
            0x11B00,  //               unassigned
            0x11C00,  // 11C00..11C6F; Bhaiksuki
            0x11C70,  // 11C70..11CBF; Marchen
            0x11CC0,  //               unassigned
            0x11D00,  // 11D00..11D5F; Masaram Gondi
            0x11D60,  // 11D60..11DAF; Gunjala Gondi
            0x11DB0,  //               unassigned
            0x11EE0,  // 11EE0..11EFF; Makasar
            0x11F00,  //               unassigned
            0x11FB0,  // 11FB0..11FBF; Lisu Supplement
            0x11FC0,  // 11FC0..11FFF; Tamil Supplement
            0x12000,  // 12000..123FF; Cuneiform
            0x12400,  // 12400..1247F; Cuneiform Numbers and Punctuation
            0x12480,  // 12480..1254F; Early Dynastic Cuneiform
            0x12550,  //               unassigned
            0x13000,  // 13000..1342F; Egyptian Hieroglyphs
            0x13430,  // 13430..1343F; Egyptian Hieroglyph Format Controls
            0x13440,  //               unassigned
            0x14400,  // 14400..1467F; Anatolian Hieroglyphs
            0x14680,  //               unassigned
            0x16800,  // 16800..16A3F; Bamum Supplement
            0x16A40,  // 16A40..16A6F; Mro
            0x16A70,  //               unassigned
            0x16AD0,  // 16AD0..16AFF; Bassa Vah
            0x16B00,  // 16B00..16B8F; Pahawh Hmong
            0x16B90,  //               unassigned
            0x16E40,  // 16E40..16E9F; Medefaidrin
            0x16EA0,  //               unassigned
            0x16F00,  // 16F00..16F9F; Miao
            0x16FA0,  //               unassigned
            0x16FE0,  // 16FE0..16FFF; Ideographic Symbols and Punctuation
            0x17000,  // 17000..187FF; Tangut
            0x18800,  // 18800..18AFF; Tangut Components
            0x18B00,  // 18B00..18CFF; Khitan Small Script
            0x18D00,  // 18D00..18D8F; Tangut Supplement
            0x18D90,  //               unassigned
            0x1B000,  // 1B000..1B0FF; Kana Supplement
            0x1B100,  // 1B100..1B12F; Kana Extended-A
            0x1B130,  // 1B130..1B16F; Small Kana Extension
            0x1B170,  // 1B170..1B2FF; Nushu
            0x1B300,  //               unassigned
            0x1BC00,  // 1BC00..1BC9F; Duployan
            0x1BCA0,  // 1BCA0..1BCAF; Shorthand Format Controls
            0x1BCB0,  //               unassigned
            0x1D000,  // 1D000..1D0FF; Byzantine Musical Symbols
            0x1D100,  // 1D100..1D1FF; Musical Symbols
            0x1D200,  // 1D200..1D24F; Ancient Greek Musical Notation
            0x1D250,  //               unassigned
            0x1D2E0,  // 1D2E0..1D2FF; Mayan Numerals
            0x1D300,  // 1D300..1D35F; Tai Xuan Jing Symbols
            0x1D360,  // 1D360..1D37F; Counting Rod Numerals
            0x1D380,  //               unassigned
            0x1D400,  // 1D400..1D7FF; Mathematical Alphanumeric Symbols
            0x1D800,  // 1D800..1DAAF; Sutton SignWriting
            0x1DAB0,  //               unassigned
            0x1E000,  // 1E000..1E02F; Glagolitic Supplement
            0x1E030,  //               unassigned
            0x1E100,  // 1E100..1E14F; Nyiakeng Puachue Hmong
            0x1E150,  //               unassigned
            0x1E2C0,  // 1E2C0..1E2FF; Wancho
            0x1E300,  //               unassigned
            0x1E800,  // 1E800..1E8DF; Mende Kikakui
            0x1E8E0,  //               unassigned
            0x1E900,  // 1E900..1E95F; Adlam
            0x1E960,  //               unassigned
            0x1EC70,  // 1EC70..1ECBF; Indic Siyaq Numbers
            0x1ECC0,  //               unassigned
            0x1ED00,  // 1ED00..1ED4F; Ottoman Siyaq Numbers
            0x1ED50,  //               unassigned
            0x1EE00,  // 1EE00..1EEFF; Arabic Mathematical Alphabetic Symbols
            0x1EF00,  //               unassigned
            0x1F000,  // 1F000..1F02F; Mahjong Tiles
            0x1F030,  // 1F030..1F09F; Domino Tiles
            0x1F0A0,  // 1F0A0..1F0FF; Playing Cards
            0x1F100,  // 1F100..1F1FF; Enclosed Alphanumeric Supplement
            0x1F200,  // 1F200..1F2FF; Enclosed Ideographic Supplement
            0x1F300,  // 1F300..1F5FF; Miscellaneous Symbols and Pictographs
            0x1F600,  // 1F600..1F64F; Emoticons
            0x1F650,  // 1F650..1F67F; Ornamental Dingbats
            0x1F680,  // 1F680..1F6FF; Transport and Map Symbols
            0x1F700,  // 1F700..1F77F; Alchemical Symbols
            0x1F780,  // 1F780..1F7FF; Geometric Shapes Extended
            0x1F800,  // 1F800..1F8FF; Supplemental Arrows-C
            0x1F900,  // 1F900..1F9FF; Supplemental Symbols and Pictographs
            0x1FA00,  // 1FA00..1FA6F; Chess Symbols
            0x1FA70,  // 1FA70..1FAFF; Symbols and Pictographs Extended-A
            0x1FB00,  // 1FB00..1FBFF; Symbols for Legacy Computing
            0x1FC00,  //               unassigned
            0x20000,  // 20000..2A6DF; CJK Unified Ideographs Extension B
            0x2A6E0,  //               unassigned
            0x2A700,  // 2A700..2B73F; CJK Unified Ideographs Extension C
            0x2B740,  // 2B740..2B81F; CJK Unified Ideographs Extension D
            0x2B820,  // 2B820..2CEAF; CJK Unified Ideographs Extension E
            0x2CEB0,  // 2CEB0..2EBEF; CJK Unified Ideographs Extension F
            0x2EBF0,  //               unassigned
            0x2F800,  // 2F800..2FA1F; CJK Compatibility Ideographs Supplement
            0x2FA20,  //               unassigned
            0x30000,  // 30000..3134F; CJK Unified Ideographs Extension G
            0x31350,  //               unassigned
            0xE0000,  // E0000..E007F; Tags
            0xE0080,  //               unassigned
            0xE0100,  // E0100..E01EF; Variation Selectors Supplement
            0xE01F0,  //               unassigned
            0xF0000,  // F0000..FFFFF; Supplementary Private Use Area-A
            0x100000, // 100000..10FFFF; Supplementary Private Use Area-B
        };

        private static final UnicodeBlock[] blocks = {
            BASIC_LATIN,
            LATIN_1_SUPPLEMENT,
            LATIN_EXTENDED_A,
            LATIN_EXTENDED_B,
            IPA_EXTENSIONS,
            SPACING_MODIFIER_LETTERS,
            COMBINING_DIACRITICAL_MARKS,
            GREEK,
            CYRILLIC,
            CYRILLIC_SUPPLEMENTARY,
            ARMENIAN,
            HEBREW,
            ARABIC,
            SYRIAC,
            ARABIC_SUPPLEMENT,
            THAANA,
            NKO,
            SAMARITAN,
            MANDAIC,
            SYRIAC_SUPPLEMENT,
            null,
            ARABIC_EXTENDED_A,
            DEVANAGARI,
            BENGALI,
            GURMUKHI,
            GUJARATI,
            ORIYA,
            TAMIL,
            TELUGU,
            KANNADA,
            MALAYALAM,
            SINHALA,
            THAI,
            LAO,
            TIBETAN,
            MYANMAR,
            GEORGIAN,
            HANGUL_JAMO,
            ETHIOPIC,
            ETHIOPIC_SUPPLEMENT,
            CHEROKEE,
            UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS,
            OGHAM,
            RUNIC,
            TAGALOG,
            HANUNOO,
            BUHID,
            TAGBANWA,
            KHMER,
            MONGOLIAN,
            UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS_EXTENDED,
            LIMBU,
            TAI_LE,
            NEW_TAI_LUE,
            KHMER_SYMBOLS,
            BUGINESE,
            TAI_THAM,
            COMBINING_DIACRITICAL_MARKS_EXTENDED,
            BALINESE,
            SUNDANESE,
            BATAK,
            LEPCHA,
            OL_CHIKI,
            CYRILLIC_EXTENDED_C,
            GEORGIAN_EXTENDED,
            SUNDANESE_SUPPLEMENT,
            VEDIC_EXTENSIONS,
            PHONETIC_EXTENSIONS,
            PHONETIC_EXTENSIONS_SUPPLEMENT,
            COMBINING_DIACRITICAL_MARKS_SUPPLEMENT,
            LATIN_EXTENDED_ADDITIONAL,
            GREEK_EXTENDED,
            GENERAL_PUNCTUATION,
            SUPERSCRIPTS_AND_SUBSCRIPTS,
            CURRENCY_SYMBOLS,
            COMBINING_MARKS_FOR_SYMBOLS,
            LETTERLIKE_SYMBOLS,
            NUMBER_FORMS,
            ARROWS,
            MATHEMATICAL_OPERATORS,
            MISCELLANEOUS_TECHNICAL,
            CONTROL_PICTURES,
            OPTICAL_CHARACTER_RECOGNITION,
            ENCLOSED_ALPHANUMERICS,
            BOX_DRAWING,
            BLOCK_ELEMENTS,
            GEOMETRIC_SHAPES,
            MISCELLANEOUS_SYMBOLS,
            DINGBATS,
            MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A,
            SUPPLEMENTAL_ARROWS_A,
            BRAILLE_PATTERNS,
            SUPPLEMENTAL_ARROWS_B,
            MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B,
            SUPPLEMENTAL_MATHEMATICAL_OPERATORS,
            MISCELLANEOUS_SYMBOLS_AND_ARROWS,
            GLAGOLITIC,
            LATIN_EXTENDED_C,
            COPTIC,
            GEORGIAN_SUPPLEMENT,
            TIFINAGH,
            ETHIOPIC_EXTENDED,
            CYRILLIC_EXTENDED_A,
            SUPPLEMENTAL_PUNCTUATION,
            CJK_RADICALS_SUPPLEMENT,
            KANGXI_RADICALS,
            null,
            IDEOGRAPHIC_DESCRIPTION_CHARACTERS,
            CJK_SYMBOLS_AND_PUNCTUATION,
            HIRAGANA,
            KATAKANA,
            BOPOMOFO,
            HANGUL_COMPATIBILITY_JAMO,
            KANBUN,
            BOPOMOFO_EXTENDED,
            CJK_STROKES,
            KATAKANA_PHONETIC_EXTENSIONS,
            ENCLOSED_CJK_LETTERS_AND_MONTHS,
            CJK_COMPATIBILITY,
            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
            YIJING_HEXAGRAM_SYMBOLS,
            CJK_UNIFIED_IDEOGRAPHS,
            YI_SYLLABLES,
            YI_RADICALS,
            LISU,
            VAI,
            CYRILLIC_EXTENDED_B,
            BAMUM,
            MODIFIER_TONE_LETTERS,
            LATIN_EXTENDED_D,
            SYLOTI_NAGRI,
            COMMON_INDIC_NUMBER_FORMS,
            PHAGS_PA,
            SAURASHTRA,
            DEVANAGARI_EXTENDED,
            KAYAH_LI,
            REJANG,
            HANGUL_JAMO_EXTENDED_A,
            JAVANESE,
            MYANMAR_EXTENDED_B,
            CHAM,
            MYANMAR_EXTENDED_A,
            TAI_VIET,
            MEETEI_MAYEK_EXTENSIONS,
            ETHIOPIC_EXTENDED_A,
            LATIN_EXTENDED_E,
            CHEROKEE_SUPPLEMENT,
            MEETEI_MAYEK,
            HANGUL_SYLLABLES,
            HANGUL_JAMO_EXTENDED_B,
            HIGH_SURROGATES,
            HIGH_PRIVATE_USE_SURROGATES,
            LOW_SURROGATES,
            PRIVATE_USE_AREA,
            CJK_COMPATIBILITY_IDEOGRAPHS,
            ALPHABETIC_PRESENTATION_FORMS,
            ARABIC_PRESENTATION_FORMS_A,
            VARIATION_SELECTORS,
            VERTICAL_FORMS,
            COMBINING_HALF_MARKS,
            CJK_COMPATIBILITY_FORMS,
            SMALL_FORM_VARIANTS,
            ARABIC_PRESENTATION_FORMS_B,
            HALFWIDTH_AND_FULLWIDTH_FORMS,
            SPECIALS,
            LINEAR_B_SYLLABARY,
            LINEAR_B_IDEOGRAMS,
            AEGEAN_NUMBERS,
            ANCIENT_GREEK_NUMBERS,
            ANCIENT_SYMBOLS,
            PHAISTOS_DISC,
            null,
            LYCIAN,
            CARIAN,
            COPTIC_EPACT_NUMBERS,
            OLD_ITALIC,
            GOTHIC,
            OLD_PERMIC,
            UGARITIC,
            OLD_PERSIAN,
            null,
            DESERET,
            SHAVIAN,
            OSMANYA,
            OSAGE,
            ELBASAN,
            CAUCASIAN_ALBANIAN,
            null,
            LINEAR_A,
            null,
            CYPRIOT_SYLLABARY,
            IMPERIAL_ARAMAIC,
            PALMYRENE,
            NABATAEAN,
            null,
            HATRAN,
            PHOENICIAN,
            LYDIAN,
            null,
            MEROITIC_HIEROGLYPHS,
            MEROITIC_CURSIVE,
            KHAROSHTHI,
            OLD_SOUTH_ARABIAN,
            OLD_NORTH_ARABIAN,
            null,
            MANICHAEAN,
            AVESTAN,
            INSCRIPTIONAL_PARTHIAN,
            INSCRIPTIONAL_PAHLAVI,
            PSALTER_PAHLAVI,
            null,
            OLD_TURKIC,
            null,
            OLD_HUNGARIAN,
            HANIFI_ROHINGYA,
            null,
            RUMI_NUMERAL_SYMBOLS,
            YEZIDI,
            null,
            OLD_SOGDIAN,
            SOGDIAN,
            null,
            CHORASMIAN,
            ELYMAIC,
            BRAHMI,
            KAITHI,
            SORA_SOMPENG,
            CHAKMA,
            MAHAJANI,
            SHARADA,
            SINHALA_ARCHAIC_NUMBERS,
            KHOJKI,
            null,
            MULTANI,
            KHUDAWADI,
            GRANTHA,
            null,
            NEWA,
            TIRHUTA,
            null,
            SIDDHAM,
            MODI,
            MONGOLIAN_SUPPLEMENT,
            TAKRI,
            null,
            AHOM,
            null,
            DOGRA,
            null,
            WARANG_CITI,
            DIVES_AKURU,
            null,
            NANDINAGARI,
            ZANABAZAR_SQUARE,
            SOYOMBO,
            null,
            PAU_CIN_HAU,
            null,
            BHAIKSUKI,
            MARCHEN,
            null,
            MASARAM_GONDI,
            GUNJALA_GONDI,
            null,
            MAKASAR,
            null,
            LISU_SUPPLEMENT,
            TAMIL_SUPPLEMENT,
            CUNEIFORM,
            CUNEIFORM_NUMBERS_AND_PUNCTUATION,
            EARLY_DYNASTIC_CUNEIFORM,
            null,
            EGYPTIAN_HIEROGLYPHS,
            EGYPTIAN_HIEROGLYPH_FORMAT_CONTROLS,
            null,
            ANATOLIAN_HIEROGLYPHS,
            null,
            BAMUM_SUPPLEMENT,
            MRO,
            null,
            BASSA_VAH,
            PAHAWH_HMONG,
            null,
            MEDEFAIDRIN,
            null,
            MIAO,
            null,
            IDEOGRAPHIC_SYMBOLS_AND_PUNCTUATION,
            TANGUT,
            TANGUT_COMPONENTS,
            KHITAN_SMALL_SCRIPT,
            TANGUT_SUPPLEMENT,
            null,
            KANA_SUPPLEMENT,
            KANA_EXTENDED_A,
            SMALL_KANA_EXTENSION,
            NUSHU,
            null,
            DUPLOYAN,
            SHORTHAND_FORMAT_CONTROLS,
            null,
            BYZANTINE_MUSICAL_SYMBOLS,
            MUSICAL_SYMBOLS,
            ANCIENT_GREEK_MUSICAL_NOTATION,
            null,
            MAYAN_NUMERALS,
            TAI_XUAN_JING_SYMBOLS,
            COUNTING_ROD_NUMERALS,
            null,
            MATHEMATICAL_ALPHANUMERIC_SYMBOLS,
            SUTTON_SIGNWRITING,
            null,
            GLAGOLITIC_SUPPLEMENT,
            null,
            NYIAKENG_PUACHUE_HMONG,
            null,
            WANCHO,
            null,
            MENDE_KIKAKUI,
            null,
            ADLAM,
            null,
            INDIC_SIYAQ_NUMBERS,
            null,
            OTTOMAN_SIYAQ_NUMBERS,
            null,
            ARABIC_MATHEMATICAL_ALPHABETIC_SYMBOLS,
            null,
            MAHJONG_TILES,
            DOMINO_TILES,
            PLAYING_CARDS,
            ENCLOSED_ALPHANUMERIC_SUPPLEMENT,
            ENCLOSED_IDEOGRAPHIC_SUPPLEMENT,
            MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS,
            EMOTICONS,
            ORNAMENTAL_DINGBATS,
            TRANSPORT_AND_MAP_SYMBOLS,
            ALCHEMICAL_SYMBOLS,
            GEOMETRIC_SHAPES_EXTENDED,
            SUPPLEMENTAL_ARROWS_C,
            SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS,
            CHESS_SYMBOLS,
            SYMBOLS_AND_PICTOGRAPHS_EXTENDED_A,
            SYMBOLS_FOR_LEGACY_COMPUTING,
            null,
            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
            null,
            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C,
            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D,
            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E,
            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F,
            null,
            CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT,
            null,
            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_G,
            null,
            TAGS,
            null,
            VARIATION_SELECTORS_SUPPLEMENT,
            null,
            SUPPLEMENTARY_PRIVATE_USE_AREA_A,
            SUPPLEMENTARY_PRIVATE_USE_AREA_B,
        };


        /**
         * Returns the object representing the Unicode block containing the
         * given character, or {@code null} if the character is not a
         * member of a defined block.
         *
         * <p><b>Note:</b> This method cannot handle
         * <a href="Character.html#supplementary"> supplementary
         * characters</a>.  To support all Unicode characters, including
         * supplementary characters, use the {@link #of(int)} method.
         *
         * @param   c  The character in question
         * @return  The {@code UnicodeBlock} instance representing the
         *          Unicode block of which this character is a member, or
         *          {@code null} if the character is not a member of any
         *          Unicode block
         */
        public static UnicodeBlock of(char c) {
            return of((int)c);
        }

        /**
         * Returns the object representing the Unicode block
         * containing the given character (Unicode code point), or
         * {@code null} if the character is not a member of a
         * defined block.
         *
         * @param   codePoint the character (Unicode code point) in question.
         * @return  The {@code UnicodeBlock} instance representing the
         *          Unicode block of which this character is a member, or
         *          {@code null} if the character is not a member of any
         *          Unicode block
         * @throws  IllegalArgumentException if the specified
         * {@code codePoint} is an invalid Unicode code point.
         * @see Character#isValidCodePoint(int)
         * @since   1.5
         */
        public static UnicodeBlock of(int codePoint) {
            if (!isValidCodePoint(codePoint)) {
                throw new IllegalArgumentException(
                    String.format("Not a valid Unicode code point: 0x%X", codePoint));
            }

            int top, bottom, current;
            bottom = 0;
            top = blockStarts.length;
            current = top/2;

            // invariant: top > current >= bottom && codePoint >= unicodeBlockStarts[bottom]
            while (top - bottom > 1) {
                if (codePoint >= blockStarts[current]) {
                    bottom = current;
                } else {
                    top = current;
                }
                current = (top + bottom) / 2;
            }
            return blocks[current];
        }

        /**
         * Returns the UnicodeBlock with the given name. Block
         * names are determined by The Unicode Standard. The file
         * {@code Blocks-<version>.txt} defines blocks for a particular
         * version of the standard. The {@link Character} class specifies
         * the version of the standard that it supports.
         * <p>
         * This method accepts block names in the following forms:
         * <ol>
         * <li> Canonical block names as defined by the Unicode Standard.
         * For example, the standard defines a "Basic Latin" block. Therefore, this
         * method accepts "Basic Latin" as a valid block name. The documentation of
         * each UnicodeBlock provides the canonical name.
         * <li>Canonical block names with all spaces removed. For example, "BasicLatin"
         * is a valid block name for the "Basic Latin" block.
         * <li>The text representation of each constant UnicodeBlock identifier.
         * For example, this method will return the {@link #BASIC_LATIN} block if
         * provided with the "BASIC_LATIN" name. This form replaces all spaces and
         * hyphens in the canonical name with underscores.
         * </ol>
         * Finally, character case is ignored for all of the valid block name forms.
         * For example, "BASIC_LATIN" and "basic_latin" are both valid block names.
         * The en_US locale's case mapping rules are used to provide case-insensitive
         * string comparisons for block name validation.
         * <p>
         * If the Unicode Standard changes block names, both the previous and
         * current names will be accepted.
         *
         * @param blockName A {@code UnicodeBlock} name.
         * @return The {@code UnicodeBlock} instance identified
         *         by {@code blockName}
         * @throws IllegalArgumentException if {@code blockName} is an
         *         invalid name
         * @throws NullPointerException if {@code blockName} is null
         * @since 1.5
         */
        public static final UnicodeBlock forName(String blockName) {
            UnicodeBlock block = map.get(blockName.toUpperCase(Locale.US));
            if (block == null) {
                throw new IllegalArgumentException("Not a valid block name: "
                            + blockName);
            }
            return block;
        }
    }


    /**
     * A family of character subsets representing the character scripts
     * defined in the <a href="http://www.unicode.org/reports/tr24/">
     * <i>Unicode Standard Annex #24: Script Names</i></a>. Every Unicode
     * character is assigned to a single Unicode script, either a specific
     * script, such as {@link Character.UnicodeScript#LATIN Latin}, or
     * one of the following three special values,
     * {@link Character.UnicodeScript#INHERITED Inherited},
     * {@link Character.UnicodeScript#COMMON Common} or
     * {@link Character.UnicodeScript#UNKNOWN Unknown}.
     *
     * @since 1.7
     */
    public static enum UnicodeScript {
        /**
         * Unicode script "Common".
         */
        COMMON,

        /**
         * Unicode script "Latin".
         */
        LATIN,

        /**
         * Unicode script "Greek".
         */
        GREEK,

        /**
         * Unicode script "Cyrillic".
         */
        CYRILLIC,

        /**
         * Unicode script "Armenian".
         */
        ARMENIAN,

        /**
         * Unicode script "Hebrew".
         */
        HEBREW,

        /**
         * Unicode script "Arabic".
         */
        ARABIC,

        /**
         * Unicode script "Syriac".
         */
        SYRIAC,

        /**
         * Unicode script "Thaana".
         */
        THAANA,

        /**
         * Unicode script "Devanagari".
         */
        DEVANAGARI,

        /**
         * Unicode script "Bengali".
         */
        BENGALI,

        /**
         * Unicode script "Gurmukhi".
         */
        GURMUKHI,

        /**
         * Unicode script "Gujarati".
         */
        GUJARATI,

        /**
         * Unicode script "Oriya".
         */
        ORIYA,

        /**
         * Unicode script "Tamil".
         */
        TAMIL,

        /**
         * Unicode script "Telugu".
         */
        TELUGU,

        /**
         * Unicode script "Kannada".
         */
        KANNADA,

        /**
         * Unicode script "Malayalam".
         */
        MALAYALAM,

        /**
         * Unicode script "Sinhala".
         */
        SINHALA,

        /**
         * Unicode script "Thai".
         */
        THAI,

        /**
         * Unicode script "Lao".
         */
        LAO,

        /**
         * Unicode script "Tibetan".
         */
        TIBETAN,

        /**
         * Unicode script "Myanmar".
         */
        MYANMAR,

        /**
         * Unicode script "Georgian".
         */
        GEORGIAN,

        /**
         * Unicode script "Hangul".
         */
        HANGUL,

        /**
         * Unicode script "Ethiopic".
         */
        ETHIOPIC,

        /**
         * Unicode script "Cherokee".
         */
        CHEROKEE,

        /**
         * Unicode script "Canadian_Aboriginal".
         */
        CANADIAN_ABORIGINAL,

        /**
         * Unicode script "Ogham".
         */
        OGHAM,

        /**
         * Unicode script "Runic".
         */
        RUNIC,

        /**
         * Unicode script "Khmer".
         */
        KHMER,

        /**
         * Unicode script "Mongolian".
         */
        MONGOLIAN,

        /**
         * Unicode script "Hiragana".
         */
        HIRAGANA,

        /**
         * Unicode script "Katakana".
         */
        KATAKANA,

        /**
         * Unicode script "Bopomofo".
         */
        BOPOMOFO,

        /**
         * Unicode script "Han".
         */
        HAN,

        /**
         * Unicode script "Yi".
         */
        YI,

        /**
         * Unicode script "Old_Italic".
         */
        OLD_ITALIC,

        /**
         * Unicode script "Gothic".
         */
        GOTHIC,

        /**
         * Unicode script "Deseret".
         */
        DESERET,

        /**
         * Unicode script "Inherited".
         */
        INHERITED,

        /**
         * Unicode script "Tagalog".
         */
        TAGALOG,

        /**
         * Unicode script "Hanunoo".
         */
        HANUNOO,

        /**
         * Unicode script "Buhid".
         */
        BUHID,

        /**
         * Unicode script "Tagbanwa".
         */
        TAGBANWA,

        /**
         * Unicode script "Limbu".
         */
        LIMBU,

        /**
         * Unicode script "Tai_Le".
         */
        TAI_LE,

        /**
         * Unicode script "Linear_B".
         */
        LINEAR_B,

        /**
         * Unicode script "Ugaritic".
         */
        UGARITIC,

        /**
         * Unicode script "Shavian".
         */
        SHAVIAN,

        /**
         * Unicode script "Osmanya".
         */
        OSMANYA,

        /**
         * Unicode script "Cypriot".
         */
        CYPRIOT,

        /**
         * Unicode script "Braille".
         */
        BRAILLE,

        /**
         * Unicode script "Buginese".
         */
        BUGINESE,

        /**
         * Unicode script "Coptic".
         */
        COPTIC,

        /**
         * Unicode script "New_Tai_Lue".
         */
        NEW_TAI_LUE,

        /**
         * Unicode script "Glagolitic".
         */
        GLAGOLITIC,

        /**
         * Unicode script "Tifinagh".
         */
        TIFINAGH,

        /**
         * Unicode script "Syloti_Nagri".
         */
        SYLOTI_NAGRI,

        /**
         * Unicode script "Old_Persian".
         */
        OLD_PERSIAN,

        /**
         * Unicode script "Kharoshthi".
         */
        KHAROSHTHI,

        /**
         * Unicode script "Balinese".
         */
        BALINESE,

        /**
         * Unicode script "Cuneiform".
         */
        CUNEIFORM,

        /**
         * Unicode script "Phoenician".
         */
        PHOENICIAN,

        /**
         * Unicode script "Phags_Pa".
         */
        PHAGS_PA,

        /**
         * Unicode script "Nko".
         */
        NKO,

        /**
         * Unicode script "Sundanese".
         */
        SUNDANESE,

        /**
         * Unicode script "Batak".
         */
        BATAK,

        /**
         * Unicode script "Lepcha".
         */
        LEPCHA,

        /**
         * Unicode script "Ol_Chiki".
         */
        OL_CHIKI,

        /**
         * Unicode script "Vai".
         */
        VAI,

        /**
         * Unicode script "Saurashtra".
         */
        SAURASHTRA,

        /**
         * Unicode script "Kayah_Li".
         */
        KAYAH_LI,

        /**
         * Unicode script "Rejang".
         */
        REJANG,

        /**
         * Unicode script "Lycian".
         */
        LYCIAN,

        /**
         * Unicode script "Carian".
         */
        CARIAN,

        /**
         * Unicode script "Lydian".
         */
        LYDIAN,

        /**
         * Unicode script "Cham".
         */
        CHAM,

        /**
         * Unicode script "Tai_Tham".
         */
        TAI_THAM,

        /**
         * Unicode script "Tai_Viet".
         */
        TAI_VIET,

        /**
         * Unicode script "Avestan".
         */
        AVESTAN,

        /**
         * Unicode script "Egyptian_Hieroglyphs".
         */
        EGYPTIAN_HIEROGLYPHS,

        /**
         * Unicode script "Samaritan".
         */
        SAMARITAN,

        /**
         * Unicode script "Mandaic".
         */
        MANDAIC,

        /**
         * Unicode script "Lisu".
         */
        LISU,

        /**
         * Unicode script "Bamum".
         */
        BAMUM,

        /**
         * Unicode script "Javanese".
         */
        JAVANESE,

        /**
         * Unicode script "Meetei_Mayek".
         */
        MEETEI_MAYEK,

        /**
         * Unicode script "Imperial_Aramaic".
         */
        IMPERIAL_ARAMAIC,

        /**
         * Unicode script "Old_South_Arabian".
         */
        OLD_SOUTH_ARABIAN,

        /**
         * Unicode script "Inscriptional_Parthian".
         */
        INSCRIPTIONAL_PARTHIAN,

        /**
         * Unicode script "Inscriptional_Pahlavi".
         */
        INSCRIPTIONAL_PAHLAVI,

        /**
         * Unicode script "Old_Turkic".
         */
        OLD_TURKIC,

        /**
         * Unicode script "Brahmi".
         */
        BRAHMI,

        /**
         * Unicode script "Kaithi".
         */
        KAITHI,

        /**
         * Unicode script "Meroitic Hieroglyphs".
         * @since 1.8
         */
        MEROITIC_HIEROGLYPHS,

        /**
         * Unicode script "Meroitic Cursive".
         * @since 1.8
         */
        MEROITIC_CURSIVE,

        /**
         * Unicode script "Sora Sompeng".
         * @since 1.8
         */
        SORA_SOMPENG,

        /**
         * Unicode script "Chakma".
         * @since 1.8
         */
        CHAKMA,

        /**
         * Unicode script "Sharada".
         * @since 1.8
         */
        SHARADA,

        /**
         * Unicode script "Takri".
         * @since 1.8
         */
        TAKRI,

        /**
         * Unicode script "Miao".
         * @since 1.8
         */
        MIAO,

        /**
         * Unicode script "Caucasian Albanian".
         * @since 9
         */
        CAUCASIAN_ALBANIAN,

        /**
         * Unicode script "Bassa Vah".
         * @since 9
         */
        BASSA_VAH,

        /**
         * Unicode script "Duployan".
         * @since 9
         */
        DUPLOYAN,

        /**
         * Unicode script "Elbasan".
         * @since 9
         */
        ELBASAN,

        /**
         * Unicode script "Grantha".
         * @since 9
         */
        GRANTHA,

        /**
         * Unicode script "Pahawh Hmong".
         * @since 9
         */
        PAHAWH_HMONG,

        /**
         * Unicode script "Khojki".
         * @since 9
         */
        KHOJKI,

        /**
         * Unicode script "Linear A".
         * @since 9
         */
        LINEAR_A,

        /**
         * Unicode script "Mahajani".
         * @since 9
         */
        MAHAJANI,

        /**
         * Unicode script "Manichaean".
         * @since 9
         */
        MANICHAEAN,

        /**
         * Unicode script "Mende Kikakui".
         * @since 9
         */
        MENDE_KIKAKUI,

        /**
         * Unicode script "Modi".
         * @since 9
         */
        MODI,

        /**
         * Unicode script "Mro".
         * @since 9
         */
        MRO,

        /**
         * Unicode script "Old North Arabian".
         * @since 9
         */
        OLD_NORTH_ARABIAN,

        /**
         * Unicode script "Nabataean".
         * @since 9
         */
        NABATAEAN,

        /**
         * Unicode script "Palmyrene".
         * @since 9
         */
        PALMYRENE,

        /**
         * Unicode script "Pau Cin Hau".
         * @since 9
         */
        PAU_CIN_HAU,

        /**
         * Unicode script "Old Permic".
         * @since 9
         */
        OLD_PERMIC,

        /**
         * Unicode script "Psalter Pahlavi".
         * @since 9
         */
        PSALTER_PAHLAVI,

        /**
         * Unicode script "Siddham".
         * @since 9
         */
        SIDDHAM,

        /**
         * Unicode script "Khudawadi".
         * @since 9
         */
        KHUDAWADI,

        /**
         * Unicode script "Tirhuta".
         * @since 9
         */
        TIRHUTA,

        /**
         * Unicode script "Warang Citi".
         * @since 9
         */
        WARANG_CITI,

        /**
         * Unicode script "Ahom".
         * @since 9
         */
        AHOM,

        /**
         * Unicode script "Anatolian Hieroglyphs".
         * @since 9
         */
        ANATOLIAN_HIEROGLYPHS,

        /**
         * Unicode script "Hatran".
         * @since 9
         */
        HATRAN,

        /**
         * Unicode script "Multani".
         * @since 9
         */
        MULTANI,

        /**
         * Unicode script "Old Hungarian".
         * @since 9
         */
        OLD_HUNGARIAN,

        /**
         * Unicode script "SignWriting".
         * @since 9
         */
        SIGNWRITING,

        /**
         * Unicode script "Adlam".
         * @since 11
         */
        ADLAM,

        /**
         * Unicode script "Bhaiksuki".
         * @since 11
         */
        BHAIKSUKI,

        /**
         * Unicode script "Marchen".
         * @since 11
         */
        MARCHEN,

        /**
         * Unicode script "Newa".
         * @since 11
         */
        NEWA,

        /**
         * Unicode script "Osage".
         * @since 11
         */
        OSAGE,

        /**
         * Unicode script "Tangut".
         * @since 11
         */
        TANGUT,

        /**
         * Unicode script "Masaram Gondi".
         * @since 11
         */
        MASARAM_GONDI,

        /**
         * Unicode script "Nushu".
         * @since 11
         */
        NUSHU,

        /**
         * Unicode script "Soyombo".
         * @since 11
         */
        SOYOMBO,

        /**
         * Unicode script "Zanabazar Square".
         * @since 11
         */
        ZANABAZAR_SQUARE,

        /**
         * Unicode script "Hanifi Rohingya".
         * @since 12
         */
        HANIFI_ROHINGYA,

        /**
         * Unicode script "Old Sogdian".
         * @since 12
         */
        OLD_SOGDIAN,

        /**
         * Unicode script "Sogdian".
         * @since 12
         */
        SOGDIAN,

        /**
         * Unicode script "Dogra".
         * @since 12
         */
        DOGRA,

        /**
         * Unicode script "Gunjala Gondi".
         * @since 12
         */
        GUNJALA_GONDI,

        /**
         * Unicode script "Makasar".
         * @since 12
         */
        MAKASAR,

        /**
         * Unicode script "Medefaidrin".
         * @since 12
         */
        MEDEFAIDRIN,

        /**
         * Unicode script "Elymaic".
         * @since 13
         */
        ELYMAIC,

        /**
         * Unicode script "Nandinagari".
         * @since 13
         */
        NANDINAGARI,

        /**
         * Unicode script "Nyiakeng Puachue Hmong".
         * @since 13
         */
        NYIAKENG_PUACHUE_HMONG,

        /**
         * Unicode script "Wancho".
         * @since 13
         */
        WANCHO,

        /**
         * Unicode script "Yezidi".
         * @since 15
         */
        YEZIDI,

        /**
         * Unicode script "Chorasmian".
         * @since 15
         */
        CHORASMIAN,

        /**
         * Unicode script "Dives Akuru".
         * @since 15
         */
        DIVES_AKURU,

        /**
         * Unicode script "Khitan Small Script".
         * @since 15
         */
        KHITAN_SMALL_SCRIPT,

        /**
         * Unicode script "Unknown".
         */
        UNKNOWN;

        private static final int[] scriptStarts = {
            0x0000,   // 0000..0040; COMMON
            0x0041,   // 0041..005A; LATIN
            0x005B,   // 005B..0060; COMMON
            0x0061,   // 0061..007A; LATIN
            0x007B,   // 007B..00A9; COMMON
            0x00AA,   // 00AA      ; LATIN
            0x00AB,   // 00AB..00B9; COMMON
            0x00BA,   // 00BA      ; LATIN
            0x00BB,   // 00BB..00BF; COMMON
            0x00C0,   // 00C0..00D6; LATIN
            0x00D7,   // 00D7      ; COMMON
            0x00D8,   // 00D8..00F6; LATIN
            0x00F7,   // 00F7      ; COMMON
            0x00F8,   // 00F8..02B8; LATIN
            0x02B9,   // 02B9..02DF; COMMON
            0x02E0,   // 02E0..02E4; LATIN
            0x02E5,   // 02E5..02E9; COMMON
            0x02EA,   // 02EA..02EB; BOPOMOFO
            0x02EC,   // 02EC..02FF; COMMON
            0x0300,   // 0300..036F; INHERITED
            0x0370,   // 0370..0373; GREEK
            0x0374,   // 0374      ; COMMON
            0x0375,   // 0375..0377; GREEK
            0x0378,   // 0378..0379; UNKNOWN
            0x037A,   // 037A..037D; GREEK
            0x037E,   // 037E      ; COMMON
            0x037F,   // 037F      ; GREEK
            0x0380,   // 0380..0383; UNKNOWN
            0x0384,   // 0384      ; GREEK
            0x0385,   // 0385      ; COMMON
            0x0386,   // 0386      ; GREEK
            0x0387,   // 0387      ; COMMON
            0x0388,   // 0388..038A; GREEK
            0x038B,   // 038B      ; UNKNOWN
            0x038C,   // 038C      ; GREEK
            0x038D,   // 038D      ; UNKNOWN
            0x038E,   // 038E..03A1; GREEK
            0x03A2,   // 03A2      ; UNKNOWN
            0x03A3,   // 03A3..03E1; GREEK
            0x03E2,   // 03E2..03EF; COPTIC
            0x03F0,   // 03F0..03FF; GREEK
            0x0400,   // 0400..0484; CYRILLIC
            0x0485,   // 0485..0486; INHERITED
            0x0487,   // 0487..052F; CYRILLIC
            0x0530,   // 0530      ; UNKNOWN
            0x0531,   // 0531..0556; ARMENIAN
            0x0557,   // 0557..0558; UNKNOWN
            0x0559,   // 0559..058A; ARMENIAN
            0x058B,   // 058B..058C; UNKNOWN
            0x058D,   // 058D..058F; ARMENIAN
            0x0590,   // 0590      ; UNKNOWN
            0x0591,   // 0591..05C7; HEBREW
            0x05C8,   // 05C8..05CF; UNKNOWN
            0x05D0,   // 05D0..05EA; HEBREW
            0x05EB,   // 05EB..05EE; UNKNOWN
            0x05EF,   // 05EF..05F4; HEBREW
            0x05F5,   // 05F5..05FF; UNKNOWN
            0x0600,   // 0600..0604; ARABIC
            0x0605,   // 0605      ; COMMON
            0x0606,   // 0606..060B; ARABIC
            0x060C,   // 060C      ; COMMON
            0x060D,   // 060D..061A; ARABIC
            0x061B,   // 061B      ; COMMON
            0x061C,   // 061C      ; ARABIC
            0x061D,   // 061D      ; UNKNOWN
            0x061E,   // 061E      ; ARABIC
            0x061F,   // 061F      ; COMMON
            0x0620,   // 0620..063F; ARABIC
            0x0640,   // 0640      ; COMMON
            0x0641,   // 0641..064A; ARABIC
            0x064B,   // 064B..0655; INHERITED
            0x0656,   // 0656..066F; ARABIC
            0x0670,   // 0670      ; INHERITED
            0x0671,   // 0671..06DC; ARABIC
            0x06DD,   // 06DD      ; COMMON
            0x06DE,   // 06DE..06FF; ARABIC
            0x0700,   // 0700..070D; SYRIAC
            0x070E,   // 070E      ; UNKNOWN
            0x070F,   // 070F..074A; SYRIAC
            0x074B,   // 074B..074C; UNKNOWN
            0x074D,   // 074D..074F; SYRIAC
            0x0750,   // 0750..077F; ARABIC
            0x0780,   // 0780..07B1; THAANA
            0x07B2,   // 07B2..07BF; UNKNOWN
            0x07C0,   // 07C0..07FA; NKO
            0x07FB,   // 07FB..07FC; UNKNOWN
            0x07FD,   // 07FD..07FF; NKO
            0x0800,   // 0800..082D; SAMARITAN
            0x082E,   // 082E..082F; UNKNOWN
            0x0830,   // 0830..083E; SAMARITAN
            0x083F,   // 083F      ; UNKNOWN
            0x0840,   // 0840..085B; MANDAIC
            0x085C,   // 085C..085D; UNKNOWN
            0x085E,   // 085E      ; MANDAIC
            0x085F,   // 085F      ; UNKNOWN
            0x0860,   // 0860..086A; SYRIAC
            0x086B,   // 086B..089F; UNKNOWN
            0x08A0,   // 08A0..08B4; ARABIC
            0x08B5,   // 08B5      ; UNKNOWN
            0x08B6,   // 08B6..08C7; ARABIC
            0x08C8,   // 08C8..08D2; UNKNOWN
            0x08D3,   // 08D3..08E1; ARABIC
            0x08E2,   // 08E2      ; COMMON
            0x08E3,   // 08E3..08FF; ARABIC
            0x0900,   // 0900..0950; DEVANAGARI
            0x0951,   // 0951..0954; INHERITED
            0x0955,   // 0955..0963; DEVANAGARI
            0x0964,   // 0964..0965; COMMON
            0x0966,   // 0966..097F; DEVANAGARI
            0x0980,   // 0980..0983; BENGALI
            0x0984,   // 0984      ; UNKNOWN
            0x0985,   // 0985..098C; BENGALI
            0x098D,   // 098D..098E; UNKNOWN
            0x098F,   // 098F..0990; BENGALI
            0x0991,   // 0991..0992; UNKNOWN
            0x0993,   // 0993..09A8; BENGALI
            0x09A9,   // 09A9      ; UNKNOWN
            0x09AA,   // 09AA..09B0; BENGALI
            0x09B1,   // 09B1      ; UNKNOWN
            0x09B2,   // 09B2      ; BENGALI
            0x09B3,   // 09B3..09B5; UNKNOWN
            0x09B6,   // 09B6..09B9; BENGALI
            0x09BA,   // 09BA..09BB; UNKNOWN
            0x09BC,   // 09BC..09C4; BENGALI
            0x09C5,   // 09C5..09C6; UNKNOWN
            0x09C7,   // 09C7..09C8; BENGALI
            0x09C9,   // 09C9..09CA; UNKNOWN
            0x09CB,   // 09CB..09CE; BENGALI
            0x09CF,   // 09CF..09D6; UNKNOWN
            0x09D7,   // 09D7      ; BENGALI
            0x09D8,   // 09D8..09DB; UNKNOWN
            0x09DC,   // 09DC..09DD; BENGALI
            0x09DE,   // 09DE      ; UNKNOWN
            0x09DF,   // 09DF..09E3; BENGALI
            0x09E4,   // 09E4..09E5; UNKNOWN
            0x09E6,   // 09E6..09FE; BENGALI
            0x09FF,   // 09FF..0A00; UNKNOWN
            0x0A01,   // 0A01..0A03; GURMUKHI
            0x0A04,   // 0A04      ; UNKNOWN
            0x0A05,   // 0A05..0A0A; GURMUKHI
            0x0A0B,   // 0A0B..0A0E; UNKNOWN
            0x0A0F,   // 0A0F..0A10; GURMUKHI
            0x0A11,   // 0A11..0A12; UNKNOWN
            0x0A13,   // 0A13..0A28; GURMUKHI
            0x0A29,   // 0A29      ; UNKNOWN
            0x0A2A,   // 0A2A..0A30; GURMUKHI
            0x0A31,   // 0A31      ; UNKNOWN
            0x0A32,   // 0A32..0A33; GURMUKHI
            0x0A34,   // 0A34      ; UNKNOWN
            0x0A35,   // 0A35..0A36; GURMUKHI
            0x0A37,   // 0A37      ; UNKNOWN
            0x0A38,   // 0A38..0A39; GURMUKHI
            0x0A3A,   // 0A3A..0A3B; UNKNOWN
            0x0A3C,   // 0A3C      ; GURMUKHI
            0x0A3D,   // 0A3D      ; UNKNOWN
            0x0A3E,   // 0A3E..0A42; GURMUKHI
            0x0A43,   // 0A43..0A46; UNKNOWN
            0x0A47,   // 0A47..0A48; GURMUKHI
            0x0A49,   // 0A49..0A4A; UNKNOWN
            0x0A4B,   // 0A4B..0A4D; GURMUKHI
            0x0A4E,   // 0A4E..0A50; UNKNOWN
            0x0A51,   // 0A51      ; GURMUKHI
            0x0A52,   // 0A52..0A58; UNKNOWN
            0x0A59,   // 0A59..0A5C; GURMUKHI
            0x0A5D,   // 0A5D      ; UNKNOWN
            0x0A5E,   // 0A5E      ; GURMUKHI
            0x0A5F,   // 0A5F..0A65; UNKNOWN
            0x0A66,   // 0A66..0A76; GURMUKHI
            0x0A77,   // 0A77..0A80; UNKNOWN
            0x0A81,   // 0A81..0A83; GUJARATI
            0x0A84,   // 0A84      ; UNKNOWN
            0x0A85,   // 0A85..0A8D; GUJARATI
            0x0A8E,   // 0A8E      ; UNKNOWN
            0x0A8F,   // 0A8F..0A91; GUJARATI
            0x0A92,   // 0A92      ; UNKNOWN
            0x0A93,   // 0A93..0AA8; GUJARATI
            0x0AA9,   // 0AA9      ; UNKNOWN
            0x0AAA,   // 0AAA..0AB0; GUJARATI
            0x0AB1,   // 0AB1      ; UNKNOWN
            0x0AB2,   // 0AB2..0AB3; GUJARATI
            0x0AB4,   // 0AB4      ; UNKNOWN
            0x0AB5,   // 0AB5..0AB9; GUJARATI
            0x0ABA,   // 0ABA..0ABB; UNKNOWN
            0x0ABC,   // 0ABC..0AC5; GUJARATI
            0x0AC6,   // 0AC6      ; UNKNOWN
            0x0AC7,   // 0AC7..0AC9; GUJARATI
            0x0ACA,   // 0ACA      ; UNKNOWN
            0x0ACB,   // 0ACB..0ACD; GUJARATI
            0x0ACE,   // 0ACE..0ACF; UNKNOWN
            0x0AD0,   // 0AD0      ; GUJARATI
            0x0AD1,   // 0AD1..0ADF; UNKNOWN
            0x0AE0,   // 0AE0..0AE3; GUJARATI
            0x0AE4,   // 0AE4..0AE5; UNKNOWN
            0x0AE6,   // 0AE6..0AF1; GUJARATI
            0x0AF2,   // 0AF2..0AF8; UNKNOWN
            0x0AF9,   // 0AF9..0AFF; GUJARATI
            0x0B00,   // 0B00      ; UNKNOWN
            0x0B01,   // 0B01..0B03; ORIYA
            0x0B04,   // 0B04      ; UNKNOWN
            0x0B05,   // 0B05..0B0C; ORIYA
            0x0B0D,   // 0B0D..0B0E; UNKNOWN
            0x0B0F,   // 0B0F..0B10; ORIYA
            0x0B11,   // 0B11..0B12; UNKNOWN
            0x0B13,   // 0B13..0B28; ORIYA
            0x0B29,   // 0B29      ; UNKNOWN
            0x0B2A,   // 0B2A..0B30; ORIYA
            0x0B31,   // 0B31      ; UNKNOWN
            0x0B32,   // 0B32..0B33; ORIYA
            0x0B34,   // 0B34      ; UNKNOWN
            0x0B35,   // 0B35..0B39; ORIYA
            0x0B3A,   // 0B3A..0B3B; UNKNOWN
            0x0B3C,   // 0B3C..0B44; ORIYA
            0x0B45,   // 0B45..0B46; UNKNOWN
            0x0B47,   // 0B47..0B48; ORIYA
            0x0B49,   // 0B49..0B4A; UNKNOWN
            0x0B4B,   // 0B4B..0B4D; ORIYA
            0x0B4E,   // 0B4E..0B54; UNKNOWN
            0x0B55,   // 0B55..0B57; ORIYA
            0x0B58,   // 0B58..0B5B; UNKNOWN
            0x0B5C,   // 0B5C..0B5D; ORIYA
            0x0B5E,   // 0B5E      ; UNKNOWN
            0x0B5F,   // 0B5F..0B63; ORIYA
            0x0B64,   // 0B64..0B65; UNKNOWN
            0x0B66,   // 0B66..0B77; ORIYA
            0x0B78,   // 0B78..0B81; UNKNOWN
            0x0B82,   // 0B82..0B83; TAMIL
            0x0B84,   // 0B84      ; UNKNOWN
            0x0B85,   // 0B85..0B8A; TAMIL
            0x0B8B,   // 0B8B..0B8D; UNKNOWN
            0x0B8E,   // 0B8E..0B90; TAMIL
            0x0B91,   // 0B91      ; UNKNOWN
            0x0B92,   // 0B92..0B95; TAMIL
            0x0B96,   // 0B96..0B98; UNKNOWN
            0x0B99,   // 0B99..0B9A; TAMIL
            0x0B9B,   // 0B9B      ; UNKNOWN
            0x0B9C,   // 0B9C      ; TAMIL
            0x0B9D,   // 0B9D      ; UNKNOWN
            0x0B9E,   // 0B9E..0B9F; TAMIL
            0x0BA0,   // 0BA0..0BA2; UNKNOWN
            0x0BA3,   // 0BA3..0BA4; TAMIL
            0x0BA5,   // 0BA5..0BA7; UNKNOWN
            0x0BA8,   // 0BA8..0BAA; TAMIL
            0x0BAB,   // 0BAB..0BAD; UNKNOWN
            0x0BAE,   // 0BAE..0BB9; TAMIL
            0x0BBA,   // 0BBA..0BBD; UNKNOWN
            0x0BBE,   // 0BBE..0BC2; TAMIL
            0x0BC3,   // 0BC3..0BC5; UNKNOWN
            0x0BC6,   // 0BC6..0BC8; TAMIL
            0x0BC9,   // 0BC9      ; UNKNOWN
            0x0BCA,   // 0BCA..0BCD; TAMIL
            0x0BCE,   // 0BCE..0BCF; UNKNOWN
            0x0BD0,   // 0BD0      ; TAMIL
            0x0BD1,   // 0BD1..0BD6; UNKNOWN
            0x0BD7,   // 0BD7      ; TAMIL
            0x0BD8,   // 0BD8..0BE5; UNKNOWN
            0x0BE6,   // 0BE6..0BFA; TAMIL
            0x0BFB,   // 0BFB..0BFF; UNKNOWN
            0x0C00,   // 0C00..0C0C; TELUGU
            0x0C0D,   // 0C0D      ; UNKNOWN
            0x0C0E,   // 0C0E..0C10; TELUGU
            0x0C11,   // 0C11      ; UNKNOWN
            0x0C12,   // 0C12..0C28; TELUGU
            0x0C29,   // 0C29      ; UNKNOWN
            0x0C2A,   // 0C2A..0C39; TELUGU
            0x0C3A,   // 0C3A..0C3C; UNKNOWN
            0x0C3D,   // 0C3D..0C44; TELUGU
            0x0C45,   // 0C45      ; UNKNOWN
            0x0C46,   // 0C46..0C48; TELUGU
            0x0C49,   // 0C49      ; UNKNOWN
            0x0C4A,   // 0C4A..0C4D; TELUGU
            0x0C4E,   // 0C4E..0C54; UNKNOWN
            0x0C55,   // 0C55..0C56; TELUGU
            0x0C57,   // 0C57      ; UNKNOWN
            0x0C58,   // 0C58..0C5A; TELUGU
            0x0C5B,   // 0C5B..0C5F; UNKNOWN
            0x0C60,   // 0C60..0C63; TELUGU
            0x0C64,   // 0C64..0C65; UNKNOWN
            0x0C66,   // 0C66..0C6F; TELUGU
            0x0C70,   // 0C70..0C76; UNKNOWN
            0x0C77,   // 0C77..0C7F; TELUGU
            0x0C80,   // 0C80..0C8C; KANNADA
            0x0C8D,   // 0C8D      ; UNKNOWN
            0x0C8E,   // 0C8E..0C90; KANNADA
            0x0C91,   // 0C91      ; UNKNOWN
            0x0C92,   // 0C92..0CA8; KANNADA
            0x0CA9,   // 0CA9      ; UNKNOWN
            0x0CAA,   // 0CAA..0CB3; KANNADA
            0x0CB4,   // 0CB4      ; UNKNOWN
            0x0CB5,   // 0CB5..0CB9; KANNADA
            0x0CBA,   // 0CBA..0CBB; UNKNOWN
            0x0CBC,   // 0CBC..0CC4; KANNADA
            0x0CC5,   // 0CC5      ; UNKNOWN
            0x0CC6,   // 0CC6..0CC8; KANNADA
            0x0CC9,   // 0CC9      ; UNKNOWN
            0x0CCA,   // 0CCA..0CCD; KANNADA
            0x0CCE,   // 0CCE..0CD4; UNKNOWN
            0x0CD5,   // 0CD5..0CD6; KANNADA
            0x0CD7,   // 0CD7..0CDD; UNKNOWN
            0x0CDE,   // 0CDE      ; KANNADA
            0x0CDF,   // 0CDF      ; UNKNOWN
            0x0CE0,   // 0CE0..0CE3; KANNADA
            0x0CE4,   // 0CE4..0CE5; UNKNOWN
            0x0CE6,   // 0CE6..0CEF; KANNADA
            0x0CF0,   // 0CF0      ; UNKNOWN
            0x0CF1,   // 0CF1..0CF2; KANNADA
            0x0CF3,   // 0CF3..0CFF; UNKNOWN
            0x0D00,   // 0D00..0D0C; MALAYALAM
            0x0D0D,   // 0D0D      ; UNKNOWN
            0x0D0E,   // 0D0E..0D10; MALAYALAM
            0x0D11,   // 0D11      ; UNKNOWN
            0x0D12,   // 0D12..0D44; MALAYALAM
            0x0D45,   // 0D45      ; UNKNOWN
            0x0D46,   // 0D46..0D48; MALAYALAM
            0x0D49,   // 0D49      ; UNKNOWN
            0x0D4A,   // 0D4A..0D4F; MALAYALAM
            0x0D50,   // 0D50..0D53; UNKNOWN
            0x0D54,   // 0D54..0D63; MALAYALAM
            0x0D64,   // 0D64..0D65; UNKNOWN
            0x0D66,   // 0D66..0D7F; MALAYALAM
            0x0D80,   // 0D80      ; UNKNOWN
            0x0D81,   // 0D81..0D83; SINHALA
            0x0D84,   // 0D84      ; UNKNOWN
            0x0D85,   // 0D85..0D96; SINHALA
            0x0D97,   // 0D97..0D99; UNKNOWN
            0x0D9A,   // 0D9A..0DB1; SINHALA
            0x0DB2,   // 0DB2      ; UNKNOWN
            0x0DB3,   // 0DB3..0DBB; SINHALA
            0x0DBC,   // 0DBC      ; UNKNOWN
            0x0DBD,   // 0DBD      ; SINHALA
            0x0DBE,   // 0DBE..0DBF; UNKNOWN
            0x0DC0,   // 0DC0..0DC6; SINHALA
            0x0DC7,   // 0DC7..0DC9; UNKNOWN
            0x0DCA,   // 0DCA      ; SINHALA
            0x0DCB,   // 0DCB..0DCE; UNKNOWN
            0x0DCF,   // 0DCF..0DD4; SINHALA
            0x0DD5,   // 0DD5      ; UNKNOWN
            0x0DD6,   // 0DD6      ; SINHALA
            0x0DD7,   // 0DD7      ; UNKNOWN
            0x0DD8,   // 0DD8..0DDF; SINHALA
            0x0DE0,   // 0DE0..0DE5; UNKNOWN
            0x0DE6,   // 0DE6..0DEF; SINHALA
            0x0DF0,   // 0DF0..0DF1; UNKNOWN
            0x0DF2,   // 0DF2..0DF4; SINHALA
            0x0DF5,   // 0DF5..0E00; UNKNOWN
            0x0E01,   // 0E01..0E3A; THAI
            0x0E3B,   // 0E3B..0E3E; UNKNOWN
            0x0E3F,   // 0E3F      ; COMMON
            0x0E40,   // 0E40..0E5B; THAI
            0x0E5C,   // 0E5C..0E80; UNKNOWN
            0x0E81,   // 0E81..0E82; LAO
            0x0E83,   // 0E83      ; UNKNOWN
            0x0E84,   // 0E84      ; LAO
            0x0E85,   // 0E85      ; UNKNOWN
            0x0E86,   // 0E86..0E8A; LAO
            0x0E8B,   // 0E8B      ; UNKNOWN
            0x0E8C,   // 0E8C..0EA3; LAO
            0x0EA4,   // 0EA4      ; UNKNOWN
            0x0EA5,   // 0EA5      ; LAO
            0x0EA6,   // 0EA6      ; UNKNOWN
            0x0EA7,   // 0EA7..0EBD; LAO
            0x0EBE,   // 0EBE..0EBF; UNKNOWN
            0x0EC0,   // 0EC0..0EC4; LAO
            0x0EC5,   // 0EC5      ; UNKNOWN
            0x0EC6,   // 0EC6      ; LAO
            0x0EC7,   // 0EC7      ; UNKNOWN
            0x0EC8,   // 0EC8..0ECD; LAO
            0x0ECE,   // 0ECE..0ECF; UNKNOWN
            0x0ED0,   // 0ED0..0ED9; LAO
            0x0EDA,   // 0EDA..0EDB; UNKNOWN
            0x0EDC,   // 0EDC..0EDF; LAO
            0x0EE0,   // 0EE0..0EFF; UNKNOWN
            0x0F00,   // 0F00..0F47; TIBETAN
            0x0F48,   // 0F48      ; UNKNOWN
            0x0F49,   // 0F49..0F6C; TIBETAN
            0x0F6D,   // 0F6D..0F70; UNKNOWN
            0x0F71,   // 0F71..0F97; TIBETAN
            0x0F98,   // 0F98      ; UNKNOWN
            0x0F99,   // 0F99..0FBC; TIBETAN
            0x0FBD,   // 0FBD      ; UNKNOWN
            0x0FBE,   // 0FBE..0FCC; TIBETAN
            0x0FCD,   // 0FCD      ; UNKNOWN
            0x0FCE,   // 0FCE..0FD4; TIBETAN
            0x0FD5,   // 0FD5..0FD8; COMMON
            0x0FD9,   // 0FD9..0FDA; TIBETAN
            0x0FDB,   // 0FDB..0FFF; UNKNOWN
            0x1000,   // 1000..109F; MYANMAR
            0x10A0,   // 10A0..10C5; GEORGIAN
            0x10C6,   // 10C6      ; UNKNOWN
            0x10C7,   // 10C7      ; GEORGIAN
            0x10C8,   // 10C8..10CC; UNKNOWN
            0x10CD,   // 10CD      ; GEORGIAN
            0x10CE,   // 10CE..10CF; UNKNOWN
            0x10D0,   // 10D0..10FA; GEORGIAN
            0x10FB,   // 10FB      ; COMMON
            0x10FC,   // 10FC..10FF; GEORGIAN
            0x1100,   // 1100..11FF; HANGUL
            0x1200,   // 1200..1248; ETHIOPIC
            0x1249,   // 1249      ; UNKNOWN
            0x124A,   // 124A..124D; ETHIOPIC
            0x124E,   // 124E..124F; UNKNOWN
            0x1250,   // 1250..1256; ETHIOPIC
            0x1257,   // 1257      ; UNKNOWN
            0x1258,   // 1258      ; ETHIOPIC
            0x1259,   // 1259      ; UNKNOWN
            0x125A,   // 125A..125D; ETHIOPIC
            0x125E,   // 125E..125F; UNKNOWN
            0x1260,   // 1260..1288; ETHIOPIC
            0x1289,   // 1289      ; UNKNOWN
            0x128A,   // 128A..128D; ETHIOPIC
            0x128E,   // 128E..128F; UNKNOWN
            0x1290,   // 1290..12B0; ETHIOPIC
            0x12B1,   // 12B1      ; UNKNOWN
            0x12B2,   // 12B2..12B5; ETHIOPIC
            0x12B6,   // 12B6..12B7; UNKNOWN
            0x12B8,   // 12B8..12BE; ETHIOPIC
            0x12BF,   // 12BF      ; UNKNOWN
            0x12C0,   // 12C0      ; ETHIOPIC
            0x12C1,   // 12C1      ; UNKNOWN
            0x12C2,   // 12C2..12C5; ETHIOPIC
            0x12C6,   // 12C6..12C7; UNKNOWN
            0x12C8,   // 12C8..12D6; ETHIOPIC
            0x12D7,   // 12D7      ; UNKNOWN
            0x12D8,   // 12D8..1310; ETHIOPIC
            0x1311,   // 1311      ; UNKNOWN
            0x1312,   // 1312..1315; ETHIOPIC
            0x1316,   // 1316..1317; UNKNOWN
            0x1318,   // 1318..135A; ETHIOPIC
            0x135B,   // 135B..135C; UNKNOWN
            0x135D,   // 135D..137C; ETHIOPIC
            0x137D,   // 137D..137F; UNKNOWN
            0x1380,   // 1380..1399; ETHIOPIC
            0x139A,   // 139A..139F; UNKNOWN
            0x13A0,   // 13A0..13F5; CHEROKEE
            0x13F6,   // 13F6..13F7; UNKNOWN
            0x13F8,   // 13F8..13FD; CHEROKEE
            0x13FE,   // 13FE..13FF; UNKNOWN
            0x1400,   // 1400..167F; CANADIAN_ABORIGINAL
            0x1680,   // 1680..169C; OGHAM
            0x169D,   // 169D..169F; UNKNOWN
            0x16A0,   // 16A0..16EA; RUNIC
            0x16EB,   // 16EB..16ED; COMMON
            0x16EE,   // 16EE..16F8; RUNIC
            0x16F9,   // 16F9..16FF; UNKNOWN
            0x1700,   // 1700..170C; TAGALOG
            0x170D,   // 170D      ; UNKNOWN
            0x170E,   // 170E..1714; TAGALOG
            0x1715,   // 1715..171F; UNKNOWN
            0x1720,   // 1720..1734; HANUNOO
            0x1735,   // 1735..1736; COMMON
            0x1737,   // 1737..173F; UNKNOWN
            0x1740,   // 1740..1753; BUHID
            0x1754,   // 1754..175F; UNKNOWN
            0x1760,   // 1760..176C; TAGBANWA
            0x176D,   // 176D      ; UNKNOWN
            0x176E,   // 176E..1770; TAGBANWA
            0x1771,   // 1771      ; UNKNOWN
            0x1772,   // 1772..1773; TAGBANWA
            0x1774,   // 1774..177F; UNKNOWN
            0x1780,   // 1780..17DD; KHMER
            0x17DE,   // 17DE..17DF; UNKNOWN
            0x17E0,   // 17E0..17E9; KHMER
            0x17EA,   // 17EA..17EF; UNKNOWN
            0x17F0,   // 17F0..17F9; KHMER
            0x17FA,   // 17FA..17FF; UNKNOWN
            0x1800,   // 1800..1801; MONGOLIAN
            0x1802,   // 1802..1803; COMMON
            0x1804,   // 1804      ; MONGOLIAN
            0x1805,   // 1805      ; COMMON
            0x1806,   // 1806..180E; MONGOLIAN
            0x180F,   // 180F      ; UNKNOWN
            0x1810,   // 1810..1819; MONGOLIAN
            0x181A,   // 181A..181F; UNKNOWN
            0x1820,   // 1820..1878; MONGOLIAN
            0x1879,   // 1879..187F; UNKNOWN
            0x1880,   // 1880..18AA; MONGOLIAN
            0x18AB,   // 18AB..18AF; UNKNOWN
            0x18B0,   // 18B0..18F5; CANADIAN_ABORIGINAL
            0x18F6,   // 18F6..18FF; UNKNOWN
            0x1900,   // 1900..191E; LIMBU
            0x191F,   // 191F      ; UNKNOWN
            0x1920,   // 1920..192B; LIMBU
            0x192C,   // 192C..192F; UNKNOWN
            0x1930,   // 1930..193B; LIMBU
            0x193C,   // 193C..193F; UNKNOWN
            0x1940,   // 1940      ; LIMBU
            0x1941,   // 1941..1943; UNKNOWN
            0x1944,   // 1944..194F; LIMBU
            0x1950,   // 1950..196D; TAI_LE
            0x196E,   // 196E..196F; UNKNOWN
            0x1970,   // 1970..1974; TAI_LE
            0x1975,   // 1975..197F; UNKNOWN
            0x1980,   // 1980..19AB; NEW_TAI_LUE
            0x19AC,   // 19AC..19AF; UNKNOWN
            0x19B0,   // 19B0..19C9; NEW_TAI_LUE
            0x19CA,   // 19CA..19CF; UNKNOWN
            0x19D0,   // 19D0..19DA; NEW_TAI_LUE
            0x19DB,   // 19DB..19DD; UNKNOWN
            0x19DE,   // 19DE..19DF; NEW_TAI_LUE
            0x19E0,   // 19E0..19FF; KHMER
            0x1A00,   // 1A00..1A1B; BUGINESE
            0x1A1C,   // 1A1C..1A1D; UNKNOWN
            0x1A1E,   // 1A1E..1A1F; BUGINESE
            0x1A20,   // 1A20..1A5E; TAI_THAM
            0x1A5F,   // 1A5F      ; UNKNOWN
            0x1A60,   // 1A60..1A7C; TAI_THAM
            0x1A7D,   // 1A7D..1A7E; UNKNOWN
            0x1A7F,   // 1A7F..1A89; TAI_THAM
            0x1A8A,   // 1A8A..1A8F; UNKNOWN
            0x1A90,   // 1A90..1A99; TAI_THAM
            0x1A9A,   // 1A9A..1A9F; UNKNOWN
            0x1AA0,   // 1AA0..1AAD; TAI_THAM
            0x1AAE,   // 1AAE..1AAF; UNKNOWN
            0x1AB0,   // 1AB0..1AC0; INHERITED
            0x1AC1,   // 1AC1..1AFF; UNKNOWN
            0x1B00,   // 1B00..1B4B; BALINESE
            0x1B4C,   // 1B4C..1B4F; UNKNOWN
            0x1B50,   // 1B50..1B7C; BALINESE
            0x1B7D,   // 1B7D..1B7F; UNKNOWN
            0x1B80,   // 1B80..1BBF; SUNDANESE
            0x1BC0,   // 1BC0..1BF3; BATAK
            0x1BF4,   // 1BF4..1BFB; UNKNOWN
            0x1BFC,   // 1BFC..1BFF; BATAK
            0x1C00,   // 1C00..1C37; LEPCHA
            0x1C38,   // 1C38..1C3A; UNKNOWN
            0x1C3B,   // 1C3B..1C49; LEPCHA
            0x1C4A,   // 1C4A..1C4C; UNKNOWN
            0x1C4D,   // 1C4D..1C4F; LEPCHA
            0x1C50,   // 1C50..1C7F; OL_CHIKI
            0x1C80,   // 1C80..1C88; CYRILLIC
            0x1C89,   // 1C89..1C8F; UNKNOWN
            0x1C90,   // 1C90..1CBA; GEORGIAN
            0x1CBB,   // 1CBB..1CBC; UNKNOWN
            0x1CBD,   // 1CBD..1CBF; GEORGIAN
            0x1CC0,   // 1CC0..1CC7; SUNDANESE
            0x1CC8,   // 1CC8..1CCF; UNKNOWN
            0x1CD0,   // 1CD0..1CD2; INHERITED
            0x1CD3,   // 1CD3      ; COMMON
            0x1CD4,   // 1CD4..1CE0; INHERITED
            0x1CE1,   // 1CE1      ; COMMON
            0x1CE2,   // 1CE2..1CE8; INHERITED
            0x1CE9,   // 1CE9..1CEC; COMMON
            0x1CED,   // 1CED      ; INHERITED
            0x1CEE,   // 1CEE..1CF3; COMMON
            0x1CF4,   // 1CF4      ; INHERITED
            0x1CF5,   // 1CF5..1CF7; COMMON
            0x1CF8,   // 1CF8..1CF9; INHERITED
            0x1CFA,   // 1CFA      ; COMMON
            0x1CFB,   // 1CFB..1CFF; UNKNOWN
            0x1D00,   // 1D00..1D25; LATIN
            0x1D26,   // 1D26..1D2A; GREEK
            0x1D2B,   // 1D2B      ; CYRILLIC
            0x1D2C,   // 1D2C..1D5C; LATIN
            0x1D5D,   // 1D5D..1D61; GREEK
            0x1D62,   // 1D62..1D65; LATIN
            0x1D66,   // 1D66..1D6A; GREEK
            0x1D6B,   // 1D6B..1D77; LATIN
            0x1D78,   // 1D78      ; CYRILLIC
            0x1D79,   // 1D79..1DBE; LATIN
            0x1DBF,   // 1DBF      ; GREEK
            0x1DC0,   // 1DC0..1DF9; INHERITED
            0x1DFA,   // 1DFA      ; UNKNOWN
            0x1DFB,   // 1DFB..1DFF; INHERITED
            0x1E00,   // 1E00..1EFF; LATIN
            0x1F00,   // 1F00..1F15; GREEK
            0x1F16,   // 1F16..1F17; UNKNOWN
            0x1F18,   // 1F18..1F1D; GREEK
            0x1F1E,   // 1F1E..1F1F; UNKNOWN
            0x1F20,   // 1F20..1F45; GREEK
            0x1F46,   // 1F46..1F47; UNKNOWN
            0x1F48,   // 1F48..1F4D; GREEK
            0x1F4E,   // 1F4E..1F4F; UNKNOWN
            0x1F50,   // 1F50..1F57; GREEK
            0x1F58,   // 1F58      ; UNKNOWN
            0x1F59,   // 1F59      ; GREEK
            0x1F5A,   // 1F5A      ; UNKNOWN
            0x1F5B,   // 1F5B      ; GREEK
            0x1F5C,   // 1F5C      ; UNKNOWN
            0x1F5D,   // 1F5D      ; GREEK
            0x1F5E,   // 1F5E      ; UNKNOWN
            0x1F5F,   // 1F5F..1F7D; GREEK
            0x1F7E,   // 1F7E..1F7F; UNKNOWN
            0x1F80,   // 1F80..1FB4; GREEK
            0x1FB5,   // 1FB5      ; UNKNOWN
            0x1FB6,   // 1FB6..1FC4; GREEK
            0x1FC5,   // 1FC5      ; UNKNOWN
            0x1FC6,   // 1FC6..1FD3; GREEK
            0x1FD4,   // 1FD4..1FD5; UNKNOWN
            0x1FD6,   // 1FD6..1FDB; GREEK
            0x1FDC,   // 1FDC      ; UNKNOWN
            0x1FDD,   // 1FDD..1FEF; GREEK
            0x1FF0,   // 1FF0..1FF1; UNKNOWN
            0x1FF2,   // 1FF2..1FF4; GREEK
            0x1FF5,   // 1FF5      ; UNKNOWN
            0x1FF6,   // 1FF6..1FFE; GREEK
            0x1FFF,   // 1FFF      ; UNKNOWN
            0x2000,   // 2000..200B; COMMON
            0x200C,   // 200C..200D; INHERITED
            0x200E,   // 200E..2064; COMMON
            0x2065,   // 2065      ; UNKNOWN
            0x2066,   // 2066..2070; COMMON
            0x2071,   // 2071      ; LATIN
            0x2072,   // 2072..2073; UNKNOWN
            0x2074,   // 2074..207E; COMMON
            0x207F,   // 207F      ; LATIN
            0x2080,   // 2080..208E; COMMON
            0x208F,   // 208F      ; UNKNOWN
            0x2090,   // 2090..209C; LATIN
            0x209D,   // 209D..209F; UNKNOWN
            0x20A0,   // 20A0..20BF; COMMON
            0x20C0,   // 20C0..20CF; UNKNOWN
            0x20D0,   // 20D0..20F0; INHERITED
            0x20F1,   // 20F1..20FF; UNKNOWN
            0x2100,   // 2100..2125; COMMON
            0x2126,   // 2126      ; GREEK
            0x2127,   // 2127..2129; COMMON
            0x212A,   // 212A..212B; LATIN
            0x212C,   // 212C..2131; COMMON
            0x2132,   // 2132      ; LATIN
            0x2133,   // 2133..214D; COMMON
            0x214E,   // 214E      ; LATIN
            0x214F,   // 214F..215F; COMMON
            0x2160,   // 2160..2188; LATIN
            0x2189,   // 2189..218B; COMMON
            0x218C,   // 218C..218F; UNKNOWN
            0x2190,   // 2190..2426; COMMON
            0x2427,   // 2427..243F; UNKNOWN
            0x2440,   // 2440..244A; COMMON
            0x244B,   // 244B..245F; UNKNOWN
            0x2460,   // 2460..27FF; COMMON
            0x2800,   // 2800..28FF; BRAILLE
            0x2900,   // 2900..2B73; COMMON
            0x2B74,   // 2B74..2B75; UNKNOWN
            0x2B76,   // 2B76..2B95; COMMON
            0x2B96,   // 2B96      ; UNKNOWN
            0x2B97,   // 2B97..2BFF; COMMON
            0x2C00,   // 2C00..2C2E; GLAGOLITIC
            0x2C2F,   // 2C2F      ; UNKNOWN
            0x2C30,   // 2C30..2C5E; GLAGOLITIC
            0x2C5F,   // 2C5F      ; UNKNOWN
            0x2C60,   // 2C60..2C7F; LATIN
            0x2C80,   // 2C80..2CF3; COPTIC
            0x2CF4,   // 2CF4..2CF8; UNKNOWN
            0x2CF9,   // 2CF9..2CFF; COPTIC
            0x2D00,   // 2D00..2D25; GEORGIAN
            0x2D26,   // 2D26      ; UNKNOWN
            0x2D27,   // 2D27      ; GEORGIAN
            0x2D28,   // 2D28..2D2C; UNKNOWN
            0x2D2D,   // 2D2D      ; GEORGIAN
            0x2D2E,   // 2D2E..2D2F; UNKNOWN
            0x2D30,   // 2D30..2D67; TIFINAGH
            0x2D68,   // 2D68..2D6E; UNKNOWN
            0x2D6F,   // 2D6F..2D70; TIFINAGH
            0x2D71,   // 2D71..2D7E; UNKNOWN
            0x2D7F,   // 2D7F      ; TIFINAGH
            0x2D80,   // 2D80..2D96; ETHIOPIC
            0x2D97,   // 2D97..2D9F; UNKNOWN
            0x2DA0,   // 2DA0..2DA6; ETHIOPIC
            0x2DA7,   // 2DA7      ; UNKNOWN
            0x2DA8,   // 2DA8..2DAE; ETHIOPIC
            0x2DAF,   // 2DAF      ; UNKNOWN
            0x2DB0,   // 2DB0..2DB6; ETHIOPIC
            0x2DB7,   // 2DB7      ; UNKNOWN
            0x2DB8,   // 2DB8..2DBE; ETHIOPIC
            0x2DBF,   // 2DBF      ; UNKNOWN
            0x2DC0,   // 2DC0..2DC6; ETHIOPIC
            0x2DC7,   // 2DC7      ; UNKNOWN
            0x2DC8,   // 2DC8..2DCE; ETHIOPIC
            0x2DCF,   // 2DCF      ; UNKNOWN
            0x2DD0,   // 2DD0..2DD6; ETHIOPIC
            0x2DD7,   // 2DD7      ; UNKNOWN
            0x2DD8,   // 2DD8..2DDE; ETHIOPIC
            0x2DDF,   // 2DDF      ; UNKNOWN
            0x2DE0,   // 2DE0..2DFF; CYRILLIC
            0x2E00,   // 2E00..2E52; COMMON
            0x2E53,   // 2E53..2E7F; UNKNOWN
            0x2E80,   // 2E80..2E99; HAN
            0x2E9A,   // 2E9A      ; UNKNOWN
            0x2E9B,   // 2E9B..2EF3; HAN
            0x2EF4,   // 2EF4..2EFF; UNKNOWN
            0x2F00,   // 2F00..2FD5; HAN
            0x2FD6,   // 2FD6..2FEF; UNKNOWN
            0x2FF0,   // 2FF0..2FFB; COMMON
            0x2FFC,   // 2FFC..2FFF; UNKNOWN
            0x3000,   // 3000..3004; COMMON
            0x3005,   // 3005      ; HAN
            0x3006,   // 3006      ; COMMON
            0x3007,   // 3007      ; HAN
            0x3008,   // 3008..3020; COMMON
            0x3021,   // 3021..3029; HAN
            0x302A,   // 302A..302D; INHERITED
            0x302E,   // 302E..302F; HANGUL
            0x3030,   // 3030..3037; COMMON
            0x3038,   // 3038..303B; HAN
            0x303C,   // 303C..303F; COMMON
            0x3040,   // 3040      ; UNKNOWN
            0x3041,   // 3041..3096; HIRAGANA
            0x3097,   // 3097..3098; UNKNOWN
            0x3099,   // 3099..309A; INHERITED
            0x309B,   // 309B..309C; COMMON
            0x309D,   // 309D..309F; HIRAGANA
            0x30A0,   // 30A0      ; COMMON
            0x30A1,   // 30A1..30FA; KATAKANA
            0x30FB,   // 30FB..30FC; COMMON
            0x30FD,   // 30FD..30FF; KATAKANA
            0x3100,   // 3100..3104; UNKNOWN
            0x3105,   // 3105..312F; BOPOMOFO
            0x3130,   // 3130      ; UNKNOWN
            0x3131,   // 3131..318E; HANGUL
            0x318F,   // 318F      ; UNKNOWN
            0x3190,   // 3190..319F; COMMON
            0x31A0,   // 31A0..31BF; BOPOMOFO
            0x31C0,   // 31C0..31E3; COMMON
            0x31E4,   // 31E4..31EF; UNKNOWN
            0x31F0,   // 31F0..31FF; KATAKANA
            0x3200,   // 3200..321E; HANGUL
            0x321F,   // 321F      ; UNKNOWN
            0x3220,   // 3220..325F; COMMON
            0x3260,   // 3260..327E; HANGUL
            0x327F,   // 327F..32CF; COMMON
            0x32D0,   // 32D0..32FE; KATAKANA
            0x32FF,   // 32FF      ; COMMON
            0x3300,   // 3300..3357; KATAKANA
            0x3358,   // 3358..33FF; COMMON
            0x3400,   // 3400..4DBF; HAN
            0x4DC0,   // 4DC0..4DFF; COMMON
            0x4E00,   // 4E00..9FFC; HAN
            0x9FFD,   // 9FFD..9FFF; UNKNOWN
            0xA000,   // A000..A48C; YI
            0xA48D,   // A48D..A48F; UNKNOWN
            0xA490,   // A490..A4C6; YI
            0xA4C7,   // A4C7..A4CF; UNKNOWN
            0xA4D0,   // A4D0..A4FF; LISU
            0xA500,   // A500..A62B; VAI
            0xA62C,   // A62C..A63F; UNKNOWN
            0xA640,   // A640..A69F; CYRILLIC
            0xA6A0,   // A6A0..A6F7; BAMUM
            0xA6F8,   // A6F8..A6FF; UNKNOWN
            0xA700,   // A700..A721; COMMON
            0xA722,   // A722..A787; LATIN
            0xA788,   // A788..A78A; COMMON
            0xA78B,   // A78B..A7BF; LATIN
            0xA7C0,   // A7C0..A7C1; UNKNOWN
            0xA7C2,   // A7C2..A7CA; LATIN
            0xA7CB,   // A7CB..A7F4; UNKNOWN
            0xA7F5,   // A7F5..A7FF; LATIN
            0xA800,   // A800..A82C; SYLOTI_NAGRI
            0xA82D,   // A82D..A82F; UNKNOWN
            0xA830,   // A830..A839; COMMON
            0xA83A,   // A83A..A83F; UNKNOWN
            0xA840,   // A840..A877; PHAGS_PA
            0xA878,   // A878..A87F; UNKNOWN
            0xA880,   // A880..A8C5; SAURASHTRA
            0xA8C6,   // A8C6..A8CD; UNKNOWN
            0xA8CE,   // A8CE..A8D9; SAURASHTRA
            0xA8DA,   // A8DA..A8DF; UNKNOWN
            0xA8E0,   // A8E0..A8FF; DEVANAGARI
            0xA900,   // A900..A92D; KAYAH_LI
            0xA92E,   // A92E      ; COMMON
            0xA92F,   // A92F      ; KAYAH_LI
            0xA930,   // A930..A953; REJANG
            0xA954,   // A954..A95E; UNKNOWN
            0xA95F,   // A95F      ; REJANG
            0xA960,   // A960..A97C; HANGUL
            0xA97D,   // A97D..A97F; UNKNOWN
            0xA980,   // A980..A9CD; JAVANESE
            0xA9CE,   // A9CE      ; UNKNOWN
            0xA9CF,   // A9CF      ; COMMON
            0xA9D0,   // A9D0..A9D9; JAVANESE
            0xA9DA,   // A9DA..A9DD; UNKNOWN
            0xA9DE,   // A9DE..A9DF; JAVANESE
            0xA9E0,   // A9E0..A9FE; MYANMAR
            0xA9FF,   // A9FF      ; UNKNOWN
            0xAA00,   // AA00..AA36; CHAM
            0xAA37,   // AA37..AA3F; UNKNOWN
            0xAA40,   // AA40..AA4D; CHAM
            0xAA4E,   // AA4E..AA4F; UNKNOWN
            0xAA50,   // AA50..AA59; CHAM
            0xAA5A,   // AA5A..AA5B; UNKNOWN
            0xAA5C,   // AA5C..AA5F; CHAM
            0xAA60,   // AA60..AA7F; MYANMAR
            0xAA80,   // AA80..AAC2; TAI_VIET
            0xAAC3,   // AAC3..AADA; UNKNOWN
            0xAADB,   // AADB..AADF; TAI_VIET
            0xAAE0,   // AAE0..AAF6; MEETEI_MAYEK
            0xAAF7,   // AAF7..AB00; UNKNOWN
            0xAB01,   // AB01..AB06; ETHIOPIC
            0xAB07,   // AB07..AB08; UNKNOWN
            0xAB09,   // AB09..AB0E; ETHIOPIC
            0xAB0F,   // AB0F..AB10; UNKNOWN
            0xAB11,   // AB11..AB16; ETHIOPIC
            0xAB17,   // AB17..AB1F; UNKNOWN
            0xAB20,   // AB20..AB26; ETHIOPIC
            0xAB27,   // AB27      ; UNKNOWN
            0xAB28,   // AB28..AB2E; ETHIOPIC
            0xAB2F,   // AB2F      ; UNKNOWN
            0xAB30,   // AB30..AB5A; LATIN
            0xAB5B,   // AB5B      ; COMMON
            0xAB5C,   // AB5C..AB64; LATIN
            0xAB65,   // AB65      ; GREEK
            0xAB66,   // AB66..AB69; LATIN
            0xAB6A,   // AB6A..AB6B; COMMON
            0xAB6C,   // AB6C..AB6F; UNKNOWN
            0xAB70,   // AB70..ABBF; CHEROKEE
            0xABC0,   // ABC0..ABED; MEETEI_MAYEK
            0xABEE,   // ABEE..ABEF; UNKNOWN
            0xABF0,   // ABF0..ABF9; MEETEI_MAYEK
            0xABFA,   // ABFA..ABFF; UNKNOWN
            0xAC00,   // AC00..D7A3; HANGUL
            0xD7A4,   // D7A4..D7AF; UNKNOWN
            0xD7B0,   // D7B0..D7C6; HANGUL
            0xD7C7,   // D7C7..D7CA; UNKNOWN
            0xD7CB,   // D7CB..D7FB; HANGUL
            0xD7FC,   // D7FC..F8FF; UNKNOWN
            0xF900,   // F900..FA6D; HAN
            0xFA6E,   // FA6E..FA6F; UNKNOWN
            0xFA70,   // FA70..FAD9; HAN
            0xFADA,   // FADA..FAFF; UNKNOWN
            0xFB00,   // FB00..FB06; LATIN
            0xFB07,   // FB07..FB12; UNKNOWN
            0xFB13,   // FB13..FB17; ARMENIAN
            0xFB18,   // FB18..FB1C; UNKNOWN
            0xFB1D,   // FB1D..FB36; HEBREW
            0xFB37,   // FB37      ; UNKNOWN
            0xFB38,   // FB38..FB3C; HEBREW
            0xFB3D,   // FB3D      ; UNKNOWN
            0xFB3E,   // FB3E      ; HEBREW
            0xFB3F,   // FB3F      ; UNKNOWN
            0xFB40,   // FB40..FB41; HEBREW
            0xFB42,   // FB42      ; UNKNOWN
            0xFB43,   // FB43..FB44; HEBREW
            0xFB45,   // FB45      ; UNKNOWN
            0xFB46,   // FB46..FB4F; HEBREW
            0xFB50,   // FB50..FBC1; ARABIC
            0xFBC2,   // FBC2..FBD2; UNKNOWN
            0xFBD3,   // FBD3..FD3D; ARABIC
            0xFD3E,   // FD3E..FD3F; COMMON
            0xFD40,   // FD40..FD4F; UNKNOWN
            0xFD50,   // FD50..FD8F; ARABIC
            0xFD90,   // FD90..FD91; UNKNOWN
            0xFD92,   // FD92..FDC7; ARABIC
            0xFDC8,   // FDC8..FDEF; UNKNOWN
            0xFDF0,   // FDF0..FDFD; ARABIC
            0xFDFE,   // FDFE..FDFF; UNKNOWN
            0xFE00,   // FE00..FE0F; INHERITED
            0xFE10,   // FE10..FE19; COMMON
            0xFE1A,   // FE1A..FE1F; UNKNOWN
            0xFE20,   // FE20..FE2D; INHERITED
            0xFE2E,   // FE2E..FE2F; CYRILLIC
            0xFE30,   // FE30..FE52; COMMON
            0xFE53,   // FE53      ; UNKNOWN
            0xFE54,   // FE54..FE66; COMMON
            0xFE67,   // FE67      ; UNKNOWN
            0xFE68,   // FE68..FE6B; COMMON
            0xFE6C,   // FE6C..FE6F; UNKNOWN
            0xFE70,   // FE70..FE74; ARABIC
            0xFE75,   // FE75      ; UNKNOWN
            0xFE76,   // FE76..FEFC; ARABIC
            0xFEFD,   // FEFD..FEFE; UNKNOWN
            0xFEFF,   // FEFF      ; COMMON
            0xFF00,   // FF00      ; UNKNOWN
            0xFF01,   // FF01..FF20; COMMON
            0xFF21,   // FF21..FF3A; LATIN
            0xFF3B,   // FF3B..FF40; COMMON
            0xFF41,   // FF41..FF5A; LATIN
            0xFF5B,   // FF5B..FF65; COMMON
            0xFF66,   // FF66..FF6F; KATAKANA
            0xFF70,   // FF70      ; COMMON
            0xFF71,   // FF71..FF9D; KATAKANA
            0xFF9E,   // FF9E..FF9F; COMMON
            0xFFA0,   // FFA0..FFBE; HANGUL
            0xFFBF,   // FFBF..FFC1; UNKNOWN
            0xFFC2,   // FFC2..FFC7; HANGUL
            0xFFC8,   // FFC8..FFC9; UNKNOWN
            0xFFCA,   // FFCA..FFCF; HANGUL
            0xFFD0,   // FFD0..FFD1; UNKNOWN
            0xFFD2,   // FFD2..FFD7; HANGUL
            0xFFD8,   // FFD8..FFD9; UNKNOWN
            0xFFDA,   // FFDA..FFDC; HANGUL
            0xFFDD,   // FFDD..FFDF; UNKNOWN
            0xFFE0,   // FFE0..FFE6; COMMON
            0xFFE7,   // FFE7      ; UNKNOWN
            0xFFE8,   // FFE8..FFEE; COMMON
            0xFFEF,   // FFEF..FFF8; UNKNOWN
            0xFFF9,   // FFF9..FFFD; COMMON
            0xFFFE,   // FFFE..FFFF; UNKNOWN
            0x10000,  // 10000..1000B; LINEAR_B
            0x1000C,  // 1000C       ; UNKNOWN
            0x1000D,  // 1000D..10026; LINEAR_B
            0x10027,  // 10027       ; UNKNOWN
            0x10028,  // 10028..1003A; LINEAR_B
            0x1003B,  // 1003B       ; UNKNOWN
            0x1003C,  // 1003C..1003D; LINEAR_B
            0x1003E,  // 1003E       ; UNKNOWN
            0x1003F,  // 1003F..1004D; LINEAR_B
            0x1004E,  // 1004E..1004F; UNKNOWN
            0x10050,  // 10050..1005D; LINEAR_B
            0x1005E,  // 1005E..1007F; UNKNOWN
            0x10080,  // 10080..100FA; LINEAR_B
            0x100FB,  // 100FB..100FF; UNKNOWN
            0x10100,  // 10100..10102; COMMON
            0x10103,  // 10103..10106; UNKNOWN
            0x10107,  // 10107..10133; COMMON
            0x10134,  // 10134..10136; UNKNOWN
            0x10137,  // 10137..1013F; COMMON
            0x10140,  // 10140..1018E; GREEK
            0x1018F,  // 1018F       ; UNKNOWN
            0x10190,  // 10190..1019C; COMMON
            0x1019D,  // 1019D..1019F; UNKNOWN
            0x101A0,  // 101A0       ; GREEK
            0x101A1,  // 101A1..101CF; UNKNOWN
            0x101D0,  // 101D0..101FC; COMMON
            0x101FD,  // 101FD       ; INHERITED
            0x101FE,  // 101FE..1027F; UNKNOWN
            0x10280,  // 10280..1029C; LYCIAN
            0x1029D,  // 1029D..1029F; UNKNOWN
            0x102A0,  // 102A0..102D0; CARIAN
            0x102D1,  // 102D1..102DF; UNKNOWN
            0x102E0,  // 102E0       ; INHERITED
            0x102E1,  // 102E1..102FB; COMMON
            0x102FC,  // 102FC..102FF; UNKNOWN
            0x10300,  // 10300..10323; OLD_ITALIC
            0x10324,  // 10324..1032C; UNKNOWN
            0x1032D,  // 1032D..1032F; OLD_ITALIC
            0x10330,  // 10330..1034A; GOTHIC
            0x1034B,  // 1034B..1034F; UNKNOWN
            0x10350,  // 10350..1037A; OLD_PERMIC
            0x1037B,  // 1037B..1037F; UNKNOWN
            0x10380,  // 10380..1039D; UGARITIC
            0x1039E,  // 1039E       ; UNKNOWN
            0x1039F,  // 1039F       ; UGARITIC
            0x103A0,  // 103A0..103C3; OLD_PERSIAN
            0x103C4,  // 103C4..103C7; UNKNOWN
            0x103C8,  // 103C8..103D5; OLD_PERSIAN
            0x103D6,  // 103D6..103FF; UNKNOWN
            0x10400,  // 10400..1044F; DESERET
            0x10450,  // 10450..1047F; SHAVIAN
            0x10480,  // 10480..1049D; OSMANYA
            0x1049E,  // 1049E..1049F; UNKNOWN
            0x104A0,  // 104A0..104A9; OSMANYA
            0x104AA,  // 104AA..104AF; UNKNOWN
            0x104B0,  // 104B0..104D3; OSAGE
            0x104D4,  // 104D4..104D7; UNKNOWN
            0x104D8,  // 104D8..104FB; OSAGE
            0x104FC,  // 104FC..104FF; UNKNOWN
            0x10500,  // 10500..10527; ELBASAN
            0x10528,  // 10528..1052F; UNKNOWN
            0x10530,  // 10530..10563; CAUCASIAN_ALBANIAN
            0x10564,  // 10564..1056E; UNKNOWN
            0x1056F,  // 1056F       ; CAUCASIAN_ALBANIAN
            0x10570,  // 10570..105FF; UNKNOWN
            0x10600,  // 10600..10736; LINEAR_A
            0x10737,  // 10737..1073F; UNKNOWN
            0x10740,  // 10740..10755; LINEAR_A
            0x10756,  // 10756..1075F; UNKNOWN
            0x10760,  // 10760..10767; LINEAR_A
            0x10768,  // 10768..107FF; UNKNOWN
            0x10800,  // 10800..10805; CYPRIOT
            0x10806,  // 10806..10807; UNKNOWN
            0x10808,  // 10808       ; CYPRIOT
            0x10809,  // 10809       ; UNKNOWN
            0x1080A,  // 1080A..10835; CYPRIOT
            0x10836,  // 10836       ; UNKNOWN
            0x10837,  // 10837..10838; CYPRIOT
            0x10839,  // 10839..1083B; UNKNOWN
            0x1083C,  // 1083C       ; CYPRIOT
            0x1083D,  // 1083D..1083E; UNKNOWN
            0x1083F,  // 1083F       ; CYPRIOT
            0x10840,  // 10840..10855; IMPERIAL_ARAMAIC
            0x10856,  // 10856       ; UNKNOWN
            0x10857,  // 10857..1085F; IMPERIAL_ARAMAIC
            0x10860,  // 10860..1087F; PALMYRENE
            0x10880,  // 10880..1089E; NABATAEAN
            0x1089F,  // 1089F..108A6; UNKNOWN
            0x108A7,  // 108A7..108AF; NABATAEAN
            0x108B0,  // 108B0..108DF; UNKNOWN
            0x108E0,  // 108E0..108F2; HATRAN
            0x108F3,  // 108F3       ; UNKNOWN
            0x108F4,  // 108F4..108F5; HATRAN
            0x108F6,  // 108F6..108FA; UNKNOWN
            0x108FB,  // 108FB..108FF; HATRAN
            0x10900,  // 10900..1091B; PHOENICIAN
            0x1091C,  // 1091C..1091E; UNKNOWN
            0x1091F,  // 1091F       ; PHOENICIAN
            0x10920,  // 10920..10939; LYDIAN
            0x1093A,  // 1093A..1093E; UNKNOWN
            0x1093F,  // 1093F       ; LYDIAN
            0x10940,  // 10940..1097F; UNKNOWN
            0x10980,  // 10980..1099F; MEROITIC_HIEROGLYPHS
            0x109A0,  // 109A0..109B7; MEROITIC_CURSIVE
            0x109B8,  // 109B8..109BB; UNKNOWN
            0x109BC,  // 109BC..109CF; MEROITIC_CURSIVE
            0x109D0,  // 109D0..109D1; UNKNOWN
            0x109D2,  // 109D2..109FF; MEROITIC_CURSIVE
            0x10A00,  // 10A00..10A03; KHAROSHTHI
            0x10A04,  // 10A04       ; UNKNOWN
            0x10A05,  // 10A05..10A06; KHAROSHTHI
            0x10A07,  // 10A07..10A0B; UNKNOWN
            0x10A0C,  // 10A0C..10A13; KHAROSHTHI
            0x10A14,  // 10A14       ; UNKNOWN
            0x10A15,  // 10A15..10A17; KHAROSHTHI
            0x10A18,  // 10A18       ; UNKNOWN
            0x10A19,  // 10A19..10A35; KHAROSHTHI
            0x10A36,  // 10A36..10A37; UNKNOWN
            0x10A38,  // 10A38..10A3A; KHAROSHTHI
            0x10A3B,  // 10A3B..10A3E; UNKNOWN
            0x10A3F,  // 10A3F..10A48; KHAROSHTHI
            0x10A49,  // 10A49..10A4F; UNKNOWN
            0x10A50,  // 10A50..10A58; KHAROSHTHI
            0x10A59,  // 10A59..10A5F; UNKNOWN
            0x10A60,  // 10A60..10A7F; OLD_SOUTH_ARABIAN
            0x10A80,  // 10A80..10A9F; OLD_NORTH_ARABIAN
            0x10AA0,  // 10AA0..10ABF; UNKNOWN
            0x10AC0,  // 10AC0..10AE6; MANICHAEAN
            0x10AE7,  // 10AE7..10AEA; UNKNOWN
            0x10AEB,  // 10AEB..10AF6; MANICHAEAN
            0x10AF7,  // 10AF7..10AFF; UNKNOWN
            0x10B00,  // 10B00..10B35; AVESTAN
            0x10B36,  // 10B36..10B38; UNKNOWN
            0x10B39,  // 10B39..10B3F; AVESTAN
            0x10B40,  // 10B40..10B55; INSCRIPTIONAL_PARTHIAN
            0x10B56,  // 10B56..10B57; UNKNOWN
            0x10B58,  // 10B58..10B5F; INSCRIPTIONAL_PARTHIAN
            0x10B60,  // 10B60..10B72; INSCRIPTIONAL_PAHLAVI
            0x10B73,  // 10B73..10B77; UNKNOWN
            0x10B78,  // 10B78..10B7F; INSCRIPTIONAL_PAHLAVI
            0x10B80,  // 10B80..10B91; PSALTER_PAHLAVI
            0x10B92,  // 10B92..10B98; UNKNOWN
            0x10B99,  // 10B99..10B9C; PSALTER_PAHLAVI
            0x10B9D,  // 10B9D..10BA8; UNKNOWN
            0x10BA9,  // 10BA9..10BAF; PSALTER_PAHLAVI
            0x10BB0,  // 10BB0..10BFF; UNKNOWN
            0x10C00,  // 10C00..10C48; OLD_TURKIC
            0x10C49,  // 10C49..10C7F; UNKNOWN
            0x10C80,  // 10C80..10CB2; OLD_HUNGARIAN
            0x10CB3,  // 10CB3..10CBF; UNKNOWN
            0x10CC0,  // 10CC0..10CF2; OLD_HUNGARIAN
            0x10CF3,  // 10CF3..10CF9; UNKNOWN
            0x10CFA,  // 10CFA..10CFF; OLD_HUNGARIAN
            0x10D00,  // 10D00..10D27; HANIFI_ROHINGYA
            0x10D28,  // 10D28..10D2F; UNKNOWN
            0x10D30,  // 10D30..10D39; HANIFI_ROHINGYA
            0x10D3A,  // 10D3A..10E5F; UNKNOWN
            0x10E60,  // 10E60..10E7E; ARABIC
            0x10E7F,  // 10E7F       ; UNKNOWN
            0x10E80,  // 10E80..10EA9; YEZIDI
            0x10EAA,  // 10EAA       ; UNKNOWN
            0x10EAB,  // 10EAB..10EAD; YEZIDI
            0x10EAE,  // 10EAE..10EAF; UNKNOWN
            0x10EB0,  // 10EB0..10EB1; YEZIDI
            0x10EB2,  // 10EB2..10EFF; UNKNOWN
            0x10F00,  // 10F00..10F27; OLD_SOGDIAN
            0x10F28,  // 10F28..10F2F; UNKNOWN
            0x10F30,  // 10F30..10F59; SOGDIAN
            0x10F5A,  // 10F5A..10FAF; UNKNOWN
            0x10FB0,  // 10FB0..10FCB; CHORASMIAN
            0x10FCC,  // 10FCC..10FDF; UNKNOWN
            0x10FE0,  // 10FE0..10FF6; ELYMAIC
            0x10FF7,  // 10FF7..10FFF; UNKNOWN
            0x11000,  // 11000..1104D; BRAHMI
            0x1104E,  // 1104E..11051; UNKNOWN
            0x11052,  // 11052..1106F; BRAHMI
            0x11070,  // 11070..1107E; UNKNOWN
            0x1107F,  // 1107F       ; BRAHMI
            0x11080,  // 11080..110C1; KAITHI
            0x110C2,  // 110C2..110CC; UNKNOWN
            0x110CD,  // 110CD       ; KAITHI
            0x110CE,  // 110CE..110CF; UNKNOWN
            0x110D0,  // 110D0..110E8; SORA_SOMPENG
            0x110E9,  // 110E9..110EF; UNKNOWN
            0x110F0,  // 110F0..110F9; SORA_SOMPENG
            0x110FA,  // 110FA..110FF; UNKNOWN
            0x11100,  // 11100..11134; CHAKMA
            0x11135,  // 11135       ; UNKNOWN
            0x11136,  // 11136..11147; CHAKMA
            0x11148,  // 11148..1114F; UNKNOWN
            0x11150,  // 11150..11176; MAHAJANI
            0x11177,  // 11177..1117F; UNKNOWN
            0x11180,  // 11180..111DF; SHARADA
            0x111E0,  // 111E0       ; UNKNOWN
            0x111E1,  // 111E1..111F4; SINHALA
            0x111F5,  // 111F5..111FF; UNKNOWN
            0x11200,  // 11200..11211; KHOJKI
            0x11212,  // 11212       ; UNKNOWN
            0x11213,  // 11213..1123E; KHOJKI
            0x1123F,  // 1123F..1127F; UNKNOWN
            0x11280,  // 11280..11286; MULTANI
            0x11287,  // 11287       ; UNKNOWN
            0x11288,  // 11288       ; MULTANI
            0x11289,  // 11289       ; UNKNOWN
            0x1128A,  // 1128A..1128D; MULTANI
            0x1128E,  // 1128E       ; UNKNOWN
            0x1128F,  // 1128F..1129D; MULTANI
            0x1129E,  // 1129E       ; UNKNOWN
            0x1129F,  // 1129F..112A9; MULTANI
            0x112AA,  // 112AA..112AF; UNKNOWN
            0x112B0,  // 112B0..112EA; KHUDAWADI
            0x112EB,  // 112EB..112EF; UNKNOWN
            0x112F0,  // 112F0..112F9; KHUDAWADI
            0x112FA,  // 112FA..112FF; UNKNOWN
            0x11300,  // 11300..11303; GRANTHA
            0x11304,  // 11304       ; UNKNOWN
            0x11305,  // 11305..1130C; GRANTHA
            0x1130D,  // 1130D..1130E; UNKNOWN
            0x1130F,  // 1130F..11310; GRANTHA
            0x11311,  // 11311..11312; UNKNOWN
            0x11313,  // 11313..11328; GRANTHA
            0x11329,  // 11329       ; UNKNOWN
            0x1132A,  // 1132A..11330; GRANTHA
            0x11331,  // 11331       ; UNKNOWN
            0x11332,  // 11332..11333; GRANTHA
            0x11334,  // 11334       ; UNKNOWN
            0x11335,  // 11335..11339; GRANTHA
            0x1133A,  // 1133A       ; UNKNOWN
            0x1133B,  // 1133B       ; INHERITED
            0x1133C,  // 1133C..11344; GRANTHA
            0x11345,  // 11345..11346; UNKNOWN
            0x11347,  // 11347..11348; GRANTHA
            0x11349,  // 11349..1134A; UNKNOWN
            0x1134B,  // 1134B..1134D; GRANTHA
            0x1134E,  // 1134E..1134F; UNKNOWN
            0x11350,  // 11350       ; GRANTHA
            0x11351,  // 11351..11356; UNKNOWN
            0x11357,  // 11357       ; GRANTHA
            0x11358,  // 11358..1135C; UNKNOWN
            0x1135D,  // 1135D..11363; GRANTHA
            0x11364,  // 11364..11365; UNKNOWN
            0x11366,  // 11366..1136C; GRANTHA
            0x1136D,  // 1136D..1136F; UNKNOWN
            0x11370,  // 11370..11374; GRANTHA
            0x11375,  // 11375..113FF; UNKNOWN
            0x11400,  // 11400..1145B; NEWA
            0x1145C,  // 1145C       ; UNKNOWN
            0x1145D,  // 1145D..11461; NEWA
            0x11462,  // 11462..1147F; UNKNOWN
            0x11480,  // 11480..114C7; TIRHUTA
            0x114C8,  // 114C8..114CF; UNKNOWN
            0x114D0,  // 114D0..114D9; TIRHUTA
            0x114DA,  // 114DA..1157F; UNKNOWN
            0x11580,  // 11580..115B5; SIDDHAM
            0x115B6,  // 115B6..115B7; UNKNOWN
            0x115B8,  // 115B8..115DD; SIDDHAM
            0x115DE,  // 115DE..115FF; UNKNOWN
            0x11600,  // 11600..11644; MODI
            0x11645,  // 11645..1164F; UNKNOWN
            0x11650,  // 11650..11659; MODI
            0x1165A,  // 1165A..1165F; UNKNOWN
            0x11660,  // 11660..1166C; MONGOLIAN
            0x1166D,  // 1166D..1167F; UNKNOWN
            0x11680,  // 11680..116B8; TAKRI
            0x116B9,  // 116B9..116BF; UNKNOWN
            0x116C0,  // 116C0..116C9; TAKRI
            0x116CA,  // 116CA..116FF; UNKNOWN
            0x11700,  // 11700..1171A; AHOM
            0x1171B,  // 1171B..1171C; UNKNOWN
            0x1171D,  // 1171D..1172B; AHOM
            0x1172C,  // 1172C..1172F; UNKNOWN
            0x11730,  // 11730..1173F; AHOM
            0x11740,  // 11740..117FF; UNKNOWN
            0x11800,  // 11800..1183B; DOGRA
            0x1183C,  // 1183C..1189F; UNKNOWN
            0x118A0,  // 118A0..118F2; WARANG_CITI
            0x118F3,  // 118F3..118FE; UNKNOWN
            0x118FF,  // 118FF       ; WARANG_CITI
            0x11900,  // 11900..11906; DIVES_AKURU
            0x11907,  // 11907..11908; UNKNOWN
            0x11909,  // 11909       ; DIVES_AKURU
            0x1190A,  // 1190A..1190B; UNKNOWN
            0x1190C,  // 1190C..11913; DIVES_AKURU
            0x11914,  // 11914       ; UNKNOWN
            0x11915,  // 11915..11916; DIVES_AKURU
            0x11917,  // 11917       ; UNKNOWN
            0x11918,  // 11918..11935; DIVES_AKURU
            0x11936,  // 11936       ; UNKNOWN
            0x11937,  // 11937..11938; DIVES_AKURU
            0x11939,  // 11939..1193A; UNKNOWN
            0x1193B,  // 1193B..11946; DIVES_AKURU
            0x11947,  // 11947..1194F; UNKNOWN
            0x11950,  // 11950..11959; DIVES_AKURU
            0x1195A,  // 1195A..1199F; UNKNOWN
            0x119A0,  // 119A0..119A7; NANDINAGARI
            0x119A8,  // 119A8..119A9; UNKNOWN
            0x119AA,  // 119AA..119D7; NANDINAGARI
            0x119D8,  // 119D8..119D9; UNKNOWN
            0x119DA,  // 119DA..119E4; NANDINAGARI
            0x119E5,  // 119E5..119FF; UNKNOWN
            0x11A00,  // 11A00..11A47; ZANABAZAR_SQUARE
            0x11A48,  // 11A48..11A4F; UNKNOWN
            0x11A50,  // 11A50..11AA2; SOYOMBO
            0x11AA3,  // 11AA3..11ABF; UNKNOWN
            0x11AC0,  // 11AC0..11AF8; PAU_CIN_HAU
            0x11AF9,  // 11AF9..11BFF; UNKNOWN
            0x11C00,  // 11C00..11C08; BHAIKSUKI
            0x11C09,  // 11C09       ; UNKNOWN
            0x11C0A,  // 11C0A..11C36; BHAIKSUKI
            0x11C37,  // 11C37       ; UNKNOWN
            0x11C38,  // 11C38..11C45; BHAIKSUKI
            0x11C46,  // 11C46..11C4F; UNKNOWN
            0x11C50,  // 11C50..11C6C; BHAIKSUKI
            0x11C6D,  // 11C6D..11C6F; UNKNOWN
            0x11C70,  // 11C70..11C8F; MARCHEN
            0x11C90,  // 11C90..11C91; UNKNOWN
            0x11C92,  // 11C92..11CA7; MARCHEN
            0x11CA8,  // 11CA8       ; UNKNOWN
            0x11CA9,  // 11CA9..11CB6; MARCHEN
            0x11CB7,  // 11CB7..11CFF; UNKNOWN
            0x11D00,  // 11D00..11D06; MASARAM_GONDI
            0x11D07,  // 11D07       ; UNKNOWN
            0x11D08,  // 11D08..11D09; MASARAM_GONDI
            0x11D0A,  // 11D0A       ; UNKNOWN
            0x11D0B,  // 11D0B..11D36; MASARAM_GONDI
            0x11D37,  // 11D37..11D39; UNKNOWN
            0x11D3A,  // 11D3A       ; MASARAM_GONDI
            0x11D3B,  // 11D3B       ; UNKNOWN
            0x11D3C,  // 11D3C..11D3D; MASARAM_GONDI
            0x11D3E,  // 11D3E       ; UNKNOWN
            0x11D3F,  // 11D3F..11D47; MASARAM_GONDI
            0x11D48,  // 11D48..11D4F; UNKNOWN
            0x11D50,  // 11D50..11D59; MASARAM_GONDI
            0x11D5A,  // 11D5A..11D5F; UNKNOWN
            0x11D60,  // 11D60..11D65; GUNJALA_GONDI
            0x11D66,  // 11D66       ; UNKNOWN
            0x11D67,  // 11D67..11D68; GUNJALA_GONDI
            0x11D69,  // 11D69       ; UNKNOWN
            0x11D6A,  // 11D6A..11D8E; GUNJALA_GONDI
            0x11D8F,  // 11D8F       ; UNKNOWN
            0x11D90,  // 11D90..11D91; GUNJALA_GONDI
            0x11D92,  // 11D92       ; UNKNOWN
            0x11D93,  // 11D93..11D98; GUNJALA_GONDI
            0x11D99,  // 11D99..11D9F; UNKNOWN
            0x11DA0,  // 11DA0..11DA9; GUNJALA_GONDI
            0x11DAA,  // 11DAA..11EDF; UNKNOWN
            0x11EE0,  // 11EE0..11EF8; MAKASAR
            0x11EF9,  // 11EF9..11FAF; UNKNOWN
            0x11FB0,  // 11FB0       ; LISU
            0x11FB1,  // 11FB1..11FBF; UNKNOWN
            0x11FC0,  // 11FC0..11FF1; TAMIL
            0x11FF2,  // 11FF2..11FFE; UNKNOWN
            0x11FFF,  // 11FFF       ; TAMIL
            0x12000,  // 12000..12399; CUNEIFORM
            0x1239A,  // 1239A..123FF; UNKNOWN
            0x12400,  // 12400..1246E; CUNEIFORM
            0x1246F,  // 1246F       ; UNKNOWN
            0x12470,  // 12470..12474; CUNEIFORM
            0x12475,  // 12475..1247F; UNKNOWN
            0x12480,  // 12480..12543; CUNEIFORM
            0x12544,  // 12544..12FFF; UNKNOWN
            0x13000,  // 13000..1342E; EGYPTIAN_HIEROGLYPHS
            0x1342F,  // 1342F       ; UNKNOWN
            0x13430,  // 13430..13438; EGYPTIAN_HIEROGLYPHS
            0x13439,  // 13439..143FF; UNKNOWN
            0x14400,  // 14400..14646; ANATOLIAN_HIEROGLYPHS
            0x14647,  // 14647..167FF; UNKNOWN
            0x16800,  // 16800..16A38; BAMUM
            0x16A39,  // 16A39..16A3F; UNKNOWN
            0x16A40,  // 16A40..16A5E; MRO
            0x16A5F,  // 16A5F       ; UNKNOWN
            0x16A60,  // 16A60..16A69; MRO
            0x16A6A,  // 16A6A..16A6D; UNKNOWN
            0x16A6E,  // 16A6E..16A6F; MRO
            0x16A70,  // 16A70..16ACF; UNKNOWN
            0x16AD0,  // 16AD0..16AED; BASSA_VAH
            0x16AEE,  // 16AEE..16AEF; UNKNOWN
            0x16AF0,  // 16AF0..16AF5; BASSA_VAH
            0x16AF6,  // 16AF6..16AFF; UNKNOWN
            0x16B00,  // 16B00..16B45; PAHAWH_HMONG
            0x16B46,  // 16B46..16B4F; UNKNOWN
            0x16B50,  // 16B50..16B59; PAHAWH_HMONG
            0x16B5A,  // 16B5A       ; UNKNOWN
            0x16B5B,  // 16B5B..16B61; PAHAWH_HMONG
            0x16B62,  // 16B62       ; UNKNOWN
            0x16B63,  // 16B63..16B77; PAHAWH_HMONG
            0x16B78,  // 16B78..16B7C; UNKNOWN
            0x16B7D,  // 16B7D..16B8F; PAHAWH_HMONG
            0x16B90,  // 16B90..16E3F; UNKNOWN
            0x16E40,  // 16E40..16E9A; MEDEFAIDRIN
            0x16E9B,  // 16E9B..16EFF; UNKNOWN
            0x16F00,  // 16F00..16F4A; MIAO
            0x16F4B,  // 16F4B..16F4E; UNKNOWN
            0x16F4F,  // 16F4F..16F87; MIAO
            0x16F88,  // 16F88..16F8E; UNKNOWN
            0x16F8F,  // 16F8F..16F9F; MIAO
            0x16FA0,  // 16FA0..16FDF; UNKNOWN
            0x16FE0,  // 16FE0       ; TANGUT
            0x16FE1,  // 16FE1       ; NUSHU
            0x16FE2,  // 16FE2..16FE3; COMMON
            0x16FE4,  // 16FE4       ; KHITAN_SMALL_SCRIPT
            0x16FE5,  // 16FE5..16FEF; UNKNOWN
            0x16FF0,  // 16FF0..16FF1; HAN
            0x16FF2,  // 16FF2..16FFF; UNKNOWN
            0x17000,  // 17000..187F7; TANGUT
            0x187F8,  // 187F8..187FF; UNKNOWN
            0x18800,  // 18800..18AFF; TANGUT
            0x18B00,  // 18B00..18CD5; KHITAN_SMALL_SCRIPT
            0x18CD6,  // 18CD6..18CFF; UNKNOWN
            0x18D00,  // 18D00..18D08; TANGUT
            0x18D09,  // 18D09..1AFFF; UNKNOWN
            0x1B000,  // 1B000       ; KATAKANA
            0x1B001,  // 1B001..1B11E; HIRAGANA
            0x1B11F,  // 1B11F..1B14F; UNKNOWN
            0x1B150,  // 1B150..1B152; HIRAGANA
            0x1B153,  // 1B153..1B163; UNKNOWN
            0x1B164,  // 1B164..1B167; KATAKANA
            0x1B168,  // 1B168..1B16F; UNKNOWN
            0x1B170,  // 1B170..1B2FB; NUSHU
            0x1B2FC,  // 1B2FC..1BBFF; UNKNOWN
            0x1BC00,  // 1BC00..1BC6A; DUPLOYAN
            0x1BC6B,  // 1BC6B..1BC6F; UNKNOWN
            0x1BC70,  // 1BC70..1BC7C; DUPLOYAN
            0x1BC7D,  // 1BC7D..1BC7F; UNKNOWN
            0x1BC80,  // 1BC80..1BC88; DUPLOYAN
            0x1BC89,  // 1BC89..1BC8F; UNKNOWN
            0x1BC90,  // 1BC90..1BC99; DUPLOYAN
            0x1BC9A,  // 1BC9A..1BC9B; UNKNOWN
            0x1BC9C,  // 1BC9C..1BC9F; DUPLOYAN
            0x1BCA0,  // 1BCA0..1BCA3; COMMON
            0x1BCA4,  // 1BCA4..1CFFF; UNKNOWN
            0x1D000,  // 1D000..1D0F5; COMMON
            0x1D0F6,  // 1D0F6..1D0FF; UNKNOWN
            0x1D100,  // 1D100..1D126; COMMON
            0x1D127,  // 1D127..1D128; UNKNOWN
            0x1D129,  // 1D129..1D166; COMMON
            0x1D167,  // 1D167..1D169; INHERITED
            0x1D16A,  // 1D16A..1D17A; COMMON
            0x1D17B,  // 1D17B..1D182; INHERITED
            0x1D183,  // 1D183..1D184; COMMON
            0x1D185,  // 1D185..1D18B; INHERITED
            0x1D18C,  // 1D18C..1D1A9; COMMON
            0x1D1AA,  // 1D1AA..1D1AD; INHERITED
            0x1D1AE,  // 1D1AE..1D1E8; COMMON
            0x1D1E9,  // 1D1E9..1D1FF; UNKNOWN
            0x1D200,  // 1D200..1D245; GREEK
            0x1D246,  // 1D246..1D2DF; UNKNOWN
            0x1D2E0,  // 1D2E0..1D2F3; COMMON
            0x1D2F4,  // 1D2F4..1D2FF; UNKNOWN
            0x1D300,  // 1D300..1D356; COMMON
            0x1D357,  // 1D357..1D35F; UNKNOWN
            0x1D360,  // 1D360..1D378; COMMON
            0x1D379,  // 1D379..1D3FF; UNKNOWN
            0x1D400,  // 1D400..1D454; COMMON
            0x1D455,  // 1D455       ; UNKNOWN
            0x1D456,  // 1D456..1D49C; COMMON
            0x1D49D,  // 1D49D       ; UNKNOWN
            0x1D49E,  // 1D49E..1D49F; COMMON
            0x1D4A0,  // 1D4A0..1D4A1; UNKNOWN
            0x1D4A2,  // 1D4A2       ; COMMON
            0x1D4A3,  // 1D4A3..1D4A4; UNKNOWN
            0x1D4A5,  // 1D4A5..1D4A6; COMMON
            0x1D4A7,  // 1D4A7..1D4A8; UNKNOWN
            0x1D4A9,  // 1D4A9..1D4AC; COMMON
            0x1D4AD,  // 1D4AD       ; UNKNOWN
            0x1D4AE,  // 1D4AE..1D4B9; COMMON
            0x1D4BA,  // 1D4BA       ; UNKNOWN
            0x1D4BB,  // 1D4BB       ; COMMON
            0x1D4BC,  // 1D4BC       ; UNKNOWN
            0x1D4BD,  // 1D4BD..1D4C3; COMMON
            0x1D4C4,  // 1D4C4       ; UNKNOWN
            0x1D4C5,  // 1D4C5..1D505; COMMON
            0x1D506,  // 1D506       ; UNKNOWN
            0x1D507,  // 1D507..1D50A; COMMON
            0x1D50B,  // 1D50B..1D50C; UNKNOWN
            0x1D50D,  // 1D50D..1D514; COMMON
            0x1D515,  // 1D515       ; UNKNOWN
            0x1D516,  // 1D516..1D51C; COMMON
            0x1D51D,  // 1D51D       ; UNKNOWN
            0x1D51E,  // 1D51E..1D539; COMMON
            0x1D53A,  // 1D53A       ; UNKNOWN
            0x1D53B,  // 1D53B..1D53E; COMMON
            0x1D53F,  // 1D53F       ; UNKNOWN
            0x1D540,  // 1D540..1D544; COMMON
            0x1D545,  // 1D545       ; UNKNOWN
            0x1D546,  // 1D546       ; COMMON
            0x1D547,  // 1D547..1D549; UNKNOWN
            0x1D54A,  // 1D54A..1D550; COMMON
            0x1D551,  // 1D551       ; UNKNOWN
            0x1D552,  // 1D552..1D6A5; COMMON
            0x1D6A6,  // 1D6A6..1D6A7; UNKNOWN
            0x1D6A8,  // 1D6A8..1D7CB; COMMON
            0x1D7CC,  // 1D7CC..1D7CD; UNKNOWN
            0x1D7CE,  // 1D7CE..1D7FF; COMMON
            0x1D800,  // 1D800..1DA8B; SIGNWRITING
            0x1DA8C,  // 1DA8C..1DA9A; UNKNOWN
            0x1DA9B,  // 1DA9B..1DA9F; SIGNWRITING
            0x1DAA0,  // 1DAA0       ; UNKNOWN
            0x1DAA1,  // 1DAA1..1DAAF; SIGNWRITING
            0x1DAB0,  // 1DAB0..1DFFF; UNKNOWN
            0x1E000,  // 1E000..1E006; GLAGOLITIC
            0x1E007,  // 1E007       ; UNKNOWN
            0x1E008,  // 1E008..1E018; GLAGOLITIC
            0x1E019,  // 1E019..1E01A; UNKNOWN
            0x1E01B,  // 1E01B..1E021; GLAGOLITIC
            0x1E022,  // 1E022       ; UNKNOWN
            0x1E023,  // 1E023..1E024; GLAGOLITIC
            0x1E025,  // 1E025       ; UNKNOWN
            0x1E026,  // 1E026..1E02A; GLAGOLITIC
            0x1E02B,  // 1E02B..1E0FF; UNKNOWN
            0x1E100,  // 1E100..1E12C; NYIAKENG_PUACHUE_HMONG
            0x1E12D,  // 1E12D..1E12F; UNKNOWN
            0x1E130,  // 1E130..1E13D; NYIAKENG_PUACHUE_HMONG
            0x1E13E,  // 1E13E..1E13F; UNKNOWN
            0x1E140,  // 1E140..1E149; NYIAKENG_PUACHUE_HMONG
            0x1E14A,  // 1E14A..1E14D; UNKNOWN
            0x1E14E,  // 1E14E..1E14F; NYIAKENG_PUACHUE_HMONG
            0x1E150,  // 1E150..1E2BF; UNKNOWN
            0x1E2C0,  // 1E2C0..1E2F9; WANCHO
            0x1E2FA,  // 1E2FA..1E2FE; UNKNOWN
            0x1E2FF,  // 1E2FF       ; WANCHO
            0x1E300,  // 1E300..1E7FF; UNKNOWN
            0x1E800,  // 1E800..1E8C4; MENDE_KIKAKUI
            0x1E8C5,  // 1E8C5..1E8C6; UNKNOWN
            0x1E8C7,  // 1E8C7..1E8D6; MENDE_KIKAKUI
            0x1E8D7,  // 1E8D7..1E8FF; UNKNOWN
            0x1E900,  // 1E900..1E94B; ADLAM
            0x1E94C,  // 1E94C..1E94F; UNKNOWN
            0x1E950,  // 1E950..1E959; ADLAM
            0x1E95A,  // 1E95A..1E95D; UNKNOWN
            0x1E95E,  // 1E95E..1E95F; ADLAM
            0x1E960,  // 1E960..1EC70; UNKNOWN
            0x1EC71,  // 1EC71..1ECB4; COMMON
            0x1ECB5,  // 1ECB5..1ED00; UNKNOWN
            0x1ED01,  // 1ED01..1ED3D; COMMON
            0x1ED3E,  // 1ED3E..1EDFF; UNKNOWN
            0x1EE00,  // 1EE00..1EE03; ARABIC
            0x1EE04,  // 1EE04       ; UNKNOWN
            0x1EE05,  // 1EE05..1EE1F; ARABIC
            0x1EE20,  // 1EE20       ; UNKNOWN
            0x1EE21,  // 1EE21..1EE22; ARABIC
            0x1EE23,  // 1EE23       ; UNKNOWN
            0x1EE24,  // 1EE24       ; ARABIC
            0x1EE25,  // 1EE25..1EE26; UNKNOWN
            0x1EE27,  // 1EE27       ; ARABIC
            0x1EE28,  // 1EE28       ; UNKNOWN
            0x1EE29,  // 1EE29..1EE32; ARABIC
            0x1EE33,  // 1EE33       ; UNKNOWN
            0x1EE34,  // 1EE34..1EE37; ARABIC
            0x1EE38,  // 1EE38       ; UNKNOWN
            0x1EE39,  // 1EE39       ; ARABIC
            0x1EE3A,  // 1EE3A       ; UNKNOWN
            0x1EE3B,  // 1EE3B       ; ARABIC
            0x1EE3C,  // 1EE3C..1EE41; UNKNOWN
            0x1EE42,  // 1EE42       ; ARABIC
            0x1EE43,  // 1EE43..1EE46; UNKNOWN
            0x1EE47,  // 1EE47       ; ARABIC
            0x1EE48,  // 1EE48       ; UNKNOWN
            0x1EE49,  // 1EE49       ; ARABIC
            0x1EE4A,  // 1EE4A       ; UNKNOWN
            0x1EE4B,  // 1EE4B       ; ARABIC
            0x1EE4C,  // 1EE4C       ; UNKNOWN
            0x1EE4D,  // 1EE4D..1EE4F; ARABIC
            0x1EE50,  // 1EE50       ; UNKNOWN
            0x1EE51,  // 1EE51..1EE52; ARABIC
            0x1EE53,  // 1EE53       ; UNKNOWN
            0x1EE54,  // 1EE54       ; ARABIC
            0x1EE55,  // 1EE55..1EE56; UNKNOWN
            0x1EE57,  // 1EE57       ; ARABIC
            0x1EE58,  // 1EE58       ; UNKNOWN
            0x1EE59,  // 1EE59       ; ARABIC
            0x1EE5A,  // 1EE5A       ; UNKNOWN
            0x1EE5B,  // 1EE5B       ; ARABIC
            0x1EE5C,  // 1EE5C       ; UNKNOWN
            0x1EE5D,  // 1EE5D       ; ARABIC
            0x1EE5E,  // 1EE5E       ; UNKNOWN
            0x1EE5F,  // 1EE5F       ; ARABIC
            0x1EE60,  // 1EE60       ; UNKNOWN
            0x1EE61,  // 1EE61..1EE62; ARABIC
            0x1EE63,  // 1EE63       ; UNKNOWN
            0x1EE64,  // 1EE64       ; ARABIC
            0x1EE65,  // 1EE65..1EE66; UNKNOWN
            0x1EE67,  // 1EE67..1EE6A; ARABIC
            0x1EE6B,  // 1EE6B       ; UNKNOWN
            0x1EE6C,  // 1EE6C..1EE72; ARABIC
            0x1EE73,  // 1EE73       ; UNKNOWN
            0x1EE74,  // 1EE74..1EE77; ARABIC
            0x1EE78,  // 1EE78       ; UNKNOWN
            0x1EE79,  // 1EE79..1EE7C; ARABIC
            0x1EE7D,  // 1EE7D       ; UNKNOWN
            0x1EE7E,  // 1EE7E       ; ARABIC
            0x1EE7F,  // 1EE7F       ; UNKNOWN
            0x1EE80,  // 1EE80..1EE89; ARABIC
            0x1EE8A,  // 1EE8A       ; UNKNOWN
            0x1EE8B,  // 1EE8B..1EE9B; ARABIC
            0x1EE9C,  // 1EE9C..1EEA0; UNKNOWN
            0x1EEA1,  // 1EEA1..1EEA3; ARABIC
            0x1EEA4,  // 1EEA4       ; UNKNOWN
            0x1EEA5,  // 1EEA5..1EEA9; ARABIC
            0x1EEAA,  // 1EEAA       ; UNKNOWN
            0x1EEAB,  // 1EEAB..1EEBB; ARABIC
            0x1EEBC,  // 1EEBC..1EEEF; UNKNOWN
            0x1EEF0,  // 1EEF0..1EEF1; ARABIC
            0x1EEF2,  // 1EEF2..1EFFF; UNKNOWN
            0x1F000,  // 1F000..1F02B; COMMON
            0x1F02C,  // 1F02C..1F02F; UNKNOWN
            0x1F030,  // 1F030..1F093; COMMON
            0x1F094,  // 1F094..1F09F; UNKNOWN
            0x1F0A0,  // 1F0A0..1F0AE; COMMON
            0x1F0AF,  // 1F0AF..1F0B0; UNKNOWN
            0x1F0B1,  // 1F0B1..1F0BF; COMMON
            0x1F0C0,  // 1F0C0       ; UNKNOWN
            0x1F0C1,  // 1F0C1..1F0CF; COMMON
            0x1F0D0,  // 1F0D0       ; UNKNOWN
            0x1F0D1,  // 1F0D1..1F0F5; COMMON
            0x1F0F6,  // 1F0F6..1F0FF; UNKNOWN
            0x1F100,  // 1F100..1F1AD; COMMON
            0x1F1AE,  // 1F1AE..1F1E5; UNKNOWN
            0x1F1E6,  // 1F1E6..1F1FF; COMMON
            0x1F200,  // 1F200       ; HIRAGANA
            0x1F201,  // 1F201..1F202; COMMON
            0x1F203,  // 1F203..1F20F; UNKNOWN
            0x1F210,  // 1F210..1F23B; COMMON
            0x1F23C,  // 1F23C..1F23F; UNKNOWN
            0x1F240,  // 1F240..1F248; COMMON
            0x1F249,  // 1F249..1F24F; UNKNOWN
            0x1F250,  // 1F250..1F251; COMMON
            0x1F252,  // 1F252..1F25F; UNKNOWN
            0x1F260,  // 1F260..1F265; COMMON
            0x1F266,  // 1F266..1F2FF; UNKNOWN
            0x1F300,  // 1F300..1F6D7; COMMON
            0x1F6D8,  // 1F6D8..1F6DF; UNKNOWN
            0x1F6E0,  // 1F6E0..1F6EC; COMMON
            0x1F6ED,  // 1F6ED..1F6EF; UNKNOWN
            0x1F6F0,  // 1F6F0..1F6FC; COMMON
            0x1F6FD,  // 1F6FD..1F6FF; UNKNOWN
            0x1F700,  // 1F700..1F773; COMMON
            0x1F774,  // 1F774..1F77F; UNKNOWN
            0x1F780,  // 1F780..1F7D8; COMMON
            0x1F7D9,  // 1F7D9..1F7DF; UNKNOWN
            0x1F7E0,  // 1F7E0..1F7EB; COMMON
            0x1F7EC,  // 1F7EC..1F7FF; UNKNOWN
            0x1F800,  // 1F800..1F80B; COMMON
            0x1F80C,  // 1F80C..1F80F; UNKNOWN
            0x1F810,  // 1F810..1F847; COMMON
            0x1F848,  // 1F848..1F84F; UNKNOWN
            0x1F850,  // 1F850..1F859; COMMON
            0x1F85A,  // 1F85A..1F85F; UNKNOWN
            0x1F860,  // 1F860..1F887; COMMON
            0x1F888,  // 1F888..1F88F; UNKNOWN
            0x1F890,  // 1F890..1F8AD; COMMON
            0x1F8AE,  // 1F8AE..1F8AF; UNKNOWN
            0x1F8B0,  // 1F8B0..1F8B1; COMMON
            0x1F8B2,  // 1F8B2..1F8FF; UNKNOWN
            0x1F900,  // 1F900..1F978; COMMON
            0x1F979,  // 1F979       ; UNKNOWN
            0x1F97A,  // 1F97A..1F9CB; COMMON
            0x1F9CC,  // 1F9CC       ; UNKNOWN
            0x1F9CD,  // 1F9CD..1FA53; COMMON
            0x1FA54,  // 1FA54..1FA5F; UNKNOWN
            0x1FA60,  // 1FA60..1FA6D; COMMON
            0x1FA6E,  // 1FA6E..1FA6F; UNKNOWN
            0x1FA70,  // 1FA70..1FA74; COMMON
            0x1FA75,  // 1FA75..1FA77; UNKNOWN
            0x1FA78,  // 1FA78..1FA7A; COMMON
            0x1FA7B,  // 1FA7B..1FA7F; UNKNOWN
            0x1FA80,  // 1FA80..1FA86; COMMON
            0x1FA87,  // 1FA87..1FA8F; UNKNOWN
            0x1FA90,  // 1FA90..1FAA8; COMMON
            0x1FAA9,  // 1FAA9..1FAAF; UNKNOWN
            0x1FAB0,  // 1FAB0..1FAB6; COMMON
            0x1FAB7,  // 1FAB7..1FABF; UNKNOWN
            0x1FAC0,  // 1FAC0..1FAC2; COMMON
            0x1FAC3,  // 1FAC3..1FACF; UNKNOWN
            0x1FAD0,  // 1FAD0..1FAD6; COMMON
            0x1FAD7,  // 1FAD7..1FAFF; UNKNOWN
            0x1FB00,  // 1FB00..1FB92; COMMON
            0x1FB93,  // 1FB93       ; UNKNOWN
            0x1FB94,  // 1FB94..1FBCA; COMMON
            0x1FBCB,  // 1FBCB..1FBEF; UNKNOWN
            0x1FBF0,  // 1FBF0..1FBF9; COMMON
            0x1FBFA,  // 1FBFA..1FFFF; UNKNOWN
            0x20000,  // 20000..2A6DD; HAN
            0x2A6DE,  // 2A6DE..2A6FF; UNKNOWN
            0x2A700,  // 2A700..2B734; HAN
            0x2B735,  // 2B735..2B73F; UNKNOWN
            0x2B740,  // 2B740..2B81D; HAN
            0x2B81E,  // 2B81E..2B81F; UNKNOWN
            0x2B820,  // 2B820..2CEA1; HAN
            0x2CEA2,  // 2CEA2..2CEAF; UNKNOWN
            0x2CEB0,  // 2CEB0..2EBE0; HAN
            0x2EBE1,  // 2EBE1..2F7FF; UNKNOWN
            0x2F800,  // 2F800..2FA1D; HAN
            0x2FA1E,  // 2FA1E..2FFFF; UNKNOWN
            0x30000,  // 30000..3134A; HAN
            0x3134B,  // 3134B..E0000; UNKNOWN
            0xE0001,  // E0001       ; COMMON
            0xE0002,  // E0002..E001F; UNKNOWN
            0xE0020,  // E0020..E007F; COMMON
            0xE0080,  // E0080..E00FF; UNKNOWN
            0xE0100,  // E0100..E01EF; INHERITED
            0xE01F0,  // E01F0..10FFFF; UNKNOWN
        };

        private static final UnicodeScript[] scripts = {
            COMMON,                   // 0000..0040
            LATIN,                    // 0041..005A
            COMMON,                   // 005B..0060
            LATIN,                    // 0061..007A
            COMMON,                   // 007B..00A9
            LATIN,                    // 00AA
            COMMON,                   // 00AB..00B9
            LATIN,                    // 00BA
            COMMON,                   // 00BB..00BF
            LATIN,                    // 00C0..00D6
            COMMON,                   // 00D7
            LATIN,                    // 00D8..00F6
            COMMON,                   // 00F7
            LATIN,                    // 00F8..02B8
            COMMON,                   // 02B9..02DF
            LATIN,                    // 02E0..02E4
            COMMON,                   // 02E5..02E9
            BOPOMOFO,                 // 02EA..02EB
            COMMON,                   // 02EC..02FF
            INHERITED,                // 0300..036F
            GREEK,                    // 0370..0373
            COMMON,                   // 0374
            GREEK,                    // 0375..0377
            UNKNOWN,                  // 0378..0379
            GREEK,                    // 037A..037D
            COMMON,                   // 037E
            GREEK,                    // 037F
            UNKNOWN,                  // 0380..0383
            GREEK,                    // 0384
            COMMON,                   // 0385
            GREEK,                    // 0386
            COMMON,                   // 0387
            GREEK,                    // 0388..038A
            UNKNOWN,                  // 038B
            GREEK,                    // 038C
            UNKNOWN,                  // 038D
            GREEK,                    // 038E..03A1
            UNKNOWN,                  // 03A2
            GREEK,                    // 03A3..03E1
            COPTIC,                   // 03E2..03EF
            GREEK,                    // 03F0..03FF
            CYRILLIC,                 // 0400..0484
            INHERITED,                // 0485..0486
            CYRILLIC,                 // 0487..052F
            UNKNOWN,                  // 0530
            ARMENIAN,                 // 0531..0556
            UNKNOWN,                  // 0557..0558
            ARMENIAN,                 // 0559..058A
            UNKNOWN,                  // 058B..058C
            ARMENIAN,                 // 058D..058F
            UNKNOWN,                  // 0590
            HEBREW,                   // 0591..05C7
            UNKNOWN,                  // 05C8..05CF
            HEBREW,                   // 05D0..05EA
            UNKNOWN,                  // 05EB..05EE
            HEBREW,                   // 05EF..05F4
            UNKNOWN,                  // 05F5..05FF
            ARABIC,                   // 0600..0604
            COMMON,                   // 0605
            ARABIC,                   // 0606..060B
            COMMON,                   // 060C
            ARABIC,                   // 060D..061A
            COMMON,                   // 061B
            ARABIC,                   // 061C
            UNKNOWN,                  // 061D
            ARABIC,                   // 061E
            COMMON,                   // 061F
            ARABIC,                   // 0620..063F
            COMMON,                   // 0640
            ARABIC,                   // 0641..064A
            INHERITED,                // 064B..0655
            ARABIC,                   // 0656..066F
            INHERITED,                // 0670
            ARABIC,                   // 0671..06DC
            COMMON,                   // 06DD
            ARABIC,                   // 06DE..06FF
            SYRIAC,                   // 0700..070D
            UNKNOWN,                  // 070E
            SYRIAC,                   // 070F..074A
            UNKNOWN,                  // 074B..074C
            SYRIAC,                   // 074D..074F
            ARABIC,                   // 0750..077F
            THAANA,                   // 0780..07B1
            UNKNOWN,                  // 07B2..07BF
            NKO,                      // 07C0..07FA
            UNKNOWN,                  // 07FB..07FC
            NKO,                      // 07FD..07FF
            SAMARITAN,                // 0800..082D
            UNKNOWN,                  // 082E..082F
            SAMARITAN,                // 0830..083E
            UNKNOWN,                  // 083F
            MANDAIC,                  // 0840..085B
            UNKNOWN,                  // 085C..085D
            MANDAIC,                  // 085E
            UNKNOWN,                  // 085F
            SYRIAC,                   // 0860..086A
            UNKNOWN,                  // 086B..089F
            ARABIC,                   // 08A0..08B4
            UNKNOWN,                  // 08B5
            ARABIC,                   // 08B6..08C7
            UNKNOWN,                  // 08C8..08D2
            ARABIC,                   // 08D3..08E1
            COMMON,                   // 08E2
            ARABIC,                   // 08E3..08FF
            DEVANAGARI,               // 0900..0950
            INHERITED,                // 0951..0954
            DEVANAGARI,               // 0955..0963
            COMMON,                   // 0964..0965
            DEVANAGARI,               // 0966..097F
            BENGALI,                  // 0980..0983
            UNKNOWN,                  // 0984
            BENGALI,                  // 0985..098C
            UNKNOWN,                  // 098D..098E
            BENGALI,                  // 098F..0990
            UNKNOWN,                  // 0991..0992
            BENGALI,                  // 0993..09A8
            UNKNOWN,                  // 09A9
            BENGALI,                  // 09AA..09B0
            UNKNOWN,                  // 09B1
            BENGALI,                  // 09B2
            UNKNOWN,                  // 09B3..09B5
            BENGALI,                  // 09B6..09B9
            UNKNOWN,                  // 09BA..09BB
            BENGALI,                  // 09BC..09C4
            UNKNOWN,                  // 09C5..09C6
            BENGALI,                  // 09C7..09C8
            UNKNOWN,                  // 09C9..09CA
            BENGALI,                  // 09CB..09CE
            UNKNOWN,                  // 09CF..09D6
            BENGALI,                  // 09D7
            UNKNOWN,                  // 09D8..09DB
            BENGALI,                  // 09DC..09DD
            UNKNOWN,                  // 09DE
            BENGALI,                  // 09DF..09E3
            UNKNOWN,                  // 09E4..09E5
            BENGALI,                  // 09E6..09FE
            UNKNOWN,                  // 09FF..0A00
            GURMUKHI,                 // 0A01..0A03
            UNKNOWN,                  // 0A04
            GURMUKHI,                 // 0A05..0A0A
            UNKNOWN,                  // 0A0B..0A0E
            GURMUKHI,                 // 0A0F..0A10
            UNKNOWN,                  // 0A11..0A12
            GURMUKHI,                 // 0A13..0A28
            UNKNOWN,                  // 0A29
            GURMUKHI,                 // 0A2A..0A30
            UNKNOWN,                  // 0A31
            GURMUKHI,                 // 0A32..0A33
            UNKNOWN,                  // 0A34
            GURMUKHI,                 // 0A35..0A36
            UNKNOWN,                  // 0A37
            GURMUKHI,                 // 0A38..0A39
            UNKNOWN,                  // 0A3A..0A3B
            GURMUKHI,                 // 0A3C
            UNKNOWN,                  // 0A3D
            GURMUKHI,                 // 0A3E..0A42
            UNKNOWN,                  // 0A43..0A46
            GURMUKHI,                 // 0A47..0A48
            UNKNOWN,                  // 0A49..0A4A
            GURMUKHI,                 // 0A4B..0A4D
            UNKNOWN,                  // 0A4E..0A50
            GURMUKHI,                 // 0A51
            UNKNOWN,                  // 0A52..0A58
            GURMUKHI,                 // 0A59..0A5C
            UNKNOWN,                  // 0A5D
            GURMUKHI,                 // 0A5E
            UNKNOWN,                  // 0A5F..0A65
            GURMUKHI,                 // 0A66..0A76
            UNKNOWN,                  // 0A77..0A80
            GUJARATI,                 // 0A81..0A83
            UNKNOWN,                  // 0A84
            GUJARATI,                 // 0A85..0A8D
            UNKNOWN,                  // 0A8E
            GUJARATI,                 // 0A8F..0A91
            UNKNOWN,                  // 0A92
            GUJARATI,                 // 0A93..0AA8
            UNKNOWN,                  // 0AA9
            GUJARATI,                 // 0AAA..0AB0
            UNKNOWN,                  // 0AB1
            GUJARATI,                 // 0AB2..0AB3
            UNKNOWN,                  // 0AB4
            GUJARATI,                 // 0AB5..0AB9
            UNKNOWN,                  // 0ABA..0ABB
            GUJARATI,                 // 0ABC..0AC5
            UNKNOWN,                  // 0AC6
            GUJARATI,                 // 0AC7..0AC9
            UNKNOWN,                  // 0ACA
            GUJARATI,                 // 0ACB..0ACD
            UNKNOWN,                  // 0ACE..0ACF
            GUJARATI,                 // 0AD0
            UNKNOWN,                  // 0AD1..0ADF
            GUJARATI,                 // 0AE0..0AE3
            UNKNOWN,                  // 0AE4..0AE5
            GUJARATI,                 // 0AE6..0AF1
            UNKNOWN,                  // 0AF2..0AF8
            GUJARATI,                 // 0AF9..0AFF
            UNKNOWN,                  // 0B00
            ORIYA,                    // 0B01..0B03
            UNKNOWN,                  // 0B04
            ORIYA,                    // 0B05..0B0C
            UNKNOWN,                  // 0B0D..0B0E
            ORIYA,                    // 0B0F..0B10
            UNKNOWN,                  // 0B11..0B12
            ORIYA,                    // 0B13..0B28
            UNKNOWN,                  // 0B29
            ORIYA,                    // 0B2A..0B30
            UNKNOWN,                  // 0B31
            ORIYA,                    // 0B32..0B33
            UNKNOWN,                  // 0B34
            ORIYA,                    // 0B35..0B39
            UNKNOWN,                  // 0B3A..0B3B
            ORIYA,                    // 0B3C..0B44
            UNKNOWN,                  // 0B45..0B46
            ORIYA,                    // 0B47..0B48
            UNKNOWN,                  // 0B49..0B4A
            ORIYA,                    // 0B4B..0B4D
            UNKNOWN,                  // 0B4E..0B54
            ORIYA,                    // 0B55..0B57
            UNKNOWN,                  // 0B58..0B5B
            ORIYA,                    // 0B5C..0B5D
            UNKNOWN,                  // 0B5E
            ORIYA,                    // 0B5F..0B63
            UNKNOWN,                  // 0B64..0B65
            ORIYA,                    // 0B66..0B77
            UNKNOWN,                  // 0B78..0B81
            TAMIL,                    // 0B82..0B83
            UNKNOWN,                  // 0B84
            TAMIL,                    // 0B85..0B8A
            UNKNOWN,                  // 0B8B..0B8D
            TAMIL,                    // 0B8E..0B90
            UNKNOWN,                  // 0B91
            TAMIL,                    // 0B92..0B95
            UNKNOWN,                  // 0B96..0B98
            TAMIL,                    // 0B99..0B9A
            UNKNOWN,                  // 0B9B
            TAMIL,                    // 0B9C
            UNKNOWN,                  // 0B9D
            TAMIL,                    // 0B9E..0B9F
            UNKNOWN,                  // 0BA0..0BA2
            TAMIL,                    // 0BA3..0BA4
            UNKNOWN,                  // 0BA5..0BA7
            TAMIL,                    // 0BA8..0BAA
            UNKNOWN,                  // 0BAB..0BAD
            TAMIL,                    // 0BAE..0BB9
            UNKNOWN,                  // 0BBA..0BBD
            TAMIL,                    // 0BBE..0BC2
            UNKNOWN,                  // 0BC3..0BC5
            TAMIL,                    // 0BC6..0BC8
            UNKNOWN,                  // 0BC9
            TAMIL,                    // 0BCA..0BCD
            UNKNOWN,                  // 0BCE..0BCF
            TAMIL,                    // 0BD0
            UNKNOWN,                  // 0BD1..0BD6
            TAMIL,                    // 0BD7
            UNKNOWN,                  // 0BD8..0BE5
            TAMIL,                    // 0BE6..0BFA
            UNKNOWN,                  // 0BFB..0BFF
            TELUGU,                   // 0C00..0C0C
            UNKNOWN,                  // 0C0D
            TELUGU,                   // 0C0E..0C10
            UNKNOWN,                  // 0C11
            TELUGU,                   // 0C12..0C28
            UNKNOWN,                  // 0C29
            TELUGU,                   // 0C2A..0C39
            UNKNOWN,                  // 0C3A..0C3C
            TELUGU,                   // 0C3D..0C44
            UNKNOWN,                  // 0C45
            TELUGU,                   // 0C46..0C48
            UNKNOWN,                  // 0C49
            TELUGU,                   // 0C4A..0C4D
            UNKNOWN,                  // 0C4E..0C54
            TELUGU,                   // 0C55..0C56
            UNKNOWN,                  // 0C57
            TELUGU,                   // 0C58..0C5A
            UNKNOWN,                  // 0C5B..0C5F
            TELUGU,                   // 0C60..0C63
            UNKNOWN,                  // 0C64..0C65
            TELUGU,                   // 0C66..0C6F
            UNKNOWN,                  // 0C70..0C76
            TELUGU,                   // 0C77..0C7F
            KANNADA,                  // 0C80..0C8C
            UNKNOWN,                  // 0C8D
            KANNADA,                  // 0C8E..0C90
            UNKNOWN,                  // 0C91
            KANNADA,                  // 0C92..0CA8
            UNKNOWN,                  // 0CA9
            KANNADA,                  // 0CAA..0CB3
            UNKNOWN,                  // 0CB4
            KANNADA,                  // 0CB5..0CB9
            UNKNOWN,                  // 0CBA..0CBB
            KANNADA,                  // 0CBC..0CC4
            UNKNOWN,                  // 0CC5
            KANNADA,                  // 0CC6..0CC8
            UNKNOWN,                  // 0CC9
            KANNADA,                  // 0CCA..0CCD
            UNKNOWN,                  // 0CCE..0CD4
            KANNADA,                  // 0CD5..0CD6
            UNKNOWN,                  // 0CD7..0CDD
            KANNADA,                  // 0CDE
            UNKNOWN,                  // 0CDF
            KANNADA,                  // 0CE0..0CE3
            UNKNOWN,                  // 0CE4..0CE5
            KANNADA,                  // 0CE6..0CEF
            UNKNOWN,                  // 0CF0
            KANNADA,                  // 0CF1..0CF2
            UNKNOWN,                  // 0CF3..0CFF
            MALAYALAM,                // 0D00..0D0C
            UNKNOWN,                  // 0D0D
            MALAYALAM,                // 0D0E..0D10
            UNKNOWN,                  // 0D11
            MALAYALAM,                // 0D12..0D44
            UNKNOWN,                  // 0D45
            MALAYALAM,                // 0D46..0D48
            UNKNOWN,                  // 0D49
            MALAYALAM,                // 0D4A..0D4F
            UNKNOWN,                  // 0D50..0D53
            MALAYALAM,                // 0D54..0D63
            UNKNOWN,                  // 0D64..0D65
            MALAYALAM,                // 0D66..0D7F
            UNKNOWN,                  // 0D80
            SINHALA,                  // 0D81..0D83
            UNKNOWN,                  // 0D84
            SINHALA,                  // 0D85..0D96
            UNKNOWN,                  // 0D97..0D99
            SINHALA,                  // 0D9A..0DB1
            UNKNOWN,                  // 0DB2
            SINHALA,                  // 0DB3..0DBB
            UNKNOWN,                  // 0DBC
            SINHALA,                  // 0DBD
            UNKNOWN,                  // 0DBE..0DBF
            SINHALA,                  // 0DC0..0DC6
            UNKNOWN,                  // 0DC7..0DC9
            SINHALA,                  // 0DCA
            UNKNOWN,                  // 0DCB..0DCE
            SINHALA,                  // 0DCF..0DD4
            UNKNOWN,                  // 0DD5
            SINHALA,                  // 0DD6
            UNKNOWN,                  // 0DD7
            SINHALA,                  // 0DD8..0DDF
            UNKNOWN,                  // 0DE0..0DE5
            SINHALA,                  // 0DE6..0DEF
            UNKNOWN,                  // 0DF0..0DF1
            SINHALA,                  // 0DF2..0DF4
            UNKNOWN,                  // 0DF5..0E00
            THAI,                     // 0E01..0E3A
            UNKNOWN,                  // 0E3B..0E3E
            COMMON,                   // 0E3F
            THAI,                     // 0E40..0E5B
            UNKNOWN,                  // 0E5C..0E80
            LAO,                      // 0E81..0E82
            UNKNOWN,                  // 0E83
            LAO,                      // 0E84
            UNKNOWN,                  // 0E85
            LAO,                      // 0E86..0E8A
            UNKNOWN,                  // 0E8B
            LAO,                      // 0E8C..0EA3
            UNKNOWN,                  // 0EA4
            LAO,                      // 0EA5
            UNKNOWN,                  // 0EA6
            LAO,                      // 0EA7..0EBD
            UNKNOWN,                  // 0EBE..0EBF
            LAO,                      // 0EC0..0EC4
            UNKNOWN,                  // 0EC5
            LAO,                      // 0EC6
            UNKNOWN,                  // 0EC7
            LAO,                      // 0EC8..0ECD
            UNKNOWN,                  // 0ECE..0ECF
            LAO,                      // 0ED0..0ED9
            UNKNOWN,                  // 0EDA..0EDB
            LAO,                      // 0EDC..0EDF
            UNKNOWN,                  // 0EE0..0EFF
            TIBETAN,                  // 0F00..0F47
            UNKNOWN,                  // 0F48
            TIBETAN,                  // 0F49..0F6C
            UNKNOWN,                  // 0F6D..0F70
            TIBETAN,                  // 0F71..0F97
            UNKNOWN,                  // 0F98
            TIBETAN,                  // 0F99..0FBC
            UNKNOWN,                  // 0FBD
            TIBETAN,                  // 0FBE..0FCC
            UNKNOWN,                  // 0FCD
            TIBETAN,                  // 0FCE..0FD4
            COMMON,                   // 0FD5..0FD8
            TIBETAN,                  // 0FD9..0FDA
            UNKNOWN,                  // 0FDB..0FFF
            MYANMAR,                  // 1000..109F
            GEORGIAN,                 // 10A0..10C5
            UNKNOWN,                  // 10C6
            GEORGIAN,                 // 10C7
            UNKNOWN,                  // 10C8..10CC
            GEORGIAN,                 // 10CD
            UNKNOWN,                  // 10CE..10CF
            GEORGIAN,                 // 10D0..10FA
            COMMON,                   // 10FB
            GEORGIAN,                 // 10FC..10FF
            HANGUL,                   // 1100..11FF
            ETHIOPIC,                 // 1200..1248
            UNKNOWN,                  // 1249
            ETHIOPIC,                 // 124A..124D
            UNKNOWN,                  // 124E..124F
            ETHIOPIC,                 // 1250..1256
            UNKNOWN,                  // 1257
            ETHIOPIC,                 // 1258
            UNKNOWN,                  // 1259
            ETHIOPIC,                 // 125A..125D
            UNKNOWN,                  // 125E..125F
            ETHIOPIC,                 // 1260..1288
            UNKNOWN,                  // 1289
            ETHIOPIC,                 // 128A..128D
            UNKNOWN,                  // 128E..128F
            ETHIOPIC,                 // 1290..12B0
            UNKNOWN,                  // 12B1
            ETHIOPIC,                 // 12B2..12B5
            UNKNOWN,                  // 12B6..12B7
            ETHIOPIC,                 // 12B8..12BE
            UNKNOWN,                  // 12BF
            ETHIOPIC,                 // 12C0
            UNKNOWN,                  // 12C1
            ETHIOPIC,                 // 12C2..12C5
            UNKNOWN,                  // 12C6..12C7
            ETHIOPIC,                 // 12C8..12D6
            UNKNOWN,                  // 12D7
            ETHIOPIC,                 // 12D8..1310
            UNKNOWN,                  // 1311
            ETHIOPIC,                 // 1312..1315
            UNKNOWN,                  // 1316..1317
            ETHIOPIC,                 // 1318..135A
            UNKNOWN,                  // 135B..135C
            ETHIOPIC,                 // 135D..137C
            UNKNOWN,                  // 137D..137F
            ETHIOPIC,                 // 1380..1399
            UNKNOWN,                  // 139A..139F
            CHEROKEE,                 // 13A0..13F5
            UNKNOWN,                  // 13F6..13F7
            CHEROKEE,                 // 13F8..13FD
            UNKNOWN,                  // 13FE..13FF
            CANADIAN_ABORIGINAL,      // 1400..167F
            OGHAM,                    // 1680..169C
            UNKNOWN,                  // 169D..169F
            RUNIC,                    // 16A0..16EA
            COMMON,                   // 16EB..16ED
            RUNIC,                    // 16EE..16F8
            UNKNOWN,                  // 16F9..16FF
            TAGALOG,                  // 1700..170C
            UNKNOWN,                  // 170D
            TAGALOG,                  // 170E..1714
            UNKNOWN,                  // 1715..171F
            HANUNOO,                  // 1720..1734
            COMMON,                   // 1735..1736
            UNKNOWN,                  // 1737..173F
            BUHID,                    // 1740..1753
            UNKNOWN,                  // 1754..175F
            TAGBANWA,                 // 1760..176C
            UNKNOWN,                  // 176D
            TAGBANWA,                 // 176E..1770
            UNKNOWN,                  // 1771
            TAGBANWA,                 // 1772..1773
            UNKNOWN,                  // 1774..177F
            KHMER,                    // 1780..17DD
            UNKNOWN,                  // 17DE..17DF
            KHMER,                    // 17E0..17E9
            UNKNOWN,                  // 17EA..17EF
            KHMER,                    // 17F0..17F9
            UNKNOWN,                  // 17FA..17FF
            MONGOLIAN,                // 1800..1801
            COMMON,                   // 1802..1803
            MONGOLIAN,                // 1804
            COMMON,                   // 1805
            MONGOLIAN,                // 1806..180E
            UNKNOWN,                  // 180F
            MONGOLIAN,                // 1810..1819
            UNKNOWN,                  // 181A..181F
            MONGOLIAN,                // 1820..1878
            UNKNOWN,                  // 1879..187F
            MONGOLIAN,                // 1880..18AA
            UNKNOWN,                  // 18AB..18AF
            CANADIAN_ABORIGINAL,      // 18B0..18F5
            UNKNOWN,                  // 18F6..18FF
            LIMBU,                    // 1900..191E
            UNKNOWN,                  // 191F
            LIMBU,                    // 1920..192B
            UNKNOWN,                  // 192C..192F
            LIMBU,                    // 1930..193B
            UNKNOWN,                  // 193C..193F
            LIMBU,                    // 1940
            UNKNOWN,                  // 1941..1943
            LIMBU,                    // 1944..194F
            TAI_LE,                   // 1950..196D
            UNKNOWN,                  // 196E..196F
            TAI_LE,                   // 1970..1974
            UNKNOWN,                  // 1975..197F
            NEW_TAI_LUE,              // 1980..19AB
            UNKNOWN,                  // 19AC..19AF
            NEW_TAI_LUE,              // 19B0..19C9
            UNKNOWN,                  // 19CA..19CF
            NEW_TAI_LUE,              // 19D0..19DA
            UNKNOWN,                  // 19DB..19DD
            NEW_TAI_LUE,              // 19DE..19DF
            KHMER,                    // 19E0..19FF
            BUGINESE,                 // 1A00..1A1B
            UNKNOWN,                  // 1A1C..1A1D
            BUGINESE,                 // 1A1E..1A1F
            TAI_THAM,                 // 1A20..1A5E
            UNKNOWN,                  // 1A5F
            TAI_THAM,                 // 1A60..1A7C
            UNKNOWN,                  // 1A7D..1A7E
            TAI_THAM,                 // 1A7F..1A89
            UNKNOWN,                  // 1A8A..1A8F
            TAI_THAM,                 // 1A90..1A99
            UNKNOWN,                  // 1A9A..1A9F
            TAI_THAM,                 // 1AA0..1AAD
            UNKNOWN,                  // 1AAE..1AAF
            INHERITED,                // 1AB0..1AC0
            UNKNOWN,                  // 1AC1..1AFF
            BALINESE,                 // 1B00..1B4B
            UNKNOWN,                  // 1B4C..1B4F
            BALINESE,                 // 1B50..1B7C
            UNKNOWN,                  // 1B7D..1B7F
            SUNDANESE,                // 1B80..1BBF
            BATAK,                    // 1BC0..1BF3
            UNKNOWN,                  // 1BF4..1BFB
            BATAK,                    // 1BFC..1BFF
            LEPCHA,                   // 1C00..1C37
            UNKNOWN,                  // 1C38..1C3A
            LEPCHA,                   // 1C3B..1C49
            UNKNOWN,                  // 1C4A..1C4C
            LEPCHA,                   // 1C4D..1C4F
            OL_CHIKI,                 // 1C50..1C7F
            CYRILLIC,                 // 1C80..1C88
            UNKNOWN,                  // 1C89..1C8F
            GEORGIAN,                 // 1C90..1CBA
            UNKNOWN,                  // 1CBB..1CBC
            GEORGIAN,                 // 1CBD..1CBF
            SUNDANESE,                // 1CC0..1CC7
            UNKNOWN,                  // 1CC8..1CCF
            INHERITED,                // 1CD0..1CD2
            COMMON,                   // 1CD3
            INHERITED,                // 1CD4..1CE0
            COMMON,                   // 1CE1
            INHERITED,                // 1CE2..1CE8
            COMMON,                   // 1CE9..1CEC
            INHERITED,                // 1CED
            COMMON,                   // 1CEE..1CF3
            INHERITED,                // 1CF4
            COMMON,                   // 1CF5..1CF7
            INHERITED,                // 1CF8..1CF9
            COMMON,                   // 1CFA
            UNKNOWN,                  // 1CFB..1CFF
            LATIN,                    // 1D00..1D25
            GREEK,                    // 1D26..1D2A
            CYRILLIC,                 // 1D2B
            LATIN,                    // 1D2C..1D5C
            GREEK,                    // 1D5D..1D61
            LATIN,                    // 1D62..1D65
            GREEK,                    // 1D66..1D6A
            LATIN,                    // 1D6B..1D77
            CYRILLIC,                 // 1D78
            LATIN,                    // 1D79..1DBE
            GREEK,                    // 1DBF
            INHERITED,                // 1DC0..1DF9
            UNKNOWN,                  // 1DFA
            INHERITED,                // 1DFB..1DFF
            LATIN,                    // 1E00..1EFF
            GREEK,                    // 1F00..1F15
            UNKNOWN,                  // 1F16..1F17
            GREEK,                    // 1F18..1F1D
            UNKNOWN,                  // 1F1E..1F1F
            GREEK,                    // 1F20..1F45
            UNKNOWN,                  // 1F46..1F47
            GREEK,                    // 1F48..1F4D
            UNKNOWN,                  // 1F4E..1F4F
            GREEK,                    // 1F50..1F57
            UNKNOWN,                  // 1F58
            GREEK,                    // 1F59
            UNKNOWN,                  // 1F5A
            GREEK,                    // 1F5B
            UNKNOWN,                  // 1F5C
            GREEK,                    // 1F5D
            UNKNOWN,                  // 1F5E
            GREEK,                    // 1F5F..1F7D
            UNKNOWN,                  // 1F7E..1F7F
            GREEK,                    // 1F80..1FB4
            UNKNOWN,                  // 1FB5
            GREEK,                    // 1FB6..1FC4
            UNKNOWN,                  // 1FC5
            GREEK,                    // 1FC6..1FD3
            UNKNOWN,                  // 1FD4..1FD5
            GREEK,                    // 1FD6..1FDB
            UNKNOWN,                  // 1FDC
            GREEK,                    // 1FDD..1FEF
            UNKNOWN,                  // 1FF0..1FF1
            GREEK,                    // 1FF2..1FF4
            UNKNOWN,                  // 1FF5
            GREEK,                    // 1FF6..1FFE
            UNKNOWN,                  // 1FFF
            COMMON,                   // 2000..200B
            INHERITED,                // 200C..200D
            COMMON,                   // 200E..2064
            UNKNOWN,                  // 2065
            COMMON,                   // 2066..2070
            LATIN,                    // 2071
            UNKNOWN,                  // 2072..2073
            COMMON,                   // 2074..207E
            LATIN,                    // 207F
            COMMON,                   // 2080..208E
            UNKNOWN,                  // 208F
            LATIN,                    // 2090..209C
            UNKNOWN,                  // 209D..209F
            COMMON,                   // 20A0..20BF
            UNKNOWN,                  // 20C0..20CF
            INHERITED,                // 20D0..20F0
            UNKNOWN,                  // 20F1..20FF
            COMMON,                   // 2100..2125
            GREEK,                    // 2126
            COMMON,                   // 2127..2129
            LATIN,                    // 212A..212B
            COMMON,                   // 212C..2131
            LATIN,                    // 2132
            COMMON,                   // 2133..214D
            LATIN,                    // 214E
            COMMON,                   // 214F..215F
            LATIN,                    // 2160..2188
            COMMON,                   // 2189..218B
            UNKNOWN,                  // 218C..218F
            COMMON,                   // 2190..2426
            UNKNOWN,                  // 2427..243F
            COMMON,                   // 2440..244A
            UNKNOWN,                  // 244B..245F
            COMMON,                   // 2460..27FF
            BRAILLE,                  // 2800..28FF
            COMMON,                   // 2900..2B73
            UNKNOWN,                  // 2B74..2B75
            COMMON,                   // 2B76..2B95
            UNKNOWN,                  // 2B96
            COMMON,                   // 2B97..2BFF
            GLAGOLITIC,               // 2C00..2C2E
            UNKNOWN,                  // 2C2F
            GLAGOLITIC,               // 2C30..2C5E
            UNKNOWN,                  // 2C5F
            LATIN,                    // 2C60..2C7F
            COPTIC,                   // 2C80..2CF3
            UNKNOWN,                  // 2CF4..2CF8
            COPTIC,                   // 2CF9..2CFF
            GEORGIAN,                 // 2D00..2D25
            UNKNOWN,                  // 2D26
            GEORGIAN,                 // 2D27
            UNKNOWN,                  // 2D28..2D2C
            GEORGIAN,                 // 2D2D
            UNKNOWN,                  // 2D2E..2D2F
            TIFINAGH,                 // 2D30..2D67
            UNKNOWN,                  // 2D68..2D6E
            TIFINAGH,                 // 2D6F..2D70
            UNKNOWN,                  // 2D71..2D7E
            TIFINAGH,                 // 2D7F
            ETHIOPIC,                 // 2D80..2D96
            UNKNOWN,                  // 2D97..2D9F
            ETHIOPIC,                 // 2DA0..2DA6
            UNKNOWN,                  // 2DA7
            ETHIOPIC,                 // 2DA8..2DAE
            UNKNOWN,                  // 2DAF
            ETHIOPIC,                 // 2DB0..2DB6
            UNKNOWN,                  // 2DB7
            ETHIOPIC,                 // 2DB8..2DBE
            UNKNOWN,                  // 2DBF
            ETHIOPIC,                 // 2DC0..2DC6
            UNKNOWN,                  // 2DC7
            ETHIOPIC,                 // 2DC8..2DCE
            UNKNOWN,                  // 2DCF
            ETHIOPIC,                 // 2DD0..2DD6
            UNKNOWN,                  // 2DD7
            ETHIOPIC,                 // 2DD8..2DDE
            UNKNOWN,                  // 2DDF
            CYRILLIC,                 // 2DE0..2DFF
            COMMON,                   // 2E00..2E52
            UNKNOWN,                  // 2E53..2E7F
            HAN,                      // 2E80..2E99
            UNKNOWN,                  // 2E9A
            HAN,                      // 2E9B..2EF3
            UNKNOWN,                  // 2EF4..2EFF
            HAN,                      // 2F00..2FD5
            UNKNOWN,                  // 2FD6..2FEF
            COMMON,                   // 2FF0..2FFB
            UNKNOWN,                  // 2FFC..2FFF
            COMMON,                   // 3000..3004
            HAN,                      // 3005
            COMMON,                   // 3006
            HAN,                      // 3007
            COMMON,                   // 3008..3020
            HAN,                      // 3021..3029
            INHERITED,                // 302A..302D
            HANGUL,                   // 302E..302F
            COMMON,                   // 3030..3037
            HAN,                      // 3038..303B
            COMMON,                   // 303C..303F
            UNKNOWN,                  // 3040
            HIRAGANA,                 // 3041..3096
            UNKNOWN,                  // 3097..3098
            INHERITED,                // 3099..309A
            COMMON,                   // 309B..309C
            HIRAGANA,                 // 309D..309F
            COMMON,                   // 30A0
            KATAKANA,                 // 30A1..30FA
            COMMON,                   // 30FB..30FC
            KATAKANA,                 // 30FD..30FF
            UNKNOWN,                  // 3100..3104
            BOPOMOFO,                 // 3105..312F
            UNKNOWN,                  // 3130
            HANGUL,                   // 3131..318E
            UNKNOWN,                  // 318F
            COMMON,                   // 3190..319F
            BOPOMOFO,                 // 31A0..31BF
            COMMON,                   // 31C0..31E3
            UNKNOWN,                  // 31E4..31EF
            KATAKANA,                 // 31F0..31FF
            HANGUL,                   // 3200..321E
            UNKNOWN,                  // 321F
            COMMON,                   // 3220..325F
            HANGUL,                   // 3260..327E
            COMMON,                   // 327F..32CF
            KATAKANA,                 // 32D0..32FE
            COMMON,                   // 32FF
            KATAKANA,                 // 3300..3357
            COMMON,                   // 3358..33FF
            HAN,                      // 3400..4DBF
            COMMON,                   // 4DC0..4DFF
            HAN,                      // 4E00..9FFC
            UNKNOWN,                  // 9FFD..9FFF
            YI,                       // A000..A48C
            UNKNOWN,                  // A48D..A48F
            YI,                       // A490..A4C6
            UNKNOWN,                  // A4C7..A4CF
            LISU,                     // A4D0..A4FF
            VAI,                      // A500..A62B
            UNKNOWN,                  // A62C..A63F
            CYRILLIC,                 // A640..A69F
            BAMUM,                    // A6A0..A6F7
            UNKNOWN,                  // A6F8..A6FF
            COMMON,                   // A700..A721
            LATIN,                    // A722..A787
            COMMON,                   // A788..A78A
            LATIN,                    // A78B..A7BF
            UNKNOWN,                  // A7C0..A7C1
            LATIN,                    // A7C2..A7CA
            UNKNOWN,                  // A7CB..A7F4
            LATIN,                    // A7F5..A7FF
            SYLOTI_NAGRI,             // A800..A82C
            UNKNOWN,                  // A82D..A82F
            COMMON,                   // A830..A839
            UNKNOWN,                  // A83A..A83F
            PHAGS_PA,                 // A840..A877
            UNKNOWN,                  // A878..A87F
            SAURASHTRA,               // A880..A8C5
            UNKNOWN,                  // A8C6..A8CD
            SAURASHTRA,               // A8CE..A8D9
            UNKNOWN,                  // A8DA..A8DF
            DEVANAGARI,               // A8E0..A8FF
            KAYAH_LI,                 // A900..A92D
            COMMON,                   // A92E
            KAYAH_LI,                 // A92F
            REJANG,                   // A930..A953
            UNKNOWN,                  // A954..A95E
            REJANG,                   // A95F
            HANGUL,                   // A960..A97C
            UNKNOWN,                  // A97D..A97F
            JAVANESE,                 // A980..A9CD
            UNKNOWN,                  // A9CE
            COMMON,                   // A9CF
            JAVANESE,                 // A9D0..A9D9
            UNKNOWN,                  // A9DA..A9DD
            JAVANESE,                 // A9DE..A9DF
            MYANMAR,                  // A9E0..A9FE
            UNKNOWN,                  // A9FF
            CHAM,                     // AA00..AA36
            UNKNOWN,                  // AA37..AA3F
            CHAM,                     // AA40..AA4D
            UNKNOWN,                  // AA4E..AA4F
            CHAM,                     // AA50..AA59
            UNKNOWN,                  // AA5A..AA5B
            CHAM,                     // AA5C..AA5F
            MYANMAR,                  // AA60..AA7F
            TAI_VIET,                 // AA80..AAC2
            UNKNOWN,                  // AAC3..AADA
            TAI_VIET,                 // AADB..AADF
            MEETEI_MAYEK,             // AAE0..AAF6
            UNKNOWN,                  // AAF7..AB00
            ETHIOPIC,                 // AB01..AB06
            UNKNOWN,                  // AB07..AB08
            ETHIOPIC,                 // AB09..AB0E
            UNKNOWN,                  // AB0F..AB10
            ETHIOPIC,                 // AB11..AB16
            UNKNOWN,                  // AB17..AB1F
            ETHIOPIC,                 // AB20..AB26
            UNKNOWN,                  // AB27
            ETHIOPIC,                 // AB28..AB2E
            UNKNOWN,                  // AB2F
            LATIN,                    // AB30..AB5A
            COMMON,                   // AB5B
            LATIN,                    // AB5C..AB64
            GREEK,                    // AB65
            LATIN,                    // AB66..AB69
            COMMON,                   // AB6A..AB6B
            UNKNOWN,                  // AB6C..AB6F
            CHEROKEE,                 // AB70..ABBF
            MEETEI_MAYEK,             // ABC0..ABED
            UNKNOWN,                  // ABEE..ABEF
            MEETEI_MAYEK,             // ABF0..ABF9
            UNKNOWN,                  // ABFA..ABFF
            HANGUL,                   // AC00..D7A3
            UNKNOWN,                  // D7A4..D7AF
            HANGUL,                   // D7B0..D7C6
            UNKNOWN,                  // D7C7..D7CA
            HANGUL,                   // D7CB..D7FB
            UNKNOWN,                  // D7FC..F8FF
            HAN,                      // F900..FA6D
            UNKNOWN,                  // FA6E..FA6F
            HAN,                      // FA70..FAD9
            UNKNOWN,                  // FADA..FAFF
            LATIN,                    // FB00..FB06
            UNKNOWN,                  // FB07..FB12
            ARMENIAN,                 // FB13..FB17
            UNKNOWN,                  // FB18..FB1C
            HEBREW,                   // FB1D..FB36
            UNKNOWN,                  // FB37
            HEBREW,                   // FB38..FB3C
            UNKNOWN,                  // FB3D
            HEBREW,                   // FB3E
            UNKNOWN,                  // FB3F
            HEBREW,                   // FB40..FB41
            UNKNOWN,                  // FB42
            HEBREW,                   // FB43..FB44
            UNKNOWN,                  // FB45
            HEBREW,                   // FB46..FB4F
            ARABIC,                   // FB50..FBC1
            UNKNOWN,                  // FBC2..FBD2
            ARABIC,                   // FBD3..FD3D
            COMMON,                   // FD3E..FD3F
            UNKNOWN,                  // FD40..FD4F
            ARABIC,                   // FD50..FD8F
            UNKNOWN,                  // FD90..FD91
            ARABIC,                   // FD92..FDC7
            UNKNOWN,                  // FDC8..FDEF
            ARABIC,                   // FDF0..FDFD
            UNKNOWN,                  // FDFE..FDFF
            INHERITED,                // FE00..FE0F
            COMMON,                   // FE10..FE19
            UNKNOWN,                  // FE1A..FE1F
            INHERITED,                // FE20..FE2D
            CYRILLIC,                 // FE2E..FE2F
            COMMON,                   // FE30..FE52
            UNKNOWN,                  // FE53
            COMMON,                   // FE54..FE66
            UNKNOWN,                  // FE67
            COMMON,                   // FE68..FE6B
            UNKNOWN,                  // FE6C..FE6F
            ARABIC,                   // FE70..FE74
            UNKNOWN,                  // FE75
            ARABIC,                   // FE76..FEFC
            UNKNOWN,                  // FEFD..FEFE
            COMMON,                   // FEFF
            UNKNOWN,                  // FF00
            COMMON,                   // FF01..FF20
            LATIN,                    // FF21..FF3A
            COMMON,                   // FF3B..FF40
            LATIN,                    // FF41..FF5A
            COMMON,                   // FF5B..FF65
            KATAKANA,                 // FF66..FF6F
            COMMON,                   // FF70
            KATAKANA,                 // FF71..FF9D
            COMMON,                   // FF9E..FF9F
            HANGUL,                   // FFA0..FFBE
            UNKNOWN,                  // FFBF..FFC1
            HANGUL,                   // FFC2..FFC7
            UNKNOWN,                  // FFC8..FFC9
            HANGUL,                   // FFCA..FFCF
            UNKNOWN,                  // FFD0..FFD1
            HANGUL,                   // FFD2..FFD7
            UNKNOWN,                  // FFD8..FFD9
            HANGUL,                   // FFDA..FFDC
            UNKNOWN,                  // FFDD..FFDF
            COMMON,                   // FFE0..FFE6
            UNKNOWN,                  // FFE7
            COMMON,                   // FFE8..FFEE
            UNKNOWN,                  // FFEF..FFF8
            COMMON,                   // FFF9..FFFD
            UNKNOWN,                  // FFFE..FFFF
            LINEAR_B,                 // 10000..1000B
            UNKNOWN,                  // 1000C
            LINEAR_B,                 // 1000D..10026
            UNKNOWN,                  // 10027
            LINEAR_B,                 // 10028..1003A
            UNKNOWN,                  // 1003B
            LINEAR_B,                 // 1003C..1003D
            UNKNOWN,                  // 1003E
            LINEAR_B,                 // 1003F..1004D
            UNKNOWN,                  // 1004E..1004F
            LINEAR_B,                 // 10050..1005D
            UNKNOWN,                  // 1005E..1007F
            LINEAR_B,                 // 10080..100FA
            UNKNOWN,                  // 100FB..100FF
            COMMON,                   // 10100..10102
            UNKNOWN,                  // 10103..10106
            COMMON,                   // 10107..10133
            UNKNOWN,                  // 10134..10136
            COMMON,                   // 10137..1013F
            GREEK,                    // 10140..1018E
            UNKNOWN,                  // 1018F
            COMMON,                   // 10190..1019C
            UNKNOWN,                  // 1019D..1019F
            GREEK,                    // 101A0
            UNKNOWN,                  // 101A1..101CF
            COMMON,                   // 101D0..101FC
            INHERITED,                // 101FD
            UNKNOWN,                  // 101FE..1027F
            LYCIAN,                   // 10280..1029C
            UNKNOWN,                  // 1029D..1029F
            CARIAN,                   // 102A0..102D0
            UNKNOWN,                  // 102D1..102DF
            INHERITED,                // 102E0
            COMMON,                   // 102E1..102FB
            UNKNOWN,                  // 102FC..102FF
            OLD_ITALIC,               // 10300..10323
            UNKNOWN,                  // 10324..1032C
            OLD_ITALIC,               // 1032D..1032F
            GOTHIC,                   // 10330..1034A
            UNKNOWN,                  // 1034B..1034F
            OLD_PERMIC,               // 10350..1037A
            UNKNOWN,                  // 1037B..1037F
            UGARITIC,                 // 10380..1039D
            UNKNOWN,                  // 1039E
            UGARITIC,                 // 1039F
            OLD_PERSIAN,              // 103A0..103C3
            UNKNOWN,                  // 103C4..103C7
            OLD_PERSIAN,              // 103C8..103D5
            UNKNOWN,                  // 103D6..103FF
            DESERET,                  // 10400..1044F
            SHAVIAN,                  // 10450..1047F
            OSMANYA,                  // 10480..1049D
            UNKNOWN,                  // 1049E..1049F
            OSMANYA,                  // 104A0..104A9
            UNKNOWN,                  // 104AA..104AF
            OSAGE,                    // 104B0..104D3
            UNKNOWN,                  // 104D4..104D7
            OSAGE,                    // 104D8..104FB
            UNKNOWN,                  // 104FC..104FF
            ELBASAN,                  // 10500..10527
            UNKNOWN,                  // 10528..1052F
            CAUCASIAN_ALBANIAN,       // 10530..10563
            UNKNOWN,                  // 10564..1056E
            CAUCASIAN_ALBANIAN,       // 1056F
            UNKNOWN,                  // 10570..105FF
            LINEAR_A,                 // 10600..10736
            UNKNOWN,                  // 10737..1073F
            LINEAR_A,                 // 10740..10755
            UNKNOWN,                  // 10756..1075F
            LINEAR_A,                 // 10760..10767
            UNKNOWN,                  // 10768..107FF
            CYPRIOT,                  // 10800..10805
            UNKNOWN,                  // 10806..10807
            CYPRIOT,                  // 10808
            UNKNOWN,                  // 10809
            CYPRIOT,                  // 1080A..10835
            UNKNOWN,                  // 10836
            CYPRIOT,                  // 10837..10838
            UNKNOWN,                  // 10839..1083B
            CYPRIOT,                  // 1083C
            UNKNOWN,                  // 1083D..1083E
            CYPRIOT,                  // 1083F
            IMPERIAL_ARAMAIC,         // 10840..10855
            UNKNOWN,                  // 10856
            IMPERIAL_ARAMAIC,         // 10857..1085F
            PALMYRENE,                // 10860..1087F
            NABATAEAN,                // 10880..1089E
            UNKNOWN,                  // 1089F..108A6
            NABATAEAN,                // 108A7..108AF
            UNKNOWN,                  // 108B0..108DF
            HATRAN,                   // 108E0..108F2
            UNKNOWN,                  // 108F3
            HATRAN,                   // 108F4..108F5
            UNKNOWN,                  // 108F6..108FA
            HATRAN,                   // 108FB..108FF
            PHOENICIAN,               // 10900..1091B
            UNKNOWN,                  // 1091C..1091E
            PHOENICIAN,               // 1091F
            LYDIAN,                   // 10920..10939
            UNKNOWN,                  // 1093A..1093E
            LYDIAN,                   // 1093F
            UNKNOWN,                  // 10940..1097F
            MEROITIC_HIEROGLYPHS,     // 10980..1099F
            MEROITIC_CURSIVE,         // 109A0..109B7
            UNKNOWN,                  // 109B8..109BB
            MEROITIC_CURSIVE,         // 109BC..109CF
            UNKNOWN,                  // 109D0..109D1
            MEROITIC_CURSIVE,         // 109D2..109FF
            KHAROSHTHI,               // 10A00..10A03
            UNKNOWN,                  // 10A04
            KHAROSHTHI,               // 10A05..10A06
            UNKNOWN,                  // 10A07..10A0B
            KHAROSHTHI,               // 10A0C..10A13
            UNKNOWN,                  // 10A14
            KHAROSHTHI,               // 10A15..10A17
            UNKNOWN,                  // 10A18
            KHAROSHTHI,               // 10A19..10A35
            UNKNOWN,                  // 10A36..10A37
            KHAROSHTHI,               // 10A38..10A3A
            UNKNOWN,                  // 10A3B..10A3E
            KHAROSHTHI,               // 10A3F..10A48
            UNKNOWN,                  // 10A49..10A4F
            KHAROSHTHI,               // 10A50..10A58
            UNKNOWN,                  // 10A59..10A5F
            OLD_SOUTH_ARABIAN,        // 10A60..10A7F
            OLD_NORTH_ARABIAN,        // 10A80..10A9F
            UNKNOWN,                  // 10AA0..10ABF
            MANICHAEAN,               // 10AC0..10AE6
            UNKNOWN,                  // 10AE7..10AEA
            MANICHAEAN,               // 10AEB..10AF6
            UNKNOWN,                  // 10AF7..10AFF
            AVESTAN,                  // 10B00..10B35
            UNKNOWN,                  // 10B36..10B38
            AVESTAN,                  // 10B39..10B3F
            INSCRIPTIONAL_PARTHIAN,   // 10B40..10B55
            UNKNOWN,                  // 10B56..10B57
            INSCRIPTIONAL_PARTHIAN,   // 10B58..10B5F
            INSCRIPTIONAL_PAHLAVI,    // 10B60..10B72
            UNKNOWN,                  // 10B73..10B77
            INSCRIPTIONAL_PAHLAVI,    // 10B78..10B7F
            PSALTER_PAHLAVI,          // 10B80..10B91
            UNKNOWN,                  // 10B92..10B98
            PSALTER_PAHLAVI,          // 10B99..10B9C
            UNKNOWN,                  // 10B9D..10BA8
            PSALTER_PAHLAVI,          // 10BA9..10BAF
            UNKNOWN,                  // 10BB0..10BFF
            OLD_TURKIC,               // 10C00..10C48
            UNKNOWN,                  // 10C49..10C7F
            OLD_HUNGARIAN,            // 10C80..10CB2
            UNKNOWN,                  // 10CB3..10CBF
            OLD_HUNGARIAN,            // 10CC0..10CF2
            UNKNOWN,                  // 10CF3..10CF9
            OLD_HUNGARIAN,            // 10CFA..10CFF
            HANIFI_ROHINGYA,          // 10D00..10D27
            UNKNOWN,                  // 10D28..10D2F
            HANIFI_ROHINGYA,          // 10D30..10D39
            UNKNOWN,                  // 10D3A..10E5F
            ARABIC,                   // 10E60..10E7E
            UNKNOWN,                  // 10E7F
            YEZIDI,                   // 10E80..10EA9
            UNKNOWN,                  // 10EAA
            YEZIDI,                   // 10EAB..10EAD
            UNKNOWN,                  // 10EAE..10EAF
            YEZIDI,                   // 10EB0..10EB1
            UNKNOWN,                  // 10EB2..10EFF
            OLD_SOGDIAN,              // 10F00..10F27
            UNKNOWN,                  // 10F28..10F2F
            SOGDIAN,                  // 10F30..10F59
            UNKNOWN,                  // 10F5A..10FAF
            CHORASMIAN,               // 10FB0..10FCB
            UNKNOWN,                  // 10FCC..10FDF
            ELYMAIC,                  // 10FE0..10FF6
            UNKNOWN,                  // 10FF7..10FFF
            BRAHMI,                   // 11000..1104D
            UNKNOWN,                  // 1104E..11051
            BRAHMI,                   // 11052..1106F
            UNKNOWN,                  // 11070..1107E
            BRAHMI,                   // 1107F
            KAITHI,                   // 11080..110C1
            UNKNOWN,                  // 110C2..110CC
            KAITHI,                   // 110CD
            UNKNOWN,                  // 110CE..110CF
            SORA_SOMPENG,             // 110D0..110E8
            UNKNOWN,                  // 110E9..110EF
            SORA_SOMPENG,             // 110F0..110F9
            UNKNOWN,                  // 110FA..110FF
            CHAKMA,                   // 11100..11134
            UNKNOWN,                  // 11135
            CHAKMA,                   // 11136..11147
            UNKNOWN,                  // 11148..1114F
            MAHAJANI,                 // 11150..11176
            UNKNOWN,                  // 11177..1117F
            SHARADA,                  // 11180..111DF
            UNKNOWN,                  // 111E0
            SINHALA,                  // 111E1..111F4
            UNKNOWN,                  // 111F5..111FF
            KHOJKI,                   // 11200..11211
            UNKNOWN,                  // 11212
            KHOJKI,                   // 11213..1123E
            UNKNOWN,                  // 1123F..1127F
            MULTANI,                  // 11280..11286
            UNKNOWN,                  // 11287
            MULTANI,                  // 11288
            UNKNOWN,                  // 11289
            MULTANI,                  // 1128A..1128D
            UNKNOWN,                  // 1128E
            MULTANI,                  // 1128F..1129D
            UNKNOWN,                  // 1129E
            MULTANI,                  // 1129F..112A9
            UNKNOWN,                  // 112AA..112AF
            KHUDAWADI,                // 112B0..112EA
            UNKNOWN,                  // 112EB..112EF
            KHUDAWADI,                // 112F0..112F9
            UNKNOWN,                  // 112FA..112FF
            GRANTHA,                  // 11300..11303
            UNKNOWN,                  // 11304
            GRANTHA,                  // 11305..1130C
            UNKNOWN,                  // 1130D..1130E
            GRANTHA,                  // 1130F..11310
            UNKNOWN,                  // 11311..11312
            GRANTHA,                  // 11313..11328
            UNKNOWN,                  // 11329
            GRANTHA,                  // 1132A..11330
            UNKNOWN,                  // 11331
            GRANTHA,                  // 11332..11333
            UNKNOWN,                  // 11334
            GRANTHA,                  // 11335..11339
            UNKNOWN,                  // 1133A
            INHERITED,                // 1133B
            GRANTHA,                  // 1133C..11344
            UNKNOWN,                  // 11345..11346
            GRANTHA,                  // 11347..11348
            UNKNOWN,                  // 11349..1134A
            GRANTHA,                  // 1134B..1134D
            UNKNOWN,                  // 1134E..1134F
            GRANTHA,                  // 11350
            UNKNOWN,                  // 11351..11356
            GRANTHA,                  // 11357
            UNKNOWN,                  // 11358..1135C
            GRANTHA,                  // 1135D..11363
            UNKNOWN,                  // 11364..11365
            GRANTHA,                  // 11366..1136C
            UNKNOWN,                  // 1136D..1136F
            GRANTHA,                  // 11370..11374
            UNKNOWN,                  // 11375..113FF
            NEWA,                     // 11400..1145B
            UNKNOWN,                  // 1145C
            NEWA,                     // 1145D..11461
            UNKNOWN,                  // 11462..1147F
            TIRHUTA,                  // 11480..114C7
            UNKNOWN,                  // 114C8..114CF
            TIRHUTA,                  // 114D0..114D9
            UNKNOWN,                  // 114DA..1157F
            SIDDHAM,                  // 11580..115B5
            UNKNOWN,                  // 115B6..115B7
            SIDDHAM,                  // 115B8..115DD
            UNKNOWN,                  // 115DE..115FF
            MODI,                     // 11600..11644
            UNKNOWN,                  // 11645..1164F
            MODI,                     // 11650..11659
            UNKNOWN,                  // 1165A..1165F
            MONGOLIAN,                // 11660..1166C
            UNKNOWN,                  // 1166D..1167F
            TAKRI,                    // 11680..116B8
            UNKNOWN,                  // 116B9..116BF
            TAKRI,                    // 116C0..116C9
            UNKNOWN,                  // 116CA..116FF
            AHOM,                     // 11700..1171A
            UNKNOWN,                  // 1171B..1171C
            AHOM,                     // 1171D..1172B
            UNKNOWN,                  // 1172C..1172F
            AHOM,                     // 11730..1173F
            UNKNOWN,                  // 11740..117FF
            DOGRA,                    // 11800..1183B
            UNKNOWN,                  // 1183C..1189F
            WARANG_CITI,              // 118A0..118F2
            UNKNOWN,                  // 118F3..118FE
            WARANG_CITI,              // 118FF
            DIVES_AKURU,              // 11900..11906
            UNKNOWN,                  // 11907..11908
            DIVES_AKURU,              // 11909
            UNKNOWN,                  // 1190A..1190B
            DIVES_AKURU,              // 1190C..11913
            UNKNOWN,                  // 11914
            DIVES_AKURU,              // 11915..11916
            UNKNOWN,                  // 11917
            DIVES_AKURU,              // 11918..11935
            UNKNOWN,                  // 11936
            DIVES_AKURU,              // 11937..11938
            UNKNOWN,                  // 11939..1193A
            DIVES_AKURU,              // 1193B..11946
            UNKNOWN,                  // 11947..1194F
            DIVES_AKURU,              // 11950..11959
            UNKNOWN,                  // 1195A..1199F
            NANDINAGARI,              // 119A0..119A7
            UNKNOWN,                  // 119A8..119A9
            NANDINAGARI,              // 119AA..119D7
            UNKNOWN,                  // 119D8..119D9
            NANDINAGARI,              // 119DA..119E4
            UNKNOWN,                  // 119E5..119FF
            ZANABAZAR_SQUARE,         // 11A00..11A47
            UNKNOWN,                  // 11A48..11A4F
            SOYOMBO,                  // 11A50..11AA2
            UNKNOWN,                  // 11AA3..11ABF
            PAU_CIN_HAU,              // 11AC0..11AF8
            UNKNOWN,                  // 11AF9..11BFF
            BHAIKSUKI,                // 11C00..11C08
            UNKNOWN,                  // 11C09
            BHAIKSUKI,                // 11C0A..11C36
            UNKNOWN,                  // 11C37
            BHAIKSUKI,                // 11C38..11C45
            UNKNOWN,                  // 11C46..11C4F
            BHAIKSUKI,                // 11C50..11C6C
            UNKNOWN,                  // 11C6D..11C6F
            MARCHEN,                  // 11C70..11C8F
            UNKNOWN,                  // 11C90..11C91
            MARCHEN,                  // 11C92..11CA7
            UNKNOWN,                  // 11CA8
            MARCHEN,                  // 11CA9..11CB6
            UNKNOWN,                  // 11CB7..11CFF
            MASARAM_GONDI,            // 11D00..11D06
            UNKNOWN,                  // 11D07
            MASARAM_GONDI,            // 11D08..11D09
            UNKNOWN,                  // 11D0A
            MASARAM_GONDI,            // 11D0B..11D36
            UNKNOWN,                  // 11D37..11D39
            MASARAM_GONDI,            // 11D3A
            UNKNOWN,                  // 11D3B
            MASARAM_GONDI,            // 11D3C..11D3D
            UNKNOWN,                  // 11D3E
            MASARAM_GONDI,            // 11D3F..11D47
            UNKNOWN,                  // 11D48..11D4F
            MASARAM_GONDI,            // 11D50..11D59
            UNKNOWN,                  // 11D5A..11D5F
            GUNJALA_GONDI,            // 11D60..11D65
            UNKNOWN,                  // 11D66
            GUNJALA_GONDI,            // 11D67..11D68
            UNKNOWN,                  // 11D69
            GUNJALA_GONDI,            // 11D6A..11D8E
            UNKNOWN,                  // 11D8F
            GUNJALA_GONDI,            // 11D90..11D91
            UNKNOWN,                  // 11D92
            GUNJALA_GONDI,            // 11D93..11D98
            UNKNOWN,                  // 11D99..11D9F
            GUNJALA_GONDI,            // 11DA0..11DA9
            UNKNOWN,                  // 11DAA..11EDF
            MAKASAR,                  // 11EE0..11EF8
            UNKNOWN,                  // 11EF9..11FAF
            LISU,                     // 11FB0
            UNKNOWN,                  // 11FB1..11FBF
            TAMIL,                    // 11FC0..11FF1
            UNKNOWN,                  // 11FF2..11FFE
            TAMIL,                    // 11FFF
            CUNEIFORM,                // 12000..12399
            UNKNOWN,                  // 1239A..123FF
            CUNEIFORM,                // 12400..1246E
            UNKNOWN,                  // 1246F
            CUNEIFORM,                // 12470..12474
            UNKNOWN,                  // 12475..1247F
            CUNEIFORM,                // 12480..12543
            UNKNOWN,                  // 12544..12FFF
            EGYPTIAN_HIEROGLYPHS,     // 13000..1342E
            UNKNOWN,                  // 1342F
            EGYPTIAN_HIEROGLYPHS,     // 13430..13438
            UNKNOWN,                  // 13439..143FF
            ANATOLIAN_HIEROGLYPHS,    // 14400..14646
            UNKNOWN,                  // 14647..167FF
            BAMUM,                    // 16800..16A38
            UNKNOWN,                  // 16A39..16A3F
            MRO,                      // 16A40..16A5E
            UNKNOWN,                  // 16A5F
            MRO,                      // 16A60..16A69
            UNKNOWN,                  // 16A6A..16A6D
            MRO,                      // 16A6E..16A6F
            UNKNOWN,                  // 16A70..16ACF
            BASSA_VAH,                // 16AD0..16AED
            UNKNOWN,                  // 16AEE..16AEF
            BASSA_VAH,                // 16AF0..16AF5
            UNKNOWN,                  // 16AF6..16AFF
            PAHAWH_HMONG,             // 16B00..16B45
            UNKNOWN,                  // 16B46..16B4F
            PAHAWH_HMONG,             // 16B50..16B59
            UNKNOWN,                  // 16B5A
            PAHAWH_HMONG,             // 16B5B..16B61
            UNKNOWN,                  // 16B62
            PAHAWH_HMONG,             // 16B63..16B77
            UNKNOWN,                  // 16B78..16B7C
            PAHAWH_HMONG,             // 16B7D..16B8F
            UNKNOWN,                  // 16B90..16E3F
            MEDEFAIDRIN,              // 16E40..16E9A
            UNKNOWN,                  // 16E9B..16EFF
            MIAO,                     // 16F00..16F4A
            UNKNOWN,                  // 16F4B..16F4E
            MIAO,                     // 16F4F..16F87
            UNKNOWN,                  // 16F88..16F8E
            MIAO,                     // 16F8F..16F9F
            UNKNOWN,                  // 16FA0..16FDF
            TANGUT,                   // 16FE0
            NUSHU,                    // 16FE1
            COMMON,                   // 16FE2..16FE3
            KHITAN_SMALL_SCRIPT,      // 16FE4
            UNKNOWN,                  // 16FE5..16FEF
            HAN,                      // 16FF0..16FF1
            UNKNOWN,                  // 16FF2..16FFF
            TANGUT,                   // 17000..187F7
            UNKNOWN,                  // 187F8..187FF
            TANGUT,                   // 18800..18AFF
            KHITAN_SMALL_SCRIPT,      // 18B00..18CD5
            UNKNOWN,                  // 18CD6..18CFF
            TANGUT,                   // 18D00..18D08
            UNKNOWN,                  // 18D09..1AFFF
            KATAKANA,                 // 1B000
            HIRAGANA,                 // 1B001..1B11E
            UNKNOWN,                  // 1B11F..1B14F
            HIRAGANA,                 // 1B150..1B152
            UNKNOWN,                  // 1B153..1B163
            KATAKANA,                 // 1B164..1B167
            UNKNOWN,                  // 1B168..1B16F
            NUSHU,                    // 1B170..1B2FB
            UNKNOWN,                  // 1B2FC..1BBFF
            DUPLOYAN,                 // 1BC00..1BC6A
            UNKNOWN,                  // 1BC6B..1BC6F
            DUPLOYAN,                 // 1BC70..1BC7C
            UNKNOWN,                  // 1BC7D..1BC7F
            DUPLOYAN,                 // 1BC80..1BC88
            UNKNOWN,                  // 1BC89..1BC8F
            DUPLOYAN,                 // 1BC90..1BC99
            UNKNOWN,                  // 1BC9A..1BC9B
            DUPLOYAN,                 // 1BC9C..1BC9F
            COMMON,                   // 1BCA0..1BCA3
            UNKNOWN,                  // 1BCA4..1CFFF
            COMMON,                   // 1D000..1D0F5
            UNKNOWN,                  // 1D0F6..1D0FF
            COMMON,                   // 1D100..1D126
            UNKNOWN,                  // 1D127..1D128
            COMMON,                   // 1D129..1D166
            INHERITED,                // 1D167..1D169
            COMMON,                   // 1D16A..1D17A
            INHERITED,                // 1D17B..1D182
            COMMON,                   // 1D183..1D184
            INHERITED,                // 1D185..1D18B
            COMMON,                   // 1D18C..1D1A9
            INHERITED,                // 1D1AA..1D1AD
            COMMON,                   // 1D1AE..1D1E8
            UNKNOWN,                  // 1D1E9..1D1FF
            GREEK,                    // 1D200..1D245
            UNKNOWN,                  // 1D246..1D2DF
            COMMON,                   // 1D2E0..1D2F3
            UNKNOWN,                  // 1D2F4..1D2FF
            COMMON,                   // 1D300..1D356
            UNKNOWN,                  // 1D357..1D35F
            COMMON,                   // 1D360..1D378
            UNKNOWN,                  // 1D379..1D3FF
            COMMON,                   // 1D400..1D454
            UNKNOWN,                  // 1D455
            COMMON,                   // 1D456..1D49C
            UNKNOWN,                  // 1D49D
            COMMON,                   // 1D49E..1D49F
            UNKNOWN,                  // 1D4A0..1D4A1
            COMMON,                   // 1D4A2
            UNKNOWN,                  // 1D4A3..1D4A4
            COMMON,                   // 1D4A5..1D4A6
            UNKNOWN,                  // 1D4A7..1D4A8
            COMMON,                   // 1D4A9..1D4AC
            UNKNOWN,                  // 1D4AD
            COMMON,                   // 1D4AE..1D4B9
            UNKNOWN,                  // 1D4BA
            COMMON,                   // 1D4BB
            UNKNOWN,                  // 1D4BC
            COMMON,                   // 1D4BD..1D4C3
            UNKNOWN,                  // 1D4C4
            COMMON,                   // 1D4C5..1D505
            UNKNOWN,                  // 1D506
            COMMON,                   // 1D507..1D50A
            UNKNOWN,                  // 1D50B..1D50C
            COMMON,                   // 1D50D..1D514
            UNKNOWN,                  // 1D515
            COMMON,                   // 1D516..1D51C
            UNKNOWN,                  // 1D51D
            COMMON,                   // 1D51E..1D539
            UNKNOWN,                  // 1D53A
            COMMON,                   // 1D53B..1D53E
            UNKNOWN,                  // 1D53F
            COMMON,                   // 1D540..1D544
            UNKNOWN,                  // 1D545
            COMMON,                   // 1D546
            UNKNOWN,                  // 1D547..1D549
            COMMON,                   // 1D54A..1D550
            UNKNOWN,                  // 1D551
            COMMON,                   // 1D552..1D6A5
            UNKNOWN,                  // 1D6A6..1D6A7
            COMMON,                   // 1D6A8..1D7CB
            UNKNOWN,                  // 1D7CC..1D7CD
            COMMON,                   // 1D7CE..1D7FF
            SIGNWRITING,              // 1D800..1DA8B
            UNKNOWN,                  // 1DA8C..1DA9A
            SIGNWRITING,              // 1DA9B..1DA9F
            UNKNOWN,                  // 1DAA0
            SIGNWRITING,              // 1DAA1..1DAAF
            UNKNOWN,                  // 1DAB0..1DFFF
            GLAGOLITIC,               // 1E000..1E006
            UNKNOWN,                  // 1E007
            GLAGOLITIC,               // 1E008..1E018
            UNKNOWN,                  // 1E019..1E01A
            GLAGOLITIC,               // 1E01B..1E021
            UNKNOWN,                  // 1E022
            GLAGOLITIC,               // 1E023..1E024
            UNKNOWN,                  // 1E025
            GLAGOLITIC,               // 1E026..1E02A
            UNKNOWN,                  // 1E02B..1E0FF
            NYIAKENG_PUACHUE_HMONG,   // 1E100..1E12C
            UNKNOWN,                  // 1E12D..1E12F
            NYIAKENG_PUACHUE_HMONG,   // 1E130..1E13D
            UNKNOWN,                  // 1E13E..1E13F
            NYIAKENG_PUACHUE_HMONG,   // 1E140..1E149
            UNKNOWN,                  // 1E14A..1E14D
            NYIAKENG_PUACHUE_HMONG,   // 1E14E..1E14F
            UNKNOWN,                  // 1E150..1E2BF
            WANCHO,                   // 1E2C0..1E2F9
            UNKNOWN,                  // 1E2FA..1E2FE
            WANCHO,                   // 1E2FF
            UNKNOWN,                  // 1E300..1E7FF
            MENDE_KIKAKUI,            // 1E800..1E8C4
            UNKNOWN,                  // 1E8C5..1E8C6
            MENDE_KIKAKUI,            // 1E8C7..1E8D6
            UNKNOWN,                  // 1E8D7..1E8FF
            ADLAM,                    // 1E900..1E94B
            UNKNOWN,                  // 1E94C..1E94F
            ADLAM,                    // 1E950..1E959
            UNKNOWN,                  // 1E95A..1E95D
            ADLAM,                    // 1E95E..1E95F
            UNKNOWN,                  // 1E960..1EC70
            COMMON,                   // 1EC71..1ECB4
            UNKNOWN,                  // 1ECB5..1ED00
            COMMON,                   // 1ED01..1ED3D
            UNKNOWN,                  // 1ED3E..1EDFF
            ARABIC,                   // 1EE00..1EE03
            UNKNOWN,                  // 1EE04
            ARABIC,                   // 1EE05..1EE1F
            UNKNOWN,                  // 1EE20
            ARABIC,                   // 1EE21..1EE22
            UNKNOWN,                  // 1EE23
            ARABIC,                   // 1EE24
            UNKNOWN,                  // 1EE25..1EE26
            ARABIC,                   // 1EE27
            UNKNOWN,                  // 1EE28
            ARABIC,                   // 1EE29..1EE32
            UNKNOWN,                  // 1EE33
            ARABIC,                   // 1EE34..1EE37
            UNKNOWN,                  // 1EE38
            ARABIC,                   // 1EE39
            UNKNOWN,                  // 1EE3A
            ARABIC,                   // 1EE3B
            UNKNOWN,                  // 1EE3C..1EE41
            ARABIC,                   // 1EE42
            UNKNOWN,                  // 1EE43..1EE46
            ARABIC,                   // 1EE47
            UNKNOWN,                  // 1EE48
            ARABIC,                   // 1EE49
            UNKNOWN,                  // 1EE4A
            ARABIC,                   // 1EE4B
            UNKNOWN,                  // 1EE4C
            ARABIC,                   // 1EE4D..1EE4F
            UNKNOWN,                  // 1EE50
            ARABIC,                   // 1EE51..1EE52
            UNKNOWN,                  // 1EE53
            ARABIC,                   // 1EE54
            UNKNOWN,                  // 1EE55..1EE56
            ARABIC,                   // 1EE57
            UNKNOWN,                  // 1EE58
            ARABIC,                   // 1EE59
            UNKNOWN,                  // 1EE5A
            ARABIC,                   // 1EE5B
            UNKNOWN,                  // 1EE5C
            ARABIC,                   // 1EE5D
            UNKNOWN,                  // 1EE5E
            ARABIC,                   // 1EE5F
            UNKNOWN,                  // 1EE60
            ARABIC,                   // 1EE61..1EE62
            UNKNOWN,                  // 1EE63
            ARABIC,                   // 1EE64
            UNKNOWN,                  // 1EE65..1EE66
            ARABIC,                   // 1EE67..1EE6A
            UNKNOWN,                  // 1EE6B
            ARABIC,                   // 1EE6C..1EE72
            UNKNOWN,                  // 1EE73
            ARABIC,                   // 1EE74..1EE77
            UNKNOWN,                  // 1EE78
            ARABIC,                   // 1EE79..1EE7C
            UNKNOWN,                  // 1EE7D
            ARABIC,                   // 1EE7E
            UNKNOWN,                  // 1EE7F
            ARABIC,                   // 1EE80..1EE89
            UNKNOWN,                  // 1EE8A
            ARABIC,                   // 1EE8B..1EE9B
            UNKNOWN,                  // 1EE9C..1EEA0
            ARABIC,                   // 1EEA1..1EEA3
            UNKNOWN,                  // 1EEA4
            ARABIC,                   // 1EEA5..1EEA9
            UNKNOWN,                  // 1EEAA
            ARABIC,                   // 1EEAB..1EEBB
            UNKNOWN,                  // 1EEBC..1EEEF
            ARABIC,                   // 1EEF0..1EEF1
            UNKNOWN,                  // 1EEF2..1EFFF
            COMMON,                   // 1F000..1F02B
            UNKNOWN,                  // 1F02C..1F02F
            COMMON,                   // 1F030..1F093
            UNKNOWN,                  // 1F094..1F09F
            COMMON,                   // 1F0A0..1F0AE
            UNKNOWN,                  // 1F0AF..1F0B0
            COMMON,                   // 1F0B1..1F0BF
            UNKNOWN,                  // 1F0C0
            COMMON,                   // 1F0C1..1F0CF
            UNKNOWN,                  // 1F0D0
            COMMON,                   // 1F0D1..1F0F5
            UNKNOWN,                  // 1F0F6..1F0FF
            COMMON,                   // 1F100..1F1AD
            UNKNOWN,                  // 1F1AE..1F1E5
            COMMON,                   // 1F1E6..1F1FF
            HIRAGANA,                 // 1F200
            COMMON,                   // 1F201..1F202
            UNKNOWN,                  // 1F203..1F20F
            COMMON,                   // 1F210..1F23B
            UNKNOWN,                  // 1F23C..1F23F
            COMMON,                   // 1F240..1F248
            UNKNOWN,                  // 1F249..1F24F
            COMMON,                   // 1F250..1F251
            UNKNOWN,                  // 1F252..1F25F
            COMMON,                   // 1F260..1F265
            UNKNOWN,                  // 1F266..1F2FF
            COMMON,                   // 1F300..1F6D7
            UNKNOWN,                  // 1F6D8..1F6DF
            COMMON,                   // 1F6E0..1F6EC
            UNKNOWN,                  // 1F6ED..1F6EF
            COMMON,                   // 1F6F0..1F6FC
            UNKNOWN,                  // 1F6FD..1F6FF
            COMMON,                   // 1F700..1F773
            UNKNOWN,                  // 1F774..1F77F
            COMMON,                   // 1F780..1F7D8
            UNKNOWN,                  // 1F7D9..1F7DF
            COMMON,                   // 1F7E0..1F7EB
            UNKNOWN,                  // 1F7EC..1F7FF
            COMMON,                   // 1F800..1F80B
            UNKNOWN,                  // 1F80C..1F80F
            COMMON,                   // 1F810..1F847
            UNKNOWN,                  // 1F848..1F84F
            COMMON,                   // 1F850..1F859
            UNKNOWN,                  // 1F85A..1F85F
            COMMON,                   // 1F860..1F887
            UNKNOWN,                  // 1F888..1F88F
            COMMON,                   // 1F890..1F8AD
            UNKNOWN,                  // 1F8AE..1F8AF
            COMMON,                   // 1F8B0..1F8B1
            UNKNOWN,                  // 1F8B2..1F8FF
            COMMON,                   // 1F900..1F978
            UNKNOWN,                  // 1F979
            COMMON,                   // 1F97A..1F9CB
            UNKNOWN,                  // 1F9CC
            COMMON,                   // 1F9CD..1FA53
            UNKNOWN,                  // 1FA54..1FA5F
            COMMON,                   // 1FA60..1FA6D
            UNKNOWN,                  // 1FA6E..1FA6F
            COMMON,                   // 1FA70..1FA74
            UNKNOWN,                  // 1FA75..1FA77
            COMMON,                   // 1FA78..1FA7A
            UNKNOWN,                  // 1FA7B..1FA7F
            COMMON,                   // 1FA80..1FA86
            UNKNOWN,                  // 1FA87..1FA8F
            COMMON,                   // 1FA90..1FAA8
            UNKNOWN,                  // 1FAA9..1FAAF
            COMMON,                   // 1FAB0..1FAB6
            UNKNOWN,                  // 1FAB7..1FABF
            COMMON,                   // 1FAC0..1FAC2
            UNKNOWN,                  // 1FAC3..1FACF
            COMMON,                   // 1FAD0..1FAD6
            UNKNOWN,                  // 1FAD7..1FAFF
            COMMON,                   // 1FB00..1FB92
            UNKNOWN,                  // 1FB93
            COMMON,                   // 1FB94..1FBCA
            UNKNOWN,                  // 1FBCB..1FBEF
            COMMON,                   // 1FBF0..1FBF9
            UNKNOWN,                  // 1FBFA..1FFFF
            HAN,                      // 20000..2A6DD
            UNKNOWN,                  // 2A6DE..2A6FF
            HAN,                      // 2A700..2B734
            UNKNOWN,                  // 2B735..2B73F
            HAN,                      // 2B740..2B81D
            UNKNOWN,                  // 2B81E..2B81F
            HAN,                      // 2B820..2CEA1
            UNKNOWN,                  // 2CEA2..2CEAF
            HAN,                      // 2CEB0..2EBE0
            UNKNOWN,                  // 2EBE1..2F7FF
            HAN,                      // 2F800..2FA1D
            UNKNOWN,                  // 2FA1E..2FFFF
            HAN,                      // 30000..3134A
            UNKNOWN,                  // 3134B..E0000
            COMMON,                   // E0001
            UNKNOWN,                  // E0002..E001F
            COMMON,                   // E0020..E007F
            UNKNOWN,                  // E0080..E00FF
            INHERITED,                // E0100..E01EF
            UNKNOWN,                  // E01F0..10FFFF
        };

        private static final HashMap<String, Character.UnicodeScript> aliases;
        static {
            aliases = new HashMap<>((int)(157 / 0.75f + 1.0f));
            aliases.put("ADLM", ADLAM);
            aliases.put("AGHB", CAUCASIAN_ALBANIAN);
            aliases.put("AHOM", AHOM);
            aliases.put("ARAB", ARABIC);
            aliases.put("ARMI", IMPERIAL_ARAMAIC);
            aliases.put("ARMN", ARMENIAN);
            aliases.put("AVST", AVESTAN);
            aliases.put("BALI", BALINESE);
            aliases.put("BAMU", BAMUM);
            aliases.put("BASS", BASSA_VAH);
            aliases.put("BATK", BATAK);
            aliases.put("BENG", BENGALI);
            aliases.put("BHKS", BHAIKSUKI);
            aliases.put("BOPO", BOPOMOFO);
            aliases.put("BRAH", BRAHMI);
            aliases.put("BRAI", BRAILLE);
            aliases.put("BUGI", BUGINESE);
            aliases.put("BUHD", BUHID);
            aliases.put("CAKM", CHAKMA);
            aliases.put("CANS", CANADIAN_ABORIGINAL);
            aliases.put("CARI", CARIAN);
            aliases.put("CHAM", CHAM);
            aliases.put("CHER", CHEROKEE);
            aliases.put("CHRS", CHORASMIAN);
            aliases.put("COPT", COPTIC);
            aliases.put("CPRT", CYPRIOT);
            aliases.put("CYRL", CYRILLIC);
            aliases.put("DEVA", DEVANAGARI);
            aliases.put("DIAK", DIVES_AKURU);
            aliases.put("DOGR", DOGRA);
            aliases.put("DSRT", DESERET);
            aliases.put("DUPL", DUPLOYAN);
            aliases.put("EGYP", EGYPTIAN_HIEROGLYPHS);
            aliases.put("ELBA", ELBASAN);
            aliases.put("ELYM", ELYMAIC);
            aliases.put("ETHI", ETHIOPIC);
            aliases.put("GEOR", GEORGIAN);
            aliases.put("GLAG", GLAGOLITIC);
            aliases.put("GONM", MASARAM_GONDI);
            aliases.put("GOTH", GOTHIC);
            aliases.put("GONG", GUNJALA_GONDI);
            aliases.put("GRAN", GRANTHA);
            aliases.put("GREK", GREEK);
            aliases.put("GUJR", GUJARATI);
            aliases.put("GURU", GURMUKHI);
            aliases.put("HANG", HANGUL);
            aliases.put("HANI", HAN);
            aliases.put("HANO", HANUNOO);
            aliases.put("HATR", HATRAN);
            aliases.put("HEBR", HEBREW);
            aliases.put("HIRA", HIRAGANA);
            aliases.put("HLUW", ANATOLIAN_HIEROGLYPHS);
            aliases.put("HMNG", PAHAWH_HMONG);
            aliases.put("HMNP", NYIAKENG_PUACHUE_HMONG);
            // it appears we don't have the KATAKANA_OR_HIRAGANA
            //aliases.put("HRKT", KATAKANA_OR_HIRAGANA);
            aliases.put("HUNG", OLD_HUNGARIAN);
            aliases.put("ITAL", OLD_ITALIC);
            aliases.put("JAVA", JAVANESE);
            aliases.put("KALI", KAYAH_LI);
            aliases.put("KANA", KATAKANA);
            aliases.put("KHAR", KHAROSHTHI);
            aliases.put("KHMR", KHMER);
            aliases.put("KHOJ", KHOJKI);
            aliases.put("KITS", KHITAN_SMALL_SCRIPT);
            aliases.put("KNDA", KANNADA);
            aliases.put("KTHI", KAITHI);
            aliases.put("LANA", TAI_THAM);
            aliases.put("LAOO", LAO);
            aliases.put("LATN", LATIN);
            aliases.put("LEPC", LEPCHA);
            aliases.put("LIMB", LIMBU);
            aliases.put("LINA", LINEAR_A);
            aliases.put("LINB", LINEAR_B);
            aliases.put("LISU", LISU);
            aliases.put("LYCI", LYCIAN);
            aliases.put("LYDI", LYDIAN);
            aliases.put("MAHJ", MAHAJANI);
            aliases.put("MAKA", MAKASAR);
            aliases.put("MARC", MARCHEN);
            aliases.put("MAND", MANDAIC);
            aliases.put("MANI", MANICHAEAN);
            aliases.put("MEDF", MEDEFAIDRIN);
            aliases.put("MEND", MENDE_KIKAKUI);
            aliases.put("MERC", MEROITIC_CURSIVE);
            aliases.put("MERO", MEROITIC_HIEROGLYPHS);
            aliases.put("MLYM", MALAYALAM);
            aliases.put("MODI", MODI);
            aliases.put("MONG", MONGOLIAN);
            aliases.put("MROO", MRO);
            aliases.put("MTEI", MEETEI_MAYEK);
            aliases.put("MULT", MULTANI);
            aliases.put("MYMR", MYANMAR);
            aliases.put("NAND", NANDINAGARI);
            aliases.put("NARB", OLD_NORTH_ARABIAN);
            aliases.put("NBAT", NABATAEAN);
            aliases.put("NEWA", NEWA);
            aliases.put("NKOO", NKO);
            aliases.put("NSHU", NUSHU);
            aliases.put("OGAM", OGHAM);
            aliases.put("OLCK", OL_CHIKI);
            aliases.put("ORKH", OLD_TURKIC);
            aliases.put("ORYA", ORIYA);
            aliases.put("OSGE", OSAGE);
            aliases.put("OSMA", OSMANYA);
            aliases.put("PALM", PALMYRENE);
            aliases.put("PAUC", PAU_CIN_HAU);
            aliases.put("PERM", OLD_PERMIC);
            aliases.put("PHAG", PHAGS_PA);
            aliases.put("PHLI", INSCRIPTIONAL_PAHLAVI);
            aliases.put("PHLP", PSALTER_PAHLAVI);
            aliases.put("PHNX", PHOENICIAN);
            aliases.put("PLRD", MIAO);
            aliases.put("PRTI", INSCRIPTIONAL_PARTHIAN);
            aliases.put("RJNG", REJANG);
            aliases.put("ROHG", HANIFI_ROHINGYA);
            aliases.put("RUNR", RUNIC);
            aliases.put("SAMR", SAMARITAN);
            aliases.put("SARB", OLD_SOUTH_ARABIAN);
            aliases.put("SAUR", SAURASHTRA);
            aliases.put("SGNW", SIGNWRITING);
            aliases.put("SHAW", SHAVIAN);
            aliases.put("SHRD", SHARADA);
            aliases.put("SIDD", SIDDHAM);
            aliases.put("SIND", KHUDAWADI);
            aliases.put("SINH", SINHALA);
            aliases.put("SOGD", SOGDIAN);
            aliases.put("SOGO", OLD_SOGDIAN);
            aliases.put("SORA", SORA_SOMPENG);
            aliases.put("SOYO", SOYOMBO);
            aliases.put("SUND", SUNDANESE);
            aliases.put("SYLO", SYLOTI_NAGRI);
            aliases.put("SYRC", SYRIAC);
            aliases.put("TAGB", TAGBANWA);
            aliases.put("TAKR", TAKRI);
            aliases.put("TALE", TAI_LE);
            aliases.put("TALU", NEW_TAI_LUE);
            aliases.put("TAML", TAMIL);
            aliases.put("TANG", TANGUT);
            aliases.put("TAVT", TAI_VIET);
            aliases.put("TELU", TELUGU);
            aliases.put("TFNG", TIFINAGH);
            aliases.put("TGLG", TAGALOG);
            aliases.put("THAA", THAANA);
            aliases.put("THAI", THAI);
            aliases.put("TIBT", TIBETAN);
            aliases.put("TIRH", TIRHUTA);
            aliases.put("UGAR", UGARITIC);
            aliases.put("VAII", VAI);
            aliases.put("WARA", WARANG_CITI);
            aliases.put("WCHO", WANCHO);
            aliases.put("XPEO", OLD_PERSIAN);
            aliases.put("XSUX", CUNEIFORM);
            aliases.put("YIII", YI);
            aliases.put("YEZI", YEZIDI);
            aliases.put("ZANB", ZANABAZAR_SQUARE);
            aliases.put("ZINH", INHERITED);
            aliases.put("ZYYY", COMMON);
            aliases.put("ZZZZ", UNKNOWN);
        }

        /**
         * Returns the enum constant representing the Unicode script of which
         * the given character (Unicode code point) is assigned to.
         *
         * @param   codePoint the character (Unicode code point) in question.
         * @return  The {@code UnicodeScript} constant representing the
         *          Unicode script of which this character is assigned to.
         *
         * @throws  IllegalArgumentException if the specified
         * {@code codePoint} is an invalid Unicode code point.
         * @see Character#isValidCodePoint(int)
         *
         */
        public static UnicodeScript of(int codePoint) {
            if (!isValidCodePoint(codePoint))
                throw new IllegalArgumentException(
                    String.format("Not a valid Unicode code point: 0x%X", codePoint));
            int type = getType(codePoint);
            // leave SURROGATE and PRIVATE_USE for table lookup
            if (type == UNASSIGNED)
                return UNKNOWN;
            int index = Arrays.binarySearch(scriptStarts, codePoint);
            if (index < 0)
                index = -index - 2;
            return scripts[index];
        }

        /**
         * Returns the UnicodeScript constant with the given Unicode script
         * name or the script name alias. Script names and their aliases are
         * determined by The Unicode Standard. The files {@code Scripts<version>.txt}
         * and {@code PropertyValueAliases<version>.txt} define script names
         * and the script name aliases for a particular version of the
         * standard. The {@link Character} class specifies the version of
         * the standard that it supports.
         * <p>
         * Character case is ignored for all of the valid script names.
         * The en_US locale's case mapping rules are used to provide
         * case-insensitive string comparisons for script name validation.
         *
         * @param scriptName A {@code UnicodeScript} name.
         * @return The {@code UnicodeScript} constant identified
         *         by {@code scriptName}
         * @throws IllegalArgumentException if {@code scriptName} is an
         *         invalid name
         * @throws NullPointerException if {@code scriptName} is null
         */
        public static final UnicodeScript forName(String scriptName) {
            scriptName = scriptName.toUpperCase(Locale.ENGLISH);
                                 //.replace(' ', '_'));
            UnicodeScript sc = aliases.get(scriptName);
            if (sc != null)
                return sc;
            return valueOf(scriptName);
        }
    }

    /**
     * The value of the {@code Character}.
     *
     * @serial
     */
    private final char value;

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    @java.io.Serial
    private static final long serialVersionUID = 3786198910865385080L;

    /**
     * Constructs a newly allocated {@code Character} object that
     * represents the specified {@code char} value.
     *
     * @param  value   the value to be represented by the
     *                  {@code Character} object.
     *
     * @deprecated
     * It is rarely appropriate to use this constructor. The static factory
     * {@link #valueOf(char)} is generally a better choice, as it is
     * likely to yield significantly better space and time performance.
     */
    @Deprecated(since="9", forRemoval = true)
    public Character(char value) {
        this.value = value;
    }

    private static class CharacterCache {
        private CharacterCache(){}

        static final Character[] cache;
        static Character[] archivedCache;

        static {
            int size = 127 + 1;

            // Load and use the archived cache if it exists
            CDS.initializeFromArchive(CharacterCache.class);
            if (archivedCache == null || archivedCache.length != size) {
                Character[] c = new Character[size];
                for (int i = 0; i < size; i++) {
                    c[i] = new Character((char) i);
                }
                archivedCache = c;
            }
            cache = archivedCache;
        }
    }

    /**
     * Returns a {@code Character} instance representing the specified
     * {@code char} value.
     * If a new {@code Character} instance is not required, this method
     * should generally be used in preference to the constructor
     * {@link #Character(char)}, as this method is likely to yield
     * significantly better space and time performance by caching
     * frequently requested values.
     *
     * This method will always cache values in the range {@code
     * '\u005Cu0000'} to {@code '\u005Cu007F'}, inclusive, and may
     * cache other values outside of this range.
     *
     * @param  c a char value.
     * @return a {@code Character} instance representing {@code c}.
     * @since  1.5
     */
    @IntrinsicCandidate
    public static Character valueOf(char c) {
        if (c <= 127) { // must cache
            return CharacterCache.cache[(int)c];
        }
        return new Character(c);
    }

    /**
     * Returns the value of this {@code Character} object.
     * @return  the primitive {@code char} value represented by
     *          this object.
     */
    @IntrinsicCandidate
    public char charValue() {
        return value;
    }

    /**
     * Returns a hash code for this {@code Character}; equal to the result
     * of invoking {@code charValue()}.
     *
     * @return a hash code value for this {@code Character}
     */
    @Override
    public int hashCode() {
        return Character.hashCode(value);
    }

    /**
     * Returns a hash code for a {@code char} value; compatible with
     * {@code Character.hashCode()}.
     *
     * @since 1.8
     *
     * @param value The {@code char} for which to return a hash code.
     * @return a hash code value for a {@code char} value.
     */
    public static int hashCode(char value) {
        return (int)value;
    }

    /**
     * Compares this object against the specified object.
     * The result is {@code true} if and only if the argument is not
     * {@code null} and is a {@code Character} object that
     * represents the same {@code char} value as this object.
     *
     * @param   obj   the object to compare with.
     * @return  {@code true} if the objects are the same;
     *          {@code false} otherwise.
     */
    public boolean equals(Object obj) {
        if (obj instanceof Character) {
            return value == ((Character)obj).charValue();
        }
        return false;
    }

    /**
     * Returns a {@code String} object representing this
     * {@code Character}'s value.  The result is a string of
     * length 1 whose sole component is the primitive
     * {@code char} value represented by this
     * {@code Character} object.
     *
     * @return  a string representation of this object.
     */
    @Override
    public String toString() {
        return String.valueOf(value);
    }

    /**
     * Returns a {@code String} object representing the
     * specified {@code char}.  The result is a string of length
     * 1 consisting solely of the specified {@code char}.
     *
     * @apiNote This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #toString(int)} method.
     *
     * @param c the {@code char} to be converted
     * @return the string representation of the specified {@code char}
     * @since 1.4
     */
    public static String toString(char c) {
        return String.valueOf(c);
    }

    /**
     * Returns a {@code String} object representing the
     * specified character (Unicode code point).  The result is a string of
     * length 1 or 2, consisting solely of the specified {@code codePoint}.
     *
     * @param codePoint the {@code codePoint} to be converted
     * @return the string representation of the specified {@code codePoint}
     * @throws IllegalArgumentException if the specified
     *      {@code codePoint} is not a {@linkplain #isValidCodePoint
     *      valid Unicode code point}.
     * @since 11
     */
    public static String toString(int codePoint) {
        return String.valueOfCodePoint(codePoint);
    }

    /**
     * Determines whether the specified code point is a valid
     * <a href="http://www.unicode.org/glossary/#code_point">
     * Unicode code point value</a>.
     *
     * @param  codePoint the Unicode code point to be tested
     * @return {@code true} if the specified code point value is between
     *         {@link #MIN_CODE_POINT} and
     *         {@link #MAX_CODE_POINT} inclusive;
     *         {@code false} otherwise.
     * @since  1.5
     */
    public static boolean isValidCodePoint(int codePoint) {
        // Optimized form of:
        //     codePoint >= MIN_CODE_POINT && codePoint <= MAX_CODE_POINT
        int plane = codePoint >>> 16;
        return plane < ((MAX_CODE_POINT + 1) >>> 16);
    }

    /**
     * Determines whether the specified character (Unicode code point)
     * is in the <a href="#BMP">Basic Multilingual Plane (BMP)</a>.
     * Such code points can be represented using a single {@code char}.
     *
     * @param  codePoint the character (Unicode code point) to be tested
     * @return {@code true} if the specified code point is between
     *         {@link #MIN_VALUE} and {@link #MAX_VALUE} inclusive;
     *         {@code false} otherwise.
     * @since  1.7
     */
    public static boolean isBmpCodePoint(int codePoint) {
        return codePoint >>> 16 == 0;
        // Optimized form of:
        //     codePoint >= MIN_VALUE && codePoint <= MAX_VALUE
        // We consistently use logical shift (>>>) to facilitate
        // additional runtime optimizations.
    }

    /**
     * Determines whether the specified character (Unicode code point)
     * is in the <a href="#supplementary">supplementary character</a> range.
     *
     * @param  codePoint the character (Unicode code point) to be tested
     * @return {@code true} if the specified code point is between
     *         {@link #MIN_SUPPLEMENTARY_CODE_POINT} and
     *         {@link #MAX_CODE_POINT} inclusive;
     *         {@code false} otherwise.
     * @since  1.5
     */
    public static boolean isSupplementaryCodePoint(int codePoint) {
        return codePoint >= MIN_SUPPLEMENTARY_CODE_POINT
            && codePoint <  MAX_CODE_POINT + 1;
    }

    /**
     * Determines if the given {@code char} value is a
     * <a href="http://www.unicode.org/glossary/#high_surrogate_code_unit">
     * Unicode high-surrogate code unit</a>
     * (also known as <i>leading-surrogate code unit</i>).
     *
     * <p>Such values do not represent characters by themselves,
     * but are used in the representation of
     * <a href="#supplementary">supplementary characters</a>
     * in the UTF-16 encoding.
     *
     * @param  ch the {@code char} value to be tested.
     * @return {@code true} if the {@code char} value is between
     *         {@link #MIN_HIGH_SURROGATE} and
     *         {@link #MAX_HIGH_SURROGATE} inclusive;
     *         {@code false} otherwise.
     * @see    Character#isLowSurrogate(char)
     * @see    Character.UnicodeBlock#of(int)
     * @since  1.5
     */
    public static boolean isHighSurrogate(char ch) {
        // Help VM constant-fold; MAX_HIGH_SURROGATE + 1 == MIN_LOW_SURROGATE
        return ch >= MIN_HIGH_SURROGATE && ch < (MAX_HIGH_SURROGATE + 1);
    }

    /**
     * Determines if the given {@code char} value is a
     * <a href="http://www.unicode.org/glossary/#low_surrogate_code_unit">
     * Unicode low-surrogate code unit</a>
     * (also known as <i>trailing-surrogate code unit</i>).
     *
     * <p>Such values do not represent characters by themselves,
     * but are used in the representation of
     * <a href="#supplementary">supplementary characters</a>
     * in the UTF-16 encoding.
     *
     * @param  ch the {@code char} value to be tested.
     * @return {@code true} if the {@code char} value is between
     *         {@link #MIN_LOW_SURROGATE} and
     *         {@link #MAX_LOW_SURROGATE} inclusive;
     *         {@code false} otherwise.
     * @see    Character#isHighSurrogate(char)
     * @since  1.5
     */
    public static boolean isLowSurrogate(char ch) {
        return ch >= MIN_LOW_SURROGATE && ch < (MAX_LOW_SURROGATE + 1);
    }

    /**
     * Determines if the given {@code char} value is a Unicode
     * <i>surrogate code unit</i>.
     *
     * <p>Such values do not represent characters by themselves,
     * but are used in the representation of
     * <a href="#supplementary">supplementary characters</a>
     * in the UTF-16 encoding.
     *
     * <p>A char value is a surrogate code unit if and only if it is either
     * a {@linkplain #isLowSurrogate(char) low-surrogate code unit} or
     * a {@linkplain #isHighSurrogate(char) high-surrogate code unit}.
     *
     * @param  ch the {@code char} value to be tested.
     * @return {@code true} if the {@code char} value is between
     *         {@link #MIN_SURROGATE} and
     *         {@link #MAX_SURROGATE} inclusive;
     *         {@code false} otherwise.
     * @since  1.7
     */
    public static boolean isSurrogate(char ch) {
        return ch >= MIN_SURROGATE && ch < (MAX_SURROGATE + 1);
    }

    /**
     * Determines whether the specified pair of {@code char}
     * values is a valid
     * <a href="http://www.unicode.org/glossary/#surrogate_pair">
     * Unicode surrogate pair</a>.
     *
     * <p>This method is equivalent to the expression:
     * <blockquote><pre>{@code
     * isHighSurrogate(high) && isLowSurrogate(low)
     * }</pre></blockquote>
     *
     * @param  high the high-surrogate code value to be tested
     * @param  low the low-surrogate code value to be tested
     * @return {@code true} if the specified high and
     * low-surrogate code values represent a valid surrogate pair;
     * {@code false} otherwise.
     * @since  1.5
     */
    public static boolean isSurrogatePair(char high, char low) {
        return isHighSurrogate(high) && isLowSurrogate(low);
    }

    /**
     * Determines the number of {@code char} values needed to
     * represent the specified character (Unicode code point). If the
     * specified character is equal to or greater than 0x10000, then
     * the method returns 2. Otherwise, the method returns 1.
     *
     * <p>This method doesn't validate the specified character to be a
     * valid Unicode code point. The caller must validate the
     * character value using {@link #isValidCodePoint(int) isValidCodePoint}
     * if necessary.
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  2 if the character is a valid supplementary character; 1 otherwise.
     * @see     Character#isSupplementaryCodePoint(int)
     * @since   1.5
     */
    public static int charCount(int codePoint) {
        return codePoint >= MIN_SUPPLEMENTARY_CODE_POINT ? 2 : 1;
    }

    /**
     * Converts the specified surrogate pair to its supplementary code
     * point value. This method does not validate the specified
     * surrogate pair. The caller must validate it using {@link
     * #isSurrogatePair(char, char) isSurrogatePair} if necessary.
     *
     * @param  high the high-surrogate code unit
     * @param  low the low-surrogate code unit
     * @return the supplementary code point composed from the
     *         specified surrogate pair.
     * @since  1.5
     */
    public static int toCodePoint(char high, char low) {
        // Optimized form of:
        // return ((high - MIN_HIGH_SURROGATE) << 10)
        //         + (low - MIN_LOW_SURROGATE)
        //         + MIN_SUPPLEMENTARY_CODE_POINT;
        return ((high << 10) + low) + (MIN_SUPPLEMENTARY_CODE_POINT
                                       - (MIN_HIGH_SURROGATE << 10)
                                       - MIN_LOW_SURROGATE);
    }

    /**
     * Returns the code point at the given index of the
     * {@code CharSequence}. If the {@code char} value at
     * the given index in the {@code CharSequence} is in the
     * high-surrogate range, the following index is less than the
     * length of the {@code CharSequence}, and the
     * {@code char} value at the following index is in the
     * low-surrogate range, then the supplementary code point
     * corresponding to this surrogate pair is returned. Otherwise,
     * the {@code char} value at the given index is returned.
     *
     * @param seq a sequence of {@code char} values (Unicode code
     * units)
     * @param index the index to the {@code char} values (Unicode
     * code units) in {@code seq} to be converted
     * @return the Unicode code point at the given index
     * @throws NullPointerException if {@code seq} is null.
     * @throws IndexOutOfBoundsException if the value
     * {@code index} is negative or not less than
     * {@link CharSequence#length() seq.length()}.
     * @since  1.5
     */
    public static int codePointAt(CharSequence seq, int index) {
        char c1 = seq.charAt(index);
        if (isHighSurrogate(c1) && ++index < seq.length()) {
            char c2 = seq.charAt(index);
            if (isLowSurrogate(c2)) {
                return toCodePoint(c1, c2);
            }
        }
        return c1;
    }

    /**
     * Returns the code point at the given index of the
     * {@code char} array. If the {@code char} value at
     * the given index in the {@code char} array is in the
     * high-surrogate range, the following index is less than the
     * length of the {@code char} array, and the
     * {@code char} value at the following index is in the
     * low-surrogate range, then the supplementary code point
     * corresponding to this surrogate pair is returned. Otherwise,
     * the {@code char} value at the given index is returned.
     *
     * @param a the {@code char} array
     * @param index the index to the {@code char} values (Unicode
     * code units) in the {@code char} array to be converted
     * @return the Unicode code point at the given index
     * @throws NullPointerException if {@code a} is null.
     * @throws IndexOutOfBoundsException if the value
     * {@code index} is negative or not less than
     * the length of the {@code char} array.
     * @since  1.5
     */
    public static int codePointAt(char[] a, int index) {
        return codePointAtImpl(a, index, a.length);
    }

    /**
     * Returns the code point at the given index of the
     * {@code char} array, where only array elements with
     * {@code index} less than {@code limit} can be used. If
     * the {@code char} value at the given index in the
     * {@code char} array is in the high-surrogate range, the
     * following index is less than the {@code limit}, and the
     * {@code char} value at the following index is in the
     * low-surrogate range, then the supplementary code point
     * corresponding to this surrogate pair is returned. Otherwise,
     * the {@code char} value at the given index is returned.
     *
     * @param a the {@code char} array
     * @param index the index to the {@code char} values (Unicode
     * code units) in the {@code char} array to be converted
     * @param limit the index after the last array element that
     * can be used in the {@code char} array
     * @return the Unicode code point at the given index
     * @throws NullPointerException if {@code a} is null.
     * @throws IndexOutOfBoundsException if the {@code index}
     * argument is negative or not less than the {@code limit}
     * argument, or if the {@code limit} argument is negative or
     * greater than the length of the {@code char} array.
     * @since  1.5
     */
    public static int codePointAt(char[] a, int index, int limit) {
        if (index >= limit || limit < 0 || limit > a.length) {
            throw new IndexOutOfBoundsException();
        }
        return codePointAtImpl(a, index, limit);
    }

    // throws ArrayIndexOutOfBoundsException if index out of bounds
    static int codePointAtImpl(char[] a, int index, int limit) {
        char c1 = a[index];
        if (isHighSurrogate(c1) && ++index < limit) {
            char c2 = a[index];
            if (isLowSurrogate(c2)) {
                return toCodePoint(c1, c2);
            }
        }
        return c1;
    }

    /**
     * Returns the code point preceding the given index of the
     * {@code CharSequence}. If the {@code char} value at
     * {@code (index - 1)} in the {@code CharSequence} is in
     * the low-surrogate range, {@code (index - 2)} is not
     * negative, and the {@code char} value at {@code (index - 2)}
     * in the {@code CharSequence} is in the
     * high-surrogate range, then the supplementary code point
     * corresponding to this surrogate pair is returned. Otherwise,
     * the {@code char} value at {@code (index - 1)} is
     * returned.
     *
     * @param seq the {@code CharSequence} instance
     * @param index the index following the code point that should be returned
     * @return the Unicode code point value before the given index.
     * @throws NullPointerException if {@code seq} is null.
     * @throws IndexOutOfBoundsException if the {@code index}
     * argument is less than 1 or greater than {@link
     * CharSequence#length() seq.length()}.
     * @since  1.5
     */
    public static int codePointBefore(CharSequence seq, int index) {
        char c2 = seq.charAt(--index);
        if (isLowSurrogate(c2) && index > 0) {
            char c1 = seq.charAt(--index);
            if (isHighSurrogate(c1)) {
                return toCodePoint(c1, c2);
            }
        }
        return c2;
    }

    /**
     * Returns the code point preceding the given index of the
     * {@code char} array. If the {@code char} value at
     * {@code (index - 1)} in the {@code char} array is in
     * the low-surrogate range, {@code (index - 2)} is not
     * negative, and the {@code char} value at {@code (index - 2)}
     * in the {@code char} array is in the
     * high-surrogate range, then the supplementary code point
     * corresponding to this surrogate pair is returned. Otherwise,
     * the {@code char} value at {@code (index - 1)} is
     * returned.
     *
     * @param a the {@code char} array
     * @param index the index following the code point that should be returned
     * @return the Unicode code point value before the given index.
     * @throws NullPointerException if {@code a} is null.
     * @throws IndexOutOfBoundsException if the {@code index}
     * argument is less than 1 or greater than the length of the
     * {@code char} array
     * @since  1.5
     */
    public static int codePointBefore(char[] a, int index) {
        return codePointBeforeImpl(a, index, 0);
    }

    /**
     * Returns the code point preceding the given index of the
     * {@code char} array, where only array elements with
     * {@code index} greater than or equal to {@code start}
     * can be used. If the {@code char} value at {@code (index - 1)}
     * in the {@code char} array is in the
     * low-surrogate range, {@code (index - 2)} is not less than
     * {@code start}, and the {@code char} value at
     * {@code (index - 2)} in the {@code char} array is in
     * the high-surrogate range, then the supplementary code point
     * corresponding to this surrogate pair is returned. Otherwise,
     * the {@code char} value at {@code (index - 1)} is
     * returned.
     *
     * @param a the {@code char} array
     * @param index the index following the code point that should be returned
     * @param start the index of the first array element in the
     * {@code char} array
     * @return the Unicode code point value before the given index.
     * @throws NullPointerException if {@code a} is null.
     * @throws IndexOutOfBoundsException if the {@code index}
     * argument is not greater than the {@code start} argument or
     * is greater than the length of the {@code char} array, or
     * if the {@code start} argument is negative or not less than
     * the length of the {@code char} array.
     * @since  1.5
     */
    public static int codePointBefore(char[] a, int index, int start) {
        if (index <= start || start < 0 || start >= a.length) {
            throw new IndexOutOfBoundsException();
        }
        return codePointBeforeImpl(a, index, start);
    }

    // throws ArrayIndexOutOfBoundsException if index-1 out of bounds
    static int codePointBeforeImpl(char[] a, int index, int start) {
        char c2 = a[--index];
        if (isLowSurrogate(c2) && index > start) {
            char c1 = a[--index];
            if (isHighSurrogate(c1)) {
                return toCodePoint(c1, c2);
            }
        }
        return c2;
    }

    /**
     * Returns the leading surrogate (a
     * <a href="http://www.unicode.org/glossary/#high_surrogate_code_unit">
     * high surrogate code unit</a>) of the
     * <a href="http://www.unicode.org/glossary/#surrogate_pair">
     * surrogate pair</a>
     * representing the specified supplementary character (Unicode
     * code point) in the UTF-16 encoding.  If the specified character
     * is not a
     * <a href="Character.html#supplementary">supplementary character</a>,
     * an unspecified {@code char} is returned.
     *
     * <p>If
     * {@link #isSupplementaryCodePoint isSupplementaryCodePoint(x)}
     * is {@code true}, then
     * {@link #isHighSurrogate isHighSurrogate}{@code (highSurrogate(x))} and
     * {@link #toCodePoint toCodePoint}{@code (highSurrogate(x), }{@link #lowSurrogate lowSurrogate}{@code (x)) == x}
     * are also always {@code true}.
     *
     * @param   codePoint a supplementary character (Unicode code point)
     * @return  the leading surrogate code unit used to represent the
     *          character in the UTF-16 encoding
     * @since   1.7
     */
    public static char highSurrogate(int codePoint) {
        return (char) ((codePoint >>> 10)
            + (MIN_HIGH_SURROGATE - (MIN_SUPPLEMENTARY_CODE_POINT >>> 10)));
    }

    /**
     * Returns the trailing surrogate (a
     * <a href="http://www.unicode.org/glossary/#low_surrogate_code_unit">
     * low surrogate code unit</a>) of the
     * <a href="http://www.unicode.org/glossary/#surrogate_pair">
     * surrogate pair</a>
     * representing the specified supplementary character (Unicode
     * code point) in the UTF-16 encoding.  If the specified character
     * is not a
     * <a href="Character.html#supplementary">supplementary character</a>,
     * an unspecified {@code char} is returned.
     *
     * <p>If
     * {@link #isSupplementaryCodePoint isSupplementaryCodePoint(x)}
     * is {@code true}, then
     * {@link #isLowSurrogate isLowSurrogate}{@code (lowSurrogate(x))} and
     * {@link #toCodePoint toCodePoint}{@code (}{@link #highSurrogate highSurrogate}{@code (x), lowSurrogate(x)) == x}
     * are also always {@code true}.
     *
     * @param   codePoint a supplementary character (Unicode code point)
     * @return  the trailing surrogate code unit used to represent the
     *          character in the UTF-16 encoding
     * @since   1.7
     */
    public static char lowSurrogate(int codePoint) {
        return (char) ((codePoint & 0x3ff) + MIN_LOW_SURROGATE);
    }

    /**
     * Converts the specified character (Unicode code point) to its
     * UTF-16 representation. If the specified code point is a BMP
     * (Basic Multilingual Plane or Plane 0) value, the same value is
     * stored in {@code dst[dstIndex]}, and 1 is returned. If the
     * specified code point is a supplementary character, its
     * surrogate values are stored in {@code dst[dstIndex]}
     * (high-surrogate) and {@code dst[dstIndex+1]}
     * (low-surrogate), and 2 is returned.
     *
     * @param  codePoint the character (Unicode code point) to be converted.
     * @param  dst an array of {@code char} in which the
     * {@code codePoint}'s UTF-16 value is stored.
     * @param dstIndex the start index into the {@code dst}
     * array where the converted value is stored.
     * @return 1 if the code point is a BMP code point, 2 if the
     * code point is a supplementary code point.
     * @throws IllegalArgumentException if the specified
     * {@code codePoint} is not a valid Unicode code point.
     * @throws NullPointerException if the specified {@code dst} is null.
     * @throws IndexOutOfBoundsException if {@code dstIndex}
     * is negative or not less than {@code dst.length}, or if
     * {@code dst} at {@code dstIndex} doesn't have enough
     * array element(s) to store the resulting {@code char}
     * value(s). (If {@code dstIndex} is equal to
     * {@code dst.length-1} and the specified
     * {@code codePoint} is a supplementary character, the
     * high-surrogate value is not stored in
     * {@code dst[dstIndex]}.)
     * @since  1.5
     */
    public static int toChars(int codePoint, char[] dst, int dstIndex) {
        if (isBmpCodePoint(codePoint)) {
            dst[dstIndex] = (char) codePoint;
            return 1;
        } else if (isValidCodePoint(codePoint)) {
            toSurrogates(codePoint, dst, dstIndex);
            return 2;
        } else {
            throw new IllegalArgumentException(
                String.format("Not a valid Unicode code point: 0x%X", codePoint));
        }
    }

    /**
     * Converts the specified character (Unicode code point) to its
     * UTF-16 representation stored in a {@code char} array. If
     * the specified code point is a BMP (Basic Multilingual Plane or
     * Plane 0) value, the resulting {@code char} array has
     * the same value as {@code codePoint}. If the specified code
     * point is a supplementary code point, the resulting
     * {@code char} array has the corresponding surrogate pair.
     *
     * @param  codePoint a Unicode code point
     * @return a {@code char} array having
     *         {@code codePoint}'s UTF-16 representation.
     * @throws IllegalArgumentException if the specified
     * {@code codePoint} is not a valid Unicode code point.
     * @since  1.5
     */
    public static char[] toChars(int codePoint) {
        if (isBmpCodePoint(codePoint)) {
            return new char[] { (char) codePoint };
        } else if (isValidCodePoint(codePoint)) {
            char[] result = new char[2];
            toSurrogates(codePoint, result, 0);
            return result;
        } else {
            throw new IllegalArgumentException(
                String.format("Not a valid Unicode code point: 0x%X", codePoint));
        }
    }

    static void toSurrogates(int codePoint, char[] dst, int index) {
        // We write elements "backwards" to guarantee all-or-nothing
        dst[index+1] = lowSurrogate(codePoint);
        dst[index] = highSurrogate(codePoint);
    }

    /**
     * Returns the number of Unicode code points in the text range of
     * the specified char sequence. The text range begins at the
     * specified {@code beginIndex} and extends to the
     * {@code char} at index {@code endIndex - 1}. Thus the
     * length (in {@code char}s) of the text range is
     * {@code endIndex-beginIndex}. Unpaired surrogates within
     * the text range count as one code point each.
     *
     * @param seq the char sequence
     * @param beginIndex the index to the first {@code char} of
     * the text range.
     * @param endIndex the index after the last {@code char} of
     * the text range.
     * @return the number of Unicode code points in the specified text
     * range
     * @throws NullPointerException if {@code seq} is null.
     * @throws IndexOutOfBoundsException if the
     * {@code beginIndex} is negative, or {@code endIndex}
     * is larger than the length of the given sequence, or
     * {@code beginIndex} is larger than {@code endIndex}.
     * @since  1.5
     */
    public static int codePointCount(CharSequence seq, int beginIndex, int endIndex) {
        Objects.checkFromToIndex(beginIndex, endIndex, seq.length());
        int n = endIndex - beginIndex;
        for (int i = beginIndex; i < endIndex; ) {
            if (isHighSurrogate(seq.charAt(i++)) && i < endIndex &&
                isLowSurrogate(seq.charAt(i))) {
                n--;
                i++;
            }
        }
        return n;
    }

    /**
     * Returns the number of Unicode code points in a subarray of the
     * {@code char} array argument. The {@code offset}
     * argument is the index of the first {@code char} of the
     * subarray and the {@code count} argument specifies the
     * length of the subarray in {@code char}s. Unpaired
     * surrogates within the subarray count as one code point each.
     *
     * @param a the {@code char} array
     * @param offset the index of the first {@code char} in the
     * given {@code char} array
     * @param count the length of the subarray in {@code char}s
     * @return the number of Unicode code points in the specified subarray
     * @throws NullPointerException if {@code a} is null.
     * @throws IndexOutOfBoundsException if {@code offset} or
     * {@code count} is negative, or if {@code offset +
     * count} is larger than the length of the given array.
     * @since  1.5
     */
    public static int codePointCount(char[] a, int offset, int count) {
        Objects.checkFromIndexSize(count, offset, a.length);
        return codePointCountImpl(a, offset, count);
    }

    static int codePointCountImpl(char[] a, int offset, int count) {
        int endIndex = offset + count;
        int n = count;
        for (int i = offset; i < endIndex; ) {
            if (isHighSurrogate(a[i++]) && i < endIndex &&
                isLowSurrogate(a[i])) {
                n--;
                i++;
            }
        }
        return n;
    }

    /**
     * Returns the index within the given char sequence that is offset
     * from the given {@code index} by {@code codePointOffset}
     * code points. Unpaired surrogates within the text range given by
     * {@code index} and {@code codePointOffset} count as
     * one code point each.
     *
     * @param seq the char sequence
     * @param index the index to be offset
     * @param codePointOffset the offset in code points
     * @return the index within the char sequence
     * @throws NullPointerException if {@code seq} is null.
     * @throws IndexOutOfBoundsException if {@code index}
     *   is negative or larger then the length of the char sequence,
     *   or if {@code codePointOffset} is positive and the
     *   subsequence starting with {@code index} has fewer than
     *   {@code codePointOffset} code points, or if
     *   {@code codePointOffset} is negative and the subsequence
     *   before {@code index} has fewer than the absolute value
     *   of {@code codePointOffset} code points.
     * @since 1.5
     */
    public static int offsetByCodePoints(CharSequence seq, int index,
                                         int codePointOffset) {
        int length = seq.length();
        if (index < 0 || index > length) {
            throw new IndexOutOfBoundsException();
        }

        int x = index;
        if (codePointOffset >= 0) {
            int i;
            for (i = 0; x < length && i < codePointOffset; i++) {
                if (isHighSurrogate(seq.charAt(x++)) && x < length &&
                    isLowSurrogate(seq.charAt(x))) {
                    x++;
                }
            }
            if (i < codePointOffset) {
                throw new IndexOutOfBoundsException();
            }
        } else {
            int i;
            for (i = codePointOffset; x > 0 && i < 0; i++) {
                if (isLowSurrogate(seq.charAt(--x)) && x > 0 &&
                    isHighSurrogate(seq.charAt(x-1))) {
                    x--;
                }
            }
            if (i < 0) {
                throw new IndexOutOfBoundsException();
            }
        }
        return x;
    }

    /**
     * Returns the index within the given {@code char} subarray
     * that is offset from the given {@code index} by
     * {@code codePointOffset} code points. The
     * {@code start} and {@code count} arguments specify a
     * subarray of the {@code char} array. Unpaired surrogates
     * within the text range given by {@code index} and
     * {@code codePointOffset} count as one code point each.
     *
     * @param a the {@code char} array
     * @param start the index of the first {@code char} of the
     * subarray
     * @param count the length of the subarray in {@code char}s
     * @param index the index to be offset
     * @param codePointOffset the offset in code points
     * @return the index within the subarray
     * @throws NullPointerException if {@code a} is null.
     * @throws IndexOutOfBoundsException
     *   if {@code start} or {@code count} is negative,
     *   or if {@code start + count} is larger than the length of
     *   the given array,
     *   or if {@code index} is less than {@code start} or
     *   larger then {@code start + count},
     *   or if {@code codePointOffset} is positive and the text range
     *   starting with {@code index} and ending with {@code start + count - 1}
     *   has fewer than {@code codePointOffset} code
     *   points,
     *   or if {@code codePointOffset} is negative and the text range
     *   starting with {@code start} and ending with {@code index - 1}
     *   has fewer than the absolute value of
     *   {@code codePointOffset} code points.
     * @since 1.5
     */
    public static int offsetByCodePoints(char[] a, int start, int count,
                                         int index, int codePointOffset) {
        if (count > a.length-start || start < 0 || count < 0
            || index < start || index > start+count) {
            throw new IndexOutOfBoundsException();
        }
        return offsetByCodePointsImpl(a, start, count, index, codePointOffset);
    }

    static int offsetByCodePointsImpl(char[]a, int start, int count,
                                      int index, int codePointOffset) {
        int x = index;
        if (codePointOffset >= 0) {
            int limit = start + count;
            int i;
            for (i = 0; x < limit && i < codePointOffset; i++) {
                if (isHighSurrogate(a[x++]) && x < limit &&
                    isLowSurrogate(a[x])) {
                    x++;
                }
            }
            if (i < codePointOffset) {
                throw new IndexOutOfBoundsException();
            }
        } else {
            int i;
            for (i = codePointOffset; x > start && i < 0; i++) {
                if (isLowSurrogate(a[--x]) && x > start &&
                    isHighSurrogate(a[x-1])) {
                    x--;
                }
            }
            if (i < 0) {
                throw new IndexOutOfBoundsException();
            }
        }
        return x;
    }

    /**
     * Determines if the specified character is a lowercase character.
     * <p>
     * A character is lowercase if its general category type, provided
     * by {@code Character.getType(ch)}, is
     * {@code LOWERCASE_LETTER}, or it has contributory property
     * Other_Lowercase as defined by the Unicode Standard.
     * <p>
     * The following are examples of lowercase characters:
     * <blockquote><pre>
     * a b c d e f g h i j k l m n o p q r s t u v w x y z
     * '&#92;u00DF' '&#92;u00E0' '&#92;u00E1' '&#92;u00E2' '&#92;u00E3' '&#92;u00E4' '&#92;u00E5' '&#92;u00E6'
     * '&#92;u00E7' '&#92;u00E8' '&#92;u00E9' '&#92;u00EA' '&#92;u00EB' '&#92;u00EC' '&#92;u00ED' '&#92;u00EE'
     * '&#92;u00EF' '&#92;u00F0' '&#92;u00F1' '&#92;u00F2' '&#92;u00F3' '&#92;u00F4' '&#92;u00F5' '&#92;u00F6'
     * '&#92;u00F8' '&#92;u00F9' '&#92;u00FA' '&#92;u00FB' '&#92;u00FC' '&#92;u00FD' '&#92;u00FE' '&#92;u00FF'
     * </pre></blockquote>
     * <p> Many other Unicode characters are lowercase too.
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #isLowerCase(int)} method.
     *
     * @param   ch   the character to be tested.
     * @return  {@code true} if the character is lowercase;
     *          {@code false} otherwise.
     * @see     Character#isLowerCase(char)
     * @see     Character#isTitleCase(char)
     * @see     Character#toLowerCase(char)
     * @see     Character#getType(char)
     */
    public static boolean isLowerCase(char ch) {
        return isLowerCase((int)ch);
    }

    /**
     * Determines if the specified character (Unicode code point) is a
     * lowercase character.
     * <p>
     * A character is lowercase if its general category type, provided
     * by {@link Character#getType getType(codePoint)}, is
     * {@code LOWERCASE_LETTER}, or it has contributory property
     * Other_Lowercase as defined by the Unicode Standard.
     * <p>
     * The following are examples of lowercase characters:
     * <blockquote><pre>
     * a b c d e f g h i j k l m n o p q r s t u v w x y z
     * '&#92;u00DF' '&#92;u00E0' '&#92;u00E1' '&#92;u00E2' '&#92;u00E3' '&#92;u00E4' '&#92;u00E5' '&#92;u00E6'
     * '&#92;u00E7' '&#92;u00E8' '&#92;u00E9' '&#92;u00EA' '&#92;u00EB' '&#92;u00EC' '&#92;u00ED' '&#92;u00EE'
     * '&#92;u00EF' '&#92;u00F0' '&#92;u00F1' '&#92;u00F2' '&#92;u00F3' '&#92;u00F4' '&#92;u00F5' '&#92;u00F6'
     * '&#92;u00F8' '&#92;u00F9' '&#92;u00FA' '&#92;u00FB' '&#92;u00FC' '&#92;u00FD' '&#92;u00FE' '&#92;u00FF'
     * </pre></blockquote>
     * <p> Many other Unicode characters are lowercase too.
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  {@code true} if the character is lowercase;
     *          {@code false} otherwise.
     * @see     Character#isLowerCase(int)
     * @see     Character#isTitleCase(int)
     * @see     Character#toLowerCase(int)
     * @see     Character#getType(int)
     * @since   1.5
     */
    public static boolean isLowerCase(int codePoint) {
        return CharacterData.of(codePoint).isLowerCase(codePoint);
    }

    /**
     * Determines if the specified character is an uppercase character.
     * <p>
     * A character is uppercase if its general category type, provided by
     * {@code Character.getType(ch)}, is {@code UPPERCASE_LETTER}.
     * or it has contributory property Other_Uppercase as defined by the Unicode Standard.
     * <p>
     * The following are examples of uppercase characters:
     * <blockquote><pre>
     * A B C D E F G H I J K L M N O P Q R S T U V W X Y Z
     * '&#92;u00C0' '&#92;u00C1' '&#92;u00C2' '&#92;u00C3' '&#92;u00C4' '&#92;u00C5' '&#92;u00C6' '&#92;u00C7'
     * '&#92;u00C8' '&#92;u00C9' '&#92;u00CA' '&#92;u00CB' '&#92;u00CC' '&#92;u00CD' '&#92;u00CE' '&#92;u00CF'
     * '&#92;u00D0' '&#92;u00D1' '&#92;u00D2' '&#92;u00D3' '&#92;u00D4' '&#92;u00D5' '&#92;u00D6' '&#92;u00D8'
     * '&#92;u00D9' '&#92;u00DA' '&#92;u00DB' '&#92;u00DC' '&#92;u00DD' '&#92;u00DE'
     * </pre></blockquote>
     * <p> Many other Unicode characters are uppercase too.
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #isUpperCase(int)} method.
     *
     * @param   ch   the character to be tested.
     * @return  {@code true} if the character is uppercase;
     *          {@code false} otherwise.
     * @see     Character#isLowerCase(char)
     * @see     Character#isTitleCase(char)
     * @see     Character#toUpperCase(char)
     * @see     Character#getType(char)
     * @since   1.0
     */
    public static boolean isUpperCase(char ch) {
        return isUpperCase((int)ch);
    }

    /**
     * Determines if the specified character (Unicode code point) is an uppercase character.
     * <p>
     * A character is uppercase if its general category type, provided by
     * {@link Character#getType(int) getType(codePoint)}, is {@code UPPERCASE_LETTER},
     * or it has contributory property Other_Uppercase as defined by the Unicode Standard.
     * <p>
     * The following are examples of uppercase characters:
     * <blockquote><pre>
     * A B C D E F G H I J K L M N O P Q R S T U V W X Y Z
     * '&#92;u00C0' '&#92;u00C1' '&#92;u00C2' '&#92;u00C3' '&#92;u00C4' '&#92;u00C5' '&#92;u00C6' '&#92;u00C7'
     * '&#92;u00C8' '&#92;u00C9' '&#92;u00CA' '&#92;u00CB' '&#92;u00CC' '&#92;u00CD' '&#92;u00CE' '&#92;u00CF'
     * '&#92;u00D0' '&#92;u00D1' '&#92;u00D2' '&#92;u00D3' '&#92;u00D4' '&#92;u00D5' '&#92;u00D6' '&#92;u00D8'
     * '&#92;u00D9' '&#92;u00DA' '&#92;u00DB' '&#92;u00DC' '&#92;u00DD' '&#92;u00DE'
     * </pre></blockquote>
     * <p> Many other Unicode characters are uppercase too.
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  {@code true} if the character is uppercase;
     *          {@code false} otherwise.
     * @see     Character#isLowerCase(int)
     * @see     Character#isTitleCase(int)
     * @see     Character#toUpperCase(int)
     * @see     Character#getType(int)
     * @since   1.5
     */
    public static boolean isUpperCase(int codePoint) {
        return CharacterData.of(codePoint).isUpperCase(codePoint);
    }

    /**
     * Determines if the specified character is a titlecase character.
     * <p>
     * A character is a titlecase character if its general
     * category type, provided by {@code Character.getType(ch)},
     * is {@code TITLECASE_LETTER}.
     * <p>
     * Some characters look like pairs of Latin letters. For example, there
     * is an uppercase letter that looks like "LJ" and has a corresponding
     * lowercase letter that looks like "lj". A third form, which looks like "Lj",
     * is the appropriate form to use when rendering a word in lowercase
     * with initial capitals, as for a book title.
     * <p>
     * These are some of the Unicode characters for which this method returns
     * {@code true}:
     * <ul>
     * <li>{@code LATIN CAPITAL LETTER D WITH SMALL LETTER Z WITH CARON}
     * <li>{@code LATIN CAPITAL LETTER L WITH SMALL LETTER J}
     * <li>{@code LATIN CAPITAL LETTER N WITH SMALL LETTER J}
     * <li>{@code LATIN CAPITAL LETTER D WITH SMALL LETTER Z}
     * </ul>
     * <p> Many other Unicode characters are titlecase too.
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #isTitleCase(int)} method.
     *
     * @param   ch   the character to be tested.
     * @return  {@code true} if the character is titlecase;
     *          {@code false} otherwise.
     * @see     Character#isLowerCase(char)
     * @see     Character#isUpperCase(char)
     * @see     Character#toTitleCase(char)
     * @see     Character#getType(char)
     * @since   1.0.2
     */
    public static boolean isTitleCase(char ch) {
        return isTitleCase((int)ch);
    }

    /**
     * Determines if the specified character (Unicode code point) is a titlecase character.
     * <p>
     * A character is a titlecase character if its general
     * category type, provided by {@link Character#getType(int) getType(codePoint)},
     * is {@code TITLECASE_LETTER}.
     * <p>
     * Some characters look like pairs of Latin letters. For example, there
     * is an uppercase letter that looks like "LJ" and has a corresponding
     * lowercase letter that looks like "lj". A third form, which looks like "Lj",
     * is the appropriate form to use when rendering a word in lowercase
     * with initial capitals, as for a book title.
     * <p>
     * These are some of the Unicode characters for which this method returns
     * {@code true}:
     * <ul>
     * <li>{@code LATIN CAPITAL LETTER D WITH SMALL LETTER Z WITH CARON}
     * <li>{@code LATIN CAPITAL LETTER L WITH SMALL LETTER J}
     * <li>{@code LATIN CAPITAL LETTER N WITH SMALL LETTER J}
     * <li>{@code LATIN CAPITAL LETTER D WITH SMALL LETTER Z}
     * </ul>
     * <p> Many other Unicode characters are titlecase too.
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  {@code true} if the character is titlecase;
     *          {@code false} otherwise.
     * @see     Character#isLowerCase(int)
     * @see     Character#isUpperCase(int)
     * @see     Character#toTitleCase(int)
     * @see     Character#getType(int)
     * @since   1.5
     */
    public static boolean isTitleCase(int codePoint) {
        return getType(codePoint) == Character.TITLECASE_LETTER;
    }

    /**
     * Determines if the specified character is a digit.
     * <p>
     * A character is a digit if its general category type, provided
     * by {@code Character.getType(ch)}, is
     * {@code DECIMAL_DIGIT_NUMBER}.
     * <p>
     * Some Unicode character ranges that contain digits:
     * <ul>
     * <li>{@code '\u005Cu0030'} through {@code '\u005Cu0039'},
     *     ISO-LATIN-1 digits ({@code '0'} through {@code '9'})
     * <li>{@code '\u005Cu0660'} through {@code '\u005Cu0669'},
     *     Arabic-Indic digits
     * <li>{@code '\u005Cu06F0'} through {@code '\u005Cu06F9'},
     *     Extended Arabic-Indic digits
     * <li>{@code '\u005Cu0966'} through {@code '\u005Cu096F'},
     *     Devanagari digits
     * <li>{@code '\u005CuFF10'} through {@code '\u005CuFF19'},
     *     Fullwidth digits
     * </ul>
     *
     * Many other character ranges contain digits as well.
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #isDigit(int)} method.
     *
     * @param   ch   the character to be tested.
     * @return  {@code true} if the character is a digit;
     *          {@code false} otherwise.
     * @see     Character#digit(char, int)
     * @see     Character#forDigit(int, int)
     * @see     Character#getType(char)
     */
    public static boolean isDigit(char ch) {
        return isDigit((int)ch);
    }

    /**
     * Determines if the specified character (Unicode code point) is a digit.
     * <p>
     * A character is a digit if its general category type, provided
     * by {@link Character#getType(int) getType(codePoint)}, is
     * {@code DECIMAL_DIGIT_NUMBER}.
     * <p>
     * Some Unicode character ranges that contain digits:
     * <ul>
     * <li>{@code '\u005Cu0030'} through {@code '\u005Cu0039'},
     *     ISO-LATIN-1 digits ({@code '0'} through {@code '9'})
     * <li>{@code '\u005Cu0660'} through {@code '\u005Cu0669'},
     *     Arabic-Indic digits
     * <li>{@code '\u005Cu06F0'} through {@code '\u005Cu06F9'},
     *     Extended Arabic-Indic digits
     * <li>{@code '\u005Cu0966'} through {@code '\u005Cu096F'},
     *     Devanagari digits
     * <li>{@code '\u005CuFF10'} through {@code '\u005CuFF19'},
     *     Fullwidth digits
     * </ul>
     *
     * Many other character ranges contain digits as well.
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  {@code true} if the character is a digit;
     *          {@code false} otherwise.
     * @see     Character#forDigit(int, int)
     * @see     Character#getType(int)
     * @since   1.5
     */
    public static boolean isDigit(int codePoint) {
        return CharacterData.of(codePoint).isDigit(codePoint);
    }

    /**
     * Determines if a character is defined in Unicode.
     * <p>
     * A character is defined if at least one of the following is true:
     * <ul>
     * <li>It has an entry in the UnicodeData file.
     * <li>It has a value in a range defined by the UnicodeData file.
     * </ul>
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #isDefined(int)} method.
     *
     * @param   ch   the character to be tested
     * @return  {@code true} if the character has a defined meaning
     *          in Unicode; {@code false} otherwise.
     * @see     Character#isDigit(char)
     * @see     Character#isLetter(char)
     * @see     Character#isLetterOrDigit(char)
     * @see     Character#isLowerCase(char)
     * @see     Character#isTitleCase(char)
     * @see     Character#isUpperCase(char)
     * @since   1.0.2
     */
    public static boolean isDefined(char ch) {
        return isDefined((int)ch);
    }

    /**
     * Determines if a character (Unicode code point) is defined in Unicode.
     * <p>
     * A character is defined if at least one of the following is true:
     * <ul>
     * <li>It has an entry in the UnicodeData file.
     * <li>It has a value in a range defined by the UnicodeData file.
     * </ul>
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  {@code true} if the character has a defined meaning
     *          in Unicode; {@code false} otherwise.
     * @see     Character#isDigit(int)
     * @see     Character#isLetter(int)
     * @see     Character#isLetterOrDigit(int)
     * @see     Character#isLowerCase(int)
     * @see     Character#isTitleCase(int)
     * @see     Character#isUpperCase(int)
     * @since   1.5
     */
    public static boolean isDefined(int codePoint) {
        return getType(codePoint) != Character.UNASSIGNED;
    }

    /**
     * Determines if the specified character is a letter.
     * <p>
     * A character is considered to be a letter if its general
     * category type, provided by {@code Character.getType(ch)},
     * is any of the following:
     * <ul>
     * <li> {@code UPPERCASE_LETTER}
     * <li> {@code LOWERCASE_LETTER}
     * <li> {@code TITLECASE_LETTER}
     * <li> {@code MODIFIER_LETTER}
     * <li> {@code OTHER_LETTER}
     * </ul>
     *
     * Not all letters have case. Many characters are
     * letters but are neither uppercase nor lowercase nor titlecase.
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #isLetter(int)} method.
     *
     * @param   ch   the character to be tested.
     * @return  {@code true} if the character is a letter;
     *          {@code false} otherwise.
     * @see     Character#isDigit(char)
     * @see     Character#isJavaIdentifierStart(char)
     * @see     Character#isJavaLetter(char)
     * @see     Character#isJavaLetterOrDigit(char)
     * @see     Character#isLetterOrDigit(char)
     * @see     Character#isLowerCase(char)
     * @see     Character#isTitleCase(char)
     * @see     Character#isUnicodeIdentifierStart(char)
     * @see     Character#isUpperCase(char)
     */
    public static boolean isLetter(char ch) {
        return isLetter((int)ch);
    }

    /**
     * Determines if the specified character (Unicode code point) is a letter.
     * <p>
     * A character is considered to be a letter if its general
     * category type, provided by {@link Character#getType(int) getType(codePoint)},
     * is any of the following:
     * <ul>
     * <li> {@code UPPERCASE_LETTER}
     * <li> {@code LOWERCASE_LETTER}
     * <li> {@code TITLECASE_LETTER}
     * <li> {@code MODIFIER_LETTER}
     * <li> {@code OTHER_LETTER}
     * </ul>
     *
     * Not all letters have case. Many characters are
     * letters but are neither uppercase nor lowercase nor titlecase.
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  {@code true} if the character is a letter;
     *          {@code false} otherwise.
     * @see     Character#isDigit(int)
     * @see     Character#isJavaIdentifierStart(int)
     * @see     Character#isLetterOrDigit(int)
     * @see     Character#isLowerCase(int)
     * @see     Character#isTitleCase(int)
     * @see     Character#isUnicodeIdentifierStart(int)
     * @see     Character#isUpperCase(int)
     * @since   1.5
     */
    public static boolean isLetter(int codePoint) {
        return ((((1 << Character.UPPERCASE_LETTER) |
            (1 << Character.LOWERCASE_LETTER) |
            (1 << Character.TITLECASE_LETTER) |
            (1 << Character.MODIFIER_LETTER) |
            (1 << Character.OTHER_LETTER)) >> getType(codePoint)) & 1)
            != 0;
    }

    /**
     * Determines if the specified character is a letter or digit.
     * <p>
     * A character is considered to be a letter or digit if either
     * {@code Character.isLetter(char ch)} or
     * {@code Character.isDigit(char ch)} returns
     * {@code true} for the character.
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #isLetterOrDigit(int)} method.
     *
     * @param   ch   the character to be tested.
     * @return  {@code true} if the character is a letter or digit;
     *          {@code false} otherwise.
     * @see     Character#isDigit(char)
     * @see     Character#isJavaIdentifierPart(char)
     * @see     Character#isJavaLetter(char)
     * @see     Character#isJavaLetterOrDigit(char)
     * @see     Character#isLetter(char)
     * @see     Character#isUnicodeIdentifierPart(char)
     * @since   1.0.2
     */
    public static boolean isLetterOrDigit(char ch) {
        return isLetterOrDigit((int)ch);
    }

    /**
     * Determines if the specified character (Unicode code point) is a letter or digit.
     * <p>
     * A character is considered to be a letter or digit if either
     * {@link #isLetter(int) isLetter(codePoint)} or
     * {@link #isDigit(int) isDigit(codePoint)} returns
     * {@code true} for the character.
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  {@code true} if the character is a letter or digit;
     *          {@code false} otherwise.
     * @see     Character#isDigit(int)
     * @see     Character#isJavaIdentifierPart(int)
     * @see     Character#isLetter(int)
     * @see     Character#isUnicodeIdentifierPart(int)
     * @since   1.5
     */
    public static boolean isLetterOrDigit(int codePoint) {
        return ((((1 << Character.UPPERCASE_LETTER) |
            (1 << Character.LOWERCASE_LETTER) |
            (1 << Character.TITLECASE_LETTER) |
            (1 << Character.MODIFIER_LETTER) |
            (1 << Character.OTHER_LETTER) |
            (1 << Character.DECIMAL_DIGIT_NUMBER)) >> getType(codePoint)) & 1)
            != 0;
    }

    /**
     * Determines if the specified character is permissible as the first
     * character in a Java identifier.
     * <p>
     * A character may start a Java identifier if and only if
     * one of the following conditions is true:
     * <ul>
     * <li> {@link #isLetter(char) isLetter(ch)} returns {@code true}
     * <li> {@link #getType(char) getType(ch)} returns {@code LETTER_NUMBER}
     * <li> {@code ch} is a currency symbol (such as {@code '$'})
     * <li> {@code ch} is a connecting punctuation character (such as {@code '_'}).
     * </ul>
     *
     * @param   ch the character to be tested.
     * @return  {@code true} if the character may start a Java
     *          identifier; {@code false} otherwise.
     * @see     Character#isJavaLetterOrDigit(char)
     * @see     Character#isJavaIdentifierStart(char)
     * @see     Character#isJavaIdentifierPart(char)
     * @see     Character#isLetter(char)
     * @see     Character#isLetterOrDigit(char)
     * @see     Character#isUnicodeIdentifierStart(char)
     * @since   1.0.2
     * @deprecated Replaced by isJavaIdentifierStart(char).
     */
    @Deprecated(since="1.1")
    public static boolean isJavaLetter(char ch) {
        return isJavaIdentifierStart(ch);
    }

    /**
     * Determines if the specified character may be part of a Java
     * identifier as other than the first character.
     * <p>
     * A character may be part of a Java identifier if and only if one
     * of the following conditions is true:
     * <ul>
     * <li>  it is a letter
     * <li>  it is a currency symbol (such as {@code '$'})
     * <li>  it is a connecting punctuation character (such as {@code '_'})
     * <li>  it is a digit
     * <li>  it is a numeric letter (such as a Roman numeral character)
     * <li>  it is a combining mark
     * <li>  it is a non-spacing mark
     * <li> {@code isIdentifierIgnorable} returns
     * {@code true} for the character.
     * </ul>
     *
     * @param   ch the character to be tested.
     * @return  {@code true} if the character may be part of a
     *          Java identifier; {@code false} otherwise.
     * @see     Character#isJavaLetter(char)
     * @see     Character#isJavaIdentifierStart(char)
     * @see     Character#isJavaIdentifierPart(char)
     * @see     Character#isLetter(char)
     * @see     Character#isLetterOrDigit(char)
     * @see     Character#isUnicodeIdentifierPart(char)
     * @see     Character#isIdentifierIgnorable(char)
     * @since   1.0.2
     * @deprecated Replaced by isJavaIdentifierPart(char).
     */
    @Deprecated(since="1.1")
    public static boolean isJavaLetterOrDigit(char ch) {
        return isJavaIdentifierPart(ch);
    }

    /**
     * Determines if the specified character (Unicode code point) is alphabetic.
     * <p>
     * A character is considered to be alphabetic if its general category type,
     * provided by {@link Character#getType(int) getType(codePoint)}, is any of
     * the following:
     * <ul>
     * <li> {@code UPPERCASE_LETTER}
     * <li> {@code LOWERCASE_LETTER}
     * <li> {@code TITLECASE_LETTER}
     * <li> {@code MODIFIER_LETTER}
     * <li> {@code OTHER_LETTER}
     * <li> {@code LETTER_NUMBER}
     * </ul>
     * or it has contributory property Other_Alphabetic as defined by the
     * Unicode Standard.
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  {@code true} if the character is a Unicode alphabet
     *          character, {@code false} otherwise.
     * @since   1.7
     */
    public static boolean isAlphabetic(int codePoint) {
        return (((((1 << Character.UPPERCASE_LETTER) |
            (1 << Character.LOWERCASE_LETTER) |
            (1 << Character.TITLECASE_LETTER) |
            (1 << Character.MODIFIER_LETTER) |
            (1 << Character.OTHER_LETTER) |
            (1 << Character.LETTER_NUMBER)) >> getType(codePoint)) & 1) != 0) ||
            CharacterData.of(codePoint).isOtherAlphabetic(codePoint);
    }

    /**
     * Determines if the specified character (Unicode code point) is a CJKV
     * (Chinese, Japanese, Korean and Vietnamese) ideograph, as defined by
     * the Unicode Standard.
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  {@code true} if the character is a Unicode ideograph
     *          character, {@code false} otherwise.
     * @since   1.7
     */
    public static boolean isIdeographic(int codePoint) {
        return CharacterData.of(codePoint).isIdeographic(codePoint);
    }

    /**
     * Determines if the specified character is
     * permissible as the first character in a Java identifier.
     * <p>
     * A character may start a Java identifier if and only if
     * one of the following conditions is true:
     * <ul>
     * <li> {@link #isLetter(char) isLetter(ch)} returns {@code true}
     * <li> {@link #getType(char) getType(ch)} returns {@code LETTER_NUMBER}
     * <li> {@code ch} is a currency symbol (such as {@code '$'})
     * <li> {@code ch} is a connecting punctuation character (such as {@code '_'}).
     * </ul>
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #isJavaIdentifierStart(int)} method.
     *
     * @param   ch the character to be tested.
     * @return  {@code true} if the character may start a Java identifier;
     *          {@code false} otherwise.
     * @see     Character#isJavaIdentifierPart(char)
     * @see     Character#isLetter(char)
     * @see     Character#isUnicodeIdentifierStart(char)
     * @see     javax.lang.model.SourceVersion#isIdentifier(CharSequence)
     * @since   1.1
     */
    public static boolean isJavaIdentifierStart(char ch) {
        return isJavaIdentifierStart((int)ch);
    }

    /**
     * Determines if the character (Unicode code point) is
     * permissible as the first character in a Java identifier.
     * <p>
     * A character may start a Java identifier if and only if
     * one of the following conditions is true:
     * <ul>
     * <li> {@link #isLetter(int) isLetter(codePoint)}
     *      returns {@code true}
     * <li> {@link #getType(int) getType(codePoint)}
     *      returns {@code LETTER_NUMBER}
     * <li> the referenced character is a currency symbol (such as {@code '$'})
     * <li> the referenced character is a connecting punctuation character
     *      (such as {@code '_'}).
     * </ul>
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  {@code true} if the character may start a Java identifier;
     *          {@code false} otherwise.
     * @see     Character#isJavaIdentifierPart(int)
     * @see     Character#isLetter(int)
     * @see     Character#isUnicodeIdentifierStart(int)
     * @see     javax.lang.model.SourceVersion#isIdentifier(CharSequence)
     * @since   1.5
     */
    public static boolean isJavaIdentifierStart(int codePoint) {
        return CharacterData.of(codePoint).isJavaIdentifierStart(codePoint);
    }

    /**
     * Determines if the specified character may be part of a Java
     * identifier as other than the first character.
     * <p>
     * A character may be part of a Java identifier if any of the following
     * conditions are true:
     * <ul>
     * <li>  it is a letter
     * <li>  it is a currency symbol (such as {@code '$'})
     * <li>  it is a connecting punctuation character (such as {@code '_'})
     * <li>  it is a digit
     * <li>  it is a numeric letter (such as a Roman numeral character)
     * <li>  it is a combining mark
     * <li>  it is a non-spacing mark
     * <li> {@code isIdentifierIgnorable} returns
     * {@code true} for the character
     * </ul>
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #isJavaIdentifierPart(int)} method.
     *
     * @param   ch      the character to be tested.
     * @return {@code true} if the character may be part of a
     *          Java identifier; {@code false} otherwise.
     * @see     Character#isIdentifierIgnorable(char)
     * @see     Character#isJavaIdentifierStart(char)
     * @see     Character#isLetterOrDigit(char)
     * @see     Character#isUnicodeIdentifierPart(char)
     * @see     javax.lang.model.SourceVersion#isIdentifier(CharSequence)
     * @since   1.1
     */
    public static boolean isJavaIdentifierPart(char ch) {
        return isJavaIdentifierPart((int)ch);
    }

    /**
     * Determines if the character (Unicode code point) may be part of a Java
     * identifier as other than the first character.
     * <p>
     * A character may be part of a Java identifier if any of the following
     * conditions are true:
     * <ul>
     * <li>  it is a letter
     * <li>  it is a currency symbol (such as {@code '$'})
     * <li>  it is a connecting punctuation character (such as {@code '_'})
     * <li>  it is a digit
     * <li>  it is a numeric letter (such as a Roman numeral character)
     * <li>  it is a combining mark
     * <li>  it is a non-spacing mark
     * <li> {@link #isIdentifierIgnorable(int)
     * isIdentifierIgnorable(codePoint)} returns {@code true} for
     * the code point
     * </ul>
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return {@code true} if the character may be part of a
     *          Java identifier; {@code false} otherwise.
     * @see     Character#isIdentifierIgnorable(int)
     * @see     Character#isJavaIdentifierStart(int)
     * @see     Character#isLetterOrDigit(int)
     * @see     Character#isUnicodeIdentifierPart(int)
     * @see     javax.lang.model.SourceVersion#isIdentifier(CharSequence)
     * @since   1.5
     */
    public static boolean isJavaIdentifierPart(int codePoint) {
        return CharacterData.of(codePoint).isJavaIdentifierPart(codePoint);
    }

    /**
     * Determines if the specified character is permissible as the
     * first character in a Unicode identifier.
     * <p>
     * A character may start a Unicode identifier if and only if
     * one of the following conditions is true:
     * <ul>
     * <li> {@link #isLetter(char) isLetter(ch)} returns {@code true}
     * <li> {@link #getType(char) getType(ch)} returns
     *      {@code LETTER_NUMBER}.
     * <li> it is an <a href="http://www.unicode.org/reports/tr44/#Other_ID_Start">
     *      {@code Other_ID_Start}</a> character.
     * </ul>
     * <p>
     * This method conforms to <a href="https://unicode.org/reports/tr31/#R1">
     * UAX31-R1: Default Identifiers</a> requirement of the Unicode Standard,
     * with the following profile of UAX31:
     * <pre>
     * Start := ID_Start + 'VERTICAL TILDE' (U+2E2F)
     * </pre>
     * {@code 'VERTICAL TILDE'} is added to {@code Start} for backward
     * compatibility.
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #isUnicodeIdentifierStart(int)} method.
     *
     * @param   ch      the character to be tested.
     * @return  {@code true} if the character may start a Unicode
     *          identifier; {@code false} otherwise.
     * @see     Character#isJavaIdentifierStart(char)
     * @see     Character#isLetter(char)
     * @see     Character#isUnicodeIdentifierPart(char)
     * @since   1.1
     */
    public static boolean isUnicodeIdentifierStart(char ch) {
        return isUnicodeIdentifierStart((int)ch);
    }

    /**
     * Determines if the specified character (Unicode code point) is permissible as the
     * first character in a Unicode identifier.
     * <p>
     * A character may start a Unicode identifier if and only if
     * one of the following conditions is true:
     * <ul>
     * <li> {@link #isLetter(int) isLetter(codePoint)}
     *      returns {@code true}
     * <li> {@link #getType(int) getType(codePoint)}
     *      returns {@code LETTER_NUMBER}.
     * <li> it is an <a href="http://www.unicode.org/reports/tr44/#Other_ID_Start">
     *      {@code Other_ID_Start}</a> character.
     * </ul>
     * <p>
     * This method conforms to <a href="https://unicode.org/reports/tr31/#R1">
     * UAX31-R1: Default Identifiers</a> requirement of the Unicode Standard,
     * with the following profile of UAX31:
     * <pre>
     * Start := ID_Start + 'VERTICAL TILDE' (U+2E2F)
     * </pre>
     * {@code 'VERTICAL TILDE'} is added to {@code Start} for backward
     * compatibility.
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  {@code true} if the character may start a Unicode
     *          identifier; {@code false} otherwise.
     * @see     Character#isJavaIdentifierStart(int)
     * @see     Character#isLetter(int)
     * @see     Character#isUnicodeIdentifierPart(int)
     * @since   1.5
     */
    public static boolean isUnicodeIdentifierStart(int codePoint) {
        return CharacterData.of(codePoint).isUnicodeIdentifierStart(codePoint);
    }

    /**
     * Determines if the specified character may be part of a Unicode
     * identifier as other than the first character.
     * <p>
     * A character may be part of a Unicode identifier if and only if
     * one of the following statements is true:
     * <ul>
     * <li>  it is a letter
     * <li>  it is a connecting punctuation character (such as {@code '_'})
     * <li>  it is a digit
     * <li>  it is a numeric letter (such as a Roman numeral character)
     * <li>  it is a combining mark
     * <li>  it is a non-spacing mark
     * <li> {@code isIdentifierIgnorable} returns
     * {@code true} for this character.
     * <li> it is an <a href="http://www.unicode.org/reports/tr44/#Other_ID_Start">
     *      {@code Other_ID_Start}</a> character.
     * <li> it is an <a href="http://www.unicode.org/reports/tr44/#Other_ID_Continue">
     *      {@code Other_ID_Continue}</a> character.
     * </ul>
     * <p>
     * This method conforms to <a href="https://unicode.org/reports/tr31/#R1">
     * UAX31-R1: Default Identifiers</a> requirement of the Unicode Standard,
     * with the following profile of UAX31:
     * <pre>
     * Continue := Start + ID_Continue + ignorable
     * Medial := empty
     * ignorable := isIdentifierIgnorable(char) returns true for the character
     * </pre>
     * {@code ignorable} is added to {@code Continue} for backward
     * compatibility.
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #isUnicodeIdentifierPart(int)} method.
     *
     * @param   ch      the character to be tested.
     * @return  {@code true} if the character may be part of a
     *          Unicode identifier; {@code false} otherwise.
     * @see     Character#isIdentifierIgnorable(char)
     * @see     Character#isJavaIdentifierPart(char)
     * @see     Character#isLetterOrDigit(char)
     * @see     Character#isUnicodeIdentifierStart(char)
     * @since   1.1
     */
    public static boolean isUnicodeIdentifierPart(char ch) {
        return isUnicodeIdentifierPart((int)ch);
    }

    /**
     * Determines if the specified character (Unicode code point) may be part of a Unicode
     * identifier as other than the first character.
     * <p>
     * A character may be part of a Unicode identifier if and only if
     * one of the following statements is true:
     * <ul>
     * <li>  it is a letter
     * <li>  it is a connecting punctuation character (such as {@code '_'})
     * <li>  it is a digit
     * <li>  it is a numeric letter (such as a Roman numeral character)
     * <li>  it is a combining mark
     * <li>  it is a non-spacing mark
     * <li> {@code isIdentifierIgnorable} returns
     * {@code true} for this character.
     * <li> it is an <a href="http://www.unicode.org/reports/tr44/#Other_ID_Start">
     *      {@code Other_ID_Start}</a> character.
     * <li> it is an <a href="http://www.unicode.org/reports/tr44/#Other_ID_Continue">
     *      {@code Other_ID_Continue}</a> character.
     * </ul>
     * <p>
     * This method conforms to <a href="https://unicode.org/reports/tr31/#R1">
     * UAX31-R1: Default Identifiers</a> requirement of the Unicode Standard,
     * with the following profile of UAX31:
     * <pre>
     * Continue := Start + ID_Continue + ignorable
     * Medial := empty
     * ignorable := isIdentifierIgnorable(int) returns true for the character
     * </pre>
     * {@code ignorable} is added to {@code Continue} for backward
     * compatibility.
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  {@code true} if the character may be part of a
     *          Unicode identifier; {@code false} otherwise.
     * @see     Character#isIdentifierIgnorable(int)
     * @see     Character#isJavaIdentifierPart(int)
     * @see     Character#isLetterOrDigit(int)
     * @see     Character#isUnicodeIdentifierStart(int)
     * @since   1.5
     */
    public static boolean isUnicodeIdentifierPart(int codePoint) {
        return CharacterData.of(codePoint).isUnicodeIdentifierPart(codePoint);
    }

    /**
     * Determines if the specified character should be regarded as
     * an ignorable character in a Java identifier or a Unicode identifier.
     * <p>
     * The following Unicode characters are ignorable in a Java identifier
     * or a Unicode identifier:
     * <ul>
     * <li>ISO control characters that are not whitespace
     * <ul>
     * <li>{@code '\u005Cu0000'} through {@code '\u005Cu0008'}
     * <li>{@code '\u005Cu000E'} through {@code '\u005Cu001B'}
     * <li>{@code '\u005Cu007F'} through {@code '\u005Cu009F'}
     * </ul>
     *
     * <li>all characters that have the {@code FORMAT} general
     * category value
     * </ul>
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #isIdentifierIgnorable(int)} method.
     *
     * @param   ch      the character to be tested.
     * @return  {@code true} if the character is an ignorable control
     *          character that may be part of a Java or Unicode identifier;
     *           {@code false} otherwise.
     * @see     Character#isJavaIdentifierPart(char)
     * @see     Character#isUnicodeIdentifierPart(char)
     * @since   1.1
     */
    public static boolean isIdentifierIgnorable(char ch) {
        return isIdentifierIgnorable((int)ch);
    }

    /**
     * Determines if the specified character (Unicode code point) should be regarded as
     * an ignorable character in a Java identifier or a Unicode identifier.
     * <p>
     * The following Unicode characters are ignorable in a Java identifier
     * or a Unicode identifier:
     * <ul>
     * <li>ISO control characters that are not whitespace
     * <ul>
     * <li>{@code '\u005Cu0000'} through {@code '\u005Cu0008'}
     * <li>{@code '\u005Cu000E'} through {@code '\u005Cu001B'}
     * <li>{@code '\u005Cu007F'} through {@code '\u005Cu009F'}
     * </ul>
     *
     * <li>all characters that have the {@code FORMAT} general
     * category value
     * </ul>
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  {@code true} if the character is an ignorable control
     *          character that may be part of a Java or Unicode identifier;
     *          {@code false} otherwise.
     * @see     Character#isJavaIdentifierPart(int)
     * @see     Character#isUnicodeIdentifierPart(int)
     * @since   1.5
     */
    public static boolean isIdentifierIgnorable(int codePoint) {
        return CharacterData.of(codePoint).isIdentifierIgnorable(codePoint);
    }

    /**
     * Converts the character argument to lowercase using case
     * mapping information from the UnicodeData file.
     * <p>
     * Note that
     * {@code Character.isLowerCase(Character.toLowerCase(ch))}
     * does not always return {@code true} for some ranges of
     * characters, particularly those that are symbols or ideographs.
     *
     * <p>In general, {@link String#toLowerCase()} should be used to map
     * characters to lowercase. {@code String} case mapping methods
     * have several benefits over {@code Character} case mapping methods.
     * {@code String} case mapping methods can perform locale-sensitive
     * mappings, context-sensitive mappings, and 1:M character mappings, whereas
     * the {@code Character} case mapping methods cannot.
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #toLowerCase(int)} method.
     *
     * @param   ch   the character to be converted.
     * @return  the lowercase equivalent of the character, if any;
     *          otherwise, the character itself.
     * @see     Character#isLowerCase(char)
     * @see     String#toLowerCase()
     */
    public static char toLowerCase(char ch) {
        return (char)toLowerCase((int)ch);
    }

    /**
     * Converts the character (Unicode code point) argument to
     * lowercase using case mapping information from the UnicodeData
     * file.
     *
     * <p> Note that
     * {@code Character.isLowerCase(Character.toLowerCase(codePoint))}
     * does not always return {@code true} for some ranges of
     * characters, particularly those that are symbols or ideographs.
     *
     * <p>In general, {@link String#toLowerCase()} should be used to map
     * characters to lowercase. {@code String} case mapping methods
     * have several benefits over {@code Character} case mapping methods.
     * {@code String} case mapping methods can perform locale-sensitive
     * mappings, context-sensitive mappings, and 1:M character mappings, whereas
     * the {@code Character} case mapping methods cannot.
     *
     * @param   codePoint   the character (Unicode code point) to be converted.
     * @return  the lowercase equivalent of the character (Unicode code
     *          point), if any; otherwise, the character itself.
     * @see     Character#isLowerCase(int)
     * @see     String#toLowerCase()
     *
     * @since   1.5
     */
    public static int toLowerCase(int codePoint) {
        return CharacterData.of(codePoint).toLowerCase(codePoint);
    }

    /**
     * Converts the character argument to uppercase using case mapping
     * information from the UnicodeData file.
     * <p>
     * Note that
     * {@code Character.isUpperCase(Character.toUpperCase(ch))}
     * does not always return {@code true} for some ranges of
     * characters, particularly those that are symbols or ideographs.
     *
     * <p>In general, {@link String#toUpperCase()} should be used to map
     * characters to uppercase. {@code String} case mapping methods
     * have several benefits over {@code Character} case mapping methods.
     * {@code String} case mapping methods can perform locale-sensitive
     * mappings, context-sensitive mappings, and 1:M character mappings, whereas
     * the {@code Character} case mapping methods cannot.
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #toUpperCase(int)} method.
     *
     * @param   ch   the character to be converted.
     * @return  the uppercase equivalent of the character, if any;
     *          otherwise, the character itself.
     * @see     Character#isUpperCase(char)
     * @see     String#toUpperCase()
     */
    public static char toUpperCase(char ch) {
        return (char)toUpperCase((int)ch);
    }

    /**
     * Converts the character (Unicode code point) argument to
     * uppercase using case mapping information from the UnicodeData
     * file.
     *
     * <p>Note that
     * {@code Character.isUpperCase(Character.toUpperCase(codePoint))}
     * does not always return {@code true} for some ranges of
     * characters, particularly those that are symbols or ideographs.
     *
     * <p>In general, {@link String#toUpperCase()} should be used to map
     * characters to uppercase. {@code String} case mapping methods
     * have several benefits over {@code Character} case mapping methods.
     * {@code String} case mapping methods can perform locale-sensitive
     * mappings, context-sensitive mappings, and 1:M character mappings, whereas
     * the {@code Character} case mapping methods cannot.
     *
     * @param   codePoint   the character (Unicode code point) to be converted.
     * @return  the uppercase equivalent of the character, if any;
     *          otherwise, the character itself.
     * @see     Character#isUpperCase(int)
     * @see     String#toUpperCase()
     *
     * @since   1.5
     */
    public static int toUpperCase(int codePoint) {
        return CharacterData.of(codePoint).toUpperCase(codePoint);
    }

    /**
     * Converts the character argument to titlecase using case mapping
     * information from the UnicodeData file. If a character has no
     * explicit titlecase mapping and is not itself a titlecase char
     * according to UnicodeData, then the uppercase mapping is
     * returned as an equivalent titlecase mapping. If the
     * {@code char} argument is already a titlecase
     * {@code char}, the same {@code char} value will be
     * returned.
     * <p>
     * Note that
     * {@code Character.isTitleCase(Character.toTitleCase(ch))}
     * does not always return {@code true} for some ranges of
     * characters.
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #toTitleCase(int)} method.
     *
     * @param   ch   the character to be converted.
     * @return  the titlecase equivalent of the character, if any;
     *          otherwise, the character itself.
     * @see     Character#isTitleCase(char)
     * @see     Character#toLowerCase(char)
     * @see     Character#toUpperCase(char)
     * @since   1.0.2
     */
    public static char toTitleCase(char ch) {
        return (char)toTitleCase((int)ch);
    }

    /**
     * Converts the character (Unicode code point) argument to titlecase using case mapping
     * information from the UnicodeData file. If a character has no
     * explicit titlecase mapping and is not itself a titlecase char
     * according to UnicodeData, then the uppercase mapping is
     * returned as an equivalent titlecase mapping. If the
     * character argument is already a titlecase
     * character, the same character value will be
     * returned.
     *
     * <p>Note that
     * {@code Character.isTitleCase(Character.toTitleCase(codePoint))}
     * does not always return {@code true} for some ranges of
     * characters.
     *
     * @param   codePoint   the character (Unicode code point) to be converted.
     * @return  the titlecase equivalent of the character, if any;
     *          otherwise, the character itself.
     * @see     Character#isTitleCase(int)
     * @see     Character#toLowerCase(int)
     * @see     Character#toUpperCase(int)
     * @since   1.5
     */
    public static int toTitleCase(int codePoint) {
        return CharacterData.of(codePoint).toTitleCase(codePoint);
    }

    /**
     * Returns the numeric value of the character {@code ch} in the
     * specified radix.
     * <p>
     * If the radix is not in the range {@code MIN_RADIX} &le;
     * {@code radix} &le; {@code MAX_RADIX} or if the
     * value of {@code ch} is not a valid digit in the specified
     * radix, {@code -1} is returned. A character is a valid digit
     * if at least one of the following is true:
     * <ul>
     * <li>The method {@code isDigit} is {@code true} of the character
     *     and the Unicode decimal digit value of the character (or its
     *     single-character decomposition) is less than the specified radix.
     *     In this case the decimal digit value is returned.
     * <li>The character is one of the uppercase Latin letters
     *     {@code 'A'} through {@code 'Z'} and its code is less than
     *     {@code radix + 'A' - 10}.
     *     In this case, {@code ch - 'A' + 10}
     *     is returned.
     * <li>The character is one of the lowercase Latin letters
     *     {@code 'a'} through {@code 'z'} and its code is less than
     *     {@code radix + 'a' - 10}.
     *     In this case, {@code ch - 'a' + 10}
     *     is returned.
     * <li>The character is one of the fullwidth uppercase Latin letters A
     *     ({@code '\u005CuFF21'}) through Z ({@code '\u005CuFF3A'})
     *     and its code is less than
     *     {@code radix + '\u005CuFF21' - 10}.
     *     In this case, {@code ch - '\u005CuFF21' + 10}
     *     is returned.
     * <li>The character is one of the fullwidth lowercase Latin letters a
     *     ({@code '\u005CuFF41'}) through z ({@code '\u005CuFF5A'})
     *     and its code is less than
     *     {@code radix + '\u005CuFF41' - 10}.
     *     In this case, {@code ch - '\u005CuFF41' + 10}
     *     is returned.
     * </ul>
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #digit(int, int)} method.
     *
     * @param   ch      the character to be converted.
     * @param   radix   the radix.
     * @return  the numeric value represented by the character in the
     *          specified radix.
     * @see     Character#forDigit(int, int)
     * @see     Character#isDigit(char)
     */
    public static int digit(char ch, int radix) {
        return digit((int)ch, radix);
    }

    /**
     * Returns the numeric value of the specified character (Unicode
     * code point) in the specified radix.
     *
     * <p>If the radix is not in the range {@code MIN_RADIX} &le;
     * {@code radix} &le; {@code MAX_RADIX} or if the
     * character is not a valid digit in the specified
     * radix, {@code -1} is returned. A character is a valid digit
     * if at least one of the following is true:
     * <ul>
     * <li>The method {@link #isDigit(int) isDigit(codePoint)} is {@code true} of the character
     *     and the Unicode decimal digit value of the character (or its
     *     single-character decomposition) is less than the specified radix.
     *     In this case the decimal digit value is returned.
     * <li>The character is one of the uppercase Latin letters
     *     {@code 'A'} through {@code 'Z'} and its code is less than
     *     {@code radix + 'A' - 10}.
     *     In this case, {@code codePoint - 'A' + 10}
     *     is returned.
     * <li>The character is one of the lowercase Latin letters
     *     {@code 'a'} through {@code 'z'} and its code is less than
     *     {@code radix + 'a' - 10}.
     *     In this case, {@code codePoint - 'a' + 10}
     *     is returned.
     * <li>The character is one of the fullwidth uppercase Latin letters A
     *     ({@code '\u005CuFF21'}) through Z ({@code '\u005CuFF3A'})
     *     and its code is less than
     *     {@code radix + '\u005CuFF21' - 10}.
     *     In this case,
     *     {@code codePoint - '\u005CuFF21' + 10}
     *     is returned.
     * <li>The character is one of the fullwidth lowercase Latin letters a
     *     ({@code '\u005CuFF41'}) through z ({@code '\u005CuFF5A'})
     *     and its code is less than
     *     {@code radix + '\u005CuFF41'- 10}.
     *     In this case,
     *     {@code codePoint - '\u005CuFF41' + 10}
     *     is returned.
     * </ul>
     *
     * @param   codePoint the character (Unicode code point) to be converted.
     * @param   radix   the radix.
     * @return  the numeric value represented by the character in the
     *          specified radix.
     * @see     Character#forDigit(int, int)
     * @see     Character#isDigit(int)
     * @since   1.5
     */
    public static int digit(int codePoint, int radix) {
        return CharacterData.of(codePoint).digit(codePoint, radix);
    }

    /**
     * Returns the {@code int} value that the specified Unicode
     * character represents. For example, the character
     * {@code '\u005Cu216C'} (the roman numeral fifty) will return
     * an int with a value of 50.
     * <p>
     * The letters A-Z in their uppercase ({@code '\u005Cu0041'} through
     * {@code '\u005Cu005A'}), lowercase
     * ({@code '\u005Cu0061'} through {@code '\u005Cu007A'}), and
     * full width variant ({@code '\u005CuFF21'} through
     * {@code '\u005CuFF3A'} and {@code '\u005CuFF41'} through
     * {@code '\u005CuFF5A'}) forms have numeric values from 10
     * through 35. This is independent of the Unicode specification,
     * which does not assign numeric values to these {@code char}
     * values.
     * <p>
     * If the character does not have a numeric value, then -1 is returned.
     * If the character has a numeric value that cannot be represented as a
     * nonnegative integer (for example, a fractional value), then -2
     * is returned.
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #getNumericValue(int)} method.
     *
     * @param   ch      the character to be converted.
     * @return  the numeric value of the character, as a nonnegative {@code int}
     *          value; -2 if the character has a numeric value but the value
     *          can not be represented as a nonnegative {@code int} value;
     *          -1 if the character has no numeric value.
     * @see     Character#forDigit(int, int)
     * @see     Character#isDigit(char)
     * @since   1.1
     */
    public static int getNumericValue(char ch) {
        return getNumericValue((int)ch);
    }

    /**
     * Returns the {@code int} value that the specified
     * character (Unicode code point) represents. For example, the character
     * {@code '\u005Cu216C'} (the Roman numeral fifty) will return
     * an {@code int} with a value of 50.
     * <p>
     * The letters A-Z in their uppercase ({@code '\u005Cu0041'} through
     * {@code '\u005Cu005A'}), lowercase
     * ({@code '\u005Cu0061'} through {@code '\u005Cu007A'}), and
     * full width variant ({@code '\u005CuFF21'} through
     * {@code '\u005CuFF3A'} and {@code '\u005CuFF41'} through
     * {@code '\u005CuFF5A'}) forms have numeric values from 10
     * through 35. This is independent of the Unicode specification,
     * which does not assign numeric values to these {@code char}
     * values.
     * <p>
     * If the character does not have a numeric value, then -1 is returned.
     * If the character has a numeric value that cannot be represented as a
     * nonnegative integer (for example, a fractional value), then -2
     * is returned.
     *
     * @param   codePoint the character (Unicode code point) to be converted.
     * @return  the numeric value of the character, as a nonnegative {@code int}
     *          value; -2 if the character has a numeric value but the value
     *          can not be represented as a nonnegative {@code int} value;
     *          -1 if the character has no numeric value.
     * @see     Character#forDigit(int, int)
     * @see     Character#isDigit(int)
     * @since   1.5
     */
    public static int getNumericValue(int codePoint) {
        return CharacterData.of(codePoint).getNumericValue(codePoint);
    }

    /**
     * Determines if the specified character is ISO-LATIN-1 white space.
     * This method returns {@code true} for the following five
     * characters only:
     * <table class="striped">
     * <caption style="display:none">truechars</caption>
     * <thead>
     * <tr><th scope="col">Character
     *     <th scope="col">Code
     *     <th scope="col">Name
     * </thead>
     * <tbody>
     * <tr><th scope="row">{@code '\t'}</th>            <td>{@code U+0009}</td>
     *     <td>{@code HORIZONTAL TABULATION}</td></tr>
     * <tr><th scope="row">{@code '\n'}</th>            <td>{@code U+000A}</td>
     *     <td>{@code NEW LINE}</td></tr>
     * <tr><th scope="row">{@code '\f'}</th>            <td>{@code U+000C}</td>
     *     <td>{@code FORM FEED}</td></tr>
     * <tr><th scope="row">{@code '\r'}</th>            <td>{@code U+000D}</td>
     *     <td>{@code CARRIAGE RETURN}</td></tr>
     * <tr><th scope="row">{@code ' '}</th>  <td>{@code U+0020}</td>
     *     <td>{@code SPACE}</td></tr>
     * </tbody>
     * </table>
     *
     * @param      ch   the character to be tested.
     * @return     {@code true} if the character is ISO-LATIN-1 white
     *             space; {@code false} otherwise.
     * @see        Character#isSpaceChar(char)
     * @see        Character#isWhitespace(char)
     * @deprecated Replaced by isWhitespace(char).
     */
    @Deprecated(since="1.1")
    public static boolean isSpace(char ch) {
        return (ch <= 0x0020) &&
            (((((1L << 0x0009) |
            (1L << 0x000A) |
            (1L << 0x000C) |
            (1L << 0x000D) |
            (1L << 0x0020)) >> ch) & 1L) != 0);
    }


    /**
     * Determines if the specified character is a Unicode space character.
     * A character is considered to be a space character if and only if
     * it is specified to be a space character by the Unicode Standard. This
     * method returns true if the character's general category type is any of
     * the following:
     * <ul>
     * <li> {@code SPACE_SEPARATOR}
     * <li> {@code LINE_SEPARATOR}
     * <li> {@code PARAGRAPH_SEPARATOR}
     * </ul>
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #isSpaceChar(int)} method.
     *
     * @param   ch      the character to be tested.
     * @return  {@code true} if the character is a space character;
     *          {@code false} otherwise.
     * @see     Character#isWhitespace(char)
     * @since   1.1
     */
    public static boolean isSpaceChar(char ch) {
        return isSpaceChar((int)ch);
    }

    /**
     * Determines if the specified character (Unicode code point) is a
     * Unicode space character.  A character is considered to be a
     * space character if and only if it is specified to be a space
     * character by the Unicode Standard. This method returns true if
     * the character's general category type is any of the following:
     *
     * <ul>
     * <li> {@link #SPACE_SEPARATOR}
     * <li> {@link #LINE_SEPARATOR}
     * <li> {@link #PARAGRAPH_SEPARATOR}
     * </ul>
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  {@code true} if the character is a space character;
     *          {@code false} otherwise.
     * @see     Character#isWhitespace(int)
     * @since   1.5
     */
    public static boolean isSpaceChar(int codePoint) {
        return ((((1 << Character.SPACE_SEPARATOR) |
                  (1 << Character.LINE_SEPARATOR) |
                  (1 << Character.PARAGRAPH_SEPARATOR)) >> getType(codePoint)) & 1)
            != 0;
    }

    /**
     * Determines if the specified character is white space according to Java.
     * A character is a Java whitespace character if and only if it satisfies
     * one of the following criteria:
     * <ul>
     * <li> It is a Unicode space character ({@code SPACE_SEPARATOR},
     *      {@code LINE_SEPARATOR}, or {@code PARAGRAPH_SEPARATOR})
     *      but is not also a non-breaking space ({@code '\u005Cu00A0'},
     *      {@code '\u005Cu2007'}, {@code '\u005Cu202F'}).
     * <li> It is {@code '\u005Ct'}, U+0009 HORIZONTAL TABULATION.
     * <li> It is {@code '\u005Cn'}, U+000A LINE FEED.
     * <li> It is {@code '\u005Cu000B'}, U+000B VERTICAL TABULATION.
     * <li> It is {@code '\u005Cf'}, U+000C FORM FEED.
     * <li> It is {@code '\u005Cr'}, U+000D CARRIAGE RETURN.
     * <li> It is {@code '\u005Cu001C'}, U+001C FILE SEPARATOR.
     * <li> It is {@code '\u005Cu001D'}, U+001D GROUP SEPARATOR.
     * <li> It is {@code '\u005Cu001E'}, U+001E RECORD SEPARATOR.
     * <li> It is {@code '\u005Cu001F'}, U+001F UNIT SEPARATOR.
     * </ul>
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #isWhitespace(int)} method.
     *
     * @param   ch the character to be tested.
     * @return  {@code true} if the character is a Java whitespace
     *          character; {@code false} otherwise.
     * @see     Character#isSpaceChar(char)
     * @since   1.1
     */
    public static boolean isWhitespace(char ch) {
        return isWhitespace((int)ch);
    }

    /**
     * Determines if the specified character (Unicode code point) is
     * white space according to Java.  A character is a Java
     * whitespace character if and only if it satisfies one of the
     * following criteria:
     * <ul>
     * <li> It is a Unicode space character ({@link #SPACE_SEPARATOR},
     *      {@link #LINE_SEPARATOR}, or {@link #PARAGRAPH_SEPARATOR})
     *      but is not also a non-breaking space ({@code '\u005Cu00A0'},
     *      {@code '\u005Cu2007'}, {@code '\u005Cu202F'}).
     * <li> It is {@code '\u005Ct'}, U+0009 HORIZONTAL TABULATION.
     * <li> It is {@code '\u005Cn'}, U+000A LINE FEED.
     * <li> It is {@code '\u005Cu000B'}, U+000B VERTICAL TABULATION.
     * <li> It is {@code '\u005Cf'}, U+000C FORM FEED.
     * <li> It is {@code '\u005Cr'}, U+000D CARRIAGE RETURN.
     * <li> It is {@code '\u005Cu001C'}, U+001C FILE SEPARATOR.
     * <li> It is {@code '\u005Cu001D'}, U+001D GROUP SEPARATOR.
     * <li> It is {@code '\u005Cu001E'}, U+001E RECORD SEPARATOR.
     * <li> It is {@code '\u005Cu001F'}, U+001F UNIT SEPARATOR.
     * </ul>
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  {@code true} if the character is a Java whitespace
     *          character; {@code false} otherwise.
     * @see     Character#isSpaceChar(int)
     * @since   1.5
     */
    public static boolean isWhitespace(int codePoint) {
        return CharacterData.of(codePoint).isWhitespace(codePoint);
    }

    /**
     * Determines if the specified character is an ISO control
     * character.  A character is considered to be an ISO control
     * character if its code is in the range {@code '\u005Cu0000'}
     * through {@code '\u005Cu001F'} or in the range
     * {@code '\u005Cu007F'} through {@code '\u005Cu009F'}.
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #isISOControl(int)} method.
     *
     * @param   ch      the character to be tested.
     * @return  {@code true} if the character is an ISO control character;
     *          {@code false} otherwise.
     *
     * @see     Character#isSpaceChar(char)
     * @see     Character#isWhitespace(char)
     * @since   1.1
     */
    public static boolean isISOControl(char ch) {
        return isISOControl((int)ch);
    }

    /**
     * Determines if the referenced character (Unicode code point) is an ISO control
     * character.  A character is considered to be an ISO control
     * character if its code is in the range {@code '\u005Cu0000'}
     * through {@code '\u005Cu001F'} or in the range
     * {@code '\u005Cu007F'} through {@code '\u005Cu009F'}.
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  {@code true} if the character is an ISO control character;
     *          {@code false} otherwise.
     * @see     Character#isSpaceChar(int)
     * @see     Character#isWhitespace(int)
     * @since   1.5
     */
    public static boolean isISOControl(int codePoint) {
        // Optimized form of:
        //     (codePoint >= 0x00 && codePoint <= 0x1F) ||
        //     (codePoint >= 0x7F && codePoint <= 0x9F);
        return codePoint <= 0x9F &&
            (codePoint >= 0x7F || (codePoint >>> 5 == 0));
    }

    /**
     * Returns a value indicating a character's general category.
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #getType(int)} method.
     *
     * @param   ch      the character to be tested.
     * @return  a value of type {@code int} representing the
     *          character's general category.
     * @see     Character#COMBINING_SPACING_MARK
     * @see     Character#CONNECTOR_PUNCTUATION
     * @see     Character#CONTROL
     * @see     Character#CURRENCY_SYMBOL
     * @see     Character#DASH_PUNCTUATION
     * @see     Character#DECIMAL_DIGIT_NUMBER
     * @see     Character#ENCLOSING_MARK
     * @see     Character#END_PUNCTUATION
     * @see     Character#FINAL_QUOTE_PUNCTUATION
     * @see     Character#FORMAT
     * @see     Character#INITIAL_QUOTE_PUNCTUATION
     * @see     Character#LETTER_NUMBER
     * @see     Character#LINE_SEPARATOR
     * @see     Character#LOWERCASE_LETTER
     * @see     Character#MATH_SYMBOL
     * @see     Character#MODIFIER_LETTER
     * @see     Character#MODIFIER_SYMBOL
     * @see     Character#NON_SPACING_MARK
     * @see     Character#OTHER_LETTER
     * @see     Character#OTHER_NUMBER
     * @see     Character#OTHER_PUNCTUATION
     * @see     Character#OTHER_SYMBOL
     * @see     Character#PARAGRAPH_SEPARATOR
     * @see     Character#PRIVATE_USE
     * @see     Character#SPACE_SEPARATOR
     * @see     Character#START_PUNCTUATION
     * @see     Character#SURROGATE
     * @see     Character#TITLECASE_LETTER
     * @see     Character#UNASSIGNED
     * @see     Character#UPPERCASE_LETTER
     * @since   1.1
     */
    public static int getType(char ch) {
        return getType((int)ch);
    }

    /**
     * Returns a value indicating a character's general category.
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  a value of type {@code int} representing the
     *          character's general category.
     * @see     Character#COMBINING_SPACING_MARK COMBINING_SPACING_MARK
     * @see     Character#CONNECTOR_PUNCTUATION CONNECTOR_PUNCTUATION
     * @see     Character#CONTROL CONTROL
     * @see     Character#CURRENCY_SYMBOL CURRENCY_SYMBOL
     * @see     Character#DASH_PUNCTUATION DASH_PUNCTUATION
     * @see     Character#DECIMAL_DIGIT_NUMBER DECIMAL_DIGIT_NUMBER
     * @see     Character#ENCLOSING_MARK ENCLOSING_MARK
     * @see     Character#END_PUNCTUATION END_PUNCTUATION
     * @see     Character#FINAL_QUOTE_PUNCTUATION FINAL_QUOTE_PUNCTUATION
     * @see     Character#FORMAT FORMAT
     * @see     Character#INITIAL_QUOTE_PUNCTUATION INITIAL_QUOTE_PUNCTUATION
     * @see     Character#LETTER_NUMBER LETTER_NUMBER
     * @see     Character#LINE_SEPARATOR LINE_SEPARATOR
     * @see     Character#LOWERCASE_LETTER LOWERCASE_LETTER
     * @see     Character#MATH_SYMBOL MATH_SYMBOL
     * @see     Character#MODIFIER_LETTER MODIFIER_LETTER
     * @see     Character#MODIFIER_SYMBOL MODIFIER_SYMBOL
     * @see     Character#NON_SPACING_MARK NON_SPACING_MARK
     * @see     Character#OTHER_LETTER OTHER_LETTER
     * @see     Character#OTHER_NUMBER OTHER_NUMBER
     * @see     Character#OTHER_PUNCTUATION OTHER_PUNCTUATION
     * @see     Character#OTHER_SYMBOL OTHER_SYMBOL
     * @see     Character#PARAGRAPH_SEPARATOR PARAGRAPH_SEPARATOR
     * @see     Character#PRIVATE_USE PRIVATE_USE
     * @see     Character#SPACE_SEPARATOR SPACE_SEPARATOR
     * @see     Character#START_PUNCTUATION START_PUNCTUATION
     * @see     Character#SURROGATE SURROGATE
     * @see     Character#TITLECASE_LETTER TITLECASE_LETTER
     * @see     Character#UNASSIGNED UNASSIGNED
     * @see     Character#UPPERCASE_LETTER UPPERCASE_LETTER
     * @since   1.5
     */
    public static int getType(int codePoint) {
        return CharacterData.of(codePoint).getType(codePoint);
    }

    /**
     * Determines the character representation for a specific digit in
     * the specified radix. If the value of {@code radix} is not a
     * valid radix, or the value of {@code digit} is not a valid
     * digit in the specified radix, the null character
     * ({@code '\u005Cu0000'}) is returned.
     * <p>
     * The {@code radix} argument is valid if it is greater than or
     * equal to {@code MIN_RADIX} and less than or equal to
     * {@code MAX_RADIX}. The {@code digit} argument is valid if
     * {@code 0 <= digit < radix}.
     * <p>
     * If the digit is less than 10, then
     * {@code '0' + digit} is returned. Otherwise, the value
     * {@code 'a' + digit - 10} is returned.
     *
     * @param   digit   the number to convert to a character.
     * @param   radix   the radix.
     * @return  the {@code char} representation of the specified digit
     *          in the specified radix.
     * @see     Character#MIN_RADIX
     * @see     Character#MAX_RADIX
     * @see     Character#digit(char, int)
     */
    public static char forDigit(int digit, int radix) {
        if ((digit >= radix) || (digit < 0)) {
            return '\0';
        }
        if ((radix < Character.MIN_RADIX) || (radix > Character.MAX_RADIX)) {
            return '\0';
        }
        if (digit < 10) {
            return (char)('0' + digit);
        }
        return (char)('a' - 10 + digit);
    }

    /**
     * Returns the Unicode directionality property for the given
     * character.  Character directionality is used to calculate the
     * visual ordering of text. The directionality value of undefined
     * {@code char} values is {@code DIRECTIONALITY_UNDEFINED}.
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #getDirectionality(int)} method.
     *
     * @param  ch {@code char} for which the directionality property
     *            is requested.
     * @return the directionality property of the {@code char} value.
     *
     * @see Character#DIRECTIONALITY_UNDEFINED
     * @see Character#DIRECTIONALITY_LEFT_TO_RIGHT
     * @see Character#DIRECTIONALITY_RIGHT_TO_LEFT
     * @see Character#DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
     * @see Character#DIRECTIONALITY_EUROPEAN_NUMBER
     * @see Character#DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR
     * @see Character#DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR
     * @see Character#DIRECTIONALITY_ARABIC_NUMBER
     * @see Character#DIRECTIONALITY_COMMON_NUMBER_SEPARATOR
     * @see Character#DIRECTIONALITY_NONSPACING_MARK
     * @see Character#DIRECTIONALITY_BOUNDARY_NEUTRAL
     * @see Character#DIRECTIONALITY_PARAGRAPH_SEPARATOR
     * @see Character#DIRECTIONALITY_SEGMENT_SEPARATOR
     * @see Character#DIRECTIONALITY_WHITESPACE
     * @see Character#DIRECTIONALITY_OTHER_NEUTRALS
     * @see Character#DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING
     * @see Character#DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE
     * @see Character#DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
     * @see Character#DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE
     * @see Character#DIRECTIONALITY_POP_DIRECTIONAL_FORMAT
     * @see Character#DIRECTIONALITY_LEFT_TO_RIGHT_ISOLATE
     * @see Character#DIRECTIONALITY_RIGHT_TO_LEFT_ISOLATE
     * @see Character#DIRECTIONALITY_FIRST_STRONG_ISOLATE
     * @see Character#DIRECTIONALITY_POP_DIRECTIONAL_ISOLATE
     * @since 1.4
     */
    public static byte getDirectionality(char ch) {
        return getDirectionality((int)ch);
    }

    /**
     * Returns the Unicode directionality property for the given
     * character (Unicode code point).  Character directionality is
     * used to calculate the visual ordering of text. The
     * directionality value of undefined character is {@link
     * #DIRECTIONALITY_UNDEFINED}.
     *
     * @param   codePoint the character (Unicode code point) for which
     *          the directionality property is requested.
     * @return the directionality property of the character.
     *
     * @see Character#DIRECTIONALITY_UNDEFINED DIRECTIONALITY_UNDEFINED
     * @see Character#DIRECTIONALITY_LEFT_TO_RIGHT DIRECTIONALITY_LEFT_TO_RIGHT
     * @see Character#DIRECTIONALITY_RIGHT_TO_LEFT DIRECTIONALITY_RIGHT_TO_LEFT
     * @see Character#DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
     * @see Character#DIRECTIONALITY_EUROPEAN_NUMBER DIRECTIONALITY_EUROPEAN_NUMBER
     * @see Character#DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR
     * @see Character#DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR
     * @see Character#DIRECTIONALITY_ARABIC_NUMBER DIRECTIONALITY_ARABIC_NUMBER
     * @see Character#DIRECTIONALITY_COMMON_NUMBER_SEPARATOR DIRECTIONALITY_COMMON_NUMBER_SEPARATOR
     * @see Character#DIRECTIONALITY_NONSPACING_MARK DIRECTIONALITY_NONSPACING_MARK
     * @see Character#DIRECTIONALITY_BOUNDARY_NEUTRAL DIRECTIONALITY_BOUNDARY_NEUTRAL
     * @see Character#DIRECTIONALITY_PARAGRAPH_SEPARATOR DIRECTIONALITY_PARAGRAPH_SEPARATOR
     * @see Character#DIRECTIONALITY_SEGMENT_SEPARATOR DIRECTIONALITY_SEGMENT_SEPARATOR
     * @see Character#DIRECTIONALITY_WHITESPACE DIRECTIONALITY_WHITESPACE
     * @see Character#DIRECTIONALITY_OTHER_NEUTRALS DIRECTIONALITY_OTHER_NEUTRALS
     * @see Character#DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING
     * @see Character#DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE
     * @see Character#DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
     * @see Character#DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE
     * @see Character#DIRECTIONALITY_POP_DIRECTIONAL_FORMAT DIRECTIONALITY_POP_DIRECTIONAL_FORMAT
     * @see Character#DIRECTIONALITY_LEFT_TO_RIGHT_ISOLATE DIRECTIONALITY_LEFT_TO_RIGHT_ISOLATE
     * @see Character#DIRECTIONALITY_RIGHT_TO_LEFT_ISOLATE DIRECTIONALITY_RIGHT_TO_LEFT_ISOLATE
     * @see Character#DIRECTIONALITY_FIRST_STRONG_ISOLATE DIRECTIONALITY_FIRST_STRONG_ISOLATE
     * @see Character#DIRECTIONALITY_POP_DIRECTIONAL_ISOLATE DIRECTIONALITY_POP_DIRECTIONAL_ISOLATE
     * @since    1.5
     */
    public static byte getDirectionality(int codePoint) {
        return CharacterData.of(codePoint).getDirectionality(codePoint);
    }

    /**
     * Determines whether the character is mirrored according to the
     * Unicode specification.  Mirrored characters should have their
     * glyphs horizontally mirrored when displayed in text that is
     * right-to-left.  For example, {@code '\u005Cu0028'} LEFT
     * PARENTHESIS is semantically defined to be an <i>opening
     * parenthesis</i>.  This will appear as a "(" in text that is
     * left-to-right but as a ")" in text that is right-to-left.
     *
     * <p><b>Note:</b> This method cannot handle <a
     * href="#supplementary"> supplementary characters</a>. To support
     * all Unicode characters, including supplementary characters, use
     * the {@link #isMirrored(int)} method.
     *
     * @param  ch {@code char} for which the mirrored property is requested
     * @return {@code true} if the char is mirrored, {@code false}
     *         if the {@code char} is not mirrored or is not defined.
     * @since 1.4
     */
    public static boolean isMirrored(char ch) {
        return isMirrored((int)ch);
    }

    /**
     * Determines whether the specified character (Unicode code point)
     * is mirrored according to the Unicode specification.  Mirrored
     * characters should have their glyphs horizontally mirrored when
     * displayed in text that is right-to-left.  For example,
     * {@code '\u005Cu0028'} LEFT PARENTHESIS is semantically
     * defined to be an <i>opening parenthesis</i>.  This will appear
     * as a "(" in text that is left-to-right but as a ")" in text
     * that is right-to-left.
     *
     * @param   codePoint the character (Unicode code point) to be tested.
     * @return  {@code true} if the character is mirrored, {@code false}
     *          if the character is not mirrored or is not defined.
     * @since   1.5
     */
    public static boolean isMirrored(int codePoint) {
        return CharacterData.of(codePoint).isMirrored(codePoint);
    }

    /**
     * Compares two {@code Character} objects numerically.
     *
     * @param   anotherCharacter   the {@code Character} to be compared.
     * @return  the value {@code 0} if the argument {@code Character}
     *          is equal to this {@code Character}; a value less than
     *          {@code 0} if this {@code Character} is numerically less
     *          than the {@code Character} argument; and a value greater than
     *          {@code 0} if this {@code Character} is numerically greater
     *          than the {@code Character} argument (unsigned comparison).
     *          Note that this is strictly a numerical comparison; it is not
     *          locale-dependent.
     * @since   1.2
     */
    public int compareTo(Character anotherCharacter) {
        return compare(this.value, anotherCharacter.value);
    }

    /**
     * Compares two {@code char} values numerically.
     * The value returned is identical to what would be returned by:
     * <pre>
     *    Character.valueOf(x).compareTo(Character.valueOf(y))
     * </pre>
     *
     * @param  x the first {@code char} to compare
     * @param  y the second {@code char} to compare
     * @return the value {@code 0} if {@code x == y};
     *         a value less than {@code 0} if {@code x < y}; and
     *         a value greater than {@code 0} if {@code x > y}
     * @since 1.7
     */
    public static int compare(char x, char y) {
        return x - y;
    }

    /**
     * Converts the character (Unicode code point) argument to uppercase using
     * information from the UnicodeData file.
     *
     * @param   codePoint   the character (Unicode code point) to be converted.
     * @return  either the uppercase equivalent of the character, if
     *          any, or an error flag ({@code Character.ERROR})
     *          that indicates that a 1:M {@code char} mapping exists.
     * @see     Character#isLowerCase(char)
     * @see     Character#isUpperCase(char)
     * @see     Character#toLowerCase(char)
     * @see     Character#toTitleCase(char)
     * @since 1.4
     */
    static int toUpperCaseEx(int codePoint) {
        assert isValidCodePoint(codePoint);
        return CharacterData.of(codePoint).toUpperCaseEx(codePoint);
    }

    /**
     * Converts the character (Unicode code point) argument to uppercase using case
     * mapping information from the SpecialCasing file in the Unicode
     * specification. If a character has no explicit uppercase
     * mapping, then the {@code char} itself is returned in the
     * {@code char[]}.
     *
     * @param   codePoint   the character (Unicode code point) to be converted.
     * @return a {@code char[]} with the uppercased character.
     * @since 1.4
     */
    static char[] toUpperCaseCharArray(int codePoint) {
        // As of Unicode 6.0, 1:M uppercasings only happen in the BMP.
        assert isBmpCodePoint(codePoint);
        return CharacterData.of(codePoint).toUpperCaseCharArray(codePoint);
    }

    /**
     * The number of bits used to represent a {@code char} value in unsigned
     * binary form, constant {@code 16}.
     *
     * @since 1.5
     */
    public static final int SIZE = 16;

    /**
     * The number of bytes used to represent a {@code char} value in unsigned
     * binary form.
     *
     * @since 1.8
     */
    public static final int BYTES = SIZE / Byte.SIZE;

    /**
     * Returns the value obtained by reversing the order of the bytes in the
     * specified {@code char} value.
     *
     * @param ch The {@code char} of which to reverse the byte order.
     * @return the value obtained by reversing (or, equivalently, swapping)
     *     the bytes in the specified {@code char} value.
     * @since 1.5
     */
    @IntrinsicCandidate
    public static char reverseBytes(char ch) {
        return (char) (((ch & 0xFF00) >> 8) | (ch << 8));
    }

    /**
     * Returns the Unicode name of the specified character
     * {@code codePoint}, or null if the code point is
     * {@link #UNASSIGNED unassigned}.
     * <p>
     * Note: if the specified character is not assigned a name by
     * the <i>UnicodeData</i> file (part of the Unicode Character
     * Database maintained by the Unicode Consortium), the returned
     * name is the same as the result of expression:
     *
     * <blockquote>{@code
     *     Character.UnicodeBlock.of(codePoint).toString().replace('_', ' ')
     *     + " "
     *     + Integer.toHexString(codePoint).toUpperCase(Locale.ROOT);
     *
     * }</blockquote>
     *
     * @param  codePoint the character (Unicode code point)
     *
     * @return the Unicode name of the specified character, or null if
     *         the code point is unassigned.
     *
     * @throws IllegalArgumentException if the specified
     *            {@code codePoint} is not a valid Unicode
     *            code point.
     *
     * @since 1.7
     */
    public static String getName(int codePoint) {
        if (!isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException(
                String.format("Not a valid Unicode code point: 0x%X", codePoint));
        }
        String name = CharacterName.getInstance().getName(codePoint);
        if (name != null)
            return name;
        if (getType(codePoint) == UNASSIGNED)
            return null;
        UnicodeBlock block = UnicodeBlock.of(codePoint);
        if (block != null)
            return block.toString().replace('_', ' ') + " "
                   + Integer.toHexString(codePoint).toUpperCase(Locale.ROOT);
        // should never come here
        return Integer.toHexString(codePoint).toUpperCase(Locale.ROOT);
    }

    /**
     * Returns the code point value of the Unicode character specified by
     * the given Unicode character name.
     * <p>
     * Note: if a character is not assigned a name by the <i>UnicodeData</i>
     * file (part of the Unicode Character Database maintained by the Unicode
     * Consortium), its name is defined as the result of expression:
     *
     * <blockquote>{@code
     *     Character.UnicodeBlock.of(codePoint).toString().replace('_', ' ')
     *     + " "
     *     + Integer.toHexString(codePoint).toUpperCase(Locale.ROOT);
     *
     * }</blockquote>
     * <p>
     * The {@code name} matching is case insensitive, with any leading and
     * trailing whitespace character removed.
     *
     * @param  name the Unicode character name
     *
     * @return the code point value of the character specified by its name.
     *
     * @throws IllegalArgumentException if the specified {@code name}
     *         is not a valid Unicode character name.
     * @throws NullPointerException if {@code name} is {@code null}
     *
     * @since 9
     */
    public static int codePointOf(String name) {
        name = name.trim().toUpperCase(Locale.ROOT);
        int cp = CharacterName.getInstance().getCodePoint(name);
        if (cp != -1)
            return cp;
        try {
            int off = name.lastIndexOf(' ');
            if (off != -1) {
                cp = Integer.parseInt(name, off + 1, name.length(), 16);
                if (isValidCodePoint(cp) && name.equals(getName(cp)))
                    return cp;
            }
        } catch (Exception x) {}
        throw new IllegalArgumentException("Unrecognized character name :" + name);
    }
}
