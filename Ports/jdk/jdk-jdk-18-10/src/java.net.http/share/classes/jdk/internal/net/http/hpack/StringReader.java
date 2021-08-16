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

import java.io.IOException;
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
final class StringReader {

    private static final int NEW             = 0;
    private static final int FIRST_BYTE_READ = 1;
    private static final int LENGTH_READ     = 2;
    private static final int DONE            = 4;

    private final IntegerReader intReader = new IntegerReader();
    private final Huffman.Reader huffmanReader = new QuickHuffman.Reader();
    private final ISO_8859_1.Reader plainReader = new ISO_8859_1.Reader();

    private int state = NEW;

    private boolean huffman;
    private int remainingLength;

    boolean read(ByteBuffer input, Appendable output) throws IOException {
        if (state == DONE) {
            return true;
        }
        if (!input.hasRemaining()) {
            return false;
        }
        if (state == NEW) {
            int p = input.position();
            huffman = (input.get(p) & 0b10000000) != 0;
            state = FIRST_BYTE_READ;
            intReader.configure(7);
        }
        if (state == FIRST_BYTE_READ) {
            boolean lengthRead = intReader.read(input);
            if (!lengthRead) {
                return false;
            }
            remainingLength = intReader.get();
            state = LENGTH_READ;
        }
        if (state == LENGTH_READ) {
            boolean isLast = input.remaining() >= remainingLength;
            int oldLimit = input.limit();
            if (isLast) {
                input.limit(input.position() + remainingLength);
            }
            remainingLength -= Math.min(input.remaining(), remainingLength);
            if (huffman) {
                huffmanReader.read(input, output, isLast);
            } else {
                plainReader.read(input, output);
            }
            if (isLast) {
                input.limit(oldLimit);
                state = DONE;
            }
            return isLast;
        }
        throw new InternalError(Arrays.toString(
                new Object[]{state, huffman, remainingLength}));
    }

    boolean isHuffmanEncoded() {
        if (state < FIRST_BYTE_READ) {
            throw new IllegalStateException("Has not been fully read yet");
        }
        return huffman;
    }

    void reset() {
        if (huffman) {
            huffmanReader.reset();
        } else {
            plainReader.reset();
        }
        intReader.reset();
        state = NEW;
    }
}
