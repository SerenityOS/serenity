/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
import jdk.internal.vm.annotation.IntrinsicCandidate;

import static java.lang.String.LATIN1;
import static java.lang.String.UTF16;
import static java.lang.String.checkIndex;
import static java.lang.String.checkOffset;

final class StringLatin1 {

    public static char charAt(byte[] value, int index) {
        checkIndex(index, value.length);
        return (char)(value[index] & 0xff);
    }

    public static boolean canEncode(int cp) {
        return cp >>> 8 == 0;
    }

    public static int length(byte[] value) {
        return value.length;
    }

    public static int codePointAt(byte[] value, int index, int end) {
        return value[index] & 0xff;
    }

    public static int codePointBefore(byte[] value, int index) {
        return value[index - 1] & 0xff;
    }

    public static int codePointCount(byte[] value, int beginIndex, int endIndex) {
        return endIndex - beginIndex;
    }

    public static char[] toChars(byte[] value) {
        char[] dst = new char[value.length];
        inflate(value, 0, dst, 0, value.length);
        return dst;
    }

    public static byte[] inflate(byte[] value, int off, int len) {
        byte[] ret = StringUTF16.newBytesFor(len);
        inflate(value, off, ret, 0, len);
        return ret;
    }

    public static void getChars(byte[] value, int srcBegin, int srcEnd, char dst[], int dstBegin) {
        inflate(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    public static void getBytes(byte[] value, int srcBegin, int srcEnd, byte dst[], int dstBegin) {
        System.arraycopy(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    @IntrinsicCandidate
    public static boolean equals(byte[] value, byte[] other) {
        if (value.length == other.length) {
            for (int i = 0; i < value.length; i++) {
                if (value[i] != other[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @IntrinsicCandidate
    public static int compareTo(byte[] value, byte[] other) {
        int len1 = value.length;
        int len2 = other.length;
        return compareTo(value, other, len1, len2);
    }

    public static int compareTo(byte[] value, byte[] other, int len1, int len2) {
        int lim = Math.min(len1, len2);
        for (int k = 0; k < lim; k++) {
            if (value[k] != other[k]) {
                return getChar(value, k) - getChar(other, k);
            }
        }
        return len1 - len2;
    }

    @IntrinsicCandidate
    public static int compareToUTF16(byte[] value, byte[] other) {
        int len1 = length(value);
        int len2 = StringUTF16.length(other);
        return compareToUTF16Values(value, other, len1, len2);
    }

    /*
     * Checks the boundary and then compares the byte arrays.
     */
    public static int compareToUTF16(byte[] value, byte[] other, int len1, int len2) {
        checkOffset(len1, length(value));
        checkOffset(len2, StringUTF16.length(other));

        return compareToUTF16Values(value, other, len1, len2);
    }

    private static int compareToUTF16Values(byte[] value, byte[] other, int len1, int len2) {
        int lim = Math.min(len1, len2);
        for (int k = 0; k < lim; k++) {
            char c1 = getChar(value, k);
            char c2 = StringUTF16.getChar(other, k);
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        return len1 - len2;
    }

    public static int compareToCI(byte[] value, byte[] other) {
        int len1 = value.length;
        int len2 = other.length;
        int lim = Math.min(len1, len2);
        for (int k = 0; k < lim; k++) {
            if (value[k] != other[k]) {
                char c1 = (char) CharacterDataLatin1.instance.toUpperCase(getChar(value, k));
                char c2 = (char) CharacterDataLatin1.instance.toUpperCase(getChar(other, k));
                if (c1 != c2) {
                    c1 = Character.toLowerCase(c1);
                    c2 = Character.toLowerCase(c2);
                    if (c1 != c2) {
                        return c1 - c2;
                    }
                }
            }
        }
        return len1 - len2;
    }

    public static int compareToCI_UTF16(byte[] value, byte[] other) {
        int len1 = length(value);
        int len2 = StringUTF16.length(other);
        int lim = Math.min(len1, len2);
        for (int k = 0; k < lim; k++) {
            char c1 = getChar(value, k);
            char c2 = StringUTF16.getChar(other, k);
            if (c1 != c2) {
                c1 = (char) CharacterDataLatin1.instance.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
                if (c1 != c2) {
                    c1 = Character.toLowerCase(c1);
                    c2 = Character.toLowerCase(c2);
                    if (c1 != c2) {
                        return c1 - c2;
                    }
                }
            }
        }
        return len1 - len2;
    }

    public static int hashCode(byte[] value) {
        int h = 0;
        for (byte v : value) {
            h = 31 * h + (v & 0xff);
        }
        return h;
    }

    public static int indexOf(byte[] value, int ch, int fromIndex) {
        if (!canEncode(ch)) {
            return -1;
        }
        int max = value.length;
        if (fromIndex < 0) {
            fromIndex = 0;
        } else if (fromIndex >= max) {
            // Note: fromIndex might be near -1>>>1.
            return -1;
        }
        return indexOfChar(value, ch, fromIndex, max);
    }

    @IntrinsicCandidate
    private static int indexOfChar(byte[] value, int ch, int fromIndex, int max) {
        byte c = (byte)ch;
        for (int i = fromIndex; i < max; i++) {
            if (value[i] == c) {
               return i;
            }
        }
        return -1;
    }

    @IntrinsicCandidate
    public static int indexOf(byte[] value, byte[] str) {
        if (str.length == 0) {
            return 0;
        }
        if (value.length == 0) {
            return -1;
        }
        return indexOf(value, value.length, str, str.length, 0);
    }

    @IntrinsicCandidate
    public static int indexOf(byte[] value, int valueCount, byte[] str, int strCount, int fromIndex) {
        byte first = str[0];
        int max = (valueCount - strCount);
        for (int i = fromIndex; i <= max; i++) {
            // Look for first character.
            if (value[i] != first) {
                while (++i <= max && value[i] != first);
            }
            // Found first character, now look at the rest of value
            if (i <= max) {
                int j = i + 1;
                int end = j + strCount - 1;
                for (int k = 1; j < end && value[j] == str[k]; j++, k++);
                if (j == end) {
                    // Found whole string.
                    return i;
                }
            }
        }
        return -1;
    }

    public static int lastIndexOf(byte[] src, int srcCount,
                                  byte[] tgt, int tgtCount, int fromIndex) {
        int min = tgtCount - 1;
        int i = min + fromIndex;
        int strLastIndex = tgtCount - 1;
        char strLastChar = (char)(tgt[strLastIndex] & 0xff);

  startSearchForLastChar:
        while (true) {
            while (i >= min && (src[i] & 0xff) != strLastChar) {
                i--;
            }
            if (i < min) {
                return -1;
            }
            int j = i - 1;
            int start = j - strLastIndex;
            int k = strLastIndex - 1;
            while (j > start) {
                if ((src[j--] & 0xff) != (tgt[k--] & 0xff)) {
                    i--;
                    continue startSearchForLastChar;
                }
            }
            return start + 1;
        }
    }

    public static int lastIndexOf(final byte[] value, int ch, int fromIndex) {
        if (!canEncode(ch)) {
            return -1;
        }
        int off  = Math.min(fromIndex, value.length - 1);
        for (; off >= 0; off--) {
            if (value[off] == (byte)ch) {
                return off;
            }
        }
        return -1;
    }

    public static String replace(byte[] value, char oldChar, char newChar) {
        if (canEncode(oldChar)) {
            int len = value.length;
            int i = -1;
            while (++i < len) {
                if (value[i] == (byte)oldChar) {
                    break;
                }
            }
            if (i < len) {
                if (canEncode(newChar)) {
                    byte[] buf = StringConcatHelper.newArray(len);
                    for (int j = 0; j < i; j++) {    // TBD arraycopy?
                        buf[j] = value[j];
                    }
                    while (i < len) {
                        byte c = value[i];
                        buf[i] = (c == (byte)oldChar) ? (byte)newChar : c;
                        i++;
                    }
                    return new String(buf, LATIN1);
                } else {
                    byte[] buf = StringUTF16.newBytesFor(len);
                    // inflate from latin1 to UTF16
                    inflate(value, 0, buf, 0, i);
                    while (i < len) {
                        char c = (char)(value[i] & 0xff);
                        StringUTF16.putChar(buf, i, (c == oldChar) ? newChar : c);
                        i++;
                    }
                    return new String(buf, UTF16);
                }
            }
        }
        return null; // for string to return this;
    }

    public static String replace(byte[] value, int valLen, byte[] targ,
                                 int targLen, byte[] repl, int replLen)
    {
        assert targLen > 0;
        int i, j, p = 0;
        if (valLen == 0 || (i = indexOf(value, valLen, targ, targLen, 0)) < 0) {
            return null; // for string to return this;
        }

        // find and store indices of substrings to replace
        int[] pos = new int[16];
        pos[0] = i;
        i += targLen;
        while ((j = indexOf(value, valLen, targ, targLen, i)) > 0) {
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

        byte[] result = StringConcatHelper.newArray(resultLen);
        int posFrom = 0, posTo = 0;
        for (int q = 0; q < p; ++q) {
            int nextPos = pos[q];
            while (posFrom < nextPos) {
                result[posTo++] = value[posFrom++];
            }
            posFrom += targLen;
            for (int k = 0; k < replLen; ++k) {
                result[posTo++] = repl[k];
            }
        }
        while (posFrom < valLen) {
            result[posTo++] = value[posFrom++];
        }
        return new String(result, LATIN1);
    }

    // case insensitive
    public static boolean regionMatchesCI(byte[] value, int toffset,
                                          byte[] other, int ooffset, int len) {
        int last = toffset + len;
        while (toffset < last) {
            char c1 = (char)(value[toffset++] & 0xff);
            char c2 = (char)(other[ooffset++] & 0xff);
            if (c1 == c2) {
                continue;
            }
            int u1 = CharacterDataLatin1.instance.toUpperCase(c1);
            int u2 = CharacterDataLatin1.instance.toUpperCase(c2);
            if (u1 == u2) {
                continue;
            }
            if (Character.toLowerCase(u1) == Character.toLowerCase(u2)) {
                continue;
            }
            return false;
        }
        return true;
    }

    public static boolean regionMatchesCI_UTF16(byte[] value, int toffset,
                                                byte[] other, int ooffset, int len) {
        int last = toffset + len;
        while (toffset < last) {
            char c1 = (char)(value[toffset++] & 0xff);
            char c2 = StringUTF16.getChar(other, ooffset++);
            if (c1 == c2) {
                continue;
            }
            char u1 = (char) CharacterDataLatin1.instance.toUpperCase(c1);
            char u2 = Character.toUpperCase(c2);
            if (u1 == u2) {
                continue;
            }
            if (Character.toLowerCase(u1) == Character.toLowerCase(u2)) {
                continue;
            }
            return false;
        }
        return true;
    }

    public static String toLowerCase(String str, byte[] value, Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        int first;
        final int len = value.length;
        // Now check if there are any characters that need to be changed, or are surrogate
        for (first = 0 ; first < len; first++) {
            int cp = value[first] & 0xff;
            if (cp != CharacterDataLatin1.instance.toLowerCase(cp)) {  // no need to check Character.ERROR
                break;
            }
        }
        if (first == len)
            return str;
        String lang = locale.getLanguage();
        if (lang == "tr" || lang == "az" || lang == "lt") {
            return toLowerCaseEx(str, value, first, locale, true);
        }
        byte[] result = new byte[len];
        System.arraycopy(value, 0, result, 0, first);  // Just copy the first few
                                                       // lowerCase characters.
        for (int i = first; i < len; i++) {
            int cp = value[i] & 0xff;
            cp = CharacterDataLatin1.instance.toLowerCase(cp);
            if (!canEncode(cp)) {                      // not a latin1 character
                return toLowerCaseEx(str, value, first, locale, false);
            }
            result[i] = (byte)cp;
        }
        return new String(result, LATIN1);
    }

    private static String toLowerCaseEx(String str, byte[] value,
                                        int first, Locale locale, boolean localeDependent)
    {
        byte[] result = StringUTF16.newBytesFor(value.length);
        int resultOffset = 0;
        for (int i = 0; i < first; i++) {
            StringUTF16.putChar(result, resultOffset++, value[i] & 0xff);
        }
        for (int i = first; i < value.length; i++) {
            int srcChar = value[i] & 0xff;
            int lowerChar;
            char[] lowerCharArray;
            if (localeDependent) {
                lowerChar = ConditionalSpecialCasing.toLowerCaseEx(str, i, locale);
            } else {
                lowerChar = CharacterDataLatin1.instance.toLowerCase(srcChar);
            }
            if (Character.isBmpCodePoint(lowerChar)) {    // Character.ERROR is not a bmp
                StringUTF16.putChar(result, resultOffset++, lowerChar);
            } else {
                if (lowerChar == Character.ERROR) {
                    lowerCharArray = ConditionalSpecialCasing.toLowerCaseCharArray(str, i, locale);
                } else {
                    lowerCharArray = Character.toChars(lowerChar);
                }
                /* Grow result if needed */
                int mapLen = lowerCharArray.length;
                if (mapLen > 1) {
                    byte[] result2 = StringUTF16.newBytesFor((result.length >> 1) + mapLen - 1);
                    System.arraycopy(result, 0, result2, 0, resultOffset << 1);
                    result = result2;
                }
                for (int x = 0; x < mapLen; ++x) {
                    StringUTF16.putChar(result, resultOffset++, lowerCharArray[x]);
                }
            }
        }
        return StringUTF16.newString(result, 0, resultOffset);
    }

    public static String toUpperCase(String str, byte[] value, Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        int first;
        final int len = value.length;

        // Now check if there are any characters that need to be changed, or are surrogate
        for (first = 0 ; first < len; first++ ) {
            int cp = value[first] & 0xff;
            if (cp != CharacterDataLatin1.instance.toUpperCaseEx(cp)) {   // no need to check Character.ERROR
                break;
            }
        }
        if (first == len) {
            return str;
        }
        String lang = locale.getLanguage();
        if (lang == "tr" || lang == "az" || lang == "lt") {
            return toUpperCaseEx(str, value, first, locale, true);
        }
        byte[] result = new byte[len];
        System.arraycopy(value, 0, result, 0, first);  // Just copy the first few
                                                       // upperCase characters.
        for (int i = first; i < len; i++) {
            int cp = value[i] & 0xff;
            cp = CharacterDataLatin1.instance.toUpperCaseEx(cp);
            if (!canEncode(cp)) {                      // not a latin1 character
                return toUpperCaseEx(str, value, first, locale, false);
            }
            result[i] = (byte)cp;
        }
        return new String(result, LATIN1);
    }

    private static String toUpperCaseEx(String str, byte[] value,
                                        int first, Locale locale, boolean localeDependent)
    {
        byte[] result = StringUTF16.newBytesFor(value.length);
        int resultOffset = 0;
        for (int i = 0; i < first; i++) {
            StringUTF16.putChar(result, resultOffset++, value[i] & 0xff);
        }
        for (int i = first; i < value.length; i++) {
            int srcChar = value[i] & 0xff;
            int upperChar;
            char[] upperCharArray;
            if (localeDependent) {
                upperChar = ConditionalSpecialCasing.toUpperCaseEx(str, i, locale);
            } else {
                upperChar = CharacterDataLatin1.instance.toUpperCaseEx(srcChar);
            }
            if (Character.isBmpCodePoint(upperChar)) {
                StringUTF16.putChar(result, resultOffset++, upperChar);
            } else {
                if (upperChar == Character.ERROR) {
                    if (localeDependent) {
                        upperCharArray =
                            ConditionalSpecialCasing.toUpperCaseCharArray(str, i, locale);
                    } else {
                        upperCharArray = CharacterDataLatin1.instance.toUpperCaseCharArray(srcChar);
                    }
                } else {
                    upperCharArray = Character.toChars(upperChar);
                }
                /* Grow result if needed */
                int mapLen = upperCharArray.length;
                if (mapLen > 1) {
                    byte[] result2 = StringUTF16.newBytesFor((result.length >> 1) + mapLen - 1);
                    System.arraycopy(result, 0, result2, 0, resultOffset << 1);
                    result = result2;
                }
                for (int x = 0; x < mapLen; ++x) {
                    StringUTF16.putChar(result, resultOffset++, upperCharArray[x]);
                }
            }
        }
        return StringUTF16.newString(result, 0, resultOffset);
    }

    public static String trim(byte[] value) {
        int len = value.length;
        int st = 0;
        while ((st < len) && ((value[st] & 0xff) <= ' ')) {
            st++;
        }
        while ((st < len) && ((value[len - 1] & 0xff) <= ' ')) {
            len--;
        }
        return ((st > 0) || (len < value.length)) ?
            newString(value, st, len - st) : null;
    }

    public static int indexOfNonWhitespace(byte[] value) {
        int length = value.length;
        int left = 0;
        while (left < length) {
            char ch = getChar(value, left);
            if (ch != ' ' && ch != '\t' && !CharacterDataLatin1.instance.isWhitespace(ch)) {
                break;
            }
            left++;
        }
        return left;
    }

    public static int lastIndexOfNonWhitespace(byte[] value) {
        int length = value.length;
        int right = length;
        while (0 < right) {
            char ch = getChar(value, right - 1);
            if (ch != ' ' && ch != '\t' && !CharacterDataLatin1.instance.isWhitespace(ch)) {
                break;
            }
            right--;
        }
        return right;
    }

    public static String strip(byte[] value) {
        int left = indexOfNonWhitespace(value);
        if (left == value.length) {
            return "";
        }
        int right = lastIndexOfNonWhitespace(value);
        boolean ifChanged = (left > 0) || (right < value.length);
        return ifChanged ? newString(value, left, right - left) : null;
    }

    public static String stripLeading(byte[] value) {
        int left = indexOfNonWhitespace(value);
        return (left != 0) ? newString(value, left, value.length - left) : null;
    }

    public static String stripTrailing(byte[] value) {
        int right = lastIndexOfNonWhitespace(value);
        return (right != value.length) ? newString(value, 0, right) : null;
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
            return new LinesSpliterator(value, 0, value.length);
        }
    }

    static Stream<String> lines(byte[] value) {
        return StreamSupport.stream(LinesSpliterator.spliterator(value), false);
    }

    public static void putChar(byte[] val, int index, int c) {
        //assert (canEncode(c));
        val[index] = (byte)(c);
    }

    public static char getChar(byte[] val, int index) {
        return (char)(val[index] & 0xff);
    }

    public static byte[] toBytes(int[] val, int off, int len) {
        byte[] ret = new byte[len];
        for (int i = 0; i < len; i++) {
            int cp = val[off++];
            if (!canEncode(cp)) {
                return null;
            }
            ret[i] = (byte)cp;
        }
        return ret;
    }

    public static byte[] toBytes(char c) {
        return new byte[] { (byte)c };
    }

    public static String newString(byte[] val, int index, int len) {
        if (len == 0) {
            return "";
        }
        return new String(Arrays.copyOfRange(val, index, index + len),
                          LATIN1);
    }

    public static void fillNull(byte[] val, int index, int end) {
        Arrays.fill(val, index, end, (byte)0);
    }

    // inflatedCopy byte[] -> char[]
    @IntrinsicCandidate
    public static void inflate(byte[] src, int srcOff, char[] dst, int dstOff, int len) {
        for (int i = 0; i < len; i++) {
            dst[dstOff++] = (char)(src[srcOff++] & 0xff);
        }
    }

    // inflatedCopy byte[] -> byte[]
    @IntrinsicCandidate
    public static void inflate(byte[] src, int srcOff, byte[] dst, int dstOff, int len) {
        StringUTF16.inflate(src, srcOff, dst, dstOff, len);
    }

    static class CharsSpliterator implements Spliterator.OfInt {
        private final byte[] array;
        private int index;        // current index, modified on advance/split
        private final int fence;  // one past last index
        private final int cs;

        CharsSpliterator(byte[] array, int acs) {
            this(array, 0, array.length, acs);
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
            if ((a = array).length >= (hi = fence) &&
                (i = index) >= 0 && i < (index = hi)) {
                do { action.accept(a[i] & 0xff); } while (++i < hi);
            }
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (action == null)
                throw new NullPointerException();
            if (index >= 0 && index < fence) {
                action.accept(array[index++] & 0xff);
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
}
