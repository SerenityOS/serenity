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

/*
 */

package sun.nio.cs.ext;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import sun.nio.cs.Surrogate;

abstract class ISO2022
    extends Charset
{
    public ISO2022(String csname, String[] aliases) {
        super(csname, aliases);
    }

    public abstract CharsetDecoder newDecoder();

    public abstract CharsetEncoder newEncoder();

    // No default Decoder implementation is provided here; the concrete
    // encodings differ enough that most had been specialized for
    // performance reasons, leaving the generic implementation that existed
    // here before JDK-8261418 unused except by ISO2022_KR. As both a
    // simplification and an optimization the implementation was moved
    // there and specialized.

    protected static class Encoder extends CharsetEncoder {

        private static final byte ISO_ESC = 0x1b;
        private static final byte ISO_SI = 0x0f;
        private static final byte ISO_SO = 0x0e;
        private static final byte ISO_SS2_7 = 0x4e;
        private static final byte ISO_SS3_7 = 0x4f;

        private final Surrogate.Parser sgp = new Surrogate.Parser();
        public static final byte SS2 = (byte)0x8e;
        public static final byte PLANE2 = (byte)0xA2;
        public static final byte PLANE3 = (byte)0xA3;

        protected final byte maximumDesignatorLength = 4;

        protected byte[] SODesig,
                         SS2Desig = null,
                         SS3Desig = null;

        protected CharsetEncoder ISOEncoder;

        private boolean shiftout = false;
        private boolean SODesDefined = false;
        private boolean SS2DesDefined = false;
        private boolean SS3DesDefined = false;

        private boolean newshiftout = false;
        private boolean newSODesDefined = false;
        private boolean newSS2DesDefined = false;
        private boolean newSS3DesDefined = false;

        protected Encoder(Charset cs) {
            super(cs, 4.0f, 8.0f);
        }

        public boolean canEncode(char c) {
            return (ISOEncoder.canEncode(c));
        }

        protected void implReset() {
            shiftout = false;
            SODesDefined = false;
            SS2DesDefined = false;
            SS3DesDefined = false;
        }

        private int unicodeToNative(char unicode, byte ebyte[]) {
            int index = 0;
            char[] convChar = {unicode};
            byte[] convByte = new byte[4];
            int converted;

            try{
                CharBuffer cc = CharBuffer.wrap(convChar);
                ByteBuffer bb = ByteBuffer.wrap(convByte);
                ISOEncoder.encode(cc, bb, true);
                bb.flip();
                converted = bb.remaining();
            } catch(Exception e) {
                return -1;
            }

            if (converted == 2) {
                if (!SODesDefined) {
                    newSODesDefined = true;
                    ebyte[0] = ISO_ESC;
                    System.arraycopy(SODesig, 0, ebyte, 1, SODesig.length);
                    index = SODesig.length + 1;
                }
                if (!shiftout) {
                    newshiftout = true;
                    ebyte[index++] = ISO_SO;
                }
                ebyte[index++] = (byte)(convByte[0] & 0x7f);
                ebyte[index++] = (byte)(convByte[1] & 0x7f);
            } else {
                if(convByte[0] == SS2) {
                    if (convByte[1] == PLANE2) {
                        if (!SS2DesDefined) {
                            newSS2DesDefined = true;
                            ebyte[0] = ISO_ESC;
                            System.arraycopy(SS2Desig, 0, ebyte, 1, SS2Desig.length);
                            index = SS2Desig.length + 1;
                        }
                        ebyte[index++] = ISO_ESC;
                        ebyte[index++] = ISO_SS2_7;
                        ebyte[index++] = (byte)(convByte[2] & 0x7f);
                        ebyte[index++] = (byte)(convByte[3] & 0x7f);
                    } else if (convByte[1] == PLANE3) {
                        if(!SS3DesDefined){
                            newSS3DesDefined = true;
                            ebyte[0] = ISO_ESC;
                            System.arraycopy(SS3Desig, 0, ebyte, 1, SS3Desig.length);
                            index = SS3Desig.length + 1;
                        }
                        ebyte[index++] = ISO_ESC;
                        ebyte[index++] = ISO_SS3_7;
                        ebyte[index++] = (byte)(convByte[2] & 0x7f);
                        ebyte[index++] = (byte)(convByte[3] & 0x7f);
                    }
                }
            }
            return index;
        }

        private CoderResult encodeArrayLoop(CharBuffer src,
                                            ByteBuffer dst)
        {
            char[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();

            byte[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();

            int outputSize;
            byte[] outputByte = new byte[8];
            newshiftout = shiftout;
            newSODesDefined = SODesDefined;
            newSS2DesDefined = SS2DesDefined;
            newSS3DesDefined = SS3DesDefined;

            try {
                while (sp < sl) {
                    char c = sa[sp];
                    if (Character.isSurrogate(c)) {
                        if (sgp.parse(c, sa, sp, sl) < 0)
                            return sgp.error();
                        return sgp.unmappableResult();
                    }

                    if (c < 0x80) {     // ASCII
                        if (shiftout){
                            newshiftout = false;
                            outputSize = 2;
                            outputByte[0] = ISO_SI;
                            outputByte[1] = (byte)(c & 0x7f);
                        } else {
                            outputSize = 1;
                            outputByte[0] = (byte)(c & 0x7f);
                        }
                        if(sa[sp] == '\n'){
                            newSODesDefined = false;
                            newSS2DesDefined = false;
                            newSS3DesDefined = false;
                        }
                    } else {
                        outputSize = unicodeToNative(c, outputByte);
                        if (outputSize == 0) {
                            return CoderResult.unmappableForLength(1);
                        }
                    }
                    if (dl - dp < outputSize)
                        return CoderResult.OVERFLOW;

                    for (int i = 0; i < outputSize; i++)
                        da[dp++] = outputByte[i];
                    sp++;
                    shiftout = newshiftout;
                    SODesDefined = newSODesDefined;
                    SS2DesDefined = newSS2DesDefined;
                    SS3DesDefined = newSS3DesDefined;
                }
                return CoderResult.UNDERFLOW;
             } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
             }
        }

        private CoderResult encodeBufferLoop(CharBuffer src,
                                             ByteBuffer dst)
        {
            int outputSize;
            byte[] outputByte = new byte[8];
            newshiftout = shiftout;
            newSODesDefined = SODesDefined;
            newSS2DesDefined = SS2DesDefined;
            newSS3DesDefined = SS3DesDefined;
            int mark = src.position();

            try {
                while (src.hasRemaining()) {
                    char inputChar = src.get();
                    if (Character.isSurrogate(inputChar)) {
                        if (sgp.parse(inputChar, src) < 0)
                            return sgp.error();
                        return sgp.unmappableResult();
                    }
                    if (inputChar < 0x80) {     // ASCII
                        if (shiftout){
                            newshiftout = false;
                            outputSize = 2;
                            outputByte[0] = ISO_SI;
                            outputByte[1] = (byte)(inputChar & 0x7f);
                        } else {
                            outputSize = 1;
                            outputByte[0] = (byte)(inputChar & 0x7f);
                        }
                        if (inputChar == '\n') {
                            newSODesDefined = false;
                            newSS2DesDefined = false;
                            newSS3DesDefined = false;
                        }
                    } else {
                        outputSize = unicodeToNative(inputChar, outputByte);
                        if (outputSize == 0) {
                            return CoderResult.unmappableForLength(1);
                        }
                    }

                    if (dst.remaining() < outputSize)
                        return CoderResult.OVERFLOW;
                    for (int i = 0; i < outputSize; i++)
                        dst.put(outputByte[i]);
                    mark++;
                    shiftout = newshiftout;
                    SODesDefined = newSODesDefined;
                    SS2DesDefined = newSS2DesDefined;
                    SS3DesDefined = newSS3DesDefined;
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
