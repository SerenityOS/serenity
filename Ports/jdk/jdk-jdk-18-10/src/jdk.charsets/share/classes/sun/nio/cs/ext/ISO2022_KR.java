/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import sun.nio.cs.HistoricallyNamedCharset;
import sun.nio.cs.*;

public class ISO2022_KR extends ISO2022
implements HistoricallyNamedCharset
{
    private static class Holder {
        private static final Charset ksc5601_cs = new EUC_KR();
    }

    public ISO2022_KR() {
        super("ISO-2022-KR", ExtendedCharsets.aliasesFor("ISO-2022-KR"));
    }

    public boolean contains(Charset cs) {
        // overlapping repertoire of EUC_KR, aka KSC5601
        return ((cs instanceof EUC_KR) ||
             (cs.name().equals("US-ASCII")) ||
             (cs instanceof ISO2022_KR));
    }

    public String historicalName() {
        return "ISO2022KR";
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }


    private static class Decoder extends CharsetDecoder {

        private static final byte[] SOD = new byte[] {'$', ')', 'C' };

        private static final DoubleByte.Decoder KSC5601 = (DoubleByte.Decoder)
                new EUC_KR().newDecoder();

        private static final byte ISO_ESC = 0x1b;
        private static final byte ISO_SI = 0x0f;
        private static final byte ISO_SO = 0x0e;
        private static final byte ISO_SS2_7 = 0x4e;
        private static final byte ISO_SS3_7 = 0x4f;
        private static final byte MSB = (byte)0x80;
        private static final char REPLACE_CHAR = '\uFFFD';
        private static final byte minDesignatorLength = 3;

        private static final byte SOFlag = 0;
        private static final byte SS2Flag = 1;
        private static final byte SS3Flag = 2;

        private boolean shiftout;

        private Decoder(Charset cs) {
            super(cs, 1.0f, 1.0f);
        }

        protected void implReset() {
            shiftout = false;
        }

        private char decode(byte byte1, byte byte2, byte shiftFlag)
        {
            if (shiftFlag == SOFlag) {
                return KSC5601.decodeDouble((byte1 | MSB) & 0xFF, (byte2 | MSB) & 0xFF);
            }
            return REPLACE_CHAR;
        }

        private boolean findDesig(byte[] in, int sp, int sl) {
            if (sl - sp >= SOD.length) {
                int j = 0;
                while (j < SOD.length && in[sp + j] == SOD[j]) { j++; }
                return j == SOD.length;
            }
            return false;
        }

        private boolean findDesigBuf(ByteBuffer in) {
            if (in.remaining() >= SOD.length) {
                int j = 0;
                in.mark();
                while (j < SOD.length && in.get() == SOD[j]) { j++; }
                if (j == SOD.length)
                    return true;
                in.reset();
            }
            return false;
        }

        private CoderResult decodeArrayLoop(ByteBuffer src,
                                            CharBuffer dst)
        {
            byte[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();

            char[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();

            int b1, b2, b3;

            try {
                while (sp < sl) {
                    b1 = sa[sp] & 0xff;
                    int inputSize = 1;
                    switch (b1) {
                        case ISO_SO:
                            shiftout = true;
                            inputSize = 1;
                            break;
                        case ISO_SI:
                            shiftout = false;
                            inputSize = 1;
                            break;
                        case ISO_ESC:
                            if (sl - sp - 1 < minDesignatorLength)
                                return CoderResult.UNDERFLOW;

                            if (findDesig(sa, sp + 1, sl)) {
                                inputSize = SOD.length + 1;
                                break;
                            }
                            if (sl - sp < 2)
                                return CoderResult.UNDERFLOW;
                            b1 = sa[sp + 1];
                            switch (b1) {
                                case ISO_SS2_7:
                                    if (sl - sp < 4)
                                        return CoderResult.UNDERFLOW;
                                    b2 = sa[sp +2];
                                    b3 = sa[sp +3];
                                    if (dl - dp <1)
                                        return CoderResult.OVERFLOW;
                                    da[dp] = decode((byte)b2,
                                            (byte)b3,
                                            SS2Flag);
                                    dp++;
                                    inputSize = 4;
                                    break;
                                case ISO_SS3_7:
                                    if (sl - sp < 4)
                                        return CoderResult.UNDERFLOW;
                                    b2 = sa[sp + 2];
                                    b3 = sa[sp + 3];
                                    if (dl - dp < 1)
                                        return CoderResult.OVERFLOW;
                                    da[dp] = decode((byte)b2,
                                            (byte)b3,
                                            SS3Flag);
                                    dp++;
                                    inputSize = 4;
                                    break;
                                default:
                                    return CoderResult.malformedForLength(2);
                            }
                            break;
                        default:
                            if (dl - dp < 1)
                                return CoderResult.OVERFLOW;
                            if (!shiftout) {
                                da[dp++]=(char)(sa[sp] & 0xff);
                            } else {
                                if (dl - dp < 1)
                                    return CoderResult.OVERFLOW;
                                if (sl - sp < 2)
                                    return CoderResult.UNDERFLOW;
                                b2 = sa[sp+1] & 0xff;
                                da[dp++] = decode((byte)b1,
                                        (byte)b2,
                                        SOFlag);
                                inputSize = 2;
                            }
                            break;
                    }
                    sp += inputSize;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
            }
        }

        private CoderResult decodeBufferLoop(ByteBuffer src,
                                             CharBuffer dst)
        {
            int mark = src.position();
            int b1, b2, b3;

            try {
                while (src.hasRemaining()) {
                    b1 = src.get();
                    int inputSize = 1;
                    switch (b1) {
                        case ISO_SO:
                            shiftout = true;
                            break;
                        case ISO_SI:
                            shiftout = false;
                            break;
                        case ISO_ESC:
                            if (src.remaining() < minDesignatorLength)
                                return CoderResult.UNDERFLOW;

                            if (findDesigBuf(src)) {
                                inputSize = SOD.length + 1;
                                break;
                            }

                            if (src.remaining() < 1)
                                return CoderResult.UNDERFLOW;
                            b1 = src.get();
                            switch(b1) {
                                case ISO_SS2_7:
                                    if (src.remaining() < 2)
                                        return CoderResult.UNDERFLOW;
                                    b2 = src.get();
                                    b3 = src.get();
                                    if (dst.remaining() < 1)
                                        return CoderResult.OVERFLOW;
                                    dst.put(decode((byte)b2,
                                            (byte)b3,
                                            SS2Flag));
                                    inputSize = 4;
                                    break;
                                case ISO_SS3_7:
                                    if (src.remaining() < 2)
                                        return CoderResult.UNDERFLOW;
                                    b2 = src.get();
                                    b3 = src.get();
                                    if (dst.remaining() < 1)
                                        return CoderResult.OVERFLOW;
                                    dst.put(decode((byte)b2,
                                            (byte)b3,
                                            SS3Flag));
                                    inputSize = 4;
                                    break;
                                default:
                                    return CoderResult.malformedForLength(2);
                            }
                            break;
                        default:
                            if (dst.remaining() < 1)
                                return CoderResult.OVERFLOW;
                            if (!shiftout) {
                                dst.put((char)(b1 & 0xff));
                            } else {
                                if (src.remaining() < 1)
                                    return CoderResult.UNDERFLOW;
                                b2 = src.get() & 0xff;
                                dst.put(decode((byte)b1,
                                        (byte)b2,
                                        SOFlag));
                                inputSize = 2;
                            }
                            break;
                    }
                    mark += inputSize;
                }
                return CoderResult.UNDERFLOW;
            } catch (Exception e) { e.printStackTrace(); return CoderResult.OVERFLOW; }
            finally {
                src.position(mark);
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

    private static class Encoder extends ISO2022.Encoder {

        private static final byte[] SOD = new byte[] {'$', ')', 'C' };

        public Encoder(Charset cs) {
            super(cs);
            SODesig = SOD;
            try {
                ISOEncoder = Holder.ksc5601_cs.newEncoder();
            } catch (Exception e) { }
        }

        public boolean canEncode(char c) {
            return (ISOEncoder.canEncode(c));
        }
    }
}
