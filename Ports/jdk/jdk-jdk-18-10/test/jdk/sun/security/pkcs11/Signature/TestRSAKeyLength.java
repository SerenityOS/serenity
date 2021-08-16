/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6695485 8242332
 * @summary Make sure initSign/initVerify() check RSA key lengths
 * @author Yu-Ching Valerie Peng
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestRSAKeyLength
 * @run main/othervm -Djava.security.manager=allow TestRSAKeyLength sm
 */

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignedObject;

public class TestRSAKeyLength extends PKCS11Test {

    public static void main(String[] args) throws Exception {
        main(new TestRSAKeyLength(), args);
    }

    @Override
    public void main(Provider p) throws Exception {

        boolean isValidKeyLength[] = {
                true, true, true, false, false, true, true, false, false
        };
        String algos[] = {
                "SHA1withRSA", "SHA224withRSA", "SHA256withRSA",
                "SHA384withRSA", "SHA512withRSA", "SHA3-224withRSA",
                "SHA3-256withRSA", "SHA3-384withRSA", "SHA3-512withRSA"
        };
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", p);
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();
        PrivateKey privKey = kp.getPrivate();
        PublicKey pubKey = kp.getPublic();

        if (algos.length != isValidKeyLength.length) {
            throw new Exception("Internal Error: number of test algos" +
                " and results length mismatch!");
        }
        for (int i = 0; i < algos.length; i++) {
            Signature sig = Signature.getInstance(algos[i], p);
            System.out.println("Testing RSA signature " + algos[i]);
            try {
                sig.initSign(privKey);
                if (!isValidKeyLength[i]) {
                    throw new Exception("initSign: Expected IKE not thrown!");
                }
            } catch (InvalidKeyException ike) {
                if (isValidKeyLength[i]) {
                    throw new Exception("initSign: Unexpected " + ike);
                }
            }
            try {
                sig.initVerify(pubKey);
                if (!isValidKeyLength[i]) {
                    throw new RuntimeException("initVerify: Expected IKE not thrown!");
                }
                new SignedObject("Test string for getSignature test.", privKey, sig);
            } catch (InvalidKeyException ike) {
                if (isValidKeyLength[i]) {
                    throw new Exception("initSign: Unexpected " + ike);
                }
            }
        }
    }
}
