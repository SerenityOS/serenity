/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
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


import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

/**
 * An abstract base class for subclasses which decode
 * IBM double byte ebcdic host encodings such as ibm code
 * pages 933, 935, 937,... etc
 *
 */

public abstract class DBCS_IBM_EBCDIC_Decoder extends CharsetDecoder
{

    private DBCSDecoderMapping decoderMapping;
    protected static final char REPLACE_CHAR='\uFFFD';

    protected String  singleByteToChar;
    protected short   index1[];
    protected String  index2;
    protected int     mask1;
    protected int     mask2;
    protected int     shift;

    private static final int SBCS = 0;
    private static final int DBCS = 1;

    private static final int SO = 0x0e;
    private static final int SI = 0x0f;
    private int  currentState;

    protected DBCS_IBM_EBCDIC_Decoder(Charset cs) {
        super(cs, 0.5f, 1.0f);
    }

    protected void implReset() {
        currentState = SBCS;
    }

    private CoderResult decodeArrayLoop(ByteBuffer src, CharBuffer dst) {
        byte[] sa = src.array();
        int sp = src.arrayOffset() + src.position();
        int sl = src.arrayOffset() + src.limit();
        assert (sp <= sl);
        sp = (sp <= sl ? sp : sl);
        char[] da = dst.array();
        int dp = dst.arrayOffset() + dst.position();
        int dl = dst.arrayOffset() + dst.limit();
        assert (dp <= dl);
        dp = (dp <= dl ? dp : dl);

        try {
            while (sp < sl) {
                int b1, b2;
                b1 = sa[sp];
                int inputSize = 1;
                int v = 0;
                char outputChar = REPLACE_CHAR;

                if (b1 < 0)
                    b1 += 256;

                if (b1 == SO) {  // Shift out
                    // For SO characters - simply validate the state and if OK
                    //    update the state and go to the next byte

                    if (currentState != SBCS)
                        return CoderResult.malformedForLength(1);
                    else
                        currentState = DBCS;
                } else if (b1 == SI) {
                    // For SI characters - simply validate the state and if OK
                    //    update the state and go to the next byte

                    if (currentState != DBCS) {
                        return CoderResult.malformedForLength(1);
                    } else {
                        currentState = SBCS;
                    }
                } else {
                    if (currentState == SBCS) {
                        outputChar = singleByteToChar.charAt(b1);
                    } else {
                    if (sl - sp < 2)
                        return CoderResult.UNDERFLOW;
                    b2 = sa[sp + 1];
                    if (b2 < 0)
                        b2 += 256;

                    inputSize++;

                    // Check validity of dbcs ebcdic byte pair values
                    if ((b1 != 0x40 || b2 != 0x40) &&
                      (b2 < 0x41 || b2 > 0xfe)) {
                      return CoderResult.malformedForLength(2);
                    }

                    // Lookup in the two level index
                    v = b1 * 256 + b2;
                    outputChar = index2.charAt(index1[((v & mask1) >> shift)]
                                                + (v & mask2));
                    }
                    if (outputChar == '\uFFFD')
                        return CoderResult.unmappableForLength(inputSize);

                    if (dl - dp < 1)
                        return CoderResult.OVERFLOW;
                    da[dp++] = outputChar;
                }
                sp += inputSize;
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
                int b1, b2;
                int v = 0;
                b1 = src.get();
                int inputSize = 1;
                char outputChar = REPLACE_CHAR;

                if (b1 < 0)
                    b1 += 256;


                if (b1 == SO) {  // Shift out
                    // For SO characters - simply validate the state and if OK
                    //    update the state and go to the next byte

                    if (currentState != SBCS)
                        return CoderResult.malformedForLength(1);
                    else
                        currentState = DBCS;
                } else if (b1 == SI) {
                    // For SI characters - simply validate the state and if OK
                    //    update the state and go to the next byte

                    if (currentState != DBCS) {
                        return CoderResult.malformedForLength(1);
                    } else {
                        currentState = SBCS;
                    }
                } else {
                    if (currentState == SBCS) {
                      outputChar = singleByteToChar.charAt(b1);
                    } else {
                        if (src.remaining() < 1)
                            return CoderResult.UNDERFLOW;
                        b2 = src.get();
                        if (b2 < 0)
                            b2 += 256;
                        inputSize++;

                        // Check validity of dbcs ebcdic byte pair values
                        if ((b1 != 0x40 || b2 != 0x40) &&
                           (b2 < 0x41 || b2 > 0xfe)) {
                          return CoderResult.malformedForLength(2);
                        }

                        // Lookup in the two level index
                        v = b1 * 256 + b2;
                        outputChar = index2.charAt(index1[((v & mask1) >> shift)]
                                                            + (v & mask2));
                    }
                    if (outputChar == REPLACE_CHAR)
                        return CoderResult.unmappableForLength(inputSize);

                    if (!dst.hasRemaining())
                        return CoderResult.OVERFLOW;
                    dst.put(outputChar);
                }
                mark += inputSize;
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
}
