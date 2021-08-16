/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.cs.ext;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import sun.nio.cs.DoubleByte;
import sun.nio.cs.HistoricallyNamedCharset;
import sun.nio.cs.US_ASCII;
import sun.nio.cs.*;

public class ISO2022_CN
    extends Charset
    implements HistoricallyNamedCharset
{
    private static final byte ISO_ESC = 0x1b;
    private static final byte ISO_SI = 0x0f;
    private static final byte ISO_SO = 0x0e;
    private static final byte ISO_SS2_7 = 0x4e;
    private static final byte ISO_SS3_7 = 0x4f;
    private static final byte MSB = (byte)0x80;
    private static final char REPLACE_CHAR = '\uFFFD';

    private static final byte SODesigGB = 0;
    private static final byte SODesigCNS = 1;

    public ISO2022_CN() {
        super("ISO-2022-CN", ExtendedCharsets.aliasesFor("ISO-2022-CN"));
    }

    public String historicalName() {
        return "ISO2022CN";
    }

    public boolean contains(Charset cs) {
        return ((cs instanceof EUC_CN)     // GB2312-80 repertoire
                || (cs instanceof US_ASCII)
                || (cs instanceof EUC_TW)  // CNS11643 repertoire
                || (cs instanceof ISO2022_CN));
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        throw new UnsupportedOperationException();
    }

    public boolean canEncode() {
        return false;
    }

    static class Decoder extends CharsetDecoder {
        private boolean shiftOut;
        private byte currentSODesig;

        private static final DoubleByte.Decoder GB2312 =
                (DoubleByte.Decoder)new EUC_CN().newDecoder();

        Decoder(Charset cs) {
            super(cs, 1.0f, 1.0f);
            shiftOut = false;
            currentSODesig = SODesigGB;
        }

        protected void implReset() {
            shiftOut= false;
            currentSODesig = SODesigGB;
        }

        private char cnsDecode(byte byte1, byte byte2, byte SS) {
            byte1 |= MSB;
            byte2 |= MSB;
            int p;
            if (SS == ISO_SS2_7)
                p = 1;    //plane 2, index -- 1
            else if (SS == ISO_SS3_7)
                p = 2;    //plane 3, index -- 2
            else
                return REPLACE_CHAR;  //never happen.
            return EUC_TW.Decoder.decodeSingleOrReplace(byte1 & 0xff,
                                                        byte2 & 0xff,
                                                        p,
                                                        REPLACE_CHAR);
        }

        private char SODecode(byte byte1, byte byte2, byte SOD) {
            byte1 |= MSB;
            byte2 |= MSB;
            if (SOD == SODesigGB) {
                return GB2312.decodeDouble(byte1 & 0xff,
                                           byte2 & 0xff);
            } else {    // SOD == SODesigCNS
                return EUC_TW.Decoder.decodeSingleOrReplace(byte1 & 0xff,
                                                            byte2 & 0xff,
                                                            0,
                                                            REPLACE_CHAR);
            }
        }

        private CoderResult decodeBufferLoop(ByteBuffer src,
                                             CharBuffer dst)
        {
            int mark = src.position();
            byte b1, b2, b3, b4;
            int inputSize;
            char c;
            try {
                while (src.hasRemaining()) {
                    b1 = src.get();
                    inputSize = 1;

                    while (b1 == ISO_ESC ||
                           b1 == ISO_SO ||
                           b1 == ISO_SI) {
                        if (b1 == ISO_ESC) {  // ESC
                            currentSODesig = SODesigGB;

                            if (src.remaining() < 1)
                                return CoderResult.UNDERFLOW;

                            b2 = src.get();
                            inputSize++;

                            if ((b2 & (byte)0x80) != 0)
                                return CoderResult.malformedForLength(inputSize);

                            if (b2 == (byte)0x24) {
                                if (src.remaining() < 1)
                                    return CoderResult.UNDERFLOW;

                                b3 = src.get();
                                inputSize++;

                                if ((b3 & (byte)0x80) != 0)
                                    return CoderResult.malformedForLength(inputSize);
                                if (b3 == 'A'){              // "$A"
                                    currentSODesig = SODesigGB;
                                } else if (b3 == ')') {
                                    if (src.remaining() < 1)
                                        return CoderResult.UNDERFLOW;
                                    b4 = src.get();
                                    inputSize++;
                                    if (b4 == 'A'){          // "$)A"
                                        currentSODesig = SODesigGB;
                                    } else if (b4 == 'G'){   // "$)G"
                                        currentSODesig = SODesigCNS;
                                    } else {
                                        return CoderResult.malformedForLength(inputSize);
                                    }
                                } else if (b3 == '*') {
                                    if (src.remaining() < 1)
                                        return CoderResult.UNDERFLOW;
                                    b4 = src.get();
                                    inputSize++;
                                    if (b4 != 'H') {         // "$*H"
                                        //SS2Desig -> CNS-P1
                                        return CoderResult.malformedForLength(inputSize);
                                    }
                                } else if (b3 == '+') {
                                    if (src.remaining() < 1)
                                        return CoderResult.UNDERFLOW;
                                    b4 = src.get();
                                    inputSize++;
                                    if (b4 != 'I'){          // "$+I"
                                        //SS3Desig -> CNS-P2.
                                        return CoderResult.malformedForLength(inputSize);
                                    }
                                } else {
                                        return CoderResult.malformedForLength(inputSize);
                                }
                            } else if (b2 == ISO_SS2_7 || b2 == ISO_SS3_7) {
                                if (src.remaining() < 2)
                                    return CoderResult.UNDERFLOW;
                                b3 = src.get();
                                b4 = src.get();
                                inputSize += 2;
                                if (dst.remaining() < 1)
                                    return CoderResult.OVERFLOW;
                                //SS2->CNS-P2, SS3->CNS-P3
                                c = cnsDecode(b3, b4, b2);
                                if (c == REPLACE_CHAR)
                                    return CoderResult.unmappableForLength(inputSize);
                                dst.put(c);
                            } else {
                                return CoderResult.malformedForLength(inputSize);
                            }
                        } else if (b1 == ISO_SO) {
                            shiftOut = true;
                        } else if (b1 == ISO_SI) { // shift back in
                            shiftOut = false;
                        }
                        mark += inputSize;
                        if (src.remaining() < 1)
                            return CoderResult.UNDERFLOW;
                        b1 = src.get();
                        inputSize = 1;
                    }

                    if (dst.remaining() < 1)
                        return CoderResult.OVERFLOW;

                    if (!shiftOut) {
                        dst.put((char)(b1 & 0xff));  //clear the upper byte
                        mark += inputSize;
                    } else {
                        if (src.remaining() < 1)
                            return CoderResult.UNDERFLOW;
                        b2 = src.get();
                        inputSize++;
                        c = SODecode(b1, b2, currentSODesig);
                        if (c == REPLACE_CHAR)
                            return CoderResult.unmappableForLength(inputSize);
                        dst.put(c);
                        mark += inputSize;
                    }
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        private CoderResult decodeArrayLoop(ByteBuffer src,
                                            CharBuffer dst)
        {
            int inputSize;
            byte b1, b2, b3, b4;
            char c;

            byte[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();

            char[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();

            try {
                while (sp < sl) {
                    b1 = sa[sp];
                    inputSize = 1;

                    while (b1 == ISO_ESC || b1 == ISO_SO || b1 == ISO_SI) {
                        if (b1 == ISO_ESC) {  // ESC
                            currentSODesig = SODesigGB;

                            if (sp + 2 > sl)
                                return CoderResult.UNDERFLOW;

                            b2 = sa[sp + 1];
                            inputSize++;

                            if ((b2 & (byte)0x80) != 0)
                                return CoderResult.malformedForLength(inputSize);
                            if (b2 == (byte)0x24) {
                                if (sp + 3 > sl)
                                    return CoderResult.UNDERFLOW;

                                b3 = sa[sp + 2];
                                inputSize++;

                                if ((b3 & (byte)0x80) != 0)
                                    return CoderResult.malformedForLength(inputSize);
                                if (b3 == 'A') {              // "$A"
                                    /* <ESC>$A is not a legal designator sequence for
                                       ISO2022_CN, it is listed as an escape sequence
                                       for GB2312 in ISO2022-JP-2. Keep it here just for
                                       the sake of "compatibility".
                                     */
                                    currentSODesig = SODesigGB;
                                } else if (b3 == ')') {
                                    if (sp + 4 > sl)
                                        return CoderResult.UNDERFLOW;
                                    b4 = sa[sp + 3];
                                    inputSize++;

                                    if (b4 == 'A'){          // "$)A"
                                        currentSODesig = SODesigGB;
                                    } else if (b4 == 'G'){   // "$)G"
                                        currentSODesig = SODesigCNS;
                                    } else {
                                        return CoderResult.malformedForLength(inputSize);
                                    }
                                } else if (b3 == '*') {
                                    if (sp + 4 > sl)
                                        return CoderResult.UNDERFLOW;
                                    b4 = sa[sp + 3];
                                    inputSize++;
                                    if (b4 != 'H'){          // "$*H"
                                        return CoderResult.malformedForLength(inputSize);
                                    }
                                } else if (b3 == '+') {
                                    if (sp + 4 > sl)
                                        return CoderResult.UNDERFLOW;
                                    b4 = sa[sp + 3];
                                    inputSize++;
                                    if (b4 != 'I'){          // "$+I"
                                        return CoderResult.malformedForLength(inputSize);
                                    }
                                } else {
                                        return CoderResult.malformedForLength(inputSize);
                                }
                            } else if (b2 == ISO_SS2_7 || b2 == ISO_SS3_7) {
                                if (sp + 4 > sl) {
                                    return CoderResult.UNDERFLOW;
                                }
                                b3 = sa[sp + 2];
                                b4 = sa[sp + 3];
                                if (dl - dp < 1)  {
                                    return CoderResult.OVERFLOW;
                                }
                                inputSize += 2;
                                c = cnsDecode(b3, b4, b2);
                                if (c == REPLACE_CHAR)
                                    return CoderResult.unmappableForLength(inputSize);
                                da[dp++] = c;
                            } else {
                                return CoderResult.malformedForLength(inputSize);
                            }
                        } else if (b1 == ISO_SO) {
                            shiftOut = true;
                        } else if (b1 == ISO_SI) { // shift back in
                            shiftOut = false;
                        }
                        sp += inputSize;
                        if (sp + 1 > sl)
                            return CoderResult.UNDERFLOW;
                        b1 = sa[sp];
                        inputSize = 1;
                    }

                    if (dl - dp < 1) {
                        return CoderResult.OVERFLOW;
                    }

                    if (!shiftOut) {
                        da[dp++] = (char)(b1 & 0xff);  //clear the upper byte
                    } else {
                        if (sp + 2 > sl)
                            return CoderResult.UNDERFLOW;
                        b2 = sa[sp + 1];
                        inputSize++;
                        c = SODecode(b1, b2, currentSODesig);
                        if (c == REPLACE_CHAR)
                            return CoderResult.unmappableForLength(inputSize);
                        da[dp++] = c;
                    }
                    sp += inputSize;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
            }
        }

        protected CoderResult decodeLoop(ByteBuffer src,
                                         CharBuffer dst)
        {
            if (src.hasArray() && dst.hasArray())
                return decodeArrayLoop(src, dst);
            else
                return decodeBufferLoop(src, dst);
        }
    }
}
