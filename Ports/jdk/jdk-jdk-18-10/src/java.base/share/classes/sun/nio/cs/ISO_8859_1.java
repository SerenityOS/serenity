/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Objects;

import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.util.Preconditions;
import jdk.internal.vm.annotation.IntrinsicCandidate;

public class ISO_8859_1
    extends Charset
    implements HistoricallyNamedCharset
{
    public static final ISO_8859_1 INSTANCE = new ISO_8859_1();

    public ISO_8859_1() {
        super("ISO-8859-1", StandardCharsets.aliases_ISO_8859_1());
    }

    public String historicalName() {
        return "ISO8859_1";
    }

    public boolean contains(Charset cs) {
        return ((cs instanceof US_ASCII)
                || (cs instanceof ISO_8859_1));
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    private static class Decoder extends CharsetDecoder {

        private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();

        private Decoder(Charset cs) {
            super(cs, 1.0f, 1.0f);
        }

        private CoderResult decodeArrayLoop(ByteBuffer src,
                                            CharBuffer dst)
        {
            byte[] sa = src.array();
            int soff = src.arrayOffset();
            int sp = soff + src.position();
            int sl = soff + src.limit();

            char[] da = dst.array();
            int doff = dst.arrayOffset();
            int dp = doff + dst.position();
            int dl = doff + dst.limit();

            int decodeLen = Math.min(sl - sp, dl - dp);
            JLA.inflateBytesToChars(sa, sp, da, dp, decodeLen);
            sp += decodeLen;
            dp += decodeLen;
            src.position(sp - soff);
            dst.position(dp - doff);
            if (sl - sp > dl - dp) {
                return CoderResult.OVERFLOW;
            }
            return CoderResult.UNDERFLOW;
        }

        private CoderResult decodeBufferLoop(ByteBuffer src,
                                             CharBuffer dst)
        {
            int mark = src.position();
            try {
                while (src.hasRemaining()) {
                    byte b = src.get();
                    if (!dst.hasRemaining())
                        return CoderResult.OVERFLOW;
                    dst.put((char)(b & 0xff));
                    mark++;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        protected CoderResult decodeLoop(ByteBuffer src,
                                         CharBuffer dst)
        {
            if (src.hasArray() && dst.hasArray())
                return decodeArrayLoop(src, dst);
            else
                return decodeBufferLoop(src, dst);
        }
    }

    private static class Encoder extends CharsetEncoder {

        private Encoder(Charset cs) {
            super(cs, 1.0f, 1.0f);
        }

        public boolean canEncode(char c) {
            return c <= '\u00FF';
        }

        public boolean isLegalReplacement(byte[] repl) {
            return true;  // we accept any byte value
        }

        private final Surrogate.Parser sgp = new Surrogate.Parser();

        // Method possible replaced with a compiler intrinsic.
        private static int encodeISOArray(char[] sa, int sp,
                                          byte[] da, int dp, int len) {
            if (len <= 0) {
                return 0;
            }
            encodeISOArrayCheck(sa, sp, da, dp, len);
            return implEncodeISOArray(sa, sp, da, dp, len);
        }

        @IntrinsicCandidate
        private static int implEncodeISOArray(char[] sa, int sp,
                                              byte[] da, int dp, int len)
        {
            int i = 0;
            for (; i < len; i++) {
                char c = sa[sp++];
                if (c > '\u00FF')
                    break;
                da[dp++] = (byte)c;
            }
            return i;
        }

        private static void encodeISOArrayCheck(char[] sa, int sp,
                                                byte[] da, int dp, int len) {
            Objects.requireNonNull(sa);
            Objects.requireNonNull(da);
            Preconditions.checkIndex(sp, sa.length, Preconditions.AIOOBE_FORMATTER);
            Preconditions.checkIndex(dp, da.length, Preconditions.AIOOBE_FORMATTER);

            Preconditions.checkIndex(sp + len - 1, sa.length, Preconditions.AIOOBE_FORMATTER);
            Preconditions.checkIndex(dp + len - 1, da.length, Preconditions.AIOOBE_FORMATTER);
        }

        private CoderResult encodeArrayLoop(CharBuffer src,
                                            ByteBuffer dst)
        {
            char[] sa = src.array();
            int soff = src.arrayOffset();
            int sp = soff + src.position();
            int sl = soff + src.limit();
            assert (sp <= sl);
            sp = (sp <= sl ? sp : sl);
            byte[] da = dst.array();
            int doff = dst.arrayOffset();
            int dp = doff + dst.position();
            int dl = doff + dst.limit();
            assert (dp <= dl);
            dp = (dp <= dl ? dp : dl);
            int dlen = dl - dp;
            int slen = sl - sp;
            int len  = (dlen < slen) ? dlen : slen;
            try {
                int ret = encodeISOArray(sa, sp, da, dp, len);
                sp = sp + ret;
                dp = dp + ret;
                if (ret != len) {
                    if (sgp.parse(sa[sp], sa, sp, sl) < 0)
                        return sgp.error();
                    return sgp.unmappableResult();
                }
                if (len < slen)
                    return CoderResult.OVERFLOW;
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - soff);
                dst.position(dp - doff);
            }
        }

        private CoderResult encodeBufferLoop(CharBuffer src,
                                             ByteBuffer dst)
        {
            int mark = src.position();
            try {
                while (src.hasRemaining()) {
                    char c = src.get();
                    if (c <= '\u00FF') {
                        if (!dst.hasRemaining())
                            return CoderResult.OVERFLOW;
                        dst.put((byte)c);
                        mark++;
                        continue;
                    }
                    if (sgp.parse(c, src) < 0)
                        return sgp.error();
                    return sgp.unmappableResult();
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        protected CoderResult encodeLoop(CharBuffer src,
                                         ByteBuffer dst)
        {
            if (src.hasArray() && dst.hasArray())
                return encodeArrayLoop(src, dst);
            else
                return encodeBufferLoop(src, dst);
        }
    }
}
