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
 * @bug 8005408
 * @summary KeyStore API enhancements
 */

import java.io.*;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.spec.InvalidKeySpecException;

// Store a password in a keystore and retrieve it again.

public class StorePasswordTest {
    private final static String DIR = System.getProperty("test.src", ".");
    private static final char[] PASSWORD = "passphrase".toCharArray();
    private static final String KEYSTORE = "pwdstore.p12";
    private static final String ALIAS = "my password";
    private static final String USER_PASSWORD = "hello1";

    public static void main(String[] args) throws Exception {

        new File(KEYSTORE).delete();

        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(null, null);

        // Set entry
        Set<KeyStore.Entry.Attribute> attrs = new HashSet<>();
        attrs.add(new PKCS12Attribute("1.3.5.7.9", "printable1"));
        attrs.add(new PKCS12Attribute("2.4.6.8.10", "1F:2F:3F:4F:5F"));
        int originalAttrCount = attrs.size() + 2;
        keystore.setEntry(ALIAS,
            new KeyStore.SecretKeyEntry(convertPassword(USER_PASSWORD), attrs),
                new KeyStore.PasswordProtection(PASSWORD));

        try (FileOutputStream outStream = new FileOutputStream(KEYSTORE)) {
            System.out.println("Storing keystore to: " + KEYSTORE);
            keystore.store(outStream, PASSWORD);
        }

        try (FileInputStream inStream = new FileInputStream(KEYSTORE)) {
            System.out.println("Loading keystore from: " + KEYSTORE);
            keystore.load(inStream, PASSWORD);
            System.out.println("Loaded keystore with " + keystore.size() +
                " entries");
        }

        KeyStore.Entry entry = keystore.getEntry(ALIAS,
            new KeyStore.PasswordProtection(PASSWORD));
        int attrCount = entry.getAttributes().size();
        System.out.println("Retrieved entry with " + attrCount + " attrs: " +
            entry);
        if (attrCount != originalAttrCount) {
            throw new Exception("Failed to recover all the entry attributes");
        }

        SecretKey key = (SecretKey) keystore.getKey(ALIAS, PASSWORD);
        SecretKeyFactory factory =
            SecretKeyFactory.getInstance(key.getAlgorithm());
        PBEKeySpec keySpec =
            (PBEKeySpec) factory.getKeySpec(key, PBEKeySpec.class);
        char[] pwd = keySpec.getPassword();
        System.out.println("Recovered credential: " + new String(pwd));

        if (!Arrays.equals(USER_PASSWORD.toCharArray(), pwd)) {
            throw new Exception("Failed to recover the stored password");
        }
    }

    private static SecretKey convertPassword(String password)
        throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
        return factory.generateSecret(new PBEKeySpec(password.toCharArray()));
    }
}
