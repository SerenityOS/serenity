/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test the Cipher.update/doFinal(ByteBuffer, ByteBuffer) methods
 * @author Andreas Sterbenz
 * @key randomness
 */

import java.util.*;
import java.nio.*;

import java.security.*;

import javax.crypto.*;
import javax.crypto.spec.*;

public class ByteBuffers {

    public static void main(String[] args) throws Exception {
        Provider p = Security.getProvider("SunJCE");
        Random random = new Random();
        int n = 10 * 1024;
        byte[] t = new byte[n];
        random.nextBytes(t);

        byte[] keyBytes = new byte[8];
        random.nextBytes(keyBytes);
        SecretKey key = new SecretKeySpec(keyBytes, "DES");

        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] outBytes = cipher.doFinal(t);

        // create ByteBuffers for input (i1, i2, i3) and fill them
        ByteBuffer i0 = ByteBuffer.allocate(n + 256);
        i0.position(random.nextInt(256));
        i0.limit(i0.position() + n);
        ByteBuffer i1 = i0.slice();
        i1.put(t);

        ByteBuffer i2 = ByteBuffer.allocateDirect(t.length);
        i2.put(t);

        i1.clear();
        ByteBuffer i3 = i1.asReadOnlyBuffer();

        ByteBuffer o0 = ByteBuffer.allocate(n + 512);
        o0.position(random.nextInt(256));
        o0.limit(o0.position() + n + 256);
        ByteBuffer o1 = o0.slice();

        ByteBuffer o2 = ByteBuffer.allocateDirect(t.length + 256);

        crypt(cipher, i1, o1, outBytes, random);
        crypt(cipher, i2, o1, outBytes, random);
        crypt(cipher, i3, o1, outBytes, random);
        crypt(cipher, i1, o2, outBytes, random);
        crypt(cipher, i2, o2, outBytes, random);
        crypt(cipher, i3, o2, outBytes, random);

        System.out.println("All tests passed");
    }

    private static void crypt(Cipher cipher, ByteBuffer in, ByteBuffer out, byte[] outBytes, Random random) throws Exception {
        in.clear();
        out.clear();
        out.put(new byte[out.remaining()]);
        out.clear();
        int lim = in.limit();
        in.limit(random.nextInt(lim));
        cipher.update(in, out);
        if (in.hasRemaining()) {
            throw new Exception("Buffer not consumed");
        }
        in.limit(lim);
        cipher.doFinal(in, out);
        if (in.hasRemaining()) {
            throw new Exception("Buffer not consumed");
        }
        out.flip();
        byte[] b = new byte[out.remaining()];
        out.get(b);
        if (Arrays.equals(outBytes, b) == false) {
            throw new Exception("Encryption output mismatch");
        }
    }
}
