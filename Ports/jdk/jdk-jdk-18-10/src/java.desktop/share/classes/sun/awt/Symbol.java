/*
 * Copyright (c) 1997, 2005, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;

public class Symbol extends Charset {
    public Symbol () {
        super("Symbol", null);
    }
    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    /* Seems like supporting a decoder is required, but we aren't going
     * to be publically exposing this class, so no need to waste work
     */
    public CharsetDecoder newDecoder() {
        throw new Error("Decoder is not implemented for Symbol Charset");
    }

    public boolean contains(Charset cs) {
        return cs instanceof Symbol;
    }

    private static class Encoder extends CharsetEncoder {
        public Encoder(Charset cs) {
            super(cs, 1.0f, 1.0f);
        }

        public boolean canEncode(char c) {
            if (c >= 0x2200 && c <= 0x22ef) {
                if (table_math[c - 0x2200] != 0x00) {
                    return true;
                }
            } else if (c >= 0x0391 && c <= 0x03d6) {
                if (table_greek[c - 0x0391] != 0x00) {
                    return true;
                }
            }
            return false;
        }

        protected CoderResult encodeLoop(CharBuffer src, ByteBuffer dst) {
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

            try {
                while (sp < sl) {
                    char c = sa[sp];
                    if (dl - dp < 1)
                        return CoderResult.OVERFLOW;
                    if (!canEncode(c))
                        return CoderResult.unmappableForLength(1);
                    sp++;
                    if (c >= 0x2200 && c <= 0x22ef){
                        da[dp++] = table_math[c - 0x2200];
                    } else if (c >= 0x0391 && c <= 0x03d6) {
                        da[dp++]= table_greek[c - 0x0391];
                    }
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
            }
        }

        private static byte[] table_math = {
            (byte)0042, (byte)0000, (byte)0144, (byte)0044,
            (byte)0000, (byte)0306, (byte)0104, (byte)0321,    // 00
            (byte)0316, (byte)0317, (byte)0000, (byte)0000,
            (byte)0000, (byte)0047, (byte)0000, (byte)0120,
            (byte)0000, (byte)0345, (byte)0055, (byte)0000,
            (byte)0000, (byte)0244, (byte)0000, (byte)0052,    // 10
            (byte)0260, (byte)0267, (byte)0326, (byte)0000,
            (byte)0000, (byte)0265, (byte)0245, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0275,
            (byte)0000, (byte)0000, (byte)0000, (byte)0331,    // 20
            (byte)0332, (byte)0307, (byte)0310, (byte)0362,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0134, (byte)0000, (byte)0000, (byte)0000,    // 30
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0176, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0100, (byte)0000, (byte)0000,    // 40
            (byte)0273, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,    // 50
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0271, (byte)0272, (byte)0000, (byte)0000,
            (byte)0243, (byte)0263, (byte)0000, (byte)0000,    // 60
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,    // 70
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0314, (byte)0311,
            (byte)0313, (byte)0000, (byte)0315, (byte)0312,    // 80
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0305, (byte)0000, (byte)0304,    // 90
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0136, (byte)0000, (byte)0000,    // a0
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,    // b0
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0340, (byte)0327, (byte)0000, (byte)0000,    // c0
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,    // d0
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,    // e0
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0274,
        };

        private static byte[] table_greek = {
            (byte)0101, (byte)0102, (byte)0107,
            (byte)0104, (byte)0105, (byte)0132, (byte)0110,    // 90
            (byte)0121, (byte)0111, (byte)0113, (byte)0114,
            (byte)0115, (byte)0116, (byte)0130, (byte)0117,
            (byte)0120, (byte)0122, (byte)0000, (byte)0123,
            (byte)0124, (byte)0125, (byte)0106, (byte)0103,    // a0
            (byte)0131, (byte)0127, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0141, (byte)0142, (byte)0147,
            (byte)0144, (byte)0145, (byte)0172, (byte)0150,    // b0
            (byte)0161, (byte)0151, (byte)0153, (byte)0154,
            (byte)0155, (byte)0156, (byte)0170, (byte)0157,
            (byte)0160, (byte)0162, (byte)0126, (byte)0163,
            (byte)0164, (byte)0165, (byte)0146, (byte)0143,    // c0
            (byte)0171, (byte)0167, (byte)0000, (byte)0000,
            (byte)0000, (byte)0000, (byte)0000, (byte)0000,
            (byte)0000, (byte)0112, (byte)0241, (byte)0000,
            (byte)0000, (byte)0152, (byte)0166,                // d0
        };

        /* The default implementation creates a decoder and we don't have one */
        public boolean isLegalReplacement(byte[] repl) {
            return true;
        }
    }
}
