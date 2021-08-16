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
 * Copyright (C) 1996-2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

package jdk.internal.icu.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.MissingResourceException;

import jdk.internal.icu.lang.UCharacter.HangulSyllableType;
import jdk.internal.icu.lang.UCharacter.NumericType;
import jdk.internal.icu.text.UTF16;
import jdk.internal.icu.text.UnicodeSet;
import jdk.internal.icu.util.VersionInfo;

/**
* <p>Internal class used for Unicode character property database.</p>
* <p>This classes store binary data read from uprops.icu.
* It does not have the capability to parse the data into more high-level
* information. It only returns bytes of information when required.</p>
* <p>Due to the form most commonly used for retrieval, array of char is used
* to store the binary data.</p>
* <p>UCharacterPropertyDB also contains information on accessing indexes to
* significant points in the binary data.</p>
* <p>Responsibility for molding the binary data into more meaning form lies on
* <a href=UCharacter.html>UCharacter</a>.</p>
* @author Syn Wee Quek
* @since release 2.1, february 1st 2002
*/

public final class UCharacterProperty
{
    // public data members -----------------------------------------------

    /*
     * public singleton instance
     */
    public static final UCharacterProperty INSTANCE;

    /**
     * Trie data
     */
    public Trie2_16 m_trie_;

    /**
     * Unicode version
     */
    public VersionInfo m_unicodeVersion_;

    /**
     * Character type mask
     */
    public static final int TYPE_MASK = 0x1F;

    // uprops.h enum UPropertySource --------------------------------------- ***

    /** From uchar.c/uprops.icu main trie */
    public static final int SRC_CHAR=1;
    /** From uchar.c/uprops.icu properties vectors trie */
    public static final int SRC_PROPSVEC=2;
    /** From ubidi_props.c/ubidi.icu */
    public static final int SRC_BIDI=5;
    /** From normalizer2impl.cpp/nfc.nrm */
    public static final int SRC_NFC=8;
    /** From normalizer2impl.cpp/nfkc.nrm */
    public static final int SRC_NFKC=9;

    // public methods ----------------------------------------------------

    /**
     * Gets the main property value for code point ch.
     * @param ch code point whose property value is to be retrieved
     * @return property value of code point
     */
    public final int getProperty(int ch)
    {
        return m_trie_.get(ch);
    }

    /**
     * Gets the unicode additional properties.
     * Java version of C u_getUnicodeProperties().
     * @param codepoint codepoint whose additional properties is to be
     *                  retrieved
     * @param column The column index.
     * @return unicode properties
     */
    public int getAdditional(int codepoint, int column) {
        assert column >= 0;
        if (column >= m_additionalColumnsCount_) {
            return 0;
        }
        return m_additionalVectors_[m_additionalTrie_.get(codepoint) + column];
    }

    /**
     * <p>Get the "age" of the code point.</p>
     * <p>The "age" is the Unicode version when the code point was first
     * designated (as a non-character or for Private Use) or assigned a
     * character.</p>
     * <p>This can be useful to avoid emitting code points to receiving
     * processes that do not accept newer characters.</p>
     * <p>The data is from the UCD file DerivedAge.txt.</p>
     * <p>This API does not check the validity of the codepoint.</p>
     * @param codepoint The code point.
     * @return the Unicode version number
     */
    public VersionInfo getAge(int codepoint)
    {
        int version = getAdditional(codepoint, 0) >> AGE_SHIFT_;
        return VersionInfo.getInstance(
                           (version >> FIRST_NIBBLE_SHIFT_) & LAST_NIBBLE_MASK_,
                           version & LAST_NIBBLE_MASK_, 0, 0);
    }

    // int-value and enumerated properties --------------------------------- ***

    public int getType(int c) {
        return getProperty(c)&TYPE_MASK;
    }

    /*
     * Map some of the Grapheme Cluster Break values to Hangul Syllable Types.
     * Hangul_Syllable_Type is fully redundant with a subset of Grapheme_Cluster_Break.
     */
    private static final int /* UHangulSyllableType */ gcbToHst[]={
        HangulSyllableType.NOT_APPLICABLE,   /* U_GCB_OTHER */
        HangulSyllableType.NOT_APPLICABLE,   /* U_GCB_CONTROL */
        HangulSyllableType.NOT_APPLICABLE,   /* U_GCB_CR */
        HangulSyllableType.NOT_APPLICABLE,   /* U_GCB_EXTEND */
        HangulSyllableType.LEADING_JAMO,     /* U_GCB_L */
        HangulSyllableType.NOT_APPLICABLE,   /* U_GCB_LF */
        HangulSyllableType.LV_SYLLABLE,      /* U_GCB_LV */
        HangulSyllableType.LVT_SYLLABLE,     /* U_GCB_LVT */
        HangulSyllableType.TRAILING_JAMO,    /* U_GCB_T */
        HangulSyllableType.VOWEL_JAMO        /* U_GCB_V */
        /*
         * Omit GCB values beyond what we need for hst.
         * The code below checks for the array length.
         */
    };

    private class IntProperty {
        int column;  // SRC_PROPSVEC column, or "source" if mask==0
        int mask;
        int shift;

        IntProperty(int column, int mask, int shift) {
            this.column=column;
            this.mask=mask;
            this.shift=shift;
        }

        IntProperty(int source) {
            this.column=source;
            this.mask=0;
        }

        int getValue(int c) {
            // systematic, directly stored properties
            return (getAdditional(c, column)&mask)>>>shift;
        }
    }

    private class BiDiIntProperty extends IntProperty {
        BiDiIntProperty() {
            super(SRC_BIDI);
        }
    }

    private class CombiningClassIntProperty extends IntProperty {
        CombiningClassIntProperty(int source) {
            super(source);
        }
    }

    private class NormQuickCheckIntProperty extends IntProperty {  // UCHAR_NF*_QUICK_CHECK properties
        int which;
        int max;

        NormQuickCheckIntProperty(int source, int which, int max) {
            super(source);
            this.which=which;
            this.max=max;
        }
    }

    private IntProperty intProp =  new BiDiIntProperty() {  // BIDI_PAIRED_BRACKET_TYPE
        int getValue(int c) {
            return UBiDiProps.INSTANCE.getPairedBracketType(c);
        }
    };

    public int getIntPropertyValue(int c, int which) {
        if (which == BIDI_PAIRED_BRACKET_TYPE) {
            return intProp.getValue(c);
        }
        return 0; // undefined
    }

    /**
     * Forms a supplementary code point from the argument character<br>
     * Note this is for internal use hence no checks for the validity of the
     * surrogate characters are done
     * @param lead lead surrogate character
     * @param trail trailing surrogate character
     * @return code point of the supplementary character
     */
    public static int getRawSupplementary(char lead, char trail)
    {
        return (lead << LEAD_SURROGATE_SHIFT_) + trail + SURROGATE_OFFSET_;
    }

    /**
     * Gets the type mask
     * @param type character type
     * @return mask
     */
    public static final int getMask(int type)
    {
        return 1 << type;
    }

    /**
     * Returns the digit values of characters like 'A' - 'Z', normal,
     * half-width and full-width. This method assumes that the other digit
     * characters are checked by the calling method.
     * @param ch character to test
     * @return -1 if ch is not a character of the form 'A' - 'Z', otherwise
     *         its corresponding digit will be returned.
     */
    public static int getEuropeanDigit(int ch) {
        if ((ch > 0x7a && ch < 0xff21)
            || ch < 0x41 || (ch > 0x5a && ch < 0x61)
            || ch > 0xff5a || (ch > 0xff3a && ch < 0xff41)) {
            return -1;
        }
        if (ch <= 0x7a) {
            // ch >= 0x41 or ch < 0x61
            return ch + 10 - ((ch <= 0x5a) ? 0x41 : 0x61);
        }
        // ch >= 0xff21
        if (ch <= 0xff3a) {
            return ch + 10 - 0xff21;
        }
        // ch >= 0xff41 && ch <= 0xff5a
        return ch + 10 - 0xff41;
    }

    public int digit(int c) {
        int value = getNumericTypeValue(getProperty(c)) - NTV_DECIMAL_START_;
        if(value<=9) {
            return value;
        } else {
            return -1;
        }
    }

    // protected variables -----------------------------------------------

    /**
     * Extra property trie
     */
    Trie2_16 m_additionalTrie_;
    /**
     * Extra property vectors, 1st column for age and second for binary
     * properties.
     */
    int m_additionalVectors_[];
    /**
     * Number of additional columns
     */
    int m_additionalColumnsCount_;
    /**
     * Maximum values for block, bits used as in vector word
     * 0
     */
    int m_maxBlockScriptValue_;
    /**
     * Maximum values for script, bits used as in vector word
     * 0
     */
     int m_maxJTGValue_;
    /**
     * Script_Extensions data
     */
    public char[] m_scriptExtensions_;

    // private variables -------------------------------------------------

    /**
     * Default name of the datafile
     */
    @SuppressWarnings("deprecation")
    private static final String DATA_FILE_NAME_ =
            "/jdk/internal/icu/impl/data/icudt" +
            VersionInfo.ICU_DATA_VERSION_PATH +
            "/uprops.icu";

    /**
     * Shift value for lead surrogate to form a supplementary character.
     */
    private static final int LEAD_SURROGATE_SHIFT_ = 10;
    /**
     * Offset to add to combined surrogate pair to avoid masking.
     */
    private static final int SURROGATE_OFFSET_ =
                           UTF16.SUPPLEMENTARY_MIN_VALUE -
                           (UTF16.SURROGATE_MIN_VALUE <<
                           LEAD_SURROGATE_SHIFT_) -
                           UTF16.TRAIL_SURROGATE_MIN_VALUE;


    // property data constants -------------------------------------------------

    /**
     * Numeric types and values in the main properties words.
     */
    private static final int NUMERIC_TYPE_VALUE_SHIFT_ = 6;
    private static final int getNumericTypeValue(int props) {
        return props >> NUMERIC_TYPE_VALUE_SHIFT_;
    }

    /* constants for the storage form of numeric types and values */
    /** No numeric value. */
    private static final int NTV_NONE_ = 0;
    /** Decimal digits: nv=0..9 */
    private static final int NTV_DECIMAL_START_ = 1;
    /** Other digits: nv=0..9 */
    private static final int NTV_DIGIT_START_ = 11;
    /** Small integers: nv=0..154 */
    private static final int NTV_NUMERIC_START_ = 21;

    private static final int ntvGetType(int ntv) {
        return
            (ntv==NTV_NONE_) ? NumericType.NONE :
            (ntv<NTV_DIGIT_START_) ?  NumericType.DECIMAL :
            (ntv<NTV_NUMERIC_START_) ? NumericType.DIGIT :
            NumericType.NUMERIC;
    }

    /*
     * Properties in vector word 0
     * Bits
     * 31..24   DerivedAge version major/minor one nibble each
     * 23..22   3..1: Bits 21..20 & 7..0 = Script_Extensions index
     *             3: Script value from Script_Extensions
     *             2: Script=Inherited
     *             1: Script=Common
     *             0: Script=bits 21..20 & 7..0
     * 21..20   Bits 9..8 of the UScriptCode, or index to Script_Extensions
     * 19..17   East Asian Width
     * 16.. 8   UBlockCode
     *  7.. 0   UScriptCode, or index to Script_Extensions
     */

    /**
     * Script_Extensions: mask includes Script
     */
    public static final int SCRIPT_X_MASK = 0x00f000ff;
    //private static final int SCRIPT_X_SHIFT = 22;

    // The UScriptCode or Script_Extensions index is split across two bit fields.
    // (Starting with Unicode 13/ICU 66/2019 due to more varied Script_Extensions.)
    // Shift the high bits right by 12 to assemble the full value.
    public static final int SCRIPT_HIGH_MASK = 0x00300000;
    public static final int SCRIPT_HIGH_SHIFT = 12;
    public static final int MAX_SCRIPT = 0x3ff;

    /**
     * Integer properties mask and shift values for East Asian cell width.
     * Equivalent to icu4c UPROPS_EA_MASK
     */
    private static final int EAST_ASIAN_MASK_ = 0x000e0000;
    /**
     * Integer properties mask and shift values for East Asian cell width.
     * Equivalent to icu4c UPROPS_EA_SHIFT
     */
    private static final int EAST_ASIAN_SHIFT_ = 17;
    /**
     * Integer properties mask and shift values for blocks.
     * Equivalent to icu4c UPROPS_BLOCK_MASK
     */
    private static final int BLOCK_MASK_ = 0x0001ff00;
    /**
     * Integer properties mask and shift values for blocks.
     * Equivalent to icu4c UPROPS_BLOCK_SHIFT
     */
    private static final int BLOCK_SHIFT_ = 8;
    /**
     * Integer properties mask and shift values for scripts.
     * Equivalent to icu4c UPROPS_SHIFT_LOW_MASK.
     */
    public static final int SCRIPT_LOW_MASK = 0x000000ff;

    public static final int mergeScriptCodeOrIndex(int scriptX) {
        return
            ((scriptX & SCRIPT_HIGH_MASK) >> SCRIPT_HIGH_SHIFT) |
            (scriptX & SCRIPT_LOW_MASK);
    }

    /**
     * Additional properties used in internal trie data
     */
    /*
     * Properties in vector word 1
     * Each bit encodes one binary property.
     * The following constants represent the bit number, use 1<<UPROPS_XYZ.
     * UPROPS_BINARY_1_TOP<=32!
     *
     * Keep this list of property enums in sync with
     * propListNames[] in icu/source/tools/genprops/props2.c!
     *
     * ICU 2.6/uprops format version 3.2 stores full properties instead of "Other_".
     */
    private static final int WHITE_SPACE_PROPERTY_ = 0;
    private static final int DASH_PROPERTY_ = 1;
    private static final int HYPHEN_PROPERTY_ = 2;
    private static final int QUOTATION_MARK_PROPERTY_ = 3;
    private static final int TERMINAL_PUNCTUATION_PROPERTY_ = 4;
    private static final int MATH_PROPERTY_ = 5;
    private static final int HEX_DIGIT_PROPERTY_ = 6;
    private static final int ASCII_HEX_DIGIT_PROPERTY_ = 7;
    private static final int ALPHABETIC_PROPERTY_ = 8;
    private static final int IDEOGRAPHIC_PROPERTY_ = 9;
    private static final int DIACRITIC_PROPERTY_ = 10;
    private static final int EXTENDER_PROPERTY_ = 11;
    private static final int NONCHARACTER_CODE_POINT_PROPERTY_ = 12;
    private static final int GRAPHEME_EXTEND_PROPERTY_ = 13;
    private static final int GRAPHEME_LINK_PROPERTY_ = 14;
    private static final int IDS_BINARY_OPERATOR_PROPERTY_ = 15;
    private static final int IDS_TRINARY_OPERATOR_PROPERTY_ = 16;
    private static final int RADICAL_PROPERTY_ = 17;
    private static final int UNIFIED_IDEOGRAPH_PROPERTY_ = 18;
    private static final int DEFAULT_IGNORABLE_CODE_POINT_PROPERTY_ = 19;
    private static final int DEPRECATED_PROPERTY_ = 20;
    private static final int LOGICAL_ORDER_EXCEPTION_PROPERTY_ = 21;
    private static final int XID_START_PROPERTY_ = 22;
    private static final int XID_CONTINUE_PROPERTY_ = 23;
    private static final int ID_START_PROPERTY_    = 24;
    private static final int ID_CONTINUE_PROPERTY_ = 25;
    private static final int GRAPHEME_BASE_PROPERTY_ = 26;
    private static final int S_TERM_PROPERTY_ = 27;
    private static final int VARIATION_SELECTOR_PROPERTY_ = 28;
    private static final int PATTERN_SYNTAX = 29;                   /* new in ICU 3.4 and Unicode 4.1 */
    private static final int PATTERN_WHITE_SPACE = 30;

    /*
     * Properties in vector word 2
     * Bits
     * 31..26   reserved
     * 25..20   Line Break
     * 19..15   Sentence Break
     * 14..10   Word Break
     *  9.. 5   Grapheme Cluster Break
     *  4.. 0   Decomposition Type
     */
    private static final int LB_MASK          = 0x03f00000;
    private static final int LB_SHIFT         = 20;

    private static final int SB_MASK          = 0x000f8000;
    private static final int SB_SHIFT         = 15;

    private static final int WB_MASK          = 0x00007c00;
    private static final int WB_SHIFT         = 10;

    private static final int GCB_MASK         = 0x000003e0;
    private static final int GCB_SHIFT        = 5;

    /**
     * Integer properties mask for decomposition type.
     * Equivalent to icu4c UPROPS_DT_MASK.
     */
    private static final int DECOMPOSITION_TYPE_MASK_ = 0x0000001f;

    /**
     * First nibble shift
     */
    private static final int FIRST_NIBBLE_SHIFT_ = 0x4;
    /**
     * Second nibble mask
     */
    private static final int LAST_NIBBLE_MASK_ = 0xF;
    /**
     * Age value shift
     */
    private static final int AGE_SHIFT_ = 24;

    // private constructors --------------------------------------------------

    /**
     * Constructor
     * @exception IOException thrown when data reading fails or data corrupted
     */
    private UCharacterProperty() throws IOException
    {
        // jar access
        ByteBuffer bytes=ICUBinary.getRequiredData(DATA_FILE_NAME_);
        m_unicodeVersion_ = ICUBinary.readHeaderAndDataVersion(bytes, DATA_FORMAT, new IsAcceptable());
        // Read or skip the 16 indexes.
        int propertyOffset = bytes.getInt();
        /* exceptionOffset = */ bytes.getInt();
        /* caseOffset = */ bytes.getInt();
        int additionalOffset = bytes.getInt();
        int additionalVectorsOffset = bytes.getInt();
        m_additionalColumnsCount_ = bytes.getInt();
        int scriptExtensionsOffset = bytes.getInt();
        int reservedOffset7 = bytes.getInt();
        /* reservedOffset8 = */ bytes.getInt();
        /* dataTopOffset = */ bytes.getInt();
        m_maxBlockScriptValue_ = bytes.getInt();
        m_maxJTGValue_ = bytes.getInt();
        ICUBinary.skipBytes(bytes, (16 - 12) << 2);

        // read the main properties trie
        m_trie_ = Trie2_16.createFromSerialized(bytes);
        int expectedTrieLength = (propertyOffset - 16) * 4;
        int trieLength = m_trie_.getSerializedLength();
        if(trieLength > expectedTrieLength) {
            throw new IOException("uprops.icu: not enough bytes for main trie");
        }
        // skip padding after trie bytes
        ICUBinary.skipBytes(bytes, expectedTrieLength - trieLength);

        // skip unused intervening data structures
        ICUBinary.skipBytes(bytes, (additionalOffset - propertyOffset) * 4);

        if(m_additionalColumnsCount_ > 0) {
            // reads the additional property block
            m_additionalTrie_ = Trie2_16.createFromSerialized(bytes);
            expectedTrieLength = (additionalVectorsOffset-additionalOffset)*4;
            trieLength = m_additionalTrie_.getSerializedLength();
            if(trieLength > expectedTrieLength) {
                throw new IOException("uprops.icu: not enough bytes for additional-properties trie");
            }
            // skip padding after trie bytes
            ICUBinary.skipBytes(bytes, expectedTrieLength - trieLength);

            // additional properties
            int size = scriptExtensionsOffset - additionalVectorsOffset;
            m_additionalVectors_ = new int[size];
            for (int i = 0; i < size; i ++) {
                m_additionalVectors_[i] = bytes.getInt();
            }
        }

        // Script_Extensions
        int numChars = (reservedOffset7 - scriptExtensionsOffset) * 2;
        if(numChars > 0) {
            m_scriptExtensions_ = new char[numChars];
            for(int i = 0; i < numChars; ++i) {
                m_scriptExtensions_[i] = bytes.getChar();
            }
        }
    }

    private static final class IsAcceptable implements ICUBinary.Authenticate {
        // @Override when we switch to Java 6
        public boolean isDataVersionAcceptable(byte version[]) {
            return version[0] == 7;
        }
    }

    private static final int DATA_FORMAT = 0x5550726F;  // "UPro"

    public void upropsvec_addPropertyStarts(UnicodeSet set) {
        /* add the start code point of each same-value range of the properties vectors trie */
        if(m_additionalColumnsCount_>0) {
            /* if m_additionalColumnsCount_==0 then the properties vectors trie may not be there at all */
            Iterator<Trie2.Range> trieIterator = m_additionalTrie_.iterator();
            Trie2.Range range;
            while(trieIterator.hasNext() && !(range=trieIterator.next()).leadSurrogate) {
                set.add(range.startCodePoint);
            }
        }
    }

    // This static initializer block must be placed after
    // other static member initialization
    static {
        try {
            INSTANCE = new UCharacterProperty();
        }
        catch (IOException e) {
            throw new MissingResourceException(e.getMessage(),DATA_FILE_NAME_,"");
        }
    }


    // Moved from UProperty.java
    /**
     * Enumerated property Bidi_Paired_Bracket_Type (new in Unicode 6.3).
     * Used in UAX #9: Unicode Bidirectional Algorithm
     * (http://www.unicode.org/reports/tr9/)
     * Returns UCharacter.BidiPairedBracketType values.
     * @stable ICU 52
     */
    public static final int BIDI_PAIRED_BRACKET_TYPE = 0x1015;

}
