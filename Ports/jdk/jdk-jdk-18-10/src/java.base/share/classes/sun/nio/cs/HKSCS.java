/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Arrays;
import sun.nio.cs.DoubleByte;
import sun.nio.cs.Surrogate;
import static sun.nio.cs.CharsetMapping.*;

public class HKSCS {

    public static class Decoder extends DoubleByte.Decoder {
        static int b2Min = 0x40;
        static int b2Max = 0xfe;

        private char[][] b2cBmp;
        private char[][] b2cSupp;
        private DoubleByte.Decoder big5Dec;

        protected Decoder(Charset cs,
                          DoubleByte.Decoder big5Dec,
                          char[][] b2cBmp, char[][] b2cSupp)
        {
            // super(cs, 0.5f, 1.0f);
            // need to extends DoubleByte.Decoder so the
            // sun.io can use it. this implementation
            super(cs, 0.5f, 1.0f, null, null, 0, 0, true);
            this.big5Dec = big5Dec;
            this.b2cBmp = b2cBmp;
            this.b2cSupp = b2cSupp;
        }

        public char decodeSingle(int b) {
            return big5Dec.decodeSingle(b);
        }

        public char decodeBig5(int b1, int b2) {
            return big5Dec.decodeDouble(b1, b2);
        }

        public char decodeDouble(int b1, int b2) {
            return b2cBmp[b1][b2 - b2Min];
        }

        public char decodeDoubleEx(int b1, int b2) {
            /* if the b2cSupp is null, the subclass need
               to override the methold
            if (b2cSupp == null)
                return UNMAPPABLE_DECODING;
             */
            return b2cSupp[b1][b2 - b2Min];
        }

        protected CoderResult decodeArrayLoop(ByteBuffer src, CharBuffer dst) {
            byte[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();

            char[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();

            try {
                while (sp < sl) {
                    int b1 = sa[sp] & 0xff;
                    char c = decodeSingle(b1);
                    int inSize = 1, outSize = 1;
                    char[] cc = null;
                    if (c == UNMAPPABLE_DECODING) {
                        if (sl - sp < 2)
                            return CoderResult.UNDERFLOW;
                        int b2 = sa[sp + 1] & 0xff;
                        inSize++;
                        if (b2 < b2Min || b2 > b2Max)
                            return CoderResult.unmappableForLength(2);
                        c = decodeDouble(b1, b2);           //bmp
                        if (c == UNMAPPABLE_DECODING) {
                            c = decodeDoubleEx(b1, b2);     //supp
                            if (c == UNMAPPABLE_DECODING) {
                                c = decodeBig5(b1, b2);     //big5
                                if (c == UNMAPPABLE_DECODING)
                                    return CoderResult.unmappableForLength(2);
                            } else {
                                // supplementary character in u+2xxxx area
                                outSize = 2;
                            }
                        }
                    }
                    if (dl - dp < outSize)
                        return CoderResult.OVERFLOW;
                    if (outSize == 2) {
                        // supplementary characters
                        da[dp++] = Surrogate.high(0x20000 + c);
                        da[dp++] = Surrogate.low(0x20000 + c);
                    } else {
                        da[dp++] = c;
                    }
                    sp += inSize;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
            }
        }

        protected CoderResult decodeBufferLoop(ByteBuffer src, CharBuffer dst) {
            int mark = src.position();
            try {
                while (src.hasRemaining()) {
                    char[] cc = null;
                    int b1 = src.get() & 0xff;
                    int inSize = 1, outSize = 1;
                    char c = decodeSingle(b1);
                    if (c == UNMAPPABLE_DECODING) {
                        if (src.remaining() < 1)
                            return CoderResult.UNDERFLOW;
                        int b2 = src.get() & 0xff;
                        inSize++;
                        if (b2 < b2Min || b2 > b2Max)
                            return CoderResult.unmappableForLength(2);
                        c = decodeDouble(b1, b2);           //bmp
                        if (c == UNMAPPABLE_DECODING) {
                            c = decodeDoubleEx(b1, b2);     //supp
                            if (c == UNMAPPABLE_DECODING) {
                                c = decodeBig5(b1, b2);     //big5
                                if (c == UNMAPPABLE_DECODING)
                                    return CoderResult.unmappableForLength(2);
                            } else {
                                outSize = 2;
                            }
                        }
                    }
                    if (dst.remaining() < outSize)
                        return CoderResult.OVERFLOW;
                    if (outSize == 2) {
                        dst.put(Surrogate.high(0x20000 + c));
                        dst.put(Surrogate.low(0x20000 + c));
                    } else {
                        dst.put(c);
                    }
                    mark += inSize;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        public int decode(byte[] src, int sp, int len, char[] dst) {
            int dp = 0;
            int sl = sp + len;
            char repl = replacement().charAt(0);
            while (sp < sl) {
                int b1 = src[sp++] & 0xff;
                char c = decodeSingle(b1);
                if (c == UNMAPPABLE_DECODING) {
                    if (sl == sp) {
                        c = repl;
                    } else {
                        int b2 = src[sp++] & 0xff;
                        if (b2 < b2Min || b2 > b2Max) {
                            c = repl;
                        } else if ((c = decodeDouble(b1, b2)) == UNMAPPABLE_DECODING) {
                            c = decodeDoubleEx(b1, b2);     //supp
                            if (c == UNMAPPABLE_DECODING) {
                                c = decodeBig5(b1, b2);     //big5
                                if (c == UNMAPPABLE_DECODING)
                                    c = repl;
                            } else {
                                // supplementary character in u+2xxxx area
                                dst[dp++] = Surrogate.high(0x20000 + c);
                                dst[dp++] = Surrogate.low(0x20000 + c);
                                continue;
                            }
                        }
                    }
                }
                dst[dp++] = c;
            }
            return dp;
        }

        public CoderResult decodeLoop(ByteBuffer src, CharBuffer dst) {
            if (src.hasArray() && dst.hasArray())
                return decodeArrayLoop(src, dst);
            else
                return decodeBufferLoop(src, dst);
        }

        public static void initb2c(char[][]b2c, String[] b2cStr)
        {
            for (int i = 0; i < b2cStr.length; i++) {
                if (b2cStr[i] == null)
                    b2c[i] = DoubleByte.B2C_UNMAPPABLE;
                else
                    b2c[i] = b2cStr[i].toCharArray();
            }
        }

    }

    public static class Encoder extends DoubleByte.Encoder {
        private DoubleByte.Encoder big5Enc;
        private char[][] c2bBmp;
        private char[][] c2bSupp;

        protected Encoder(Charset cs,
                          DoubleByte.Encoder big5Enc,
                          char[][] c2bBmp,
                          char[][] c2bSupp)
        {
            super(cs, null, null, true);
            this.big5Enc = big5Enc;
            this.c2bBmp = c2bBmp;
            this.c2bSupp = c2bSupp;
        }

        public int encodeBig5(char ch) {
            return big5Enc.encodeChar(ch);
        }

        public int encodeChar(char ch) {
            int bb = c2bBmp[ch >> 8][ch & 0xff];
            if (bb == UNMAPPABLE_ENCODING)
                return encodeBig5(ch);
            return bb;
        }

        public int encodeSupp(int cp) {
            if ((cp & 0xf0000) != 0x20000)
                return UNMAPPABLE_ENCODING;
            return c2bSupp[(cp >> 8) & 0xff][cp & 0xff];
        }

        public boolean canEncode(char c) {
            return encodeChar(c) != UNMAPPABLE_ENCODING;
        }

        protected CoderResult encodeArrayLoop(CharBuffer src, ByteBuffer dst) {
            char[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();

            byte[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();

            try {
                while (sp < sl) {
                    char c = sa[sp];
                    int inSize = 1;
                    int bb = encodeChar(c);
                    if (bb == UNMAPPABLE_ENCODING) {
                        if (Character.isSurrogate(c)) {
                            int cp;
                            if ((cp = sgp().parse(c, sa, sp, sl)) < 0)
                                return sgp.error();
                            bb = encodeSupp(cp);
                            if (bb == UNMAPPABLE_ENCODING)
                                return CoderResult.unmappableForLength(2);
                            inSize = 2;
                        } else {
                            return CoderResult.unmappableForLength(1);
                        }
                    }
                    if (bb > MAX_SINGLEBYTE) {    // DoubleByte
                        if (dl - dp < 2)
                            return CoderResult.OVERFLOW;
                        da[dp++] = (byte)(bb >> 8);
                        da[dp++] = (byte)bb;
                    } else {                      // SingleByte
                        if (dl - dp < 1)
                            return CoderResult.OVERFLOW;
                        da[dp++] = (byte)bb;
                    }
                    sp += inSize;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
            }
        }

        protected CoderResult encodeBufferLoop(CharBuffer src, ByteBuffer dst) {
            int mark = src.position();
            try {
                while (src.hasRemaining()) {
                    int inSize = 1;
                    char c = src.get();
                    int bb = encodeChar(c);
                    if (bb == UNMAPPABLE_ENCODING) {
                        if (Character.isSurrogate(c)) {
                            int cp;
                            if ((cp = sgp().parse(c, src)) < 0)
                                return sgp.error();
                            bb = encodeSupp(cp);
                            if (bb == UNMAPPABLE_ENCODING)
                                return CoderResult.unmappableForLength(2);
                            inSize = 2;
                        } else {
                            return CoderResult.unmappableForLength(1);
                        }
                    }
                    if (bb > MAX_SINGLEBYTE) {  // DoubleByte
                        if (dst.remaining() < 2)
                            return CoderResult.OVERFLOW;
                        dst.put((byte)(bb >> 8));
                        dst.put((byte)(bb));
                    } else {
                        if (dst.remaining() < 1)
                        return CoderResult.OVERFLOW;
                        dst.put((byte)bb);
                    }
                    mark += inSize;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        protected CoderResult encodeLoop(CharBuffer src, ByteBuffer dst) {
            if (src.hasArray() && dst.hasArray())
                return encodeArrayLoop(src, dst);
            else
                return encodeBufferLoop(src, dst);
        }

        private byte[] repl = replacement();
        protected void implReplaceWith(byte[] newReplacement) {
            repl = newReplacement;
        }

        public int encode(char[] src, int sp, int len, byte[] dst) {
            int dp = 0;
            int sl = sp + len;
            while (sp < sl) {
                char c = src[sp++];
                int bb = encodeChar(c);
                if (bb == UNMAPPABLE_ENCODING) {
                    if (!Character.isHighSurrogate(c) || sp == sl ||
                        !Character.isLowSurrogate(src[sp]) ||
                        (bb = encodeSupp(Character.toCodePoint(c, src[sp++])))
                        == UNMAPPABLE_ENCODING) {
                        dst[dp++] = repl[0];
                        if (repl.length > 1)
                            dst[dp++] = repl[1];
                        continue;
                    }
                }
                if (bb > MAX_SINGLEBYTE) {        // DoubleByte
                    dst[dp++] = (byte)(bb >> 8);
                    dst[dp++] = (byte)bb;
                } else {                          // SingleByte
                    dst[dp++] = (byte)bb;
                }
            }
            return dp;
        }

        public int encodeFromUTF16(byte[] src, int sp, int len, byte[] dst) {
            int dp = 0;
            int sl = sp + len;
            int dl = dst.length;
            while (sp < sl) {
                char c = StringUTF16.getChar(src, sp++);
                int bb = encodeChar(c);
                if (bb == UNMAPPABLE_ENCODING) {
                    if (!Character.isHighSurrogate(c) || sp == sl ||
                        !Character.isLowSurrogate(StringUTF16.getChar(src,sp)) ||
                        (bb = encodeSupp(Character.toCodePoint(c, StringUTF16.getChar(src, sp++))))
                        == UNMAPPABLE_ENCODING) {
                        dst[dp++] = repl[0];
                        if (repl.length > 1)
                            dst[dp++] = repl[1];
                        continue;
                    }
                }
                if (bb > MAX_SINGLEBYTE) { // DoubleByte
                    dst[dp++] = (byte)(bb >> 8);
                    dst[dp++] = (byte)bb;
                } else {                   // SingleByte
                    dst[dp++] = (byte)bb;
                }
            }
            return dp;
        }

        static char[] C2B_UNMAPPABLE = new char[0x100];
        static {
            Arrays.fill(C2B_UNMAPPABLE, (char)UNMAPPABLE_ENCODING);
        }

        public static void initc2b(char[][] c2b, String[] b2cStr, String pua) {
            // init c2b/c2bSupp from b2cStr and supp
            int b2Min = 0x40;
            Arrays.fill(c2b, C2B_UNMAPPABLE);
            for (int b1 = 0; b1 < 0x100; b1++) {
                String s = b2cStr[b1];
                if (s == null)
                    continue;
                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    if (c == UNMAPPABLE_DECODING)
                        continue;
                    int hi = c >> 8;
                    if (c2b[hi] == C2B_UNMAPPABLE) {
                        c2b[hi] = new char[0x100];
                        Arrays.fill(c2b[hi], (char)UNMAPPABLE_ENCODING);
                    }
                    c2b[hi][c & 0xff] = (char)((b1 << 8) | (i + b2Min));
                }
            }
            if (pua != null) {        // add the compatibility pua entries
                char c = '\ue000';    //first pua character
                for (int i = 0; i < pua.length(); i++) {
                    char bb = pua.charAt(i);
                    if (bb != UNMAPPABLE_DECODING) {
                        int hi = c >> 8;
                        if (c2b[hi] == C2B_UNMAPPABLE) {
                            c2b[hi] = new char[0x100];
                            Arrays.fill(c2b[hi], (char)UNMAPPABLE_ENCODING);
                        }
                        c2b[hi][c & 0xff] = bb;
                    }
                    c++;
                }
            }
        }
    }
}
