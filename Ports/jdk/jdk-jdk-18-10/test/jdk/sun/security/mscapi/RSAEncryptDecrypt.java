/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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


/**
 * @test
 * @bug 6457422 6931562 8180570
 * @summary Confirm that plaintext can be encrypted and then decrypted using the
 *     RSA cipher in the SunMSCAPI crypto provider. NOTE: The RSA cipher is
 *     absent from the SunMSCAPI provider in OpenJDK builds.
 * @requires os.family == "windows"
 */

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.Key;

public class RSAEncryptDecrypt {
    public static final byte[] PLAINTEXT = {1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6};

    public static void main(String[] args) throws Exception {

        KeyPairGenerator generator =
            KeyPairGenerator.getInstance("RSA", "SunMSCAPI");

        KeyPair keyPair = generator.generateKeyPair();
        Key publicKey = keyPair.getPublic();
        Key privateKey = keyPair.getPrivate();

        Cipher cipher = null;

        try {
            cipher = Cipher.getInstance("RSA", "SunMSCAPI");

        } catch (GeneralSecurityException e) {
            System.out.println("Cipher not supported by provider, skipping...");
            return;
        }

        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        displayBytes("Plaintext data:", PLAINTEXT);
        byte[] data = cipher.doFinal(PLAINTEXT);
        displayBytes("Encrypted data:", data);

        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        data = cipher.doFinal(data);
        displayBytes("Decrypted data:", data);
    }

    private static void displayBytes(String label, byte[] bytes) {
        System.out.println(label + " [length=" + bytes.length + "]");
        for (byte b : bytes) {
            System.out.print("0x" + Integer.toHexString(b & 0xFF) + " ");
        }
        System.out.println();
    }
}
