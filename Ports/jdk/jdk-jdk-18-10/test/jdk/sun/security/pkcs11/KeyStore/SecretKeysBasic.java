/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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
/* @test
 * @bug 6599979
 * @summary Ensure that re-assigning the alias works
 * @library /test/lib ..
 * @run testng/othervm SecretKeysBasic
 */
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class SecretKeysBasic extends PKCS11Test {

    private static final char SEP = File.separatorChar;
    private static char[] tokenPwd;
    private static final char[] nssPwd =
            new char[]{'t', 'e', 's', 't', '1', '2'};
    private static final char[] solarisPwd =
            new char[]{'p', 'i', 'n'};
    private static SecretKey sk1;
    private static SecretKey sk2;
    private static SecretKey softkey;
    private static KeyStore ks;
    private static final String KS_TYPE = "PKCS11";
    private static Provider provider;

    @BeforeClass
    public void setUp() throws Exception {
        copyNssCertKeyToClassesDir();
        setCommonSystemProps();
        System.setProperty("TOKEN", "nss");
        System.setProperty("CUSTOM_P11_CONFIG", Path.of(BASE)
                .resolve("BasicData").resolve("p11-nss.txt").toString());
    }

    @Test
    public void testBasic() throws Exception {
        main(new SecretKeysBasic());
    }

    public void main(Provider p) throws Exception {
        this.provider = p;

        // create secret key
        byte[] keyVal = new byte[16];
        (new SecureRandom()).nextBytes(keyVal);
        // NSS will throw CKR_HOST_MEMORY if calling C_DecryptInit w/
        // (keyVal[0] == 0)
        if (keyVal[0] == 0) {
            keyVal[0] = 1;
        }
        softkey = new SecretKeySpec(keyVal, "AES");
        dumpKey("softkey", softkey);

        KeyGenerator kg = KeyGenerator.getInstance("DESede", provider);
        sk1 = kg.generateKey();
        dumpKey("skey1", sk1);
        sk2 = kg.generateKey();
        dumpKey("skey2", sk2);

        String token = System.getProperty("TOKEN");

        if (token == null || token.length() == 0) {
            System.out.println("Error: missing TOKEN system property");
            throw new Exception("token arg required");
        }

        if ("nss".equals(token)) {
            tokenPwd = nssPwd;
        } else if ("solaris".equals(token)) {
            tokenPwd = solarisPwd;
        }

        int testnum = 1;
        doTest();
    }

    private static boolean checkSecretKeyEntry(String alias,
            SecretKey expected,
            boolean saveBeforeCheck)
            throws Exception {

        // A bug in NSS 3.12 (Mozilla bug 471665) causes AES key lengths
        // to be read incorrectly.  Checking for improper 16 byte length
        // in key string.
        if (isNSS(provider) && expected.getAlgorithm().equals("AES") &&
                (getNSSVersion() >= 3.12 && getNSSVersion() <= 3.122)) {
            System.out.println("NSS 3.12 bug returns incorrect AES key "+
                    "length breaking key storage. Aborting...");
            return true;
        }

        if (saveBeforeCheck) {
            ks.setKeyEntry(alias, expected, null, null);
        }
        SecretKey result = (SecretKey) (ks.getKey(alias, null));
        String keyEncFormat = result.getFormat();
        if (keyEncFormat == null) {
            // sensitive or un-extractable keys - verify by encrypt/decrypt
            byte[] data = new byte[64];
            Cipher c =
                    Cipher.getInstance(result.getAlgorithm() + "/CBC/NoPadding",
                    provider);
            c.init(Cipher.ENCRYPT_MODE, expected);
            byte[] encOut = c.doFinal(data);
            c.init(Cipher.DECRYPT_MODE, result, c.getParameters());
            byte[] decOut = c.doFinal(encOut);
            if (!Arrays.equals(data, decOut)) {
                return false;
            }
        } else if (keyEncFormat.toUpperCase().equals("RAW")) {
            if (!Arrays.equals(result.getEncoded(), expected.getEncoded())) {
                dumpKey("\texpected:", expected);
                dumpKey("\treturns:", result);
                return false;
            }
        }
        return true;
    }

    private static void dumpKey(String info, SecretKey key) {
        System.out.println(info + "> " + key);
        System.out.println("\tALGO=" + key.getAlgorithm());
        if (key.getFormat() != null) {
            StringBuilder sb = new StringBuilder();
            for (byte b : key.getEncoded()) {
                sb.append(String.format("%02x", b & 0xff));
            }
            System.out.println("\t[" + key.getFormat() + "] VALUE=" + sb);
        } else {
            System.out.println("\tVALUE=n/a");
        }
    }

    private static void doTest() throws Exception {
        // Make sure both NSS libraries are the same version.
        if (isNSS(provider) &&
                (getLibsoftokn3Version() != getLibnss3Version())) {
            System.out.println("libsoftokn3 and libnss3 versions do not match.  Aborting test...");
            return;
        }

        if (ks == null) {
            ks = KeyStore.getInstance(KS_TYPE, provider);
            ks.load(null, tokenPwd);
        }

        System.out.println("Number of entries: " + ks.size());
        if (ks.size() != 0) {
            System.out.println("Deleting entries under aliases: ");
            for (Enumeration<String> aliases = ks.aliases();
                    aliases.hasMoreElements();) {
                String alias = aliases.nextElement();
                System.out.println("\t" + alias);
                ks.deleteEntry(alias);
            }
        }

        String alias = "testSKey";

        boolean testResult = checkSecretKeyEntry(alias, softkey, true);
        if (!testResult) {
            System.out.println("FAILURE: setKey() w/ softSecretKey failed");
        }

        if (!checkSecretKeyEntry(alias, sk1, true)) {
            testResult = false;
            System.out.println("FAILURE: setKey() w/ skey1 failed");
        }
        if (!checkSecretKeyEntry(alias, sk2, true)) {
            testResult = false;
            System.out.println("FAILURE: setKey() w/ skey2 failed");
        }

        ks.store(null);
        System.out.println("Reloading keystore...");

        ks.load(null, "whatever".toCharArray());
        if (ks.size() != 1) {
            System.out.println("FAILURE: reload#1 ks.size() != 1");
        }
        if (!checkSecretKeyEntry(alias, sk2, false)) {
            testResult = false;
            System.out.println("FAILURE: reload#1 ks entry check failed");
        }

        ks.deleteEntry(alias);
        ks.store(null);

        System.out.println("Reloading keystore...");
        ks.load(null, "whatever".toCharArray());
        if (ks.size() != 0) {
            testResult = false;
            System.out.println("FAILURE: reload#2 ks.size() != 0");
        }
        if (!testResult) {
            throw new Exception("One or more test failed!");
        }
    }
}
