/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * see ./DKSTest.sh
 */

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.KeyStore;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;

// Load and store entries in domain keystores

public class DKSTest {

    private static final String TEST_SRC = System.getProperty("test.src");
    private static final String USER_DIR = System.getProperty("user.dir");
    private static final String CERT = TEST_SRC + "/../../pkcs12/trusted.pem";
    private static final String CONFIG = "file://" + TEST_SRC + "/domains.cfg";
    private static final Map<String, KeyStore.ProtectionParameter> PASSWORDS =
        new HashMap<String, KeyStore.ProtectionParameter>() {{
            put("keystore",
                new KeyStore.PasswordProtection("test123".toCharArray()));
            put("policy_keystore",
                new KeyStore.PasswordProtection(
                    "Alias.password".toCharArray()));
            put("pw_keystore",
                new KeyStore.PasswordProtection("test12".toCharArray()));
            put("eckeystore1",
                new KeyStore.PasswordProtection("password".toCharArray()));
            put("truststore",
                new KeyStore.PasswordProtection("changeit".toCharArray()));
            put("empty",
                new KeyStore.PasswordProtection("passphrase".toCharArray()));
        }};

    private static final Map<String, KeyStore.ProtectionParameter>
        WRONG_PASSWORDS = new HashMap<String, KeyStore.ProtectionParameter>() {{
            put("policy_keystore",
                new KeyStore.PasswordProtection(
                    "wrong".toCharArray()));
            put("pw_keystore",
                new KeyStore.PasswordProtection("wrong".toCharArray()));
            put("eckeystore1",
                new KeyStore.PasswordProtection("wrong".toCharArray()));
        }};

    public static void main(String[] args) throws Exception {
        /*
         * domain keystore: keystores with wrong passwords
         */
        try {
            URI config = new URI(CONFIG + "#keystores");
            KeyStore ks = KeyStore.getInstance("DKS");
            ks.load(new DomainLoadStoreParameter(config, WRONG_PASSWORDS));
            throw new RuntimeException("Expected exception not thrown");
        } catch (IOException e) {
            System.out.println("Expected exception: " + e);
            if (!causedBy(e, UnrecoverableKeyException.class)) {
                e.printStackTrace(System.out);
                throw new RuntimeException("Unexpected cause");
            }
            System.out.println("Expected cause: " + e);
        }

        /*
         * domain keystore: system
         */
        URI config = new URI(CONFIG + "#system");
        int cacertsCount;
        int expected;
        KeyStore keystore = KeyStore.getInstance("DKS");
        // load entries
        keystore.load(new DomainLoadStoreParameter(config, PASSWORDS));
        cacertsCount = expected = keystore.size();
        System.out.println("\nLoading domain keystore: " + config + "\t[" +
            expected + " entries]");
        checkEntries(keystore, expected);

        /*
         * domain keystore: system_plus
         */
        config = new URI(CONFIG + "#system_plus");
        expected = cacertsCount + 1;
        keystore = KeyStore.getInstance("DKS");
        // load entries
        keystore.load(new DomainLoadStoreParameter(config, PASSWORDS));
        System.out.println("\nLoading domain keystore: " + config + "\t[" +
            expected + " entries]");
        checkEntries(keystore, expected);

        /*
         * domain keystore: system_env
         */
        config = new URI(CONFIG + "#system_env");
        expected = 1 + cacertsCount;
        keystore = KeyStore.getInstance("DKS");
        // load entries
        keystore.load(
            new DomainLoadStoreParameter(config,
                Collections.<String, KeyStore.ProtectionParameter>emptyMap()));
        System.out.println("\nLoading domain keystore: " + config + "\t[" +
            expected + " entries]");
        checkEntries(keystore, expected);

        /*
         * domain keystore: empty
         */
        KeyStore empty = KeyStore.getInstance("JKS");
        empty.load(null, null);

        try (OutputStream outStream =
            new FileOutputStream(new File(USER_DIR, "empty.jks"))) {
            empty.store(outStream, "passphrase".toCharArray());
        }
        config = new URI(CONFIG + "#empty");
        expected = 0;
        keystore = KeyStore.getInstance("DKS");
        // load entries
        keystore.load(new DomainLoadStoreParameter(config, PASSWORDS));
        System.out.println("\nLoading domain keystore: " + config + "\t[" +
            expected + " entries]");
        checkEntries(keystore, expected);

        /*
         * domain keystore: keystores
         */
        config = new URI(CONFIG + "#keystores");
        expected = 2 + 1 + 1;
        keystore = KeyStore.getInstance("DKS");
        // load entries
        keystore.load(new DomainLoadStoreParameter(config, PASSWORDS));
        System.out.println("\nLoading domain keystore: " + config + "\t[" +
            expected + " entries]");
        checkEntries(keystore, expected);
        // set a new trusted certificate entry
        Certificate cert = loadCertificate(CERT);
        String alias = "pw_keystore tmp-cert";
        System.out.println("Setting new trusted certificate entry: " + alias);
        keystore.setEntry(alias,
            new KeyStore.TrustedCertificateEntry(cert), null);
        expected++;
        // store entries
        config = new URI(CONFIG + "#keystores_tmp");
        System.out.println("Storing domain keystore: " + config + "\t[" +
            expected + " entries]");
        keystore.store(new DomainLoadStoreParameter(config, PASSWORDS));
        keystore = KeyStore.getInstance("DKS");
        // reload entries
        keystore.load(new DomainLoadStoreParameter(config, PASSWORDS));
        System.out.println("Reloading domain keystore: " + config + "\t[" +
            expected + " entries]");
        checkEntries(keystore, expected);
        // get the new trusted certificate entry
        System.out.println("Getting new trusted certificate entry: " + alias);
        if (!keystore.isCertificateEntry(alias)) {
            throw new Exception("Error: cannot retrieve certificate entry: " +
                alias);
        }
        keystore.setEntry(alias,
            new KeyStore.TrustedCertificateEntry(cert), null);
    }

    private static void checkEntries(KeyStore keystore, int expected)
        throws Exception {
        int i = 0;
        for (String alias : Collections.list(keystore.aliases())) {
            System.out.print(".");
            i++;
        }
        System.out.println();
        if (expected != i) {
            throw new Exception("Error: unexpected entry count in keystore: " +
                "loaded=" + i + ", expected=" + expected);
        }
    }

    private static Certificate loadCertificate(String certFile)
        throws Exception {
        X509Certificate cert = null;
        try (FileInputStream certStream = new FileInputStream(certFile)) {
            CertificateFactory factory =
                CertificateFactory.getInstance("X.509");
            return factory.generateCertificate(certStream);
        }
    }

    // checks if an exception was caused by specified exception class
    private static boolean causedBy(Exception e, Class klass) {
        Throwable cause = e;
        while ((cause = cause.getCause()) != null) {
            if (cause.getClass().equals(klass)) {
                return true;
            }
        }
        return false;
    }
}
