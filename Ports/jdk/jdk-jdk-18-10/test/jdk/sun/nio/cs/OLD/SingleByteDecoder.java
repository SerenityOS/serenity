/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.charset.CharacterCodingException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;


public abstract class SingleByteDecoder
    extends CharsetDecoder
{

    private final String byteToCharTable;

    protected SingleByteDecoder(Charset cs, String byteToCharTable) {
        super(cs, 1.0f, 1.0f);
        this.byteToCharTable = byteToCharTable;
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
                int b = sa[sp];

                char c = decode(b);
                if (c == '\uFFFD')
                    return CoderResult.unmappableForLength(1);
                if (dl - dp < 1)
                    return CoderResult.OVERFLOW;
                da[dp++] = c;
                sp++;
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
                int b = src.get();

                char c = decode(b);
                if (c == '\uFFFD')
                    return CoderResult.unmappableForLength(1);
                if (!dst.hasRemaining())
                    return CoderResult.OVERFLOW;
                mark++;
                dst.put(c);
            }
            return CoderResult.UNDERFLOW;
        } finally {
            src.position(mark);
        }
    }

    protected CoderResult decodeLoop(ByteBuffer src, CharBuffer dst) {
        if (true && src.hasArray() && dst.hasArray())
            return decodeArrayLoop(src, dst);
        else
            return decodeBufferLoop(src, dst);
    }

    public char decode(int byteIndex) {
        int n = byteIndex + 128;
        if (n >= byteToCharTable.length() || n < 0)
            return '\uFFFD';
        return byteToCharTable.charAt(n);
    }
}
