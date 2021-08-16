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
import java.util.Optional;

/**
 * A codec helping representing arrays in a string form.
 *
 * Encoding can be done in a controllable fashion (allowing the user to encode two
 * or more arrays in a time) or as a single operation.
 */
public class ArrayCodec<E> {
    private static final String ELLIPSIS = "...";

    private boolean exhausted;
    private StringBuilder encoded;

    private List<E> source;
    private String element;

    private boolean bounded = false;
    private int maxWidth;
    private int idx;

    private ArrayCodec(List<E> source) {
        this.source = source;
    }

    /**
     * Creates a codec for a char array
     *
     * @param array source array
     * @return an ArrayCodec for the provided array
     */
    public static ArrayCodec<Character> of(char[] array) {
        var source = new ArrayList<Character>(array.length);
        for (char value: array) {
            source.add(value);
        }
        return new ArrayCodec<>(source);
    }

    /**
     * Creates a codec for a byte array
     *
     * @param array source array
     * @return an ArrayCodec for the provided array
     */
    public static ArrayCodec<Byte> of(byte[] array) {
        var source = new ArrayList<Byte>(array.length);
        for (byte value: array) {
            source.add(value);
        }
        return new ArrayCodec<>(source);
    }

    /**
     * Creates a codec for an int array
     *
     * @param array source array
     * @return an ArrayCodec for the provided array
     */
    public static ArrayCodec<Integer> of(int[] array) {
        var source = new ArrayList<Integer>(array.length);
        for (int value: array) {
            source.add(value);
        }
        return new ArrayCodec<>(source);
    }

    /**
     * Creates a codec for a long array
     *
     * @param array source array
     * @return an ArrayCodec for the provided array
     */
    public static ArrayCodec<Long> of(long[] array) {
        var source = new ArrayList<Long>(array.length);
        for (long value: array) {
            source.add(value);
        }
        return new ArrayCodec<>(source);
    }

    /**
     * Creates a codec for a String array
     *
     * @param array source array
     * @return an ArrayCodec for the provided array
     */
    public static ArrayCodec<String> of(String[] array) {
        var source = new ArrayList<String>(array.length);
        for (String value: array) {
            source.add(value);
        }
        return new ArrayCodec<>(source);
    }

    /**
     * Creates a codec for a generic Object array
     *
     * @param array source array
     * @return an ArrayCodec for the provided array
     */
    public static ArrayCodec<Object> of(Object[] array) {
        var source = new ArrayList<Object>(array.length);
        for (Object value: array) {
            source.add(value);
        }
        return new ArrayCodec<Object>(source);
    }

    /**
     * Creates a codec for a generic array, trying to recognize its component type
     *
     * @param array source array
     * @throws IllegalArgumentException if {@code array}'s component type is not supported
     * @return an ArrayCodec for the provided array
     */
    public static ArrayCodec of(Object array) {
        var type = array.getClass().getComponentType();
        if (type == byte.class) {
            return ArrayCodec.of((byte[])array);
        } else if (type == int.class) {
            return ArrayCodec.of((int[])array);
        } else if (type == long.class) {
            return ArrayCodec.of((long[])array);
        } else if (type == char.class) {
            return ArrayCodec.of((char[])array);
        } else if (type == String.class) {
            return ArrayCodec.of((String[])array);
        } else if (!type.isPrimitive() && !type.isArray()) {
            return ArrayCodec.of((Object[])array);
        }

        throw new IllegalArgumentException("Unsupported array component type: " + type);
    }

    /**
     * Formats an array at-once.
     * The array is enclosed in brackets, its elements are separated with
     * commas. String elements are additionally surrounded by double quotes.
     * Unprintable symbols are C-stye escaped.
     *
     * <p>Sample outputs:
     *
     * <pre>
     *   [0, 1, 2, 3, 4]
     *   ["one", "first", "tree"]
     *   [object1, object2, object3]
     *   [a, b, \n, &#92;u0002/, c]
     * </pre>
     *
     * @throws IllegalArgumentException if {@code array}'s component type is not supported
     * @return an ArrayCodec for the provided array
     */
    public static String format(Object array) {
        var codec = ArrayCodec.of(array);
        codec.startFormatting(0, -1);
        while (!codec.isExhausted()) {
            codec.formatNext();
            codec.appendFormatted();
        }
        return codec.getEncoded();
    }

    /**
     * Starts formatting with the given parameters.
     *
     * @param startIdx first element's index to start formattig with
     * @param maxWidth maximum allowed formatting width (in characters).
     * @return an ArrayCodec for the provided array
     */
    public void startFormatting(int startIdx, int maxWidth) {
        encoded = new StringBuilder(startIdx == 0 ? "[" : ELLIPSIS);
        exhausted = false;
        this.maxWidth = maxWidth;
        bounded = (maxWidth > 0);
        idx = startIdx;
    }

    /**
     * Format next element, store it in the internal element storage.
     */
    public void formatNext() {
        int limit = source.size();

        String prefix = idx == 0 || idx >= limit ? "" : " ";
        String suffix = (idx + 1 == limit) || (source.isEmpty() && idx == 0)
                ? "]"
                : idx >= limit ? "" : ",";
        element = prefix +
                (idx >= limit ? "" : Format.asLiteral(source.get(idx)))
                + suffix;
    }

    /**
     * Append formatted element to internal StringBuilder.
     *
     * The formatted-so-far string can be accessed via {@link #getEncoded}
     * no elements in array left the method silently does nothing.
     */
    public void appendFormatted() {
        if (exhausted) {
            return;
        }

        boolean isLast = idx == source.size() - 1;
        if (isLast || source.isEmpty()) {
            exhausted = true;
        }

        if (bounded && encoded.length() + element.length() > maxWidth - ELLIPSIS.length()) {
            encoded.append(isLast ? element : " " + ELLIPSIS);
            exhausted = true;
        } else {
            encoded.append(element);
        }
        idx++;
    }

    /**
     * Aligns the element by another codec.
     *
     * If another codec's last encoded element string is longer than this
     * codec's, widens this codec's encoded element with spaces so the
     * two strings have the same length;
     *
     * @param another Another codec to compare encoded element width with
     */
    public void alignBy(ArrayCodec<E> another) {
        if (!element.equals("") && !element.equals("]")) {
            int delta = another.element.length() - element.length();
            if (delta > 0) {
                element = Format.paddingForWidth(delta) + element;
            }
        }
    }

    /**
     * Indicates if there are no elements left in the source array
     *
     * @return {@code true} if there are no elements left, {@code false} otherwise
     */
    public boolean isExhausted() {
        return exhausted;
    }

    /**
     * Returns the string encoded-so-far
     *
     * @return the string encoded-so-far
     */
    public String getEncoded() {
        return encoded.toString();
    }

    /**
     * Returns the length of the string encoded-so-far
     *
     * @return the length of the string encoded-so-far
     */
    public int getEncodedLength() {
        return encoded.length();
    }

    /**
     * Returns the length of the last encoded element
     *
     * @return the length of the last encoded element
     */
    public int getElementLength() {
        return element.length();
    }

    /**
     * Finds and returns the first mismatch index in another codec
     *
     * @param another a codec mismatch with whom is to be found
     * @return the first mismatched element's index or -1 if arrays are identical
     */
    public int findMismatchIndex(ArrayCodec<E> another) {
        int result = 0;
        while ((source.size() > result) && (another.source.size() > result)) {
            Object first = source.get(result);
            Object second = another.source.get(result);

            if (first == null || second == null) {
                if (first == null && second == null) {
                    continue;   // Both elements are null (i.e. equal)
                } else {
                    return result;  // Only one element is null, here's the failure index
                }
            }

            if (!first.equals(second)) {
                return result;
            }

            result++;
        }

        return source.size() != another.source.size()
            ? result    // Lengths are different, but the shorter arrays is a preffix to the longer array.
            : -1;       // Arrays are identical, there's no mismatch index
    }

    /**
     * Indicates whether source array for another codec is equal to this codec's array
     *
     * @return {@code true} if source arrays are equal, {@code false} otherwise
     */
    public boolean equals(ArrayCodec<E> another) {
        return source.equals(another.source);
    }
}
