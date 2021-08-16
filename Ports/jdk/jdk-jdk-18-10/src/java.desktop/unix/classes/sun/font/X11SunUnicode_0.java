/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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


package sun.font;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CharsetDecoder;

public class X11SunUnicode_0 extends Charset {
    public X11SunUnicode_0 () {
        super("X11SunUnicode_0", null);
    }

    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    /* Seems like supporting a decoder is required, but we aren't going
     * to be publically exposing this class, so no need to waste work
     */
    public CharsetDecoder newDecoder() {
        throw new Error("Decoder is not implemented for X11SunUnicode_0 Charset");
    }

    public boolean contains(Charset cs) {
        return cs instanceof X11SunUnicode_0;
    }

    private static class Encoder extends DoubleByteEncoder {
        public Encoder(Charset cs) {
            super(cs, index1, index2);
        }

        private static final String innerIndex0=
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0120\u0121\u0122\u0000\u0123\u0124\u0125"+
            "\u0126\u0127\u0128\u0129\u012A\u012B\u012C\u012D"+
            "\u012E\u012F\u0130\u0131\u0132\u0133\u0134\u0135"+
            "\u0136\u0137\u0138\u0139\u013A\u013B\u013C\u013D"+
            "\u013E\u013F\u0140\u0141\u0142\u0143\u0144\u0145"+
            "\u0146\u0147\u0148\u0149\u014A\u014B\u014C\u014D"+
            "\u014E\u014F\u0150\u0151\u0152\u0153\u0154\u0155"+
            "\u0156\u0157\u0000\u0000\u0158\u0159\u015A\u015B"+
            "\u015C\u015D\u015E\u015F\u0160\u0161\u0162\u0163"+
            "\u0164\u0165\u0166\u0167\u0168\u0169\u0000\u0000"+
            "\u016A\u016B\u016C\u016D\u016E\u0000\u0000\u0000"+
            "\u016F\u0170\u0171\u0172\u0173\u0174\u0175\u0176"+
            "\u0177\u0178\u0179\u017A\u017B\u017C\u017D\u017E"+
            "\u017F\u0180\u0181\u0182\u0183\u0184\u0185\u0186"+
            "\u0187\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"+
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000";

        private static final short[] index1 = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };

        private static final String[] index2 = {
            innerIndex0
        };

        /* The default implementation creates a decoder and we don't have one */
        public boolean isLegalReplacement(byte[] repl) {
            return true;
        }
    }
}
