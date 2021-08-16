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

package sun.nio.cs.ext;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import sun.nio.cs.DoubleByte;
import sun.nio.cs.HistoricallyNamedCharset;
import sun.nio.cs.*;

public class Big5_HKSCS_2001 extends Charset
{
    public Big5_HKSCS_2001() {
        super("x-Big5-HKSCS-2001", ExtendedCharsets.aliasesFor("x-Big5-HKSCS-2001"));
    }

    public boolean contains(Charset cs) {
        return ((cs.name().equals("US-ASCII"))
                || (cs instanceof Big5)
                || (cs instanceof Big5_HKSCS_2001));
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    private static class Decoder extends HKSCS.Decoder {
        private static DoubleByte.Decoder big5 =
            (DoubleByte.Decoder)new Big5().newDecoder();

        private static final char[][] b2cBmp = new char[0x100][];
        private static final char[][] b2cSupp = new char[0x100][];
        static {
            initb2c(b2cBmp, HKSCS2001Mapping.b2cBmpStr);
            initb2c(b2cSupp, HKSCS2001Mapping.b2cSuppStr);
        }

        private Decoder(Charset cs) {
            super(cs, big5, b2cBmp, b2cSupp);
        }
    }

    private static class Encoder extends HKSCS.Encoder {
        private static DoubleByte.Encoder big5 =
            (DoubleByte.Encoder)new Big5().newEncoder();

        static final char[][] c2bBmp = new char[0x100][];
        static final char[][] c2bSupp = new char[0x100][];
        static {
            initc2b(c2bBmp, HKSCS2001Mapping.b2cBmpStr,
                    HKSCS2001Mapping.pua);
            initc2b(c2bSupp, HKSCS2001Mapping.b2cSuppStr, null);
        }

        private Encoder(Charset cs) {
            super(cs, big5, c2bBmp, c2bSupp);
        }
    }
}
