/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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


/* @test
 * @bug 6803681
 * @summary Test IBM1364
 * @modules jdk.charsets
 */

import java.util.Arrays;
import java.nio.*;
import java.nio.charset.*;

public class TestIBM1364 {
    private static String c2bNRStr = "\u00AD\u00B7\u2015\u223C\u2299\uFF5E";
    private static byte[] c2bNRBytes = new byte[] {
        (byte)0x0e,
        (byte)0x41, (byte)0x48,
        (byte)0x41, (byte)0x43,
        (byte)0x41, (byte)0x49,
        (byte)0x42, (byte)0xa1,
        (byte)0x49, (byte)0x6f,
        (byte)0x49, (byte)0x54,
        (byte)0x0f };

    // end at SO
    private static String mixedStr = "\u008d\u008e\u0020\u3000\u3001\u71ba\u3164\u0088\ue757";
    private static byte[] mixedBytes = new byte[] {
         (byte)0x09,
         (byte)0x0a,
         (byte)0x40,
         (byte)0x0e,
         (byte)0x40, (byte)0x40,
         (byte)0x41, (byte)0x41,
         (byte)0x6c, (byte)0x45,
         (byte)0x84, (byte)0x41,
         (byte)0x0f,
         (byte)0x28,
         (byte)0x0e,
         (byte)0xdd, (byte)0xfd,
         (byte)0x0f };

    // end at SI
    private static String mixedStr2 = "\u008d\u008e\u0020\u3000\u3001\u71ba\u3164\u0088";
    private static byte[] mixedBytes2 = new byte[] {
         (byte)0x09,
         (byte)0x0a,
         (byte)0x40,
         (byte)0x0e,
         (byte)0x40, (byte)0x40,
         (byte)0x41, (byte)0x41,
         (byte)0x6c, (byte)0x45,
         (byte)0x84, (byte)0x41,
         (byte)0x0f,
         (byte)0x28 };

    private static byte[][] malformedBytes = {
        { (byte)0x0e,
          (byte)0x039, (byte)0x40,
          (byte)0x0f
        },
        { (byte)0x0e,
          (byte)0x039, (byte)0x42,
          (byte)0x0f
        },
        { (byte)0x0e,
          (byte)0x040, (byte)0x41,
          (byte)0x0f
        },
        { (byte)0x0e,
          (byte)0x040, (byte)0xee,
          (byte)0x0f
        },
        { (byte)0x0e,
          (byte)0x0ef, (byte)0x30,
          (byte)0x0f
        },
        { (byte)0x0e,
          (byte)0x0ff, (byte)0x41,
          (byte)0x0f
        }
    };

    private static byte[][] unmappedBytes = {
        { (byte)0x0e,
          (byte)0x06c, (byte)0x46,
          (byte)0x0f,
        },
        { (byte)0x0e,
          (byte)0x078, (byte)0x46,
          (byte)0x0f,
        },
        { (byte)0x0e,
          (byte)0x083, (byte)0xfe,
          (byte)0x0f,
        },
        { (byte)0xfa },
        { (byte)0xfe },
    };

    public static void main(String[] args) throws Exception {
        if (!(Arrays.equals(mixedStr.getBytes("cp1364"), mixedBytes)) ||
            !mixedStr.equals(new String(mixedBytes, "cp1364")))
            throw new RuntimeException("cp1364 failed on mixed!");

        if (!(Arrays.equals(mixedStr2.getBytes("cp1364"), mixedBytes2)) ||
            !mixedStr2.equals(new String(mixedBytes2, "cp1364")))
            throw new RuntimeException("cp1364 failed on mixed!");

        if (!(Arrays.equals(c2bNRStr.getBytes("cp1364"), c2bNRBytes)) ||
            c2bNRStr.equals(new String(c2bNRBytes, "cp1364")))
            throw new RuntimeException("cp1364 failed on c2bNR!");

        ByteBuffer bb = ByteBuffer.allocateDirect(mixedBytes.length);
        bb.put(mixedBytes).flip();
        CharBuffer cb = Charset.forName("ibm1364").decode(bb);
        if (!mixedStr.equals(new String(cb.toString())))
            throw new RuntimeException("cp1364 failed on direct decod()!");

        bb = ByteBuffer.allocateDirect(mixedBytes2.length);
        bb.put(mixedBytes2).flip();
        cb = Charset.forName("ibm1364").decode(bb);
        if (!mixedStr2.equals(new String(cb.toString())))
            throw new RuntimeException("cp1364 failed on direct decod()!");

        cb = ByteBuffer.allocateDirect(mixedStr.length() * 2).asCharBuffer();
        cb.put(mixedStr.toCharArray()).flip();
        bb = Charset.forName("x-ibm1364").encode(cb);
        if (!(Arrays.equals(Arrays.copyOf(bb.array(), bb.limit()), mixedBytes)))
            throw new RuntimeException("cp1364 failed on direct encode()!");

        cb = ByteBuffer.allocateDirect(mixedStr2.length() * 2).asCharBuffer();
        cb.put(mixedStr2.toCharArray()).flip();
        bb = Charset.forName("x-ibm1364").encode(cb);
        if (!(Arrays.equals(Arrays.copyOf(bb.array(), bb.limit()), mixedBytes2)))
            throw new RuntimeException("cp1364 failed on direct encode()!");

        // malformed
        cb = CharBuffer.allocate(1024);
        CharBuffer cbd = ByteBuffer.allocateDirect(1024).asCharBuffer();
        CharsetDecoder dec = Charset.forName("x-ibm1364").newDecoder();
        for (byte[] ba:malformedBytes) {
            cb.clear();
            dec.reset();
            if (!dec.reset().decode(ByteBuffer.wrap(ba), cb, true).isMalformed() ||
                !dec.reset().decode(ByteBuffer.wrap(ba), cbd, true).isMalformed())
                throw new RuntimeException("cp1364 failed on decode()/malformed!");
        }

        //unmappable
        for (byte[] ba:unmappedBytes) {
            cb.clear();
            dec.reset();
            if (!dec.reset().decode(ByteBuffer.wrap(ba), cb, true).isUnmappable() ||
                !dec.reset().decode(ByteBuffer.wrap(ba), cbd, true).isUnmappable())
                throw new RuntimeException("cp1364 failed on decode()/unmappable!");
        }

        //overflow
        cb.limit(mixedStr.length() - 1);
        cbd.limit(mixedStr.length() - 1);
        if (!dec.reset().decode(ByteBuffer.wrap(mixedBytes), cb, true).isOverflow() ||
            !dec.reset().decode(ByteBuffer.wrap(mixedBytes), cbd, true).isOverflow())
            throw new RuntimeException("cp1364 failed on decode()/overflow!");

        CharsetEncoder enc = Charset.forName("x-ibm1364").newEncoder();
        // last "0x0f" is from flush()
        bb = ByteBuffer.allocate(mixedBytes.length - 2);
        ByteBuffer bbd = ByteBuffer.allocateDirect(mixedBytes.length - 2);
        if (!enc.reset()
                .encode(CharBuffer.wrap(mixedStr.toCharArray()), bb, true)
                .isOverflow() ||
            !enc.reset()
                .encode(CharBuffer.wrap(mixedStr.toCharArray()), bbd, true)
                .isOverflow())
            throw new RuntimeException("cp1364 failed on encode()/overflow!");

        // flush() overflow
        bb = ByteBuffer.allocate(mixedBytes.length - 1);
        bbd = ByteBuffer.allocateDirect(mixedBytes.length - 1);

        enc.reset().encode(CharBuffer.wrap(mixedStr.toCharArray()), bb, true);
        enc.reset().encode(CharBuffer.wrap(mixedStr.toCharArray()), bbd, true);

        if (!enc.flush(bb).isOverflow() ||
            !enc.flush(bbd).isOverflow())
            throw new RuntimeException("cp1364 failed on encode()/flush()/overflow!");
    }
}
