/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5053815
 * @summary unspecified exceptions in X509TrustManager.checkClient[Server]Truste
d
 * @author Xuelei Fan
 */

import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.security.*;
import java.util.Enumeration;

public class CheckNullEntity {

    /*
     * =============================================================
     * Set the various variables needed for the tests, then
     * specify what tests to run on each side.
     */

    /*
     * Should we run the client or server in a separate thread?
     * Both sides can throw exceptions, but do you have a preference
     * as to which side should be the main thread.
     */
    static boolean separateServerThread = true;

    /*
     * Where do we find the keystores?
     */
    static String pathToStores = "../../../../javax/net/ssl/etc";
    static String keyStoreFile = "keystore";
    static String trustStoreFile = "truststore";
    static String passwd = "passphrase";

    private void initialize() throws Exception {
        String trustFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + trustStoreFile;
        char[] passphrase = "passphrase".toCharArray();

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(trustFilename), passphrase);

        for (Enumeration e = ks.aliases() ; e.hasMoreElements() ;) {
            String alias = (String)e.nextElement();
            if (ks.isCertificateEntry(alias)) {
                certChain[0] = (X509Certificate)ks.getCertificate(alias);
                break;
            }
        }

        TrustManagerFactory tmf =
            TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        trustManager = (X509TrustManager)(tmf.getTrustManagers())[0];
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */
    public static void main(String[] args) throws Exception {
        /*
         * Start the tests.
         */
        new CheckNullEntity();
    }

    X509Certificate[] certChain = {null, null};
    X509TrustManager trustManager = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    CheckNullEntity() throws Exception {
        String authType = "RSA";
        int failed = 0x3F; // indicate six tests for normal TM
        int extFailed = 0x3F; // indicate six tests for extended TM

        initialize();
        try {
            try {
                trustManager.checkClientTrusted(certChain, (String)null);
            } catch (IllegalArgumentException iae) {
                // get the right exception
                failed >>= 1;
            }

            try {
                trustManager.checkServerTrusted(certChain, (String)null);
            } catch (IllegalArgumentException iae) {
                // get the right exception
                failed >>= 1;
            }

            try {
                trustManager.checkClientTrusted(certChain, "");
            } catch (IllegalArgumentException iae) {
                // get the right exception
                failed >>= 1;
            }

            try {
                trustManager.checkServerTrusted(certChain, "");
            } catch (IllegalArgumentException iae) {
                // get the right exception
                failed >>= 1;
            }

            try {
                trustManager.checkClientTrusted(null, authType);
            } catch (IllegalArgumentException iae) {
                // get the right exception
                failed >>= 1;
            }

            try {
                trustManager.checkServerTrusted(null, authType);
            } catch (IllegalArgumentException iae) {
                // get the right exception
                failed >>= 1;
            }

            if (trustManager instanceof X509ExtendedTrustManager) {
                try {
                    ((X509ExtendedTrustManager)trustManager).checkClientTrusted(
                        certChain, (String)null, (Socket)null);
                } catch (IllegalArgumentException iae) {
                    // get the right exception
                    extFailed >>= 1;
                }

                try {
                    ((X509ExtendedTrustManager)trustManager).checkServerTrusted(
                        certChain, (String)null, (Socket)null);
                } catch (IllegalArgumentException iae) {
                    // get the right exception
                    extFailed >>= 1;
                }

                try {
                    ((X509ExtendedTrustManager)trustManager).checkClientTrusted(
                        certChain, "", (Socket)null);
                } catch (IllegalArgumentException iae) {
                    // get the right exception
                    extFailed >>= 1;
                }

                try {
                    ((X509ExtendedTrustManager)trustManager).checkServerTrusted(
                        certChain, "", (Socket)null);
                } catch (IllegalArgumentException iae) {
                    // get the right exception
                    extFailed >>= 1;
                }

                try {
                    ((X509ExtendedTrustManager)trustManager).checkClientTrusted(
                        null, authType, (Socket)null);
                } catch (IllegalArgumentException iae) {
                    // get the right exception
                    extFailed >>= 1;
                }

                try {
                    ((X509ExtendedTrustManager)trustManager).checkServerTrusted(
                        null, authType, (Socket)null);
                } catch (IllegalArgumentException iae) {
                    // get the right exception
                    extFailed >>= 1;
                }
            } else {
                extFailed = 0;
            }
        } catch (NullPointerException npe) {
            // IllegalArgumentException should be thrown
            failed = 1;
        } catch (Exception e) {
            // ignore
            System.out.println("Got another exception e" + e);
        }

        if (failed != 0 || extFailed != 0) {
            throw new Exception("Should throw IllegalArgumentException");
        }
    }
}
