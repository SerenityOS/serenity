/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4856966 8080462 8242332
 * @summary Test the MessageDigest.update(ByteBuffer) method
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @key randomness
 * @modules jdk.crypto.cryptoki
 * @run main/othervm ByteBuffers
 */

import java.nio.ByteBuffer;
import java.security.*;
import java.util.Arrays;
import java.util.Random;
import java.util.List;

public class ByteBuffers extends PKCS11Test {

    private static Random random = new Random();

    public static void main(String[] args) throws Exception {
        main(new ByteBuffers(), args);
    }

    @Override
    public void main(Provider p) throws Exception {
        List<String> ALGS = getSupportedAlgorithms("MessageDigest",
                "SHA", p);

        int n = 10 * 1024;
        byte[] t = new byte[n];
        random.nextBytes(t);

        for (String alg : ALGS) {
            runTest(p, alg, t);
        }
    }

    private void runTest(Provider p, String alg, byte[] data) throws Exception {
        System.out.println("Test against " + p.getName() + " and " + alg);
        MessageDigest md = MessageDigest.getInstance(alg, p);

        byte[] d1 = md.digest(data);

        int n = data.length;

        // test 1: ByteBuffer with an accessible backing array
        ByteBuffer b1 = ByteBuffer.allocate(n + 256);
        b1.position(random.nextInt(256));
        b1.limit(b1.position() + n);
        ByteBuffer b2 = b1.slice();
        b2.put(data);
        b2.clear();
        byte[] d2 = digest(md, b2);
        if (Arrays.equals(d1, d2) == false) {
            throw new Exception("Test 1 failed");
        }

        // test 2: direct ByteBuffer
        ByteBuffer b3 = ByteBuffer.allocateDirect(n);
        b3.put(data);
        b3.clear();
        byte[] d3 = digest(md, b3);
        if (Arrays.equals(d1, d2) == false) {
            throw new Exception("Test 2 failed");
        }

        // test 3: ByteBuffer without an accessible backing array
        b2.clear();
        ByteBuffer b4 = b2.asReadOnlyBuffer();
        byte[] d4 = digest(md, b4);
        if (Arrays.equals(d1, d2) == false) {
            throw new Exception("Test 3 failed");
        }
        System.out.println("All tests passed");
    }

    private static byte[] digest(MessageDigest md, ByteBuffer b)
            throws Exception {
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
