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

public class SJIS_OLD
    extends Charset
    implements HistoricallyNamedCharset
{

    public SJIS_OLD() {
        super("Shift_JIS_OLD", null);
    }

    public String historicalName() {
        return "SJIS";
    }

    public boolean contains(Charset cs) {
        return ((cs.name().equals("US-ASCII"))
                || (cs instanceof JIS_X_0201_OLD)
                || (cs instanceof SJIS_OLD)
                || (cs instanceof JIS_X_0208_OLD));
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

        JIS_X_0201_OLD.Decoder jis0201;

        protected Decoder(Charset cs) {
            super(cs);
            jis0201 = new JIS_X_0201_OLD.Decoder(cs);
        }

        protected char decodeSingle(int b) {
            // If the high bits are all off, it's ASCII == Unicode
            if ((b & 0xFF80) == 0) {
                return (char)b;
            }
            return jis0201.decode(b);
        }

        protected char decodeDouble(int c1, int c2) {
            int adjust = c2 < 0x9F ? 1 : 0;
            int rowOffset = c1 < 0xA0 ? 0x70 : 0xB0;
            int cellOffset = (adjust == 1) ? (c2 > 0x7F ? 0x20 : 0x1F) : 0x7E;
            int b1 = ((c1 - rowOffset) << 1) - adjust;
            int b2 = c2 - cellOffset;
            return super.decodeDouble(b1, b2);
        }

        // Make some protected methods public for use by JISAutoDetect
        public CoderResult decodeLoop(ByteBuffer src, CharBuffer dst) {
            return super.decodeLoop(src, dst);
        }
        public void implReset() {
            super.implReset();
        }
        public CoderResult implFlush(CharBuffer out) {
            return super.implFlush(out);
        }
    }

    static class Encoder extends JIS_X_0208_Encoder {

        private JIS_X_0201_OLD.Encoder jis0201;

        private static final short[] j0208Index1 =
            JIS_X_0208_Encoder.getIndex1();
        private static final String[] j0208Index2 =
            JIS_X_0208_Encoder.getIndex2();

        protected Encoder(Charset cs) {
            super(cs);
            jis0201 = new JIS_X_0201_OLD.Encoder(cs);
        }

        protected int encodeSingle(char inputChar) {
            byte b;

            // \u0000 - \u007F map straight through
            if ((inputChar & 0xFF80) == 0)
                return (byte)inputChar;

            if ((b = jis0201.encode(inputChar)) == 0)
                return -1;
            else
                return b;
        }

        protected int encodeDouble(char ch) {
            int offset = j0208Index1[ch >> 8] << 8;
            int pos = j0208Index2[offset >> 12].charAt((offset & 0xfff) + (ch & 0xff));
            if (pos == 0) {
                /* Zero value indicates this Unicode has no mapping to
                 * JIS0208.
                 * We bail here because the JIS -> SJIS algorithm produces
                 * bogus SJIS values for invalid JIS input.  Zero should be
                 * the only invalid JIS value in our table.
                 */
                return 0;
            }
            /*
             * This algorithm for converting from JIS to SJIS comes from
             * Ken Lunde's "Understanding Japanese Information Processing",
             * pg 163.
             */
            int c1 = (pos >> 8) & 0xff;
            int c2 = pos & 0xff;
            int rowOffset = c1 < 0x5F ? 0x70 : 0xB0;
            int cellOffset = (c1 % 2 == 1) ? (c2 > 0x5F ? 0x20 : 0x1F) : 0x7E;
            return ((((c1 + 1 ) >> 1) + rowOffset) << 8) | (c2 + cellOffset);
        }
    }
}
