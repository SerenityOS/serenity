/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
// (c) 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

// created: 2018may10 Markus W. Scherer

package jdk.internal.icu.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Abstract map from Unicode code points (U+0000..U+10FFFF) to integer values.
 * This does not implement java.util.Map.
 *
 * @stable ICU 63
 */
public abstract class CodePointMap implements Iterable<CodePointMap.Range> {
    /**
     * Selectors for how getRange() should report value ranges overlapping with surrogates.
     * Most users should use NORMAL.
     *
     * @see #getRange
     * @stable ICU 63
     */
    public enum RangeOption {
        /**
         * getRange() enumerates all same-value ranges as stored in the map.
         * Most users should use this option.
         *
         * @stable ICU 63
         */
        NORMAL,
        /**
         * getRange() enumerates all same-value ranges as stored in the map,
         * except that lead surrogates (U+D800..U+DBFF) are treated as having the
         * surrogateValue, which is passed to getRange() as a separate parameter.
         * The surrogateValue is not transformed via filter().
         * See {@link Character#isHighSurrogate}.
         *
         * <p>Most users should use NORMAL instead.
         *
         * <p>This option is useful for maps that map surrogate code *units* to
         * special values optimized for UTF-16 string processing
         * or for special error behavior for unpaired surrogates,
         * but those values are not to be associated with the lead surrogate code *points*.
         *
         * @stable ICU 63
         */
        FIXED_LEAD_SURROGATES,
        /**
         * getRange() enumerates all same-value ranges as stored in the map,
         * except that all surrogates (U+D800..U+DFFF) are treated as having the
         * surrogateValue, which is passed to getRange() as a separate parameter.
         * The surrogateValue is not transformed via filter().
         * See {@link Character#isSurrogate}.
         *
         * <p>Most users should use NORMAL instead.
         *
         * <p>This option is useful for maps that map surrogate code *units* to
         * special values optimized for UTF-16 string processing
         * or for special error behavior for unpaired surrogates,
         * but those values are not to be associated with the lead surrogate code *points*.
         *
         * @stable ICU 63
         */
        FIXED_ALL_SURROGATES
    }

    /**
     * Callback function interface: Modifies a map value.
     * Optionally called by getRange().
     * The modified value will be returned by the getRange() function.
     *
     * <p>Can be used to ignore some of the value bits,
     * make a filter for one of several values,
     * return a value index computed from the map value, etc.
     *
     * @see #getRange
     * @see #iterator
     * @stable ICU 63
     */
    public interface ValueFilter {
        /**
         * Modifies the map value.
         *
         * @param value map value
         * @return modified value
         * @stable ICU 63
         */
        public int apply(int value);
    }

    /**
     * Range iteration result data.
     * Code points from start to end map to the same value.
     * The value may have been modified by {@link ValueFilter#apply(int)},
     * or it may be the surrogateValue if a RangeOption other than "normal" was used.
     *
     * @see #getRange
     * @see #iterator
     * @stable ICU 63
     */
    public static final class Range {
        private int start;
        private int end;
        private int value;

        /**
         * Constructor. Sets start and end to -1 and value to 0.
         *
         * @stable ICU 63
         */
        public Range() {
            start = end = -1;
            value = 0;
        }

        /**
         * @return the start code point
         * @stable ICU 63
         */
        public int getStart() { return start; }
        /**
         * @return the (inclusive) end code point
         * @stable ICU 63
         */
        public int getEnd() { return end; }
        /**
         * @return the range value
         * @stable ICU 63
         */
        public int getValue() { return value; }
        /**
         * Sets the range. When using {@link #iterator()},
         * iteration will resume after the newly set end.
         *
         * @param start new start code point
         * @param end new end code point
         * @param value new value
         * @stable ICU 63
         */
        public void set(int start, int end, int value) {
            this.start = start;
            this.end = end;
            this.value = value;
        }
    }

    private final class RangeIterator implements Iterator<Range> {
        private Range range = new Range();

        @Override
        public boolean hasNext() {
            return -1 <= range.end && range.end < 0x10ffff;
        }

        @Override
        public Range next() {
            if (getRange(range.end + 1, null, range)) {
                return range;
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public final void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Iterates over code points of a string and fetches map values.
     * This does not implement java.util.Iterator.
     *
     * <pre>
     * void onString(CodePointMap map, CharSequence s, int start) {
     *     CodePointMap.StringIterator iter = map.stringIterator(s, start);
     *     while (iter.next()) {
     *         int end = iter.getIndex();  // code point from between start and end
     *         useValue(s, start, end, iter.getCodePoint(), iter.getValue());
     *         start = end;
     *     }
     * }
     * </pre>
     *
     * <p>This class is not intended for public subclassing.
     *
     * @stable ICU 63
     */
    public class StringIterator {
        /**
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        protected CharSequence s;
        /**
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        protected int sIndex;
        /**
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        protected int c;
        /**
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        protected int value;

        /**
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        protected StringIterator(CharSequence s, int sIndex) {
            this.s = s;
            this.sIndex = sIndex;
            c = -1;
            value = 0;
        }

        /**
         * Resets the iterator to a new string and/or a new string index.
         *
         * @param s string to iterate over
         * @param sIndex string index where the iteration will start
         * @stable ICU 63
         */
        public void reset(CharSequence s, int sIndex) {
            this.s = s;
            this.sIndex = sIndex;
            c = -1;
            value = 0;
        }

        /**
         * Reads the next code point, post-increments the string index,
         * and gets a value from the map.
         * Sets an implementation-defined error value if the code point is an unpaired surrogate.
         *
         * @return true if the string index was not yet at the end of the string;
         *         otherwise the iterator did not advance
         * @stable ICU 63
         */
        public boolean next() {
            if (sIndex >= s.length()) {
                return false;
            }
            c = Character.codePointAt(s, sIndex);
            sIndex += Character.charCount(c);
            value = get(c);
            return true;
        }

        /**
         * Reads the previous code point, pre-decrements the string index,
         * and gets a value from the map.
         * Sets an implementation-defined error value if the code point is an unpaired surrogate.
         *
         * @return true if the string index was not yet at the start of the string;
         *         otherwise the iterator did not advance
         * @stable ICU 63
         */
        public boolean previous() {
            if (sIndex <= 0) {
                return false;
            }
            c = Character.codePointBefore(s, sIndex);
            sIndex -= Character.charCount(c);
            value = get(c);
            return true;
        }
        /**
         * @return the string index
         * @stable ICU 63
         */
        public final int getIndex() { return sIndex; }
        /**
         * @return the code point
         * @stable ICU 63
         */
        public final int getCodePoint() { return c; }
        /**
         * @return the map value,
         *         or an implementation-defined error value if
         *         the code point is an unpaired surrogate
         * @stable ICU 63
         */
        public final int getValue() { return value; }
    }

    /**
     * Protected no-args constructor.
     *
     * @stable ICU 63
     */
    protected CodePointMap() {
    }

    /**
     * Returns the value for a code point as stored in the map, with range checking.
     * Returns an implementation-defined error value if c is not in the range 0..U+10FFFF.
     *
     * @param c the code point
     * @return the map value,
     *         or an implementation-defined error value if
     *         the code point is not in the range 0..U+10FFFF
     * @stable ICU 63
     */
    public abstract int get(int c);

    /**
     * Sets the range object to a range of code points beginning with the start parameter.
     * The range start is the same as the start input parameter
     * (even if there are preceding code points that have the same value).
     * The range end is the last code point such that
     * all those from start to there have the same value.
     * Returns false if start is not 0..U+10FFFF.
     * Can be used to efficiently iterate over all same-value ranges in a map.
     * (This is normally faster than iterating over code points and get()ting each value,
     * but may be much slower than a data structure that stores ranges directly.)
     *
     * <p>If the {@link ValueFilter} parameter is not null, then
     * the value to be delivered is passed through that filter, and the return value is the end
     * of the range where all values are modified to the same actual value.
     * The value is unchanged if that parameter is null.
     *
     * <p>Example:
     * <pre>
     * int start = 0;
     * CodePointMap.Range range = new CodePointMap.Range();
     * while (map.getRange(start, null, range)) {
     *     int end = range.getEnd();
     *     int value = range.getValue();
     *     // Work with the range start..end and its value.
     *     start = end + 1;
     * }
     * </pre>
     *
     * @param start range start
     * @param filter an object that may modify the map data value,
     *     or null if the values from the map are to be used unmodified
     * @param range the range object that will be set to the code point range and value
     * @return true if start is 0..U+10FFFF; otherwise no new range is fetched
     * @stable ICU 63
     */
    public abstract boolean getRange(int start, ValueFilter filter, Range range);

    /**
     * Sets the range object to a range of code points beginning with the start parameter.
     * The range start is the same as the start input parameter
     * (even if there are preceding code points that have the same value).
     * The range end is the last code point such that
     * all those from start to there have the same value.
     * Returns false if start is not 0..U+10FFFF.
     *
     * <p>Same as the simpler {@link #getRange(int, ValueFilter, Range)} but optionally
     * modifies the range if it overlaps with surrogate code points.
     *
     * @param start range start
     * @param option defines whether surrogates are treated normally,
     *               or as having the surrogateValue; usually {@link RangeOption#NORMAL}
     * @param surrogateValue value for surrogates; ignored if option=={@link RangeOption#NORMAL}
     * @param filter an object that may modify the map data value,
     *     or null if the values from the map are to be used unmodified
     * @param range the range object that will be set to the code point range and value
     * @return true if start is 0..U+10FFFF; otherwise no new range is fetched
     * @stable ICU 63
     */
    public boolean getRange(int start, RangeOption option, int surrogateValue,
            ValueFilter filter, Range range) {
        assert option != null;
        if (!getRange(start, filter, range)) {
            return false;
        }
        if (option == RangeOption.NORMAL) {
            return true;
        }
        int surrEnd = option == RangeOption.FIXED_ALL_SURROGATES ? 0xdfff : 0xdbff;
        int end = range.end;
        if (end < 0xd7ff || start > surrEnd) {
            return true;
        }
        // The range overlaps with surrogates, or ends just before the first one.
        if (range.value == surrogateValue) {
            if (end >= surrEnd) {
                // Surrogates followed by a non-surrValue range,
                // or surrogates are part of a larger surrValue range.
                return true;
            }
        } else {
            if (start <= 0xd7ff) {
                range.end = 0xd7ff;  // Non-surrValue range ends before surrValue surrogates.
                return true;
            }
            // Start is a surrogate with a non-surrValue code *unit* value.
            // Return a surrValue code *point* range.
            range.value = surrogateValue;
            if (end > surrEnd) {
                range.end = surrEnd;  // Surrogate range ends before non-surrValue rest of range.
                return true;
            }
        }
        // See if the surrValue surrogate range can be merged with
        // an immediately following range.
        if (getRange(surrEnd + 1, filter, range) && range.value == surrogateValue) {
            range.start = start;
            return true;
        }
        range.start = start;
        range.end = surrEnd;
        range.value = surrogateValue;
        return true;
    }

    /**
     * Convenience iterator over same-map-value code point ranges.
     * Same as looping over all ranges with {@link #getRange(int, ValueFilter, Range)}
     * without filtering.
     * Adjacent ranges have different map values.
     *
     * <p>The iterator always returns the same Range object.
     *
     * @return a Range iterator
     * @stable ICU 63
     */
    @Override
    public Iterator<Range> iterator() {
        return new RangeIterator();
    }

    /**
     * Returns an iterator (not a java.util.Iterator) over code points of a string
     * for fetching map values.
     *
     * @param s string to iterate over
     * @param sIndex string index where the iteration will start
     * @return the iterator
     * @stable ICU 63
     */
    public StringIterator stringIterator(CharSequence s, int sIndex) {
        return new StringIterator(s, sIndex);
    }
}
