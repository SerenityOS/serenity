/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;
import sun.nio.cs.Surrogate;
import sun.nio.cs.ArrayDecoder;
import sun.nio.cs.ArrayEncoder;
import static sun.nio.cs.CharsetMapping.*;

/*
 * Four types of "DoubleByte" charsets are implemented in this class
 * (1)DoubleByte
 *    The "mostly widely used" multibyte charset, a combination of
 *    a singlebyte character set (usually the ASCII charset) and a
 *    doublebyte character set. The codepoint values of singlebyte
 *    and doublebyte don't overlap. Microsoft's multibyte charsets
 *    and IBM's "DBCS_ASCII" charsets, such as IBM1381, 942, 943,
 *    948, 949 and 950 are such charsets.
 *
 * (2)DoubleByte_EBCDIC
 *    IBM EBCDIC Mix multibyte charset. Use SO and SI to shift (switch)
 *    in and out between the singlebyte character set and doublebyte
 *    character set.
 *
 * (3)DoubleByte_SIMPLE_EUC
 *    It's a "simple" form of EUC encoding scheme, only have the
 *    singlebyte character set G0 and one doublebyte character set
 *    G1 are defined, G2 (with SS2) and G3 (with SS3) are not used.
 *    So it is actually the same as the "typical" type (1) mentioned
 *    above, except it return "malformed" for the SS2 and SS3 when
 *    decoding.
 *
 * (4)DoubleByte ONLY
 *    A "pure" doublebyte only character set. From implementation
 *    point of view, this is the type (1) with "decodeSingle" always
 *    returns unmappable.
 *
 * For simplicity, all implementations share the same decoding and
 * encoding data structure.
 *
 * Decoding:
 *
 *    char[][] b2c;
 *    char[] b2cSB;
 *    int b2Min, b2Max
 *
 *    public char decodeSingle(int b) {
 *        return b2cSB.[b];
 *    }
 *
 *    public char decodeDouble(int b1, int b2) {
 *        if (b2 < b2Min || b2 > b2Max)
 *            return UNMAPPABLE_DECODING;
 *         return b2c[b1][b2 - b2Min];
 *    }
 *
 *    (1)b2Min, b2Max are the corresponding min and max value of the
 *       low-half of the double-byte.
 *    (2)The high 8-bit/b1 of the double-byte are used to indexed into
 *       b2c array.
 *
 * Encoding:
 *
 *    char[] c2b;
 *    char[] c2bIndex;
 *
 *    public int encodeChar(char ch) {
 *        return c2b[c2bIndex[ch >> 8] + (ch & 0xff)];
 *    }
 *
 */

public class DoubleByte {

    public static final char[] B2C_UNMAPPABLE;
    static {
        B2C_UNMAPPABLE = new char[0x100];
        Arrays.fill(B2C_UNMAPPABLE, UNMAPPABLE_DECODING);
    }

    public static class Decoder extends CharsetDecoder
                                implements DelegatableDecoder, ArrayDecoder
    {
        private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();

        final char[][] b2c;
        final char[] b2cSB;
        final int b2Min;
        final int b2Max;
        final boolean isASCIICompatible;

        // for SimpleEUC override
        protected CoderResult crMalformedOrUnderFlow(int b) {
            return CoderResult.UNDERFLOW;
        }

        protected CoderResult crMalformedOrUnmappable(int b1, int b2) {
            if (b2c[b1] == B2C_UNMAPPABLE ||                // isNotLeadingByte(b1)
                b2c[b2] != B2C_UNMAPPABLE ||                // isLeadingByte(b2)
                decodeSingle(b2) != UNMAPPABLE_DECODING) {  // isSingle(b2)
                return CoderResult.malformedForLength(1);
            }
            return CoderResult.unmappableForLength(2);
        }

        public Decoder(Charset cs, float avgcpb, float maxcpb,
                       char[][] b2c, char[] b2cSB,
                       int b2Min, int b2Max,
                       boolean isASCIICompatible) {
            super(cs, avgcpb, maxcpb);
            this.b2c = b2c;
            this.b2cSB = b2cSB;
            this.b2Min = b2Min;
            this.b2Max = b2Max;
            this.isASCIICompatible = isASCIICompatible;
        }

        public Decoder(Charset cs, char[][] b2c, char[] b2cSB, int b2Min, int b2Max,
                       boolean isASCIICompatible) {
            this(cs, 0.5f, 1.0f, b2c, b2cSB, b2Min, b2Max, isASCIICompatible);
        }

        public Decoder(Charset cs, char[][] b2c, char[] b2cSB, int b2Min, int b2Max) {
            this(cs, 0.5f, 1.0f, b2c, b2cSB, b2Min, b2Max, false);
        }

        protected CoderResult decodeArrayLoop(ByteBuffer src, CharBuffer dst) {
            byte[] sa = src.array();
            int soff = src.arrayOffset();
            int sp = soff + src.position();
            int sl = soff + src.limit();

            char[] da = dst.array();
            int doff = dst.arrayOffset();
            int dp = doff + dst.position();
            int dl = doff + dst.limit();

            try {
                if (isASCIICompatible) {
                    int n = JLA.decodeASCII(sa, sp, da, dp, Math.min(dl - dp, sl - sp));
                    dp += n;
                    sp += n;
                }
                while (sp < sl && dp < dl) {
                    // inline the decodeSingle/Double() for better performance
                    int inSize = 1;
                    int b1 = sa[sp] & 0xff;
                    char c = b2cSB[b1];
                    if (c == UNMAPPABLE_DECODING) {
                        if (sl - sp < 2)
                            return crMalformedOrUnderFlow(b1);
                        int b2 = sa[sp + 1] & 0xff;
                        if (b2 < b2Min || b2 > b2Max ||
                            (c = b2c[b1][b2 - b2Min]) == UNMAPPABLE_DECODING) {
                            return crMalformedOrUnmappable(b1, b2);
                        }
                        inSize++;
                    }
                    da[dp++] = c;
                    sp += inSize;
                }
                return (sp >= sl) ? CoderResult.UNDERFLOW
                                  : CoderResult.OVERFLOW;
            } finally {
                src.position(sp - soff);
                dst.position(dp - doff);
            }
        }

        protected CoderResult decodeBufferLoop(ByteBuffer src, CharBuffer dst) {
            int mark = src.position();
            try {

                while (src.hasRemaining() && dst.hasRemaining()) {
                    int b1 = src.get() & 0xff;
                    char c = b2cSB[b1];
                    int inSize = 1;
                    if (c == UNMAPPABLE_DECODING) {
                        if (src.remaining() < 1)
                            return crMalformedOrUnderFlow(b1);
                        int b2 = src.get() & 0xff;
                        if (b2 < b2Min || b2 > b2Max ||
                            (c = b2c[b1][b2 - b2Min]) == UNMAPPABLE_DECODING)
                            return crMalformedOrUnmappable(b1, b2);
                        inSize++;
                    }
                    dst.put(c);
                    mark += inSize;
                }
                return src.hasRemaining()? CoderResult.OVERFLOW
                                         : CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        // Make some protected methods public for use by JISAutoDetect
        public CoderResult decodeLoop(ByteBuffer src, CharBuffer dst) {
            if (src.hasArray() && dst.hasArray())
                return decodeArrayLoop(src, dst);
            else
                return decodeBufferLoop(src, dst);
        }

        @Override
        public int decode(byte[] src, int sp, int len, char[] dst) {
            int dp = 0;
            int sl = sp + len;
            char repl = replacement().charAt(0);
            while (sp < sl) {
                int b1 = src[sp++] & 0xff;
                char c = b2cSB[b1];
                if (c == UNMAPPABLE_DECODING) {
                    if (sp < sl) {
                        int b2 = src[sp++] & 0xff;
                        if (b2 < b2Min || b2 > b2Max ||
                            (c = b2c[b1][b2 - b2Min]) == UNMAPPABLE_DECODING) {
                            if (crMalformedOrUnmappable(b1, b2).length() == 1) {
                                sp--;
                            }
                        }
                    }
                    if (c == UNMAPPABLE_DECODING) {
                         c = repl;
                    }
                }
                dst[dp++] = c;
            }
            return dp;
        }

        @Override
        public boolean isASCIICompatible() {
            return isASCIICompatible;
        }

        public void implReset() {
            super.implReset();
        }

        public CoderResult implFlush(CharBuffer out) {
            return super.implFlush(out);
        }

        // decode loops are not using decodeSingle/Double() for performance
        // reason.
        public char decodeSingle(int b) {
            return b2cSB[b];
        }

        public char decodeDouble(int b1, int b2) {
            if (b1 < 0 || b1 > b2c.length ||
                b2 < b2Min || b2 > b2Max)
                return UNMAPPABLE_DECODING;
            return  b2c[b1][b2 - b2Min];
        }
    }

    // IBM_EBCDIC_DBCS
    public static class Decoder_EBCDIC extends Decoder {
        private static final int SBCS = 0;
        private static final int DBCS = 1;
        private static final int SO = 0x0e;
        private static final int SI = 0x0f;
        private int  currentState;

        public Decoder_EBCDIC(Charset cs,
                              char[][] b2c, char[] b2cSB, int b2Min, int b2Max,
                              boolean isASCIICompatible) {
            super(cs, b2c, b2cSB, b2Min, b2Max, isASCIICompatible);
        }

        public Decoder_EBCDIC(Charset cs,
                              char[][] b2c, char[] b2cSB, int b2Min, int b2Max) {
            super(cs, b2c, b2cSB, b2Min, b2Max, false);
        }

        public void implReset() {
            currentState = SBCS;
        }

        // Check validity of dbcs ebcdic byte pair values
        //
        // First byte : 0x41 -- 0xFE
        // Second byte: 0x41 -- 0xFE
        // Doublebyte blank: 0x4040
        //
        // The validation implementation in "old" DBCS_IBM_EBCDIC and sun.io
        // as
        //            if ((b1 != 0x40 || b2 != 0x40) &&
        //                (b2 < 0x41 || b2 > 0xfe)) {...}
        // is not correct/complete (range check for b1)
        //
        private static boolean isDoubleByte(int b1, int b2) {
            return (0x41 <= b1 && b1 <= 0xfe && 0x41 <= b2 && b2 <= 0xfe)
                   || (b1 == 0x40 && b2 == 0x40); // DBCS-HOST SPACE
        }

        protected CoderResult decodeArrayLoop(ByteBuffer src, CharBuffer dst) {
            byte[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();
            char[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();

            try {
                // don't check dp/dl together here, it's possible to
                // decdoe a SO/SI without space in output buffer.
                while (sp < sl) {
                    int b1 = sa[sp] & 0xff;
                    int inSize = 1;
                    if (b1 == SO) {  // Shift out
                        if (currentState != SBCS)
                            return CoderResult.malformedForLength(1);
                        else
                            currentState = DBCS;
                    } else if (b1 == SI) {
                        if (currentState != DBCS)
                            return CoderResult.malformedForLength(1);
                        else
                            currentState = SBCS;
                    } else {
                        char c;
                        if (currentState == SBCS) {
                            c = b2cSB[b1];
                            if (c == UNMAPPABLE_DECODING)
                                return CoderResult.unmappableForLength(1);
                        } else {
                            if (sl - sp < 2)
                                return CoderResult.UNDERFLOW;
                            int b2 = sa[sp + 1] & 0xff;
                            if (b2 < b2Min || b2 > b2Max ||
                                (c = b2c[b1][b2 - b2Min]) == UNMAPPABLE_DECODING) {
                                if (!isDoubleByte(b1, b2))
                                    return CoderResult.malformedForLength(2);
                                return CoderResult.unmappableForLength(2);
                            }
                            inSize++;
                        }
                        if (dl - dp < 1)
                            return CoderResult.OVERFLOW;

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
                    int b1 = src.get() & 0xff;
                    int inSize = 1;
                    if (b1 == SO) {  // Shift out
                        if (currentState != SBCS)
                            return CoderResult.malformedForLength(1);
                        else
                            currentState = DBCS;
                    } else if (b1 == SI) {
                        if (currentState != DBCS)
                            return CoderResult.malformedForLength(1);
                        else
                            currentState = SBCS;
                    } else {
                        char c = UNMAPPABLE_DECODING;
                        if (currentState == SBCS) {
                            c = b2cSB[b1];
                            if (c == UNMAPPABLE_DECODING)
                                return CoderResult.unmappableForLength(1);
                        } else {
                            if (src.remaining() < 1)
                                return CoderResult.UNDERFLOW;
                            int b2 = src.get()&0xff;
                            if (b2 < b2Min || b2 > b2Max ||
                                (c = b2c[b1][b2 - b2Min]) == UNMAPPABLE_DECODING) {
                                if (!isDoubleByte(b1, b2))
                                    return CoderResult.malformedForLength(2);
                                return CoderResult.unmappableForLength(2);
                            }
                            inSize++;
                        }

                        if (dst.remaining() < 1)
                            return CoderResult.OVERFLOW;

                        dst.put(c);
                    }
                    mark += inSize;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        @Override
        public int decode(byte[] src, int sp, int len, char[] dst) {
            int dp = 0;
            int sl = sp + len;
            currentState = SBCS;
            char repl = replacement().charAt(0);
            while (sp < sl) {
                int b1 = src[sp++] & 0xff;
                if (b1 == SO) {  // Shift out
                    if (currentState != SBCS)
                        dst[dp++] = repl;
                    else
                        currentState = DBCS;
                } else if (b1 == SI) {
                    if (currentState != DBCS)
                        dst[dp++] = repl;
                    else
                        currentState = SBCS;
                } else {
                    char c =  UNMAPPABLE_DECODING;
                    if (currentState == SBCS) {
                        c = b2cSB[b1];
                        if (c == UNMAPPABLE_DECODING)
                            c = repl;
                    } else {
                        if (sl == sp) {
                            c = repl;
                        } else {
                            int b2 = src[sp++] & 0xff;
                            if (b2 < b2Min || b2 > b2Max ||
                                (c = b2c[b1][b2 - b2Min]) == UNMAPPABLE_DECODING) {
                                c = repl;
                            }
                        }
                    }
                    dst[dp++] = c;
                }
            }
            return dp;
        }
    }

    // DBCS_ONLY
    public static class Decoder_DBCSONLY extends Decoder {
        static final char[] b2cSB_UNMAPPABLE;
        static {
            b2cSB_UNMAPPABLE = new char[0x100];
            Arrays.fill(b2cSB_UNMAPPABLE, UNMAPPABLE_DECODING);
        }

        // always returns unmappableForLenth(2) for doublebyte_only
        @Override
        protected CoderResult crMalformedOrUnmappable(int b1, int b2) {
            return CoderResult.unmappableForLength(2);
        }

        public Decoder_DBCSONLY(Charset cs, char[][] b2c, char[] b2cSB, int b2Min, int b2Max,
                                boolean isASCIICompatible) {
            super(cs, 0.5f, 1.0f, b2c, b2cSB_UNMAPPABLE, b2Min, b2Max, isASCIICompatible);
        }

        public Decoder_DBCSONLY(Charset cs, char[][] b2c, char[] b2cSB, int b2Min, int b2Max) {
            super(cs, 0.5f, 1.0f, b2c, b2cSB_UNMAPPABLE, b2Min, b2Max, false);
        }
    }

    // EUC_SIMPLE
    // The only thing we need to "override" is to check SS2/SS3 and
    // return "malformed" if found
    public static class Decoder_EUC_SIM extends Decoder {
        private final int SS2 =  0x8E;
        private final int SS3 =  0x8F;

        public Decoder_EUC_SIM(Charset cs,
                               char[][] b2c, char[] b2cSB, int b2Min, int b2Max,
                               boolean isASCIICompatible) {
            super(cs, b2c, b2cSB, b2Min, b2Max, isASCIICompatible);
        }

        // No support provided for G2/G3 for SimpleEUC
        protected CoderResult crMalformedOrUnderFlow(int b) {
            if (b == SS2 || b == SS3 )
                return CoderResult.malformedForLength(1);
            return CoderResult.UNDERFLOW;
        }

        protected CoderResult crMalformedOrUnmappable(int b1, int b2) {
            if (b1 == SS2 || b1 == SS3 )
                return CoderResult.malformedForLength(1);
            return CoderResult.unmappableForLength(2);
        }

        @Override
        public int decode(byte[] src, int sp, int len, char[] dst) {
            int dp = 0;
            int sl = sp + len;
            char repl = replacement().charAt(0);
            while (sp < sl) {
                int b1 = src[sp++] & 0xff;
                char c = b2cSB[b1];
                if (c == UNMAPPABLE_DECODING) {
                    if (sp < sl) {
                        int b2 = src[sp++] & 0xff;
                        if (b2 < b2Min || b2 > b2Max ||
                            (c = b2c[b1][b2 - b2Min]) == UNMAPPABLE_DECODING) {
                            if (b1 == SS2 || b1 == SS3) {
                                sp--;
                            }
                            c = repl;
                        }
                    } else {
                        c = repl;
                    }
                }
                dst[dp++] = c;
            }
            return dp;
        }
    }

    public static class Encoder extends CharsetEncoder
                                implements ArrayEncoder
    {
        protected final int MAX_SINGLEBYTE = 0xff;
        private final char[] c2b;
        private final char[] c2bIndex;
        protected Surrogate.Parser sgp;
        final boolean isASCIICompatible;

        public Encoder(Charset cs, char[] c2b, char[] c2bIndex) {
            this(cs, c2b, c2bIndex, false);
        }

        public Encoder(Charset cs, char[] c2b, char[] c2bIndex, boolean isASCIICompatible) {
            super(cs, 2.0f, 2.0f);
            this.c2b = c2b;
            this.c2bIndex = c2bIndex;
            this.isASCIICompatible = isASCIICompatible;
        }

        public Encoder(Charset cs, float avg, float max, byte[] repl, char[] c2b, char[] c2bIndex,
                       boolean isASCIICompatible) {
            super(cs, avg, max, repl);
            this.c2b = c2b;
            this.c2bIndex = c2bIndex;
            this.isASCIICompatible = isASCIICompatible;
        }

        public boolean canEncode(char c) {
            return encodeChar(c) != UNMAPPABLE_ENCODING;
        }

        protected Surrogate.Parser sgp() {
            if (sgp == null)
                sgp = new Surrogate.Parser();
            return sgp;
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
                    int bb = encodeChar(c);
                    if (bb == UNMAPPABLE_ENCODING) {
                        if (Character.isSurrogate(c)) {
                            if (sgp().parse(c, sa, sp, sl) < 0)
                                return sgp.error();
                            return sgp.unmappableResult();
                        }
                        return CoderResult.unmappableForLength(1);
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

                    sp++;
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
                    char c = src.get();
                    int bb = encodeChar(c);
                    if (bb == UNMAPPABLE_ENCODING) {
                        if (Character.isSurrogate(c)) {
                            if (sgp().parse(c, src) < 0)
                                return sgp.error();
                            return sgp.unmappableResult();
                        }
                        return CoderResult.unmappableForLength(1);
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
                    mark++;
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

        protected byte[] repl = replacement();
        protected void implReplaceWith(byte[] newReplacement) {
            repl = newReplacement;
        }

        @Override
        public int encode(char[] src, int sp, int len, byte[] dst) {
            int dp = 0;
            int sl = sp + len;
            int dl = dst.length;
            while (sp < sl) {
                char c = src[sp++];
                int bb = encodeChar(c);
                if (bb == UNMAPPABLE_ENCODING) {
                    if (Character.isHighSurrogate(c) && sp < sl &&
                        Character.isLowSurrogate(src[sp])) {
                        sp++;
                    }
                    dst[dp++] = repl[0];
                    if (repl.length > 1)
                        dst[dp++] = repl[1];
                    continue;
                } //else
                if (bb > MAX_SINGLEBYTE) { // DoubleByte
                    dst[dp++] = (byte)(bb >> 8);
                    dst[dp++] = (byte)bb;
                } else {                          // SingleByte
                    dst[dp++] = (byte)bb;
                }
            }
            return dp;
        }

        @Override
        public int encodeFromLatin1(byte[] src, int sp, int len, byte[] dst) {
            int dp = 0;
            int sl = sp + len;
            while (sp < sl) {
                char c = (char)(src[sp++] & 0xff);
                int bb = encodeChar(c);
                if (bb == UNMAPPABLE_ENCODING) {
                    // no surrogate pair in latin1 string
                    dst[dp++] = repl[0];
                    if (repl.length > 1) {
                        dst[dp++] = repl[1];
                    }
                    continue;
                } //else
                if (bb > MAX_SINGLEBYTE) { // DoubleByte
                    dst[dp++] = (byte)(bb >> 8);
                    dst[dp++] = (byte)bb;
                } else {                   // SingleByte
                    dst[dp++] = (byte)bb;
                }

            }
            return dp;
        }

        @Override
        public int encodeFromUTF16(byte[] src, int sp, int len, byte[] dst) {
            int dp = 0;
            int sl = sp + len;
            while (sp < sl) {
                char c = StringUTF16.getChar(src, sp++);
                int bb = encodeChar(c);
                if (bb == UNMAPPABLE_ENCODING) {
                    if (Character.isHighSurrogate(c) && sp < sl &&
                        Character.isLowSurrogate(StringUTF16.getChar(src, sp))) {
                        sp++;
                    }
                    dst[dp++] = repl[0];
                    if (repl.length > 1) {
                        dst[dp++] = repl[1];
                    }
                    continue;
                } //else
                if (bb > MAX_SINGLEBYTE) { // DoubleByte
                    dst[dp++] = (byte)(bb >> 8);
                    dst[dp++] = (byte)bb;
                } else {                   // SingleByte
                    dst[dp++] = (byte)bb;
                }
            }
            return dp;
        }

        @Override
        public boolean isASCIICompatible() {
            return isASCIICompatible;
        }

        public int encodeChar(char ch) {
            return c2b[c2bIndex[ch >> 8] + (ch & 0xff)];
        }

        // init the c2b and c2bIndex tables from b2c.
        public static void initC2B(String[] b2c, String b2cSB, String b2cNR,  String c2bNR,
                            int b2Min, int b2Max,
                            char[] c2b, char[] c2bIndex)
        {
            Arrays.fill(c2b, (char)UNMAPPABLE_ENCODING);
            int off = 0x100;

            char[][] b2c_ca = new char[b2c.length][];
            char[] b2cSB_ca = null;
            if (b2cSB != null)
                b2cSB_ca = b2cSB.toCharArray();

            for (int i = 0; i < b2c.length; i++) {
                if (b2c[i] == null)
                    continue;
                b2c_ca[i] = b2c[i].toCharArray();
            }

            if (b2cNR != null) {
                int j = 0;
                while (j < b2cNR.length()) {
                    char b  = b2cNR.charAt(j++);
                    char c  = b2cNR.charAt(j++);
                    if (b < 0x100 && b2cSB_ca != null) {
                        if (b2cSB_ca[b] == c)
                            b2cSB_ca[b] = UNMAPPABLE_DECODING;
                    } else {
                        if (b2c_ca[b >> 8][(b & 0xff) - b2Min] == c)
                            b2c_ca[b >> 8][(b & 0xff) - b2Min] = UNMAPPABLE_DECODING;
                    }
                }
            }

            if (b2cSB_ca != null) {      // SingleByte
                for (int b = 0; b < b2cSB_ca.length; b++) {
                    char c = b2cSB_ca[b];
                    if (c == UNMAPPABLE_DECODING)
                        continue;
                    int index = c2bIndex[c >> 8];
                    if (index == 0) {
                        index = off;
                        off += 0x100;
                        c2bIndex[c >> 8] = (char)index;
                    }
                    c2b[index + (c & 0xff)] = (char)b;
                }
            }

            for (int b1 = 0; b1 < b2c.length; b1++) {  // DoubleByte
                char[] db = b2c_ca[b1];
                if (db == null)
                    continue;
                for (int b2 = b2Min; b2 <= b2Max; b2++) {
                    char c = db[b2 - b2Min];
                    if (c == UNMAPPABLE_DECODING)
                        continue;
                    int index = c2bIndex[c >> 8];
                    if (index == 0) {
                        index = off;
                        off += 0x100;
                        c2bIndex[c >> 8] = (char)index;
                    }
                    c2b[index + (c & 0xff)] = (char)((b1 << 8) | b2);
                }
            }

            if (c2bNR != null) {
                // add c->b only nr entries
                for (int i = 0; i < c2bNR.length(); i += 2) {
                    char b = c2bNR.charAt(i);
                    char c = c2bNR.charAt(i + 1);
                    int index = (c >> 8);
                    if (c2bIndex[index] == 0) {
                        c2bIndex[index] = (char)off;
                        off += 0x100;
                    }
                    index = c2bIndex[index] + (c & 0xff);
                    c2b[index] = b;
                }
            }
        }
    }

    public static class Encoder_DBCSONLY extends Encoder {

        public Encoder_DBCSONLY(Charset cs, byte[] repl,
                                char[] c2b, char[] c2bIndex,
                                boolean isASCIICompatible) {
            super(cs, 2.0f, 2.0f, repl, c2b, c2bIndex, isASCIICompatible);
        }

        public int encodeChar(char ch) {
            int bb = super.encodeChar(ch);
            if (bb <= MAX_SINGLEBYTE)
                return UNMAPPABLE_ENCODING;
            return bb;
        }
    }

    public static class Encoder_EBCDIC extends Encoder {
        static final int SBCS = 0;
        static final int DBCS = 1;
        static final byte SO = 0x0e;
        static final byte SI = 0x0f;

        protected int  currentState = SBCS;

        public Encoder_EBCDIC(Charset cs, char[] c2b, char[] c2bIndex,
                              boolean isASCIICompatible) {
            super(cs, 4.0f, 5.0f, new byte[] {(byte)0x6f}, c2b, c2bIndex, isASCIICompatible);
        }

        protected void implReset() {
            currentState = SBCS;
        }

        protected CoderResult implFlush(ByteBuffer out) {
            if (currentState == DBCS) {
                if (out.remaining() < 1)
                    return CoderResult.OVERFLOW;
                out.put(SI);
            }
            implReset();
            return CoderResult.UNDERFLOW;
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
                    int bb = encodeChar(c);
                    if (bb == UNMAPPABLE_ENCODING) {
                        if (Character.isSurrogate(c)) {
                            if (sgp().parse(c, sa, sp, sl) < 0)
                                return sgp.error();
                            return sgp.unmappableResult();
                        }
                        return CoderResult.unmappableForLength(1);
                    }
                    if (bb > MAX_SINGLEBYTE) {  // DoubleByte
                        if (currentState == SBCS) {
                            if (dl - dp < 1)
                                return CoderResult.OVERFLOW;
                            currentState = DBCS;
                            da[dp++] = SO;
                        }
                        if (dl - dp < 2)
                            return CoderResult.OVERFLOW;
                        da[dp++] = (byte)(bb >> 8);
                        da[dp++] = (byte)bb;
                    } else {                    // SingleByte
                        if (currentState == DBCS) {
                            if (dl - dp < 1)
                                return CoderResult.OVERFLOW;
                            currentState = SBCS;
                            da[dp++] = SI;
                        }
                        if (dl - dp < 1)
                            return CoderResult.OVERFLOW;
                        da[dp++] = (byte)bb;

                    }
                    sp++;
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
                    char c = src.get();
                    int bb = encodeChar(c);
                    if (bb == UNMAPPABLE_ENCODING) {
                        if (Character.isSurrogate(c)) {
                            if (sgp().parse(c, src) < 0)
                                return sgp.error();
                            return sgp.unmappableResult();
                        }
                        return CoderResult.unmappableForLength(1);
                    }
                    if (bb > MAX_SINGLEBYTE) {  // DoubleByte
                        if (currentState == SBCS) {
                            if (dst.remaining() < 1)
                                return CoderResult.OVERFLOW;
                            currentState = DBCS;
                            dst.put(SO);
                        }
                        if (dst.remaining() < 2)
                            return CoderResult.OVERFLOW;
                        dst.put((byte)(bb >> 8));
                        dst.put((byte)(bb));
                    } else {                  // Single-byte
                        if (currentState == DBCS) {
                            if (dst.remaining() < 1)
                                return CoderResult.OVERFLOW;
                            currentState = SBCS;
                            dst.put(SI);
                        }
                        if (dst.remaining() < 1)
                            return CoderResult.OVERFLOW;
                        dst.put((byte)bb);
                    }
                    mark++;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        @Override
        public int encode(char[] src, int sp, int len, byte[] dst) {
            int dp = 0;
            int sl = sp + len;
            while (sp < sl) {
                char c = src[sp++];
                int bb = encodeChar(c);

                if (bb == UNMAPPABLE_ENCODING) {
                    if (Character.isHighSurrogate(c) && sp < sl &&
                        Character.isLowSurrogate(src[sp])) {
                        sp++;
                    }
                    dst[dp++] = repl[0];
                    if (repl.length > 1)
                        dst[dp++] = repl[1];
                    continue;
                } //else
                if (bb > MAX_SINGLEBYTE) {           // DoubleByte
                    if (currentState == SBCS) {
                        currentState = DBCS;
                        dst[dp++] = SO;
                    }
                    dst[dp++] = (byte)(bb >> 8);
                    dst[dp++] = (byte)bb;
                } else {                             // SingleByte
                    if (currentState == DBCS) {
                         currentState = SBCS;
                         dst[dp++] = SI;
                    }
                    dst[dp++] = (byte)bb;
                }
            }

            if (currentState == DBCS) {
                 currentState = SBCS;
                 dst[dp++] = SI;
            }
            return dp;
        }

        @Override
        public int encodeFromLatin1(byte[] src, int sp, int len, byte[] dst) {
            int dp = 0;
            int sl = sp + len;
            while (sp < sl) {
                char c = (char)(src[sp++] & 0xff);
                int bb = encodeChar(c);
                if (bb == UNMAPPABLE_ENCODING) {
                    // no surrogate pair in latin1 string
                    dst[dp++] = repl[0];
                    if (repl.length > 1)
                        dst[dp++] = repl[1];
                    continue;
                } //else
                if (bb > MAX_SINGLEBYTE) {           // DoubleByte
                    if (currentState == SBCS) {
                        currentState = DBCS;
                        dst[dp++] = SO;
                    }
                    dst[dp++] = (byte)(bb >> 8);
                    dst[dp++] = (byte)bb;
                } else {                             // SingleByte
                    if (currentState == DBCS) {
                         currentState = SBCS;
                         dst[dp++] = SI;
                    }
                    dst[dp++] = (byte)bb;
                }
            }
            if (currentState == DBCS) {
                 currentState = SBCS;
                 dst[dp++] = SI;
            }
            return dp;
        }

        @Override
        public int encodeFromUTF16(byte[] src, int sp, int len, byte[] dst) {
            int dp = 0;
            int sl = sp + len;
            while (sp < sl) {
                char c = StringUTF16.getChar(src, sp++);
                int bb = encodeChar(c);
                if (bb == UNMAPPABLE_ENCODING) {
                    if (Character.isHighSurrogate(c) && sp < sl &&
                        Character.isLowSurrogate(StringUTF16.getChar(src, sp))) {
                        sp++;
                    }
                    dst[dp++] = repl[0];
                    if (repl.length > 1)
                        dst[dp++] = repl[1];
                    continue;
                } //else
                if (bb > MAX_SINGLEBYTE) {           // DoubleByte
                    if (currentState == SBCS) {
                        currentState = DBCS;
                        dst[dp++] = SO;
                    }
                    dst[dp++] = (byte)(bb >> 8);
                    dst[dp++] = (byte)bb;
                } else {                             // SingleByte
                    if (currentState == DBCS) {
                         currentState = SBCS;
                         dst[dp++] = SI;
                    }
                    dst[dp++] = (byte)bb;
                }
            }
            if (currentState == DBCS) {
                 currentState = SBCS;
                 dst[dp++] = SI;
            }
            return dp;
        }
    }

    // EUC_SIMPLE
    public static class Encoder_EUC_SIM extends Encoder {
        public Encoder_EUC_SIM(Charset cs, char[] c2b, char[] c2bIndex,
                               boolean isASCIICompatible) {
            super(cs, c2b, c2bIndex, isASCIICompatible);
        }
    }

}
