/*
 * Copyright (c) 2002, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import sun.nio.cs.HistoricallyNamedCharset;
import sun.nio.cs.Surrogate;

public class EUC_JP_OLD
    extends Charset
    implements HistoricallyNamedCharset
{
    public EUC_JP_OLD() {
        super("EUC-JP_OLD", null);
    }

    public String historicalName() {
        return "EUC_JP";
    }

    public boolean contains(Charset cs) {
        return ((cs.name().equals("US-ASCII"))
                || (cs instanceof JIS_X_0201_OLD)
                || (cs instanceof JIS_X_0208_OLD)
                || (cs instanceof JIS_X_0212_OLD)
                || (cs instanceof EUC_JP_OLD));
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {

        // Need to force the replacement byte to 0x3f
        // because JIS_X_0208_Encoder defines its own
        // alternative 2 byte substitution to permit it
        // to exist as a self-standing Encoder

        byte[] replacementBytes = { (byte)0x3f };
        return new Encoder(this).replaceWith(replacementBytes);
    }


    static class Decoder extends JIS_X_0208_Decoder {

        JIS_X_0201_OLD.Decoder decoderJ0201;
        JIS_X_0212_Decoder decoderJ0212;

        private static final short[] j0208Index1 =
          JIS_X_0208_Decoder.getIndex1();
        private static final String[] j0208Index2 =
          JIS_X_0208_Decoder.getIndex2();

        protected Decoder(Charset cs) {
            super(cs);
            decoderJ0201 = new JIS_X_0201_OLD.Decoder(cs);
            decoderJ0212 = new JIS_X_0212_Decoder(cs);
            start = 0xa1;
            end = 0xfe;
        }
        protected char decode0212(int byte1, int byte2) {
             return decoderJ0212.decodeDouble(byte1, byte2);
        }

        protected char decodeDouble(int byte1, int byte2) {
            if (byte1 == 0x8e) {
                return decoderJ0201.decode(byte2 - 256);
            }
            // Fix for bug 4121358 - similar fix for bug 4117820 put
            // into ByteToCharDoubleByte.getUnicode()
            if (((byte1 < 0) || (byte1 > getIndex1().length))
                || ((byte2 < start) || (byte2 > end)))
                return REPLACE_CHAR;

            int n = (j0208Index1[byte1 - 0x80] & 0xf) * (end - start + 1)
                    + (byte2 - start);
            return j0208Index2[j0208Index1[byte1 - 0x80] >> 4].charAt(n);
        }

        private CoderResult decodeArrayLoop(ByteBuffer src,
                                            CharBuffer dst)
        {
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

            int b1 = 0, b2 = 0;
            int inputSize = 0;
            char outputChar = REPLACE_CHAR; // U+FFFD;

            try {
                while (sp < sl) {
                    b1 = sa[sp] & 0xff;
                    inputSize = 1;

                    if ((b1 & 0x80) == 0) {
                        outputChar = (char)b1;
                    }
                    else {      // Multibyte char
                        if ((b1 & 0xff) == 0x8f) {   // JIS0212
                            if (sp + 3 > sl)
                               return CoderResult.UNDERFLOW;
                            b1 = sa[sp + 1] & 0xff;
                            b2 = sa[sp + 2] & 0xff;
                            inputSize += 2;
                            outputChar = decode0212(b1-0x80, b2-0x80);
                        } else {
                          // JIS0208
                            if (sp + 2 > sl)
                               return CoderResult.UNDERFLOW;
                            b2 = sa[sp + 1] & 0xff;
                            inputSize++;
                            outputChar = decodeDouble(b1, b2);
                        }
                    }
                    if (outputChar == REPLACE_CHAR) { // can't be decoded
                        return CoderResult.unmappableForLength(inputSize);
                    }
                    if (dp + 1 > dl)
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

        private CoderResult decodeBufferLoop(ByteBuffer src,
                                             CharBuffer dst)
        {
            int mark = src.position();
            int b1 = 0, b2 = 0;
            int inputSize = 0;

            char outputChar = REPLACE_CHAR; // U+FFFD;

            try {
                while (src.hasRemaining()) {
                    b1 = src.get() & 0xff;
                    inputSize = 1;

                    if ((b1 & 0x80) == 0) {
                        outputChar = (char)b1;
                    } else {    // Multibyte char
                        if ((b1 & 0xff) == 0x8f) {   // JIS0212
                            if (src.remaining() < 2)
                               return CoderResult.UNDERFLOW;
                            b1 = src.get() & 0xff;
                            b2 = src.get() & 0xff;
                            inputSize += 2;
                            outputChar = decode0212(b1-0x80, b2-0x80);
                        } else {
                          // JIS0208
                            if (src.remaining() < 1)
                               return CoderResult.UNDERFLOW;
                            b2 = src.get() & 0xff;
                            inputSize++;
                            outputChar = decodeDouble(b1, b2);
                        }
                    }

                    if (outputChar == REPLACE_CHAR) {
                        return CoderResult.unmappableForLength(inputSize);
                    }
                if (dst.remaining() < 1)
                    return CoderResult.OVERFLOW;
                dst.put(outputChar);
                mark += inputSize;
                }
                return CoderResult.UNDERFLOW;
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
        public void implReset() {
            super.implReset();
        }
        public CoderResult implFlush(CharBuffer out) {
            return super.implFlush(out);
        }
    }


    static class Encoder extends JIS_X_0208_Encoder {

        JIS_X_0201_OLD.Encoder encoderJ0201;
        JIS_X_0212_Encoder encoderJ0212;

        private static final short[] j0208Index1 =
          JIS_X_0208_Encoder.getIndex1();
        private static final String[] j0208Index2 =
          JIS_X_0208_Encoder.getIndex2();

        private final Surrogate.Parser sgp = new Surrogate.Parser();

        protected Encoder(Charset cs) {
            super(cs, 3.0f, 3.0f);
            encoderJ0201 = new JIS_X_0201_OLD.Encoder(cs);
            encoderJ0212 = new JIS_X_0212_Encoder(cs);
        }

        public boolean canEncode(char c) {
            byte[]  encodedBytes = new byte[3];

            if (encodeSingle(c, encodedBytes) == 0) { //doublebyte
                if (encodeDouble(c) == 0)
                    return false;
            }
            return true;
        }

        protected int encodeSingle(char inputChar, byte[] outputByte) {
            byte b;

            if (inputChar == 0) {
                outputByte[0] = (byte)0;
                return 1;
            }

            if ((b = encoderJ0201.encode(inputChar)) == 0)
                return 0;

            if (b > 0 && b < 128) {
                outputByte[0] = b;
                return 1;
            }

            outputByte[0] = (byte)0x8e;
            outputByte[1] = b;
            return 2;
        }

        protected int encodeDouble(char ch) {
            int offset = j0208Index1[((ch & 0xff00) >> 8 )] << 8;
            int r = j0208Index2[offset >> 12].charAt((offset & 0xfff) +
                    (ch & 0xff));
            if (r != 0)
                return r + 0x8080;
            r = encoderJ0212.encodeDouble(ch);
            if (r == 0)
                return r;
            return r + 0x8F8080;
        }

        private CoderResult encodeArrayLoop(CharBuffer src,
                                            ByteBuffer dst)
        {
            char[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();
            assert (sp <= sl);
            sp = (sp <= sl ? sp : sl);
            byte[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();
            assert (dp <= dl);
            dp = (dp <= dl ? dp : dl);

            int outputSize = 0;
            byte[]  outputByte;
            int     inputSize = 0;                 // Size of input
            byte[]  tmpBuf = new byte[3];

            try {
                while (sp < sl) {
                    outputByte = tmpBuf;
                    char c = sa[sp];

                    if (Character.isSurrogate(c)) {
                        if (sgp.parse(c, sa, sp, sl) < 0)
                            return sgp.error();
                        return sgp.unmappableResult();
                    }

                    outputSize = encodeSingle(c, outputByte);

                    if (outputSize == 0) { // DoubleByte
                        int ncode = encodeDouble(c);
                        if (ncode != 0 ) {
                            if ((ncode & 0xFF0000) == 0) {
                                outputByte[0] = (byte) ((ncode & 0xff00) >> 8);
                                outputByte[1] = (byte) (ncode & 0xff);
                                outputSize = 2;
                            } else {
                                outputByte[0] = (byte) 0x8f;
                                outputByte[1] = (byte) ((ncode & 0xff00) >> 8);
                                outputByte[2] = (byte) (ncode & 0xff);
                                outputSize = 3;
                            }
                        } else {
                                return CoderResult.unmappableForLength(1);
                        }
                    }
                    if (dl - dp < outputSize)
                        return CoderResult.OVERFLOW;
                    // Put the byte in the output buffer
                    for (int i = 0; i < outputSize; i++) {
                        da[dp++] = outputByte[i];
                    }
                    sp++;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
            }
        }

        private CoderResult encodeBufferLoop(CharBuffer src,
                                             ByteBuffer dst)
        {
            int outputSize = 0;
            byte[]  outputByte;
            int     inputSize = 0;                 // Size of input
            byte[]  tmpBuf = new byte[3];

            int mark = src.position();

            try {
                while (src.hasRemaining()) {
                    outputByte = tmpBuf;
                    char c = src.get();
                    if (Character.isSurrogate(c)) {
                        if (sgp.parse(c, src) < 0)
                            return sgp.error();
                        return sgp.unmappableResult();
                    }

                    outputSize = encodeSingle(c, outputByte);
                    if (outputSize == 0) { // DoubleByte
                        int ncode = encodeDouble(c);
                        if (ncode != 0 ) {
                            if ((ncode & 0xFF0000) == 0) {
                                outputByte[0] = (byte) ((ncode & 0xff00) >> 8);
                                outputByte[1] = (byte) (ncode & 0xff);
                                outputSize = 2;
                            } else {
                                outputByte[0] = (byte) 0x8f;
                                outputByte[1] = (byte) ((ncode & 0xff00) >> 8);
                                outputByte[2] = (byte) (ncode & 0xff);
                                outputSize = 3;
                            }
                        } else {
                                return CoderResult.unmappableForLength(1);
                        }
                    }

                    if (dst.remaining() < outputSize)
                        return CoderResult.OVERFLOW;
                    // Put the byte in the output buffer
                    for (int i = 0; i < outputSize; i++) {
                        dst.put(outputByte[i]);
                    }
                    mark++;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        protected CoderResult encodeLoop(CharBuffer src,
                                         ByteBuffer dst)
        {
            if (src.hasArray() && dst.hasArray())
                return encodeArrayLoop(src, dst);
            else
                return encodeBufferLoop(src, dst);
        }
    }
}
