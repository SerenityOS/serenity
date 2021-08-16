/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * A {@code CharSequence} is a readable sequence of {@code char} values. This
 * interface provides uniform, read-only access to many different kinds of
 * {@code char} sequences.
 * A {@code char} value represents a character in the <i>Basic
 * Multilingual Plane (BMP)</i> or a surrogate. Refer to <a
 * href="Character.html#unicode">Unicode Character Representation</a> for details.
 *
 * <p> This interface does not refine the general contracts of the {@link
 * java.lang.Object#equals(java.lang.Object) equals} and {@link
 * java.lang.Object#hashCode() hashCode} methods. The result of testing two objects
 * that implement {@code CharSequence} for equality is therefore, in general, undefined.
 * Each object may be implemented by a different class, and there
 * is no guarantee that each class will be capable of testing its instances
 * for equality with those of the other.  It is therefore inappropriate to use
 * arbitrary {@code CharSequence} instances as elements in a set or as keys in
 * a map. </p>
 *
 * @author Mike McCloskey
 * @since 1.4
 */

public interface CharSequence {

    /**
     * Returns the length of this character sequence.  The length is the number
     * of 16-bit {@code char}s in the sequence.
     *
     * @return  the number of {@code char}s in this sequence
     */
    int length();

    /**
     * Returns the {@code char} value at the specified index.  An index ranges from zero
     * to {@code length() - 1}.  The first {@code char} value of the sequence is at
     * index zero, the next at index one, and so on, as for array
     * indexing.
     *
     * <p>If the {@code char} value specified by the index is a
     * <a href="{@docRoot}/java.base/java/lang/Character.html#unicode">surrogate</a>, the surrogate
     * value is returned.
     *
     * @param   index   the index of the {@code char} value to be returned
     *
     * @return  the specified {@code char} value
     *
     * @throws  IndexOutOfBoundsException
     *          if the {@code index} argument is negative or not less than
     *          {@code length()}
     */
    char charAt(int index);

    /**
     * Returns {@code true} if this character sequence is empty.
     *
     * @implSpec
     * The default implementation returns the result of calling {@code length() == 0}.
     *
     * @return {@code true} if {@link #length()} is {@code 0}, otherwise
     * {@code false}
     *
     * @since 15
     */
    default boolean isEmpty() {
        return this.length() == 0;
    }

    /**
     * Returns a {@code CharSequence} that is a subsequence of this sequence.
     * The subsequence starts with the {@code char} value at the specified index and
     * ends with the {@code char} value at index {@code end - 1}.  The length
     * (in {@code char}s) of the
     * returned sequence is {@code end - start}, so if {@code start == end}
     * then an empty sequence is returned.
     *
     * @param   start   the start index, inclusive
     * @param   end     the end index, exclusive
     *
     * @return  the specified subsequence
     *
     * @throws  IndexOutOfBoundsException
     *          if {@code start} or {@code end} are negative,
     *          if {@code end} is greater than {@code length()},
     *          or if {@code start} is greater than {@code end}
     */
    CharSequence subSequence(int start, int end);

    /**
     * Returns a string containing the characters in this sequence in the same
     * order as this sequence.  The length of the string will be the length of
     * this sequence.
     *
     * @return  a string consisting of exactly this sequence of characters
     */
    public String toString();

    /**
     * Returns a stream of {@code int} zero-extending the {@code char} values
     * from this sequence.  Any char which maps to a <a
     * href="{@docRoot}/java.base/java/lang/Character.html#unicode">surrogate code
     * point</a> is passed through uninterpreted.
     *
     * <p>The stream binds to this sequence when the terminal stream operation
     * commences (specifically, for mutable sequences the spliterator for the
     * stream is <a href="../util/Spliterator.html#binding"><em>late-binding</em></a>).
     * If the sequence is modified during that operation then the result is
     * undefined.
     *
     * @return an IntStream of char values from this sequence
     * @since 1.8
     */
    public default IntStream chars() {
        class CharIterator implements PrimitiveIterator.OfInt {
            int cur = 0;

            public boolean hasNext() {
                return cur < length();
            }

            public int nextInt() {
                if (hasNext()) {
                    return charAt(cur++);
                } else {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public void forEachRemaining(IntConsumer block) {
                for (; cur < length(); cur++) {
                    block.accept(charAt(cur));
                }
            }
        }

        return StreamSupport.intStream(() ->
                Spliterators.spliterator(
                        new CharIterator(),
                        length(),
                        Spliterator.ORDERED),
                Spliterator.SUBSIZED | Spliterator.SIZED | Spliterator.ORDERED,
                false);
    }

    /**
     * Returns a stream of code point values from this sequence.  Any surrogate
     * pairs encountered in the sequence are combined as if by {@linkplain
     * Character#toCodePoint Character.toCodePoint} and the result is passed
     * to the stream. Any other code units, including ordinary BMP characters,
     * unpaired surrogates, and undefined code units, are zero-extended to
     * {@code int} values which are then passed to the stream.
     *
     * <p>The stream binds to this sequence when the terminal stream operation
     * commences (specifically, for mutable sequences the spliterator for the
     * stream is <a href="../util/Spliterator.html#binding"><em>late-binding</em></a>).
     * If the sequence is modified during that operation then the result is
     * undefined.
     *
     * @return an IntStream of Unicode code points from this sequence
     * @since 1.8
     */
    public default IntStream codePoints() {
        class CodePointIterator implements PrimitiveIterator.OfInt {
            int cur = 0;

            @Override
            public void forEachRemaining(IntConsumer block) {
                final int length = length();
                int i = cur;
                try {
                    while (i < length) {
                        char c1 = charAt(i++);
                        if (!Character.isHighSurrogate(c1) || i >= length) {
                            block.accept(c1);
                        } else {
                            char c2 = charAt(i);
                            if (Character.isLowSurrogate(c2)) {
                                i++;
                                block.accept(Character.toCodePoint(c1, c2));
                            } else {
                                block.accept(c1);
                            }
                        }
                    }
                } finally {
                    cur = i;
                }
            }

            public boolean hasNext() {
                return cur < length();
            }

            public int nextInt() {
                final int length = length();

                if (cur >= length) {
                    throw new NoSuchElementException();
                }
                char c1 = charAt(cur++);
                if (Character.isHighSurrogate(c1) && cur < length) {
                    char c2 = charAt(cur);
                    if (Character.isLowSurrogate(c2)) {
                        cur++;
                        return Character.toCodePoint(c1, c2);
                    }
                }
                return c1;
            }
        }

        return StreamSupport.intStream(() ->
                Spliterators.spliteratorUnknownSize(
                        new CodePointIterator(),
                        Spliterator.ORDERED),
                Spliterator.ORDERED,
                false);
    }

    /**
     * Compares two {@code CharSequence} instances lexicographically. Returns a
     * negative value, zero, or a positive value if the first sequence is lexicographically
     * less than, equal to, or greater than the second, respectively.
     *
     * <p>
     * The lexicographical ordering of {@code CharSequence} is defined as follows.
     * Consider a {@code CharSequence} <i>cs</i> of length <i>len</i> to be a
     * sequence of char values, <i>cs[0]</i> to <i>cs[len-1]</i>. Suppose <i>k</i>
     * is the lowest index at which the corresponding char values from each sequence
     * differ. The lexicographic ordering of the sequences is determined by a numeric
     * comparison of the char values <i>cs1[k]</i> with <i>cs2[k]</i>. If there is
     * no such index <i>k</i>, the shorter sequence is considered lexicographically
     * less than the other. If the sequences have the same length, the sequences are
     * considered lexicographically equal.
     *
     *
     * @param cs1 the first {@code CharSequence}
     * @param cs2 the second {@code CharSequence}
     *
     * @return  the value {@code 0} if the two {@code CharSequence} are equal;
     *          a negative integer if the first {@code CharSequence}
     *          is lexicographically less than the second; or a
     *          positive integer if the first {@code CharSequence} is
     *          lexicographically greater than the second.
     *
     * @since 11
     */
    @SuppressWarnings("unchecked")
    public static int compare(CharSequence cs1, CharSequence cs2) {
        if (Objects.requireNonNull(cs1) == Objects.requireNonNull(cs2)) {
            return 0;
        }

        if (cs1.getClass() == cs2.getClass() && cs1 instanceof Comparable) {
            return ((Comparable<Object>) cs1).compareTo(cs2);
        }

        for (int i = 0, len = Math.min(cs1.length(), cs2.length()); i < len; i++) {
            char a = cs1.charAt(i);
            char b = cs2.charAt(i);
            if (a != b) {
                return a - b;
            }
        }

        return cs1.length() - cs2.length();
    }

}
