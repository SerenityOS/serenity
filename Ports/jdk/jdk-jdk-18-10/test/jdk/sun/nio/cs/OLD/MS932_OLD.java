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


import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import sun.nio.cs.HistoricallyNamedCharset;
import sun.nio.cs.ext.*;

public class MS932_OLD extends Charset implements HistoricallyNamedCharset
{
    public MS932_OLD() {
        super("windows-31j-OLD", null);
    }

    public String historicalName() {
        return "MS932";
    }

    public boolean contains(Charset cs) {
        return ((cs.name().equals("US-ASCII"))
                || (cs instanceof JIS_X_0201_OLD)
                || (cs instanceof MS932_OLD));
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    private static class Decoder extends MS932DB.Decoder
                                         //        implements DelegatableDecoder
    {

        JIS_X_0201_OLD.Decoder jisDec0201;

        private Decoder(Charset cs) {
            super(cs);
            jisDec0201 = new JIS_X_0201_OLD.Decoder(cs);
        }

        protected char decodeSingle(int b) {
            // If the high bits are all off, it's ASCII == Unicode
            if ((b & 0xFF80) == 0) {
                return (char)b;
            }
            return jisDec0201.decode(b);
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

    private static class Encoder extends MS932DB.Encoder {

        private JIS_X_0201_OLD.Encoder jisEnc0201;


        private Encoder(Charset cs) {
            super(cs);
            jisEnc0201 = new JIS_X_0201_OLD.Encoder(cs);
        }

        protected int encodeSingle(char inputChar) {

            byte b;
            // \u0000 - \u007F map straight through
            if ((inputChar & 0xFF80) == 0) {
                return ((byte)inputChar);
            }

            if ((b = jisEnc0201.encode(inputChar)) == 0)
                return -1;
            else
                return b;
        }
    }
}
