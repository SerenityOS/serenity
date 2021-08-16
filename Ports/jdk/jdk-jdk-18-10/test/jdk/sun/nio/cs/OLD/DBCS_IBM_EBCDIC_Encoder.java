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
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import sun.nio.cs.Surrogate;

/**
 * An abstract base class for subclasses which encodes
 * IBM double byte host encodings such as ibm code
 * pages 942,943,948, etc.
 *
 */

public abstract class DBCS_IBM_EBCDIC_Encoder extends CharsetEncoder
{

    protected static final char REPLACE_CHAR='\uFFFD';
    private byte b1;
    private byte b2;

    protected short index1[];
    protected String index2;
    protected String index2a;
    protected int   mask1;
    protected int   mask2;
    protected int   shift;

    private static final int SBCS = 0;
    private static final int DBCS = 1;

    private static final byte SO = 0x0e;
    private static final byte SI = 0x0f;

    private int  currentState;

    private final Surrogate.Parser sgp = new Surrogate.Parser();

    protected DBCS_IBM_EBCDIC_Encoder(Charset cs) {
        super(cs, 4.0f, 5.0f, new byte[] {(byte)0x6f});
    }

    protected void implReset() {
        currentState = SBCS;
    }

    protected CoderResult implFlush(ByteBuffer out) {
        if (currentState == DBCS) {
            if (out.remaining() < 1)
                return CoderResult.OVERFLOW;
            out.put(SI);
        }
        implReset();
        return CoderResult.UNDERFLOW;
    }

    /**
     * Returns true if the given character can be converted to the
     * target character encoding.
     */
    public boolean canEncode(char ch) {
       int  index;
       int  theBytes;

       index = index1[((ch & mask1) >> shift)] + (ch & mask2);
       if (index  < 15000)
         theBytes = (int)(index2.charAt(index));
       else
         theBytes = (int)(index2a.charAt(index-15000));

       if (theBytes != 0)
         return (true);

       // only return true if input char was unicode null - all others are
       //     undefined
       return( ch == '\u0000');

    }

    private CoderResult encodeArrayLoop(CharBuffer src, ByteBuffer dst) {
        char[] sa = src.array();
        int sp = src.arrayOffset() + src.position();
        int sl = src.arrayOffset() + src.limit();
        byte[] da = dst.array();
        int dp = dst.arrayOffset() + dst.position();
        int dl = dst.arrayOffset() + dst.limit();
        int outputSize = 0;             // size of output
        int spaceNeeded;

        try {
            while (sp < sl) {
                int index;
                int theBytes;
                char c = sa[sp];
                if (Surrogate.is(c)) {
                    if (sgp.parse(c, sa, sp, sl) < 0)
                        return sgp.error();
                    return sgp.unmappableResult();
                }
                if (c >= '\uFFFE')
                    return CoderResult.unmappableForLength(1);


                index = index1[((c & mask1) >> shift)]
                                + (c & mask2);
                if (index < 15000)
                    theBytes = (int)(index2.charAt(index));
                else
                    theBytes = (int)(index2a.charAt(index-15000));
                b1= (byte)((theBytes & 0x0000ff00)>>8);
                b2 = (byte)(theBytes & 0x000000ff);

                if (b1 == 0x00 && b2 == 0x00
                    && c != '\u0000') {
                        return CoderResult.unmappableForLength(1);
                }

                if (currentState == DBCS && b1 == 0x00) {
                    if (dl - dp < 1)
                        return CoderResult.OVERFLOW;
                    currentState = SBCS;
                    da[dp++] = SI;
                } else if (currentState == SBCS && b1 != 0x00) {
                    if (dl - dp < 1)
                        return CoderResult.OVERFLOW;
                    currentState = DBCS;
                    da[dp++] = SO;
                }
                if (currentState == DBCS)
                    spaceNeeded = 2;
                else
                    spaceNeeded = 1;
                if (dl - dp < spaceNeeded)
                    return CoderResult.OVERFLOW;

                if (currentState == SBCS)
                    da[dp++] = b2;
                else {
                    da[dp++] = b1;
                    da[dp++] = b2;
                }
                sp++;
            }
            return CoderResult.UNDERFLOW;
        } finally {
            src.position(sp - src.arrayOffset());
            dst.position(dp - dst.arrayOffset());
        }
    }

    private CoderResult encodeBufferLoop(CharBuffer src, ByteBuffer dst) {
        int mark = src.position();
        int outputSize = 0;             // size of output
        int spaceNeeded;

        try {
            while (src.hasRemaining()) {
                int index;
                int theBytes;
                char c = src.get();
                if (Surrogate.is(c)) {
                    if (sgp.parse(c, src) < 0)
                        return sgp.error();
                    return sgp.unmappableResult();
                }
                if (c >= '\uFFFE')
                    return CoderResult.unmappableForLength(1);

                index = index1[((c & mask1) >> shift)]
                                + (c & mask2);
                if (index < 15000)
                    theBytes = (int)(index2.charAt(index));
                else
                    theBytes = (int)(index2a.charAt(index-15000));
                b1 = (byte)((theBytes & 0x0000ff00)>>8);
                b2 = (byte)(theBytes & 0x000000ff);

                if (b1== 0x00 && b2 == 0x00
                    && c != '\u0000') {
                        return CoderResult.unmappableForLength(1);
                }

                if (currentState == DBCS && b1 == 0x00) {
                    if (dst.remaining() < 1)
                        return CoderResult.OVERFLOW;
                    currentState = SBCS;
                    dst.put(SI);
                } else if (currentState == SBCS && b1 != 0x00) {
                    if (dst.remaining() < 1)
                        return CoderResult.OVERFLOW;
                    currentState = DBCS;
                    dst.put(SO);
                }

                if (currentState == DBCS)
                    spaceNeeded = 2;
                else
                    spaceNeeded = 1;

                if (dst.remaining() < spaceNeeded)
                    return CoderResult.OVERFLOW;

                if (currentState == SBCS)
                    dst.put(b2);
                else {
                    dst.put(b1);
                    dst.put(b2);
                }
                mark++;
             }
            return CoderResult.UNDERFLOW;
        } finally {
            src.position(mark);
        }
    }

    protected CoderResult encodeLoop(CharBuffer src, ByteBuffer dst) {
        if (true && src.hasArray() && dst.hasArray())
            return encodeArrayLoop(src, dst);
        else
            return encodeBufferLoop(src, dst);
    }

}
