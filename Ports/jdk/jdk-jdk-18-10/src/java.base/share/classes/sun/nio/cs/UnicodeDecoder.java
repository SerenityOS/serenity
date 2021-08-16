/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.cs;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.MalformedInputException;


abstract class UnicodeDecoder extends CharsetDecoder {

    protected static final char BYTE_ORDER_MARK = (char) 0xfeff;
    protected static final char REVERSED_MARK = (char) 0xfffe;

    protected static final int NONE = 0;
    protected static final int BIG = 1;
    protected static final int LITTLE = 2;

    private final int expectedByteOrder;
    private int currentByteOrder;
    private int defaultByteOrder = BIG;

    public UnicodeDecoder(Charset cs, int bo) {
        super(cs, 0.5f, 1.0f);
        expectedByteOrder = currentByteOrder = bo;
    }

    public UnicodeDecoder(Charset cs, int bo, int defaultBO) {
        this(cs, bo);
        defaultByteOrder = defaultBO;
    }

    private char decode(int b1, int b2) {
        if (currentByteOrder == BIG)
            return (char)((b1 << 8) | b2);
        else
            return (char)((b2 << 8) | b1);
    }

    protected CoderResult decodeLoop(ByteBuffer src, CharBuffer dst) {
        int mark = src.position();

        try {
            while (src.remaining() > 1) {
                int b1 = src.get() & 0xff;
                int b2 = src.get() & 0xff;

                // Byte Order Mark interpretation
                if (currentByteOrder == NONE) {
                    char c = (char)((b1 << 8) | b2);
                    if (c == BYTE_ORDER_MARK) {
                        currentByteOrder = BIG;
                        mark += 2;
                        continue;
                    } else if (c == REVERSED_MARK) {
                        currentByteOrder = LITTLE;
                        mark += 2;
                        continue;
                    } else {
                        currentByteOrder = defaultByteOrder;
                        // FALL THROUGH to process b1, b2 normally
                    }
                }

                char c = decode(b1, b2);

                // Surrogates
                if (Character.isSurrogate(c)) {
                    if (Character.isHighSurrogate(c)) {
                        if (src.remaining() < 2)
                            return CoderResult.UNDERFLOW;
                        char c2 = decode(src.get() & 0xff, src.get() & 0xff);
                        if (!Character.isLowSurrogate(c2))
                            return CoderResult.malformedForLength(4);
                        if (dst.remaining() < 2)
                            return CoderResult.OVERFLOW;
                        mark += 4;
                        dst.put(c);
                        dst.put(c2);
                        continue;
                    }
                    // Unpaired low surrogate
                    return CoderResult.malformedForLength(2);
                }

                if (!dst.hasRemaining())
                    return CoderResult.OVERFLOW;
                mark += 2;
                dst.put(c);

            }
            return CoderResult.UNDERFLOW;

        } finally {
            src.position(mark);
        }
    }

    protected void implReset() {
        currentByteOrder = expectedByteOrder;
    }

}
