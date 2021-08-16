/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8008296
 * @summary Store and retrieve user passwords using PKCS#12 keystore
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;

import java.io.*;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/*
 * Store and retrieve passwords protected by a selection of PBE algorithms,
 * using a PKCS#12 keystore.
 */
public class StorePasswords {

    private static final String[] PBE_ALGORITHMS = new String[] {
        "default PBE algorithm",
        "PBEWithMD5AndDES",
        "PBEWithSHA1AndDESede",
        "PBEWithSHA1AndRC2_40",
        "PBEWithSHA1AndRC2_128",
        "PBEWithSHA1AndRC4_40",
        "PBEWithSHA1AndRC4_128",
        "PBEWithHmacSHA1AndAES_128",
        "PBEWithHmacSHA224AndAES_128",
        "PBEWithHmacSHA256AndAES_128",
        "PBEWithHmacSHA384AndAES_128",
        "PBEWithHmacSHA512AndAES_128",
        "PBEWithHmacSHA1AndAES_256",
        "PBEWithHmacSHA224AndAES_256",
        "PBEWithHmacSHA256AndAES_256",
        "PBEWithHmacSHA384AndAES_256",
        "PBEWithHmacSHA512AndAES_256"
    };

    private static final String KEYSTORE = "mykeystore.p12";
    private static final char[] KEYSTORE_PWD = "changeit".toCharArray();
    private static final char[] ENTRY_PWD = "protectit".toCharArray();
    private static final char[] USER_PWD = "hello1".toCharArray();

    public static void main(String[] args) throws Exception {

        new File(KEYSTORE).delete();

        int storeCount = store();
        int recoverCount = recover();

        if (recoverCount != storeCount) {
            throw new Exception("Stored " + storeCount + " user passwords, " +
                "recovered " + recoverCount + " user passwords");
        }
        System.out.println("\nStored " + storeCount + " user passwords, " +
            "recovered " + recoverCount + " user passwords");

        new File(KEYSTORE).delete();

        storeCount = storeByShell();
        recoverCount = recoverByShell();

        if (recoverCount != storeCount || storeCount < 11) {
            throw new Exception("Stored " + storeCount + " user passwords, " +
                    "recovered " + recoverCount + " user passwords");
        }
        System.out.println("\nStored " + storeCount + " user passwords, " +
                "recovered " + recoverCount + " user passwords");

        new File(KEYSTORE).delete();
    }

    private static int store() throws Exception {
        int count = 0;
        // Load an empty PKCS#12 keystore
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        System.out.println("\nLoading PKCS#12 keystore...");
        keystore.load(null, null);

        // Derive a PBE key from the password
        PBEKeySpec keySpec = new PBEKeySpec(USER_PWD);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
        SecretKey key = factory.generateSecret(keySpec);
        PBEParameterSpec specWithEightByteSalt =
            new PBEParameterSpec("NaClNaCl".getBytes(), 1024);

        // Store the user password in a keystore entry (for each algorithm)
        for (String algorithm : PBE_ALGORITHMS) {

            try {
                System.out.println("Storing user password '" +
                    new String(USER_PWD) + "' (protected by " + algorithm +
                    ")");

                if (algorithm.equals("default PBE algorithm")) {
                     keystore.setKeyEntry(
                         "this entry is protected by " + algorithm, key,
                         ENTRY_PWD, null);
                } else {
                    keystore.setEntry(
                        "this entry is protected by " + algorithm,
                        new KeyStore.SecretKeyEntry(key),
                        new KeyStore.PasswordProtection(ENTRY_PWD, algorithm,
                            null));
                }
                count++;

            } catch (KeyStoreException e) {
                Throwable inner = e.getCause();
                if (inner instanceof UnrecoverableKeyException) {
                    Throwable inner2 = inner.getCause();
                    if (inner2 instanceof InvalidAlgorithmParameterException) {
                        System.out.println("...re-trying due to: " +
                            inner2.getMessage());

                        // Some PBE algorithms demand an 8-byte salt
                        keystore.setEntry(
                            "this entry is protected by " + algorithm,
                            new KeyStore.SecretKeyEntry(key),
                            new KeyStore.PasswordProtection(ENTRY_PWD,
                                algorithm, specWithEightByteSalt));
                        count++;

                    } else if (inner2  instanceof InvalidKeyException) {
                        System.out.println("...skipping due to: " +
                            inner2.getMessage());
                        // Unsupported crypto keysize
                        continue;
                    }
                } else {
                    throw e;
                }
            }
        }

        // Store the PKCS#12 keystore
        System.out.println("Storing PKCS#12 keystore to: " + KEYSTORE);
        try (FileOutputStream out = new FileOutputStream(KEYSTORE)) {
            keystore.store(out, KEYSTORE_PWD);
        }

        return count;
    }

    private static int recover() throws Exception {
        int count = 0;
        // Load the PKCS#12 keystore
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        System.out.println("\nLoading PKCS#12 keystore from: " + KEYSTORE);
        try (FileInputStream in = new FileInputStream(KEYSTORE)) {
            keystore.load(in, KEYSTORE_PWD);
        }

        SecretKey key;
        SecretKeyFactory factory;
        PBEKeySpec keySpec;

        // Retrieve each user password from the keystore
        for (String algorithm : PBE_ALGORITHMS) {
            key = (SecretKey) keystore.getKey("this entry is protected by " +
                algorithm, ENTRY_PWD);

            if (key != null) {
                count++;
                factory = SecretKeyFactory.getInstance(key.getAlgorithm());
                keySpec =
                    (PBEKeySpec) factory.getKeySpec(key, PBEKeySpec.class);
                char[] pwd = keySpec.getPassword();
                System.out.println("Recovered user password '" +
                     new String(pwd) + "' (protected by " + algorithm + ")");

                if (!Arrays.equals(USER_PWD, pwd)) {
                    throw new Exception("Failed to recover the user password " +
                        "protected by " + algorithm);
                }
            }
        }

        return count;
    }

    private static int storeByShell() throws Exception {
        int count = 0;
        for (String algorithm : PBE_ALGORITHMS) {
            System.out.println("Storing user password (protected by " + algorithm + " )");
            String importCmd = count < 5 ? "-importpassword" : "-importpass";
            String keyAlg = algorithm.equals("default PBE algorithm")
                    ? "" : (" -keyalg " + algorithm);
            SecurityTools.setResponse("hello1");
            OutputAnalyzer oa = SecurityTools.keytool(importCmd
                    + " -storetype pkcs12 -keystore mykeystore.p12"
                    + " -storepass changeit -alias `this entry is protected by "
                    + algorithm + "`" + keyAlg);
            if (oa.getExitValue() == 0) {
                System.out.println("OK");
                count++;
            } else {
                System.out.println("ERROR");
            }
        }
        return count;
    }

    private static int recoverByShell() throws Exception {
        return (int)SecurityTools.keytool("-list -storetype pkcs12"
                + " -keystore mykeystore.p12 -storepass changeit")
                .shouldHaveExitValue(0)
                .asLines().stream()
                .filter(s -> s.contains("this entry is protected by"))
                .count();
    }
}
