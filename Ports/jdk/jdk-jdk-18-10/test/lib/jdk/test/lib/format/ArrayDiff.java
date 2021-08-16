/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package jdk.test.lib.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A difference between two arrays, which can be pretty formatted.
 * For the calculated difference, user can request if the two arrays
 * are equal (in terms of {@link Object#equals Object.equals()} for their
 * elements). For the arrays that differ, a human-readable difference can
 * be provided.
 *
 * <p>The difference is represented as a four-line text block, comprising of the
 * first different element index, arrays printouts in the difference area,
 * and a difference mark. For Primitive and Object elements in the source
 * arrays their C-style escaped {@link String#valueOf String.valueOf()} are
 * printed, element in String[] arrays are additionally surrounded with quotes.
 * Additional formatting parameters, like maximum allowed width and number of
 * elements printed before difference, can be specified.
 *
 * <p>Output examples:
 *
 * <p> two int arrays: </p>
 * <pre>
 * Arrays differ starting from [index: 4]:
 *         ... 3, 4,   5, 6, 7]
 *         ... 3, 4, 225, 6, 7]
 *                  ^^^^
 * </pre>
 * <p> two String arrays: </p>
 * <pre>
 * Arrays differ starting from [index: 2]:
 *         ["first", "second",     "third", "u\nprintable"]
 *         ["first", "second", "incorrect", "u\nprintable"]
 *                            ^^^^^^^^^^^^
 * </pre>
 * <p> two char arrays arrays: </p>
 * <pre>
 * Arrays differ starting from [index: 7]:
 *         ... &#92;u0001, &#92;u0002, &#92;u0007, a, b, \n, ...
 *         ... &#92;u0001, &#92;u0002,      }, a, b, \n, ...
 *                            ^^^^^^^
 * </pre>
 */
public class ArrayDiff<E> implements Diff {

    private int failureIdx;
    private final int maxWidth;
    private final int contextBefore;

    private final ArrayCodec<E> first;
    private final ArrayCodec<E> second;

    private ArrayDiff(ArrayCodec<E> first, ArrayCodec<E> second,
                      int width, int getContextBefore) {
        this.first = first;
        this.second = second;
        this.maxWidth = width;
        this.contextBefore = getContextBefore;
        failureIdx = first.findMismatchIndex(second);
    }

    /**
     * Creates an ArrayDiff fom two arrays and default limits. The given arguments must be of the same
     * component type.
     *
     * @param first the first array
     * @param second the second array
     * @return an ArrayDiff instance for the two arrays
     */
    public static ArrayDiff of(Object first, Object second) {
        return ArrayDiff.of(first, second, Diff.Defaults.WIDTH, Diff.Defaults.CONTEXT_BEFORE);
    }

    /**
     * Creates an ArrayDiff fom two arrays with the given limits. The given arguments must be of the same
     * component type.
     *
     * @param first the first array
     * @param second the second array
     * @param width the maximum allowed width in characters for the formatting
     * @param contextBefore maximum number of elements to print before those that differ
     * @throws IllegalArgumentException if component types of arrays is not supported or are not the same
     * @throws NullPointerException if at least one of the arrays is null
     * @return an ArrayDiff instance for the two arrays and formatting parameters provided
     */
    public static ArrayDiff of(Object first, Object second, int width, int contextBefore) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);

        boolean bothAreArrays = first.getClass().isArray() && second.getClass().isArray();
        boolean componentTypesAreSame =
            first.getClass().getComponentType() == second.getClass().getComponentType();

        if (!bothAreArrays || !componentTypesAreSame) {
            throw new IllegalArgumentException("Both arguments should be arrays of the same type");
        }

        return new ArrayDiff(
                ArrayCodec.of(first),
                ArrayCodec.of(second),
                width, contextBefore);
    }

    /**
     * Formats the given diff.
     *
     * @return formatted difference representation.
     */
    @Override
    public String format() {
        if (areEqual()) {
            return "";
        }

        return format(false)
                .orElseGet(() -> format(true).get());
    }

    /**
     * Indicates whether the two source arrays are equal
     *
     * @return {@code true} if the arrays are different, {@code false} otherwise
     */
    @Override
    public boolean areEqual() {
        return first.equals(second);
    }

    private void extractAndAlignElements() {
        first.formatNext();
        second.formatNext();

        first.alignBy(second);
        second.alignBy(first);
    }

    private static String failureMarkForWidth(int width) {
        return new String("^").repeat(width);
    }

    private Optional<String> format(boolean bounded) {
        int idx = bounded ? Math.max(0, failureIdx - contextBefore) : 0;

        first.startFormatting(idx, bounded ? maxWidth : -1);
        second.startFormatting(idx, bounded ? maxWidth : -1);
        StringBuilder failureMark = new StringBuilder(
                Format.paddingForWidth(first.getEncodedLength()));

        for (; !(first.isExhausted() && second.isExhausted()); idx++) {
            extractAndAlignElements();

            first.appendFormatted();
            second.appendFormatted();

            { // Process failure mark
                if (idx < failureIdx) {
                    failureMark.append(Format.paddingForWidth(first.getElementLength()));
                } else  if (idx == failureIdx) {
                    int markLength = Math.max(first.getElementLength(), second.getElementLength()) - 1;
                    failureMark.append(failureMarkForWidth(markLength));
                }
            }

            final int maxEncodedLength = Math.max(
                    first.getEncodedLength(),
                    second.getEncodedLength());
            if (!bounded && maxEncodedLength > maxWidth) {
                return Optional.empty();
            }
        }

        return Optional.of(String.format(
                "Arrays differ starting from [index: %d]:%n%s%n%s%n%s",
                failureIdx,
                first.getEncoded(),
                second.getEncoded(),
                failureMark.toString()));
    }

}

