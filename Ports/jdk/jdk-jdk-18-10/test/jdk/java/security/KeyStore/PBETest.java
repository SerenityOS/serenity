/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8006591
 * @summary Protect keystore entries using stronger PBE algorithms
 */

import java.io.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

// Retrieve a keystore entry, protected by the default encryption algorithm.
// Set the keystore entry, protected by a stronger encryption algorithm.

public class PBETest {
    private final static String DIR = System.getProperty("test.src", ".");
    private final static String KEY_PROTECTION_PROPERTY =
        "keystore.PKCS12.keyProtectionAlgorithm";
    private static final String[] PBE_ALGOS = {
        "PBEWithSHA1AndDESede",
        "PBEWithHmacSHA1AndAES_128",
        "PBEWithHmacSHA224AndAES_128",
        "PBEWithHmacSHA256AndAES_128",
        "PBEWithHmacSHA384AndAES_128",
        "PBEWithHmacSHA512AndAES_128"
    };
    private static final char[] PASSWORD = "passphrase".toCharArray();
    private static final String KEYSTORE_TYPE = "JKS";
    private static final String KEYSTORE = DIR + "/keystore.jks";
    private static final String NEW_KEYSTORE_TYPE = "PKCS12";
    private static final String ALIAS = "vajra";

    private static final byte[] IV = {
        0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,
        0x19,0x1A,0x1B,0x1C,0x1D,0x1E,0x1F,0x20
    };
    private static final byte[] SALT = {
        0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08
    };
    private static final int ITERATION_COUNT = 1024;

    public static void main(String[] args) throws Exception {
        for (String algo : PBE_ALGOS) {
            String filename = algo + ".p12";
            main0(algo, filename, true);
            main0(algo, filename, false);
            Security.setProperty(KEY_PROTECTION_PROPERTY, algo);
            main0(null, "PBE.p12", false);
            Security.setProperty(KEY_PROTECTION_PROPERTY, "");
        }
        main0(null, "default.p12", false); // default algorithm
    }

    private static void main0(String algo, String filename, boolean useParams)
        throws Exception {

        KeyStore keystore = load(KEYSTORE_TYPE, KEYSTORE, PASSWORD);
        KeyStore.Entry entry =
            keystore.getEntry(ALIAS,
                new KeyStore.PasswordProtection(PASSWORD));
        System.out.println("Retrieved key entry named '" + ALIAS + "'");
        Key originalKey = null;
        if (entry instanceof KeyStore.PrivateKeyEntry) {
            originalKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
        } else if (entry instanceof KeyStore.SecretKeyEntry) {
            originalKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }

        // Set entry
        KeyStore keystore2 = load(NEW_KEYSTORE_TYPE, null, null);
        if (useParams) {
            keystore2.setEntry(ALIAS, entry,
                new KeyStore.PasswordProtection(PASSWORD, algo,
                    new PBEParameterSpec(SALT, ITERATION_COUNT,
                        new IvParameterSpec(IV))));
            System.out.println("Encrypted key entry using: " + algo +
                " (with PBE parameters)");
        } else if (algo != null) {
            keystore2.setEntry(ALIAS, entry,
                new KeyStore.PasswordProtection(PASSWORD, algo, null));
            System.out.println("Encrypted key entry using: " + algo +
                " (without PBE parameters)");
        } else {
            keystore2.setEntry(ALIAS, entry,
                new KeyStore.PasswordProtection(PASSWORD));
            String prop = Security.getProperty(KEY_PROTECTION_PROPERTY);
            if (prop == null || prop.isEmpty()) {
                System.out.println("Encrypted key entry using: " +
                    "default PKCS12 key protection algorithm");
            } else {
                System.out.println("Encrypted key entry using: " +
                    "keyProtectionAlgorithm=" + prop);
            }
        }

        try (FileOutputStream outStream = new FileOutputStream(filename)) {
            System.out.println("Storing keystore to: " + filename);
            keystore2.store(outStream, PASSWORD);
        }

        try {
            keystore2 = load(NEW_KEYSTORE_TYPE, filename, PASSWORD);
            entry = keystore2.getEntry(ALIAS,
                new KeyStore.PasswordProtection(PASSWORD));
            Key key;
            if (entry instanceof KeyStore.PrivateKeyEntry) {
                key = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
            } else if (entry instanceof KeyStore.SecretKeyEntry) {
                key = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
            } else {
                throw new Exception("Failed to retrieve key entry");
            }
            if (originalKey.equals(key)) {
                System.out.println("Retrieved key entry named '" + ALIAS + "'");
                System.out.println();
            } else {
                throw new Exception(
                    "Failed: recovered key does not match the original key");
            }

        } finally {
            new File(filename).delete();
        }
    }

    private static KeyStore load(String type, String path, char[] password)
        throws Exception {
        KeyStore keystore = KeyStore.getInstance(type);

        if (path != null) {

            try (FileInputStream inStream = new FileInputStream(path)) {
                System.out.println("Loading keystore from: " + path);
                keystore.load(inStream, password);
                System.out.println("Loaded keystore with " + keystore.size() +
                    " entries");
            }
        } else {
            keystore.load(null, null);
        }

        return keystore;
    }
}
