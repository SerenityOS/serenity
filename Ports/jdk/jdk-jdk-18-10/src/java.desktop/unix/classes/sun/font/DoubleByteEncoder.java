/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.font;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import sun.nio.cs.Surrogate;

public abstract class DoubleByteEncoder
    extends CharsetEncoder
{

    private short[] index1;
    private String[] index2;

    private final Surrogate.Parser sgp = new Surrogate.Parser();

    protected DoubleByteEncoder(Charset cs,
                                short[] index1, String[] index2)
    {
        super(cs, 2.0f, 2.0f);
        this.index1 = index1;
        this.index2 = index2;
    }

    protected DoubleByteEncoder(Charset cs,
                                short[] index1, String[] index2,
                                float avg, float max)
    {
        super(cs, avg, max);
        this.index1 = index1;
        this.index2 = index2;
    }

    protected DoubleByteEncoder(Charset cs,
                                short[] index1, String[] index2, byte[] repl)
    {
        super(cs, 2.0f, 2.0f, repl);
        this.index1 = index1;
        this.index2 = index2;
    }


    protected DoubleByteEncoder(Charset cs,
                                short[] index1, String[] index2,
                                byte[] repl, float avg, float max)
    {
        super(cs, avg, max,repl);
        this.index1 = index1;
        this.index2 = index2;
    }

    public boolean canEncode(char c) {
        return (encodeSingle(c) != -1 ||
                encodeDouble(c) != 0);
    }

    private CoderResult encodeArrayLoop(CharBuffer src, ByteBuffer dst) {
        char[] sa = src.array();
        int sp = src.arrayOffset() + src.position();
        int sl = src.arrayOffset() + src.limit();
        byte[] da = dst.array();
        int dp = dst.arrayOffset() + dst.position();
        int dl = dst.arrayOffset() + dst.limit();

        try {
            while (sp < sl) {
                char c = sa[sp];
                if (Character.isSurrogate(c)) {
                    if (sgp.parse(c, sa, sp, sl) < 0)
                        return sgp.error();
                    if (sl - sp < 2)
                        return CoderResult.UNDERFLOW;
                    char c2 = sa[sp + 1];

                    byte[] outputBytes = new byte[2];
                    outputBytes = encodeSurrogate(c, c2);

                    if (outputBytes == null) {
                        return sgp.unmappableResult();
                    }
                    else {
                        if (dl - dp < 2)
                            return CoderResult.OVERFLOW;
                        da[dp++] = outputBytes[0];
                        da[dp++] = outputBytes[1];
                        sp += 2;
                        continue;
                    }
                }
                if (c >= '\uFFFE')
                    return CoderResult.unmappableForLength(1);

                int b = encodeSingle(c);
                if (b != -1) { // Single Byte
                    if (dl - dp < 1)
                        return CoderResult.OVERFLOW;
                    da[dp++] = (byte)b;
                    sp++;
                    continue;
                }

                int ncode  = encodeDouble(c);
                if (ncode != 0 && c != '\u0000' ) {
                    if (dl - dp < 2)
                        return CoderResult.OVERFLOW;
                    da[dp++] = (byte) ((ncode & 0xff00) >> 8);
                    da[dp++] = (byte) (ncode & 0xff);
                    sp++;
                    continue;
                }
                return CoderResult.unmappableForLength(1);
                }
            return CoderResult.UNDERFLOW;
        } finally {
            src.position(sp - src.arrayOffset());
            dst.position(dp - dst.arrayOffset());
        }
    }

    private CoderResult encodeBufferLoop(CharBuffer src, ByteBuffer dst) {
        int mark = src.position();

        try {
            while (src.hasRemaining()) {
                char c = src.get();
                if (Character.isSurrogate(c)) {
                    int surr;
                    if ((surr = sgp.parse(c, src)) < 0)
                        return sgp.error();
                    char c2 = Surrogate.low(surr);
                    byte[] outputBytes = new byte[2];
                    outputBytes = encodeSurrogate(c, c2);

                    if (outputBytes == null) {
                        return sgp.unmappableResult();
                    } else {
                        if (dst.remaining() < 2)
                            return CoderResult.OVERFLOW;
                        mark += 2;
                        dst.put(outputBytes[0]);
                        dst.put(outputBytes[1]);
                        continue;
                    }
                }
                if (c >= '\uFFFE')
                    return CoderResult.unmappableForLength(1);
                int b = encodeSingle(c);

                if (b != -1) { // Single-byte character
                    if (dst.remaining() < 1)
                        return CoderResult.OVERFLOW;
                    mark++;
                    dst.put((byte)b);
                    continue;
                }
                // Double Byte character

                int ncode = encodeDouble(c);
                if (ncode != 0 && c != '\u0000') {
                    if (dst.remaining() < 2)
                        return CoderResult.OVERFLOW;
                    mark++;
                    dst.put((byte) ((ncode & 0xff00) >> 8));
                    dst.put((byte) ncode);
                    continue;
                }
                return CoderResult.unmappableForLength(1);
            }

            return CoderResult.UNDERFLOW;
        } finally {
            src.position(mark);
        }
    }

    protected CoderResult encodeLoop(CharBuffer src, ByteBuffer dst) {
        if (true && src.hasArray() && dst.hasArray())
            return encodeArrayLoop(src, dst);
        else
            return encodeBufferLoop(src, dst);
    }

    /*
     * Can be changed by subclass
     */
    protected int encodeDouble(char ch) {
        int offset = index1[((ch & 0xff00) >> 8 )] << 8;
        return index2[offset >> 12].charAt((offset & 0xfff) + (ch & 0xff));
    }

    /*
     * Can be changed by subclass
     */
    protected int encodeSingle(char inputChar) {
        if (inputChar < 0x80)
            return (byte)inputChar;
        else
            return -1;
    }

    /**
     *  Protected method which should be overridden by concrete DBCS
     *  CharsetEncoder classes which included supplementary characters
     *  within their mapping coverage.
     *  null return value indicates surrogate values could not be
     *  handled or encoded.
     */
    protected byte[] encodeSurrogate(char highSurrogate, char lowSurrogate) {
        return null;
    }
}
