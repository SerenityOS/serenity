/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
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
 */

/**
 * Simple EUC-like decoder used by IBM01383 and IBM970
 * supports G1 - no support for G2 or G3
 */


import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

abstract class SimpleEUCDecoder
    extends CharsetDecoder
{
    private final int SS2 =  0x8E;
    private final int SS3 =  0x8F;

    protected static String  mappingTableG1;
    protected static String  byteToCharTable;

    protected SimpleEUCDecoder(Charset cs) {
        super(cs, 0.5f, 1.0f);
    }

    private CoderResult decodeArrayLoop(ByteBuffer src, CharBuffer dst) {
        byte[] sa = src.array();
        int sp = src.arrayOffset() + src.position();
        int sl = src.arrayOffset() + src.limit();
        assert (sp <= sl);
        sp = (sp <= sl ? sp : sl);
        char[] da = dst.array();
        int dp = dst.arrayOffset() + dst.position();
        int dl = dst.arrayOffset() + dst.limit();
        assert (dp <= dl);
        dp = (dp <= dl ? dp : dl);

        try {
            while (sp < sl) {
                int byte1, byte2;
                int inputSize = 1;
                char outputChar = '\uFFFD';

                byte1 = sa[sp] & 0xff;

                if ( byte1 <= 0x9f ) {  // < 0x9f has its own table (G0)
                    if (byte1 == SS2 || byte1 == SS3 ) {
                        // No support provided for G2/G3 at this time.
                        return CoderResult.malformedForLength(1);
                    }
                    outputChar = byteToCharTable.charAt(byte1);
                } else if (byte1 < 0xa1 || byte1 > 0xfe) {  // invalid range?
                    return CoderResult.malformedForLength(1);
                } else {                                        // (G1)
                    if (sl - sp < 2) {
                        return CoderResult.UNDERFLOW;
                    }
                    byte2 = sa[sp + 1] & 0xff;
                    inputSize++;
                    if ( byte2 < 0xa1 || byte2 > 0xfe) {
                        return CoderResult.malformedForLength(2);
                    }
                    outputChar = mappingTableG1.charAt(((byte1 - 0xa1) * 94) + byte2 - 0xa1);
                }
                if  (outputChar == '\uFFFD') {
                    return CoderResult.unmappableForLength(inputSize);
                }
                if (dl - dp < 1)
                    return CoderResult.OVERFLOW;
                da[dp++] = outputChar;
                sp += inputSize;
            }
            return CoderResult.UNDERFLOW;
        } finally {
            src.position(sp - src.arrayOffset());
            dst.position(dp - dst.arrayOffset());
        }
    }

    private CoderResult decodeBufferLoop(ByteBuffer src, CharBuffer dst) {
        int mark = src.position();

        try {
            while (src.hasRemaining()) {
                char outputChar = '\uFFFD';
                int inputSize = 1;
                int byte1, byte2;

                byte1 = src.get() & 0xff;
                if ( byte1 <= 0x9f ) {
                    if (byte1 == SS2 || byte1 == SS3 ) {
                        return CoderResult.malformedForLength(1);
                    }
                    outputChar = byteToCharTable.charAt(byte1);
                } else if (byte1 < 0xa1 || byte1 > 0xfe) {
                    return CoderResult.malformedForLength(1);
                } else {
                    if (!src.hasRemaining()) {
                        return CoderResult.UNDERFLOW;
                    }
                    byte2 = src.get() & 0xff;
                    inputSize++;
                    if ( byte2 < 0xa1 || byte2 > 0xfe) {
                        return CoderResult.malformedForLength(2);
                    }
                    outputChar = mappingTableG1.charAt(((byte1 - 0xa1) * 94) + byte2 - 0xa1);
                }
                if (outputChar == '\uFFFD') {
                    return CoderResult.unmappableForLength(inputSize);
                }
                if (!dst.hasRemaining())
                    return CoderResult.OVERFLOW;
                    dst.put(outputChar);
                mark += inputSize;
            }
            return CoderResult.UNDERFLOW;
        } finally {
            src.position(mark);
        }
    }

    protected CoderResult decodeLoop(ByteBuffer src, CharBuffer dst) {
        if (src.hasArray() && dst.hasArray())
            return decodeArrayLoop(src, dst);
        else
            return decodeBufferLoop(src, dst);
    }
}
