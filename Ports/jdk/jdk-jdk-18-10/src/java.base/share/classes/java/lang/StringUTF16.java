/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Locale;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import jdk.internal.util.ArraysSupport;
import jdk.internal.vm.annotation.DontInline;
import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.annotation.IntrinsicCandidate;

import static java.lang.String.UTF16;
import static java.lang.String.LATIN1;

final class StringUTF16 {

    public static byte[] newBytesFor(int len) {
        if (len < 0) {
            throw new NegativeArraySizeException();
        }
        if (len > MAX_LENGTH) {
            throw new OutOfMemoryError("UTF16 String size is " + len +
                                       ", should be less than " + MAX_LENGTH);
        }
        return new byte[len << 1];
    }

    @IntrinsicCandidate
    // intrinsic performs no bounds checks
    static void putChar(byte[] val, int index, int c) {
        assert index >= 0 && index < length(val) : "Trusted caller missed bounds check";
        index <<= 1;
        val[index++] = (byte)(c >> HI_BYTE_SHIFT);
        val[index]   = (byte)(c >> LO_BYTE_SHIFT);
    }

    @IntrinsicCandidate
    // intrinsic performs no bounds checks
    static char getChar(byte[] val, int index) {
        assert index >= 0 && index < length(val) : "Trusted caller missed bounds check";
        index <<= 1;
        return (char)(((val[index++] & 0xff) << HI_BYTE_SHIFT) |
                      ((val[index]   & 0xff) << LO_BYTE_SHIFT));
    }

    public static int length(byte[] value) {
        return value.length >> 1;
    }

    private static int codePointAt(byte[] value, int index, int end, boolean checked) {
        assert index < end;
        if (checked) {
            checkIndex(index, value);
        }
        char c1 = getChar(value, index);
        if (Character.isHighSurrogate(c1) && ++index < end) {
            if (checked) {
                checkIndex(index, value);
            }
            char c2 = getChar(value, index);
            if (Character.isLowSurrogate(c2)) {
               return Character.toCodePoint(c1, c2);
            }
        }
        return c1;
    }

    public static int codePointAt(byte[] value, int index, int end) {
       return codePointAt(value, index, end, false /* unchecked */);
    }

    private static int codePointBefore(byte[] value, int index, boolean checked) {
        --index;
        if (checked) {
            checkIndex(index, value);
        }
        char c2 = getChar(value, index);
        if (Character.isLowSurrogate(c2) && index > 0) {
            --index;
            if (checked) {
                checkIndex(index, value);
            }
            char c1 = getChar(value, index);
            if (Character.isHighSurrogate(c1)) {
               return Character.toCodePoint(c1, c2);
            }
        }
        return c2;
    }

    public static int codePointBefore(byte[] value, int index) {
        return codePointBefore(value, index, false /* unchecked */);
    }

    private static int codePointCount(byte[] value, int beginIndex, int endIndex, boolean checked) {
        assert beginIndex <= endIndex;
        int count = endIndex - beginIndex;
        int i = beginIndex;
        if (checked && i < endIndex) {
            checkBoundsBeginEnd(i, endIndex, value);
        }
        for (; i < endIndex - 1; ) {
            if (Character.isHighSurrogate(getChar(value, i++)) &&
                Character.isLowSurrogate(getChar(value, i))) {
                count--;
                i++;
            }
        }
        return count;
    }

    public static int codePointCount(byte[] value, int beginIndex, int endIndex) {
        return codePointCount(value, beginIndex, endIndex, false /* unchecked */);
    }

    public static char[] toChars(byte[] value) {
        char[] dst = new char[value.length >> 1];
        getChars(value, 0, dst.length, dst, 0);
        return dst;
    }

    @IntrinsicCandidate
    public static byte[] toBytes(char[] value, int off, int len) {
        byte[] val = newBytesFor(len);
        for (int i = 0; i < len; i++) {
            putChar(val, i, value[off]);
            off++;
        }
        return val;
    }

    public static byte[] compress(char[] val, int off, int len) {
        byte[] ret = new byte[len];
        if (compress(val, off, ret, 0, len) == len) {
            return ret;
        }
        return null;
    }

    public static byte[] compress(byte[] val, int off, int len) {
        byte[] ret = new byte[len];
        if (compress(val, off, ret, 0, len) == len) {
            return ret;
        }
        return null;
    }

    // compressedCopy char[] -> byte[]
    @IntrinsicCandidate
    public static int compress(char[] src, int srcOff, byte[] dst, int dstOff, int len) {
        for (int i = 0; i < len; i++) {
            char c = src[srcOff];
            if (c > 0xFF) {
                len = 0;
                break;
            }
            dst[dstOff] = (byte)c;
            srcOff++;
            dstOff++;
        }
        return len;
    }

    // compressedCopy byte[] -> byte[]
    @IntrinsicCandidate
    public static int compress(byte[] src, int srcOff, byte[] dst, int dstOff, int len) {
        // We need a range check here because 'getChar' has no checks
        checkBoundsOffCount(srcOff, len, src);
        for (int i = 0; i < len; i++) {
            char c = getChar(src, srcOff);
            if (c > 0xFF) {
                len = 0;
                break;
            }
            dst[dstOff] = (byte)c;
            srcOff++;
            dstOff++;
        }
        return len;
    }

    public static byte[] toBytes(int[] val, int index, int len) {
        final int end = index + len;
        // Pass 1: Compute precise size of char[]
        int n = len;
        for (int i = index; i < end; i++) {
            int cp = val[i];
            if (Character.isBmpCodePoint(cp))
                continue;
            else if (Character.isValidCodePoint(cp))
                n++;
            else throw new IllegalArgumentException(Integer.toString(cp));
        }
        // Pass 2: Allocate and fill in <high, low> pair
        byte[] buf = newBytesFor(n);
        for (int i = index, j = 0; i < end; i++, j++) {
            int cp = val[i];
            if (Character.isBmpCodePoint(cp)) {
                putChar(buf, j, cp);
            } else {
                putChar(buf, j++, Character.highSurrogate(cp));
                putChar(buf, j, Character.lowSurrogate(cp));
            }
        }
        return buf;
    }

    public static byte[] toBytes(char c) {
        byte[] result = new byte[2];
        putChar(result, 0, c);
        return result;
    }

    static byte[] toBytesSupplementary(int cp) {
        byte[] result = new byte[4];
        putChar(result, 0, Character.highSurrogate(cp));
        putChar(result, 1, Character.lowSurrogate(cp));
        return result;
    }

    @IntrinsicCandidate
    public static void getChars(byte[] value, int srcBegin, int srcEnd, char dst[], int dstBegin) {
        // We need a range check here because 'getChar' has no checks
        if (srcBegin < srcEnd) {
            checkBoundsOffCount(srcBegin, srcEnd - srcBegin, value);
        }
        for (int i = srcBegin; i < srcEnd; i++) {
            dst[dstBegin++] = getChar(value, i);
        }
    }

    /* @see java.lang.String.getBytes(int, int, byte[], int) */
    public static void getBytes(byte[] value, int srcBegin, int srcEnd, byte dst[], int dstBegin) {
        srcBegin <<= 1;
        srcEnd <<= 1;
        for (int i = srcBegin + (1 >> LO_BYTE_SHIFT); i < srcEnd; i += 2) {
            dst[dstBegin++] = value[i];
        }
    }

    @IntrinsicCandidate
    public static boolean equals(byte[] value, byte[] other) {
        if (value.length == other.length) {
            int len = value.length >> 1;
            for (int i = 0; i < len; i++) {
                if (getChar(value, i) != getChar(other, i)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @IntrinsicCandidate
    public static int compareTo(byte[] value, byte[] other) {
        int len1 = length(value);
        int len2 = length(other);
        return compareValues(value, other, len1, len2);
    }

    /*
     * Checks the boundary and then compares the byte arrays.
     */
    public static int compareTo(byte[] value, byte[] other, int len1, int len2) {
        checkOffset(len1, value);
        checkOffset(len2, other);

        return compareValues(value, other, len1, len2);
    }

    private static int compareValues(byte[] value, byte[] other, int len1, int len2) {
        int lim = Math.min(len1, len2);
        for (int k = 0; k < lim; k++) {
            char c1 = getChar(value, k);
            char c2 = getChar(other, k);
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        return len1 - len2;
    }

    @IntrinsicCandidate
    public static int compareToLatin1(byte[] value, byte[] other) {
        return -StringLatin1.compareToUTF16(other, value);
    }

    public static int compareToLatin1(byte[] value, byte[] other, int len1, int len2) {
        return -StringLatin1.compareToUTF16(other, value, len2, len1);
    }

    public static int compareToCI(byte[] value, byte[] other) {
        return compareToCIImpl(value, 0, length(value), other, 0, length(other));
    }

    private static int compareToCIImpl(byte[] value, int toffset, int tlen,
                                      byte[] other, int ooffset, int olen) {
        int tlast = toffset + tlen;
        int olast = ooffset + olen;
        assert toffset >= 0 && ooffset >= 0;
        assert tlast <= length(value);
        assert olast <= length(other);

        for (int k1 = toffset, k2 = ooffset; k1 < tlast && k2 < olast; k1++, k2++) {
            int cp1 = (int)getChar(value, k1);
            int cp2 = (int)getChar(other, k2);

            if (cp1 == cp2 || compareCodePointCI(cp1, cp2) == 0) {
                continue;
            }

            // Check for supplementary characters case
            cp1 = codePointIncluding(value, cp1, k1, toffset, tlast);
            if (cp1 < 0) {
                k1++;
                cp1 = -cp1;
            }
            cp2 = codePointIncluding(other, cp2, k2, ooffset, olast);
            if (cp2 < 0) {
                k2++;
                cp2 = -cp2;
            }

            int diff = compareCodePointCI(cp1, cp2);
            if (diff != 0) {
                return diff;
            }
        }
        return tlen - olen;
    }

    // Case insensitive comparison of two code points
    private static int compareCodePointCI(int cp1, int cp2) {
        // try converting both characters to uppercase.
        // If the results match, then the comparison scan should
        // continue.
        cp1 = Character.toUpperCase(cp1);
        cp2 = Character.toUpperCase(cp2);
        if (cp1 != cp2) {
            // Unfortunately, conversion to uppercase does not work properly
            // for the Georgian alphabet, which has strange rules about case
            // conversion.  So we need to make one last check before
            // exiting.
            cp1 = Character.toLowerCase(cp1);
            cp2 = Character.toLowerCase(cp2);
            if (cp1 != cp2) {
                return cp1 - cp2;
            }
        }
        return 0;
    }

    // Returns a code point from the code unit pointed by "index". If it is
    // not a surrogate or an unpaired surrogate, then the code unit is
    // returned as is. Otherwise, it is combined with the code unit before
    // or after, depending on the type of the surrogate at index, to make a
    // supplementary code point. The return value will be negated if the code
    // unit pointed by index is a high surrogate, and index + 1 is a low surrogate.
    private static int codePointIncluding(byte[] ba, int cp, int index, int start, int end) {
        // fast check
        if (!Character.isSurrogate((char)cp)) {
            return cp;
        }
        if (Character.isLowSurrogate((char)cp)) {
            if (index > start) {
                char c = getChar(ba, index - 1);
                if (Character.isHighSurrogate(c)) {
                    return Character.toCodePoint(c, (char)cp);
                }
            }
        } else if (index + 1 < end) { // cp == high surrogate
            char c = getChar(ba, index + 1);
            if (Character.isLowSurrogate(c)) {
                // negate the code point
                return - Character.toCodePoint((char)cp, c);
            }
        }
        return cp;
    }

    public static int compareToCI_Latin1(byte[] value, byte[] other) {
        return -StringLatin1.compareToCI_UTF16(other, value);
    }

    public static int hashCode(byte[] value) {
        int h = 0;
        int length = value.length >> 1;
        for (int i = 0; i < length; i++) {
            h = 31 * h + getChar(value, i);
        }
        return h;
    }

    public static int indexOf(byte[] value, int ch, int fromIndex) {
        int max = value.length >> 1;
        if (fromIndex < 0) {
            fromIndex = 0;
        } else if (fromIndex >= max) {
            // Note: fromIndex might be near -1>>>1.
            return -1;
        }
        if (ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            // handle most cases here (ch is a BMP code point or a
            // negative value (invalid code point))
            return indexOfChar(value, ch, fromIndex, max);
        } else {
            return indexOfSupplementary(value, ch, fromIndex, max);
        }
    }

    @IntrinsicCandidate
    public static int indexOf(byte[] value, byte[] str) {
        if (str.length == 0) {
            return 0;
        }
        if (value.length < str.length) {
            return -1;
        }
        return indexOfUnsafe(value, length(value), str, length(str), 0);
    }

    @IntrinsicCandidate
    public static int indexOf(byte[] value, int valueCount, byte[] str, int strCount, int fromIndex) {
        checkBoundsBeginEnd(fromIndex, valueCount, value);
        checkBoundsBeginEnd(0, strCount, str);
        return indexOfUnsafe(value, valueCount, str, strCount, fromIndex);
    }


    private static int indexOfUnsafe(byte[] value, int valueCount, byte[] str, int strCount, int fromIndex) {
        assert fromIndex >= 0;
        assert strCount > 0;
        assert strCount <= length(str);
        assert valueCount >= strCount;
        char first = getChar(str, 0);
        int max = (valueCount - strCount);
        for (int i = fromIndex; i <= max; i++) {
            // Look for first character.
            if (getChar(value, i) != first) {
                while (++i <= max && getChar(value, i) != first);
            }
            // Found first character, now look at the rest of value
            if (i <= max) {
                int j = i + 1;
                int end = j + strCount - 1;
                for (int k = 1; j < end && getChar(value, j) == getChar(str, k); j++, k++);
                if (j == end) {
                    // Found whole string.
                    return i;
                }
            }
        }
        return -1;
    }


    /**
     * Handles indexOf Latin1 substring in UTF16 string.
     */
    @IntrinsicCandidate
    public static int indexOfLatin1(byte[] value, byte[] str) {
        if (str.length == 0) {
            return 0;
        }
        if (length(value) < str.length) {
            return -1;
        }
        return indexOfLatin1Unsafe(value, length(value), str, str.length, 0);
    }

    @IntrinsicCandidate
    public static int indexOfLatin1(byte[] src, int srcCount, byte[] tgt, int tgtCount, int fromIndex) {
        checkBoundsBeginEnd(fromIndex, srcCount, src);
        String.checkBoundsBeginEnd(0, tgtCount, tgt.length);
        return indexOfLatin1Unsafe(src, srcCount, tgt, tgtCount, fromIndex);
    }

    public static int indexOfLatin1Unsafe(byte[] src, int srcCount, byte[] tgt, int tgtCount, int fromIndex) {
        assert fromIndex >= 0;
        assert tgtCount > 0;
        assert tgtCount <= tgt.length;
        assert srcCount >= tgtCount;
        char first = (char)(tgt[0] & 0xff);
        int max = (srcCount - tgtCount);
        for (int i = fromIndex; i <= max; i++) {
            // Look for first character.
            if (getChar(src, i) != first) {
                while (++i <= max && getChar(src, i) != first);
            }
            // Found first character, now look at the rest of v2
            if (i <= max) {
                int j = i + 1;
                int end = j + tgtCount - 1;
                for (int k = 1;
                     j < end && getChar(src, j) == (tgt[k] & 0xff);
                     j++, k++);
                if (j == end) {
                    // Found whole string.
                    return i;
                }
            }
        }
        return -1;
    }

    @IntrinsicCandidate
    private static int indexOfChar(byte[] value, int ch, int fromIndex, int max) {
        checkBoundsBeginEnd(fromIndex, max, value);
        return indexOfCharUnsafe(value, ch, fromIndex, max);
    }

    private static int indexOfCharUnsafe(byte[] value, int ch, int fromIndex, int max) {
        for (int i = fromIndex; i < max; i++) {
            if (getChar(value, i) == ch) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Handles (rare) calls of indexOf with a supplementary character.
     */
    private static int indexOfSupplementary(byte[] value, int ch, int fromIndex, int max) {
        if (Character.isValidCodePoint(ch)) {
            final char hi = Character.highSurrogate(ch);
            final char lo = Character.lowSurrogate(ch);
            checkBoundsBeginEnd(fromIndex, max, value);
            for (int i = fromIndex; i < max - 1; i++) {
                if (getChar(value, i) == hi && getChar(value, i + 1) == lo) {
                    return i;
                }
            }
        }
        return -1;
    }

    // srcCoder == UTF16 && tgtCoder == UTF16
    public static int lastIndexOf(byte[] src, int srcCount,
                                  byte[] tgt, int tgtCount, int fromIndex) {
        assert fromIndex >= 0;
        assert tgtCount > 0;
        assert tgtCount <= length(tgt);
        int min = tgtCount - 1;
        int i = min + fromIndex;
        int strLastIndex = tgtCount - 1;

        checkIndex(strLastIndex, tgt);
        char strLastChar = getChar(tgt, strLastIndex);

        checkIndex(i, src);

    startSearchForLastChar:
        while (true) {
            while (i >= min && getChar(src, i) != strLastChar) {
                i--;
            }
            if (i < min) {
                return -1;
            }
            int j = i - 1;
            int start = j - strLastIndex;
            int k = strLastIndex - 1;
            while (j > start) {
                if (getChar(src, j--) != getChar(tgt, k--)) {
                    i--;
                    continue startSearchForLastChar;
                }
            }
            return start + 1;
        }
    }

    public static int lastIndexOf(byte[] value, int ch, int fromIndex) {
        if (ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            // handle most cases here (ch is a BMP code point or a
            // negative value (invalid code point))
            int i = Math.min(fromIndex, (value.length >> 1) - 1);
            for (; i >= 0; i--) {
                if (getChar(value, i) == ch) {
                    return i;
                }
            }
            return -1;
        } else {
            return lastIndexOfSupplementary(value, ch, fromIndex);
        }
    }

    /**
     * Handles (rare) calls of lastIndexOf with a supplementary character.
     */
    private static int lastIndexOfSupplementary(final byte[] value, int ch, int fromIndex) {
        if (Character.isValidCodePoint(ch)) {
            char hi = Character.highSurrogate(ch);
            char lo = Character.lowSurrogate(ch);
            int i = Math.min(fromIndex, (value.length >> 1) - 2);
            for (; i >= 0; i--) {
                if (getChar(value, i) == hi && getChar(value, i + 1) == lo) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static String replace(byte[] value, char oldChar, char newChar) {
        int len = value.length >> 1;
        int i = -1;
        while (++i < len) {
            if (getChar(value, i) == oldChar) {
                break;
            }
        }
        if (i < len) {
            byte[] buf = new byte[value.length];
            for (int j = 0; j < i; j++) {
                putChar(buf, j, getChar(value, j)); // TBD:arraycopy?
            }
            while (i < len) {
                char c = getChar(value, i);
                putChar(buf, i, c == oldChar ? newChar : c);
                i++;
            }
            // Check if we should try to compress to latin1
            if (String.COMPACT_STRINGS &&
                !StringLatin1.canEncode(oldChar) &&
                StringLatin1.canEncode(newChar)) {
                byte[] val = compress(buf, 0, len);
                if (val != null) {
                    return new String(val, LATIN1);
                }
            }
            return new String(buf, UTF16);
        }
        return null;
    }

    public static String replace(byte[] value, int valLen, boolean valLat1,
                                 byte[] targ, int targLen, boolean targLat1,
                                 byte[] repl, int replLen, boolean replLat1)
    {
        assert targLen > 0;
        assert !valLat1 || !targLat1 || !replLat1;

        //  Possible combinations of the arguments/result encodings:
        //  +---+--------+--------+--------+-----------------------+
        //  | # | VALUE  | TARGET | REPL   | RESULT                |
        //  +===+========+========+========+=======================+
        //  | 1 | Latin1 | Latin1 |  UTF16 | null or UTF16         |
        //  +---+--------+--------+--------+-----------------------+
        //  | 2 | Latin1 |  UTF16 | Latin1 | null                  |
        //  +---+--------+--------+--------+-----------------------+
        //  | 3 | Latin1 |  UTF16 |  UTF16 | null                  |
        //  +---+--------+--------+--------+-----------------------+
        //  | 4 |  UTF16 | Latin1 | Latin1 | null or UTF16         |
        //  +---+--------+--------+--------+-----------------------+
        //  | 5 |  UTF16 | Latin1 |  UTF16 | null or UTF16         |
        //  +---+--------+--------+--------+-----------------------+
        //  | 6 |  UTF16 |  UTF16 | Latin1 | null, Latin1 or UTF16 |
        //  +---+--------+--------+--------+-----------------------+
        //  | 7 |  UTF16 |  UTF16 |  UTF16 | null or UTF16         |
        //  +---+--------+--------+--------+-----------------------+

        if (String.COMPACT_STRINGS && valLat1 && !targLat1) {
            // combinations 2 or 3
            return null; // for string to return this;
        }

        int i = (String.COMPACT_STRINGS && valLat1)
                        ? StringLatin1.indexOf(value, targ) :
                (String.COMPACT_STRINGS && targLat1)
                        ? indexOfLatin1(value, targ)
                        : indexOf(value, targ);
        if (i < 0) {
            return null; // for string to return this;
        }

        // find and store indices of substrings to replace
        int j, p = 0;
        int[] pos = new int[16];
        pos[0] = i;
        i += targLen;
        while ((j = ((String.COMPACT_STRINGS && valLat1)
                            ? StringLatin1.indexOf(value, valLen, targ, targLen, i) :
                     (String.COMPACT_STRINGS && targLat1)
                            ? indexOfLatin1(value, valLen, targ, targLen, i)
                            : indexOf(value, valLen, targ, targLen, i))) > 0)
        {
            if (++p == pos.length) {
                pos = Arrays.copyOf(pos, ArraysSupport.newLength(p, 1, p >> 1));
            }
            pos[p] = j;
            i = j + targLen;
        }

        int resultLen;
        try {
            resultLen = Math.addExact(valLen,
                    Math.multiplyExact(++p, replLen - targLen));
        } catch (ArithmeticException ignored) {
           throw new OutOfMemoryError("Required length exceeds implementation limit");
        }
        if (resultLen == 0) {
            return "";
        }

        byte[] result = newBytesFor(resultLen);
        int posFrom = 0, posTo = 0;
        for (int q = 0; q < p; ++q) {
            int nextPos = pos[q];
            if (String.COMPACT_STRINGS && valLat1) {
                while (posFrom < nextPos) {
                    char c = (char)(value[posFrom++] & 0xff);
                    putChar(result, posTo++, c);
                }
            } else {
                while (posFrom < nextPos) {
                    putChar(result, posTo++, getChar(value, posFrom++));
                }
            }
            posFrom += targLen;
            if (String.COMPACT_STRINGS && replLat1) {
                for (int k = 0; k < replLen; ++k) {
                    char c = (char)(repl[k] & 0xff);
                    putChar(result, posTo++, c);
                }
            } else {
                for (int k = 0; k < replLen; ++k) {
                    putChar(result, posTo++, getChar(repl, k));
                }
            }
        }
        if (String.COMPACT_STRINGS && valLat1) {
            while (posFrom < valLen) {
                char c = (char)(value[posFrom++] & 0xff);
                putChar(result, posTo++, c);
            }
        } else {
            while (posFrom < valLen) {
                putChar(result, posTo++, getChar(value, posFrom++));
            }
        }

        if (String.COMPACT_STRINGS && replLat1 && !targLat1) {
            // combination 6
            byte[] lat1Result = compress(result, 0, resultLen);
            if (lat1Result != null) {
                return new String(lat1Result, LATIN1);
            }
        }
        return new String(result, UTF16);
    }

    public static boolean regionMatchesCI(byte[] value, int toffset,
                                          byte[] other, int ooffset, int len) {
        return compareToCIImpl(value, toffset, len, other, ooffset, len) == 0;
    }

    public static boolean regionMatchesCI_Latin1(byte[] value, int toffset,
                                                 byte[] other, int ooffset,
                                                 int len) {
        return StringLatin1.regionMatchesCI_UTF16(other, ooffset, value, toffset, len);
    }

    public static String toLowerCase(String str, byte[] value, Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        int first;
        boolean hasSurr = false;
        final int len = value.length >> 1;

        // Now check if there are any characters that need to be changed, or are surrogate
        for (first = 0 ; first < len; first++) {
            int cp = (int)getChar(value, first);
            if (Character.isSurrogate((char)cp)) {
                hasSurr = true;
                break;
            }
            if (cp != Character.toLowerCase(cp)) {  // no need to check Character.ERROR
                break;
            }
        }
        if (first == len)
            return str;
        byte[] result = new byte[value.length];
        System.arraycopy(value, 0, result, 0, first << 1);  // Just copy the first few
                                                            // lowerCase characters.
        String lang = locale.getLanguage();
        if (lang == "tr" || lang == "az" || lang == "lt") {
            return toLowerCaseEx(str, value, result, first, locale, true);
        }
        if (hasSurr) {
            return toLowerCaseEx(str, value, result, first, locale, false);
        }
        int bits = 0;
        for (int i = first; i < len; i++) {
            int cp = (int)getChar(value, i);
            if (cp == '\u03A3' ||                       // GREEK CAPITAL LETTER SIGMA
                Character.isSurrogate((char)cp)) {
                return toLowerCaseEx(str, value, result, i, locale, false);
            }
            if (cp == '\u0130') {                       // LATIN CAPITAL LETTER I WITH DOT ABOVE
                return toLowerCaseEx(str, value, result, i, locale, true);
            }
            cp = Character.toLowerCase(cp);
            if (!Character.isBmpCodePoint(cp)) {
                return toLowerCaseEx(str, value, result, i, locale, false);
            }
            bits |= cp;
            putChar(result, i, cp);
        }
        if (bits > 0xFF) {
            return new String(result, UTF16);
        } else {
            return newString(result, 0, len);
        }
    }

    private static String toLowerCaseEx(String str, byte[] value,
                                        byte[] result, int first, Locale locale,
                                        boolean localeDependent) {
        assert(result.length == value.length);
        assert(first >= 0);
        int resultOffset = first;
        int length = value.length >> 1;
        int srcCount;
        for (int i = first; i < length; i += srcCount) {
            int srcChar = getChar(value, i);
            int lowerChar;
            char[] lowerCharArray;
            srcCount = 1;
            if (Character.isSurrogate((char)srcChar)) {
                srcChar = codePointAt(value, i, length);
                srcCount = Character.charCount(srcChar);
            }
            if (localeDependent ||
                srcChar == '\u03A3' ||  // GREEK CAPITAL LETTER SIGMA
                srcChar == '\u0130') {  // LATIN CAPITAL LETTER I WITH DOT ABOVE
                lowerChar = ConditionalSpecialCasing.toLowerCaseEx(str, i, locale);
            } else {
                lowerChar = Character.toLowerCase(srcChar);
            }
            if (Character.isBmpCodePoint(lowerChar)) {    // Character.ERROR is not a bmp
                putChar(result, resultOffset++, lowerChar);
            } else {
                if (lowerChar == Character.ERROR) {
                    lowerCharArray = ConditionalSpecialCasing.toLowerCaseCharArray(str, i, locale);
                } else {
                    lowerCharArray = Character.toChars(lowerChar);
                }
                /* Grow result if needed */
                int mapLen = lowerCharArray.length;
                if (mapLen > srcCount) {
                    byte[] result2 = newBytesFor((result.length >> 1) + mapLen - srcCount);
                    System.arraycopy(result, 0, result2, 0, resultOffset << 1);
                    result = result2;
                }
                assert resultOffset >= 0;
                assert resultOffset + mapLen <= length(result);
                for (int x = 0; x < mapLen; ++x) {
                    putChar(result, resultOffset++, lowerCharArray[x]);
                }
            }
        }
        return newString(result, 0, resultOffset);
    }

    public static String toUpperCase(String str, byte[] value, Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        int first;
        boolean hasSurr = false;
        final int len = value.length >> 1;

        // Now check if there are any characters that need to be changed, or are surrogate
        for (first = 0 ; first < len; first++) {
            int cp = (int)getChar(value, first);
            if (Character.isSurrogate((char)cp)) {
                hasSurr = true;
                break;
            }
            if (cp != Character.toUpperCaseEx(cp)) {   // no need to check Character.ERROR
                break;
            }
        }
        if (first == len) {
            return str;
        }
        byte[] result = new byte[value.length];
        System.arraycopy(value, 0, result, 0, first << 1); // Just copy the first few
                                                           // upperCase characters.
        String lang = locale.getLanguage();
        if (lang == "tr" || lang == "az" || lang == "lt") {
            return toUpperCaseEx(str, value, result, first, locale, true);
        }
        if (hasSurr) {
            return toUpperCaseEx(str, value, result, first, locale, false);
        }
        int bits = 0;
        for (int i = first; i < len; i++) {
            int cp = (int)getChar(value, i);
            if (Character.isSurrogate((char)cp)) {
                return toUpperCaseEx(str, value, result, i, locale, false);
            }
            cp = Character.toUpperCaseEx(cp);
            if (!Character.isBmpCodePoint(cp)) {    // Character.ERROR is not bmp
                return toUpperCaseEx(str, value, result, i, locale, false);
            }
            bits |= cp;
            putChar(result, i, cp);
        }
        if (bits > 0xFF) {
            return new String(result, UTF16);
        } else {
            return newString(result, 0, len);
        }
    }

    private static String toUpperCaseEx(String str, byte[] value,
                                        byte[] result, int first,
                                        Locale locale, boolean localeDependent)
    {
        assert(result.length == value.length);
        assert(first >= 0);
        int resultOffset = first;
        int length = value.length >> 1;
        int srcCount;
        for (int i = first; i < length; i += srcCount) {
            int srcChar = getChar(value, i);
            int upperChar;
            char[] upperCharArray;
            srcCount = 1;
            if (Character.isSurrogate((char)srcChar)) {
                srcChar = codePointAt(value, i, length);
                srcCount = Character.charCount(srcChar);
            }
            if (localeDependent) {
                upperChar = ConditionalSpecialCasing.toUpperCaseEx(str, i, locale);
            } else {
                upperChar = Character.toUpperCaseEx(srcChar);
            }
            if (Character.isBmpCodePoint(upperChar)) {
                putChar(result, resultOffset++, upperChar);
            } else {
                if (upperChar == Character.ERROR) {
                    if (localeDependent) {
                        upperCharArray =
                            ConditionalSpecialCasing.toUpperCaseCharArray(str, i, locale);
                    } else {
                        upperCharArray = Character.toUpperCaseCharArray(srcChar);
                    }
                } else {
                    upperCharArray = Character.toChars(upperChar);
                }
                /* Grow result if needed */
                int mapLen = upperCharArray.length;
                if (mapLen > srcCount) {
                    byte[] result2 = newBytesFor((result.length >> 1) + mapLen - srcCount);
                    System.arraycopy(result, 0, result2, 0, resultOffset << 1);
                    result = result2;
                }
                assert resultOffset >= 0;
                assert resultOffset + mapLen <= length(result);
                for (int x = 0; x < mapLen; ++x) {
                    putChar(result, resultOffset++, upperCharArray[x]);
                }
            }
        }
        return newString(result, 0, resultOffset);
    }

    public static String trim(byte[] value) {
        int length = value.length >> 1;
        int len = length;
        int st = 0;
        while (st < len && getChar(value, st) <= ' ') {
            st++;
        }
        while (st < len && getChar(value, len - 1) <= ' ') {
            len--;
        }
        return ((st > 0) || (len < length )) ?
            new String(Arrays.copyOfRange(value, st << 1, len << 1), UTF16) :
            null;
    }

    public static int indexOfNonWhitespace(byte[] value) {
        int length = value.length >> 1;
        int left = 0;
        while (left < length) {
            int codepoint = codePointAt(value, left, length);
            if (codepoint != ' ' && codepoint != '\t' && !Character.isWhitespace(codepoint)) {
                break;
            }
            left += Character.charCount(codepoint);
        }
        return left;
    }

    public static int lastIndexOfNonWhitespace(byte[] value) {
        int length = value.length >>> 1;
        int right = length;
        while (0 < right) {
            int codepoint = codePointBefore(value, right);
            if (codepoint != ' ' && codepoint != '\t' && !Character.isWhitespace(codepoint)) {
                break;
            }
            right -= Character.charCount(codepoint);
        }
        return right;
    }

    public static String strip(byte[] value) {
        int length = value.length >>> 1;
        int left = indexOfNonWhitespace(value);
        if (left == length) {
            return "";
        }
        int right = lastIndexOfNonWhitespace(value);
        boolean ifChanged = (left > 0) || (right < length);
        return ifChanged ? newString(value, left, right - left) : null;
    }

    public static String stripLeading(byte[] value) {
        int length = value.length >>> 1;
        int left = indexOfNonWhitespace(value);
        return (left != 0) ? newString(value, left, length - left) : null;
    }

    public static String stripTrailing(byte[] value) {
        int length = value.length >>> 1;
        int right = lastIndexOfNonWhitespace(value);
        return (right != length) ? newString(value, 0, right) : null;
    }

    private static final class LinesSpliterator implements Spliterator<String> {
        private byte[] value;
        private int index;        // current index, modified on advance/split
        private final int fence;  // one past last index

        private LinesSpliterator(byte[] value, int start, int length) {
            this.value = value;
            this.index = start;
            this.fence = start + length;
        }

        private int indexOfLineSeparator(int start) {
            for (int current = start; current < fence; current++) {
                char ch = getChar(value, current);
                if (ch == '\n' || ch == '\r') {
                    return current;
                }
            }
            return fence;
        }

        private int skipLineSeparator(int start) {
            if (start < fence) {
                if (getChar(value, start) == '\r') {
                    int next = start + 1;
                    if (next < fence && getChar(value, next) == '\n') {
                        return next + 1;
                    }
                }
                return start + 1;
            }
            return fence;
        }

        private String next() {
            int start = index;
            int end = indexOfLineSeparator(start);
            index = skipLineSeparator(end);
            return newString(value, start, end - start);
        }

        @Override
        public boolean tryAdvance(Consumer<? super String> action) {
            if (action == null) {
                throw new NullPointerException("tryAdvance action missing");
            }
            if (index != fence) {
                action.accept(next());
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(Consumer<? super String> action) {
            if (action == null) {
                throw new NullPointerException("forEachRemaining action missing");
            }
            while (index != fence) {
                action.accept(next());
            }
        }

        @Override
        public Spliterator<String> trySplit() {
            int half = (fence + index) >>> 1;
            int mid = skipLineSeparator(indexOfLineSeparator(half));
            if (mid < fence) {
                int start = index;
                index = mid;
                return new LinesSpliterator(value, start, mid - start);
            }
            return null;
        }

        @Override
        public long estimateSize() {
            return fence - index + 1;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL;
        }

        static LinesSpliterator spliterator(byte[] value) {
            return new LinesSpliterator(value, 0, value.length >>> 1);
        }
    }

    static Stream<String> lines(byte[] value) {
        return StreamSupport.stream(LinesSpliterator.spliterator(value), false);
    }

    private static void putChars(byte[] val, int index, char[] str, int off, int end) {
        while (off < end) {
            putChar(val, index++, str[off++]);
        }
    }

    public static String newString(byte[] val, int index, int len) {
        if (len == 0) {
            return "";
        }
        if (String.COMPACT_STRINGS) {
            byte[] buf = compress(val, index, len);
            if (buf != null) {
                return new String(buf, LATIN1);
            }
        }
        int last = index + len;
        return new String(Arrays.copyOfRange(val, index << 1, last << 1), UTF16);
    }

    public static void fillNull(byte[] val, int index, int end) {
        Arrays.fill(val, index << 1, end << 1, (byte)0);
    }

    static class CharsSpliterator implements Spliterator.OfInt {
        private final byte[] array;
        private int index;        // current index, modified on advance/split
        private final int fence;  // one past last index
        private final int cs;

        CharsSpliterator(byte[] array, int acs) {
            this(array, 0, array.length >> 1, acs);
        }

        CharsSpliterator(byte[] array, int origin, int fence, int acs) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.cs = acs | Spliterator.ORDERED | Spliterator.SIZED
                      | Spliterator.SUBSIZED;
        }

        @Override
        public OfInt trySplit() {
            int lo = index, mid = (lo + fence) >>> 1;
            return (lo >= mid)
                   ? null
                   : new CharsSpliterator(array, lo, index = mid, cs);
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            byte[] a; int i, hi; // hoist accesses and checks from loop
            if (action == null)
                throw new NullPointerException();
            if (((a = array).length >> 1) >= (hi = fence) &&
                (i = index) >= 0 && i < (index = hi)) {
                do {
                    action.accept(charAt(a, i));
                } while (++i < hi);
            }
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (action == null)
                throw new NullPointerException();
            int i = index;
            if (i >= 0 && i < fence) {
                action.accept(charAt(array, i));
                index++;
                return true;
            }
            return false;
        }

        @Override
        public long estimateSize() { return (long)(fence - index); }

        @Override
        public int characteristics() {
            return cs;
        }
    }

    static class CodePointsSpliterator implements Spliterator.OfInt {
        private final byte[] array;
        private int index;        // current index, modified on advance/split
        private final int fence;  // one past last index
        private final int cs;

        CodePointsSpliterator(byte[] array, int acs) {
            this(array, 0, array.length >> 1, acs);
        }

        CodePointsSpliterator(byte[] array, int origin, int fence, int acs) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.cs = acs | Spliterator.ORDERED;
        }

        @Override
        public OfInt trySplit() {
            int lo = index, mid = (lo + fence) >>> 1;
            if (lo >= mid)
                return null;

            int midOneLess;
            // If the mid-point intersects a surrogate pair
            if (Character.isLowSurrogate(charAt(array, mid)) &&
                Character.isHighSurrogate(charAt(array, midOneLess = (mid -1)))) {
                // If there is only one pair it cannot be split
                if (lo >= midOneLess)
                    return null;
                // Shift the mid-point to align with the surrogate pair
                return new CodePointsSpliterator(array, lo, index = midOneLess, cs);
            }
            return new CodePointsSpliterator(array, lo, index = mid, cs);
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            byte[] a; int i, hi; // hoist accesses and checks from loop
            if (action == null)
                throw new NullPointerException();
            if (((a = array).length >> 1) >= (hi = fence) &&
                (i = index) >= 0 && i < (index = hi)) {
                do {
                    i = advance(a, i, hi, action);
                } while (i < hi);
            }
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (action == null)
                throw new NullPointerException();
            if (index >= 0 && index < fence) {
                index = advance(array, index, fence, action);
                return true;
            }
            return false;
        }

        // Advance one code point from the index, i, and return the next
        // index to advance from
        private static int advance(byte[] a, int i, int hi, IntConsumer action) {
            char c1 = charAt(a, i++);
            int cp = c1;
            if (Character.isHighSurrogate(c1) && i < hi) {
                char c2 = charAt(a, i);
                if (Character.isLowSurrogate(c2)) {
                    i++;
                    cp = Character.toCodePoint(c1, c2);
                }
            }
            action.accept(cp);
            return i;
        }

        @Override
        public long estimateSize() { return (long)(fence - index); }

        @Override
        public int characteristics() {
            return cs;
        }
    }

    ////////////////////////////////////////////////////////////////

    public static void putCharSB(byte[] val, int index, int c) {
        checkIndex(index, val);
        putChar(val, index, c);
    }

    public static void putCharsSB(byte[] val, int index, char[] ca, int off, int end) {
        checkBoundsBeginEnd(index, index + end - off, val);
        putChars(val, index, ca, off, end);
    }

    public static void putCharsSB(byte[] val, int index, CharSequence s, int off, int end) {
        checkBoundsBeginEnd(index, index + end - off, val);
        for (int i = off; i < end; i++) {
            putChar(val, index++, s.charAt(i));
        }
    }

    public static int codePointAtSB(byte[] val, int index, int end) {
        return codePointAt(val, index, end, true /* checked */);
    }

    public static int codePointBeforeSB(byte[] val, int index) {
        return codePointBefore(val, index, true /* checked */);
    }

    public static int codePointCountSB(byte[] val, int beginIndex, int endIndex) {
        return codePointCount(val, beginIndex, endIndex, true /* checked */);
    }

    public static int getChars(int i, int begin, int end, byte[] value) {
        checkBoundsBeginEnd(begin, end, value);
        int pos = getChars(i, end, value);
        assert begin == pos;
        return pos;
    }

    public static int getChars(long l, int begin, int end, byte[] value) {
        checkBoundsBeginEnd(begin, end, value);
        int pos = getChars(l, end, value);
        assert begin == pos;
        return pos;
    }

    public static boolean contentEquals(byte[] v1, byte[] v2, int len) {
        checkBoundsOffCount(0, len, v2);
        for (int i = 0; i < len; i++) {
            if ((char)(v1[i] & 0xff) != getChar(v2, i)) {
                return false;
            }
        }
        return true;
    }

    public static boolean contentEquals(byte[] value, CharSequence cs, int len) {
        checkOffset(len, value);
        for (int i = 0; i < len; i++) {
            if (getChar(value, i) != cs.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static int putCharsAt(byte[] value, int i, char c1, char c2, char c3, char c4) {
        int end = i + 4;
        checkBoundsBeginEnd(i, end, value);
        putChar(value, i++, c1);
        putChar(value, i++, c2);
        putChar(value, i++, c3);
        putChar(value, i++, c4);
        assert(i == end);
        return end;
    }

    public static int putCharsAt(byte[] value, int i, char c1, char c2, char c3, char c4, char c5) {
        int end = i + 5;
        checkBoundsBeginEnd(i, end, value);
        putChar(value, i++, c1);
        putChar(value, i++, c2);
        putChar(value, i++, c3);
        putChar(value, i++, c4);
        putChar(value, i++, c5);
        assert(i == end);
        return end;
    }

    public static char charAt(byte[] value, int index) {
        checkIndex(index, value);
        return getChar(value, index);
    }

    public static void reverse(byte[] val, int count) {
        checkOffset(count, val);
        int n = count - 1;
        boolean hasSurrogates = false;
        for (int j = (n-1) >> 1; j >= 0; j--) {
            int k = n - j;
            char cj = getChar(val, j);
            char ck = getChar(val, k);
            putChar(val, j, ck);
            putChar(val, k, cj);
            if (Character.isSurrogate(cj) ||
                Character.isSurrogate(ck)) {
                hasSurrogates = true;
            }
        }
        if (hasSurrogates) {
            reverseAllValidSurrogatePairs(val, count);
        }
    }

    /** Outlined helper method for reverse() */
    private static void reverseAllValidSurrogatePairs(byte[] val, int count) {
        for (int i = 0; i < count - 1; i++) {
            char c2 = getChar(val, i);
            if (Character.isLowSurrogate(c2)) {
                char c1 = getChar(val, i + 1);
                if (Character.isHighSurrogate(c1)) {
                    putChar(val, i++, c1);
                    putChar(val, i, c2);
                }
            }
        }
    }

    // inflatedCopy byte[] -> byte[]
    public static void inflate(byte[] src, int srcOff, byte[] dst, int dstOff, int len) {
        // We need a range check here because 'putChar' has no checks
        checkBoundsOffCount(dstOff, len, dst);
        for (int i = 0; i < len; i++) {
            putChar(dst, dstOff++, src[srcOff++] & 0xff);
        }
    }

    // srcCoder == UTF16 && tgtCoder == LATIN1
    public static int lastIndexOfLatin1(byte[] src, int srcCount,
                                        byte[] tgt, int tgtCount, int fromIndex) {
        assert fromIndex >= 0;
        assert tgtCount > 0;
        assert tgtCount <= tgt.length;
        int min = tgtCount - 1;
        int i = min + fromIndex;
        int strLastIndex = tgtCount - 1;

        char strLastChar = (char)(tgt[strLastIndex] & 0xff);

        checkIndex(i, src);

    startSearchForLastChar:
        while (true) {
            while (i >= min && getChar(src, i) != strLastChar) {
                i--;
            }
            if (i < min) {
                return -1;
            }
            int j = i - 1;
            int start = j - strLastIndex;
            int k = strLastIndex - 1;
            while (j > start) {
                if (getChar(src, j--) != (tgt[k--] & 0xff)) {
                    i--;
                    continue startSearchForLastChar;
                }
            }
            return start + 1;
        }
    }

    ////////////////////////////////////////////////////////////////

    private static native boolean isBigEndian();

    static final int HI_BYTE_SHIFT;
    static final int LO_BYTE_SHIFT;
    static {
        if (isBigEndian()) {
            HI_BYTE_SHIFT = 8;
            LO_BYTE_SHIFT = 0;
        } else {
            HI_BYTE_SHIFT = 0;
            LO_BYTE_SHIFT = 8;
        }
    }

    static final int MAX_LENGTH = Integer.MAX_VALUE >> 1;

    // Used by trusted callers.  Assumes all necessary bounds checks have
    // been done by the caller.

    /**
     * This is a variant of {@link Integer#getChars(int, int, byte[])}, but for
     * UTF-16 coder.
     *
     * @param i     value to convert
     * @param index next index, after the least significant digit
     * @param buf   target buffer, UTF16-coded.
     * @return index of the most significant digit or minus sign, if present
     */
    static int getChars(int i, int index, byte[] buf) {
        int q, r;
        int charPos = index;

        boolean negative = (i < 0);
        if (!negative) {
            i = -i;
        }

        // Get 2 digits/iteration using ints
        while (i <= -100) {
            q = i / 100;
            r = (q * 100) - i;
            i = q;
            putChar(buf, --charPos, Integer.DigitOnes[r]);
            putChar(buf, --charPos, Integer.DigitTens[r]);
        }

        // We know there are at most two digits left at this point.
        q = i / 10;
        r = (q * 10) - i;
        putChar(buf, --charPos, '0' + r);

        // Whatever left is the remaining digit.
        if (q < 0) {
            putChar(buf, --charPos, '0' - q);
        }

        if (negative) {
            putChar(buf, --charPos, '-');
        }
        return charPos;
    }

    /**
     * This is a variant of {@link Long#getChars(long, int, byte[])}, but for
     * UTF-16 coder.
     *
     * @param i     value to convert
     * @param index next index, after the least significant digit
     * @param buf   target buffer, UTF16-coded.
     * @return index of the most significant digit or minus sign, if present
     */
    static int getChars(long i, int index, byte[] buf) {
        long q;
        int r;
        int charPos = index;

        boolean negative = (i < 0);
        if (!negative) {
            i = -i;
        }

        // Get 2 digits/iteration using longs until quotient fits into an int
        while (i <= Integer.MIN_VALUE) {
            q = i / 100;
            r = (int)((q * 100) - i);
            i = q;
            putChar(buf, --charPos, Integer.DigitOnes[r]);
            putChar(buf, --charPos, Integer.DigitTens[r]);
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int)i;
        while (i2 <= -100) {
            q2 = i2 / 100;
            r  = (q2 * 100) - i2;
            i2 = q2;
            putChar(buf, --charPos, Integer.DigitOnes[r]);
            putChar(buf, --charPos, Integer.DigitTens[r]);
        }

        // We know there are at most two digits left at this point.
        q2 = i2 / 10;
        r  = (q2 * 10) - i2;
        putChar(buf, --charPos, '0' + r);

        // Whatever left is the remaining digit.
        if (q2 < 0) {
            putChar(buf, --charPos, '0' - q2);
        }

        if (negative) {
            putChar(buf, --charPos, '-');
        }
        return charPos;
    }
    // End of trusted methods.

    public static void checkIndex(int off, byte[] val) {
        String.checkIndex(off, length(val));
    }

    public static void checkOffset(int off, byte[] val) {
        String.checkOffset(off, length(val));
    }

    public static void checkBoundsBeginEnd(int begin, int end, byte[] val) {
        String.checkBoundsBeginEnd(begin, end, length(val));
    }

    public static void checkBoundsOffCount(int offset, int count, byte[] val) {
        String.checkBoundsOffCount(offset, count, length(val));
    }

}
