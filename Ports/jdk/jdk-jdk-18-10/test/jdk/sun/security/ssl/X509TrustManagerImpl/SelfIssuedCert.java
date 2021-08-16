/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

//
// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.
//

/*
 * @test
 * @bug 6822460
 * @summary support self-issued certificate
 * @run main/othervm SelfIssuedCert PKIX
 * @run main/othervm SelfIssuedCert SunX509
 * @author Xuelei Fan
 */

import java.io.*;
import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.KeyFactory;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.*;
import java.security.interfaces.*;

import java.util.Base64;

public class SelfIssuedCert {

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
    // Certificate information:
    // Issuer: C=US, O=Example, CN=localhost
    // Validity
    //     Not Before: Dec 19 06:11:58 2019 GMT
    //     Not After : Dec 16 06:11:58 2029 GMT
    // Subject: C=US, O=Example, CN=localhost
    // X509v3 Subject Key Identifier:
    //     80:67:BA:EE:10:6A:E3:8E:3E:8E:F7:2D:90:B6:FD:F9:54:87:47:B1
    // X509v3 Authority Key Identifier:
    //     keyid:80:67:BA:EE:10:6A:E3:8E:3E:8E:F7:2D:90:B6:FD:F9:54:87:47:B1
    static String trusedCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDRzCCAi+gAwIBAgIUFjy13iZYWMGQcGF4svfix/9q4dcwDQYJKoZIhvcNAQEL\n" +
        "BQAwMzELMAkGA1UEBhMCVVMxEDAOBgNVBAoMB0V4YW1wbGUxEjAQBgNVBAMMCWxv\n" +
        "Y2FsaG9zdDAeFw0xOTEyMTkwNjExNThaFw0yOTEyMTYwNjExNThaMDMxCzAJBgNV\n" +
        "BAYTAlVTMRAwDgYDVQQKDAdFeGFtcGxlMRIwEAYDVQQDDAlsb2NhbGhvc3QwggEi\n" +
        "MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCy57BG8Dt+a4ZwWGM07f0z/mzK\n" +
        "T/myXM4W//3pkZxO0+4oyYM7G8ks9O64NPpA0CpTPCpfY6dI1Y/kwBUdSoqx2D8t\n" +
        "OEfHOat2/AQvvWmEChFH4ZmmQFkLXBy0ueDq0TJbEd94+WhL3q9bA4uqvBsuuaTt\n" +
        "bX/GyOC52bpjg0TWY4BRdRVhveISZvqOCoqqJ1aPOnfxqySaZIC34q9gdUCUNxZD\n" +
        "qjhuQF3Q0xYsNGZSUmnKj3/0GS600BwQPqSHy287Vda88NvqJGFS4DKrw3HV3Wsk\n" +
        "IHGN+tzB5THBy70XrE+XIdXJ/I86q+FvNcTnJygn2nVNG4+vUhW8S3BzTiKPAgMB\n" +
        "AAGjUzBRMB0GA1UdDgQWBBSAZ7ruEGrjjj6O9y2Qtv35VIdHsTAfBgNVHSMEGDAW\n" +
        "gBSAZ7ruEGrjjj6O9y2Qtv35VIdHsTAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3\n" +
        "DQEBCwUAA4IBAQBX7icKmR/iUPJhfnvNHiqsyTIcowY3JSAJAyJFrViKx2tdo+qq\n" +
        "yA+EUsZlZsCwhiiG4/SjFxgaAp0Z3BBmsO/njWUEx3/fSufTHcs0+fPNkFLru5Lr\n" +
        "das4wW9Cv/wO4rz2L6qK/x7+r/wkPccaqxTpdZvXqDid2va5Lv3F7jOW5ns13piZ\n" +
        "z571RCpmhGSytYKFrAOGoI4ZBWXrkCiYQZ8KvhdBQP/MNJM+e6ajtF27rK08XTao\n" +
        "mW3FXfK6SjKQDGVwtNJ7M1qGutIpe0pNBGwvDpQuY2mk0Le46OXdaQ7AAzE+OnRJ\n" +
        "1uRDV+p95MzhtolPgB3I8Rzyd23nfrx6uxMA\n" +
        "-----END CERTIFICATE-----";

    // Certificate information:
    // Issuer: C=US, O=Example, CN=localhost
    // Validity
    //     Not Before: Dec 19 06:12:04 2019 GMT
    //     Not After : Dec 16 06:12:04 2029 GMT
    // Subject: C=US, O=Example, CN=localhost
    // X509v3 Subject Key Identifier:
    //     73:79:B7:73:F5:41:BB:3A:90:07:87:F2:CA:A5:B3:C3:45:E0:18:E0
    // X509v3 Authority Key Identifier:
    //     keyid:80:67:BA:EE:10:6A:E3:8E:3E:8E:F7:2D:90:B6:FD:F9:54:87:47:B1
    static String targetCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDNjCCAh6gAwIBAgIURM+bID1TFw41Z/Vz9tPp7HzpH7QwDQYJKoZIhvcNAQEL\n" +
        "BQAwMzELMAkGA1UEBhMCVVMxEDAOBgNVBAoMB0V4YW1wbGUxEjAQBgNVBAMMCWxv\n" +
        "Y2FsaG9zdDAeFw0xOTEyMTkwNjEyMDRaFw0yOTEyMTYwNjEyMDRaMDMxCzAJBgNV\n" +
        "BAYTAlVTMRAwDgYDVQQKDAdFeGFtcGxlMRIwEAYDVQQDDAlsb2NhbGhvc3QwggEi\n" +
        "MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCtxQXQdTlZNoASIE0TM+tgtUY3\n" +
        "jnu0EymO+RGljOIFYhz2MxN0OQ5ABofxdIhbSqtoCO9HbsVWIPKOvbACoAJ4HjTV\n" +
        "antLPlvCqbUoR96q6JWbnbQ6uZOsgiQTveQMhLJ+k9BehzcwKvwCFGNY3qW0xwUv\n" +
        "mXKWRveRAbTOjZ3i1YzcmkLOwYaeyt2Al3jPCbZySUlB94NRRAQZ4RzqfuetAvEd\n" +
        "LFW1fXNwL5bHE7JbJkWInciLOqHf5GuyXDjKE8Oz2/Ywv/5C2K2LtWa1g5jIEQtB\n" +
        "cjRa9Cjwcrs8peisC5OmL5cbJweNKr6H0mrVR8KFdFHUmM5X4uSiOMVFr/rTAgMB\n" +
        "AAGjQjBAMB0GA1UdDgQWBBRzebdz9UG7OpAHh/LKpbPDReAY4DAfBgNVHSMEGDAW\n" +
        "gBSAZ7ruEGrjjj6O9y2Qtv35VIdHsTANBgkqhkiG9w0BAQsFAAOCAQEAZ/Ijlics\n" +
        "YGCw9k4he3ZkNfqCPFTJKgkbTuM1Cy+aCXzhhdGKCZ2R0Xyi3ma3snwPtqHy5Aru\n" +
        "WwoGssxL6S8+Pb/BPZ9OelU7lEmS69AeBKOHHIEs+wEi2oco8J+WU1O4zekP8Clv\n" +
        "hHuwPhoL6g0aAUXAISaqYpHYC15oXGOJcC539kgv4VrL9UZJekxtDERUXKyzW+UC\n" +
        "ZBPalts1zM5wD43+9PuoeLiPdvMg1kH4obJYnj23zej41iwqPOWhgm0NuGoJVjSg\n" +
        "4YqtS1ePD/I2oRV0bu4P7Q72cMYdcFHfPDoe3vCcEMxUTgGBaoPHw9GwEeRoWn/L\n" +
        "whBwzXBsD0aZqQ==\n" +
        "-----END CERTIFICATE-----";

    // Private key in the format of PKCS#8
    static String targetPrivateKey =
        "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCtxQXQdTlZNoAS\n" +
        "IE0TM+tgtUY3jnu0EymO+RGljOIFYhz2MxN0OQ5ABofxdIhbSqtoCO9HbsVWIPKO\n" +
        "vbACoAJ4HjTVantLPlvCqbUoR96q6JWbnbQ6uZOsgiQTveQMhLJ+k9BehzcwKvwC\n" +
        "FGNY3qW0xwUvmXKWRveRAbTOjZ3i1YzcmkLOwYaeyt2Al3jPCbZySUlB94NRRAQZ\n" +
        "4RzqfuetAvEdLFW1fXNwL5bHE7JbJkWInciLOqHf5GuyXDjKE8Oz2/Ywv/5C2K2L\n" +
        "tWa1g5jIEQtBcjRa9Cjwcrs8peisC5OmL5cbJweNKr6H0mrVR8KFdFHUmM5X4uSi\n" +
        "OMVFr/rTAgMBAAECggEAIFDvz+C9FZZJIxXWv6d8MrQDpvlckBSwOeKgIYWd0xp4\n" +
        "AGFnUMn7mHSee40Mfs3YKrTeqw4yrN3bvigQv6w6SVR0xuvSmh+yuPUOt7sF8grn\n" +
        "J9WgWvuANyjMxM8fxiQ3fcrHiYzj+pVD4K8h+rkNYB1THZMP+FqiV9lVYsR7hF+b\n" +
        "1D967LB4oLmAaMExaSo23NZLGVTxZSxxGw6Qidz7CyKvIdVXnNIEzMnuXX60xiJm\n" +
        "PnLyZUKDmlw5kI4KaDG+6OIOpDu2FGCFVLZmycs4Ri6h6xJp3jhKAVjCcZJUty80\n" +
        "+rBfAx4BHfDrcgyEiTN7NA8gnnCzUc6uX6I/tm62gQKBgQDniWuFjSzhaAhj04+N\n" +
        "vG8sQjfVmTbON6SfFfujR/Z57qamJ8zcS/REHfc5swdn9uUTJ2xoRRNCwKZyuMXo\n" +
        "4B2/O9+sKfEPYGyjAyGo6E4rGLRNcw6Tb8hx/EFvfTOunwapynOJDDs2Z6FzWNIx\n" +
        "x4+FHs9hStwL/OTdXF/OY2vGsQKBgQDAIR93LrCC6OpGi89/UDIwpT9pFLa8cvpr\n" +
        "1MUNlHhcxQusPUgWT4pTucF/SQpPf77g3YNb5pt3DG0GELM8YAB0Uv9oZIWfJoFY\n" +
        "ebYy6tMVxhHhT0OuryMj48BMHnQG78hq8+c0NnjK7jXV6t0iKjN8ANnFqAovm+U9\n" +
        "VMobar5CwwKBgFCKN9GsCxmZg5meBQiLrKxbmGp/slXHe0cvcWoZ5T4C6wtPOu7C\n" +
        "qQRs3AvBH+llM8gW5ZnbtVh6BSxQ498e3pof7K1JpaXwp7mIpFPKAy7wl/9872wP\n" +
        "7UzhL63lgm3SuZGkb84TaCGDqOCj2/Ie9eibkA3K6YJuBPqPYHA9m0bxAoGARdcE\n" +
        "iB9pvHyMRM6nw8DULciz7y+/aWtmSnJSmyggRKDAKIEyRiHtx5eblfhoDhQCv9zl\n" +
        "1i9SzgivTOgfL1A6eg59l2YLCJpHpHDB4WppBt40O7HDialSXcZ5bXIYfTkGopI8\n" +
        "tkciy6mh2jwA3F14z5fDkc0OvtWtlAjRWvwHY18CgYAPONVJtVFiMogBU5Iyv1LB\n" +
        "oygn6AFvTI8Pjy2g5GsJBbRnKFjAJrP7HpgUxLdW+Mlnv3Xgtr/L6ep+VKoXTEwv\n" +
        "Y83gliDwG2YRjaUbkMfQqcm20/Pi4XPwhy5pwTVsXVBfzKzqJjKAFk97BD9xCUIH\n" +
        "FOGe+jaEsWvaEQrH5y17FQ==";

    static char passphrase[] = "passphrase".toCharArray();

    /*
     * Is the server ready to serve?
     */
    volatile static boolean serverReady = false;

    /*
     * Turn on SSL debugging?
     */
    static boolean debug = false;

    /*
     * Define the server side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doServerSide() throws Exception {
        SSLContext context = getSSLContext(null, targetCertStr,
                                            targetPrivateKey);
        SSLServerSocketFactory sslssf = context.getServerSocketFactory();

        SSLServerSocket sslServerSocket =
            (SSLServerSocket)sslssf.createServerSocket(serverPort);
        serverPort = sslServerSocket.getLocalPort();

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady = true;

        SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
        sslSocket.setNeedClientAuth(false);

        InputStream sslIS = sslSocket.getInputStream();
        OutputStream sslOS = sslSocket.getOutputStream();

        sslIS.read();
        sslOS.write(85);
        sslOS.flush();

        sslSocket.close();

    }

    /*
     * Define the client side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doClientSide() throws Exception {
        /*
         * Wait for server to get started.
         */
        while (!serverReady) {
            Thread.sleep(50);
        }

        SSLContext context = getSSLContext(trusedCertStr, null, null);
        SSLSocketFactory sslsf = context.getSocketFactory();

        SSLSocket sslSocket =
            (SSLSocket)sslsf.createSocket("localhost", serverPort);

        InputStream sslIS = sslSocket.getInputStream();
        OutputStream sslOS = sslSocket.getOutputStream();

        sslOS.write(280);
        sslOS.flush();
        sslIS.read();

        sslSocket.close();
    }

    // get the ssl context
    private static SSLContext getSSLContext(String trusedCertStr,
            String keyCertStr, String keySpecStr) throws Exception {

        // generate certificate from cert string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // create a key store
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);

        // import the trused cert
        Certificate trusedCert = null;
        ByteArrayInputStream is = null;
        if (trusedCertStr != null) {
            is = new ByteArrayInputStream(trusedCertStr.getBytes());
            trusedCert = cf.generateCertificate(is);
            is.close();

            ks.setCertificateEntry("RSA Export Signer", trusedCert);
        }

        if (keyCertStr != null) {
            // generate the private key.
            PKCS8EncodedKeySpec priKeySpec = new PKCS8EncodedKeySpec(
                                Base64.getMimeDecoder().decode(keySpecStr));
            KeyFactory kf = KeyFactory.getInstance("RSA");
            RSAPrivateKey priKey =
                    (RSAPrivateKey)kf.generatePrivate(priKeySpec);

            // generate certificate chain
            is = new ByteArrayInputStream(keyCertStr.getBytes());
            Certificate keyCert = cf.generateCertificate(is);
            is.close();

            Certificate[] chain = null;
            if (trusedCert != null) {
                chain = new Certificate[2];
                chain[0] = keyCert;
                chain[1] = trusedCert;
            } else {
                chain = new Certificate[1];
                chain[0] = keyCert;
            }

            // import the key entry.
            ks.setKeyEntry("Whatever", priKey, passphrase, chain);
        }

        // create SSL context
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmAlgorithm);
        tmf.init(ks);

        SSLContext ctx = SSLContext.getInstance("TLS");
        if (keyCertStr != null && !keyCertStr.isEmpty()) {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("NewSunX509");
            kmf.init(ks, passphrase);

            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            ks = null;
        } else {
            ctx.init(null, tmf.getTrustManagers(), null);
        }

        return ctx;
    }

    private static String tmAlgorithm;        // trust manager

    private static void parseArguments(String[] args) {
        tmAlgorithm = args[0];
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */

    // use any free port by default
    volatile int serverPort = 0;

    volatile Exception serverException = null;
    volatile Exception clientException = null;

    public static void main(String args[]) throws Exception {
        if (debug)
            System.setProperty("javax.net.debug", "all");

        /*
         * Get the customized arguments.
         */
        parseArguments(args);

        /*
         * Start the tests.
         */
        new SelfIssuedCert();
    }

    Thread clientThread = null;
    Thread serverThread = null;
    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    SelfIssuedCert() throws Exception {
        if (separateServerThread) {
            startServer(true);
            startClient(false);
        } else {
            startClient(true);
            startServer(false);
        }

        /*
         * Wait for other side to close down.
         */
        if (separateServerThread) {
            serverThread.join();
        } else {
            clientThread.join();
        }

        /*
         * When we get here, the test is pretty much over.
         *
         * If the main thread excepted, that propagates back
         * immediately.  If the other thread threw an exception, we
         * should report back.
         */
        if (serverException != null)
            throw serverException;
        if (clientException != null)
            throw clientException;
    }

    void startServer(boolean newThread) throws Exception {
        if (newThread) {
            serverThread = new Thread() {
                public void run() {
                    try {
                        doServerSide();
                    } catch (Exception e) {
                        /*
                         * Our server thread just died.
                         *
                         * Release the client, if not active already...
                         */
                        System.err.println("Server died...");
                        serverReady = true;
                        serverException = e;
                    }
                }
            };
            serverThread.start();
        } else {
            doServerSide();
        }
    }

    void startClient(boolean newThread) throws Exception {
        if (newThread) {
            clientThread = new Thread() {
                public void run() {
                    try {
                        doClientSide();
                    } catch (Exception e) {
                        /*
                         * Our client thread just died.
                         */
                        System.err.println("Client died...");
                        clientException = e;
                    }
                }
            };
            clientThread.start();
        } else {
            doClientSide();
        }
    }

}
