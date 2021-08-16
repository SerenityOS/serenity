/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.

/*
 * @test
 * @bug 8139565
 * @summary Restrict certificates with DSA keys less than 1024 bits
 *
 * @run main/othervm DisabledShortDSAKeys PKIX TLSv1.2
 * @run main/othervm DisabledShortDSAKeys SunX509 TLSv1.2
 * @run main/othervm DisabledShortDSAKeys PKIX TLSv1.1
 * @run main/othervm DisabledShortDSAKeys SunX509 TLSv1.1
 * @run main/othervm DisabledShortDSAKeys PKIX TLSv1
 * @run main/othervm DisabledShortDSAKeys SunX509 TLSv1
 * @run main/othervm DisabledShortDSAKeys PKIX SSLv3
 * @run main/othervm DisabledShortDSAKeys SunX509 SSLv3
 */

import java.net.*;
import java.util.*;
import java.io.*;
import javax.net.ssl.*;
import java.security.Security;
import java.security.KeyStore;
import java.security.KeyFactory;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.*;
import java.security.interfaces.*;
import java.util.Base64;


public class DisabledShortDSAKeys {

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
    // Certificates and key used in the test.
    static String trustedCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDDjCCAs2gAwIBAgIJAO5/hbm1ByJOMAkGByqGSM44BAMwHzELMAkGA1UEBhMC\n" +
        "VVMxEDAOBgNVBAoTB0V4YW1wbGUwHhcNMTYwMjE2MDQzNTQ2WhcNMzcwMTI2MDQz\n" +
        "NTQ2WjAfMQswCQYDVQQGEwJVUzEQMA4GA1UEChMHRXhhbXBsZTCCAbgwggEsBgcq\n" +
        "hkjOOAQBMIIBHwKBgQC4aSK8nBYdWJtuBkz6yoDyjZnNuGFSpDmx1ggKpLpcnPuw\n" +
        "YKAbUhqdYhZtaIqQ4aO0T1ZS/HuOM0zvddnMUidFNX3RUvDkvdD/JYOnjqzCm+xW\n" +
        "U0NFuPHZdapQY5KFk3ugkqZpHLY1StZbu0qugZOZjbBOMwB7cHAbMDuVpEr8DQIV\n" +
        "AOi+ig+h3okFbWEE9MztiI2+DqNrAoGBAKh2EZbuWU9NoHglhVzfDUoz8CeyW6W6\n" +
        "rUZuIOQsjWaYOeRPWX0UVAGq9ykIOfamEpurKt4H8ge/pHaL9iazJjonMHOXG12A\n" +
        "0lALsMDGv22zVaJzXjOBvdPzc87opr0LIVgHASKOcDYjsICKNYPlS2cL3MJoD+bj\n" +
        "NAR67b90VBbEA4GFAAKBgQCGrkRp2tdj2mZF7Qz0tO6p3xSysbEfN6QZxOJYPTvM\n" +
        "yIYfLV9Yoy7XaRd/mCpJo/dqmsZMzowtyi+u+enuVpOLKiq/lyCktL+xUzZAjLT+\n" +
        "9dafHlS1wR3pDSa1spo9xTEi4Ff/DQDHcdGalBxSXX/UdRtSecIYAp5/fkt3QZ5v\n" +
        "0aOBkTCBjjAdBgNVHQ4EFgQUX4qbP5PgBx1J8BJ8qEgfoKVLSnQwTwYDVR0jBEgw\n" +
        "RoAUX4qbP5PgBx1J8BJ8qEgfoKVLSnShI6QhMB8xCzAJBgNVBAYTAlVTMRAwDgYD\n" +
        "VQQKEwdFeGFtcGxlggkA7n+FubUHIk4wDwYDVR0TAQH/BAUwAwEB/zALBgNVHQ8E\n" +
        "BAMCAgQwCQYHKoZIzjgEAwMwADAtAhUAkr5bINXyy/McAx6qwhb6r0/QJUgCFFUP\n" +
        "CZokA4/NqJIgq8ThpTQAE8SB\n" +
        "-----END CERTIFICATE-----";

    static String targetCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICUjCCAhGgAwIBAgIJAIiDrs/4W8rtMAkGByqGSM44BAMwHzELMAkGA1UEBhMC\n" +
        "VVMxEDAOBgNVBAoTB0V4YW1wbGUwHhcNMTYwMjE2MDQzNTQ2WhcNMzUxMTAzMDQz\n" +
        "NTQ2WjA5MQswCQYDVQQGEwJVUzEQMA4GA1UECgwHRXhhbXBsZTEYMBYGA1UEAwwP\n" +
        "d3d3LmV4YW1wbGUuY29tMIHwMIGoBgcqhkjOOAQBMIGcAkEAs6A0p3TysTtVXGSv\n" +
        "ThR/8GHpbL49KyWRJBMIlmLc5jl/wxJgnL1t07p4YTOEa6ecyTFos04Z8n2GARmp\n" +
        "zYlUywIVAJLDcf4JXhZbguRFSQdWwWhZkh+LAkBLCzh3Xvpmc/5CDqU+QHqDcuSk\n" +
        "5B8+ZHaHRi2KQ00ejilpF2qZpW5JdHe4m3Pggh0MIuaAGX+leM4JKlnObj14A0MA\n" +
        "AkAYb+DYlFgStFhF1ip7rFzY8K6i/3ellkXI2umI/XVwxUQTHSlk5nFOep5Dfzm9\n" +
        "pADJwuSe1qGHsHB5LpMZPVpto4GEMIGBMAkGA1UdEwQCMAAwCwYDVR0PBAQDAgPo\n" +
        "MB0GA1UdDgQWBBT8nsFyccF4q1dtpWE1dkNK5UiXtTAfBgNVHSMEGDAWgBRfips/\n" +
        "k+AHHUnwEnyoSB+gpUtKdDAnBgNVHSUEIDAeBggrBgEFBQcDAQYIKwYBBQUHAwIG\n" +
        "CCsGAQUFBwMDMAkGByqGSM44BAMDMAAwLQIUIcIlxpIwaZXdpMC+U076unR1Mp8C\n" +
        "FQCD/NE8O0xwq57nwFfp7tUvUHYMMA==\n" +
        "-----END CERTIFICATE-----";

    // Private key in the format of PKCS#8, key size is 512 bits.
    static String targetPrivateKey =
        "MIHGAgEAMIGoBgcqhkjOOAQBMIGcAkEAs6A0p3TysTtVXGSvThR/8GHpbL49KyWR\n" +
        "JBMIlmLc5jl/wxJgnL1t07p4YTOEa6ecyTFos04Z8n2GARmpzYlUywIVAJLDcf4J\n" +
        "XhZbguRFSQdWwWhZkh+LAkBLCzh3Xvpmc/5CDqU+QHqDcuSk5B8+ZHaHRi2KQ00e\n" +
        "jilpF2qZpW5JdHe4m3Pggh0MIuaAGX+leM4JKlnObj14BBYCFHB2Wek2g5hpNj5y\n" +
        "RQfCc6CFO0dv";

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
        SSLContext context = generateSSLContext(null, targetCertStr,
                                            targetPrivateKey);
        SSLServerSocketFactory sslssf = context.getServerSocketFactory();
        SSLServerSocket sslServerSocket =
            (SSLServerSocket)sslssf.createServerSocket(serverPort);
        serverPort = sslServerSocket.getLocalPort();

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady = true;

        try (SSLSocket sslSocket = (SSLSocket)sslServerSocket.accept()) {
            try (InputStream sslIS = sslSocket.getInputStream()) {
                sslIS.read();
            }

            throw new Exception(
                    "DSA keys shorter than 1024 bits should be disabled");
        } catch (SSLHandshakeException sslhe) {
            // the expected exception, ignore
        }
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

        SSLContext context = generateSSLContext(trustedCertStr, null, null);
        SSLSocketFactory sslsf = context.getSocketFactory();

        try (SSLSocket sslSocket =
            (SSLSocket)sslsf.createSocket("localhost", serverPort)) {

            // only enable the target protocol
            sslSocket.setEnabledProtocols(new String[] {enabledProtocol});

            // enable a block cipher
            sslSocket.setEnabledCipherSuites(
                new String[] {"TLS_DHE_DSS_WITH_AES_128_CBC_SHA"});

            try (OutputStream sslOS = sslSocket.getOutputStream()) {
                sslOS.write('B');
                sslOS.flush();
            }

            throw new Exception(
                    "DSA keys shorter than 1024 bits should be disabled");
        } catch (SSLHandshakeException sslhe) {
            // the expected exception, ignore
        }
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */
    private static String tmAlgorithm;        // trust manager
    private static String enabledProtocol;    // the target protocol

    private static void parseArguments(String[] args) {
        tmAlgorithm = args[0];
        enabledProtocol = args[1];
    }

    private static SSLContext generateSSLContext(String trustedCertStr,
            String keyCertStr, String keySpecStr) throws Exception {

        // generate certificate from cert string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // create a key store
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);

        // import the trused cert
        Certificate trusedCert = null;
        ByteArrayInputStream is = null;
        if (trustedCertStr != null) {
            is = new ByteArrayInputStream(trustedCertStr.getBytes());
            trusedCert = cf.generateCertificate(is);
            is.close();

            ks.setCertificateEntry("DSA Export Signer", trusedCert);
        }

        if (keyCertStr != null) {
            // generate the private key.
            PKCS8EncodedKeySpec priKeySpec = new PKCS8EncodedKeySpec(
                                Base64.getMimeDecoder().decode(keySpecStr));
            KeyFactory kf = KeyFactory.getInstance("DSA");
            DSAPrivateKey priKey =
                    (DSAPrivateKey)kf.generatePrivate(priKeySpec);

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


    // use any free port by default
    volatile int serverPort = 0;

    volatile Exception serverException = null;
    volatile Exception clientException = null;

    public static void main(String[] args) throws Exception {
        Security.setProperty("jdk.certpath.disabledAlgorithms",
                "DSA keySize < 1024");
        Security.setProperty("jdk.tls.disabledAlgorithms",
                "DSA keySize < 1024");

        if (debug) {
            System.setProperty("javax.net.debug", "all");
        }

        /*
         * Get the customized arguments.
         */
        parseArguments(args);

        /*
         * Start the tests.
         */
        new DisabledShortDSAKeys();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    DisabledShortDSAKeys() throws Exception {
        Exception startException = null;
        try {
            if (separateServerThread) {
                startServer(true);
                startClient(false);
            } else {
                startClient(true);
                startServer(false);
            }
        } catch (Exception e) {
            startException = e;
        }

        /*
         * Wait for other side to close down.
         */
        if (separateServerThread) {
            if (serverThread != null) {
                serverThread.join();
            }
        } else {
            if (clientThread != null) {
                clientThread.join();
            }
        }

        /*
         * When we get here, the test is pretty much over.
         * Which side threw the error?
         */
        Exception local;
        Exception remote;

        if (separateServerThread) {
            remote = serverException;
            local = clientException;
        } else {
            remote = clientException;
            local = serverException;
        }

        Exception exception = null;

        /*
         * Check various exception conditions.
         */
        if ((local != null) && (remote != null)) {
            // If both failed, return the curthread's exception.
            local.initCause(remote);
            exception = local;
        } else if (local != null) {
            exception = local;
        } else if (remote != null) {
            exception = remote;
        } else if (startException != null) {
            exception = startException;
        }

        /*
         * If there was an exception *AND* a startException,
         * output it.
         */
        if (exception != null) {
            if (exception != startException && startException != null) {
                exception.addSuppressed(startException);
            }
            throw exception;
        }

        // Fall-through: no exception to throw!
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
            try {
                doServerSide();
            } catch (Exception e) {
                serverException = e;
            } finally {
                serverReady = true;
            }
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
            try {
                doClientSide();
            } catch (Exception e) {
                clientException = e;
            }
        }
    }
}
