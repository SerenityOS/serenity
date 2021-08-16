/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6324295 6931562 8154113
 * @modules jdk.crypto.mscapi
 * @run main/othervm/java.security.policy==access.policy AccessKeyStore pass
 * @run main/othervm/java.security.policy==noaccess.policy AccessKeyStore fail
 * @summary Confirm that right permissions are granted to access keystores.
 */

import java.security.Provider;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.interfaces.RSAKey;
import java.util.Enumeration;

public class AccessKeyStore {

    public static void main(String[] args) throws Exception {

        // Check for security manager and required arg(s)
        if (System.getSecurityManager() == null) {
            throw new Exception("Missing security manager");
        }
        if (args.length <= 0) {
            throw new Exception("Missing expected test status");
        }
        boolean shouldPass = args[0].equalsIgnoreCase("pass");

        Provider p = Security.getProvider("SunMSCAPI");
        System.out.println("SunMSCAPI provider classname is " +
            p.getClass().getName());

        KeyStore keyStore = KeyStore.getInstance("Windows-MY", p);

        /*
         * If a SecurityManager exists then this will trigger a
         * SecurityException if the following permission has not
         * been granted:
         *
         *     SecurityPermission("authProvider.SunMSCAPI")
         */
        try {
            keyStore.load(null, null);
            if (!shouldPass) {
                throw new Exception(
                    "Expected KeyStore.load to throw a SecurityException");
            }
        } catch (SecurityException se) {
            if (!shouldPass) {
                System.out.println("Expected exception thrown: " + se);
                return;
            } else {
                throw se;
            }
        }

        int i = 0;
        for (Enumeration<String> e = keyStore.aliases(); e.hasMoreElements(); ) {
            String alias = e.nextElement();
            displayEntry(keyStore, alias, i++);
        }
    }

    private static void displayEntry(KeyStore keyStore, String alias,
        int index) throws KeyStoreException, NoSuchAlgorithmException  {

        if (keyStore.isKeyEntry(alias)) {
            System.out.println("[" + index + "]\n    " + alias +
                " [key-entry]\n");

            try {

                Key key = keyStore.getKey(alias, null);

                if (key instanceof RSAKey) {
                    System.out.println("    Key type: " + key.getAlgorithm() +
                        " (" + ((RSAKey)key).getModulus().bitLength() +
                        " bit)\n");
                } else {
                    System.out.println("    Key type: " + key.getAlgorithm() +
                        "\n");
                }

            } catch (UnrecoverableKeyException e) {
                System.out.println("    Key type: Unknown\n");
            }

            Certificate[] chain = keyStore.getCertificateChain(alias);
            if (chain != null) {
                System.out.println("    Certificate chain: ");
                for (int i = 0; i < chain.length; i ++) {
                    System.out.println("        ["+ (i + 1) + "]");
                    displayCert(chain[i], "            ");
                }
            }

        } else {
            System.out.println("[" + index + "]\n    " + alias +
                " [trusted-cert-entry]\n");
            Certificate[] chain = keyStore.getCertificateChain(alias);
            if (chain != null) {
                System.out.println("    Certificate chain: ");
                for (int i = 0; i < chain.length; i ++) {
                    System.out.println("        ["+ (i + 1) + "]");
                    displayCert(chain[i], "            ");
                }
            }
        }
        System.out.println("-------------------------------------------------");
    }

    private static void displayCert(Certificate cert, String tab) {
        if (cert instanceof X509Certificate) {
            X509Certificate x = (X509Certificate) cert;
            System.out.println(
                tab + "Owner: " + x.getSubjectDN().toString() + "\n" +
                tab + "Issuer: " + x.getIssuerDN().toString() + "\n" +
                tab + "Serial number: " + x.getSerialNumber().toString(16) +
                "\n"+
                tab + "Valid from: " + x.getNotBefore().toString() + "\n" +
                tab + "     until: " + x.getNotAfter().toString());
        } else {
            System.out.println(tab + "[unknown certificate format]");
        }
        System.out.println();
    }
}
