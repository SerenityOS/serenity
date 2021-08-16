/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.net.http.hpack;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Huffman coding.
 *
 * @since 9
 */
public final class Huffman {

    public interface Reader {

        void read(ByteBuffer source,
                  Appendable destination,
                  boolean isLast) throws IOException;

        /**
         * Brings this reader to the state it had upon construction.
         */
        void reset();
    }

    public interface Writer {

        Writer from(CharSequence input, int start, int end);

        boolean write(ByteBuffer destination);

        /**
         * Brings this writer to the state it had upon construction.
         *
         * @return this writer
         */
        Writer reset();

        /**
         * Calculates the number of bytes required to represent a subsequence of
         * the given {@code CharSequence} using the Huffman coding.
         *
         * @param value
         *         characters
         * @param start
         *         the start index, inclusive
         * @param end
         *         the end index, exclusive
         *
         * @return number of bytes
         * @throws NullPointerException
         *         if the value is null
         * @throws IndexOutOfBoundsException
         *         if any invocation of {@code value.charAt(i)}, where
         *         {@code start <= i < end}, throws an IndexOutOfBoundsException
         */
        int lengthOf(CharSequence value, int start, int end);

        default int lengthOf(CharSequence value) {
            return lengthOf(value, 0, value.length());
        }
    }
}
