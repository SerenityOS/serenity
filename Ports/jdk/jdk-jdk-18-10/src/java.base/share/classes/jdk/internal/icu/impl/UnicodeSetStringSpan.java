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

/*
 ******************************************************************************
 *
 *   Copyright (C) 2009-2014, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 ******************************************************************************
 */

package jdk.internal.icu.impl;

import java.util.ArrayList;

import jdk.internal.icu.text.UTF16;
import jdk.internal.icu.text.UnicodeSet;
import jdk.internal.icu.text.UnicodeSet.SpanCondition;
import jdk.internal.icu.util.OutputInt;

/*
 * Implement span() etc. for a set with strings.
 * Avoid recursion because of its exponential complexity.
 * Instead, try multiple paths at once and track them with an IndexList.
 */
public class UnicodeSetStringSpan {

    /*
     * Which span() variant will be used? The object is either built for one variant and used once,
     * or built for all and may be used many times.
     */
    public static final int WITH_COUNT    = 0x40;  // spanAndCount() may be called
    public static final int FWD           = 0x20;
    public static final int BACK          = 0x10;
    // public static final int UTF16      = 8;
    public static final int CONTAINED     = 2;
    public static final int NOT_CONTAINED = 1;

    public static final int ALL = 0x7f;

    public static final int FWD_UTF16_CONTAINED      = FWD  | /* UTF16 | */    CONTAINED;
    public static final int FWD_UTF16_NOT_CONTAINED  = FWD  | /* UTF16 | */NOT_CONTAINED;
    public static final int BACK_UTF16_CONTAINED     = BACK | /* UTF16 | */    CONTAINED;
    public static final int BACK_UTF16_NOT_CONTAINED = BACK | /* UTF16 | */NOT_CONTAINED;

    /**
     * Special spanLength short values. (since Java has not unsigned byte type)
     * All code points in the string are contained in the parent set.
     */
    static final short ALL_CP_CONTAINED = 0xff;

    /** The spanLength is >=0xfe. */
    static final short LONG_SPAN = ALL_CP_CONTAINED - 1;

    /** Set for span(). Same as parent but without strings. */
    private UnicodeSet spanSet;

    /**
     * Set for span(not contained).
     * Same as spanSet, plus characters that start or end strings.
     */
    private UnicodeSet spanNotSet;

    /** The strings of the parent set. */
    private ArrayList<String> strings;

    /** The lengths of span(), spanBack() etc. for each string. */
    private short[] spanLengths;

    /** Maximum lengths of relevant strings. */
    private int maxLength16;

    /** Are there strings that are not fully contained in the code point set? */
    private boolean someRelevant;

    /** Set up for all variants of span()? */
    private boolean all;

    /** Span helper */
    private OffsetList offsets;

    /**
     * Constructs for all variants of span(), or only for any one variant.
     * Initializes as little as possible, for single use.
     */
    public UnicodeSetStringSpan(final UnicodeSet set, final ArrayList<String> setStrings, int which) {
        spanSet = new UnicodeSet(0, 0x10ffff);
        // TODO: With Java 6, just take the parent set's strings as is,
        // as a NavigableSet<String>, rather than as an ArrayList copy of the set of strings.
        // Then iterate via the first() and higher() methods.
        // (We do not want to create multiple Iterator objects in each span().)
        // See ICU ticket #7454.
        strings = setStrings;
        all = (which == ALL);
        spanSet.retainAll(set);
        if (0 != (which & NOT_CONTAINED)) {
            // Default to the same sets.
            // addToSpanNotSet() will create a separate set if necessary.
            spanNotSet = spanSet;
        }
        offsets = new OffsetList();

        // Determine if the strings even need to be taken into account at all for span() etc.
        // If any string is relevant, then all strings need to be used for
        // span(longest match) but only the relevant ones for span(while contained).
        // TODO: Possible optimization: Distinguish CONTAINED vs. LONGEST_MATCH
        // and do not store UTF-8 strings if !thisRelevant and CONTAINED.
        // (Only store irrelevant UTF-8 strings for LONGEST_MATCH where they are relevant after all.)
        // Also count the lengths of the UTF-8 versions of the strings for memory allocation.
        int stringsLength = strings.size();

        int i, spanLength;
        someRelevant = false;
        for (i = 0; i < stringsLength; ++i) {
            String string = strings.get(i);
            int length16 = string.length();
            spanLength = spanSet.span(string, SpanCondition.CONTAINED);
            if (spanLength < length16) { // Relevant string.
                someRelevant = true;
            }
            if (/* (0 != (which & UTF16)) && */ length16 > maxLength16) {
                maxLength16 = length16;
            }
        }
        if (!someRelevant && (which & WITH_COUNT) == 0) {
            return;
        }

        // Freeze after checking for the need to use strings at all because freezing
        // a set takes some time and memory which are wasted if there are no relevant strings.
        if (all) {
            spanSet.freeze();
        }

        int spanBackLengthsOffset;

        // Allocate a block of meta data.
        int allocSize;
        if (all) {
            // 2 sets of span lengths
            allocSize = stringsLength * (2);
        } else {
            allocSize = stringsLength; // One set of span lengths.
        }
        spanLengths = new short[allocSize];

        if (all) {
            // Store span lengths for all span() variants.
            spanBackLengthsOffset = stringsLength;
        } else {
            // Store span lengths for only one span() variant.
            spanBackLengthsOffset = 0;
        }

        // Set the meta data and spanNotSet and write the UTF-8 strings.

        for (i = 0; i < stringsLength; ++i) {
            String string = strings.get(i);
            int length16 = string.length();
            spanLength = spanSet.span(string, SpanCondition.CONTAINED);
            if (spanLength < length16) { // Relevant string.
                if (true /* 0 != (which & UTF16) */) {
                    if (0 != (which & CONTAINED)) {
                        if (0 != (which & FWD)) {
                            spanLengths[i] = makeSpanLengthByte(spanLength);
                        }
                        if (0 != (which & BACK)) {
                            spanLength = length16
                                    - spanSet.spanBack(string, length16, SpanCondition.CONTAINED);
                            spanLengths[spanBackLengthsOffset + i] = makeSpanLengthByte(spanLength);
                        }
                    } else /* not CONTAINED, not all, but NOT_CONTAINED */{
                        spanLengths[i] = spanLengths[spanBackLengthsOffset + i] = 0; // Only store a relevant/irrelevant
                                                                                     // flag.
                    }
                }
                if (0 != (which & NOT_CONTAINED)) {
                    // Add string start and end code points to the spanNotSet so that
                    // a span(while not contained) stops before any string.
                    int c;
                    if (0 != (which & FWD)) {
                        c = string.codePointAt(0);
                        addToSpanNotSet(c);
                    }
                    if (0 != (which & BACK)) {
                        c = string.codePointBefore(length16);
                        addToSpanNotSet(c);
                    }
                }
            } else { // Irrelevant string.
                if (all) {
                    spanLengths[i] = spanLengths[spanBackLengthsOffset + i] = ALL_CP_CONTAINED;
                } else {
                    // All spanXYZLengths pointers contain the same address.
                    spanLengths[i] = ALL_CP_CONTAINED;
                }
            }
        }

        // Finish.
        if (all) {
            spanNotSet.freeze();
        }
    }

    /**
     * Do the strings need to be checked in span() etc.?
     *
     * @return true if strings need to be checked (call span() here),
     *         false if not (use a BMPSet for best performance).
     */
    public boolean needsStringSpanUTF16() {
        return someRelevant;
    }

    /** For fast UnicodeSet::contains(c). */
    public boolean contains(int c) {
        return spanSet.contains(c);
    }

    /**
     * Adds a starting or ending string character to the spanNotSet
     * so that a character span ends before any string.
     */
    private void addToSpanNotSet(int c) {
        if (spanNotSet == null || spanNotSet == spanSet) {
            if (spanSet.contains(c)) {
                return; // Nothing to do.
            }
            spanNotSet = spanSet.cloneAsThawed();
        }
        spanNotSet.add(c);
    }

    /*
     * Note: In span() when spanLength==0
     * (after a string match, or at the beginning after an empty code point span)
     * and in spanNot() and spanNotUTF8(),
     * string matching could use a binary search because all string matches are done
     * from the same start index.
     *
     * For UTF-8, this would require a comparison function that returns UTF-16 order.
     *
     * This optimization should not be necessary for normal UnicodeSets because most sets have no strings, and most sets
     * with strings have very few very short strings. For cases with many strings, it might be better to use a different
     * API and implementation with a DFA (state machine).
     */

    /*
     * Algorithm for span(SpanCondition.CONTAINED)
     *
     * Theoretical algorithm:
     * - Iterate through the string, and at each code point boundary:
     *   + If the code point there is in the set, then remember to continue after it.
     *   + If a set string matches at the current position, then remember to continue after it.
     *   + Either recursively span for each code point or string match, or recursively span
     *     for all but the shortest one and iteratively continue the span with the shortest local match.
     *   + Remember the longest recursive span (the farthest end point).
     *   + If there is no match at the current position,
     *     neither for the code point there nor for any set string,
     *     then stop and return the longest recursive span length.
     *
     * Optimized implementation:
     *
     * (We assume that most sets will have very few very short strings.
     * A span using a string-less set is extremely fast.)
     *
     * Create and cache a spanSet which contains all of the single code points of the original set
     * but none of its strings.
     *
     * - Start with spanLength=spanSet.span(SpanCondition.CONTAINED).
     * - Loop:
     *   + Try to match each set string at the end of the spanLength.
     *     ~ Set strings that start with set-contained code points
     *       must be matched with a partial overlap
     *       because the recursive algorithm would have tried to match them at every position.
     *     ~ Set strings that entirely consist of set-contained code points
     *       are irrelevant for span(SpanCondition.CONTAINED)
     *       because the recursive algorithm would continue after them anyway and
     *       find the longest recursive match from their end.
     *     ~ Rather than recursing, note each end point of a set string match.
     *   + If no set string matched after spanSet.span(),
     *     then return with where the spanSet.span() ended.
     *   + If at least one set string matched after spanSet.span(),
     *     then pop the shortest string match end point and continue the loop,
     *     trying to match all set strings from there.
     *   + If at least one more set string matched after a previous string match, then test if the
     *     code point after the previous string match is also contained in the set.
     *     Continue the loop with the shortest end point of
     *     either this code point or a matching set string.
     *   + If no more set string matched after a previous string match,
     *     then try another spanLength=spanSet.span(SpanCondition.CONTAINED).
     *     Stop if spanLength==0, otherwise continue the loop.
     *
     * By noting each end point of a set string match, the function visits each string position at most once and
     * finishes in linear time.
     *
     * The recursive algorithm may visit the same string position many times
     * if multiple paths lead to it and finishes in exponential time.
     */

    /*
     * Algorithm for span(SIMPLE)
     *
     * Theoretical algorithm:
     * - Iterate through the string, and at each code point boundary:
     *   + If the code point there is in the set, then remember to continue after it.
     *   + If a set string matches at the current position, then remember to continue after it.
     *   + Continue from the farthest match position and ignore all others.
     *   + If there is no match at the current position, then stop and return the current position.
     *
     * Optimized implementation:
     *
     * (Same assumption and spanSet as above.)
     *
     * - Start with spanLength=spanSet.span(SpanCondition.CONTAINED).
     * - Loop:
     *   + Try to match each set string at the end of the spanLength.
     *     ~ Set strings that start with set-contained code points
     *       must be matched with a partial overlap
     *       because the standard algorithm would have tried to match them earlier.
     *     ~ Set strings that entirely consist of set-contained code points
     *       must be matched with a full overlap because the longest-match algorithm
     *       would hide set string matches that end earlier.
     *       Such set strings need not be matched earlier inside the code point span
     *       because the standard algorithm would then have
     *       continued after the set string match anyway.
     *     ~ Remember the longest set string match (farthest end point)
     *       from the earliest starting point.
     *   + If no set string matched after spanSet.span(),
     *     then return with where the spanSet.span() ended.
     *   + If at least one set string matched,
     *     then continue the loop after the longest match from the earliest position.
     *   + If no more set string matched after a previous string match,
     *     then try another spanLength=spanSet.span(SpanCondition.CONTAINED).
     *     Stop if spanLength==0, otherwise continue the loop.
     */
    /**
     * Spans a string.
     *
     * @param s The string to be spanned
     * @param start The start index that the span begins
     * @param spanCondition The span condition
     * @return the limit (exclusive end) of the span
     */
    public int span(CharSequence s, int start, SpanCondition spanCondition) {
        if (spanCondition == SpanCondition.NOT_CONTAINED) {
            return spanNot(s, start, null);
        }
        int spanLimit = spanSet.span(s, start, SpanCondition.CONTAINED);
        if (spanLimit == s.length()) {
            return spanLimit;
        }
        return spanWithStrings(s, start, spanLimit, spanCondition);
    }

    /**
     * Synchronized method for complicated spans using the offsets.
     * Avoids synchronization for simple cases.
     *
     * @param spanLimit = spanSet.span(s, start, CONTAINED)
     */
    private synchronized int spanWithStrings(CharSequence s, int start, int spanLimit,
            SpanCondition spanCondition) {
        // Consider strings; they may overlap with the span.
        int initSize = 0;
        if (spanCondition == SpanCondition.CONTAINED) {
            // Use offset list to try all possibilities.
            initSize = maxLength16;
        }
        offsets.setMaxLength(initSize);
        int length = s.length();
        int pos = spanLimit, rest = length - spanLimit;
        int spanLength = spanLimit - start;
        int i, stringsLength = strings.size();
        for (;;) {
            if (spanCondition == SpanCondition.CONTAINED) {
                for (i = 0; i < stringsLength; ++i) {
                    int overlap = spanLengths[i];
                    if (overlap == ALL_CP_CONTAINED) {
                        continue; // Irrelevant string.
                    }
                    String string = strings.get(i);

                    int length16 = string.length();

                    // Try to match this string at pos-overlap..pos.
                    if (overlap >= LONG_SPAN) {
                        overlap = length16;
                        // While contained: No point matching fully inside the code point span.
                        overlap = string.offsetByCodePoints(overlap, -1); // Length of the string minus the last code
                                                                          // point.
                    }
                    if (overlap > spanLength) {
                        overlap = spanLength;
                    }
                    int inc = length16 - overlap; // Keep overlap+inc==length16.
                    for (;;) {
                        if (inc > rest) {
                            break;
                        }
                        // Try to match if the increment is not listed already.
                        if (!offsets.containsOffset(inc) && matches16CPB(s, pos - overlap, length, string, length16)) {
                            if (inc == rest) {
                                return length; // Reached the end of the string.
                            }
                            offsets.addOffset(inc);
                        }
                        if (overlap == 0) {
                            break;
                        }
                        --overlap;
                        ++inc;
                    }
                }
            } else /* SIMPLE */{
                int maxInc = 0, maxOverlap = 0;
                for (i = 0; i < stringsLength; ++i) {
                    int overlap = spanLengths[i];
                    // For longest match, we do need to try to match even an all-contained string
                    // to find the match from the earliest start.

                    String string = strings.get(i);

                    int length16 = string.length();

                    // Try to match this string at pos-overlap..pos.
                    if (overlap >= LONG_SPAN) {
                        overlap = length16;
                        // Longest match: Need to match fully inside the code point span
                        // to find the match from the earliest start.
                    }
                    if (overlap > spanLength) {
                        overlap = spanLength;
                    }
                    int inc = length16 - overlap; // Keep overlap+inc==length16.
                    for (;;) {
                        if (inc > rest || overlap < maxOverlap) {
                            break;
                        }
                        // Try to match if the string is longer or starts earlier.
                        if ((overlap > maxOverlap || /* redundant overlap==maxOverlap && */inc > maxInc)
                                && matches16CPB(s, pos - overlap, length, string, length16)) {
                            maxInc = inc; // Longest match from earliest start.
                            maxOverlap = overlap;
                            break;
                        }
                        --overlap;
                        ++inc;
                    }
                }

                if (maxInc != 0 || maxOverlap != 0) {
                    // Longest-match algorithm, and there was a string match.
                    // Simply continue after it.
                    pos += maxInc;
                    rest -= maxInc;
                    if (rest == 0) {
                        return length; // Reached the end of the string.
                    }
                    spanLength = 0; // Match strings from after a string match.
                    continue;
                }
            }
            // Finished trying to match all strings at pos.

            if (spanLength != 0 || pos == 0) {
                // The position is after an unlimited code point span (spanLength!=0),
                // not after a string match.
                // The only position where spanLength==0 after a span is pos==0.
                // Otherwise, an unlimited code point span is only tried again when no
                // strings match, and if such a non-initial span fails we stop.
                if (offsets.isEmpty()) {
                    return pos; // No strings matched after a span.
                }
                // Match strings from after the next string match.
            } else {
                // The position is after a string match (or a single code point).
                if (offsets.isEmpty()) {
                    // No more strings matched after a previous string match.
                    // Try another code point span from after the last string match.
                    spanLimit = spanSet.span(s, pos, SpanCondition.CONTAINED);
                    spanLength = spanLimit - pos;
                    if (spanLength == rest || // Reached the end of the string, or
                            spanLength == 0 // neither strings nor span progressed.
                    ) {
                        return spanLimit;
                    }
                    pos += spanLength;
                    rest -= spanLength;
                    continue; // spanLength>0: Match strings from after a span.
                } else {
                    // Try to match only one code point from after a string match if some
                    // string matched beyond it, so that we try all possible positions
                    // and don't overshoot.
                    spanLength = spanOne(spanSet, s, pos, rest);
                    if (spanLength > 0) {
                        if (spanLength == rest) {
                            return length; // Reached the end of the string.
                        }
                        // Match strings after this code point.
                        // There cannot be any increments below it because UnicodeSet strings
                        // contain multiple code points.
                        pos += spanLength;
                        rest -= spanLength;
                        offsets.shift(spanLength);
                        spanLength = 0;
                        continue; // Match strings from after a single code point.
                    }
                    // Match strings from after the next string match.
                }
            }
            int minOffset = offsets.popMinimum(null);
            pos += minOffset;
            rest -= minOffset;
            spanLength = 0; // Match strings from after a string match.
        }
    }

    /**
     * Spans a string and counts the smallest number of set elements on any path across the span.
     *
     * <p>For proper counting, we cannot ignore strings that are fully contained in code point spans.
     *
     * <p>If the set does not have any fully-contained strings, then we could optimize this
     * like span(), but such sets are likely rare, and this is at least still linear.
     *
     * @param s The string to be spanned
     * @param start The start index that the span begins
     * @param spanCondition The span condition
     * @param outCount The count
     * @return the limit (exclusive end) of the span
     */
    public int spanAndCount(CharSequence s, int start, SpanCondition spanCondition,
            OutputInt outCount) {
        if (spanCondition == SpanCondition.NOT_CONTAINED) {
            return spanNot(s, start, outCount);
        }
        // Consider strings; they may overlap with the span,
        // and they may result in a smaller count that with just code points.
        if (spanCondition == SpanCondition.CONTAINED) {
            return spanContainedAndCount(s, start, outCount);
        }
        // SIMPLE (not synchronized, does not use offsets)
        int stringsLength = strings.size();
        int length = s.length();
        int pos = start;
        int rest = length - start;
        int count = 0;
        while (rest != 0) {
            // Try to match the next code point.
            int cpLength = spanOne(spanSet, s, pos, rest);
            int maxInc = (cpLength > 0) ? cpLength : 0;
            // Try to match all of the strings.
            for (int i = 0; i < stringsLength; ++i) {
                String string = strings.get(i);
                int length16 = string.length();
                if (maxInc < length16 && length16 <= rest &&
                        matches16CPB(s, pos, length, string, length16)) {
                    maxInc = length16;
                }
            }
            // We are done if there is no match beyond pos.
            if (maxInc == 0) {
                outCount.value = count;
                return pos;
            }
            // Continue from the longest match.
            ++count;
            pos += maxInc;
            rest -= maxInc;
        }
        outCount.value = count;
        return pos;
    }

    private synchronized int spanContainedAndCount(CharSequence s, int start, OutputInt outCount) {
        // Use offset list to try all possibilities.
        offsets.setMaxLength(maxLength16);
        int stringsLength = strings.size();
        int length = s.length();
        int pos = start;
        int rest = length - start;
        int count = 0;
        while (rest != 0) {
            // Try to match the next code point.
            int cpLength = spanOne(spanSet, s, pos, rest);
            if (cpLength > 0) {
                offsets.addOffsetAndCount(cpLength, count + 1);
            }
            // Try to match all of the strings.
            for (int i = 0; i < stringsLength; ++i) {
                String string = strings.get(i);
                int length16 = string.length();
                // Note: If the strings were sorted by length, then we could also
                // avoid trying to match if there is already a match of the same length.
                if (length16 <= rest && !offsets.hasCountAtOffset(length16, count + 1) &&
                        matches16CPB(s, pos, length, string, length16)) {
                    offsets.addOffsetAndCount(length16, count + 1);
                }
            }
            // We are done if there is no match beyond pos.
            if (offsets.isEmpty()) {
                outCount.value = count;
                return pos;
            }
            // Continue from the nearest match.
            int minOffset = offsets.popMinimum(outCount);
            count = outCount.value;
            pos += minOffset;
            rest -= minOffset;
        }
        outCount.value = count;
        return pos;
    }

    /**
     * Span a string backwards.
     *
     * @param s The string to be spanned
     * @param spanCondition The span condition
     * @return The string index which starts the span (i.e. inclusive).
     */
    public synchronized int spanBack(CharSequence s, int length, SpanCondition spanCondition) {
        if (spanCondition == SpanCondition.NOT_CONTAINED) {
            return spanNotBack(s, length);
        }
        int pos = spanSet.spanBack(s, length, SpanCondition.CONTAINED);
        if (pos == 0) {
            return 0;
        }
        int spanLength = length - pos;

        // Consider strings; they may overlap with the span.
        int initSize = 0;
        if (spanCondition == SpanCondition.CONTAINED) {
            // Use offset list to try all possibilities.
            initSize = maxLength16;
        }
        offsets.setMaxLength(initSize);
        int i, stringsLength = strings.size();
        int spanBackLengthsOffset = 0;
        if (all) {
            spanBackLengthsOffset = stringsLength;
        }
        for (;;) {
            if (spanCondition == SpanCondition.CONTAINED) {
                for (i = 0; i < stringsLength; ++i) {
                    int overlap = spanLengths[spanBackLengthsOffset + i];
                    if (overlap == ALL_CP_CONTAINED) {
                        continue; // Irrelevant string.
                    }
                    String string = strings.get(i);

                    int length16 = string.length();

                    // Try to match this string at pos-(length16-overlap)..pos-length16.
                    if (overlap >= LONG_SPAN) {
                        overlap = length16;
                        // While contained: No point matching fully inside the code point span.
                        int len1 = 0;
                        len1 = string.offsetByCodePoints(0, 1);
                        overlap -= len1; // Length of the string minus the first code point.
                    }
                    if (overlap > spanLength) {
                        overlap = spanLength;
                    }
                    int dec = length16 - overlap; // Keep dec+overlap==length16.
                    for (;;) {
                        if (dec > pos) {
                            break;
                        }
                        // Try to match if the decrement is not listed already.
                        if (!offsets.containsOffset(dec) && matches16CPB(s, pos - dec, length, string, length16)) {
                            if (dec == pos) {
                                return 0; // Reached the start of the string.
                            }
                            offsets.addOffset(dec);
                        }
                        if (overlap == 0) {
                            break;
                        }
                        --overlap;
                        ++dec;
                    }
                }
            } else /* SIMPLE */{
                int maxDec = 0, maxOverlap = 0;
                for (i = 0; i < stringsLength; ++i) {
                    int overlap = spanLengths[spanBackLengthsOffset + i];
                    // For longest match, we do need to try to match even an all-contained string
                    // to find the match from the latest end.

                    String string = strings.get(i);

                    int length16 = string.length();

                    // Try to match this string at pos-(length16-overlap)..pos-length16.
                    if (overlap >= LONG_SPAN) {
                        overlap = length16;
                        // Longest match: Need to match fully inside the code point span
                        // to find the match from the latest end.
                    }
                    if (overlap > spanLength) {
                      overlap = spanLength;
                    }
                    int dec = length16 - overlap; // Keep dec+overlap==length16.
                    for (;;) {
                        if (dec > pos || overlap < maxOverlap) {
                            break;
                        }
                        // Try to match if the string is longer or ends later.
                        if ((overlap > maxOverlap || /* redundant overlap==maxOverlap && */dec > maxDec)
                                && matches16CPB(s, pos - dec, length, string, length16)) {
                            maxDec = dec; // Longest match from latest end.
                            maxOverlap = overlap;
                            break;
                        }
                        --overlap;
                        ++dec;
                    }
                }

                if (maxDec != 0 || maxOverlap != 0) {
                    // Longest-match algorithm, and there was a string match.
                    // Simply continue before it.
                    pos -= maxDec;
                    if (pos == 0) {
                        return 0; // Reached the start of the string.
                    }
                    spanLength = 0; // Match strings from before a string match.
                    continue;
                }
            }
            // Finished trying to match all strings at pos.

            if (spanLength != 0 || pos == length) {
                // The position is before an unlimited code point span (spanLength!=0),
                // not before a string match.
                // The only position where spanLength==0 before a span is pos==length.
                // Otherwise, an unlimited code point span is only tried again when no
                // strings match, and if such a non-initial span fails we stop.
                if (offsets.isEmpty()) {
                    return pos; // No strings matched before a span.
                }
                // Match strings from before the next string match.
            } else {
                // The position is before a string match (or a single code point).
                if (offsets.isEmpty()) {
                    // No more strings matched before a previous string match.
                    // Try another code point span from before the last string match.
                    int oldPos = pos;
                    pos = spanSet.spanBack(s, oldPos, SpanCondition.CONTAINED);
                    spanLength = oldPos - pos;
                    if (pos == 0 || // Reached the start of the string, or
                            spanLength == 0 // neither strings nor span progressed.
                    ) {
                        return pos;
                    }
                    continue; // spanLength>0: Match strings from before a span.
                } else {
                    // Try to match only one code point from before a string match if some
                    // string matched beyond it, so that we try all possible positions
                    // and don't overshoot.
                    spanLength = spanOneBack(spanSet, s, pos);
                    if (spanLength > 0) {
                        if (spanLength == pos) {
                            return 0; // Reached the start of the string.
                        }
                        // Match strings before this code point.
                        // There cannot be any decrements below it because UnicodeSet strings
                        // contain multiple code points.
                        pos -= spanLength;
                        offsets.shift(spanLength);
                        spanLength = 0;
                        continue; // Match strings from before a single code point.
                    }
                    // Match strings from before the next string match.
                }
            }
            pos -= offsets.popMinimum(null);
            spanLength = 0; // Match strings from before a string match.
        }
    }

    /**
     * Algorithm for spanNot()==span(SpanCondition.NOT_CONTAINED)
     *
     * Theoretical algorithm:
     * - Iterate through the string, and at each code point boundary:
     *   + If the code point there is in the set, then return with the current position.
     *   + If a set string matches at the current position, then return with the current position.
     *
     * Optimized implementation:
     *
     * (Same assumption as for span() above.)
     *
     * Create and cache a spanNotSet which contains
     * all of the single code points of the original set but none of its strings.
     * For each set string add its initial code point to the spanNotSet.
     * (Also add its final code point for spanNotBack().)
     *
     * - Loop:
     *   + Do spanLength=spanNotSet.span(SpanCondition.NOT_CONTAINED).
     *   + If the current code point is in the original set, then return the current position.
     *   + If any set string matches at the current position, then return the current position.
     *   + If there is no match at the current position, neither for the code point
     *     there nor for any set string, then skip this code point and continue the loop.
     *     This happens for set-string-initial code points that were added to spanNotSet
     *     when there is not actually a match for such a set string.
     *
     * @param s The string to be spanned
     * @param start The start index that the span begins
     * @param outCount If not null: Receives the number of code points across the span.
     * @return the limit (exclusive end) of the span
     */
    private int spanNot(CharSequence s, int start, OutputInt outCount) {
        int length = s.length();
        int pos = start, rest = length - start;
        int stringsLength = strings.size();
        int count = 0;
        do {
            // Span until we find a code point from the set,
            // or a code point that starts or ends some string.
            int spanLimit;
            if (outCount == null) {
                spanLimit = spanNotSet.span(s, pos, SpanCondition.NOT_CONTAINED);
            } else {
                spanLimit = spanNotSet.spanAndCount(s, pos, SpanCondition.NOT_CONTAINED, outCount);
                outCount.value = count = count + outCount.value;
            }
            if (spanLimit == length) {
                return length; // Reached the end of the string.
            }
            pos = spanLimit;
            rest = length - spanLimit;

            // Check whether the current code point is in the original set,
            // without the string starts and ends.
            int cpLength = spanOne(spanSet, s, pos, rest);
            if (cpLength > 0) {
                return pos; // There is a set element at pos.
            }

            // Try to match the strings at pos.
            for (int i = 0; i < stringsLength; ++i) {
                if (spanLengths[i] == ALL_CP_CONTAINED) {
                    continue; // Irrelevant string.
                }
                String string = strings.get(i);

                int length16 = string.length();
                if (length16 <= rest && matches16CPB(s, pos, length, string, length16)) {
                    return pos; // There is a set element at pos.
                }
            }

            // The span(while not contained) ended on a string start/end which is
            // not in the original set. Skip this code point and continue.
            // cpLength<0
            pos -= cpLength;
            rest += cpLength;
            ++count;
        } while (rest != 0);
        if (outCount != null) {
            outCount.value = count;
        }
        return length; // Reached the end of the string.
    }

    private int spanNotBack(CharSequence s, int length) {
        int pos = length;
        int i, stringsLength = strings.size();
        do {
            // Span until we find a code point from the set,
            // or a code point that starts or ends some string.
            pos = spanNotSet.spanBack(s, pos, SpanCondition.NOT_CONTAINED);
            if (pos == 0) {
                return 0; // Reached the start of the string.
            }

            // Check whether the current code point is in the original set,
            // without the string starts and ends.
            int cpLength = spanOneBack(spanSet, s, pos);
            if (cpLength > 0) {
                return pos; // There is a set element at pos.
            }

            // Try to match the strings at pos.
            for (i = 0; i < stringsLength; ++i) {
                // Use spanLengths rather than a spanLengths pointer because
                // it is easier and we only need to know whether the string is irrelevant
                // which is the same in either array.
                if (spanLengths[i] == ALL_CP_CONTAINED) {
                    continue; // Irrelevant string.
                }
                String string = strings.get(i);

                int length16 = string.length();
                if (length16 <= pos && matches16CPB(s, pos - length16, length, string, length16)) {
                    return pos; // There is a set element at pos.
                }
            }

            // The span(while not contained) ended on a string start/end which is
            // not in the original set. Skip this code point and continue.
            // cpLength<0
            pos += cpLength;
        } while (pos != 0);
        return 0; // Reached the start of the string.
    }

    static short makeSpanLengthByte(int spanLength) {
        // 0xfe==UnicodeSetStringSpan::LONG_SPAN
        return spanLength < LONG_SPAN ? (short) spanLength : LONG_SPAN;
    }

    // Compare strings without any argument checks. Requires length>0.
    private static boolean matches16(CharSequence s, int start, final String t, int length) {
        int end = start + length;
        while (length-- > 0) {
            if (s.charAt(--end) != t.charAt(length)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compare 16-bit Unicode strings (which may be malformed UTF-16)
     * at code point boundaries.
     * That is, each edge of a match must not be in the middle of a surrogate pair.
     * @param s       The string to match in.
     * @param start   The start index of s.
     * @param limit   The limit of the subsequence of s being spanned.
     * @param t       The substring to be matched in s.
     * @param tlength The length of t.
     */
    static boolean matches16CPB(CharSequence s, int start, int limit, final String t, int tlength) {
        return matches16(s, start, t, tlength)
                && !(0 < start && Character.isHighSurrogate(s.charAt(start - 1)) &&
                        Character.isLowSurrogate(s.charAt(start)))
                && !((start + tlength) < limit && Character.isHighSurrogate(s.charAt(start + tlength - 1)) &&
                        Character.isLowSurrogate(s.charAt(start + tlength)));
    }

    /**
     * Does the set contain the next code point?
     * If so, return its length; otherwise return its negative length.
     */
    static int spanOne(final UnicodeSet set, CharSequence s, int start, int length) {
        char c = s.charAt(start);
        if (c >= 0xd800 && c <= 0xdbff && length >= 2) {
            char c2 = s.charAt(start + 1);
            if (UTF16.isTrailSurrogate(c2)) {
                int supplementary = UCharacterProperty.getRawSupplementary(c, c2);
                return set.contains(supplementary) ? 2 : -2;
            }
        }
        return set.contains(c) ? 1 : -1;
    }

    static int spanOneBack(final UnicodeSet set, CharSequence s, int length) {
        char c = s.charAt(length - 1);
        if (c >= 0xdc00 && c <= 0xdfff && length >= 2) {
            char c2 = s.charAt(length - 2);
            if (UTF16.isLeadSurrogate(c2)) {
                int supplementary = UCharacterProperty.getRawSupplementary(c2, c);
                return set.contains(supplementary) ? 2 : -2;
            }
        }
        return set.contains(c) ? 1 : -1;
    }

    /**
     * Helper class for UnicodeSetStringSpan.
     *
     * <p>List of offsets from the current position from where to try matching
     * a code point or a string.
     * Stores offsets rather than indexes to simplify the code and use the same list
     * for both increments (in span()) and decrements (in spanBack()).
     *
     * <p>Assumption: The maximum offset is limited, and the offsets that are stored at any one time
     * are relatively dense, that is,
     * there are normally no gaps of hundreds or thousands of offset values.
     *
     * <p>This class optionally also tracks the minimum non-negative count for each position,
     * intended to count the smallest number of elements of any path leading to that position.
     *
     * <p>The implementation uses a circular buffer of count integers,
     * each indicating whether the corresponding offset is in the list,
     * and its path element count.
     * This avoids inserting into a sorted list of offsets (or absolute indexes)
     * and physically moving part of the list.
     *
     * <p>Note: In principle, the caller should setMaxLength() to
     * the maximum of the max string length and U16_LENGTH/U8_LENGTH
     * to account for "long" single code points.
     *
     * <p>Note: An earlier version did not track counts and stored only byte flags.
     * With boolean flags, if maxLength were guaranteed to be no more than 32 or 64,
     * the list could be stored as bit flags in a single integer.
     * Rather than handling a circular buffer with a start list index,
     * the integer would simply be shifted when lower offsets are removed.
     * UnicodeSet does not have a limit on the lengths of strings.
     */
    private static final class OffsetList {
        private int[] list;
        private int length;
        private int start;

        public OffsetList() {
            list = new int[16];  // default size
        }

        public void setMaxLength(int maxLength) {
            if (maxLength > list.length) {
                list = new int[maxLength];
            }
            clear();
        }

        public void clear() {
            for (int i = list.length; i-- > 0;) {
                list[i] = 0;
            }
            start = length = 0;
        }

        public boolean isEmpty() {
            return (length == 0);
        }

        /**
         * Reduces all stored offsets by delta, used when the current position moves by delta.
         * There must not be any offsets lower than delta.
         * If there is an offset equal to delta, it is removed.
         *
         * @param delta [1..maxLength]
         */
        public void shift(int delta) {
            int i = start + delta;
            if (i >= list.length) {
                i -= list.length;
            }
            if (list[i] != 0) {
                list[i] = 0;
                --length;
            }
            start = i;
        }

        /**
         * Adds an offset. The list must not contain it yet.
         * @param offset [1..maxLength]
         */
        public void addOffset(int offset) {
            int i = start + offset;
            if (i >= list.length) {
                i -= list.length;
            }
            assert list[i] == 0;
            list[i] = 1;
            ++length;
        }

        /**
         * Adds an offset and updates its count.
         * The list may already contain the offset.
         * @param offset [1..maxLength]
         */
        public void addOffsetAndCount(int offset, int count) {
            assert count > 0;
            int i = start + offset;
            if (i >= list.length) {
                i -= list.length;
            }
            if (list[i] == 0) {
                list[i] = count;
                ++length;
            } else if (count < list[i]) {
                list[i] = count;
            }
        }

        /**
         * @param offset [1..maxLength]
         */
        public boolean containsOffset(int offset) {
            int i = start + offset;
            if (i >= list.length) {
                i -= list.length;
            }
            return list[i] != 0;
        }

        /**
         * @param offset [1..maxLength]
         */
        public boolean hasCountAtOffset(int offset, int count) {
            int i = start + offset;
            if (i >= list.length) {
                i -= list.length;
            }
            int oldCount = list[i];
            return oldCount != 0 && oldCount <= count;
        }

        /**
         * Finds the lowest stored offset from a non-empty list, removes it,
         * and reduces all other offsets by this minimum.
         * @return min=[1..maxLength]
         */
        public int popMinimum(OutputInt outCount) {
            // Look for the next offset in list[start+1..list.length-1].
            int i = start, result;
            while (++i < list.length) {
                int count = list[i];
                if (count != 0) {
                    list[i] = 0;
                    --length;
                    result = i - start;
                    start = i;
                    if (outCount != null) { outCount.value = count; }
                    return result;
                }
            }
            // i==list.length

            // Wrap around and look for the next offset in list[0..start].
            // Since the list is not empty, there will be one.
            result = list.length - start;
            i = 0;
            int count;
            while ((count = list[i]) == 0) {
                ++i;
            }
            list[i] = 0;
            --length;
            start = i;
            if (outCount != null) { outCount.value = count; }
            return result + i;
        }
    }
}
