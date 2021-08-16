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
 * @bug 4856966 8242332
 * @summary Test the Signature.update(ByteBuffer) method
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @key randomness
 * @modules jdk.crypto.cryptoki
 * @run main/othervm ByteBuffers
 * @run main/othervm -Djava.security.manager=allow ByteBuffers sm
 */

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.Signature;
import java.util.Random;

public class ByteBuffers extends PKCS11Test {

    public static void main(String[] args) throws Exception {
        main(new ByteBuffers(), args);
    }

    @Override
    public void main(Provider p) throws Exception {

        Random random = new Random();
        int n = 10 * 1024;
        byte[] t = new byte[n];
        random.nextBytes(t);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", p);
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        Signature sig = Signature.getInstance("SHA256withRSA", p);
        sig.initSign(kp.getPrivate());
        sig.update(t);
        byte[] signature = sig.sign();

        sig.initVerify(kp.getPublic());

        // test 1: ByteBuffer with an accessible backing array
        ByteBuffer b1 = ByteBuffer.allocate(n + 256);
        b1.position(random.nextInt(256));
        b1.limit(b1.position() + n);
        ByteBuffer b2 = b1.slice();
        b2.put(t);
        b2.clear();
        verify(sig, signature, b2, random);

        // test 2: direct ByteBuffer
        ByteBuffer b3 = ByteBuffer.allocateDirect(t.length);
        b3.put(t);
        b3.clear();
        verify(sig, signature, b3, random);

        // test 3: ByteBuffer without an accessible backing array
        b2.clear();
        ByteBuffer b4 = b2.asReadOnlyBuffer();
        verify(sig, signature, b4, random);

        System.out.println("All tests passed");
    }

    private static void verify(Signature sig, byte[] signature, ByteBuffer b, Random random) throws Exception {
        int lim = b.limit();
        b.limit(random.nextInt(lim));
        sig.update(b);
        if (b.hasRemaining()) {
            throw new Exception("Buffer not consumed");
        }
        b.limit(lim);
        sig.update(b);
        if (b.hasRemaining()) {
            throw new Exception("Buffer not consumed");
        }
        if (sig.verify(signature) == false) {
            throw new Exception("Signature did not verify");
        }
    }
}
