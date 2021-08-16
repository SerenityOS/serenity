/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.util.stream.IntStream;

/**
 * @test
 * @bug 8244154 8242332
 * @summary Generate a <digest>withRSASSA-PSS signature and verify it using
 *         PKCS11 provider
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main SignatureTestPSS2
 */
public class SignatureTestPSS2 extends PKCS11Test {

    // PKCS11 does not support RSASSA-PSS keys yet
    private static final String KEYALG = "RSA";
    private static final String[] SIGALGS = {
            "SHA224withRSASSA-PSS", "SHA256withRSASSA-PSS",
            "SHA384withRSASSA-PSS", "SHA512withRSASSA-PSS",
            "SHA3-224withRSASSA-PSS", "SHA3-256withRSASSA-PSS",
            "SHA3-384withRSASSA-PSS", "SHA3-512withRSASSA-PSS"
    };

    private static final int[] KEYSIZES = { 2048, 3072 };

    /**
     * How much times signature updated.
     */
    private static final int UPDATE_TIMES = 2;

    public static void main(String[] args) throws Exception {
        main(new SignatureTestPSS2(), args);
    }

    @Override
    public void main(Provider p) throws Exception {
        for (String sa : SIGALGS) {
            Signature sig;
            try {
                sig = Signature.getInstance(sa, p);
            } catch (NoSuchAlgorithmException e) {
                System.out.println("Skip testing " + sa +
                    " due to no support");
                return;
            }
            for (int i : KEYSIZES) {
                runTest(sig, i);
            }
        }
    }

    private static void runTest(Signature s, int keySize) throws Exception {
        byte[] data = new byte[100];
        IntStream.range(0, data.length).forEach(j -> {
            data[j] = (byte) j;
        });
        System.out.println("[KEYSIZE = " + keySize + "]");

        // create a key pair
        KeyPair kpair = generateKeys(KEYALG, keySize, s.getProvider());
        test(s, kpair.getPrivate(), kpair.getPublic(), data);
    }

    private static void test(Signature sig, PrivateKey privKey,
            PublicKey pubKey, byte[] data) throws RuntimeException {
        // For signature algorithm, create and verify a signature
        try {
            checkSignature(sig, privKey, pubKey, data);
        } catch (NoSuchAlgorithmException | InvalidKeyException |
                 SignatureException | NoSuchProviderException ex) {
            throw new RuntimeException(ex);
        } catch (InvalidAlgorithmParameterException ex2) {
            System.out.println("Skip test due to " + ex2);
        }
    }

    private static KeyPair generateKeys(String keyalg, int size, Provider p)
            throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(keyalg, p);
        kpg.initialize(size);
        return kpg.generateKeyPair();
    }

    private static void checkSignature(Signature sig, PrivateKey priv,
            PublicKey pub, byte[] data) throws NoSuchAlgorithmException,
            InvalidKeyException, SignatureException, NoSuchProviderException,
            InvalidAlgorithmParameterException {
        System.out.println("Testing against " + sig.getAlgorithm());
        sig.initSign(priv);
        for (int i = 0; i < UPDATE_TIMES; i++) {
            sig.update(data);
        }
        byte[] signedData = sig.sign();

        // Make sure signature verifies with original data
        // do we need to call sig.setParameter(params) again?
        sig.initVerify(pub);
        for (int i = 0; i < UPDATE_TIMES; i++) {
            sig.update(data);
        }
        if (!sig.verify(signedData)) {
            throw new RuntimeException("Failed to verify signature");
        }

        // Make sure signature does NOT verify when the original data
        // has changed
        sig.initVerify(pub);
        for (int i = 0; i < UPDATE_TIMES + 1; i++) {
            sig.update(data);
        }

        if (sig.verify(signedData)) {
            throw new RuntimeException("Failed to detect bad signature");
        }
    }
}
