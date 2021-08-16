/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8044445 8194307 8207318
 * @summary test new methods from JEP-229: Create PKCS12 Keystores by Default
 */

import java.io.*;
import java.security.*;
import java.security.KeyStore.*;
import java.security.cert.*;
import javax.crypto.*;
import javax.security.auth.callback.*;

public class ProbeKeystores {
    private static final char[] PASSWORD = "changeit".toCharArray();
    private static final char[] BAD_PASSWORD = "badpasword".toCharArray();
    private static final LoadStoreParameter LOAD_STORE_PARAM =
        new MyLoadStoreParameter(new PasswordProtection(PASSWORD));
    private static final LoadStoreParameter BAD_LOAD_STORE_PARAM =
        new MyLoadStoreParameter(new PasswordProtection(BAD_PASSWORD));
    private static final String DIR = System.getProperty("test.src", ".");
    private static final String CERT_FILE = "trusted.pem";

    private static class MyLoadStoreParameter implements LoadStoreParameter {

        private ProtectionParameter protection;

        MyLoadStoreParameter(ProtectionParameter protection) {
            this.protection = protection;
        }

        public ProtectionParameter getProtectionParameter() {
            return protection;
        }
    }

    public static final void main(String[] args) throws Exception {

        // Testing empty keystores

        init("empty.jks", "JKS");
        init("empty.jceks", "JCEKS");
        init("empty.p12", "PKCS12");

        load("empty.jks", "JKS");
        load("empty.jceks", "JCEKS");
        load("empty.p12", "PKCS12");
        load("empty.jks", "PKCS12"); // test compatibility mode
        load("empty.p12", "JKS"); // test compatibility mode
        load("empty.jks", "PKCS12", true); // test without compatibility mode
        load("empty.jks", "JKS", false); // test without compatibility mode
        load("empty.p12", "JKS", true); // test without compatibility mode
        load("empty.p12", "PKCS12", false); // test without compatibility mode

        probe("empty.jks", "JKS");
        probe("empty.jceks", "JCEKS");
        probe("empty.p12", "PKCS12");

        build("empty.jks", "JKS", true);
        build("empty.jks", "JKS", false);
        build("empty.jceks", "JCEKS", true);
        build("empty.jceks", "JCEKS", false);
        build("empty.p12", "PKCS12", true);
        build("empty.p12", "PKCS12", false);

        // Testing keystores containing an X.509 certificate

        X509Certificate cert = loadCertificate(CERT_FILE);
        init("onecert.jks", "JKS", cert);
        init("onecert.jceks", "JCEKS", cert);
        init("onecert.p12", "PKCS12", cert);

        load("onecert.jks", "JKS");
        load("onecert.jceks", "JCEKS");
        load("onecert.p12", "PKCS12");
        load("onecert.jks", "PKCS12"); // test compatibility mode
        load("onecert.p12", "JKS"); // test compatibility mode
        load("onecert.jks", "PKCS12", true); // test without compatibility mode
        load("onecert.jks", "JKS", false); // test without compatibility mode
        load("onecert.p12", "JKS", true); // test without compatibility mode
        load("onecert.p12", "PKCS12", false); // test without compatibility mode

        probe("onecert.jks", "JKS");
        probe("onecert.jceks", "JCEKS");
        probe("onecert.p12", "PKCS12");

        build("onecert.jks", "JKS", true);
        build("onecert.jks", "JKS", false);
        build("onecert.jceks", "JCEKS", true);
        build("onecert.jceks", "JCEKS", false);
        build("onecert.p12", "PKCS12", true);
        build("onecert.p12", "PKCS12", false);

        // Testing keystores containing a secret key

        SecretKey key = generateSecretKey("AES", 128);
        init("onekey.jceks", "JCEKS", key);
        init("onekey.p12", "PKCS12", key);

        load("onekey.jceks", "JCEKS");
        load("onekey.p12", "PKCS12");
        load("onekey.p12", "JKS"); // test compatibility mode
        load("onekey.p12", "JKS", true); // test without compatibility mode
        load("onekey.p12", "PKCS12", false); // test without compatibility mode

        probe("onekey.jceks", "JCEKS");
        probe("onekey.p12", "PKCS12");

        build("onekey.jceks", "JCEKS", true);
        build("onekey.jceks", "JCEKS", false);
        build("onekey.p12", "PKCS12", true);
        build("onekey.p12", "PKCS12", false);

        System.out.println("OK.");
    }

    // Instantiate an empty keystore using the supplied keystore type
    private static void init(String file, String type) throws Exception {
        KeyStore ks = KeyStore.getInstance(type);
        ks.load(null, null);
        try (OutputStream stream = new FileOutputStream(file)) {
            ks.store(stream, PASSWORD);
        }
        System.out.println("Created a " + type + " keystore named '" + file + "'");
    }

    // Instantiate a keystore using the supplied keystore type & create an entry
    private static void init(String file, String type, X509Certificate cert)
        throws Exception {
        KeyStore ks = KeyStore.getInstance(type);
        ks.load(null, null);
        ks.setEntry("mycert", new KeyStore.TrustedCertificateEntry(cert), null);
        try (OutputStream stream = new FileOutputStream(file)) {
            ks.store(stream, PASSWORD);
        }
        System.out.println("Created a " + type + " keystore named '" + file + "'");
    }

    // Instantiate a keystore using the supplied keystore type & create an entry
    private static void init(String file, String type, SecretKey key)
        throws Exception {
        KeyStore ks = KeyStore.getInstance(type);
        ks.load(null, null);
        ks.setEntry("mykey", new KeyStore.SecretKeyEntry(key),
            new PasswordProtection(PASSWORD));
        try (OutputStream stream = new FileOutputStream(file)) {
            ks.store(stream, PASSWORD);
        }
        System.out.println("Created a " + type + " keystore named '" + file + "'");
    }

    // Instantiate a keystore by probing the supplied file for the keystore type
    private static void probe(String file, String type) throws Exception {
        // First try with the correct password
        KeyStore ks = KeyStore.getInstance(new File(file), PASSWORD);
        if (!type.equalsIgnoreCase(ks.getType())) {
            throw new Exception("ERROR: expected a " + type + " keystore, " +
                "got a " + ks.getType() + " keystore instead");
        } else {
            System.out.println("Probed a " + type + " keystore named '" + file
                    + "' with " + ks.size() + " entries");
        }

        // Next try with an incorrect password
        try {
            ks = KeyStore.getInstance(new File(file), BAD_PASSWORD);
            throw new Exception("ERROR: expected an exception but got success");
        } catch (IOException e) {
            System.out.println("Failed to load a " + type + " keystore named '" + file + "' (as expected)");
        }

        // Now try with the correct password within a LoadStoreParameter
        ks = KeyStore.getInstance(new File(file), LOAD_STORE_PARAM);
        if (!type.equalsIgnoreCase(ks.getType())) {
            throw new Exception("ERROR: expected a " + type + " keystore, " +
                "got a " + ks.getType() + " keystore instead");
        } else {
            System.out.println("Probed a " + type + " keystore named '" + file
                    + "' with " + ks.size() + " entries");
        }

        // Next try with an incorrect password within a LoadStoreParameter
        try {
            ks = KeyStore.getInstance(new File(file), BAD_LOAD_STORE_PARAM);
            throw new Exception("ERROR: expected an exception but got success");
        } catch (IOException e) {
            System.out.println("Failed to load a " + type + " keystore named '" + file + "' (as expected)");
        }
    }

    // Instantiate a keystore by probing the supplied file for the keystore type
    private static void build(String file, String type, boolean usePassword)
        throws Exception {

        Builder builder;
        if (usePassword) {
            builder = Builder.newInstance(new File(file),
                new PasswordProtection(PASSWORD));
        } else {
            builder = Builder.newInstance(new File(file),
                new CallbackHandlerProtection(new DummyHandler()));
        }
        KeyStore ks = builder.getKeyStore();
        if (!type.equalsIgnoreCase(ks.getType())) {
            throw new Exception("ERROR: expected a " + type + " keystore, " +
                "got a " + ks.getType() + " keystore instead");
        } else {
            System.out.println("Built a " + type + " keystore named '" + file + "'");
        }
    }

    // Load the keystore entries
    private static void load(String file, String type) throws Exception {
        KeyStore ks = KeyStore.getInstance(type);
        try (InputStream stream = new FileInputStream(file)) {
            ks.load(stream, PASSWORD);
        }
        if (!type.equalsIgnoreCase(ks.getType())) {
            throw new Exception("ERROR: expected a " + type + " keystore, " +
                "got a " + ks.getType() + " keystore instead");
        } else {
            System.out.println("Loaded a " + type + " keystore named '" + file + "'");
        }
    }

    // Load the keystore entries (with compatibility mode disabled)
    private static void load(String file, String type, boolean expectFailure)
        throws Exception {
        Security.setProperty("keystore.type.compat", "false");
        try {
            load(file, type);
            if (expectFailure) {
                throw new Exception("ERROR: expected load to fail but it didn't");
            }
        } catch (IOException e) {
            if (expectFailure) {
                System.out.println("Failed to load a " + type + " keystore named '" + file + "' (as expected)");
            } else {
                throw e;
            }
        } finally {
            Security.setProperty("keystore.type.compat", "true");
        }
    }

    // Read an X.509 certificate from the supplied file
    private static X509Certificate loadCertificate(String certFile)
        throws Exception {
        X509Certificate cert = null;
        try (FileInputStream certStream =
            new FileInputStream(DIR + "/" + certFile)) {
            CertificateFactory factory =
                CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(certStream);
        }
    }

    // Generate a secret key using the supplied algorithm name and key size
    private static SecretKey generateSecretKey(String algorithm, int size)
        throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance(algorithm);
        generator.init(size);
        return generator.generateKey();
    }

    private static class DummyHandler implements CallbackHandler {
        public void handle(Callback[] callbacks)
            throws IOException, UnsupportedCallbackException {
            System.out.println("** Callbackhandler invoked");
            for (int i = 0; i < callbacks.length; i++) {
                Callback cb = callbacks[i];
                if (cb instanceof PasswordCallback) {
                    PasswordCallback pcb = (PasswordCallback)cb;
                    pcb.setPassword(PASSWORD);
                    break;
                }
            }
        }
    }
}
