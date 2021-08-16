/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.util.Arrays;

//
//          0   1   2   3   4   5   6   7
//        +---+---+---+---+---+---+---+---+
//        | H |    String Length (7+)     |
//        +---+---------------------------+
//        |  String Data (Length octets)  |
//        +-------------------------------+
//
// StringWriter does not require a notion of endOfInput (isLast) in 'write'
// methods due to the nature of string representation in HPACK. Namely, the
// length of the string is put before string's contents. Therefore the length is
// always known beforehand.
//
// Expected use:
//
//     configure write* (reset configure write*)*
//
final class StringWriter {

    private static final int NEW            = 0;
    private static final int CONFIGURED     = 1;
    private static final int LENGTH_WRITTEN = 2;
    private static final int DONE           = 4;

    private final IntegerWriter intWriter = new IntegerWriter();
    private final Huffman.Writer huffmanWriter = new QuickHuffman.Writer();
    private final ISO_8859_1.Writer plainWriter = new ISO_8859_1.Writer();

    private int state = NEW;
    private boolean huffman;

    StringWriter configure(CharSequence input, boolean huffman) {
        return configure(input, 0, input.length(), huffman);
    }

    StringWriter configure(CharSequence input,
                           int start,
                           int end,
                           boolean huffman) {
        if (start < 0 || end < 0 || end > input.length() || start > end) {
            throw new IndexOutOfBoundsException(
                    String.format("input.length()=%s, start=%s, end=%s",
                            input.length(), start, end));
        }
        if (!huffman) {
            plainWriter.configure(input, start, end);
            intWriter.configure(end - start, 7, 0b0000_0000);
        } else {
            huffmanWriter.from(input, start, end);
            intWriter.configure(huffmanWriter.lengthOf(input, start, end),
                    7, 0b1000_0000);
        }

        this.huffman = huffman;
        state = CONFIGURED;
        return this;
    }

    boolean write(ByteBuffer output) {
        if (state == DONE) {
            return true;
        }
        if (state == NEW) {
            throw new IllegalStateException("Configure first");
        }
        if (!output.hasRemaining()) {
            return false;
        }
        if (state == CONFIGURED) {
            if (intWriter.write(output)) {
                state = LENGTH_WRITTEN;
            } else {
                return false;
            }
        }
        if (state == LENGTH_WRITTEN) {
            boolean written = huffman
                    ? huffmanWriter.write(output)
                    : plainWriter.write(output);
            if (written) {
                state = DONE;
                return true;
            } else {
                return false;
            }
        }
        throw new InternalError(Arrays.toString(new Object[]{state, huffman}));
    }

    void reset() {
        intWriter.reset();
        if (huffman) {
            huffmanWriter.reset();
        } else {
            plainWriter.reset();
        }
        state = NEW;
    }
}
