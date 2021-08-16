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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import sun.nio.cs.CharsetMapping;
import sun.nio.cs.*;

/*
 *  5 types of entry in SJIS_X_0213/Unicode mapping table
 *
 *  (1)Single-Byte
 *     JIS_X_0213 does not define single-byte character itself, the
 *     JIS_X_0201 entries are added in for sjis implementation.
 *
 *  (2)Double-Byte SJIS <-> BMP Unicode
 *     ex: 0x8140 U+3000    # IDEOGRAPHIC SPACE
 *
 *  (3)Double-Byte SJIS <-> Supplementary
 *     ex: 0xFCF0 U+2A61A   # <cjk> [2000] [Unicode3.1]
 *
 *  (4)Double-Byte SJIS <-> Composite
 *   ex: 0x83F6 U+31F7+309A # [2000]
 *
 *  (5)"Windows-only" special mapping entries
 *     are handled by MS932_0213.
 */

public class SJIS_0213 extends Charset {
    public SJIS_0213() {
        super("x-SJIS_0213", ExtendedCharsets.aliasesFor("x-SJIS_0213"));
    }

    public boolean contains(Charset cs) {
        return ((cs.name().equals("US-ASCII"))
                || (cs instanceof SJIS)
                || (cs instanceof SJIS_0213));
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    private static class Holder {
        @SuppressWarnings("removal")
        static final CharsetMapping mapping = AccessController.doPrivileged(
                new PrivilegedAction<CharsetMapping>() {
                    public CharsetMapping run() {
                        return CharsetMapping.get(SJIS_0213.class.getResourceAsStream("sjis0213.dat"));
                    }
                });
    }

    protected static class Decoder extends CharsetDecoder {
        protected static final char UNMAPPABLE = CharsetMapping.UNMAPPABLE_DECODING;

        protected Decoder(Charset cs) {
            super(cs, 0.5f, 1.0f);
        }

        private CoderResult decodeArrayLoop(ByteBuffer src, CharBuffer dst) {
            byte[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();

            char[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();

            try {
                while (sp < sl) {
                    int b1 = sa[sp] & 0xff;
                    char c = decodeSingle(b1);
                    int inSize = 1, outSize = 1;
                    char[] cc = null;
                    if (c == UNMAPPABLE) {
                        if (sl - sp < 2)
                            return CoderResult.UNDERFLOW;
                        int b2 = sa[sp + 1] & 0xff;
                        c = decodeDouble(b1, b2);
                        inSize++;
                        if (c == UNMAPPABLE) {
                            cc = decodeDoubleEx(b1, b2);
                            if (cc == null) {
                                if (decodeSingle(b2) == UNMAPPABLE)
                                    return CoderResult.unmappableForLength(2);
                                else
                                    return CoderResult.unmappableForLength(1);
                            }
                            outSize++;
                        }
                    }
                    if (dl - dp < outSize)
                        return CoderResult.OVERFLOW;
                    if (outSize == 2) {
                        da[dp++] = cc[0];
                        da[dp++] = cc[1];
                    } else {
                        da[dp++] = c;
                    }
                    sp += inSize;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
            }
        }

        private CoderResult decodeBufferLoop(ByteBuffer src, CharBuffer dst) {
            int mark = src.position();
            try {
                while (src.hasRemaining()) {
                    char[] cc = null;
                    int b1 = src.get() & 0xff;
                    char c = decodeSingle(b1);
                    int inSize = 1, outSize = 1;
                    if (c == UNMAPPABLE) {
                        if (src.remaining() < 1)
                            return CoderResult.UNDERFLOW;
                        int b2 = src.get() & 0xff;
                        inSize++;
                        c = decodeDouble(b1, b2);
                        if (c == UNMAPPABLE) {
                            cc = decodeDoubleEx(b1, b2);
                            if (cc == null) {
                                if (decodeSingle(b2) == UNMAPPABLE)
                                    return CoderResult.unmappableForLength(2);
                                else
                                    return CoderResult.unmappableForLength(1);
                            }
                            outSize++;
                        }
                    }
                    if (dst.remaining() < outSize)
                        return CoderResult.OVERFLOW;
                    if (outSize == 2) {
                        dst.put(cc[0]);
                        dst.put(cc[1]);
                    } else {
                        dst.put(c);
                    }
                    mark += inSize;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        protected CoderResult decodeLoop(ByteBuffer src, CharBuffer dst) {
            if (src.hasArray() && dst.hasArray())
                return decodeArrayLoop(src, dst);
            else
                return decodeBufferLoop(src, dst);
        }

        protected char decodeSingle(int b) {
            return Holder.mapping.decodeSingle(b);
        }

        protected char decodeDouble(int b1, int b2) {
            return Holder.mapping.decodeDouble(b1, b2);
        }

        private char[] cc = new char[2];
        private CharsetMapping.Entry comp = new CharsetMapping.Entry();
        protected char[] decodeDoubleEx(int b1, int b2) {
            int db = (b1 << 8) | b2;
            if (Holder.mapping.decodeSurrogate(db, cc) != null)
                return cc;
            comp.bs = db;
            if (Holder.mapping.decodeComposite(comp, cc) != null)
                return cc;
            return null;
        }
    }

    protected static class Encoder extends CharsetEncoder {
        protected static final int UNMAPPABLE = CharsetMapping.UNMAPPABLE_ENCODING;
        protected static final int MAX_SINGLEBYTE = 0xff;

        protected Encoder(Charset cs) {
            super(cs, 2.0f, 2.0f);
        }

        public boolean canEncode(char c) {
            return (encodeChar(c) != UNMAPPABLE);
        }

        protected int encodeChar(char ch) {
            return Holder.mapping.encodeChar(ch);
        }

        protected int encodeSurrogate(char hi, char lo) {
            return Holder.mapping.encodeSurrogate(hi, lo);
        }

        private CharsetMapping.Entry comp = new CharsetMapping.Entry();
        protected int encodeComposite(char base, char cc) {
            comp.cp = base;
            comp.cp2 = cc;
            return Holder.mapping.encodeComposite(comp);
        }

        protected boolean isCompositeBase(char ch) {
            comp.cp = ch;
            return Holder.mapping.isCompositeBase(comp);
        }

        // Unlike surrogate pair, the base character of a base+cc composite
        // itself is a legal codepoint in 0213, if we simply return UNDERFLOW
        // when a base candidate is the last input char in the CharBuffer, like
        // what we do for the surrogte pair, encoding will fail if this base
        // character is indeed the last character of the input char sequence.
        // Keep this base candidate in "leftoverBase" so we can flush it out
        // at the end of the encoding circle.
        char leftoverBase = 0;
        protected CoderResult encodeArrayLoop(CharBuffer src, ByteBuffer dst) {
            char[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();
            byte[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();

            try {
                while (sp < sl) {
                    int db;
                    char c = sa[sp];
                    if (leftoverBase != 0) {
                        boolean isComp = false;
                        db = encodeComposite(leftoverBase, c);
                        if (db == UNMAPPABLE)
                            db = encodeChar(leftoverBase);
                        else
                            isComp = true;
                        if (dl - dp < 2)
                            return CoderResult.OVERFLOW;
                        da[dp++] = (byte)(db >> 8);
                        da[dp++] = (byte)db;
                        leftoverBase = 0;
                        if (isComp) {
                            sp++;
                            continue;
                        }
                    }
                    if (isCompositeBase(c)) {
                        leftoverBase = c;
                    } else {
                        db = encodeChar(c);
                        if (db <= MAX_SINGLEBYTE) {      // SingleByte
                            if (dl <= dp)
                                return CoderResult.OVERFLOW;
                            da[dp++] = (byte)db;
                        } else if (db != UNMAPPABLE) {   // DoubleByte
                            if (dl - dp < 2)
                                return CoderResult.OVERFLOW;
                            da[dp++] = (byte)(db >> 8);
                            da[dp++] = (byte)db;
                        } else if (Character.isHighSurrogate(c)) {
                            if ((sp + 1) == sl)
                                return CoderResult.UNDERFLOW;
                            char c2 = sa[sp + 1];
                            if (!Character.isLowSurrogate(c2))
                                return CoderResult.malformedForLength(1);
                            db = encodeSurrogate(c, c2);
                            if (db == UNMAPPABLE)
                                return CoderResult.unmappableForLength(2);
                            if (dl - dp < 2)
                                return CoderResult.OVERFLOW;
                            da[dp++] = (byte)(db >> 8);
                            da[dp++] = (byte)db;
                            sp++;
                        } else if (Character.isLowSurrogate(c)) {
                            return CoderResult.malformedForLength(1);
                        } else {
                            return CoderResult.unmappableForLength(1);
                        }
                    }
                    sp++;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
            }
        }

        protected CoderResult encodeBufferLoop(CharBuffer src, ByteBuffer dst) {
            int mark = src.position();
            try {
                while (src.hasRemaining()) {
                    int db;
                    char c = src.get();
                    if (leftoverBase != 0) {
                        boolean isComp = false;
                        db = encodeComposite(leftoverBase, c);
                        if (db == UNMAPPABLE)
                            db = encodeChar(leftoverBase);
                        else
                            isComp = true;
                        if (dst.remaining() < 2)
                            return CoderResult.OVERFLOW;
                        dst.put((byte)(db >> 8));
                        dst.put((byte)(db));
                        leftoverBase = 0;
                        if (isComp) {
                            mark++;
                            continue;
                        }
                    }
                    if (isCompositeBase(c)) {
                        leftoverBase = c;
                    } else {
                        db = encodeChar(c);
                        if (db <= MAX_SINGLEBYTE) {    // Single-byte
                            if (dst.remaining() < 1)
                                return CoderResult.OVERFLOW;
                            dst.put((byte)db);
                        } else if (db != UNMAPPABLE) {   // DoubleByte
                            if (dst.remaining() < 2)
                                return CoderResult.OVERFLOW;
                            dst.put((byte)(db >> 8));
                            dst.put((byte)(db));
                        } else if (Character.isHighSurrogate(c)) {
                            if (!src.hasRemaining())     // Surrogates
                                return CoderResult.UNDERFLOW;
                            char c2 = src.get();
                            if (!Character.isLowSurrogate(c2))
                                return CoderResult.malformedForLength(1);
                            db = encodeSurrogate(c, c2);
                            if (db == UNMAPPABLE)
                                return CoderResult.unmappableForLength(2);
                            if (dst.remaining() < 2)
                                return CoderResult.OVERFLOW;
                            dst.put((byte)(db >> 8));
                            dst.put((byte)(db));
                            mark++;
                        } else if (Character.isLowSurrogate(c)) {
                            return CoderResult.malformedForLength(1);
                        } else {
                            return CoderResult.unmappableForLength(1);
                        }
                    }
                    mark++;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        protected CoderResult encodeLoop(CharBuffer src, ByteBuffer dst) {
            if (src.hasArray() && dst.hasArray())
                return encodeArrayLoop(src, dst);
            else
                return encodeBufferLoop(src, dst);
        }

        protected CoderResult implFlush(ByteBuffer dst) {
            if (leftoverBase > 0) {
                if (dst.remaining() < 2)
                    return CoderResult.OVERFLOW;
                int db = encodeChar(leftoverBase);
                dst.put((byte)(db >> 8));
                dst.put((byte)(db));
                leftoverBase = 0;
            }
            return CoderResult.UNDERFLOW;
        }

        protected void implReset() {
            leftoverBase = 0;
        }
    }
}
