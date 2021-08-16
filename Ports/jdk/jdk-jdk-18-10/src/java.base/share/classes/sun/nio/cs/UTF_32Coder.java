/*
 * Copyright (c) 2005, 2010, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.cs;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CoderResult;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

class UTF_32Coder {
    protected static final int BOM_BIG = 0xFEFF;
    protected static final int BOM_LITTLE = 0xFFFE0000;
    protected static final int NONE = 0;
    protected static final int BIG = 1;
    protected static final int LITTLE = 2;

    protected static class Decoder extends CharsetDecoder {
        private int currentBO;
        private int expectedBO;

        protected Decoder(Charset cs, int bo) {
            super(cs, 0.25f, 1.0f);
            this.expectedBO = bo;
            this.currentBO = NONE;
        }

        private int getCP(ByteBuffer src) {
            return (currentBO==BIG)
              ?(((src.get() & 0xff) << 24) |
                ((src.get() & 0xff) << 16) |
                ((src.get() & 0xff) <<  8) |
                (src.get() & 0xff))
              :((src.get() & 0xff) |
                ((src.get() & 0xff) <<  8) |
                ((src.get() & 0xff) << 16) |
                ((src.get() & 0xff) << 24));
        }

        protected CoderResult decodeLoop(ByteBuffer src, CharBuffer dst) {
            if (src.remaining() < 4)
                return CoderResult.UNDERFLOW;
            int mark = src.position();
            int cp;
            try {
                if (currentBO == NONE) {
                    cp = ((src.get() & 0xff) << 24) |
                         ((src.get() & 0xff) << 16) |
                         ((src.get() & 0xff) <<  8) |
                         (src.get() & 0xff);
                    if (cp == BOM_BIG && expectedBO != LITTLE) {
                        currentBO = BIG;
                        mark += 4;
                    } else if (cp == BOM_LITTLE && expectedBO != BIG) {
                        currentBO = LITTLE;
                        mark += 4;
                    } else {
                        if (expectedBO == NONE)
                            currentBO = BIG;
                        else
                            currentBO = expectedBO;
                        src.position(mark);
                    }
                }
                while (src.remaining() >= 4) {
                    cp = getCP(src);
                    if (Character.isBmpCodePoint(cp)) {
                        if (!dst.hasRemaining())
                            return CoderResult.OVERFLOW;
                        mark += 4;
                        dst.put((char) cp);
                    } else if (Character.isValidCodePoint(cp)) {
                        if (dst.remaining() < 2)
                            return CoderResult.OVERFLOW;
                        mark += 4;
                        dst.put(Character.highSurrogate(cp));
                        dst.put(Character.lowSurrogate(cp));
                    } else {
                        return CoderResult.malformedForLength(4);
                    }
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }
        protected void implReset() {
            currentBO = NONE;
        }
    }

    protected static class Encoder extends CharsetEncoder {
        private boolean doBOM = false;
        private boolean doneBOM = true;
        private int byteOrder;

        protected void put(int cp, ByteBuffer dst) {
            if (byteOrder==BIG) {
                dst.put((byte)(cp >> 24));
                dst.put((byte)(cp >> 16));
                dst.put((byte)(cp >> 8));
                dst.put((byte)cp);
            } else {
                dst.put((byte)cp);
                dst.put((byte)(cp >>  8));
                dst.put((byte)(cp >> 16));
                dst.put((byte)(cp >> 24));
            }
        }

        protected Encoder(Charset cs, int byteOrder, boolean doBOM) {
            super(cs, 4.0f,
                  doBOM?8.0f:4.0f,
                  (byteOrder==BIG)?new byte[]{(byte)0, (byte)0, (byte)0xff, (byte)0xfd}
                                  :new byte[]{(byte)0xfd, (byte)0xff, (byte)0, (byte)0});
            this.byteOrder = byteOrder;
            this.doBOM = doBOM;
            this.doneBOM = !doBOM;
        }

        protected CoderResult encodeLoop(CharBuffer src, ByteBuffer dst) {
            int mark = src.position();
            if (!doneBOM && src.hasRemaining()) {
                if (dst.remaining() < 4)
                    return CoderResult.OVERFLOW;
                put(BOM_BIG, dst);
                doneBOM = true;
            }
            try {
                while (src.hasRemaining()) {
                    char c = src.get();
                    if (!Character.isSurrogate(c)) {
                        if (dst.remaining() < 4)
                            return CoderResult.OVERFLOW;
                        mark++;
                        put(c, dst);
                    } else if (Character.isHighSurrogate(c)) {
                        if (!src.hasRemaining())
                            return CoderResult.UNDERFLOW;
                        char low = src.get();
                        if (Character.isLowSurrogate(low)) {
                            if (dst.remaining() < 4)
                                return CoderResult.OVERFLOW;
                            mark += 2;
                            put(Character.toCodePoint(c, low), dst);
                        } else {
                            return CoderResult.malformedForLength(1);
                        }
                    } else {
                        // assert Character.isLowSurrogate(c);
                        return CoderResult.malformedForLength(1);
                    }
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        protected void implReset() {
            doneBOM = !doBOM;
        }

    }
}
