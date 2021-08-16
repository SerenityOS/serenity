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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

/*
 * @test
 * @bug 8048596
 * @summary Check if wrong or empty AAD is rejected
 */
public class WrongAAD {

    private static final String PROVIDER = "SunJCE";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int TEXT_SIZE = 800;
    private static final int KEY_SIZE = 128;
    private static final int AAD_SIZE = 128;

    private final SecretKey key;
    private final byte[] plainText;
    private final Cipher encryptCipher;

    public WrongAAD() throws Exception {
        // init a secret key
        KeyGenerator kg = KeyGenerator.getInstance("AES", PROVIDER);
        kg.init(KEY_SIZE);
        key = kg.generateKey();

        // generate a plain text
        plainText = Helper.generateBytes(TEXT_SIZE);

        // init AADs
        byte[] AAD = Helper.generateBytes(AAD_SIZE);

        // init a cipher
        encryptCipher = createCipher(Cipher.ENCRYPT_MODE, null);
        encryptCipher.updateAAD(AAD);
    }

    public static void main(String[] args) throws Exception {
        WrongAAD test = new WrongAAD();
        test.decryptWithEmptyAAD();
        test.decryptWithWrongAAD();
    }

    /*
     * Attempt to decrypt a cipher text using Cipher object
     * initialized without AAD used for encryption.
     */
    private void decryptWithEmptyAAD() throws Exception {
        System.out.println("decryptWithEmptyAAD() started");
        // initialize it with empty AAD to get exception during decryption
        Cipher decryptCipher = createCipher(Cipher.DECRYPT_MODE,
                encryptCipher.getParameters());
        try (ByteArrayOutputStream baOutput = new ByteArrayOutputStream();
                CipherOutputStream ciOutput = new CipherOutputStream(baOutput,
                        decryptCipher)) {
            if (decrypt(ciOutput, baOutput)) {
                throw new RuntimeException(
                        "Decryption has been perfomed successfully in"
                                + " spite of the decrypt Cipher has NOT been"
                                + " initialized with AAD");
            }
        }
        System.out.println("decryptWithEmptyAAD() passed");
    }

    /*
     * Attempt to decrypt the cipher text using Cipher object
     * initialized with some fake AAD.
     */
    private void decryptWithWrongAAD() throws Exception {
        System.out.println("decrypt with wrong AAD");

        // initialize it with wrong AAD to get an exception during decryption
        Cipher decryptCipher = createCipher(Cipher.DECRYPT_MODE,
                encryptCipher.getParameters());
        byte[] someAAD = Helper.generateBytes(AAD_SIZE + 1);
        decryptCipher.updateAAD(someAAD);

        // init output stream
        try (ByteArrayOutputStream baOutput = new ByteArrayOutputStream();
                CipherOutputStream ciOutput = new CipherOutputStream(baOutput,
                        decryptCipher);) {
            if (decrypt(ciOutput, baOutput)) {
                throw new RuntimeException(
                        "A decryption has been perfomed successfully in"
                                + " spite of the decrypt Cipher has been"
                                + " initialized with fake AAD");
            }
        }

        System.out.println("Passed");
    }

    private boolean decrypt(CipherOutputStream ciOutput,
            ByteArrayOutputStream baOutput) throws IOException {
        try (ByteArrayInputStream baInput = new ByteArrayInputStream(plainText);
                CipherInputStream ciInput = new CipherInputStream(baInput,
                        encryptCipher)) {
            byte[] buffer = new byte[TEXT_SIZE];
            int len = ciInput.read(buffer);

            while (len != -1) {
                ciOutput.write(buffer, 0, len);
                len = ciInput.read(buffer);
            }
            ciOutput.flush();
            byte[] recoveredText = baOutput.toByteArray();
            System.out.println("recoveredText: " + new String(recoveredText));

            /*
             * See bug 8012900, AEADBadTagException is swalloed by CI/CO streams
             * If recovered text is empty, than decryption failed
             */
            if (recoveredText.length == 0) {
                return false;
            }
            return Arrays.equals(plainText, recoveredText);
        } catch (IllegalStateException e) {
            System.out.println("Expected IllegalStateException: "
                    + e.getMessage());
            e.printStackTrace(System.out);
            return false;
        }
    }

    private Cipher createCipher(int mode, AlgorithmParameters params)
            throws NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION, PROVIDER);
        if (params != null) {
            cipher.init(mode, key, params);
        } else {
            cipher.init(mode, key);
        }
        return cipher;
    }
}
