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
 * @summary test that reinitializing Signatures works correctly
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @key randomness
 * @modules jdk.crypto.cryptoki
 * @run main ReinitSignature
 */

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Random;

public class ReinitSignature extends PKCS11Test {

    public static void main(String[] args) throws Exception {
        main(new ReinitSignature());
    }

    public void main(Provider p) throws Exception {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", p);
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        PrivateKey privateKey = kp.getPrivate();
        PublicKey publicKey = kp.getPublic();
        Signature sig = Signature.getInstance("SHA256withRSA", p);
        byte[] data = new byte[10 * 1024];
        new Random().nextBytes(data);
        sig.initSign(privateKey);
        sig.initSign(privateKey);
        sig.update(data);
        sig.initSign(privateKey);
        sig.update(data);
        byte[] signature = sig.sign();
        sig.update(data);
        sig.initSign(privateKey);
        sig.update(data);
        sig.sign();
        sig.sign();
        sig.initSign(privateKey);
        sig.sign();

        System.out.println("All tests passed");
    }

}
