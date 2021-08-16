/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4844847
 * @summary Test the MessageDigest.update(ByteBuffer) method
 * @author Andreas Sterbenz
 * @key randomness
 */

import java.util.*;
import java.nio.*;

import java.security.*;

public class ByteBuffers {

    public static void main(String[] args) throws Exception {
        Provider p = Security.getProvider("SUN");
        Random random = new Random();
        int n = 10 * 1024;
        byte[] t = new byte[n];
        random.nextBytes(t);

        MessageDigest md = MessageDigest.getInstance("MD5", p);
        byte[] d1 = md.digest(t);

        // test 1: ByteBuffer with an accessible backing array
        ByteBuffer b1 = ByteBuffer.allocate(n + 256);
        b1.position(random.nextInt(256));
        b1.limit(b1.position() + n);
        ByteBuffer b2 = b1.slice();
        b2.put(t);
        b2.clear();
        byte[] d2 = digest(md, b2, random);
        if (Arrays.equals(d1, d2) == false) {
            throw new Exception("Test 1 failed");
        }

        // test 2: direct ByteBuffer
        ByteBuffer b3 = ByteBuffer.allocateDirect(t.length);
        b3.put(t);
        b3.clear();
        byte[] d3 = digest(md, b3, random);
        if (Arrays.equals(d1, d3) == false) {
            throw new Exception("Test 2 failed");
        }

        // test 3: ByteBuffer without an accessible backing array
        b2.clear();
        ByteBuffer b4 = b2.asReadOnlyBuffer();
        byte[] d4 = digest(md, b4, random);
        if (Arrays.equals(d1, d4) == false) {
            throw new Exception("Test 3 failed");
        }
        System.out.println("All tests passed");
    }

    private static byte[] digest(MessageDigest md, ByteBuffer b, Random random) throws Exception {
        int lim = b.limit();
        b.limit(random.nextInt(lim));
        md.update(b);
        if (b.hasRemaining()) {
            throw new Exception("Buffer not consumed");
        }
        b.limit(lim);
        md.update(b);
        if (b.hasRemaining()) {
            throw new Exception("Buffer not consumed");
        }
        return md.digest();
    }
}
