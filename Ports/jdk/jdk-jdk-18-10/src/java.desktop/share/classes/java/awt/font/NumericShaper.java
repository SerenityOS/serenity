/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.font;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

import jdk.internal.access.SharedSecrets;

/**
 * The {@code NumericShaper} class is used to convert Latin-1 (European)
 * digits to other Unicode decimal digits.  Users of this class will
 * primarily be people who wish to present data using
 * national digit shapes, but find it more convenient to represent the
 * data internally using Latin-1 (European) digits.  This does not
 * interpret the deprecated numeric shape selector character (U+206E).
 * <p>
 * Instances of {@code NumericShaper} are typically applied
 * as attributes to text with the
 * {@link TextAttribute#NUMERIC_SHAPING NUMERIC_SHAPING} attribute
 * of the {@code TextAttribute} class.
 * For example, this code snippet causes a {@code TextLayout} to
 * shape European digits to Arabic in an Arabic context:<br>
 * <blockquote><pre>
 * Map map = new HashMap();
 * map.put(TextAttribute.NUMERIC_SHAPING,
 *     NumericShaper.getContextualShaper(NumericShaper.ARABIC));
 * FontRenderContext frc = ...;
 * TextLayout layout = new TextLayout(text, map, frc);
 * layout.draw(g2d, x, y);
 * </pre></blockquote>
 * <br>
 * It is also possible to perform numeric shaping explicitly using instances
 * of {@code NumericShaper}, as this code snippet demonstrates:<br>
 * <blockquote><pre>
 * char[] text = ...;
 * // shape all EUROPEAN digits (except zero) to ARABIC digits
 * NumericShaper shaper = NumericShaper.getShaper(NumericShaper.ARABIC);
 * shaper.shape(text, start, count);
 *
 * // shape European digits to ARABIC digits if preceding text is Arabic, or
 * // shape European digits to TAMIL digits if preceding text is Tamil, or
 * // leave European digits alone if there is no preceding text, or
 * // preceding text is neither Arabic nor Tamil
 * NumericShaper shaper =
 *     NumericShaper.getContextualShaper(NumericShaper.ARABIC |
 *                                         NumericShaper.TAMIL,
 *                                       NumericShaper.EUROPEAN);
 * shaper.shape(text, start, count);
 * </pre></blockquote>
 *
 * <p><b>Bit mask- and enum-based Unicode ranges</b></p>
 *
 * <p>This class supports two different programming interfaces to
 * represent Unicode ranges for script-specific digits: bit
 * mask-based ones, such as {@link #ARABIC NumericShaper.ARABIC}, and
 * enum-based ones, such as {@link NumericShaper.Range#ARABIC}.
 * Multiple ranges can be specified by ORing bit mask-based constants,
 * such as:
 * <blockquote><pre>
 * NumericShaper.ARABIC | NumericShaper.TAMIL
 * </pre></blockquote>
 * or creating a {@code Set} with the {@link NumericShaper.Range}
 * constants, such as:
 * <blockquote><pre>
 * EnumSet.of(NumericShaper.Range.ARABIC, NumericShaper.Range.TAMIL)
 * </pre></blockquote>
 * The enum-based ranges are a super set of the bit mask-based ones.
 *
 * <p>If the two interfaces are mixed (including serialization),
 * Unicode range values are mapped to their counterparts where such
 * mapping is possible, such as {@code NumericShaper.Range.ARABIC}
 * from/to {@code NumericShaper.ARABIC}.  If any unmappable range
 * values are specified, such as {@code NumericShaper.Range.BALINESE},
 * those ranges are ignored.
 *
 * <p><b>Decimal Digits Precedence</b></p>
 *
 * <p>A Unicode range may have more than one set of decimal digits. If
 * multiple decimal digits sets are specified for the same Unicode
 * range, one of the sets will take precedence as follows.
 *
 * <table class="plain">
 * <caption>NumericShaper constants precedence</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Unicode Range
 *     <th scope="col">{@code NumericShaper} Constants
 *     <th scope="col">Precedence
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="rowgroup" rowspan="2">Arabic
 *     <td>{@link NumericShaper#ARABIC NumericShaper.ARABIC}
 *     <br>
 *     {@link NumericShaper#EASTERN_ARABIC NumericShaper.EASTERN_ARABIC}
 *     <td>{@link NumericShaper#EASTERN_ARABIC NumericShaper.EASTERN_ARABIC}
 *   </tr>
 *   <tr>
 *     <td>{@link NumericShaper.Range#ARABIC}
 *     <br>
 *     {@link NumericShaper.Range#EASTERN_ARABIC}
 *     <td>{@link NumericShaper.Range#EASTERN_ARABIC}
 * </tbody>
 * <tbody>
 *   <tr>
 *     <th scope="row">Tai Tham
 *     <td>{@link NumericShaper.Range#TAI_THAM_HORA}
 *     <br>
 *     {@link NumericShaper.Range#TAI_THAM_THAM}
 *     <td>{@link NumericShaper.Range#TAI_THAM_THAM}
 * </tbody>
 * </table>
 *
 * @since 1.4
 */
public final class NumericShaper implements java.io.Serializable {

    // For access from java.text.Bidi
    static {
        if (SharedSecrets.getJavaAWTFontAccess() == null) {
            SharedSecrets.setJavaAWTFontAccess(new JavaAWTFontAccessImpl());
        }
    }

    /**
     * A {@code NumericShaper.Range} represents a Unicode range of a
     * script having its own decimal digits. For example, the {@link
     * NumericShaper.Range#THAI} range has the Thai digits, THAI DIGIT
     * ZERO (U+0E50) to THAI DIGIT NINE (U+0E59).
     *
     * <p>The {@code Range} enum replaces the traditional bit
     * mask-based values (e.g., {@link NumericShaper#ARABIC}), and
     * supports more Unicode ranges than the bit mask-based ones. For
     * example, the following code using the bit mask:
     * <blockquote><pre>
     * NumericShaper.getContextualShaper(NumericShaper.ARABIC |
     *                                     NumericShaper.TAMIL,
     *                                   NumericShaper.EUROPEAN);
     * </pre></blockquote>
     * can be written using this enum as:
     * <blockquote><pre>
     * NumericShaper.getContextualShaper(EnumSet.of(
     *                                     NumericShaper.Range.ARABIC,
     *                                     NumericShaper.Range.TAMIL),
     *                                   NumericShaper.Range.EUROPEAN);
     * </pre></blockquote>
     *
     * @since 1.7
     */
    public static enum Range {
        // The order of EUROPEAN to MOGOLIAN must be consistent
        // with the bitmask-based constants.
        /**
         * The Latin (European) range with the Latin (ASCII) digits.
         */
        EUROPEAN        ('\u0030', '\u0000', '\u0300'),
        /**
         * The Arabic range with the Arabic-Indic digits.
         */
        ARABIC          ('\u0660', '\u0600', '\u0780'),
        /**
         * The Arabic range with the Eastern Arabic-Indic digits.
         */
        EASTERN_ARABIC  ('\u06f0', '\u0600', '\u0780'),
        /**
         * The Devanagari range with the Devanagari digits.
         */
        DEVANAGARI      ('\u0966', '\u0900', '\u0980'),
        /**
         * The Bengali range with the Bengali digits.
         */
        BENGALI         ('\u09e6', '\u0980', '\u0a00'),
        /**
         * The Gurmukhi range with the Gurmukhi digits.
         */
        GURMUKHI        ('\u0a66', '\u0a00', '\u0a80'),
        /**
         * The Gujarati range with the Gujarati digits.
         */
        GUJARATI        ('\u0ae6', '\u0b00', '\u0b80'),
        /**
         * The Oriya range with the Oriya digits.
         */
        ORIYA           ('\u0b66', '\u0b00', '\u0b80'),
        /**
         * The Tamil range with the Tamil digits.
         */
        TAMIL           ('\u0be6', '\u0b80', '\u0c00'),
        /**
         * The Telugu range with the Telugu digits.
         */
        TELUGU          ('\u0c66', '\u0c00', '\u0c80'),
        /**
         * The Kannada range with the Kannada digits.
         */
        KANNADA         ('\u0ce6', '\u0c80', '\u0d00'),
        /**
         * The Malayalam range with the Malayalam digits.
         */
        MALAYALAM       ('\u0d66', '\u0d00', '\u0d80'),
        /**
         * The Thai range with the Thai digits.
         */
        THAI            ('\u0e50', '\u0e00', '\u0e80'),
        /**
         * The Lao range with the Lao digits.
         */
        LAO             ('\u0ed0', '\u0e80', '\u0f00'),
        /**
         * The Tibetan range with the Tibetan digits.
         */
        TIBETAN         ('\u0f20', '\u0f00', '\u1000'),
        /**
         * The Myanmar range with the Myanmar digits.
         */
        MYANMAR         ('\u1040', '\u1000', '\u1080'),
        /**
         * The Ethiopic range with the Ethiopic digits. Ethiopic
         * does not have a decimal digit 0 so Latin (European) 0 is
         * used.
         */
        ETHIOPIC        ('\u1369', '\u1200', '\u1380') {
            @Override
            char getNumericBase() { return 1; }
        },
        /**
         * The Khmer range with the Khmer digits.
         */
        KHMER           ('\u17e0', '\u1780', '\u1800'),
        /**
         * The Mongolian range with the Mongolian digits.
         */
        MONGOLIAN       ('\u1810', '\u1800', '\u1900'),
        // The order of EUROPEAN to MOGOLIAN must be consistent
        // with the bitmask-based constants.

        /**
         * The N'Ko range with the N'Ko digits.
         */
        NKO             ('\u07c0', '\u07c0', '\u0800'),
        /**
         * The Myanmar range with the Myanmar Shan digits.
         */
        MYANMAR_SHAN    ('\u1090', '\u1000', '\u10a0'),
        /**
         * The Limbu range with the Limbu digits.
         */
        LIMBU           ('\u1946', '\u1900', '\u1950'),
        /**
         * The New Tai Lue range with the New Tai Lue digits.
         */
        NEW_TAI_LUE     ('\u19d0', '\u1980', '\u19e0'),
        /**
         * The Balinese range with the Balinese digits.
         */
        BALINESE        ('\u1b50', '\u1b00', '\u1b80'),
        /**
         * The Sundanese range with the Sundanese digits.
         */
        SUNDANESE       ('\u1bb0', '\u1b80', '\u1bc0'),
        /**
         * The Lepcha range with the Lepcha digits.
         */
        LEPCHA          ('\u1c40', '\u1c00', '\u1c50'),
        /**
         * The Ol Chiki range with the Ol Chiki digits.
         */
        OL_CHIKI        ('\u1c50', '\u1c50', '\u1c80'),
        /**
         * The Vai range with the Vai digits.
         */
        VAI             ('\ua620', '\ua500', '\ua640'),
        /**
         * The Saurashtra range with the Saurashtra digits.
         */
        SAURASHTRA      ('\ua8d0', '\ua880', '\ua8e0'),
        /**
         * The Kayah Li range with the Kayah Li digits.
         */
        KAYAH_LI        ('\ua900', '\ua900', '\ua930'),
        /**
         * The Cham range with the Cham digits.
         */
        CHAM            ('\uaa50', '\uaa00', '\uaa60'),
        /**
         * The Tai Tham Hora range with the Tai Tham Hora digits.
         */
        TAI_THAM_HORA   ('\u1a80', '\u1a20', '\u1ab0'),
        /**
         * The Tai Tham Tham range with the Tai Tham Tham digits.
         */
        TAI_THAM_THAM   ('\u1a90', '\u1a20', '\u1ab0'),
        /**
         * The Javanese range with the Javanese digits.
         */
        JAVANESE        ('\ua9d0', '\ua980', '\ua9e0'),
        /**
         * The Meetei Mayek range with the Meetei Mayek digits.
         */
        MEETEI_MAYEK    ('\uabf0', '\uabc0', '\uac00'),
        /**
         * The Sinhala range with the Sinhala digits.
         * @since 9
         */
        SINHALA         ('\u0de6', '\u0d80', '\u0e00'),
        /**
         * The Myanmar Extended-B range with the Myanmar Tai Laing digits.
         * @since 9
         */
        MYANMAR_TAI_LAING ('\ua9f0', '\ua9e0', '\uaa00');

        private static int toRangeIndex(Range script) {
            int index = script.ordinal();
            return index < NUM_KEYS ? index : -1;
        }

        private static Range indexToRange(int index) {
            return index < NUM_KEYS ? Range.values()[index] : null;
        }

        private static int toRangeMask(Set<Range> ranges) {
            int m = 0;
            for (Range range : ranges) {
                int index = range.ordinal();
                if (index < NUM_KEYS) {
                    m |= 1 << index;
                }
            }
            return m;
        }

        private static Set<Range> maskToRangeSet(int mask) {
            Set<Range> set = EnumSet.noneOf(Range.class);
            Range[] a = Range.values();
            for (int i = 0; i < NUM_KEYS; i++) {
                if ((mask & (1 << i)) != 0) {
                    set.add(a[i]);
                }
            }
            return set;
        }

        // base character of range digits
        private final int base;
        // Unicode range
        private final int start, // inclusive
                          end;   // exclusive

        private Range(int base, int start, int end) {
            this.base = base - ('0' + getNumericBase());
            this.start = start;
            this.end = end;
        }

        private int getDigitBase() {
            return base;
        }

        char getNumericBase() {
            return 0;
        }

        private boolean inRange(int c) {
            return start <= c && c < end;
        }
    }

    /** index of context for contextual shaping - values range from 0 to 18 */
    private int key;

    /** flag indicating whether to shape contextually (high bit) and which
     *  digit ranges to shape (bits 0-18)
     */
    private int mask;

    /**
     * The context {@code Range} for contextual shaping or the {@code
     * Range} for non-contextual shaping. {@code null} for the bit
     * mask-based API.
     *
     * @since 1.7
     */
    private Range shapingRange;

    /**
     * {@code Set<Range>} indicating which Unicode ranges to
     * shape. {@code null} for the bit mask-based API.
     */
    private transient Set<Range> rangeSet;

    /**
     * rangeSet.toArray() value. Sorted by Range.base when the number
     * of elements is greater than BSEARCH_THRESHOLD.
     */
    private transient Range[] rangeArray;

    /**
     * If more than BSEARCH_THRESHOLD ranges are specified, binary search is used.
     */
    private static final int BSEARCH_THRESHOLD = 3;

    /**
     * Use serialVersionUID from JDK 1.7 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -8022764705923730308L;

    /** Identifies the Latin-1 (European) and extended range, and
     *  Latin-1 (European) decimal base.
     */
    public static final int EUROPEAN = 1<<0;

    /** Identifies the ARABIC range and decimal base. */
    public static final int ARABIC = 1<<1;

    /** Identifies the ARABIC range and ARABIC_EXTENDED decimal base. */
    public static final int EASTERN_ARABIC = 1<<2;

    /** Identifies the DEVANAGARI range and decimal base. */
    public static final int DEVANAGARI = 1<<3;

    /** Identifies the BENGALI range and decimal base. */
    public static final int BENGALI = 1<<4;

    /** Identifies the GURMUKHI range and decimal base. */
    public static final int GURMUKHI = 1<<5;

    /** Identifies the GUJARATI range and decimal base. */
    public static final int GUJARATI = 1<<6;

    /** Identifies the ORIYA range and decimal base. */
    public static final int ORIYA = 1<<7;

    /** Identifies the TAMIL range and decimal base. */
    // TAMIL DIGIT ZERO was added in Unicode 4.1
    public static final int TAMIL = 1<<8;

    /** Identifies the TELUGU range and decimal base. */
    public static final int TELUGU = 1<<9;

    /** Identifies the KANNADA range and decimal base. */
    public static final int KANNADA = 1<<10;

    /** Identifies the MALAYALAM range and decimal base. */
    public static final int MALAYALAM = 1<<11;

    /** Identifies the THAI range and decimal base. */
    public static final int THAI = 1<<12;

    /** Identifies the LAO range and decimal base. */
    public static final int LAO = 1<<13;

    /** Identifies the TIBETAN range and decimal base. */
    public static final int TIBETAN = 1<<14;

    /** Identifies the MYANMAR range and decimal base. */
    public static final int MYANMAR = 1<<15;

    /** Identifies the ETHIOPIC range and decimal base. */
    public static final int ETHIOPIC = 1<<16;

    /** Identifies the KHMER range and decimal base. */
    public static final int KHMER = 1<<17;

    /** Identifies the MONGOLIAN range and decimal base. */
    public static final int MONGOLIAN = 1<<18;

    /** Identifies all ranges, for full contextual shaping.
     *
     * <p>This constant specifies all of the bit mask-based
     * ranges. Use {@code EnumSet.allOf(NumericShaper.Range.class)} to
     * specify all of the enum-based ranges.
     */
    public static final int ALL_RANGES = 0x0007ffff;

    private static final int EUROPEAN_KEY = 0;
    private static final int ARABIC_KEY = 1;
    private static final int EASTERN_ARABIC_KEY = 2;
    private static final int DEVANAGARI_KEY = 3;
    private static final int BENGALI_KEY = 4;
    private static final int GURMUKHI_KEY = 5;
    private static final int GUJARATI_KEY = 6;
    private static final int ORIYA_KEY = 7;
    private static final int TAMIL_KEY = 8;
    private static final int TELUGU_KEY = 9;
    private static final int KANNADA_KEY = 10;
    private static final int MALAYALAM_KEY = 11;
    private static final int THAI_KEY = 12;
    private static final int LAO_KEY = 13;
    private static final int TIBETAN_KEY = 14;
    private static final int MYANMAR_KEY = 15;
    private static final int ETHIOPIC_KEY = 16;
    private static final int KHMER_KEY = 17;
    private static final int MONGOLIAN_KEY = 18;

    private static final int NUM_KEYS = MONGOLIAN_KEY + 1; // fixed

    private static final int CONTEXTUAL_MASK = 1<<31;

    private static final char[] bases = {
        '\u0030' - '\u0030', // EUROPEAN
        '\u0660' - '\u0030', // ARABIC-INDIC
        '\u06f0' - '\u0030', // EXTENDED ARABIC-INDIC (EASTERN_ARABIC)
        '\u0966' - '\u0030', // DEVANAGARI
        '\u09e6' - '\u0030', // BENGALI
        '\u0a66' - '\u0030', // GURMUKHI
        '\u0ae6' - '\u0030', // GUJARATI
        '\u0b66' - '\u0030', // ORIYA
        '\u0be6' - '\u0030', // TAMIL - zero was added in Unicode 4.1
        '\u0c66' - '\u0030', // TELUGU
        '\u0ce6' - '\u0030', // KANNADA
        '\u0d66' - '\u0030', // MALAYALAM
        '\u0e50' - '\u0030', // THAI
        '\u0ed0' - '\u0030', // LAO
        '\u0f20' - '\u0030', // TIBETAN
        '\u1040' - '\u0030', // MYANMAR
        '\u1369' - '\u0031', // ETHIOPIC - no zero
        '\u17e0' - '\u0030', // KHMER
        '\u1810' - '\u0030', // MONGOLIAN
    };

    // some ranges adjoin or overlap, rethink if we want to do a binary search on this

    private static final char[] contexts = {
        '\u0000', '\u0300', // 'EUROPEAN' (really latin-1 and extended)
        '\u0600', '\u0780', // ARABIC
        '\u0600', '\u0780', // EASTERN_ARABIC -- note overlap with arabic
        '\u0900', '\u0980', // DEVANAGARI
        '\u0980', '\u0a00', // BENGALI
        '\u0a00', '\u0a80', // GURMUKHI
        '\u0a80', '\u0b00', // GUJARATI
        '\u0b00', '\u0b80', // ORIYA
        '\u0b80', '\u0c00', // TAMIL
        '\u0c00', '\u0c80', // TELUGU
        '\u0c80', '\u0d00', // KANNADA
        '\u0d00', '\u0d80', // MALAYALAM
        '\u0e00', '\u0e80', // THAI
        '\u0e80', '\u0f00', // LAO
        '\u0f00', '\u1000', // TIBETAN
        '\u1000', '\u1080', // MYANMAR
        '\u1200', '\u1380', // ETHIOPIC - note missing zero
        '\u1780', '\u1800', // KHMER
        '\u1800', '\u1900', // MONGOLIAN
        '\uffff',
    };

    // assume most characters are near each other so probing the cache is infrequent,
    // and a linear probe is ok.

    private static int ctCache = 0;
    private static int ctCacheLimit = contexts.length - 2;

    // warning, synchronize access to this as it modifies state
    private static int getContextKey(char c) {
        if (c < contexts[ctCache]) {
            while (ctCache > 0 && c < contexts[ctCache]) --ctCache;
        } else if (c >= contexts[ctCache + 1]) {
            while (ctCache < ctCacheLimit && c >= contexts[ctCache + 1]) ++ctCache;
        }

        // if we're not in a known range, then return EUROPEAN as the range key
        return (ctCache & 0x1) == 0 ? (ctCache / 2) : EUROPEAN_KEY;
    }

    // cache for the NumericShaper.Range version
    private transient volatile Range currentRange = Range.EUROPEAN;

    private Range rangeForCodePoint(final int codepoint) {
        if (currentRange.inRange(codepoint)) {
            return currentRange;
        }

        final Range[] ranges = rangeArray;
        if (ranges.length > BSEARCH_THRESHOLD) {
            int lo = 0;
            int hi = ranges.length - 1;
            while (lo <= hi) {
                int mid = (lo + hi) / 2;
                Range range = ranges[mid];
                if (codepoint < range.start) {
                    hi = mid - 1;
                } else if (codepoint >= range.end) {
                    lo = mid + 1;
                } else {
                    currentRange = range;
                    return range;
                }
            }
        } else {
            for (int i = 0; i < ranges.length; i++) {
                if (ranges[i].inRange(codepoint)) {
                    return ranges[i];
                }
            }
        }
        return Range.EUROPEAN;
    }

    /*
     * A range table of strong directional characters (types L, R, AL).
     * Even (left) indexes are starts of ranges of non-strong-directional (or undefined)
     * characters, odd (right) indexes are starts of ranges of strong directional
     * characters.
     */
    private static int[] strongTable = {
        0x0000, 0x0041,
        0x005b, 0x0061,
        0x007b, 0x00aa,
        0x00ab, 0x00b5,
        0x00b6, 0x00ba,
        0x00bb, 0x00c0,
        0x00d7, 0x00d8,
        0x00f7, 0x00f8,
        0x02b9, 0x02bb,
        0x02c2, 0x02d0,
        0x02d2, 0x02e0,
        0x02e5, 0x02ee,
        0x02ef, 0x0370,
        0x0374, 0x0376,
        0x0378, 0x037a,
        0x037e, 0x037f,
        0x0380, 0x0386,
        0x0387, 0x0388,
        0x038b, 0x038c,
        0x038d, 0x038e,
        0x03a2, 0x03a3,
        0x03f6, 0x03f7,
        0x0483, 0x048a,
        0x0530, 0x0531,
        0x0557, 0x0559,
        0x058a, 0x0590,
        0x0591, 0x05be,
        0x05bf, 0x05c0,
        0x05c1, 0x05c3,
        0x05c4, 0x05c6,
        0x05c7, 0x05c8,
        0x0600, 0x0608,
        0x0609, 0x060b,
        0x060c, 0x060d,
        0x060e, 0x061b,
        0x064b, 0x066d,
        0x0670, 0x0671,
        0x06d6, 0x06e5,
        0x06e7, 0x06ee,
        0x06f0, 0x06fa,
        0x0711, 0x0712,
        0x0730, 0x074b,
        0x07a6, 0x07b1,
        0x07eb, 0x07f4,
        0x07f6, 0x07fa,
        0x07fd, 0x07fe,
        0x0816, 0x081a,
        0x081b, 0x0824,
        0x0825, 0x0828,
        0x0829, 0x082e,
        0x0859, 0x085c,
        0x08e3, 0x0903,
        0x093a, 0x093b,
        0x093c, 0x093d,
        0x0941, 0x0949,
        0x094d, 0x094e,
        0x0951, 0x0958,
        0x0962, 0x0964,
        0x0981, 0x0982,
        0x0984, 0x0985,
        0x098d, 0x098f,
        0x0991, 0x0993,
        0x09a9, 0x09aa,
        0x09b1, 0x09b2,
        0x09b3, 0x09b6,
        0x09ba, 0x09bd,
        0x09c1, 0x09c7,
        0x09c9, 0x09cb,
        0x09cd, 0x09ce,
        0x09cf, 0x09d7,
        0x09d8, 0x09dc,
        0x09de, 0x09df,
        0x09e2, 0x09e6,
        0x09f2, 0x09f4,
        0x09fb, 0x09fc,
        0x09fe, 0x0a03,
        0x0a04, 0x0a05,
        0x0a0b, 0x0a0f,
        0x0a11, 0x0a13,
        0x0a29, 0x0a2a,
        0x0a31, 0x0a32,
        0x0a34, 0x0a35,
        0x0a37, 0x0a38,
        0x0a3a, 0x0a3e,
        0x0a41, 0x0a59,
        0x0a5d, 0x0a5e,
        0x0a5f, 0x0a66,
        0x0a70, 0x0a72,
        0x0a75, 0x0a76,
        0x0a73, 0x0a83,
        0x0a84, 0x0a85,
        0x0a8e, 0x0a8f,
        0x0a92, 0x0a93,
        0x0aa9, 0x0aaa,
        0x0ab1, 0x0ab2,
        0x0ab4, 0x0ab5,
        0x0aba, 0x0abd,
        0x0ac1, 0x0ac9,
        0x0aca, 0x0acb,
        0x0acd, 0x0ad0,
        0x0ad1, 0x0ae0,
        0x0ae2, 0x0ae6,
        0x0af1, 0x0af9,
        0x0afa, 0x0b02,
        0x0b04, 0x0b05,
        0x0b0d, 0x0b0f,
        0x0b11, 0x0b13,
        0x0b29, 0x0b2a,
        0x0b31, 0x0b32,
        0x0b34, 0x0b35,
        0x0b3a, 0x0b3d,
        0x0b3f, 0x0b40,
        0x0b41, 0x0b47,
        0x0b49, 0x0b4b,
        0x0b4d, 0x0b57,
        0x0b58, 0x0b5c,
        0x0b5e, 0x0b5f,
        0x0b62, 0x0b66,
        0x0b78, 0x0b83,
        0x0b84, 0x0b85,
        0x0b8b, 0x0b8e,
        0x0b91, 0x0b92,
        0x0b96, 0x0b99,
        0x0b9b, 0x0b9c,
        0x0b9d, 0x0b9e,
        0x0ba0, 0x0ba3,
        0x0ba5, 0x0ba8,
        0x0bab, 0x0bae,
        0x0bba, 0x0bbe,
        0x0bc0, 0x0bc1,
        0x0bc3, 0x0bc6,
        0x0bc9, 0x0bca,
        0x0bcd, 0x0bd0,
        0x0bd1, 0x0bd7,
        0x0bd8, 0x0be6,
        0x0bf3, 0x0c01,
        0x0c04, 0x0c05,
        0x0c0d, 0x0c0e,
        0x0c11, 0x0c12,
        0x0c29, 0x0c2a,
        0x0c3a, 0x0c3d,
        0x0c3e, 0x0c41,
        0x0c45, 0x0c58,
        0x0c5b, 0x0c60,
        0x0c62, 0x0c66,
        0x0c70, 0x0c7f,
        0x0c81, 0x0c82,
        0x0c8d, 0x0c8e,
        0x0c91, 0x0c92,
        0x0ca9, 0x0caa,
        0x0cb4, 0x0cb5,
        0x0cba, 0x0cbd,
        0x0cc5, 0x0cc6,
        0x0cc9, 0x0cca,
        0x0ccc, 0x0cd5,
        0x0cd7, 0x0cde,
        0x0cdf, 0x0ce0,
        0x0ce2, 0x0ce6,
        0x0cf0, 0x0cf1,
        0x0cf3, 0x0d02,
        0x0d04, 0x0d05,
        0x0d0d, 0x0d0e,
        0x0d11, 0x0d12,
        0x0d3b, 0x0d3d,
        0x0d41, 0x0d46,
        0x0d49, 0x0d4a,
        0x0d4d, 0x0d4e,
        0x0d62, 0x0d66,
        0x0d80, 0x0d82,
        0x0d84, 0x0d85,
        0x0d97, 0x0d9a,
        0x0db2, 0x0db3,
        0x0dbc, 0x0dbd,
        0x0dbe, 0x0dc0,
        0x0dc7, 0x0dcf,
        0x0dd2, 0x0dd8,
        0x0de0, 0x0de6,
        0x0df0, 0x0df2,
        0x0df5, 0x0e01,
        0x0e31, 0x0e32,
        0x0e34, 0x0e40,
        0x0e47, 0x0e4f,
        0x0e5c, 0x0e81,
        0x0e83, 0x0e84,
        0x0e85, 0x0e87,
        0x0e89, 0x0e8a,
        0x0e8b, 0x0e8d,
        0x0e8e, 0x0e94,
        0x0e98, 0x0e99,
        0x0ea0, 0x0ea1,
        0x0ea4, 0x0ea5,
        0x0ea6, 0x0ea7,
        0x0ea8, 0x0eaa,
        0x0eac, 0x0ead,
        0x0eb1, 0x0eb2,
        0x0eb4, 0x0ebd,
        0x0ebe, 0x0ec0,
        0x0ec5, 0x0ec6,
        0x0ec7, 0x0ed0,
        0x0eda, 0x0edc,
        0x0ee0, 0x0f00,
        0x0f18, 0x0f1a,
        0x0f35, 0x0f36,
        0x0f37, 0x0f38,
        0x0f39, 0x0f3e,
        0x0f48, 0x0f49,
        0x0f6d, 0x0f7f,
        0x0f80, 0x0f85,
        0x0f86, 0x0f88,
        0x0f8d, 0x0fbe,
        0x0fc6, 0x0fc7,
        0x0fcd, 0x0fce,
        0x0fdb, 0x1000,
        0x102d, 0x1031,
        0x1032, 0x1038,
        0x1039, 0x103b,
        0x103d, 0x103f,
        0x1058, 0x105a,
        0x105e, 0x1061,
        0x1071, 0x1075,
        0x1082, 0x1083,
        0x1085, 0x1087,
        0x108d, 0x108e,
        0x109d, 0x109e,
        0x10c6, 0x10c7,
        0x10c8, 0x10cd,
        0x10ce, 0x10d0,
        0x1249, 0x124a,
        0x124e, 0x1250,
        0x1257, 0x1258,
        0x1259, 0x125a,
        0x125e, 0x1260,
        0x1289, 0x128a,
        0x128e, 0x1290,
        0x12b1, 0x12b2,
        0x12b6, 0x12b8,
        0x12bf, 0x12c0,
        0x12c1, 0x12c2,
        0x12c6, 0x12c8,
        0x12d7, 0x12d8,
        0x1311, 0x1312,
        0x1316, 0x1318,
        0x135b, 0x1360,
        0x137d, 0x1380,
        0x1390, 0x13a0,
        0x13f6, 0x13f8,
        0x13fe, 0x1401,
        0x1680, 0x1681,
        0x169b, 0x16a0,
        0x16f9, 0x1700,
        0x170d, 0x170e,
        0x1712, 0x1720,
        0x1732, 0x1735,
        0x1737, 0x1740,
        0x1752, 0x1760,
        0x176d, 0x176e,
        0x1771, 0x1780,
        0x17b4, 0x17b6,
        0x17b7, 0x17be,
        0x17c6, 0x17c7,
        0x17c9, 0x17d4,
        0x17db, 0x17dc,
        0x17dd, 0x17e0,
        0x17ea, 0x1810,
        0x181a, 0x1820,
        0x1879, 0x1884,
        0x1885, 0x1887,
        0x18a9, 0x18aa,
        0x18ab, 0x18b0,
        0x18f6, 0x1900,
        0x191f, 0x1923,
        0x1927, 0x1929,
        0x192c, 0x1930,
        0x1932, 0x1933,
        0x1939, 0x1946,
        0x196e, 0x1970,
        0x1975, 0x1980,
        0x19ac, 0x19b0,
        0x19ca, 0x19d0,
        0x19db, 0x1a00,
        0x1a17, 0x1a19,
        0x1a1b, 0x1a1e,
        0x1a56, 0x1a57,
        0x1a58, 0x1a61,
        0x1a62, 0x1a63,
        0x1a65, 0x1a6d,
        0x1a73, 0x1a80,
        0x1a8a, 0x1a90,
        0x1a9a, 0x1aa0,
        0x1aae, 0x1b04,
        0x1b34, 0x1b35,
        0x1b36, 0x1b3b,
        0x1b3c, 0x1b3d,
        0x1b42, 0x1b43,
        0x1b4c, 0x1b50,
        0x1b6b, 0x1b74,
        0x1b7d, 0x1b82,
        0x1ba2, 0x1ba6,
        0x1ba8, 0x1baa,
        0x1bab, 0x1bae,
        0x1be6, 0x1be7,
        0x1be8, 0x1bea,
        0x1bed, 0x1bee,
        0x1bef, 0x1bf2,
        0x1bf4, 0x1bfc,
        0x1c2c, 0x1c34,
        0x1c36, 0x1c3b,
        0x1c4a, 0x1c4d,
        0x1c89, 0x1c90,
        0x1cbb, 0x1cbd,
        0x1cc8, 0x1cd3,
        0x1cd4, 0x1ce1,
        0x1ce2, 0x1ce9,
        0x1ced, 0x1cee,
        0x1cf4, 0x1cf5,
        0x1cf8, 0x1d00,
        0x1dc0, 0x1e00,
        0x1f16, 0x1f18,
        0x1f1e, 0x1f20,
        0x1f46, 0x1f48,
        0x1f4e, 0x1f50,
        0x1f58, 0x1f59,
        0x1f5a, 0x1f5b,
        0x1f5c, 0x1f5d,
        0x1f5e, 0x1f5f,
        0x1f7e, 0x1f80,
        0x1fb5, 0x1fb6,
        0x1fbd, 0x1fbe,
        0x1fbf, 0x1fc2,
        0x1fc5, 0x1fc6,
        0x1fcd, 0x1fd0,
        0x1fd4, 0x1fd6,
        0x1fdc, 0x1fe0,
        0x1fed, 0x1ff2,
        0x1ff5, 0x1ff6,
        0x1ffd, 0x200e,
        0x2010, 0x2071,
        0x2072, 0x207f,
        0x2080, 0x2090,
        0x209d, 0x2102,
        0x2103, 0x2107,
        0x2108, 0x210a,
        0x2114, 0x2115,
        0x2116, 0x2119,
        0x211e, 0x2124,
        0x2125, 0x2126,
        0x2127, 0x2128,
        0x2129, 0x212a,
        0x212e, 0x212f,
        0x213a, 0x213c,
        0x2140, 0x2145,
        0x214a, 0x214e,
        0x2150, 0x2160,
        0x2189, 0x2336,
        0x237b, 0x2395,
        0x2396, 0x249c,
        0x24ea, 0x26ac,
        0x26ad, 0x2800,
        0x2900, 0x2c00,
        0x2c2f, 0x2c30,
        0x2c5f, 0x2c60,
        0x2ce5, 0x2ceb,
        0x2cef, 0x2cf2,
        0x2cf4, 0x2d00,
        0x2d26, 0x2d27,
        0x2d28, 0x2d2d,
        0x2d2e, 0x2d30,
        0x2d68, 0x2d6f,
        0x2d71, 0x2d80,
        0x2d97, 0x2da0,
        0x2da7, 0x2da8,
        0x2daf, 0x2db0,
        0x2db7, 0x2db8,
        0x2dbf, 0x2dc0,
        0x2dc7, 0x2dc8,
        0x2dcf, 0x2dd0,
        0x2dd7, 0x2dd8,
        0x2ddf, 0x3005,
        0x3008, 0x3021,
        0x302a, 0x302e,
        0x3030, 0x3031,
        0x3036, 0x3038,
        0x303d, 0x3041,
        0x3097, 0x309d,
        0x30a0, 0x30a1,
        0x30fb, 0x30fc,
        0x3100, 0x3105,
        0x3130, 0x3131,
        0x318f, 0x3190,
        0x31bb, 0x31f0,
        0x321d, 0x3220,
        0x3250, 0x3260,
        0x327c, 0x327f,
        0x32b1, 0x32c0,
        0x32cc, 0x32d0,
        0x32ff, 0x3300,
        0x3377, 0x337b,
        0x33de, 0x33e0,
        0x33ff, 0x3400,
        0x4db6, 0x4e00,
        0x9ff0, 0xa000,
        0xa48d, 0xa4d0,
        0xa60d, 0xa610,
        0xa62c, 0xa640,
        0xa66f, 0xa680,
        0xa69e, 0xa6a0,
        0xa6f0, 0xa6f2,
        0xa6f8, 0xa722,
        0xa788, 0xa789,
        0xa7ba, 0xa7f7,
        0xa802, 0xa803,
        0xa806, 0xa807,
        0xa80b, 0xa80c,
        0xa825, 0xa827,
        0xa828, 0xa830,
        0xa838, 0xa840,
        0xa874, 0xa880,
        0xa8c4, 0xa8ce,
        0xa8da, 0xa8f2,
        0xa8ff, 0xa900,
        0xa926, 0xa92e,
        0xa947, 0xa952,
        0xa954, 0xa95f,
        0xa97d, 0xa983,
        0xa9b3, 0xa9b4,
        0xa9b6, 0xa9ba,
        0xa9bc, 0xa9bd,
        0xa9ce, 0xa9cf,
        0xa9da, 0xa9de,
        0xa9e5, 0xa9e6,
        0xa9ff, 0xaa00,
        0xaa29, 0xaa2f,
        0xaa31, 0xaa33,
        0xaa35, 0xaa40,
        0xaa43, 0xaa44,
        0xaa4c, 0xaa4d,
        0xaa4e, 0xaa50,
        0xaa5a, 0xaa5c,
        0xaa7c, 0xaa7d,
        0xaab0, 0xaab1,
        0xaab2, 0xaab5,
        0xaab7, 0xaab9,
        0xaabe, 0xaac0,
        0xaac1, 0xaac2,
        0xaac3, 0xaadb,
        0xaaec, 0xaaee,
        0xaaf6, 0xab01,
        0xab07, 0xab09,
        0xab0f, 0xab11,
        0xab17, 0xab20,
        0xab27, 0xab28,
        0xab2f, 0xab30,
        0xab66, 0xab70,
        0xabe5, 0xabe6,
        0xabe8, 0xabe9,
        0xabed, 0xabf0,
        0xabfa, 0xac00,
        0xd7a4, 0xd7b0,
        0xd7c7, 0xd7cb,
        0xd7fc, 0xe000,
        0xfa6e, 0xfa70,
        0xfada, 0xfb00,
        0xfb07, 0xfb13,
        0xfb18, 0xfb1d,
        0xfb1e, 0xfb1f,
        0xfb29, 0xfb2a,
        0xfd3e, 0xfd40,
        0xfdd0, 0xfdf0,
        0xfdfd, 0xfdfe,
        0xfe00, 0xfe70,
        0xfeff, 0xff21,
        0xff3b, 0xff41,
        0xff5b, 0xff66,
        0xffbf, 0xffc2,
        0xffc8, 0xffca,
        0xffd0, 0xffd2,
        0xffd8, 0xffda,
        0xffdd, 0x10000,
        0x1000c, 0x1000d,
        0x10027, 0x10028,
        0x1003b, 0x1003c,
        0x1003e, 0x1003f,
        0x1004e, 0x10050,
        0x1005e, 0x10080,
        0x100fb, 0x10100,
        0x10101, 0x10102,
        0x10103, 0x10107,
        0x10134, 0x10137,
        0x10140, 0x1018d,
        0x1018f, 0x101d0,
        0x101fd, 0x10280,
        0x1029d, 0x102a0,
        0x102d1, 0x10300,
        0x10324, 0x1032d,
        0x1034b, 0x10350,
        0x10376, 0x10380,
        0x1039e, 0x1039f,
        0x103c4, 0x103c8,
        0x103d6, 0x10400,
        0x1049e, 0x104a0,
        0x104aa, 0x104d3,
        0x104d4, 0x104d8,
        0x104fc, 0x10500,
        0x10528, 0x10530,
        0x10564, 0x1056f,
        0x10570, 0x10600,
        0x10737, 0x10740,
        0x10756, 0x10760,
        0x10768, 0x10800,
        0x1091f, 0x10920,
        0x10a01, 0x10a04,
        0x10a05, 0x10a07,
        0x10a0c, 0x10a10,
        0x10a38, 0x10a3b,
        0x10a3f, 0x10a40,
        0x10ae5, 0x10ae7,
        0x10b39, 0x10b40,
        0x10d00, 0x10d40,
        0x10e60, 0x10e7f,
        0x10f30, 0x10f70,
        0x11001, 0x11002,
        0x11038, 0x11047,
        0x1104e, 0x11066,
        0x11070, 0x11082,
        0x110b3, 0x110b7,
        0x110b9, 0x110bb,
        0x110c2, 0x110cd,
        0x110ce, 0x110d0,
        0x110e9, 0x110f0,
        0x110fa, 0x11103,
        0x11127, 0x1112c,
        0x1112d, 0x11136,
        0x11147, 0x11150,
        0x11173, 0x11174,
        0x11177, 0x11182,
        0x111b6, 0x111bf,
        0x111c9, 0x111cd,
        0x111ce, 0x111d0,
        0x111e0, 0x111e1,
        0x111f5, 0x11200,
        0x11212, 0x11213,
        0x1122f, 0x11232,
        0x11234, 0x11235,
        0x11236, 0x11238,
        0x1123e, 0x11280,
        0x11287, 0x11288,
        0x11289, 0x1128a,
        0x1128e, 0x1128f,
        0x1129e, 0x1129f,
        0x112aa, 0x112b0,
        0x112df, 0x112e0,
        0x112e3, 0x112f0,
        0x112fa, 0x11302,
        0x11304, 0x11305,
        0x1130d, 0x1130f,
        0x11311, 0x11313,
        0x11329, 0x1132a,
        0x11331, 0x11332,
        0x11334, 0x11335,
        0x1133a, 0x1133d,
        0x11340, 0x11341,
        0x11345, 0x11347,
        0x11349, 0x1134b,
        0x1134e, 0x11350,
        0x11351, 0x11357,
        0x11358, 0x1135d,
        0x11364, 0x11400,
        0x11438, 0x11440,
        0x11442, 0x11445,
        0x11446, 0x11447,
        0x1145a, 0x1145b,
        0x1145c, 0x1145d,
        0x1145e, 0x11480,
        0x114b3, 0x114b9,
        0x114ba, 0x114bb,
        0x114bf, 0x114c1,
        0x114c2, 0x114c4,
        0x114c8, 0x114d0,
        0x114da, 0x11580,
        0x115b2, 0x115b8,
        0x115bc, 0x115be,
        0x115bf, 0x115c1,
        0x115dc, 0x11600,
        0x11633, 0x1163b,
        0x1163d, 0x1163e,
        0x1163f, 0x11641,
        0x11645, 0x11650,
        0x1165a, 0x11680,
        0x116ab, 0x116ac,
        0x116ad, 0x116ae,
        0x116b0, 0x116b6,
        0x116b7, 0x116c0,
        0x116ca, 0x11700,
        0x1171b, 0x11720,
        0x11722, 0x11726,
        0x11727, 0x11730,
        0x1182f, 0x11838,
        0x11839, 0x1183b,
        0x1183c, 0x118a0,
        0x118f3, 0x118ff,
        0x11900, 0x11a00,
        0x11a01, 0x11a07,
        0x11a09, 0x11a0b,
        0x11a33, 0x11a3a,
        0x11a3b, 0x11a3f,
        0x11a47, 0x11a50,
        0x11a51, 0x11a57,
        0x11a59, 0x11a5c,
        0x11a84, 0x11a86,
        0x11a8a, 0x11a97,
        0x11a98, 0x11a9a,
        0x11aa3, 0x11ac0,
        0x11af9, 0x11c00,
        0x11c09, 0x11c0a,
        0x11c30, 0x11c3e,
        0x11c46, 0x11c50,
        0x11c6d, 0x11c70,
        0x11c90, 0x11ca9,
        0x11caa, 0x11cb1,
        0x11cb2, 0x11cb4,
        0x11cb5, 0x11d00,
        0x11d07, 0x11d08,
        0x11d0a, 0x11d0b,
        0x11d31, 0x11d46,
        0x11d47, 0x11d50,
        0x11d5a, 0x11d60,
        0x11d66, 0x11d67,
        0x11d69, 0x11d6a,
        0x11d8f, 0x11d93,
        0x11d95, 0x11d96,
        0x11d97, 0x11d98,
        0x11d99, 0x11da0,
        0x11daa, 0x11ee0,
        0x11ef3, 0x11ef5,
        0x11ef9, 0x12000,
        0x1239a, 0x12400,
        0x1246f, 0x12470,
        0x12475, 0x12480,
        0x12544, 0x13000,
        0x1342f, 0x14400,
        0x14647, 0x16800,
        0x16a39, 0x16a40,
        0x16a5f, 0x16a60,
        0x16a6a, 0x16a6e,
        0x16a70, 0x16ad0,
        0x16aee, 0x16af5,
        0x16af6, 0x16b00,
        0x16b30, 0x16b37,
        0x16b46, 0x16b50,
        0x16b5a, 0x16b5b,
        0x16b62, 0x16b63,
        0x16b78, 0x16b7d,
        0x16b90, 0x16e40,
        0x16e9b, 0x16f00,
        0x16f45, 0x16f50,
        0x16f7f, 0x16f93,
        0x16fa0, 0x16fe0,
        0x16fe2, 0x17000,
        0x187f2, 0x18800,
        0x18af3, 0x1b000,
        0x1b11f, 0x1b170,
        0x1b2fc, 0x1bc00,
        0x1bc6b, 0x1bc70,
        0x1bc7d, 0x1bc80,
        0x1bc89, 0x1bc90,
        0x1bc9a, 0x1bc9c,
        0x1bc9d, 0x1bc9f,
        0x1bca0, 0x1d000,
        0x1d0f6, 0x1d100,
        0x1d127, 0x1d129,
        0x1d167, 0x1d16a,
        0x1d173, 0x1d183,
        0x1d185, 0x1d18c,
        0x1d1aa, 0x1d1ae,
        0x1d1e9, 0x1d2e0,
        0x1d2f4, 0x1d360,
        0x1d379, 0x1d400,
        0x1d455, 0x1d456,
        0x1d49d, 0x1d49e,
        0x1d4a0, 0x1d4a2,
        0x1d4a3, 0x1d4a5,
        0x1d4a7, 0x1d4a9,
        0x1d4ad, 0x1d4ae,
        0x1d4ba, 0x1d4bb,
        0x1d4bc, 0x1d4bd,
        0x1d4c4, 0x1d4c5,
        0x1d506, 0x1d507,
        0x1d50b, 0x1d50d,
        0x1d515, 0x1d516,
        0x1d51d, 0x1d51e,
        0x1d53a, 0x1d53b,
        0x1d53f, 0x1d540,
        0x1d545, 0x1d546,
        0x1d547, 0x1d54a,
        0x1d551, 0x1d552,
        0x1d6a6, 0x1d6a8,
        0x1d6db, 0x1d6dc,
        0x1d715, 0x1d716,
        0x1d74f, 0x1d750,
        0x1d789, 0x1d78a,
        0x1d7c3, 0x1d7c4,
        0x1d7cc, 0x1d800,
        0x1da00, 0x1da37,
        0x1da3b, 0x1da6d,
        0x1da75, 0x1da76,
        0x1da84, 0x1da85,
        0x1da8c, 0x1e800,
        0x1e8d0, 0x1e8d7,
        0x1e944, 0x1e94b,
        0x1ec70, 0x1ecc0,
        0x1ee00, 0x1ef00,
        0x1f000, 0x1f110,
        0x1f12f, 0x1f130,
        0x1f16a, 0x1f170,
        0x1f1ad, 0x1f1e6,
        0x1f203, 0x1f210,
        0x1f23c, 0x1f240,
        0x1f249, 0x1f250,
        0x1f252, 0x20000,
        0x2a6d7, 0x2a700,
        0x2b735, 0x2b740,
        0x2b81e, 0x2b820,
        0x2cea2, 0x2ceb0,
        0x2ebe1, 0x2f800,
        0x2fa1e, 0xf0000,
        0xffffe, 0x100000,
        0x10fffe, 0x10ffff // sentinel
    };


    // use a binary search with a cache

    private transient volatile int stCache = 0;

    private boolean isStrongDirectional(char c) {
        int cachedIndex = stCache;
        if (c < strongTable[cachedIndex]) {
            cachedIndex = search(c, strongTable, 0, cachedIndex);
        } else if (c >= strongTable[cachedIndex + 1]) {
            cachedIndex = search(c, strongTable, cachedIndex + 1,
                                 strongTable.length - cachedIndex - 1);
        }
        boolean val = (cachedIndex & 0x1) == 1;
        stCache = cachedIndex;
        return val;
    }

    private static int getKeyFromMask(int mask) {
        int key = 0;
        while (key < NUM_KEYS && ((mask & (1<<key)) == 0)) {
            ++key;
        }
        if (key == NUM_KEYS || ((mask & ~(1<<key)) != 0)) {
            throw new IllegalArgumentException("invalid shaper: " + Integer.toHexString(mask));
        }
        return key;
    }

    /**
     * Returns a shaper for the provided unicode range.  All
     * Latin-1 (EUROPEAN) digits are converted
     * to the corresponding decimal unicode digits.
     * @param singleRange the specified Unicode range
     * @return a non-contextual numeric shaper
     * @throws IllegalArgumentException if the range is not a single range
     */
    public static NumericShaper getShaper(int singleRange) {
        int key = getKeyFromMask(singleRange);
        return new NumericShaper(key, singleRange);
    }

    /**
     * Returns a shaper for the provided Unicode
     * range. All Latin-1 (EUROPEAN) digits are converted to the
     * corresponding decimal digits of the specified Unicode range.
     *
     * @param singleRange the Unicode range given by a {@link
     *                    NumericShaper.Range} constant.
     * @return a non-contextual {@code NumericShaper}.
     * @throws NullPointerException if {@code singleRange} is {@code null}
     * @since 1.7
     */
    public static NumericShaper getShaper(Range singleRange) {
        return new NumericShaper(singleRange, EnumSet.of(singleRange));
    }

    /**
     * Returns a contextual shaper for the provided unicode range(s).
     * Latin-1 (EUROPEAN) digits are converted to the decimal digits
     * corresponding to the range of the preceding text, if the
     * range is one of the provided ranges.  Multiple ranges are
     * represented by or-ing the values together, such as,
     * {@code NumericShaper.ARABIC | NumericShaper.THAI}.  The
     * shaper assumes EUROPEAN as the starting context, that is, if
     * EUROPEAN digits are encountered before any strong directional
     * text in the string, the context is presumed to be EUROPEAN, and
     * so the digits will not shape.
     * @param ranges the specified Unicode ranges
     * @return a shaper for the specified ranges
     */
    public static NumericShaper getContextualShaper(int ranges) {
        ranges |= CONTEXTUAL_MASK;
        return new NumericShaper(EUROPEAN_KEY, ranges);
    }

    /**
     * Returns a contextual shaper for the provided Unicode
     * range(s). The Latin-1 (EUROPEAN) digits are converted to the
     * decimal digits corresponding to the range of the preceding
     * text, if the range is one of the provided ranges.
     *
     * <p>The shaper assumes EUROPEAN as the starting context, that
     * is, if EUROPEAN digits are encountered before any strong
     * directional text in the string, the context is presumed to be
     * EUROPEAN, and so the digits will not shape.
     *
     * @param ranges the specified Unicode ranges
     * @return a contextual shaper for the specified ranges
     * @throws NullPointerException if {@code ranges} is {@code null}.
     * @since 1.7
     */
    public static NumericShaper getContextualShaper(Set<Range> ranges) {
        NumericShaper shaper = new NumericShaper(Range.EUROPEAN, ranges);
        shaper.mask = CONTEXTUAL_MASK;
        return shaper;
    }

    /**
     * Returns a contextual shaper for the provided unicode range(s).
     * Latin-1 (EUROPEAN) digits will be converted to the decimal digits
     * corresponding to the range of the preceding text, if the
     * range is one of the provided ranges.  Multiple ranges are
     * represented by or-ing the values together, for example,
     * {@code NumericShaper.ARABIC | NumericShaper.THAI}.  The
     * shaper uses defaultContext as the starting context.
     * @param ranges the specified Unicode ranges
     * @param defaultContext the starting context, such as
     * {@code NumericShaper.EUROPEAN}
     * @return a shaper for the specified Unicode ranges.
     * @throws IllegalArgumentException if the specified
     * {@code defaultContext} is not a single valid range.
     */
    public static NumericShaper getContextualShaper(int ranges, int defaultContext) {
        int key = getKeyFromMask(defaultContext);
        ranges |= CONTEXTUAL_MASK;
        return new NumericShaper(key, ranges);
    }

    /**
     * Returns a contextual shaper for the provided Unicode range(s).
     * The Latin-1 (EUROPEAN) digits will be converted to the decimal
     * digits corresponding to the range of the preceding text, if the
     * range is one of the provided ranges. The shaper uses {@code
     * defaultContext} as the starting context.
     *
     * @param ranges the specified Unicode ranges
     * @param defaultContext the starting context, such as
     *                       {@code NumericShaper.Range.EUROPEAN}
     * @return a contextual shaper for the specified Unicode ranges.
     * @throws NullPointerException
     *         if {@code ranges} or {@code defaultContext} is {@code null}
     * @since 1.7
     */
    public static NumericShaper getContextualShaper(Set<Range> ranges,
                                                    Range defaultContext) {
        if (defaultContext == null) {
            throw new NullPointerException();
        }
        NumericShaper shaper = new NumericShaper(defaultContext, ranges);
        shaper.mask = CONTEXTUAL_MASK;
        return shaper;
    }

    /**
     * Private constructor.
     */
    private NumericShaper(int key, int mask) {
        this.key = key;
        this.mask = mask;
    }

    private NumericShaper(Range defaultContext, Set<Range> ranges) {
        shapingRange = defaultContext;
        rangeSet = EnumSet.copyOf(ranges); // throws NPE if ranges is null.

        // Give precedence to EASTERN_ARABIC if both ARABIC and
        // EASTERN_ARABIC are specified.
        if (rangeSet.contains(Range.EASTERN_ARABIC)
            && rangeSet.contains(Range.ARABIC)) {
            rangeSet.remove(Range.ARABIC);
        }

        // As well as the above case, give precedence to TAI_THAM_THAM if both
        // TAI_THAM_HORA and TAI_THAM_THAM are specified.
        if (rangeSet.contains(Range.TAI_THAM_THAM)
            && rangeSet.contains(Range.TAI_THAM_HORA)) {
            rangeSet.remove(Range.TAI_THAM_HORA);
        }

        rangeArray = rangeSet.toArray(new Range[rangeSet.size()]);
        if (rangeArray.length > BSEARCH_THRESHOLD) {
            // sort rangeArray for binary search
            Arrays.sort(rangeArray,
                        new Comparator<Range>() {
                            public int compare(Range s1, Range s2) {
                                return s1.base > s2.base ? 1 : s1.base == s2.base ? 0 : -1;
                            }
                        });
        }
    }

    /**
     * Converts the digits in the text that occur between start and
     * start + count.
     * @param text an array of characters to convert
     * @param start the index into {@code text} to start
     *        converting
     * @param count the number of characters in {@code text}
     *        to convert
     * @throws IndexOutOfBoundsException if start or start + count is
     *        out of bounds
     * @throws NullPointerException if text is null
     */
    public void shape(char[] text, int start, int count) {
        checkParams(text, start, count);
        if (isContextual()) {
            if (rangeSet == null) {
                shapeContextually(text, start, count, key);
            } else {
                shapeContextually(text, start, count, shapingRange);
            }
        } else {
            shapeNonContextually(text, start, count);
        }
    }

    /**
     * Converts the digits in the text that occur between start and
     * start + count, using the provided context.
     * Context is ignored if the shaper is not a contextual shaper.
     * @param text an array of characters
     * @param start the index into {@code text} to start
     *        converting
     * @param count the number of characters in {@code text}
     *        to convert
     * @param context the context to which to convert the
     *        characters, such as {@code NumericShaper.EUROPEAN}
     * @throws IndexOutOfBoundsException if start or start + count is
     *        out of bounds
     * @throws NullPointerException if text is null
     * @throws IllegalArgumentException if this is a contextual shaper
     * and the specified {@code context} is not a single valid
     * range.
     */
    public void shape(char[] text, int start, int count, int context) {
        checkParams(text, start, count);
        if (isContextual()) {
            int ctxKey = getKeyFromMask(context);
            if (rangeSet == null) {
                shapeContextually(text, start, count, ctxKey);
            } else {
                shapeContextually(text, start, count, Range.values()[ctxKey]);
            }
        } else {
            shapeNonContextually(text, start, count);
        }
    }

    /**
     * Converts the digits in the text that occur between {@code
     * start} and {@code start + count}, using the provided {@code
     * context}. {@code Context} is ignored if the shaper is not a
     * contextual shaper.
     *
     * @param text  a {@code char} array
     * @param start the index into {@code text} to start converting
     * @param count the number of {@code char}s in {@code text}
     *              to convert
     * @param context the context to which to convert the characters,
     *                such as {@code NumericShaper.Range.EUROPEAN}
     * @throws IndexOutOfBoundsException
     *         if {@code start} or {@code start + count} is out of bounds
     * @throws NullPointerException
     *         if {@code text} or {@code context} is null
     * @since 1.7
     */
    public void shape(char[] text, int start, int count, Range context) {
        checkParams(text, start, count);
        if (context == null) {
            throw new NullPointerException("context is null");
        }

        if (isContextual()) {
            if (rangeSet != null) {
                shapeContextually(text, start, count, context);
            } else {
                int key = Range.toRangeIndex(context);
                if (key >= 0) {
                    shapeContextually(text, start, count, key);
                } else {
                    shapeContextually(text, start, count, shapingRange);
                }
            }
        } else {
            shapeNonContextually(text, start, count);
        }
    }

    private void checkParams(char[] text, int start, int count) {
        if (text == null) {
            throw new NullPointerException("text is null");
        }
        if ((start < 0)
            || (start > text.length)
            || ((start + count) < 0)
            || ((start + count) > text.length)) {
            throw new IndexOutOfBoundsException(
                "bad start or count for text of length " + text.length);
        }
    }

    /**
     * Returns a {@code boolean} indicating whether or not
     * this shaper shapes contextually.
     * @return {@code true} if this shaper is contextual;
     *         {@code false} otherwise.
     */
    public boolean isContextual() {
        return (mask & CONTEXTUAL_MASK) != 0;
    }

    /**
     * Returns an {@code int} that ORs together the values for
     * all the ranges that will be shaped.
     * <p>
     * For example, to check if a shaper shapes to Arabic, you would use the
     * following:
     * <blockquote>
     *   {@code if ((shaper.getRanges() & shaper.ARABIC) != 0) &#123; ... }
     * </blockquote>
     *
     * <p>Note that this method supports only the bit mask-based
     * ranges. Call {@link #getRangeSet()} for the enum-based ranges.
     *
     * @return the values for all the ranges to be shaped.
     */
    public int getRanges() {
        return mask & ~CONTEXTUAL_MASK;
    }

    /**
     * Returns a {@code Set} representing all the Unicode ranges in
     * this {@code NumericShaper} that will be shaped.
     *
     * @return all the Unicode ranges to be shaped.
     * @since 1.7
     */
    public Set<Range> getRangeSet() {
        if (rangeSet != null) {
            return EnumSet.copyOf(rangeSet);
        }
        return Range.maskToRangeSet(mask);
    }

    /**
     * Perform non-contextual shaping.
     */
    private void shapeNonContextually(char[] text, int start, int count) {
        int base;
        char minDigit = '0';
        if (shapingRange != null) {
            base = shapingRange.getDigitBase();
            minDigit += shapingRange.getNumericBase();
        } else {
            base = bases[key];
            if (key == ETHIOPIC_KEY) {
                minDigit++; // Ethiopic doesn't use decimal zero
            }
        }
        for (int i = start, e = start + count; i < e; ++i) {
            char c = text[i];
            if (c >= minDigit && c <= '\u0039') {
                text[i] = (char)(c + base);
            }
        }
    }

    /**
     * Perform contextual shaping.
     * Synchronized to protect caches used in getContextKey.
     */
    private synchronized void shapeContextually(char[] text, int start, int count, int ctxKey) {

        // if we don't support this context, then don't shape
        if ((mask & (1<<ctxKey)) == 0) {
            ctxKey = EUROPEAN_KEY;
        }
        int lastkey = ctxKey;

        int base = bases[ctxKey];
        char minDigit = ctxKey == ETHIOPIC_KEY ? '1' : '0'; // Ethiopic doesn't use decimal zero

        synchronized (NumericShaper.class) {
            for (int i = start, e = start + count; i < e; ++i) {
                char c = text[i];
                if (c >= minDigit && c <= '\u0039') {
                    text[i] = (char)(c + base);
                }

                if (isStrongDirectional(c)) {
                    int newkey = getContextKey(c);
                    if (newkey != lastkey) {
                        lastkey = newkey;

                        ctxKey = newkey;
                        if (((mask & EASTERN_ARABIC) != 0) &&
                             (ctxKey == ARABIC_KEY ||
                              ctxKey == EASTERN_ARABIC_KEY)) {
                            ctxKey = EASTERN_ARABIC_KEY;
                        } else if (((mask & ARABIC) != 0) &&
                             (ctxKey == ARABIC_KEY ||
                              ctxKey == EASTERN_ARABIC_KEY)) {
                            ctxKey = ARABIC_KEY;
                        } else if ((mask & (1<<ctxKey)) == 0) {
                            ctxKey = EUROPEAN_KEY;
                        }

                        base = bases[ctxKey];

                        minDigit = ctxKey == ETHIOPIC_KEY ? '1' : '0'; // Ethiopic doesn't use decimal zero
                    }
                }
            }
        }
    }

    private void shapeContextually(char[] text, int start, int count, Range ctxKey) {
        // if we don't support the specified context, then don't shape.
        if (ctxKey == null || !rangeSet.contains(ctxKey)) {
            ctxKey = Range.EUROPEAN;
        }

        Range lastKey = ctxKey;
        int base = ctxKey.getDigitBase();
        char minDigit = (char)('0' + ctxKey.getNumericBase());
        final int end = start + count;
        for (int i = start; i < end; ++i) {
            char c = text[i];
            if (c >= minDigit && c <= '9') {
                text[i] = (char)(c + base);
                continue;
            }
            if (isStrongDirectional(c)) {
                ctxKey = rangeForCodePoint(c);
                if (ctxKey != lastKey) {
                    lastKey = ctxKey;
                    base = ctxKey.getDigitBase();
                    minDigit = (char)('0' + ctxKey.getNumericBase());
                }
            }
        }
    }

    /**
     * Returns a hash code for this shaper.
     * @return this shaper's hash code.
     * @see java.lang.Object#hashCode
     */
    public int hashCode() {
        int hash = mask;
        if (rangeSet != null) {
            // Use the CONTEXTUAL_MASK bit only for the enum-based
            // NumericShaper. A deserialized NumericShaper might have
            // bit masks.
            hash &= CONTEXTUAL_MASK;
            hash ^= rangeSet.hashCode();
        }
        return hash;
    }

    /**
     * Returns {@code true} if the specified object is an instance of
     * {@code NumericShaper} and shapes identically to this one,
     * regardless of the range representations, the bit mask or the
     * enum. For example, the following code produces {@code "true"}.
     * <blockquote><pre>
     * NumericShaper ns1 = NumericShaper.getShaper(NumericShaper.ARABIC);
     * NumericShaper ns2 = NumericShaper.getShaper(NumericShaper.Range.ARABIC);
     * System.out.println(ns1.equals(ns2));
     * </pre></blockquote>
     *
     * @param o the specified object to compare to this
     *          {@code NumericShaper}
     * @return {@code true} if {@code o} is an instance
     *         of {@code NumericShaper} and shapes in the same way;
     *         {@code false} otherwise.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (o != null) {
            try {
                NumericShaper rhs = (NumericShaper)o;
                if (rangeSet != null) {
                    if (rhs.rangeSet != null) {
                        return isContextual() == rhs.isContextual()
                            && rangeSet.equals(rhs.rangeSet)
                            && shapingRange == rhs.shapingRange;
                    }
                    return isContextual() == rhs.isContextual()
                        && rangeSet.equals(Range.maskToRangeSet(rhs.mask))
                        && shapingRange == Range.indexToRange(rhs.key);
                } else if (rhs.rangeSet != null) {
                    Set<Range> rset = Range.maskToRangeSet(mask);
                    Range srange = Range.indexToRange(key);
                    return isContextual() == rhs.isContextual()
                        && rset.equals(rhs.rangeSet)
                        && srange == rhs.shapingRange;
                }
                return rhs.mask == mask && rhs.key == key;
            }
            catch (ClassCastException e) {
            }
        }
        return false;
    }

    /**
     * Returns a {@code String} that describes this shaper. This method
     * is used for debugging purposes only.
     * @return a {@code String} describing this shaper.
     */
    public String toString() {
        StringBuilder buf = new StringBuilder(super.toString());

        buf.append("[contextual:").append(isContextual());

        String[] keyNames = null;
        if (isContextual()) {
            buf.append(", context:");
            buf.append(shapingRange == null ? Range.values()[key] : shapingRange);
        }

        if (rangeSet == null) {
            buf.append(", range(s): ");
            boolean first = true;
            for (int i = 0; i < NUM_KEYS; ++i) {
                if ((mask & (1 << i)) != 0) {
                    if (first) {
                        first = false;
                    } else {
                        buf.append(", ");
                    }
                    buf.append(Range.values()[i]);
                }
            }
        } else {
            buf.append(", range set: ").append(rangeSet);
        }
        buf.append(']');

        return buf.toString();
    }

    /**
     * Returns the index of the high bit in value (assuming le, actually
     * power of 2 >= value). value must be positive.
     */
    private static int getHighBit(int value) {
        if (value <= 0) {
            return -32;
        }

        int bit = 0;

        if (value >= 1 << 16) {
            value >>= 16;
            bit += 16;
        }

        if (value >= 1 << 8) {
            value >>= 8;
            bit += 8;
        }

        if (value >= 1 << 4) {
            value >>= 4;
            bit += 4;
        }

        if (value >= 1 << 2) {
            value >>= 2;
            bit += 2;
        }

        if (value >= 1 << 1) {
            bit += 1;
        }

        return bit;
    }

    /**
     * fast binary search over subrange of array.
     */
    private static int search(int value, int[] array, int start, int length)
    {
        int power = 1 << getHighBit(length);
        int extra = length - power;
        int probe = power;
        int index = start;

        if (value >= array[index + extra]) {
            index += extra;
        }

        while (probe > 1) {
            probe >>= 1;

            if (value >= array[index + probe]) {
                index += probe;
            }
        }

        return index;
    }

    /**
     * Converts the {@code NumericShaper.Range} enum-based parameters,
     * if any, to the bit mask-based counterparts and writes this
     * object to the {@code stream}. Any enum constants that have no
     * bit mask-based counterparts are ignored in the conversion.
     *
     * @param stream the output stream to write to
     * @throws IOException if an I/O error occurs while writing to {@code stream}
     * @since 1.7
     */
    @Serial
    private void writeObject(ObjectOutputStream stream) throws IOException {
        if (shapingRange != null) {
            int index = Range.toRangeIndex(shapingRange);
            if (index >= 0) {
                key = index;
            }
        }
        if (rangeSet != null) {
            mask |= Range.toRangeMask(rangeSet);
        }
        stream.defaultWriteObject();
    }
}
