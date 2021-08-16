/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
import java.io.File;
import static java.lang.System.err;
import java.security.*;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.crypto.spec.PBEParameterSpec;
import jdk.test.lib.RandomFactory;
import static java.lang.System.out;
import java.util.Arrays;

/**
 * @test
 * @bug 8048830
 * @summary Test for feature 'support stronger entry protection'. An entry is
 * stored to keystore with different PasswordProtection objects which are
 * specified by different PBE algorithms (use -Dseed=X to set PRNG seed)
 * @library /test/lib ../
 * @key randomness
 * @build jdk.test.lib.RandomFactory
 * @run main EntryProtectionTest
 */
public class EntryProtectionTest {
    private static final char[] PASSWORD = "passwd".toCharArray();
    private static final String ALIAS = "testkey";
    private static final byte[] SALT = new byte[8];
    private static final int ITERATION_COUNT = 1024;
    private static final List<KeyStore.PasswordProtection> PASSWORD_PROTECTION
            = new ArrayList<>();
    private static final String KEYSTORE_PATH = System.getProperty(
            "test.classes" + File.separator + "ks.pkcs12",
            "." + File.separator + "ks.pkcs12");

    private void runTest() throws Exception {
            KeyStore ksIn = Utils.loadKeyStore(KEYSTORE_PATH,
                    Utils.KeyStoreType.pkcs12, PASSWORD);
            KeyStore ksTest = KeyStore
                    .getInstance(Utils.KeyStoreType.pkcs12.name());
            ksTest.load(null);
            Certificate cert = ksIn.getCertificate(ALIAS);
            Key key = ksIn.getKey(ALIAS, PASSWORD);
            KeyStore.Entry keyStoreEntry = new KeyStore.PrivateKeyEntry(
                    (PrivateKey) key, new Certificate[]{cert});
            for (KeyStore.PasswordProtection passwordAlgorithm :
                    PASSWORD_PROTECTION) {
                out.println("Try to use: " +
                        passwordAlgorithm.getProtectionAlgorithm());
                ksTest.setEntry(ALIAS, keyStoreEntry, passwordAlgorithm);
                KeyStore.Entry entryRead = ksTest.getEntry(ALIAS,
                        new KeyStore.PasswordProtection(PASSWORD));
                if (!isPrivateKeyEntriesEqual((KeyStore.PrivateKeyEntry)
                        keyStoreEntry, (KeyStore.PrivateKeyEntry)entryRead)) {
                    err.println("Original entry in KeyStore: " + keyStoreEntry);
                    err.println("Enc/Dec entry : " + entryRead);
                    throw new RuntimeException(
                            String.format(
                                    "Decrypted & original enities do "
                                    + "not match. Algo: %s, Actual: %s, "
                                    + "Expected: %s",
                                    passwordAlgorithm.getProtectionAlgorithm(),
                                    entryRead, keyStoreEntry));
                }
                ksTest.deleteEntry(ALIAS);
            }
            out.println("Test Passed");
    }

    public static void main(String args[]) throws Exception {
        EntryProtectionTest entryProtectionTest = new EntryProtectionTest();
        entryProtectionTest.setUp();
        entryProtectionTest.runTest();
    }

    private void setUp() {
        out.println("Using KEYSTORE_PATH:"+KEYSTORE_PATH);
        Utils.createKeyStore(Utils.KeyStoreType.pkcs12, KEYSTORE_PATH, ALIAS);
        Random rand = RandomFactory.getRandom();
        rand.nextBytes(SALT);
        out.print("Salt: ");
        for (byte b : SALT) {
            out.format("%02X ", b);
        }
        out.println("");
        PASSWORD_PROTECTION
                .add(new KeyStore.PasswordProtection(PASSWORD,
                                "PBEWithMD5AndDES", new PBEParameterSpec(SALT,
                                        ITERATION_COUNT)));
        PASSWORD_PROTECTION.add(new KeyStore.PasswordProtection(PASSWORD,
                "PBEWithSHA1AndDESede", null));
        PASSWORD_PROTECTION.add(new KeyStore.PasswordProtection(PASSWORD,
                "PBEWithSHA1AndRC2_40", null));
        PASSWORD_PROTECTION.add(new KeyStore.PasswordProtection(PASSWORD,
                "PBEWithSHA1AndRC2_128", null));
        PASSWORD_PROTECTION.add(new KeyStore.PasswordProtection(PASSWORD,
                "PBEWithSHA1AndRC4_40", null));
        PASSWORD_PROTECTION.add(new KeyStore.PasswordProtection(PASSWORD,
                "PBEWithSHA1AndRC4_128", null));
    }

    /**
     * Checks whether given two KeyStore.PrivateKeyEntry parameters are equal
     * The KeyStore.PrivateKeyEntry fields like {privateKey, certificateChain[]}
     * are checked for equality and another field Set<attributes> is not checked
     * as default implementation adds few PKCS12 attributes during read
     * operation
     * @param  first
     *         parameter is of type KeyStore.PrivateKeyEntry
     * @param  second
     *         parameter is of type KeyStore.PrivateKeyEntry
     * @return boolean
     *         true when both the KeyStore.PrivateKeyEntry fields are equal
    */
    boolean isPrivateKeyEntriesEqual(KeyStore.PrivateKeyEntry first,
            KeyStore.PrivateKeyEntry second) {
        //compare privateKey
        if (!Arrays.equals(first.getPrivateKey().getEncoded(),
                second.getPrivateKey().getEncoded())) {
            err.println("Mismatch found in privateKey!");
            return false;
        }
        //compare certificateChain[]
        if (!Arrays.equals(first.getCertificateChain(),
                second.getCertificateChain())) {
            err.println("Mismatch found in certificate chain!");
            return false;
        }
        return true;
    }
}
