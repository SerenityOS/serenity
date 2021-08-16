/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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

public class EUC_JP_Open_OLD
    extends Charset
    implements HistoricallyNamedCharset
{
    public EUC_JP_Open_OLD() {
        super("x-eucJP-Open_OLD", null);
    }

    public String historicalName() {
        return "EUC_JP_Solaris";
    }

    public boolean contains(Charset cs) {
        return ((cs.name().equals("US-ASCII"))
                || (cs instanceof JIS_X_0201_OLD)
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

    private static class Decoder extends EUC_JP_OLD.Decoder {
        JIS_X_0201_OLD.Decoder decoderJ0201;
        JIS_X_0212_Solaris_Decoder decodeMappingJ0212;
        JIS_X_0208_Solaris_Decoder decodeMappingJ0208;

        private static final short[] j0208Index1 =
          JIS_X_0208_Solaris_Decoder.getIndex1();
        private static final String[] j0208Index2 =
          JIS_X_0208_Solaris_Decoder.getIndex2();
        private static final int start = 0xa1;
        private static final int end = 0xfe;

        protected final char REPLACE_CHAR='\uFFFD';

        private Decoder(Charset cs) {
            super(cs);
            decoderJ0201 = new JIS_X_0201_OLD.Decoder(cs);
            decodeMappingJ0212 = new JIS_X_0212_Solaris_Decoder(cs);
        }


        protected char decode0212(int byte1, int byte2) {
             return decodeMappingJ0212.decodeDouble(byte1, byte2);

        }

        protected char decodeDouble(int byte1, int byte2) {
            if (byte1 == 0x8e) {
                return decoderJ0201.decode(byte2 - 256);
            }

            if (((byte1 < 0)
                || (byte1 > j0208Index1.length))
                || ((byte2 < start)
                || (byte2 > end)))
                return REPLACE_CHAR;

            char result = super.decodeDouble(byte1, byte2);
            if (result != '\uFFFD') {
                return result;
            } else {
                int n = (j0208Index1[byte1 - 0x80] & 0xf) *
                        (end - start + 1)
                        + (byte2 - start);
                return j0208Index2[j0208Index1[byte1 - 0x80] >> 4].charAt(n);
            }
        }
    }


    private static class Encoder extends EUC_JP_OLD.Encoder {

        JIS_X_0201_OLD.Encoder encoderJ0201;
        JIS_X_0212_Solaris_Encoder encoderJ0212;

        private static final short[] j0208Index1 =
            JIS_X_0208_Solaris_Encoder.getIndex1();
        private static final String[] j0208Index2 =
            JIS_X_0208_Solaris_Encoder.getIndex2();

        private final Surrogate.Parser sgp = new Surrogate.Parser();

        private Encoder(Charset cs) {
            super(cs);
            encoderJ0201 = new JIS_X_0201_OLD.Encoder(cs);
            encoderJ0212 = new JIS_X_0212_Solaris_Encoder(cs);
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
            int r = super.encodeDouble(ch);
            if (r != 0) {
                return r;
            }
            else {
                int offset = j0208Index1[((ch & 0xff00) >> 8 )] << 8;
                r = j0208Index2[offset >> 12].charAt((offset & 0xfff) +
                    (ch & 0xFF));
                if (r > 0x7500)
                   return 0x8F8080 + encoderJ0212.encodeDouble(ch);
                }
                return (r==0 ? 0: r + 0x8080);
        }
    }
}
