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
 * @bug 8255410
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestChaChaPoly
 * @summary test for PKCS#11 ChaCha20-Poly1305 Cipher.
 */

import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;
import java.util.HexFormat;

import javax.crypto.Cipher;
import javax.crypto.spec.ChaCha20ParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.NoSuchPaddingException;

import jdk.test.lib.Utils;

public class TestChaChaPoly extends PKCS11Test {

    private static final byte[] NONCE
            = HexFormat.of().parseHex("012345670123456701234567");
    private static final SecretKeySpec KEY = new SecretKeySpec(
            HexFormat.of().parseHex("0123456701234567012345670123456701234567012345670123456701234567"),
            "ChaCha20");
    private static final ChaCha20ParameterSpec CHACHA20_PARAM_SPEC
            = new ChaCha20ParameterSpec(NONCE, 0);
    private static final IvParameterSpec IV_PARAM_SPEC
            = new IvParameterSpec(NONCE);
    private static final String ALGO = "ChaCha20-Poly1305";
    private static final SecureRandom RAND = new SecureRandom();
    private static Provider p;

    @Override
    public void main(Provider p) throws Exception {
        System.out.println("Testing " + p.getName());
        try {
            Cipher.getInstance(ALGO, p);
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("Skip; no support for " + ALGO);
            return;
        }
        this.p = p;
        testTransformations();
        testInit();
        testAEAD();
        testGetBlockSize();
        testGetIV();
        testInterop("SunJCE");
    }

    private static void testTransformations() throws Exception {
        System.out.println("== transformations ==");

        checkTransformation(p, ALGO, true);
        checkTransformation(p, ALGO + "/None/NoPadding", true);
        checkTransformation(p, ALGO + "/ECB/NoPadding", false);
        checkTransformation(p, ALGO + "/None/PKCS5Padding", false);
    }

    private static void checkTransformation(Provider p, String t,
            boolean expected) throws Exception {
        try {
            Cipher.getInstance(t, p);
            if (!expected) {
                throw new RuntimeException( "Should reject transformation: " +
                        t);
            } else {
                System.out.println("Accepted transformation: " + t);
            }
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            if (!expected) {
                System.out.println("Rejected transformation: " + t);
            } else {
                throw new RuntimeException("Should accept transformation: " +
                        t, e);
            }
        }
    }

    private static void testInit() throws Exception {
        testInitOnCrypt(Cipher.ENCRYPT_MODE);
        testInitOnCrypt(Cipher.DECRYPT_MODE);
    }

    private static void testInitOnCrypt(int opMode) throws Exception {
        System.out.println("== init (" + getOpModeName(opMode) + ") ==");

        // Need to acquire new Cipher object as ChaCha20-Poly1305 cipher
        // disallow reusing the same key and iv pair
        Cipher.getInstance(ALGO, p).init(opMode, KEY, IV_PARAM_SPEC);
        Cipher c = Cipher.getInstance(ALGO, p);
        c.init(opMode, KEY, IV_PARAM_SPEC, RAND);
        AlgorithmParameters params = c.getParameters();
        Cipher.getInstance(ALGO, p).init(opMode, KEY, params, RAND);

        try {
            // try with invalid param
            Cipher.getInstance(ALGO, p).init(opMode, KEY, CHACHA20_PARAM_SPEC);
            throw new RuntimeException("Should reject non-IvparameterSpec");
        } catch (InvalidAlgorithmParameterException e) {
            System.out.println("Expected IAPE - " + e);
        }
    }

    private static void testAEAD() throws Exception {
        byte[] expectedPt = HexFormat.of().parseHex("01234567");
        byte[] ct = testUpdateAAD(Cipher.ENCRYPT_MODE, expectedPt);
        byte[] pt = testUpdateAAD(Cipher.DECRYPT_MODE, ct);
        if (pt != null && !Arrays.equals(pt, expectedPt)) {
            System.out.println("ciphertext: " + Arrays.toString(ct));
            System.out.println("plaintext: " + Arrays.toString(pt));
            throw new RuntimeException("AEAD failed");
        }
    }

    private static byte[] testUpdateAAD(int opMode, byte[] input)
            throws Exception {
        String opModeName = getOpModeName(opMode);
        System.out.println("== updateAAD (" + opModeName + ") ==");

        byte[] aad = HexFormat.of().parseHex("0000");
        ByteBuffer aadBuf = ByteBuffer.wrap(aad);

        Cipher ccp = Cipher.getInstance(ALGO, p);
        try {
            ccp.updateAAD(aadBuf);
            throw new RuntimeException(
                    "Should throw ISE for setting AAD on uninit'ed Cipher");
        } catch (IllegalStateException e) {
            System.out.println("Expected ISE - " + e);
        }

        ccp.init(opMode, KEY, IV_PARAM_SPEC);
        ccp.update(input);
        try {
            ccp.updateAAD(aad);
            throw new RuntimeException(
                    "Should throw ISE for setting AAD after update");
        } catch (IllegalStateException e) {
            System.out.println("Expected ISE - " + e);
        }

        ccp.init(opMode, KEY, IV_PARAM_SPEC);
        ccp.updateAAD(aadBuf);
        return ccp.doFinal(input);
    }

    private static void testGetBlockSize() throws Exception {
        testGetBlockSize(Cipher.ENCRYPT_MODE);
        testGetBlockSize(Cipher.DECRYPT_MODE);
    }

    private static void testGetBlockSize(int opMode) throws Exception {
        System.out.println("== getBlockSize (" + getOpModeName(opMode) + ") ==");
        Cipher c = Cipher.getInstance(ALGO, p);
        if (c.getBlockSize() != 0) {
            throw new RuntimeException("Block size must be 0");
        }
    }

    private static void testGetIV() throws Exception {
        testGetIV(Cipher.ENCRYPT_MODE);
        testGetIV(Cipher.DECRYPT_MODE);
    }

    private static void testGetIV(int opMode) throws Exception {
        System.out.println("== getIv (" + getOpModeName(opMode) + ") ==");

        try {
            Cipher.getInstance(ALGO, p).getIV();
            Cipher.getInstance(ALGO, p).getParameters();
        } catch (Exception e) {
            throw new RuntimeException("Should not throw ex", e);
        }
        // first init w/ key only
        AlgorithmParameters params = null;
        for (int i = 0; i < 6; i++) {
            System.out.println("IV test# " + i);
            Cipher c = Cipher.getInstance(ALGO, p);
            byte[] expectedIV = NONCE;
            try {
                switch (i) {
                case 0 -> {
                    c.init(opMode, KEY);
                    expectedIV = null; // randomly-generated
                }
                case 1 -> {
                    c.init(opMode, KEY, RAND);
                    expectedIV = null; // randomly-generated
                }
                case 2 -> {
                    c.init(opMode, KEY, IV_PARAM_SPEC);
                    params = c.getParameters();
                    if (params == null) {
                        throw new RuntimeException("Params should not be null");
                    }
                }
                case 3 -> c.init(opMode, KEY, IV_PARAM_SPEC, RAND);
                case 4 -> c.init(opMode, KEY, params);
                case 5 -> c.init(opMode, KEY, params, RAND);
                }
                checkIV(c, expectedIV);
                System.out.println("=> Passed");
            } catch (GeneralSecurityException e) {
                if (opMode == Cipher.DECRYPT_MODE && i < 2) {
                    System.out.println("=> Passed: Expected Ex thrown");
                } else {
                    throw new RuntimeException("Should not throw ex", e);
                }
            }
        }
    }

    private static void checkIV(Cipher c, byte[] expectedIv) {
        // the specified cipher has been initialized; the returned IV and
        // AlgorithmParameters object should be non-null
        byte[] iv = c.getIV();
        AlgorithmParameters params = c.getParameters();
        // fail if either is null
        if (iv == null || params == null) {
            throw new RuntimeException("getIV()/getParameters() should " +
                    "not return null");
        }

        // check iv matches if not null
        if (expectedIv != null && !Arrays.equals(expectedIv, iv)) {
            throw new RuntimeException("IV should match expected value");
        }

        try {
            byte[] iv2 = params.getParameterSpec(IvParameterSpec.class).getIV();
            if (!Arrays.equals(iv, iv2)) {
                throw new RuntimeException("IV values should be consistent");
            }
        } catch (InvalidParameterSpecException ipe) {
            // should never happen
            throw new AssertionError();
        }
    }

    private static void testInterop(String interopProv) throws Exception {
        testInterop(Cipher.getInstance(ALGO, p),
                Cipher.getInstance(ALGO, interopProv));
        testInterop(Cipher.getInstance(ALGO, interopProv),
                Cipher.getInstance(ALGO, p));
    }

    private static void testInterop(Cipher encCipher, Cipher decCipher)
            throws Exception {
        System.out.println("Interop: " + encCipher.getProvider().getName() +
                " -> " + encCipher.getProvider().getName());
        byte[] pt = HexFormat.of().parseHex("012345678901234567890123456789");
        encCipher.init(Cipher.ENCRYPT_MODE, KEY);
        byte[] ct = encCipher.doFinal(pt);
        decCipher.init(Cipher.DECRYPT_MODE, KEY, encCipher.getParameters());
        byte[] pt2 = decCipher.doFinal(ct);
        if (!Arrays.equals(pt, pt2)) {
            System.out.println("HexDump/pt: " + HexFormat.of().formatHex(pt));
            System.out.println("HexDump/pt2: " + HexFormat.of().formatHex(pt2));
            throw new RuntimeException("Recovered data should match");
        }
        System.out.println("=> Passed");
    }

    private static String getOpModeName(int opMode) {
        switch (opMode) {
        case Cipher.ENCRYPT_MODE:
            return "ENCRYPT";

        case Cipher.DECRYPT_MODE:
            return "DECRYPT";

        case Cipher.WRAP_MODE:
            return "WRAP";

        case Cipher.UNWRAP_MODE:
            return "UNWRAP";

        default:
            return "";
        }
    }

    public static void main(String[] args) throws Exception {
        main(new TestChaChaPoly(), args);
    }
}
