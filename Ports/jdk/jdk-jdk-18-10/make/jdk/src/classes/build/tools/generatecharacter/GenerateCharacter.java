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

package build.tools.generatecharacter;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.util.List;

import build.tools.generatecharacter.CharacterName;

/**
 * This program generates the source code for the class java.lang.Character.
 * It also generates native C code that can perform the same operations.
 * It requires two external input data files:
 * <ul>
 * <li> Unicode specification file
 * <li> Character class template file
 * </ul>
 * The Unicode specification file is available from the Unicode consortium.
 * It has character specification lines that look like this:
 * <listing>
 * 0041;LATIN CAPITAL LETTER A;Lu;0;L;;;;;N;;;;0061;
 * </listing>
 * The Character class template file is filled in with additional
 * information to produce the file Character.java, which can then be
 * compiled by a Java compiler.  The template file contains certain
 * markers consisting of an alphabetic name string preceded by "$$".
 * Such markers are replaced with generated program text.  As a special
 * case, the marker "Lookup(xxx)" is recognized, where "xxx" consists of
 * alphabetic characters constituting a variable name.  The character "_"
 * is considered alphabetic for these purposes.
 *
 * @author  Guy Steele
 * @author  Alan Liu
 * @author  John O'Conner
 */

public class GenerateCharacter {

    final static boolean DEBUG = false;

    final static String commandMarker = "$$";
    static String ROOT                        = "";
    static String DefaultUnicodeSpecFileName  = ROOT + "UnicodeData.txt";
    static String DefaultSpecialCasingFileName = ROOT + "SpecialCasing.txt";
    static String DefaultPropListFileName     = ROOT + "PropList.txt";
    static String DefaultDerivedPropsFileName = ROOT + "DerivedCoreProperties.txt";
    static String DefaultJavaTemplateFileName = ROOT + "Character.java.template";
    static String DefaultJavaOutputFileName   = ROOT + "Character.java";
    static String DefaultCTemplateFileName    = ROOT + "Character.c.template";
    static String DefaultCOutputFileName      = ROOT + "Character.c";

    static int plane = 0;

    /* The overall idea is that, in the generated Character class source code,
    most character property data is stored in a special multi-level table whose
    structure is defined by a sequence of nonnegative integers [k1, k2, ..., kn].
    The integers must sum to 16 (the number of bits in a character).
    The first table is indexed by the k1 high-order bits of the character code.
    The result is concatenated to the next k2 bits of the character code to index
    the second table, and so on.  Eventually the kn low-order bits of the character
    code are concatenated and used to index one of two tables A and B; A contains
    32-bit integer entries and B contains 16-bit short entries.  The 48 bits that
    can be thus obtained encode the properties for the character.

    The default specification is [9, 4, 3, 0].  This particular table format was
    designed by conducting an exhaustive search of table formats to minimize the
    space consumed by the tables: the first and third tables need have only byte
    values (the second table must have short values).  Another good choice is
    [10, 6, 0], which produces a larger table but allows particularly fast table
    lookup code.

    In each case, where the word "concatenated" is used, this may imply
    first a << and then a | operation, or perhaps just a | operation if
    the values in the table can be preshifted (generally possible if the table
    entries are short rather than byte).
    */

    /* The character properties are currently encoded into A (32 bits) and B (8 bits)
       two parts.

    A: the low 32 bits are defined  in the following manner:

    1 bit Mirrored property.
    4 bits      Bidirectional category (see below) (unused if -nobidi switch specified)
    9 bits      A signed offset used for converting case .
    1 bit       If 1, adding the signed offset converts the character to lowercase.
    1 bit       If 1, subtracting the signed offset converts the character to uppercase.
        Note: for a titlecase character, both of the preceding bits will be 1
        and the signed offset will be 1.
    1 bit   If 1, this character has a titlecase equivalent (possibly itself);
        in this case, the two bits before this bit can be used to decide
        whether this character is in fact uppercase, lowercase, or titlecase.
    3 bits      This field provides a quick way to lex identifiers.
        The eight possible values for this field are as follows:
        0  May not be part of an identifier
        1  Ignorable control; may continue a Unicode identifier or Java identifier
        2  May continue a Java identifier but not a Unicode identifier (unused)
        3  May continue a Unicode identifier or Java identifier
        4  Is a Java whitespace character
        5  May start or continue a Java identifier;
           may continue but not start a Unicode identifier
           (this value is used for connector punctuation such as _)
        6  May start or continue a Java identifier;
           may not occur in a Unicode identifier
           (this value is used for currency symbols such as $)
        7  May start or continue a Unicode identifier or Java identifier
        Thus:
           5, 6, 7 may start a Java identifier
           1, 2, 3, 5, 6, 7 may continue a Java identifier
           7 may start a Unicode identifier
           1, 3, 5, 7 may continue a Unicode identifier
           1 is ignorable within an identifier
           4 is Java whitespace
    2 bits      This field indicates whether the character has a numeric property.
        The four possible values for this field are as follows:
        0  This character has no numeric property.
        1  Adding the digit offset to the character code and then
           masking with 0x1F will produce the desired numeric value.
        2  This character has a "strange" numeric value.
        3  A Java supradecimal digit: adding the digit offset to the
           character code, then masking with 0x1F, then adding 10
           will produce the desired numeric value.
    5 bits  The digit offset (see description of previous field)
    5 bits      Character type (see below)

    B: the high 16 bits are defined as:
    1 bit Other_Lowercase property
    1 bit Other_Uppercase property
    1 bit Other_Alphabetic property
    1 bit Ideographic property
    1 bit ID_Start property
    1 bit ID_Continue property
    */


    // bit masks identify each component of a 32-bit property field described
    // above.
    // shift* indicates how many shifts right must happen to get the
    // indicated property value in the lowest bits of the 32-bit space.
    private static final int
        shiftType           = 0,        maskType            =       0x001F,
        shiftDigitOffset    = 5,        maskDigitOffset     =       0x03E0,
        shiftNumericType    = 10,       maskNumericType     =       0x0C00,
        shiftIdentifierInfo = 12,       maskIdentifierInfo  =       0x7000,
                                        maskUnicodePart     =       0x1000,
                                        maskLowerCase       =      0x20000,
                                        maskUpperCase       =      0x10000,
                                        maskTitleCase       =      0x08000,
        shiftCaseOffset     = 18,       maskCaseOffset      =   0x07FC0000,
        shiftCaseOffsetSign = 5,
                                        // used only when calculating and
                                        // storing digit offsets from char values
                                        maskDigit               =   0x001F,
                                        // case offset are 9 bits
                                        maskCase                =   0x01FF,
        shiftBidi           = 27,       maskBidi              = 0x78000000;

    // maskMirrored needs to be long, if up 16-bit
    private static final long maskMirrored          = 0x80000000L;

    // bit masks identify the 8-bit property field described above, in B
    // table
    private static final long
        maskOtherLowercase  = 0x0100000000L,
        maskOtherUppercase  = 0x0200000000L,
        maskOtherAlphabetic = 0x0400000000L,
        maskIdeographic     = 0x0800000000L,
        maskIDStart         = 0x1000000000L,
        maskIDContinue      = 0x2000000000L;

    // Can compare masked values with these to determine
    // numeric or lexical types.
    public static int
        valueNotNumeric             = 0x0000,
        valueDigit                  = 0x0400,
        valueStrangeNumeric         = 0x0800,
        valueJavaSupradecimal       = 0x0C00,
        valueIgnorable              = 0x1000,
        valueJavaOnlyPart           = 0x2000,
        valueJavaUnicodePart        = 0x3000,
        valueJavaWhitespace         = 0x4000,
        valueJavaStartUnicodePart   = 0x5000,
        valueJavaOnlyStart          = 0x6000,
        valueJavaUnicodeStart       = 0x7000,
        lowJavaStart                = 0x5000,
        nonzeroJavaPart             = 0x3000,
        valueUnicodeStart           = 0x7000;

    // these values are used when only identifier properties are generated
    // for use in verifier code. Shortens the property down to a single byte.
    private static final int
        bitJavaStart            = 0x02,
        bitJavaPart             = 0x01,
        maskIsJavaIdentifierPart = bitJavaPart,
        maskIsJavaIdentifierStart = bitJavaStart;

    static int maxOffset = maskCase/2 ;
    static int minOffset = -maxOffset;

    /* The following routines provide simple, concise formatting of long integer values.
     The number in the name of the method indicates the desired number of characters
     to be produced.  If the number of digits required to represent the integer value
     is less than that number, then the output is padded on the left  with zeros
     (for hex) or with spaces (for decimal).  If the number of digits required to
     represent the integer value is greater than the desired number, then all the digits
     that are required are actually produced.
    */

    static String hex(long n) { return Long.toHexString(n).toUpperCase(); }

    static String hex2(long n) {
        String q = Long.toHexString(n & 0xFF).toUpperCase();
        return "00".substring(Math.min(2, q.length())) + q;
    }

    static String hex4(long n) {
        String q = Long.toHexString(n & 0xFFFF).toUpperCase();
        return "0000".substring(Math.min(4, q.length())) + q;
    }

    static String hex8(long n) {
        String q = Long.toHexString(n & 0xFFFFFFFFL).toUpperCase();
        return "00000000".substring(Math.min(8, q.length())) + q;
    }

    static String hex16(long n) {
        String q = Long.toHexString(n).toUpperCase();
        return "0000000000000000".substring(Math.min(16, q.length())) + q;
    }

    static String dec3(long n) {
        String q = Long.toString(n);
        return "   ".substring(Math.min(3, q.length())) + q;
    }

    static String dec5(long n) {
        String q = Long.toString(n);
        return "     ".substring(Math.min(5, q.length())) + q;
    }

    /* This routine is called when some failure occurs. */

    static void FAIL(String s) {
        System.out.println("** " + s);
    }

    /**
    * Given the data from the Unicode specification file, this routine builds a map.
    *
    * The specification file is assumed to contain its data in sorted order by
    * character code; as a result, the array passed as an argument to this method
    * has its components in the same sorted order, with one entry for each defined
    * Unicode character or character range.  (A range is indicated by two consecutive
    * entries, such that the name of the first entry begins with "<" and ends with
    * "First>" and the second entry begins with "<" and ends with "Last>".)  This is
    * therefore a sparse representation of the character property data.
    *
    * The resulting map is dense representation of the character data.  It contains
    * 2^16 = 65536 entries, each of which is a long integer.  (Right now only 32 bits
    * of this long value are used, but type long is used rather than int to facilitate
    * future extensions of this source code generator that might require more than
    * 32 bits to encode relevant character properties.)  Entry k holds the encoded
    * properties for character k.
    *
    * Method buildMap manages the transformation from the sparse representation to
    * the dense representation.  It calls method buildOne to handle the encoding
    * of character property data from a single UnicodeSpec object into 32 bits.
    * For undefined characters, method buildOne is not called and the map entry for
    * that character is set to UnicodeSpec.UNASSIGNED.
    *
    * @param data       character property data from the Unicode specification file
    * @return   an array of length 65536 with one entry for every possible char value
    *
    * @see GenerateCharacter#buildOne
    */

    static long[] buildMap(UnicodeSpec[] data, SpecialCaseMap[] specialMaps, PropList propList)
    {
        long[] result = new long[bLatin1 ? 256 : 1 << 16];
        int k = 0;
        int codePoint = plane << 16;
        UnicodeSpec nonCharSpec = new UnicodeSpec();
        for (int j = 0; j < data.length && k < result.length; j++) {
            if (data[j].codePoint == codePoint) {
                result[k] = buildOne(codePoint, data[j], specialMaps);
                ++k;
                ++codePoint;
            }
            else if(data[j].codePoint > codePoint) {
                if (data[j].name.endsWith("Last>")) {
                    // build map data for all chars except last in range
                    while (codePoint < data[j].codePoint && k < result.length) {
                        result[k] = buildOne(codePoint, data[j], specialMaps);
                        ++k;
                        ++codePoint;
                    }
                }
                else {
                    // we have a few unassigned chars before data[j].codePoint
                    while (codePoint < data[j].codePoint && k < result.length) {
                        result[k] = buildOne(codePoint, nonCharSpec, specialMaps);
                        ++k;
                        ++codePoint;
                    }
                }
                k = data[j].codePoint & 0xFFFF;
                codePoint = data[j].codePoint;
                result[k] = buildOne(codePoint, data[j], specialMaps);
                ++k;
                ++codePoint;
            }
            else {
                System.out.println("An error has occured during spec mapping.");
                System.exit(0);
            }
        }
        // if there are still unprocessed chars, process them
        // as unassigned/undefined.
        codePoint = (plane<<16) | k;
        while (k < result.length) {
            result[k] = buildOne(codePoint, nonCharSpec, specialMaps);
            ++k;
            ++codePoint;
        }
        // now add all extra supported properties from PropList, to the
        // upper 16-bit
        addExProp(result, propList, "Other_Lowercase", maskOtherLowercase);
        addExProp(result, propList, "Other_Uppercase", maskOtherUppercase);
        addExProp(result, propList, "Other_Alphabetic", maskOtherAlphabetic);
        addExProp(result, propList, "Ideographic", maskIdeographic);
        addExProp(result, propList, "ID_Start", maskIDStart);
        addExProp(result, propList, "ID_Continue", maskIDContinue);

        return result;
    }

    // The maximum and minimum offsets found while scanning the database
    static int maxOffsetSeen = 0;
    static int minOffsetSeen = 0;

    /**
     * Some Unicode separator characters are not considered Java whitespace.
     * @param c character to test
     * @return true if c in an invalid Java whitespace character, false otherwise.
     */
    static boolean isInvalidJavaWhiteSpace(int c) {
        int[] exceptions = {0x00A0, 0x2007, 0x202F, 0xFEFF};
        boolean retValue = false;
        for (int exception : exceptions) {
            if (c == exception) {
                retValue = true;
                break;
            }
        }
        return retValue;

    }

    /**
    * Given the character property data for one Unicode character, encode the data
    * of interest into a single long integer value.  (Right now only 32 bits
    * of this long value are used, but type long is used rather than int to facilitate
    * future extensions of this source code generator that might require more than
    * 32 bits to encode relevant character properties.)
    *
    * @param c   the character code for which to encode property data
    * @param us  property data record from the Unicode specification file
    *            (its character code might not be equal to c if it specifies data
    *            for a range of characters)
    * @return   an encoded long value that contains the properties for a single char
    *
    * @see GenerateCharacter#buildMap
    */

    static long buildOne(int c, UnicodeSpec us, SpecialCaseMap[] specialMaps) {
        long resultA = 0;
        // record the general category
        resultA |= us.generalCategory;

        // extract and record the uppercase letter / lowercase letter property into the
        // maskOtherUppercase/-Lowercase bit so that Character.isLower|UpperCase
        // can use a one-step lookup
        if (resultA == Character.UPPERCASE_LETTER) {
            resultA |= maskOtherUppercase;
        } else if (resultA == Character.LOWERCASE_LETTER) {
            resultA |= maskOtherLowercase;
        }

        // record the numeric properties
        NUMERIC: {
        STRANGE: {
            int val = 0;
            // c is A-Z
            if ((c >= 0x0041) && (c <= 0x005A)) {
                val = c - 0x0041;
                resultA |= valueJavaSupradecimal;
            // c is a-z
            } else if ((c >= 0x0061) && (c <= 0x007A)) {
                val = c - 0x0061;
                resultA |= valueJavaSupradecimal;
            // c is a full-width A-Z
            } else if ((c >= 0xFF21) && (c <= 0xFF3A)) {
                val = c - 0xFF21;
                resultA |= valueJavaSupradecimal;
            // c is a full-width a-z
            } else if ((c >= 0xFF41) && (c <= 0xFF5A)) {
                val = c - 0xFF41;
                resultA |= valueJavaSupradecimal;
            } else if (us.isDecimalValue()) {
                val = us.decimalValue;
                resultA |= valueDigit;
            } else if (us.isDigitValue()) {
                val = us.digitValue;
                resultA |= valueDigit;
            } else {
                if (us.numericValue.length() == 0) {
                    break NUMERIC;                      // no numeric value at all
                } else {
                    try {
                        val = Integer.parseInt(us.numericValue);
                        if (val >= 32 || val < 0) break STRANGE;
                        if (c == 0x215F) break STRANGE;
                    } catch(NumberFormatException e) {
                        break STRANGE;
                    }
                    resultA |= valueDigit;
                }
            }
            if (val >= 32 || val < 0) break STRANGE;
            resultA |= ((val - c & maskDigit) << shiftDigitOffset);
            break NUMERIC;
        } // end STRANGE
        resultA |= valueStrangeNumeric;
        } // end NUMERIC

        // record case mapping
        int offset = 0;
        // might have a 1:M mapping
        int specialMap = SpecialCaseMap.find(c, specialCaseMaps);
        boolean bHasUpper = (us.hasUpperMap()) || (specialMap != -1);
        if (bHasUpper) {
            resultA |= maskUpperCase;
        }
        if (specialMap != -1) {
            // has mapping, but cannot record the
            // proper offset; can only flag it and provide special case
            // code in Character.java
            offset = -1;
        }
        else if (us.hasUpperMap())  {
            offset = c - us.upperMap;
        }

        if (us.hasLowerMap()) {
            resultA |= maskLowerCase;
            if (offset == 0)
                offset = us.lowerMap - c;
            else if (offset != (us.lowerMap - c)) {
                if (DEBUG) {
                FAIL("Character " + hex(c) +
                " has incompatible lowercase and uppercase mappings");
                }
            }
        }
        if ((us.hasTitleMap() && us.titleMap != us.upperMap) ||
            (bHasUpper && us.hasLowerMap())) {
            resultA |= maskTitleCase;
        }
        if (bHasUpper && !us.hasLowerMap() && !us.hasTitleMap() && verbose) {
            System.out.println("Warning: Character " + hex4(c) + " has upper but " +
                               "no title case; Java won't know this");
        }
        if (offset < minOffsetSeen) minOffsetSeen = offset;
        if (offset > maxOffsetSeen) maxOffsetSeen = offset;
        if (offset > maxOffset || offset < minOffset) {
            if (DEBUG) {
            FAIL("Case offset " + offset + " for character " + hex4(c) + " must be handled as a special case");
            }
            offset = maskCase;
        }
        resultA |= ((offset & maskCase) << shiftCaseOffset);

        // record lexical info about this character
        if (us.generalCategory == UnicodeSpec.LOWERCASE_LETTER
                || us.generalCategory == UnicodeSpec.UPPERCASE_LETTER
                || us.generalCategory == UnicodeSpec.TITLECASE_LETTER
                || us.generalCategory == UnicodeSpec.MODIFIER_LETTER
                || us.generalCategory == UnicodeSpec.OTHER_LETTER
                || us.generalCategory == UnicodeSpec.LETTER_NUMBER) {
            resultA |= valueJavaUnicodeStart;
        }
        else if (us.generalCategory == UnicodeSpec.COMBINING_SPACING_MARK
                || us.generalCategory == UnicodeSpec.NON_SPACING_MARK
                || us.generalCategory == UnicodeSpec.DECIMAL_DIGIT_NUMBER) {
            resultA |= valueJavaUnicodePart;
        }
        else if (us.generalCategory == UnicodeSpec.CONNECTOR_PUNCTUATION) {
            resultA |= valueJavaStartUnicodePart;
        }
        else if (us.generalCategory == UnicodeSpec.CURRENCY_SYMBOL) {
            resultA |= valueJavaOnlyStart;
        }
        else if (((c >= 0x0000) && (c <= 0x0008))
                || ((c >= 0x000E) && (c <= 0x001B))
                || ((c >= 0x007F) && (c <= 0x009F))
                || us.generalCategory == UnicodeSpec.FORMAT) {
            resultA |= valueIgnorable;
        }
        else if (us.generalCategory == UnicodeSpec.SPACE_SEPARATOR
                || us.generalCategory == UnicodeSpec.LINE_SEPARATOR
                || us.generalCategory == UnicodeSpec.PARAGRAPH_SEPARATOR) {
            if (!isInvalidJavaWhiteSpace(c)) resultA |= valueJavaWhitespace;
        }
        else if (((c >= 0x0009) && (c <= 0x000D))
                || ((c >= 0x001C) && (c <= 0x001F))) {
            resultA |= valueJavaWhitespace;
        }

        // record bidi category
        if (!nobidi) {
            int tmpBidi =
                (us.bidiCategory > UnicodeSpec.DIRECTIONALITY_OTHER_NEUTRALS ||
                    us.bidiCategory == -1) ? maskBidi : (us.bidiCategory << shiftBidi);
            resultA |= tmpBidi;
        }

        // record mirrored property
        if (!nomirror) {
            resultA |= us.mirrored ? maskMirrored : 0;
        }

        if (identifiers) {
            long replacement = 0;
            if ((resultA & maskIdentifierInfo) >= lowJavaStart) {
                replacement |= bitJavaStart;
            }
            if ( ((resultA & nonzeroJavaPart) != 0)
                    && ((resultA & maskIdentifierInfo) != valueIgnorable)) {
                replacement |= bitJavaPart;
            }
            resultA = replacement;
        }
        return resultA;
    }

    static void addExProp(long[] map, PropList propList, String prop, long mask) {
        List<Integer> cps = propList.codepoints(prop);
        if (cps != null) {
            for (Integer cp : cps) {
                if (cp < map.length)
                    map[cp] |= mask;
            }
        }
    }

    /**
    * This is the heart of the table compression strategy.  The inputs are a map
    * and a number of bits (size).  The map is simply an array of long integer values;
    * the number of bits indicates how index values for that map are to be split.
    * The length of the given map must be a multiple of (1 << size).  The result is
    * a new map z and a compressed table t such that for every valid index value k
    * for the original map, t[(z[k>>size]<<size)|(k & ((1<<size)-1))] == map[k].
    *
    * In other words, the index k can be split into two parts, namely the "size"
    * low-order bits and all the remaining high-order bits; the high-order bits are then
    * remapped by map z to produce an index into table t.  In effect, the data of the
    * original map m is broken up into blocks of size (1<<size); the compression relies
    * on the expectation that many of these blocks will be identical and therefore need
    * be represented only once in the compressed table t.
    *
    * This method is intended to be used iteratively.  The first map to be handed
    * to it is the one constructed by method buildMap.  After that, the first of the
    * two arrays returned by this method is fed back into it for further compression.
    * At the end of the iteration, one has a starter map and a sequence of tables.
    *
    * The algorithm used to implement this computation is straightforward and not
    * especially clever.  It uses brute-force linear search (the loop labeled MIDDLE)
    * to locate identical blocks, so overall the time complexity of the algorithm
    * is quadratic in the length of the input map.  Fortunately, speed is not crucial
    * to this application.
    *
    * @param map                a map to be compressed
    * @param size       the number of index bits to be split off by the compression
    * @return   an array of length 2 containing two arrays; the first is a new map
    *           and the second is a compressed data table
    *
    * @see GenerateCharacter#buildMap
    */

    static long[][] buildTable(long[] map, int size) {
        int n = map.length;
        if (((n >> size) << size) != n) {
            FAIL("Length " + n + " is not a multiple of " + (1 << size));
        }
        int m = 1 << size;
        // We know the final length of the new map up front.
        long[] newmap = new long[n >> size];
        // The buffer is used temporarily to hold data for the compressed table
        // because we don't know its final length yet.
        long[] buffer = new long[n];
        int ptr = 0;
OUTER:  for (int i = 0; i < n; i += m) {
            // For every block of size m in the original map...
    MIDDLE: for (int j = 0; j < ptr; j += m) {
                // Find out whether there is already a block just like it in the buffer.
                for (int k = 0; k < m; k++) {
                    if (buffer[j+k] != map[i+k])
                        continue MIDDLE;
                }
                // There is a block just like it at position j, so just
                // put its index into the new map (thereby sharing it).
                newmap[i >> size] = (j >> size);
                continue OUTER;
            } // end MIDDLE
            // There is no block just like it already, so add it to
            // the buffer and put its index into the new map.
            if (m > 0) System.arraycopy(map, i, buffer, ptr, m);
            newmap[i >> size] = (ptr >> size);
            ptr += m;
        } // end OUTER
        // Now we know how long the compressed table should be,
        // so create a new array and copy data from the temporary buffer.
        long[] newdata = new long[ptr];
        if (ptr > 0) System.arraycopy(buffer, 0, newdata, 0, ptr);
        // Return the new map and the new data table.
        return new long[][]{ newmap, newdata };
    }

    /**
    * Once the compressed tables have been computed, this method reads in a
    * template file for the source code to be generated and writes out the final
    * source code by acting as a sort of specialized macro processor.
    *
    * The first output line is a comment saying that the file was automatically
    * generated; it includes a timestamp.  All other output is generated by
    * reading a line from the template file, performing macro replacements,
    * and then writing the resulting line or lines of code to the output file.
    *
    * This method handles the I/O, the timestamp comment, and the locating of
    * macro calls within each input line.  The method replaceCommand is called
    * to generate replacement text for each macro call.
    *
    * Macro calls to be replaced are indicated in the template file by
    * occurrences of the commandMarker "$$".  The rest of the call may consist
    * of Java letters (including the underscore "_") and also of balanced
    * parentheses.
    *
    * @param theTemplateFileName
    *           the file name for the template input file
    * @param theOutputFileName
    *           the file name for the source code output file
    *
    *     @see GenerateCharacter#replaceCommand
    */

    static void generateCharacterClass(String theTemplateFileName,
                                       String theOutputFileName)
        throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(theTemplateFileName));
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(theOutputFileName)));
        out.println(commentStart +
            " This file was generated AUTOMATICALLY from a template file " +
            commentEnd);
        int marklen = commandMarker.length();
        LOOP: while(true) {
            try {
                String line = in.readLine();
                if (line == null) break LOOP;
                int pos = 0;
                int depth = 0;
                while ((pos = line.indexOf(commandMarker, pos)) >= 0) {
                    int newpos = pos + marklen;
                    char ch;
                    SCAN: while (newpos < line.length() &&
                            (Character.isJavaIdentifierStart(ch = line.charAt(newpos))
                            || ch == '(' || (ch == ')' && depth > 0))) {
                        ++newpos;
                        if (ch == '(') {
                            ++depth;
                        }
                        else if (ch == ')') {
                            --depth;
                            if (depth == 0)
                                break SCAN;
                        }
                    }
                    String replacement = replaceCommand(line.substring(pos + marklen, newpos));
                    line = line.substring(0, pos) + replacement + line.substring(newpos);
                    pos += replacement.length();
                }
                out.println(line);
            }
            catch (IOException e) {
                break LOOP;
            }
        }
        in.close();
        out.close();
    }

    /**
    * The replaceCommand method takes a command (a macro call without the
    * leading marker "$$") and computes replacement text for it.
    *
    * Most of the commands are simply names of integer constants that are defined
    * in the source code of this GenerateCharacter class.  The replacement text is
    * simply the value of the constant as an appropriately formatted integer literal.
    *
    * Two cases are more complicated, however.  The command "Tables" causes the
    * final map and compressed tables to be emitted, with elaborate comments
    * describing their contents.  (This is actually handled by method genTables.)
    * The command "Lookup(xxx)", where "xxx" is the name of a variable, generates
    * an expression that will return the character property data for the character
    * whose code is the value of the variable "xxx".  (this is handled by method
    * "genAccess".)
    *
    * @param x  a command from the template file to be replaced
    * @return   the replacement text, as a String
    *
    * @see GenerateCharacter#genTables
    * @see GenerateCharacter#genAccess
    * @see GenerateCharacter#generateCharacterClass
    */

    static String replaceCommand(String x) {
        if (x.equals("Tables")) return genTables();
        if (x.equals("Initializers")) return genInitializers();
        if (x.length() >= 9 && x.startsWith("Lookup(") && x.endsWith(")") )
            return genAccess("A", x.substring(7, x.length()-1), (identifiers ? 2 : 32));
        if (x.length() >= 11 && x.startsWith("LookupEx(") && x.endsWith(")") )
            return genAccess("B", x.substring(9, x.length()-1), 16);
        if (x.equals("shiftType")) return Long.toString(shiftType);
        if (x.equals("shiftIdentifierInfo")) return Long.toString(shiftIdentifierInfo);
        if (x.equals("maskIdentifierInfo")) return "0x" + hex8(maskIdentifierInfo);
        if (x.equals("maskUnicodePart")) return "0x" + hex8(maskUnicodePart);
        if (x.equals("shiftCaseOffset")) return Long.toString(shiftCaseOffset);
        if (x.equals("shiftCaseOffsetSign")) return Long.toString(shiftCaseOffsetSign);
        if (x.equals("maskCase")) return "0x" + hex8(maskCase);
        if (x.equals("maskCaseOffset")) return "0x" + hex8(maskCaseOffset);
        if (x.equals("maskLowerCase")) return "0x" + hex8(maskLowerCase);
        if (x.equals("maskUpperCase")) return "0x" + hex8(maskUpperCase);
        if (x.equals("maskTitleCase")) return "0x" + hex8(maskTitleCase);
        if (x.equals("maskOtherLowercase")) return "0x" + hex4(maskOtherLowercase >> 32);
        if (x.equals("maskOtherUppercase")) return "0x" + hex4(maskOtherUppercase >> 32);
        if (x.equals("maskOtherAlphabetic")) return "0x" + hex4(maskOtherAlphabetic >> 32);
        if (x.equals("maskIdeographic")) return "0x" + hex4(maskIdeographic >> 32);
        if (x.equals("maskIDStart")) return "0x" + hex4(maskIDStart >> 32);
        if (x.equals("maskIDContinue")) return "0x" + hex4(maskIDContinue >> 32);
        if (x.equals("valueIgnorable")) return "0x" + hex8(valueIgnorable);
        if (x.equals("valueJavaUnicodeStart")) return "0x" + hex8(valueJavaUnicodeStart);
        if (x.equals("valueJavaOnlyStart")) return "0x" + hex8(valueJavaOnlyStart);
        if (x.equals("valueJavaUnicodePart")) return "0x" + hex8(valueJavaUnicodePart);
        if (x.equals("valueJavaOnlyPart")) return "0x" + hex8(valueJavaOnlyPart);
        if (x.equals("valueJavaWhitespace")) return "0x" + hex8(valueJavaWhitespace);
        if (x.equals("lowJavaStart")) return "0x" + hex8(lowJavaStart);
        if (x.equals("nonzeroJavaPart")) return "0x" + hex8(nonzeroJavaPart);
        if (x.equals("bitJavaStart")) return "0x" + hex8(bitJavaStart);
        if (x.equals("bitJavaPart")) return Long.toString(bitJavaPart);
        if (x.equals("valueUnicodeStart")) return "0x" + hex8(valueUnicodeStart);
        if (x.equals("maskIsJavaIdentifierStart")) return "0x" + hex(maskIsJavaIdentifierStart);
        if (x.equals("maskIsJavaIdentifierPart")) return "0x" + hex(maskIsJavaIdentifierPart);
        if (x.equals("shiftDigitOffset")) return Long.toString(shiftDigitOffset);
        if (x.equals("maskDigitOffset")) return "0x" + hex(maskDigitOffset);
        if (x.equals("maskDigit")) return "0x" + hex(maskDigit);
        if (x.equals("shiftNumericType")) return Long.toString(shiftNumericType);
        if (x.equals("maskNumericType")) return "0x" + hex(maskNumericType);
        if (x.equals("valueNotNumeric")) return "0x" + hex8(valueNotNumeric);
        if (x.equals("valueDigit")) return "0x" + hex8(valueDigit);
        if (x.equals("valueStrangeNumeric")) return "0x" + hex8(valueStrangeNumeric);
        if (x.equals("valueJavaSupradecimal")) return "0x" + hex8(valueJavaSupradecimal);
        if (x.equals("maskType")) return "0x" + hex(maskType);
        if (x.equals("shiftBidi")) return Long.toString(shiftBidi);
        if (x.equals("maskBidi")) return "0x" + hex(maskBidi);
        if (x.equals("maskMirrored")) return "0x" + hex8(maskMirrored);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.UNASSIGNED][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.UNASSIGNED);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.UPPERCASE_LETTER][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.UPPERCASE_LETTER);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.LOWERCASE_LETTER][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.LOWERCASE_LETTER);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.TITLECASE_LETTER][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.TITLECASE_LETTER);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.MODIFIER_LETTER][UnicodeSpec.LONG]))
             return Integer.toString(UnicodeSpec.MODIFIER_LETTER);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.OTHER_LETTER][UnicodeSpec.LONG]))
             return Integer.toString(UnicodeSpec.OTHER_LETTER);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.NON_SPACING_MARK][UnicodeSpec.LONG]))
             return Integer.toString(UnicodeSpec.NON_SPACING_MARK);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.ENCLOSING_MARK][UnicodeSpec.LONG]))
             return Integer.toString(UnicodeSpec.ENCLOSING_MARK);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.COMBINING_SPACING_MARK][UnicodeSpec.LONG]))
             return Integer.toString(UnicodeSpec.COMBINING_SPACING_MARK);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.DECIMAL_DIGIT_NUMBER][UnicodeSpec.LONG]))
             return Integer.toString(UnicodeSpec.DECIMAL_DIGIT_NUMBER);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.OTHER_NUMBER][UnicodeSpec.LONG]))
             return Integer.toString(UnicodeSpec.OTHER_NUMBER);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.SPACE_SEPARATOR][UnicodeSpec.LONG]))
             return Integer.toString(UnicodeSpec.SPACE_SEPARATOR);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.LINE_SEPARATOR][UnicodeSpec.LONG]))
             return Integer.toString(UnicodeSpec.LINE_SEPARATOR);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.PARAGRAPH_SEPARATOR][UnicodeSpec.LONG]))
             return Integer.toString(UnicodeSpec.PARAGRAPH_SEPARATOR);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.CONTROL][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.CONTROL);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.FORMAT][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.FORMAT);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.PRIVATE_USE][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.PRIVATE_USE);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.SURROGATE][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.SURROGATE);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.DASH_PUNCTUATION][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DASH_PUNCTUATION);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.START_PUNCTUATION][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.START_PUNCTUATION);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.END_PUNCTUATION][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.END_PUNCTUATION);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.INITIAL_QUOTE_PUNCTUATION][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.INITIAL_QUOTE_PUNCTUATION);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.FINAL_QUOTE_PUNCTUATION][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.FINAL_QUOTE_PUNCTUATION);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.CONNECTOR_PUNCTUATION][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.CONNECTOR_PUNCTUATION);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.OTHER_PUNCTUATION][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.OTHER_PUNCTUATION);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.LETTER_NUMBER][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.LETTER_NUMBER);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.MATH_SYMBOL][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.MATH_SYMBOL);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.CURRENCY_SYMBOL][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.CURRENCY_SYMBOL);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.MODIFIER_SYMBOL][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.MODIFIER_SYMBOL);
        if (x.equals(UnicodeSpec.generalCategoryList[UnicodeSpec.OTHER_SYMBOL][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.OTHER_SYMBOL);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_LEFT_TO_RIGHT][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_LEFT_TO_RIGHT);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_RIGHT_TO_LEFT][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_RIGHT_TO_LEFT);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_POP_DIRECTIONAL_FORMAT][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_POP_DIRECTIONAL_FORMAT);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_EUROPEAN_NUMBER][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_EUROPEAN_NUMBER);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_ARABIC_NUMBER][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_ARABIC_NUMBER);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_NONSPACING_MARK][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_NONSPACING_MARK);
         if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_BOUNDARY_NEUTRAL][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_BOUNDARY_NEUTRAL);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_PARAGRAPH_SEPARATOR][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_PARAGRAPH_SEPARATOR);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_SEGMENT_SEPARATOR][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_SEGMENT_SEPARATOR);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_WHITESPACE][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_WHITESPACE);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_OTHER_NEUTRALS][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_OTHER_NEUTRALS);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_LEFT_TO_RIGHT_ISOLATE][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_LEFT_TO_RIGHT_ISOLATE);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_RIGHT_TO_LEFT_ISOLATE][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_RIGHT_TO_LEFT_ISOLATE);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_FIRST_STRONG_ISOLATE][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_FIRST_STRONG_ISOLATE);
        if (x.equals(UnicodeSpec.bidiCategoryList[UnicodeSpec.DIRECTIONALITY_POP_DIRECTIONAL_ISOLATE][UnicodeSpec.LONG]))
            return Integer.toString(UnicodeSpec.DIRECTIONALITY_POP_DIRECTIONAL_ISOLATE);
        FAIL("Unknown text substitution marker " + commandMarker + x);
        return commandMarker + x;
    }

    /**
    * The genTables method generates source code for all the lookup tables
    * needed to represent the various Unicode character properties.
    * It simply calls the method genTable once for each table to be generated
    * and then generates a summary comment.
    *
    * @return   the replacement text for the "Tables" command, as a String
    *
    * @see GenerateCharacter#genTable
    * @see GenerateCharacter#replaceCommand
    */
    static String genTables() {
        int n = sizes.length;
        StringBuffer result = new StringBuffer();
        // liu : Add a comment showing the source of this table
        if (debug) {
            result.append(commentStart).append(" The following tables and code generated using:")
                    .append(commentEnd).append("\n  ")
                    .append(commentStart).append(' ')
                    .append(commandLineDescription).append(commentEnd).append("\n  ");
        }
        if (plane == 0 && !bLatin1) {
            genCaseMapTableDeclaration(result);
            genCaseMapTable(initializers, specialCaseMaps);
        }
        int totalBytes = 0;
        for (int k = 0; k < n - 1; k++) {
            genTable(result, tableNames[k], tables[k], 0, bytes[k]<<3, sizes[k], preshifted[k],
                sizes[k+1], false, false, k==0);
            int s = bytes[k];
            if (s == 1 && useCharForByte) {
                s = 2;
            }
            totalBytes += tables[k].length * s;
        }
        genTable(result, "A", tables[n - 1], 0, (identifiers ? 2 : 32),
            sizes[n - 1], false, 0, true, !(identifiers), false);

        // If we ever need more than 32 bits to represent the character properties,
        // then a table "B" may be needed as well.
        genTable(result, "B", tables[n - 1], 32, 8, sizes[n - 1], false, 0, true, true, false);

        totalBytes += ((((tables[n - 1].length * (identifiers ? 2 : 32)) + 31) >> 5) << 2);
        result.append(commentStart);
        result.append(" In all, the character property tables require ");
        result.append(totalBytes).append(" bytes.").append(commentEnd);
        if (verbose) {
            System.out.println("The character property tables require "
                 + totalBytes + " bytes.");
        }
        return result.toString();
    }

    /**
     * The genInitializers method generates the body of the
     * ensureInitted() method, which enables lazy initialization of
     * the case map table and other tables.
     */
    static String genInitializers() {
        return initializers.toString();
    }

    /**
     * Return the total number of bytes needed by all tables.  This is a stripped-
     * down copy of genTables().
     */
    static int getTotalBytes() {
        int n = sizes.length;
        int totalBytes = 0;
        for (int k = 0; k < n - 1; k++) {
            totalBytes += tables[k].length * bytes[k];
        }
        totalBytes += ((((tables[n - 1].length * (identifiers ? 2 : 32))
                         + 31) >> 5) << 2);
        return totalBytes;
    }

    static String SMALL_INITIALIZER =
        "        { // THIS CODE WAS AUTOMATICALLY CREATED BY GenerateCharacter:\n"+
        "            int len = $$name_DATA.length();\n"+
        "            int j=0;\n"+
        "            for (int i=0; i<len; ++i) {\n"+
        "                int c = $$name_DATA.charAt(i);\n"+
        "                for (int k=0; k<$$entriesPerChar; ++k) {\n"+
        "                    $$name[j++] = ($$type)c;\n"+
        "                    c >>= $$bits;\n"+
        "                }\n"+
        "            }\n"+
        "            assert (j == $$size);\n"+
        "        }\n";

    static String SAME_SIZE_INITIALIZER =
        "        { // THIS CODE WAS AUTOMATICALLY CREATED BY GenerateCharacter:\n"+
        "            assert ($$name_DATA.length() == $$size);\n"+
        "            for (int i=0; i<$$size; ++i)\n"+
        "                $$name[i] = ($$type)$$name_DATA.charAt(i);\n"+
        "        }\n";

    static String BIG_INITIALIZER =
        "        { // THIS CODE WAS AUTOMATICALLY CREATED BY GenerateCharacter:\n"+
        "            int len = $$name_DATA.length();\n"+
        "            int j=0;\n"+
        "            int charsInEntry=0;\n"+
        "            $$type entry=0;\n"+
        "            for (int i=0; i<len; ++i) {\n"+
        "                entry |= $$name_DATA.charAt(i);\n"+
        "                if (++charsInEntry == $$charsPerEntry) {\n"+
        "                    $$name[j++] = entry;\n"+
        "                    entry = 0;\n"+
        "                    charsInEntry = 0;\n"+
        "                }\n"+
        "                else {\n"+
        "                    entry <<= 16;\n"+
        "                }\n"+
        "            }\n"+
        "            assert (j == $$size);\n"+
        "        }\n";

    static String INT32_INITIALIZER =
        "        { // THIS CODE WAS AUTOMATICALLY CREATED BY GenerateCharacter:\n"+
        "            char[] data = $$name_DATA.toCharArray();\n"+
        "            assert (data.length == ($$size * 2));\n"+
        "            int i = 0, j = 0;\n"+
        "            while (i < ($$size * 2)) {\n"+
        "                int entry = data[i++] << 16;\n"+
        "                $$name[j++] = entry | data[i++];\n"+
        "            }\n"+
        "        }\n";

    static void addInitializer(String name, String type, int entriesPerChar,
                               int bits, int size) {

        String template = (entriesPerChar == 1) ? SAME_SIZE_INITIALIZER :
                          ((entriesPerChar > 0) ? SMALL_INITIALIZER : BIG_INITIALIZER);
        if (entriesPerChar == -2) {
            template = INT32_INITIALIZER;
        }
        int marklen = commandMarker.length();
        int pos = 0;
        while ((pos = template.indexOf(commandMarker, pos)) >= 0) {
            int newpos = pos + marklen;
            char ch;
            while (newpos < template.length() &&
                   Character.isJavaIdentifierStart(ch = template.charAt(newpos)) &&
                   ch != '_') // Don't allow this in token names
                ++newpos;
            String token = template.substring(pos+marklen, newpos);
            String replacement = switch (token) {
                case "name" -> name;
                case "type" -> type;
                case "bits" -> "" + bits;
                case "size" -> "" + size;
                case "entriesPerChar" -> "" + entriesPerChar;
                case "charsPerEntry" -> "" + (-entriesPerChar);
                default -> {
                    FAIL("Unrecognized token: " + token);
                    yield "ERROR";
                }
            };

            template = template.substring(0, pos) + replacement + template.substring(newpos);
            pos += replacement.length();
        }
        initializers.append(template);
    }

    /**
    * The genTable method generates source code for one lookup table.
    * Most of the complexity stems from handling various options as to
    * the type of the array components, the precise representation of the
    * values, the format in which to render each value, the number of values
    * to emit on each line of source code, and the kinds of useful comments
    * to be generated.
    *
    * @param result     a StringBuffer, to which the generated source code
    *                   text is to be appended
    * @param name       the name of the table
    * @param table      the table data (an array of long values)
    * @param extract    a distance, in bits, by which each entry of the table
    *                   is to be right-shifted before it is processed
    * @param bits       the number of bits (not bytes) to be used to represent
    *                   each table entry
    * @param size       the table data is divided up into blocks of size (1<<size);
    *                   in this method, this information is used only to affect
    *                   how many table values are to be generated per line
    * @param preshifted if this flag is true, then the table entries are to be
    *                   emitted in a preshifted form; that is, each value should
    *                   be left-shifted by the amount "shift", so that this work
    *                   is built into the table and need not be performed by an
    *                   explicit shift operator at run time
    * @param shift      this is the shift amount for preshifting of table entries
    * @param hexFormat  if this flag is true, table entries should be emitted as
    *                   hexadecimal literals; otherwise decimal literals are used
    * @param properties if this flag is true, the table entries are encoded
    *                   character properties rather than indexes into yet other tables;
    *                   therefore comments describing the encoded properties should
    *                   be generated
    * @param hexComment if this flag is true, each line of output is labelled with
    *                   a hexadecimal comment indicating the character values to
    *                   which that line applies; otherwise, decimal values indicating
    *                   table indices are generated
    *
    * @see GenerateCharacter#genTables
    * @see GenerateCharacter#replaceCommand
    */

    static void genTable(StringBuffer result, String name,
                         long[] table, int extract, int bits, int size,
                         boolean preshifted, int shift, boolean hexFormat,
                         boolean properties, boolean hexComment) {

        String atype = bits == 1 ? (Csyntax ? "unsigned long" : "int") :
            bits == 2 ? (Csyntax ? "unsigned long" : "int") :
            bits == 4 ? (Csyntax ? "unsigned long" : "int") :
            bits == 8 ? (Csyntax ? "unsigned char" : "byte") :
            bits == 16 ? (Csyntax ? "unsigned short" : "char") :
            bits == 32 ? (Csyntax ? "unsigned long" : "int") :
            (Csyntax ? "int64" : "long");
        long maxPosEntry = bits == 1 ? Integer.MAX_VALUE : // liu
            bits == 2 ? Integer.MAX_VALUE :
            bits == 4 ? Integer.MAX_VALUE :
            bits == 8 ? Byte.MAX_VALUE :
            bits == 16 ? Short.MAX_VALUE :
            bits == 32 ? Integer.MAX_VALUE :
            Long.MAX_VALUE;
        int entriesPerChar = bits <= 16 ? (16 / bits) : -(bits / 16);
        boolean shiftEntries = preshifted && shift != 0;
        if (bits == 8 && tableAsString && useCharForByte) {
            atype = "char";
            maxPosEntry = Character.MAX_VALUE;
            entriesPerChar = 1;
        }
        boolean noConversion = atype.equals("char");

        result.append(commentStart);
        result.append(" The ").append(name).append(" table has ").append(table.length);
        result.append(" entries for a total of ");
        int sizeOfTable = ((table.length * bits + 31) >> 5) << 2;
        if (bits == 8 && tableAsString && useCharForByte) {
            sizeOfTable *= 2;
        }
        result.append(sizeOfTable);
        result.append(" bytes.").append(commentEnd).append("\n\n");
        if (Csyntax)
            result.append("  static ");
        else
            result.append("  static final ");
        result.append(atype);
        result.append(" ").append(name).append("[");
        if (Csyntax)
            result.append(table.length >> (bits == 1 ? 5 : bits == 2 ? 4 : bits == 4 ? 3 : 0));
        if (tableAsString) {
            if (noConversion) {
                result.append("] = (\n");
            } else {
                result.append("] = new ").append(atype).append("[").append(table.length).append("];\n  ");
                result.append("static final String ").append(name).append("_DATA =\n");
            }
            StringBuilder theString = new StringBuilder();
            int entriesInCharSoFar = 0;
            char ch = '\u0000';
            int charsPerEntry = -entriesPerChar;
            for (long l : table) {
                long entry;
                if ("A".equals(name))
                    entry = (l & 0xffffffffL) >> extract;
                else
                    entry = (l >> extract);
                if (shiftEntries) entry <<= shift;
                if (entry >= (1L << bits)) {
                    FAIL("Entry too big");
                }
                if (entriesPerChar > 0) {
                    // Pack multiple entries into a character
                    ch = (char) (((int) ch >> bits) | (entry << (entriesPerChar - 1) * bits));
                    ++entriesInCharSoFar;
                    if (entriesInCharSoFar == entriesPerChar) {
                        // Character is full
                        theString.append(ch);
                        entriesInCharSoFar = 0;
                        ch = '\u0000';
                    }
                } else {
                    // Use multiple characters per entry
                    for (int k = 0; k < charsPerEntry; ++k) {
                        ch = (char) (entry >> ((charsPerEntry - 1) * 16));
                        entry <<= 16;
                        theString.append(ch);
                    }
                }
            }
            if (entriesInCharSoFar > 0) {
                while (entriesInCharSoFar < entriesPerChar) {
                    ch = (char)((int)ch >> bits);
                    ++entriesInCharSoFar;
                }
                theString.append(ch);
            }
            result.append(Utility.formatForSource(theString.toString(), "    "));
            if (noConversion) {
                result.append(").toCharArray()");
            }
            result.append(";\n\n  ");

            if (!noConversion) {
                addInitializer(name, atype, entriesPerChar, bits, table.length);
            }
        }
        else {
            result.append("] = {");
            boolean castEntries = shiftEntries && (bits < 32);
            int printPerLine = hexFormat ? (bits == 1 ? 32*4 :
                bits == 2 ? 16*4 :
                bits == 4 ? 8*4 :
                bits == 8 ? 8 :
                bits == 16 ? 8 :
                bits == 32 ? 4 : 2) :
                (bits == 8 ? 8 :
                bits == 16 ? 8 : 4);
            int printMask = properties ? 0 :
            Math.min(1 << size,
                printPerLine >> (castEntries ? (Csyntax ? 2 : 1) : 0)) - 1;
            int commentShift = ((1 << size) == table.length) ? 0 : size;
            int commentMask = ((1 << size) == table.length) ? printMask : (1 << size) - 1;
            long val = 0;
            for (int j = 0; j < table.length; j++) {
                if ((j & printMask) == 0) {
                    while (result.charAt(result.length() - 1) == ' ')
                        result.setLength(result.length() - 1);
                    result.append("\n    ");
                }
        PRINT:  {
                if (castEntries)
                    result.append("(").append(atype).append(")(");
                long entry = table[j] >> extract;
                int packMask = ((1 << (bits == 1 ? 5 : bits == 2 ? 4 : bits == 4 ? 3 : 2)) - 1);
                int k = j & packMask;
                if (bits >= 8)
                    val = entry;
                else if (k == 0) {
                    val = entry;
                    break PRINT;
                }
                else {
                    val |= (entry << (k*bits));
                    if (k != packMask)
                        break PRINT;
                }
                if (val > maxPosEntry && !Csyntax) { // liu
                // For values that are out of range, convert them to in-range negative values.
                // Actually, output the '-' and convert them to the negative of the corresponding
                // in-range negative values.  E.g., convert 130 == -126 (in 8 bits) -> 126.
                    result.append('-');
                    val = maxPosEntry + maxPosEntry + 2 - val;
                }
                if (hexFormat) {
                    result.append("0x");
                    if (bits == 8)
                        result.append(hex2((byte)val));
                    else if (bits == 16)
                        result.append(hex4((short)val));
                    else if (bits == 32 || bits < 8)
                        result.append(hex8((int)val));
                    else {
                        result.append(hex16(val));
                        if (!Csyntax)
                            result.append("L");
                    }
                }
                else {
                    if (bits == 8)
                        result.append(dec3(val));
                    else if (bits == 64) {
                        result.append(dec5(val));
                        if (!Csyntax)
                            result.append("L");
                    }
                    else
                        result.append(dec5(val));
                }
                if (shiftEntries)
                    result.append("<<").append(shift);
                if (castEntries) result.append(")");
                if (j < (table.length - 1))
                    result.append(", ");
                else
                    result.append("  ");
                if ((j & printMask) == printMask) {
                    result.append(" ").append(commentStart).append(" ");
                    if (hexComment)
                        result.append("0x").append(hex4((j & ~commentMask) << (16 - size)));
                    else
                        result.append(dec3((j & ~commentMask) >> commentShift));
                    if (properties) propertiesComments(result, val << extract);
                    result.append(commentEnd);
                }
                } // end PRINT
            }
            result.append("\n  };\n\n  ");
        }
    }

    static void genCaseMapTableDeclaration(StringBuffer result) {
        result.append("    static final char[][][] charMap;\n");
    }

    static void genCaseMapTable(StringBuffer result, SpecialCaseMap[] specialCaseMaps){
        String myTab = "    ";
        int ch;
        char[] map;
        result.append(myTab).append("charMap = new char[][][] {\n");
        for (SpecialCaseMap specialCaseMap : specialCaseMaps) {
            ch = specialCaseMap.getCharSource();
            map = specialCaseMap.getUpperCaseMap();
            result.append(myTab).append(myTab).append("{ ");
            result.append("{'\\u").append(hex4(ch)).append("'}, {");
            for (char c : map) {
                result.append("'\\u").append(hex4(c)).append("', ");
            }
            result.append("} },\n");
        }
        result.append(myTab).append("};\n");

    }

    /**
    * The propertiesComments method generates comments describing encoded
    * character properties.
    *
    * @param result     a StringBuffer, to which the generated source code
    *                   text is to be appended
    * @param val                encoded character properties
    *
    * @see GenerateCharacter#genTable
    */

    static void propertiesComments(StringBuffer result, long val) {
        result.append("   ");
        switch ((int) (val & maskType)) {
            case UnicodeSpec.CONTROL -> result.append("Cc");
            case UnicodeSpec.FORMAT -> result.append("Cf");
            case UnicodeSpec.PRIVATE_USE -> result.append("Co");
            case UnicodeSpec.SURROGATE -> result.append("Cs");
            case UnicodeSpec.LOWERCASE_LETTER -> result.append("Ll");
            case UnicodeSpec.MODIFIER_LETTER -> result.append("Lm");
            case UnicodeSpec.OTHER_LETTER -> result.append("Lo");
            case UnicodeSpec.TITLECASE_LETTER -> result.append("Lt");
            case UnicodeSpec.UPPERCASE_LETTER -> result.append("Lu");
            case UnicodeSpec.COMBINING_SPACING_MARK -> result.append("Mc");
            case UnicodeSpec.ENCLOSING_MARK -> result.append("Me");
            case UnicodeSpec.NON_SPACING_MARK -> result.append("Mn");
            case UnicodeSpec.DECIMAL_DIGIT_NUMBER -> result.append("Nd");
            case UnicodeSpec.LETTER_NUMBER -> result.append("Nl");
            case UnicodeSpec.OTHER_NUMBER -> result.append("No");
            case UnicodeSpec.CONNECTOR_PUNCTUATION -> result.append("Pc");
            case UnicodeSpec.DASH_PUNCTUATION -> result.append("Pd");
            case UnicodeSpec.END_PUNCTUATION -> result.append("Pe");
            case UnicodeSpec.OTHER_PUNCTUATION -> result.append("Po");
            case UnicodeSpec.START_PUNCTUATION -> result.append("Ps");
            case UnicodeSpec.CURRENCY_SYMBOL -> result.append("Sc");
            case UnicodeSpec.MODIFIER_SYMBOL -> result.append("Sk");
            case UnicodeSpec.MATH_SYMBOL -> result.append("Sm");
            case UnicodeSpec.OTHER_SYMBOL -> result.append("So");
            case UnicodeSpec.LINE_SEPARATOR -> result.append("Zl");
            case UnicodeSpec.PARAGRAPH_SEPARATOR -> result.append("Zp");
            case UnicodeSpec.SPACE_SEPARATOR -> result.append("Zs");
            case UnicodeSpec.UNASSIGNED -> result.append("unassigned");
        }

        switch ((int) ((val & maskBidi) >> shiftBidi)) {
            case UnicodeSpec.DIRECTIONALITY_LEFT_TO_RIGHT -> result.append(", L");
            case UnicodeSpec.DIRECTIONALITY_RIGHT_TO_LEFT -> result.append(", R");
            case UnicodeSpec.DIRECTIONALITY_EUROPEAN_NUMBER -> result.append(", EN");
            case UnicodeSpec.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR -> result.append(", ES");
            case UnicodeSpec.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR -> result.append(", ET");
            case UnicodeSpec.DIRECTIONALITY_ARABIC_NUMBER -> result.append(", AN");
            case UnicodeSpec.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR -> result.append(", CS");
            case UnicodeSpec.DIRECTIONALITY_PARAGRAPH_SEPARATOR -> result.append(", B");
            case UnicodeSpec.DIRECTIONALITY_SEGMENT_SEPARATOR -> result.append(", S");
            case UnicodeSpec.DIRECTIONALITY_WHITESPACE -> result.append(", WS");
            case UnicodeSpec.DIRECTIONALITY_OTHER_NEUTRALS -> result.append(", ON");
        }
        if ((val & maskUpperCase) != 0) {
            result.append(", hasUpper (subtract ");
            result.append((val & maskCaseOffset) >> shiftCaseOffset).append(")");
        }
        if ((val & maskLowerCase) != 0) {
            result.append(", hasLower (add ");
            result.append((val & maskCaseOffset) >> shiftCaseOffset).append(")");
        }
        if ((val & maskTitleCase) != 0) {
            result.append(", hasTitle");
        }
        if ((val & maskIdentifierInfo) == valueIgnorable) {
            result.append(", ignorable");
        }
        if ((val & maskIdentifierInfo) == valueJavaUnicodePart) {
            result.append(", identifier part");
        }
        if ((val & maskIdentifierInfo) == valueJavaStartUnicodePart) {
            result.append(", underscore");
        }
        if ((val & maskIdentifierInfo) == valueJavaWhitespace) {
            result.append(", whitespace");
        }
        if ((val & maskIdentifierInfo) == valueJavaOnlyStart) {
            result.append(", currency");
        }
        if ((val & maskIdentifierInfo) == valueJavaUnicodeStart) {
            result.append(", identifier start");
        }
        if ((val & maskNumericType) == valueDigit) {
            result.append(", decimal ");
            result.append((val & maskDigitOffset) >> shiftDigitOffset);
        }
        if ((val & maskNumericType) == valueStrangeNumeric) {
            result.append(", strange");
        }
        if ((val & maskNumericType) == valueJavaSupradecimal) {
            result.append(", supradecimal ");
            result.append((val & maskDigitOffset) >> shiftDigitOffset);
        }
    }

    static String[] tableNames = { "X", "Y", "Z", "P", "Q", "R", "S", "T", "U", "V", "W" };

    static String tableName(int j) { return tableNames[j]; }

    /**
    * The genAccess method generates source code for one table access expression.
    *
    * Most of the complexity stems from handling various options as to
    * table representation, such as whether it contains values so large that
    * they are represented as negative values and whether the table values are
    * preshifted.  This method also avoids such "ugly" expressions as shifting
    * by distance zero, masking when no masking is necessary, and so on.
    * For clarity, it generates expressions that do not rely on operator
    * precedence, but otherwise it avoids generating redundant parentheses.
    *
    * A generated expression might look like A[Y[(X[ch>>6]<<6)|(ch&0x3F)]]
    * or A[Z[Y[(X[ch>>7]<<4)|((ch>>3)&0xF)]|(ch&0x7)]], for example.
    *
    * @param tbl                the name of the final table to be accessed
    * @param var                the variable name that appeared in parentheses in the
    *                           "Lookup" command
    * @param bits       the number of bits (not bytes) to be used to represent
    *                   the final table entry
    * @return   the replacement text for the "Lookup(xxx)" command, as a String
    *
    * @see GenerateCharacter#replaceCommand
    */

    static String genAccess(String tbl, String var, int bits) {
        String access = null;
        int bitoffset = bits == 1 ? 5 : bits == 2 ? 4 : bits == 4 ? 3 : 0;
        for (int k = 0; k < sizes.length; k++) {
            int offset = ((k < sizes.length - 1) ? 0 : bitoffset);
            int shift = shifts[k] + offset;
            String shifted = (shift == 0) ? var : "(" + var + ">>" + shift + ")";
            int mask = (1 << (sizes[k] - offset)) - 1;
            String masked = (k == 0) ? shifted :
              "(" + shifted + "&0x" + hex(mask) + ")";
            String index = (k == 0) ? masked :
             (mask == 0) ? access : "(" + access + "|" + masked + ")";
            String indexNoParens = (index.charAt(0) != '(') ? index :
                 index.substring(1, index.length() - 1);
            String tblname = (k == sizes.length - 1) ? tbl : tableName(k);
            String fetched = tblname + "[" + indexNoParens + "]";
            String zeroextended = (zeroextend[k] == 0) ? fetched :
                "(" + fetched + "&0x" + hex(zeroextend[k]) + ")";
            int adjustment = preshifted[k] ? 0 :
               sizes[k+1] - ((k == sizes.length - 2) ? bitoffset : 0);
            String adjusted = (preshifted[k] || adjustment == 0) ? zeroextended :
                "(" + zeroextended + "<<" + adjustment + ")";
            String bitshift = (bits == 1) ? "(" + var + "&0x1F)" :
                (bits == 2) ? "((" + var + "&0xF)<<1)" :
                (bits == 4) ? "((" + var + "&7)<<2)" : null;
            access = ((k < sizes.length - 1) || (bits >= 8)) ? adjusted :
                "((" + adjusted + ">>" + bitshift + ")&" +
                (bits == 4 ? "0xF" : "" + ((1 << bits) - 1)) + ")";
        }
        return access;
    }

    /* The command line arguments are decoded and used to set the following
     global variables.
     */

    static boolean verbose = false;
    static boolean debug = false;
    static boolean nobidi = false;
    static boolean nomirror = false;
    static boolean identifiers = false;
    static boolean Csyntax = false;
    static String TemplateFileName = null;
    static String OutputFileName = null;
    static String UnicodeSpecFileName = null; // liu
    static String SpecialCasingFileName = null;
    static String PropListFileName = null;
    static String DerivedPropsFileName = null;
    static boolean useCharForByte = false;
    static int[] sizes;
    static int bins = 0; // liu; if > 0, then perform search
    static boolean tableAsString = false;
    static boolean bLatin1 = false;

    static String commandLineDescription;

    /* Other global variables, equal in length to the "sizes" array. */

    static int[] shifts;
    static int[] zeroextend;
    static int[] bytes;
    static boolean[] preshifted;
    static long[][] tables;


    /* Other global variables */
    static String commentStart;
    static String commentEnd;

    static StringBuffer initializers = new StringBuffer();

    /* special casing rules for 1:M toUpperCase mappings */
    static SpecialCaseMap[] specialCaseMaps;

    /**
    * Process the command line arguments.
    *
    * The allowed flags in command line are:
    * <dl>
    * <dt> -verbose             <dd> Emit comments to standard output describing
    *                                   what's going on during the processing.
    * <dt> -nobidi              <dd> Do not include bidi categories in the
    *                                   encoded character properties.
    * <dt> -nomirror    <dd> Do no include mirror property in the encoded
    *                        character properties.
    * <dt> -identifiers         <dd> Generate tables for scanning identifiers only.
    * <dt> -c                   <dd> Output code in C syntax instead of Java syntax.
    * <dt> -o filename          <dd> Specify output file name.
    * <dt> -template filename   <dd> Specify template input file name.
    * <dt> -spec filename        <dd> Specify Unicode spec file name.
    * <dt> -specialcasing filename <dd> Specify Unicode special casing file name.
    * <dt> -search bins          <dd> Try different partitions into the specified
    *                                    number of bins.  E.g., for 2 bins, try
    *                                    16 0, 15 1,..., 0 16.
    * <dt> -string               <dd> Create table as string.  Only valid with Java
    *                                    syntax.
    * <dt> -latin1          <dd> Create a latin 1 only property table.
    * </dl>
    * In addition, decimal literals may appear as command line arguments;
    * each one represents the number of bits of the character to be broken
    * off at each lookup step.  If present, they must add up to 16 (the number
    * of bits in a char value).  For smaller tables, the last value should
    * be 0; values other than the last one may not be zero.  If no such
    * numeric values are provided, default values are used.
    *
    * @param args       the command line arguments, as an array of String
    *
    * @see GenerateCharacter#main
    */

    static void processArgs(String[] args) {
        StringBuilder desc = new StringBuilder("java GenerateCharacter");
        for (String arg : args) {
            desc.append(" ").append(arg);
        }
        for (int j = 0; j < args.length; j++) {
            if (args[j].equals("-verbose") || args[j].equals("-v"))
                verbose = true;
            else if (args[j].equals("-d"))
                debug = true;
            else if (args[j].equals("-nobidi"))
                nobidi = true;
            else if (args[j].equals("-nomirror"))
                nomirror = true;
            else if (args[j].equals("-identifiers"))
                identifiers = true;
            else if (args[j].equals("-c"))
                Csyntax = true;
            else if (args[j].equals("-string"))
                tableAsString = true;
            else if (args[j].equals("-o")) {
                if (j == args.length - 1) {
                    FAIL("File name missing after -o");
                }
                else {
                    OutputFileName = args[++j];
                }
            }
            else if (args[j].equals("-search")) {
                if (j == args.length - 1)
                    FAIL("Bin count missing after -search");
                else {
                    bins = Integer.parseInt(args[++j]);
                    if (bins < 1 || bins > 10)
                        FAIL("Bin count must be >= 1 and <= 10");
                }
            }
            else if (args[j].equals("-template")) {
                if (j == args.length - 1)
                    FAIL("File name missing after -template");
                else
                    TemplateFileName = args[++j];
            }
            else if (args[j].equals("-spec")) { // liu
                if (j == args.length - 1) {
                    FAIL("File name missing after -spec");
                }
                else {
                    UnicodeSpecFileName = args[++j];
                }
            }
            else if (args[j].equals("-specialcasing")) {
                if (j == args.length -1) {
                    FAIL("File name missing after -specialcasing");
                }
                else {
                    SpecialCasingFileName = args[++j];
                }
            }
            else if (args[j].equals("-proplist")) {
                if (j == args.length -1) {
                    FAIL("File name missing after -proplist");
                }
                else {
                    PropListFileName = args[++j];
                }
            }
            else if (args[j].equals("-derivedprops")) {
                if (j == args.length -1) {
                    FAIL("File name missing after -derivedprops");
                }
                else {
                    DerivedPropsFileName = args[++j];
                }
            }
            else if (args[j].equals("-plane")) {
                if (j == args.length -1) {
                    FAIL("Plane number missing after -plane");
                }
                else {
                    plane = Integer.parseInt(args[++j]);
                }
                if (plane > 0) {
                    bLatin1 = false;
                }
            }
            else if ("-usecharforbyte".equals(args[j])) {
                useCharForByte = true;
            }
            else if (args[j].equals("-latin1")) {
                bLatin1 = true;
                plane = 0;
            }
            else {
                try {
                    int val = Integer.parseInt(args[j]);
                    if (val < 0 || val > 32) FAIL("Incorrect bit field width: " + args[j]);
                    if (sizes == null)
                        sizes = new int[1];
                    else {
                        int[] newsizes = new int[sizes.length + 1];
                        System.arraycopy(sizes, 0, newsizes, 0, sizes.length);
                        sizes = newsizes;
                    }
                    sizes[sizes.length - 1] = val;
                }
                catch(NumberFormatException e) {
                    FAIL("Unknown switch: " + args[j]);
                }
            }
        }
        if (Csyntax && tableAsString) {
            FAIL("Can't specify table as string with C syntax");
        }
        if (sizes == null) {
            desc.append(" [");
            if (identifiers) {
                int[] newsizes = { 8, 4, 4 };           // Good default values
                desc.append("8 4 4]");
                sizes = newsizes;
            }
            else {
                int[] newsizes = { 10, 5, 1 }; // Guy's old defaults for 2.0.14: { 9, 4, 3, 0 }
                desc.append("10 5 1]");
                sizes = newsizes;
            }
        }
        if (UnicodeSpecFileName == null) { // liu
            UnicodeSpecFileName = DefaultUnicodeSpecFileName;
            desc.append(" [-spec " + UnicodeSpecFileName + ']');
        }
        if (SpecialCasingFileName == null) {
            SpecialCasingFileName = DefaultSpecialCasingFileName;
            desc.append(" [-specialcasing " + SpecialCasingFileName + ']');
        }
        if (PropListFileName == null) {
            PropListFileName = DefaultPropListFileName;
            desc.append(" [-proplist " + PropListFileName + ']');
        }
        if (DerivedPropsFileName == null) {
            DerivedPropsFileName = DefaultDerivedPropsFileName;
            desc.append(" [-derivedprops " + DerivedPropsFileName + ']');
        }
        if (TemplateFileName == null) {
            TemplateFileName = (Csyntax ? DefaultCTemplateFileName
                  : DefaultJavaTemplateFileName);
            desc.append(" [-template " + TemplateFileName + ']');
        }
        if (OutputFileName == null) {
            OutputFileName = (Csyntax ? DefaultCOutputFileName
                    : DefaultJavaOutputFileName);
            desc.append(" [-o " + OutputFileName + ']');
        }
        commentStart = (Csyntax ? "/*" : "//");
        commentEnd = (Csyntax ? " */" : "");
        commandLineDescription = desc.toString().replace("\\", "\\\\");
    }

    private static void searchBins(long[] map, int binsOccupied) throws Exception {
        int bitsFree = 16;
        for (int i = 0; i < binsOccupied; ++i) bitsFree -= sizes[i];
        if (binsOccupied == (bins-1)) {
            sizes[binsOccupied] = bitsFree;
            generateForSizes(map);
        }
        else {
            for (int i = 1; i < bitsFree; ++i) { // Don't allow bins of 0 except for last one
                sizes[binsOccupied] = i;
                searchBins(map, binsOccupied+1);
            }
        }
    }

    private static void generateForSizes(long[] map) throws Exception {
        int sum = 0;
        shifts = new int[sizes.length];
        for (int k = sizes.length - 1; k >= 0; k--) {
            shifts[k] = sum;
            sum += sizes[k];
        }
        if ((1 << sum) < map.length || (1 << (sum - 1)) >= map.length) {
            FAIL("Bit field widths total to " + sum +
             ": wrong total for map of size " + map.length);
        }
        // need a table for each set of lookup bits in char
        tables = new long[sizes.length][];
        // the last table is the map
        tables[sizes.length - 1] = map;
        for (int j = sizes.length - 1; j > 0; j--) {
            if (verbose && bins==0)
                System.err.println("Building map " + (j+1) + " of bit width " + sizes[j]);
            long[][] temp = buildTable(tables[j], sizes[j]);
            tables[j - 1] = temp[0];
            tables[j] = temp[1];
        }
        preshifted = new boolean[sizes.length];
        zeroextend = new int[sizes.length];
        bytes = new int[sizes.length];
        for (int j = 0; j < sizes.length - 1; j++) {
            int len = tables[j + 1].length;
            int size = sizes[j + 1];
            if (len > 0x100 && (len >> size) <= 0x100) {
                len >>= size;
                preshifted[j] = false;
            }
            else if (len > 0x10000 && (len >> size) <= 0x10000) {
                len >>= size;
                preshifted[j] = false;
            }
            else preshifted[j] = true;
            if (Csyntax)
                zeroextend[j] = 0;
            else if (len > 0x7F && len <= 0xFF) {
                if (!useCharForByte) {
                    zeroextend[j] = 0xFF;
                }
            } else if (len > 0x7FFF && len <= 0xFFFF)
                zeroextend[j] = 0xFFFF;
            else zeroextend[j] = 0;
            if (len <= 0x100) bytes[j] = 1;
            else if (len <= 0x10000) bytes[j] = 2;
            else bytes[j] = 4;
        }
        preshifted[sizes.length - 1] = true;
        zeroextend[sizes.length - 1] = 0;
        bytes[sizes.length - 1] = 0;
        if (bins > 0) {
            int totalBytes = getTotalBytes();
            String access = genAccess("A", "ch", (identifiers ? 2 : 32));
            int accessComplexity = 0;
            for (int j=0; j<access.length(); ++j) {
                char ch = access.charAt(j);
                if ("[&|><".indexOf(ch) >= 0) ++accessComplexity;
                if (ch == '<' || ch == '>') ++j;
            }
            System.out.print("(");
            for (int size : sizes) {
                System.out.print(" " + size);
            }
            System.out.println(" ) " + totalBytes + " " + accessComplexity + " " + access);
            return;
        }
        if (verbose) {
            System.out.println("    n\t size\tlength\tshift\tzeroext\tbytes\tpreshifted");
            for (int j = 0; j < sizes.length; j++) {
                System.out.println(dec5(j) + "\t" +
                    dec5(sizes[j]) + "\t" +
                    dec5(tables[j].length) + "\t" +
                    dec5(shifts[j]) + "\t" +
                    dec5(zeroextend[j]) + "\t" +
                    dec5(bytes[j]) + "\t " +
                    preshifted[j]);
            }
        }
        if (verbose) {
            System.out.println("Generating source code for class Character");
            System.out.println("A table access looks like " +
                         genAccess("A", "ch", (identifiers ? 2 : 32)));
        }
        generateCharacterClass(TemplateFileName, OutputFileName);
    }

    /**
    * The main program for generating source code for the Character class.
    * The basic outline of its operation is:
    * <ol>
    * <li> Process the command line arguments.  One result of this process
    *           is a list of sizes (measured in bits and summing to 16).
    * <li> Get the Unicode character property data from the specification file.
    * <li> From that, build a map that has, for each character code, its
    *           relevant properties encoded as a long integer value.
    * <li> Repeatedly compress the map, producing a compressed table and a
    *           new map.  This is done once for each size value in the list.
    *           When this is done, we have a set of tables.
    * <li> Make some decisions about table representation; record these
    *           decisions in arrays named preshifted, zeroextend, and bytes.
    * <li> Generate the source code for the class Character by performing
    *           macro processing on a template file.
    * </ol>
    *
    * @param args       the command line arguments, as an array of String
    *
    * @see GenerateCharacter#processArgs
    * @see UnicodeSpec@readSpecFile
    * @see GenerateCharacter#buildMap
    * @see GenerateCharacter#buildTable
    * @see GenerateCharacter#generateCharacterClass
    */

    public static void main(String[] args) {
        processArgs(args);
        try {

            UnicodeSpec[] data = UnicodeSpec.readSpecFile(new File(UnicodeSpecFileName), plane);
            specialCaseMaps = SpecialCaseMap.readSpecFile(new File(SpecialCasingFileName), plane);
            PropList propList = PropList.readSpecFile(new File(PropListFileName), plane);
            propList.putAll(PropList.readSpecFile(new File(DerivedPropsFileName), plane));

            if (verbose) {
                System.out.println(data.length + " items read from Unicode spec file " + UnicodeSpecFileName); // liu
            }
            long[] map = buildMap(data, specialCaseMaps, propList);
            if (verbose) {
                System.err.println("Completed building of initial map");
            }

            if (bins == 0) {
                generateForSizes(map);
            }
            else {
                while (bins > 0) {
                    sizes = new int[bins];
                    searchBins(map, 0);
                    --bins;
                }
            }
            if (verbose && false) {
                System.out.println("Offset range seen: -" + hex8(-minOffsetSeen) + "..+" +
                             hex8(maxOffsetSeen));
                System.out.println("          allowed: -" + hex8(-minOffset) + "..+" +
                             hex8(maxOffset));
            }
        }
        catch (IOException e) { FAIL(e.toString()); }
        catch (Throwable e) {
            System.out.println("Unexpected exception:");
            e.printStackTrace();
            FAIL("Unexpected exception!");
        }
        if (verbose) { System.out.println("Done!");}
    }

}   // end class
