/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
import static java.lang.System.out;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;

/*
 * @test
 * @bug 8048604
 * @summary to verify cipherInputStream and cipherInputStream cipher function
 * @run main CICODESFuncTest
 */
public class CICODESFuncTest {
    /**
     * Algorithms name.
     */
    private static final String[] ALGORITHMS = { "DES", "DESede", "Blowfish" };
    private static final String[] MODES = { "ECB", "CBC", "CFB", "CFB24",
            "CFB32", "CFB40", "CFB72", "OFB", "OFB20", "OFB48", "OFB56",
            "OFB64", "PCBC" };
    /**
     * Padding mode.
     */
    private static final String[] PADDINGS = { "noPadding", "pkcs5padding" };
    /**
     * Plain text length.
     */
    private static final int TEXT_LENGTH = 80;
    /**
     * Initialization vector length.
     */
    private static final int IV_LENGTH = 8;

    public static void main(String[] args) throws Exception {
        Provider provider = Security.getProvider("SunJCE");
        if (provider == null) {
            throw new RuntimeException("SunJCE provider does not exist.");
        }
        for (String algorithm : ALGORITHMS) {
            for (String mode : MODES) {
                // We only test noPadding and pkcs5padding for CFB72, OFB20, ECB
                // PCBC and CBC. Otherwise test noPadding only.
                int padKinds = 1;
                if (mode.equalsIgnoreCase("CFB72")
                        || mode.equalsIgnoreCase("OFB20")
                        || mode.equalsIgnoreCase("ECB")
                        || mode.equalsIgnoreCase("PCBC")
                        || mode.equalsIgnoreCase("CBC")) {
                    padKinds = PADDINGS.length;
                }
                // PKCS5padding is meaningful only for ECB, CBC, PCBC
                for (int k = 0; k < padKinds; k++) {
                    for (ReadModel readMode : ReadModel.values()) {
                        runTest(provider, algorithm, mode, PADDINGS[k], readMode);
                    }
                }
            }
        }
    }

    private static void runTest(Provider p, String algo, String mo, String pad,
            ReadModel whichRead) throws GeneralSecurityException, IOException {
        // Do initialization
        byte[] plainText = TestUtilities.generateBytes(TEXT_LENGTH);
        byte[] iv = TestUtilities.generateBytes(IV_LENGTH);
        AlgorithmParameterSpec aps = new IvParameterSpec(iv);
        try {
            KeyGenerator kg = KeyGenerator.getInstance(algo, p);
            out.println(algo + "/" + mo + "/" + pad + "/" + whichRead);
            SecretKey key = kg.generateKey();
            Cipher ci1 = Cipher.getInstance(algo + "/" + mo + "/" + pad, p);
            if ("CFB72".equalsIgnoreCase(mo) || "OFB20".equalsIgnoreCase(mo)) {
                throw new RuntimeException(
                        "NoSuchAlgorithmException not throw when mode"
                                + " is CFB72 or OFB20");
            }
            Cipher ci2 = Cipher.getInstance(algo + "/" + mo + "/" + pad, p);
            if ("ECB".equalsIgnoreCase(mo)) {
                ci1.init(Cipher.ENCRYPT_MODE, key);
                ci2.init(Cipher.DECRYPT_MODE, key);
            } else {
                ci1.init(Cipher.ENCRYPT_MODE, key, aps);
                ci2.init(Cipher.DECRYPT_MODE, key, aps);
            }
            ByteArrayOutputStream baOutput = new ByteArrayOutputStream();
            try (CipherInputStream cInput
                    = new CipherInputStream(
                            new ByteArrayInputStream(plainText), ci1);
                    CipherOutputStream ciOutput
                        = new CipherOutputStream(baOutput, ci2);) {
                // Read from the input and write to the output using 2 types
                // of buffering : byte[] and int
                whichRead.read(cInput, ciOutput, ci1, plainText.length);
            }
            // Verify input and output are same.
            if (!Arrays.equals(plainText, baOutput.toByteArray())) {
                throw new RuntimeException("Test failed due to compare fail ");
            }
        } catch (NoSuchAlgorithmException nsaEx) {
            if ("CFB72".equalsIgnoreCase(mo) || "OFB20".equalsIgnoreCase(mo)) {
                out.println("NoSuchAlgorithmException is expected for CFB72 and OFB20");
            } else {
                throw new RuntimeException("Unexpected exception testing: "
                        + algo + "/" + mo + "/" + pad + "/" + whichRead, nsaEx);
            }
        }
    }
}
