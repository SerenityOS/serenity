/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8265500
 * @summary
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestCipherMode
 */

import java.security.Provider;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class TestCipherMode extends PKCS11Test {

    private static String[] TRANSFORMATIONS = {
        "AES/ECB/PKCS5Padding", "AES/GCM/NoPadding",
        "RSA/ECB/PKCS1Padding"
    };

    private static byte[] BYTES16 =
            Arrays.copyOf(TRANSFORMATIONS[0].getBytes(), 16);
    private static SecretKey AES_KEY = new SecretKeySpec(BYTES16, "AES");
    private static PublicKey RSA_PUBKEY = null;
    private static PrivateKey RSA_PRIVKEY = null;

    enum CipherMode {
        ENCRYPT(Cipher.ENCRYPT_MODE),
        DECRYPT(Cipher.DECRYPT_MODE),
        WRAP(Cipher.WRAP_MODE),
        UNWRAP(Cipher.UNWRAP_MODE),
        NONEXISTENT(100);

        int value;

        CipherMode(int value) {
            this.value = value;
        }
    }

    private static Key getKey(String t, CipherMode m, Provider p)
            throws NoSuchAlgorithmException {
        if (t.startsWith("AES")) {
            return AES_KEY;
        } else if (t.startsWith("RSA")) {
            if (RSA_PUBKEY == null) {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", p);
                KeyPair kp = kpg.generateKeyPair();
                RSA_PUBKEY = kp.getPublic();
                RSA_PRIVKEY = kp.getPrivate();
            }
            return ((m == CipherMode.ENCRYPT || m == CipherMode.UNWRAP)?
                    RSA_PRIVKEY : RSA_PUBKEY);
        } else {
            throw new RuntimeException("Unknown transformation: " + t);
        }
    }

    public static void main(String[] args) throws Exception {
        main(new TestCipherMode(), args);
    }

    @Override
    public void main(Provider p) throws Exception {

        // test all cipher impls, e.g. P11Cipher, P11AEADCipher, and
        // P11RSACipher
        for (String t : TRANSFORMATIONS) {
            checkModes(t, p);
        }
        System.out.println("All tests passed");
    }

    private static void checkModes(String t, Provider p) throws Exception {
        try {
            Cipher.getInstance(t, p);
        } catch (Exception e) {
            System.out.println("Skip " + t + " due to " + e.getMessage());
            return;
        }

        for (CipherMode m : CipherMode.values()) {
            System.out.println("Testing " + t + " with " + m.name());
            Cipher c;
            try {
                c = Cipher.getInstance(t, p);
                // try init and see if the expected Exception is thrown
                c.init(m.value, getKey(t, m, p), c.getParameters());
                if (m == CipherMode.NONEXISTENT) {
                    throw new Exception("ERROR: should throw IPE with init()");
                }
            } catch (UnsupportedOperationException uoe)  {
                // some may not support wrap/unwrap
                if (m == CipherMode.WRAP || m == CipherMode.UNWRAP) {
                    System.out.println("Expected UOE thrown with init()");
                    continue;
                }
                throw uoe;
            } catch (InvalidParameterException ipe) {
                if (m == CipherMode.NONEXISTENT) {
                    System.out.println("Expected IPE thrown for init()");
                    continue;
                }
                throw ipe;
            }
            switch (m) {
            case ENCRYPT:
            case DECRYPT:
                // call wrap()/unwrap() and see if ISE is thrown.
                try {
                    c.wrap(AES_KEY);
                    throw new Exception("ERROR: should throw ISE for wrap()");
                } catch (IllegalStateException ise) {
                    System.out.println("Expected ISE thrown for wrap()");
                }
                try {
                    c.unwrap(BYTES16, "AES", Cipher.SECRET_KEY);
                    throw new Exception("ERROR: should throw ISE for unwrap()");
                } catch (IllegalStateException ise) {
                    System.out.println("Expected ISE thrown for unwrap()");
                }
                break;
            case WRAP:
            case UNWRAP:
                try {
                    c.update(BYTES16);
                    throw new Exception("ERROR: should throw ISE for update()");
                } catch (IllegalStateException ise) {
                    System.out.println("Expected ISE thrown for update()");
                }
                try {
                    c.doFinal();
                    throw new Exception("ERROR: should throw ISE for" +
                            " doFinal()");
                } catch (IllegalStateException ise) {
                    System.out.println("Expected ISE thrown for doFinal()");
                }
                break;
            default:
                throw new AssertionError();
            }
        }
    }
}
