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

import java.nio.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import sun.nio.cs.HistoricallyNamedCharset;
import sun.nio.cs.Surrogate;

public class EUC_JP_LINUX_OLD
    extends Charset
    implements HistoricallyNamedCharset
{
    public EUC_JP_LINUX_OLD() {
        super("x-euc-jp-linux_OLD", null);
    }

    public String historicalName() {
        return "EUC_JP_LINUX";
    }

    public boolean contains(Charset cs) {
        return ((cs instanceof JIS_X_0201_OLD)
               || (cs.name().equals("US-ASCII"))
               || (cs instanceof EUC_JP_LINUX_OLD));
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    private static class Decoder extends CharsetDecoder {
        JIS_X_0201_OLD.Decoder decoderJ0201;
        protected final char REPLACE_CHAR='\uFFFD';

        private static final int start = 0xa1;
        private static final int end = 0xfe;
        private static final short[] jis0208Index1 =
            JIS_X_0208_Decoder.getIndex1();
        private static final String[] jis0208Index2 =
            JIS_X_0208_Decoder.getIndex2();

        private Decoder(Charset cs) {
            super(cs, 1.0f, 1.0f);
            decoderJ0201 = new JIS_X_0201_OLD.Decoder(cs);
        }

        protected char convSingleByte(int b) {
            if (b < 0 || b > 0x7f)
                return REPLACE_CHAR;
            return decoderJ0201.decode(b);
        }

        protected char decodeDouble(int byte1, int byte2) {
            if (byte1 == 0x8e) {
                return decoderJ0201.decode(byte2 - 256);
            }

            if (((byte1 < 0) || (byte1 > jis0208Index1.length))
                || ((byte2 < start) || (byte2 > end)))
                return REPLACE_CHAR;

            int n = (jis0208Index1[byte1 - 0x80] & 0xf) * (end - start + 1)
                    + (byte2 - start);
            return jis0208Index2[jis0208Index1[byte1 - 0x80] >> 4].charAt(n);
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
                            inputSize = 3;
                            return CoderResult.unmappableForLength(inputSize); // substitute
                        } else {
                          // JIS0208
                            if (sp + 2 > sl)
                               return CoderResult.UNDERFLOW;
                            b2 = sa[sp + 1] & 0xff;
                            inputSize = 2;
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
            char outputChar = REPLACE_CHAR; // U+FFFD;

            try {
                while (src.hasRemaining()) {
                    int b1 = src.get() & 0xff;
                    int inputSize = 1;

                    if ((b1 & 0x80) == 0) {
                        outputChar = (char)b1;
                    } else {    // Multibyte char

                        if ((b1 & 0xff) == 0x8f) { // JIS0212 not supported
                            if (src.remaining() < 2)
                                return CoderResult.UNDERFLOW;
                            return CoderResult.unmappableForLength(3);
                        } else {
                            // JIS0208
                            if (src.remaining() < 1)
                                return CoderResult.UNDERFLOW;
                            int b2 = src.get() & 0xff;
                            inputSize++;
                            outputChar = decodeDouble(b1, b2);
                        }
                    }

                    if (outputChar == REPLACE_CHAR)
                        return CoderResult.unmappableForLength(inputSize);
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

        protected CoderResult decodeLoop(ByteBuffer src,
                                         CharBuffer dst)
        {
            if (src.hasArray() && dst.hasArray())
                return decodeArrayLoop(src, dst);
            else
                return decodeBufferLoop(src, dst);
        }
    }


    private static class Encoder extends CharsetEncoder {

        JIS_X_0201_OLD.Encoder encoderJ0201;

        private final Surrogate.Parser sgp = new Surrogate.Parser();
        private static final short[] jis0208Index1 =
            JIS_X_0208_Encoder.getIndex1();
        private static final String[] jis0208Index2 =
            JIS_X_0208_Encoder.getIndex2();

        private Encoder(Charset cs) {
            super(cs, 2.0f, 2.0f);
            encoderJ0201 = new JIS_X_0201_OLD.Encoder(cs);
        }

        public boolean canEncode(char c) {
            byte[]  encodedBytes = new byte[2];

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
            int offset = jis0208Index1[((ch & 0xff00) >> 8 )] << 8;
            int r = jis0208Index2[offset >> 12].charAt((offset & 0xfff) + (ch & 0xff));
            if (r != 0)
                return r + 0x8080;
            return r;
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

            final byte[]  outputByte = new byte[2];

            try {
                while (sp < sl) {
                    char c = sa[sp];

                    if (Character.isSurrogate(c)) {
                        if (sgp.parse(c, sa, sp, sl) < 0)
                            return sgp.error();
                        return sgp.unmappableResult();
                    }

                    int outputSize = encodeSingle(c, outputByte);
                    if (outputSize == 0) { // DoubleByte
                        int ncode = encodeDouble(c);
                        if (ncode != 0 && ((ncode & 0xFF0000) == 0)) {
                                outputByte[0] = (byte) ((ncode & 0xff00) >> 8);
                                outputByte[1] = (byte) (ncode & 0xff);
                                outputSize = 2;
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
            final byte[]  outputByte = new byte[4];
            int mark = src.position();

            try {
                while (src.hasRemaining()) {
                    char c = src.get();
                    if (Character.isSurrogate(c)) {
                        if (sgp.parse(c, src) < 0)
                            return sgp.error();
                        return sgp.unmappableResult();
                    }

                    int outputSize = encodeSingle(c, outputByte);
                    if (outputSize == 0) { // DoubleByte
                        int ncode = encodeDouble(c);
                        if (ncode != 0 ) {
                            if ((ncode & 0xFF0000) == 0) {
                                outputByte[0] = (byte) ((ncode & 0xff00) >> 8);
                                outputByte[1] = (byte) (ncode & 0xff);
                                outputSize = 2;
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
