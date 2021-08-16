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
 * @bug 4856966
 * @summary Test signing/verifying using all the signature algorithms
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @key randomness
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestSignatures
 * @run main/othervm -Djava.security.manager=allow TestSignatures sm rsakeys.ks.policy
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.util.Enumeration;
import java.util.Random;

public class TestSignatures extends PKCS11Test {

    private static final char[] password = "test12".toCharArray();

    private static Provider provider;

    private static byte[] data;

    static KeyStore getKeyStore() throws Exception {
        KeyStore ks;
        try (InputStream in = new FileInputStream(new File(BASE, "rsakeys.ks"))) {
            ks = KeyStore.getInstance("JKS");
            ks.load(in, password);
        }
        return ks;
    }

    private static void testSignature(String algorithm, PrivateKey privateKey,
            PublicKey publicKey) throws Exception {
        System.out.println("Testing " + algorithm + "...");
        Signature s = Signature.getInstance(algorithm, provider);
        s.initSign(privateKey);
        s.update(data);
        byte[] sig = s.sign();
        s.initVerify(publicKey);
        s.update(data);
        boolean result;
        result = s.verify(sig);
        if (result == false) {
            throw new Exception("Verification 1 failed");
        }
        s.update(data);
        result = s.verify(sig);
        if (result == false) {
            throw new Exception("Verification 2 failed");
        }
        result = s.verify(sig);
        if (result == true) {
            throw new Exception("Verification 3 succeeded");
        }
    }

    private static void test(PrivateKey privateKey, PublicKey publicKey)
            throws Exception {
        testSignature("MD2withRSA", privateKey, publicKey);
        testSignature("MD5withRSA", privateKey, publicKey);
        testSignature("SHA1withRSA", privateKey, publicKey);
        testSignature("SHA224withRSA", privateKey, publicKey);
        testSignature("SHA256withRSA", privateKey, publicKey);
        RSAPublicKey rsaKey = (RSAPublicKey)publicKey;
        if (rsaKey.getModulus().bitLength() > 512) {
            // for SHA384 and SHA512 the data is too long for 512 bit keys
            testSignature("SHA384withRSA", privateKey, publicKey);
            testSignature("SHA512withRSA", privateKey, publicKey);
        }
    }

    public static void main(String[] args) throws Exception {
        main(new TestSignatures(), args);
    }

    @Override
    public void main(Provider p) throws Exception {

        long start = System.currentTimeMillis();
        provider = p;
        data = new byte[2048];
        new Random().nextBytes(data);
        KeyStore ks = getKeyStore();
        KeyFactory kf = KeyFactory.getInstance("RSA", provider);
        for (Enumeration e = ks.aliases(); e.hasMoreElements(); ) {
            String alias = (String)e.nextElement();
            if (ks.isKeyEntry(alias)) {
                System.out.println("* Key " + alias + "...");
                PrivateKey privateKey = (PrivateKey)ks.getKey(alias, password);
                PublicKey publicKey = ks.getCertificate(alias).getPublicKey();
                privateKey = (PrivateKey)kf.translateKey(privateKey);
                publicKey = (PublicKey)kf.translateKey(publicKey);
                test(privateKey, publicKey);
            }
        }
        long stop = System.currentTimeMillis();
        System.out.println("All tests passed (" + (stop - start) + " ms).");
    }
}
