/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.cs.ext;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CharsetDecoder;
import sun.nio.cs.DoubleByte;
import sun.nio.cs.*;
import static sun.nio.cs.CharsetMapping.*;

public class MS932_0213 extends Charset {
    public MS932_0213() {
        super("x-MS932_0213", ExtendedCharsets.aliasesFor("x-MS932_0213"));
    }

    public boolean contains(Charset cs) {
        return ((cs.name().equals("US-ASCII"))
                || (cs instanceof MS932)
                || (cs instanceof MS932_0213));
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    protected static class Decoder extends SJIS_0213.Decoder {
        static final DoubleByte.Decoder decMS932 =
            (DoubleByte.Decoder)new MS932().newDecoder();
        protected Decoder(Charset cs) {
            super(cs);
        }

        protected char decodeDouble(int b1, int b2) {
            char c = decMS932.decodeDouble(b1, b2);
            if (c == UNMAPPABLE_DECODING)
                return super.decodeDouble(b1, b2);
            return c;
        }
    }

    protected static class Encoder extends SJIS_0213.Encoder {
        // we only use its encodeChar() method
        static final DoubleByte.Encoder encMS932 =
            (DoubleByte.Encoder)new MS932().newEncoder();
        protected Encoder(Charset cs) {
            super(cs);
        }

        protected int encodeChar(char ch) {
            int db = encMS932.encodeChar(ch);
            if (db == UNMAPPABLE_ENCODING)
                return super.encodeChar(ch);
            return db;
        }
    }
}
