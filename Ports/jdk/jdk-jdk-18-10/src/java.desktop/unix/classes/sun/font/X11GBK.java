/*
 * Copyright (c) 1999, 2005, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.charset.*;
import sun.nio.cs.*;
import static sun.nio.cs.CharsetMapping.*;

public class X11GBK extends Charset {
    public X11GBK () {
        super("X11GBK", null);
    }
    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }
    public CharsetDecoder newDecoder() {
        return new GBK().newDecoder();
    }

    public boolean contains(Charset cs) {
        return cs instanceof X11GBK;
    }

    private class Encoder extends DoubleByte.Encoder {

        private DoubleByte.Encoder enc = (DoubleByte.Encoder)new GBK().newEncoder();

        Encoder(Charset cs) {
            super(cs, (char[])null, (char[])null);
        }

        public boolean canEncode(char ch){
            if (ch < 0x80) return false;
            return enc.canEncode(ch);
        }

        public int encodeChar(char ch) {
            if (ch < 0x80)
                return UNMAPPABLE_ENCODING;
            return enc.encodeChar(ch);
        }
    }
}
