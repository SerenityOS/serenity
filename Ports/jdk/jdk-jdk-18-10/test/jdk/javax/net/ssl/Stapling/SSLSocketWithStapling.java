/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.

/*
 * @test
 * @bug 8046321 8153829
 * @summary OCSP Stapling for TLS
 * @library ../../../../java/security/testlibrary
 * @build CertificateBuilder SimpleOCSPServer
 * @run main/othervm SSLSocketWithStapling
 */

import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.Certificate;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.PKIXRevocationChecker.Option;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import sun.security.testlibrary.SimpleOCSPServer;
import sun.security.testlibrary.CertificateBuilder;

public class SSLSocketWithStapling {

    /*
     * =============================================================
     * Set the various variables needed for the tests, then
     * specify what tests to run on each side.
     */

    // Turn on TLS debugging
    static final boolean debug = false;

    /*
     * Should we run the client or server in a separate thread?
     * Both sides can throw exceptions, but do you have a preference
     * as to which side should be the main thread.
     */
    static boolean separateServerThread = true;
    Thread clientThread = null;
    Thread serverThread = null;

    static String passwd = "passphrase";
    static String ROOT_ALIAS = "root";
    static String INT_ALIAS = "intermediate";
    static String SSL_ALIAS = "ssl";

    /*
     * Is the server ready to serve?
     */
    volatile static boolean serverReady = false;
    volatile int serverPort = 0;

    volatile Exception serverException = null;
    volatile Exception clientException = null;

    // PKI components we will need for this test
    static KeyStore rootKeystore;           // Root CA Keystore
    static KeyStore intKeystore;            // Intermediate CA Keystore
    static KeyStore serverKeystore;         // SSL Server Keystore
    static KeyStore trustStore;             // SSL Client trust store
    static SimpleOCSPServer rootOcsp;       // Root CA OCSP Responder
    static int rootOcspPort;                // Port number for root OCSP
    static SimpleOCSPServer intOcsp;        // Intermediate CA OCSP Responder
    static int intOcspPort;                 // Port number for intermed. OCSP

    // Extra configuration parameters and constants
    static final String[] TLS13ONLY = new String[] { "TLSv1.3" };
    static final String[] TLS12MAX =
            new String[] { "TLSv1.2", "TLSv1.1", "TLSv1" };

    /*
     * If the client or server is doing some kind of object creation
     * that the other side depends on, and that thread prematurely
     * exits, you may experience a hang.  The test harness will
     * terminate all hung threads after its timeout has expired,
     * currently 3 minutes by default, but you might try to be
     * smart about it....
     */
    public static void main(String[] args) throws Exception {
        if (debug) {
            System.setProperty("javax.net.debug", "ssl:handshake");
        }

        try {
            // Create the PKI we will use for the test and start the OCSP servers
            createPKI();

            testAllDefault(false);
            testAllDefault(true);
            testPKIXParametersRevEnabled(false);
            testPKIXParametersRevEnabled(true);
            testRevokedCertificate(false);
            testRevokedCertificate(true);
            testRevokedIntermediate(false);
            testRevokedIntermediate(true);
            testMissingIntermediate(false);
            testMissingIntermediate(true);
            testHardFailFallback(false);
            testHardFailFallback(true);
            testSoftFailFallback(false);
            testSoftFailFallback(true);
            testLatencyNoStaple(false, false);
            testLatencyNoStaple(false, true);
            testLatencyNoStaple(true, false);
            testLatencyNoStaple(true, true);
        } finally {
            // shut down the OCSP responders before finishing the test
            intOcsp.stop();
            rootOcsp.stop();
        }
    }

    /**
     * Default test using no externally-configured PKIXBuilderParameters
     */
    static void testAllDefault(boolean isTls13) throws Exception {
        ClientParameters cliParams = new ClientParameters();
        ServerParameters servParams = new ServerParameters();
        if (isTls13) {
            cliParams.protocols = TLS13ONLY;
            servParams.protocols = TLS13ONLY;
        } else {
            cliParams.protocols = TLS12MAX;
            servParams.protocols = TLS12MAX;
        }
        serverReady = false;
        Map<BigInteger, SimpleOCSPServer.CertStatusInfo> revInfo =
                new HashMap<>();

        // We will prove revocation checking is disabled by marking the SSL
        // certificate as revoked.  The test would only pass if revocation
        // checking did not happen.
        X509Certificate sslCert =
                (X509Certificate)serverKeystore.getCertificate(SSL_ALIAS);
        Date fiveMinsAgo = new Date(System.currentTimeMillis() -
                TimeUnit.MINUTES.toMillis(5));
        revInfo.put(sslCert.getSerialNumber(),
                new SimpleOCSPServer.CertStatusInfo(
                        SimpleOCSPServer.CertStatus.CERT_STATUS_REVOKED,
                        fiveMinsAgo));
        intOcsp.updateStatusDb(revInfo);

        System.out.println("=======================================");
        System.out.println("Stapling enabled, default configuration");
        System.out.println("=======================================");

        SSLSocketWithStapling sslTest = new SSLSocketWithStapling(cliParams,
                servParams);
        TestResult tr = sslTest.getResult();
        if (tr.clientExc != null) {
            throw tr.clientExc;
        } else if (tr.serverExc != null) {
            throw tr.serverExc;
        }

        // Return the ssl certificate to non-revoked status
        revInfo.put(sslCert.getSerialNumber(),
                new SimpleOCSPServer.CertStatusInfo(
                        SimpleOCSPServer.CertStatus.CERT_STATUS_GOOD));
        intOcsp.updateStatusDb(revInfo);

        System.out.println("                PASS");
        System.out.println("=======================================\n");
    }

    /**
     * Do a basic connection using PKIXParameters with revocation checking
     * enabled and client-side OCSP disabled.  It will only pass if all
     * stapled responses are present, valid and have a GOOD status.
     */
    static void testPKIXParametersRevEnabled(boolean isTls13) throws Exception {
        ClientParameters cliParams = new ClientParameters();
        ServerParameters servParams = new ServerParameters();
        if (isTls13) {
            cliParams.protocols = TLS13ONLY;
            servParams.protocols = TLS13ONLY;
        } else {
            cliParams.protocols = TLS12MAX;
            servParams.protocols = TLS12MAX;
        }
        serverReady = false;

        System.out.println("=====================================");
        System.out.println("Stapling enabled, PKIXParameters with");
        System.out.println("Revocation checking enabled ");
        System.out.println("=====================================");

        cliParams.pkixParams = new PKIXBuilderParameters(trustStore,
                new X509CertSelector());
        cliParams.pkixParams.setRevocationEnabled(true);
        Security.setProperty("ocsp.enable", "false");

        SSLSocketWithStapling sslTest = new SSLSocketWithStapling(cliParams,
                servParams);
        TestResult tr = sslTest.getResult();
        if (tr.clientExc != null) {
            throw tr.clientExc;
        } else if (tr.serverExc != null) {
            throw tr.serverExc;
        }

        System.out.println("                PASS");
        System.out.println("=====================================\n");
    }

    /**
     * Perform a test where the certificate is revoked and placed in the
     * TLS handshake.  Client-side OCSP is disabled, so this test will only
     * pass if the OCSP response is found, since we will check the
     * CertPathValidatorException reason for revoked status.
     */
    static void testRevokedCertificate(boolean isTls13) throws Exception {
        ClientParameters cliParams = new ClientParameters();
        ServerParameters servParams = new ServerParameters();
        if (isTls13) {
            cliParams.protocols = TLS13ONLY;
            servParams.protocols = TLS13ONLY;
        } else {
            cliParams.protocols = TLS12MAX;
            servParams.protocols = TLS12MAX;
        }
        serverReady = false;
        Map<BigInteger, SimpleOCSPServer.CertStatusInfo> revInfo =
                new HashMap<>();

        // We will prove revocation checking is disabled by marking the SSL
        // certificate as revoked.  The test would only pass if revocation
        // checking did not happen.
        X509Certificate sslCert =
                (X509Certificate)serverKeystore.getCertificate(SSL_ALIAS);
        Date fiveMinsAgo = new Date(System.currentTimeMillis() -
                TimeUnit.MINUTES.toMillis(5));
        revInfo.put(sslCert.getSerialNumber(),
                new SimpleOCSPServer.CertStatusInfo(
                        SimpleOCSPServer.CertStatus.CERT_STATUS_REVOKED,
                        fiveMinsAgo));
        intOcsp.updateStatusDb(revInfo);

        System.out.println("============================================");
        System.out.println("Stapling enabled, detect revoked certificate");
        System.out.println("============================================");

        cliParams.pkixParams = new PKIXBuilderParameters(trustStore,
                new X509CertSelector());
        cliParams.pkixParams.setRevocationEnabled(true);
        Security.setProperty("ocsp.enable", "false");

        SSLSocketWithStapling sslTest = new SSLSocketWithStapling(cliParams,
                servParams);
        TestResult tr = sslTest.getResult();
        if (!checkClientValidationFailure(tr.clientExc, BasicReason.REVOKED)) {
            if (tr.clientExc != null) {
                throw tr.clientExc;
            } else {
                throw new RuntimeException(
                        "Expected client failure, but the client succeeded");
            }
        }

        // Return the ssl certificate to non-revoked status
        revInfo.put(sslCert.getSerialNumber(),
                new SimpleOCSPServer.CertStatusInfo(
                        SimpleOCSPServer.CertStatus.CERT_STATUS_GOOD));
        intOcsp.updateStatusDb(revInfo);

        System.out.println("                 PASS");
        System.out.println("=======================================\n");
    }

    /**
     * Perform a test where the intermediate CA certificate is revoked and
     * placed in the TLS handshake.  Client-side OCSP is disabled, so this
     * test will only pass if the OCSP response for the intermediate CA is
     * found and placed into the CertificateStatus or Certificate message
     * (depending on the protocol version) since we will check
     * the CertPathValidatorException reason for revoked status.
     */
    static void testRevokedIntermediate(boolean isTls13) throws Exception {
        ClientParameters cliParams = new ClientParameters();
        ServerParameters servParams = new ServerParameters();
        if (isTls13) {
            cliParams.protocols = TLS13ONLY;
            servParams.protocols = TLS13ONLY;
        } else {
            cliParams.protocols = TLS12MAX;
            servParams.protocols = TLS12MAX;
        }
        serverReady = false;
        Map<BigInteger, SimpleOCSPServer.CertStatusInfo> revInfo =
                new HashMap<>();

        // We will prove revocation checking is disabled by marking the SSL
        // certificate as revoked.  The test would only pass if revocation
        // checking did not happen.
        X509Certificate intCACert =
                (X509Certificate)intKeystore.getCertificate(INT_ALIAS);
        Date fiveMinsAgo = new Date(System.currentTimeMillis() -
                TimeUnit.MINUTES.toMillis(5));
        revInfo.put(intCACert.getSerialNumber(),
                new SimpleOCSPServer.CertStatusInfo(
                        SimpleOCSPServer.CertStatus.CERT_STATUS_REVOKED,
                        fiveMinsAgo));
        rootOcsp.updateStatusDb(revInfo);

        System.out.println("===============================================");
        System.out.println("Stapling enabled, detect revoked CA certificate");
        System.out.println("===============================================");

        cliParams.pkixParams = new PKIXBuilderParameters(trustStore,
                new X509CertSelector());
        cliParams.pkixParams.setRevocationEnabled(true);
        Security.setProperty("ocsp.enable", "false");

        SSLSocketWithStapling sslTest = new SSLSocketWithStapling(cliParams,
                servParams);
        TestResult tr = sslTest.getResult();
        if (!checkClientValidationFailure(tr.clientExc, BasicReason.REVOKED)) {
            if (tr.clientExc != null) {
                throw tr.clientExc;
            } else {
                throw new RuntimeException(
                        "Expected client failure, but the client succeeded");
            }
        }

        // Return the ssl certificate to non-revoked status
        revInfo.put(intCACert.getSerialNumber(),
                new SimpleOCSPServer.CertStatusInfo(
                        SimpleOCSPServer.CertStatus.CERT_STATUS_GOOD));
        rootOcsp.updateStatusDb(revInfo);

        System.out.println("                 PASS");
        System.out.println("=======================================\n");
    }

    /**
     * Test a case where OCSP stapling is attempted, but partially occurs
     * because the root OCSP responder is unreachable.  This should use a
     * default hard-fail behavior.
     */
    static void testMissingIntermediate(boolean isTls13) throws Exception {
        ClientParameters cliParams = new ClientParameters();
        ServerParameters servParams = new ServerParameters();
        if (isTls13) {
            cliParams.protocols = TLS13ONLY;
            servParams.protocols = TLS13ONLY;
        } else {
            cliParams.protocols = TLS12MAX;
            servParams.protocols = TLS12MAX;
        }
        serverReady = false;

        // Make the OCSP responder reject connections
        rootOcsp.rejectConnections();

        System.out.println("=======================================");
        System.out.println("Stapling enbled in client and server,");
        System.out.println("but root OCSP responder disabled.");
        System.out.println("PKIXParameters with Revocation checking");
        System.out.println("enabled.");
        System.out.println("=======================================");

        Security.setProperty("ocsp.enable", "false");
        cliParams.pkixParams = new PKIXBuilderParameters(trustStore,
                new X509CertSelector());
        cliParams.pkixParams.setRevocationEnabled(true);

        SSLSocketWithStapling sslTest = new SSLSocketWithStapling(cliParams,
                servParams);
        TestResult tr = sslTest.getResult();
        if (!checkClientValidationFailure(tr.clientExc,
                BasicReason.UNDETERMINED_REVOCATION_STATUS)) {
            if (tr.clientExc != null) {
                throw tr.clientExc;
            } else {
                throw new RuntimeException(
                        "Expected client failure, but the client succeeded");
            }
        }

        System.out.println("                 PASS");
        System.out.println("=======================================\n");

        // Make root OCSP responder accept connections
        rootOcsp.acceptConnections();

        // Wait 5 seconds for server ready
        for (int i = 0; (i < 100 && !rootOcsp.isServerReady()); i++) {
            Thread.sleep(50);
        }
        if (!rootOcsp.isServerReady()) {
            throw new RuntimeException("Root OCSP responder not ready yet");
        }
    }

    /**
     * Test a case where client-side stapling is attempted, but does not
     * occur because OCSP responders are unreachable.  This should use a
     * default hard-fail behavior.
     */
    static void testHardFailFallback(boolean isTls13) throws Exception {
        ClientParameters cliParams = new ClientParameters();
        ServerParameters servParams = new ServerParameters();
        if (isTls13) {
            cliParams.protocols = TLS13ONLY;
            servParams.protocols = TLS13ONLY;
        } else {
            cliParams.protocols = TLS12MAX;
            servParams.protocols = TLS12MAX;
        }
        serverReady = false;

        // make OCSP responders reject connections
        intOcsp.rejectConnections();
        rootOcsp.rejectConnections();

        System.out.println("=======================================");
        System.out.println("Stapling enbled in client and server,");
        System.out.println("but OCSP responders disabled.");
        System.out.println("PKIXParameters with Revocation checking");
        System.out.println("enabled.");
        System.out.println("=======================================");

        Security.setProperty("ocsp.enable", "true");
        cliParams.pkixParams = new PKIXBuilderParameters(trustStore,
                new X509CertSelector());
        cliParams.pkixParams.setRevocationEnabled(true);

        SSLSocketWithStapling sslTest = new SSLSocketWithStapling(cliParams,
                servParams);
        TestResult tr = sslTest.getResult();
        if (!checkClientValidationFailure(tr.clientExc,
                BasicReason.UNDETERMINED_REVOCATION_STATUS)) {
            if (tr.clientExc != null) {
                throw tr.clientExc;
            } else {
                throw new RuntimeException(
                        "Expected client failure, but the client succeeded");
            }
        }

        System.out.println("                 PASS");
        System.out.println("=======================================\n");

        // Make OCSP responders accept connections
        intOcsp.acceptConnections();
        rootOcsp.acceptConnections();

        // Wait 5 seconds for server ready
        for (int i = 0; (i < 100 && (!intOcsp.isServerReady() ||
                !rootOcsp.isServerReady())); i++) {
            Thread.sleep(50);
        }
        if (!intOcsp.isServerReady() || !rootOcsp.isServerReady()) {
            throw new RuntimeException("Server not ready yet");
        }
    }

    /**
     * Test a case where client-side stapling is attempted, but does not
     * occur because OCSP responders are unreachable.  Client-side OCSP
     * checking is enabled for this, with SOFT_FAIL.
     */
    static void testSoftFailFallback(boolean isTls13) throws Exception {
        ClientParameters cliParams = new ClientParameters();
        ServerParameters servParams = new ServerParameters();
        if (isTls13) {
            cliParams.protocols = TLS13ONLY;
            servParams.protocols = TLS13ONLY;
        } else {
            cliParams.protocols = TLS12MAX;
            servParams.protocols = TLS12MAX;
        }
        serverReady = false;

        // make OCSP responders reject connections
        intOcsp.rejectConnections();
        rootOcsp.rejectConnections();

        System.out.println("=======================================");
        System.out.println("Stapling enbled in client and server,");
        System.out.println("but OCSP responders disabled.");
        System.out.println("PKIXParameters with Revocation checking");
        System.out.println("enabled and SOFT_FAIL.");
        System.out.println("=======================================");

        Security.setProperty("ocsp.enable", "true");
        cliParams.pkixParams = new PKIXBuilderParameters(trustStore,
                new X509CertSelector());
        cliParams.pkixParams.setRevocationEnabled(true);
        CertPathValidator cpv = CertPathValidator.getInstance("PKIX");
        cliParams.revChecker =
                (PKIXRevocationChecker)cpv.getRevocationChecker();
        cliParams.revChecker.setOptions(EnumSet.of(Option.SOFT_FAIL));

        SSLSocketWithStapling sslTest = new SSLSocketWithStapling(cliParams,
                servParams);
        TestResult tr = sslTest.getResult();
        if (tr.clientExc != null) {
            throw tr.clientExc;
        } else if (tr.serverExc != null) {
            throw tr.serverExc;
        }

        // make sure getSoftFailExceptions is not empty
        if (cliParams.revChecker.getSoftFailExceptions().isEmpty()) {
            throw new Exception("No soft fail exceptions");
        }

        System.out.println("                 PASS");
        System.out.println("=======================================\n");


        // Make OCSP responders accept connections
        intOcsp.acceptConnections();
        rootOcsp.acceptConnections();

        // Wait 5 seconds for server ready
        for (int i = 0; (i < 100 && (!intOcsp.isServerReady() ||
                        !rootOcsp.isServerReady())); i++) {
            Thread.sleep(50);
        }
        if (!intOcsp.isServerReady() || !rootOcsp.isServerReady()) {
            throw new RuntimeException("Server not ready yet");
        }
    }

    /**
     * This test initiates stapling from the client, but the server does not
     * support OCSP stapling for this connection.  In this case it happens
     * because the latency of the OCSP responders is longer than the server
     * is willing to wait.  To keep the test streamlined, we will set the server
     * latency to a 1 second wait, and set the responder latency to 3 seconds.
     *
     * @param fallback if we allow client-side OCSP fallback, which
     * will change the result from the client failing with CPVE (no fallback)
     * to a pass (fallback active).
     */
    static void testLatencyNoStaple(Boolean fallback, boolean isTls13)
            throws Exception {
        ClientParameters cliParams = new ClientParameters();
        ServerParameters servParams = new ServerParameters();
        if (isTls13) {
            cliParams.protocols = TLS13ONLY;
            servParams.protocols = TLS13ONLY;
        } else {
            cliParams.protocols = TLS12MAX;
            servParams.protocols = TLS12MAX;
        }
        serverReady = false;

        // Give a 1 second delay before running the test.
        intOcsp.setDelay(3000);
        rootOcsp.setDelay(3000);
        Thread.sleep(1000);

        // Wait 5 seconds for server ready
        for (int i = 0; (i < 100 && (!intOcsp.isServerReady() ||
                        !rootOcsp.isServerReady())); i++) {
            Thread.sleep(50);
        }
        if (!intOcsp.isServerReady() || !rootOcsp.isServerReady()) {
            throw new RuntimeException("Server not ready yet");
        }

        System.out.println("========================================");
        System.out.println("Stapling enbled in client.  Server does");
        System.out.println("not support stapling due to OCSP latency.");
        System.out.println("PKIXParameters with Revocation checking");
        System.out.println("enabled, client-side OCSP checking is.");
        System.out.println(fallback ? "enabled" : "disabled");
        System.out.println("========================================");

        Security.setProperty("ocsp.enable", fallback.toString());
        cliParams.pkixParams = new PKIXBuilderParameters(trustStore,
                new X509CertSelector());
        cliParams.pkixParams.setRevocationEnabled(true);
        servParams.respTimeout = 1000;

        SSLSocketWithStapling sslTest = new SSLSocketWithStapling(cliParams,
                servParams);
        TestResult tr = sslTest.getResult();

        if (fallback) {
            if (tr.clientExc != null) {
                throw tr.clientExc;
            } else if (tr.serverExc != null) {
                throw tr.serverExc;
            }
        } else {
            if (!checkClientValidationFailure(tr.clientExc,
                    BasicReason.UNDETERMINED_REVOCATION_STATUS)) {
                if (tr.clientExc != null) {
                    throw tr.clientExc;
                } else {
                    throw new RuntimeException(
                        "Expected client failure, but the client succeeded");
                }
            }
        }
        System.out.println("                 PASS");
        System.out.println("========================================\n");

        // Remove the OCSP responder latency
        intOcsp.setDelay(0);
        rootOcsp.setDelay(0);
        Thread.sleep(1000);

        // Wait 5 seconds for server ready
        for (int i = 0; (i < 100 && (!intOcsp.isServerReady() ||
                !rootOcsp.isServerReady())); i++) {
            Thread.sleep(50);
        }
        if (!intOcsp.isServerReady() || !rootOcsp.isServerReady()) {
            throw new RuntimeException("Server not ready yet");
        }
    }

    /*
     * Define the server side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doServerSide(ServerParameters servParams) throws Exception {

        // Selectively enable or disable the feature
        System.setProperty("jdk.tls.server.enableStatusRequestExtension",
                Boolean.toString(servParams.enabled));

        // Set all the other operating parameters
        System.setProperty("jdk.tls.stapling.cacheSize",
                Integer.toString(servParams.cacheSize));
        System.setProperty("jdk.tls.stapling.cacheLifetime",
                Integer.toString(servParams.cacheLifetime));
        System.setProperty("jdk.tls.stapling.responseTimeout",
                Integer.toString(servParams.respTimeout));
        System.setProperty("jdk.tls.stapling.responderURI", servParams.respUri);
        System.setProperty("jdk.tls.stapling.responderOverride",
                Boolean.toString(servParams.respOverride));
        System.setProperty("jdk.tls.stapling.ignoreExtensions",
                Boolean.toString(servParams.ignoreExts));

        // Set keystores and trust stores for the server
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(serverKeystore, passwd.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(trustStore);

        SSLContext sslc = SSLContext.getInstance("TLS");
        sslc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        SSLServerSocketFactory sslssf = new CustomizedServerSocketFactory(sslc,
                servParams.protocols, servParams.ciphers);

        try (SSLServerSocket sslServerSocket =
                (SSLServerSocket) sslssf.createServerSocket(serverPort)) {

            serverPort = sslServerSocket.getLocalPort();

            /*
             * Signal Client, we're ready for his connect.
             */
            serverReady = true;

            try (SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
                    InputStream sslIS = sslSocket.getInputStream();
                    OutputStream sslOS = sslSocket.getOutputStream()) {
                int numberIn = sslIS.read();
                int numberSent = 85;
                log("Server received number: " + numberIn);
                sslOS.write(numberSent);
                sslOS.flush();
                log("Server sent number: " + numberSent);
            }
        }
    }

    /*
     * Define the client side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doClientSide(ClientParameters cliParams) throws Exception {

        // Wait 5 seconds for server ready
        for (int i = 0; (i < 100 && !serverReady); i++) {
            Thread.sleep(50);
        }
        if (!serverReady) {
            throw new RuntimeException("Server not ready yet");
        }

        // Selectively enable or disable the feature
        System.setProperty("jdk.tls.client.enableStatusRequestExtension",
                Boolean.toString(cliParams.enabled));

        // Create the Trust Manager Factory using the PKIX variant
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");

        // If we have a customized pkixParameters then use it
        if (cliParams.pkixParams != null) {
            // LIf we have a customized PKIXRevocationChecker, add
            // it to the PKIXBuilderParameters.
            if (cliParams.revChecker != null) {
                cliParams.pkixParams.addCertPathChecker(cliParams.revChecker);
            }

            ManagerFactoryParameters trustParams =
                    new CertPathTrustManagerParameters(cliParams.pkixParams);
            tmf.init(trustParams);
        } else {
            tmf.init(trustStore);
        }

        SSLContext sslc = SSLContext.getInstance("TLS");
        sslc.init(null, tmf.getTrustManagers(), null);

        SSLSocketFactory sslsf = new CustomizedSocketFactory(sslc,
                cliParams.protocols, cliParams.ciphers);
        try (SSLSocket sslSocket = (SSLSocket)sslsf.createSocket("localhost",
                serverPort);
                InputStream sslIS = sslSocket.getInputStream();
                OutputStream sslOS = sslSocket.getOutputStream()) {
            int numberSent = 80;
            sslOS.write(numberSent);
            sslOS.flush();
            log("Client sent number: " + numberSent);
            int numberIn = sslIS.read();
            log("Client received number:" + numberIn);
        }
    }

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    SSLSocketWithStapling(ClientParameters cliParams,
            ServerParameters servParams) throws Exception {
        Exception startException = null;
        try {
            if (separateServerThread) {
                startServer(servParams, true);
                startClient(cliParams, false);
            } else {
                startClient(cliParams, true);
                startServer(servParams, false);
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
    }

    /**
     * Checks a validation failure to see if it failed for the reason we think
     * it should.  This comes in as an SSLException of some sort, but it
     * encapsulates a ValidatorException which in turn encapsulates the
     * CertPathValidatorException we are interested in.
     *
     * @param e the exception thrown at the top level
     * @param reason the underlying CertPathValidatorException BasicReason
     * we are expecting it to have.
     *
     * @return true if the reason matches up, false otherwise.
     */
    static boolean checkClientValidationFailure(Exception e,
            BasicReason reason) {
        boolean result = false;

        if (e instanceof SSLException) {
            Throwable valExc = e.getCause();
            if (valExc instanceof sun.security.validator.ValidatorException) {
                Throwable cause = valExc.getCause();
                if (cause instanceof CertPathValidatorException) {
                    CertPathValidatorException cpve =
                            (CertPathValidatorException)cause;
                    if (cpve.getReason() == reason) {
                        result = true;
                    }
                }
            }
        }
        return result;
    }

    TestResult getResult() {
        TestResult tr = new TestResult();
        tr.clientExc = clientException;
        tr.serverExc = serverException;
        return tr;
    }

    void startServer(ServerParameters servParams, boolean newThread)
            throws Exception {
        if (newThread) {
            serverThread = new Thread() {
                public void run() {
                    try {
                        doServerSide(servParams);
                    } catch (Exception e) {
                        /*
                         * Our server thread just died.
                         *
                         * Release the client, if not active already...
                         */
                        System.err.println("Server died...");
                        e.printStackTrace(System.err);
                        serverReady = true;
                        serverException = e;
                    }
                }
            };
            serverThread.start();
        } else {
            try {
                doServerSide(servParams);
            } catch (Exception e) {
                serverException = e;
            } finally {
                serverReady = true;
            }
        }
    }

    void startClient(ClientParameters cliParams, boolean newThread)
            throws Exception {
        if (newThread) {
            clientThread = new Thread() {
                public void run() {
                    try {
                        doClientSide(cliParams);
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
                doClientSide(cliParams);
            } catch (Exception e) {
                clientException = e;
            }
        }
    }

    /**
     * Creates the PKI components necessary for this test, including
     * Root CA, Intermediate CA and SSL server certificates, the keystores
     * for each entity, a client trust store, and starts the OCSP responders.
     */
    private static void createPKI() throws Exception {
        CertificateBuilder cbld = new CertificateBuilder();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyStore.Builder keyStoreBuilder =
                KeyStore.Builder.newInstance("PKCS12", null,
                        new KeyStore.PasswordProtection(passwd.toCharArray()));

        // Generate Root, IntCA, EE keys
        KeyPair rootCaKP = keyGen.genKeyPair();
        log("Generated Root CA KeyPair");
        KeyPair intCaKP = keyGen.genKeyPair();
        log("Generated Intermediate CA KeyPair");
        KeyPair sslKP = keyGen.genKeyPair();
        log("Generated SSL Cert KeyPair");

        // Set up the Root CA Cert
        cbld.setSubjectName("CN=Root CA Cert, O=SomeCompany");
        cbld.setPublicKey(rootCaKP.getPublic());
        cbld.setSerialNumber(new BigInteger("1"));
        // Make a 3 year validity starting from 60 days ago
        long start = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(60);
        long end = start + TimeUnit.DAYS.toMillis(1085);
        cbld.setValidity(new Date(start), new Date(end));
        addCommonExts(cbld, rootCaKP.getPublic(), rootCaKP.getPublic());
        addCommonCAExts(cbld);
        // Make our Root CA Cert!
        X509Certificate rootCert = cbld.build(null, rootCaKP.getPrivate(),
                "SHA256withRSA");
        log("Root CA Created:\n" + certInfo(rootCert));

        // Now build a keystore and add the keys and cert
        rootKeystore = keyStoreBuilder.getKeyStore();
        Certificate[] rootChain = {rootCert};
        rootKeystore.setKeyEntry(ROOT_ALIAS, rootCaKP.getPrivate(),
                passwd.toCharArray(), rootChain);

        // Now fire up the OCSP responder
        rootOcsp = new SimpleOCSPServer(rootKeystore, passwd, ROOT_ALIAS, null);
        rootOcsp.enableLog(debug);
        rootOcsp.setNextUpdateInterval(3600);
        rootOcsp.start();

        // Wait 5 seconds for server ready
        for (int i = 0; (i < 100 && !rootOcsp.isServerReady()); i++) {
            Thread.sleep(50);
        }
        if (!rootOcsp.isServerReady()) {
            throw new RuntimeException("Server not ready yet");
        }

        rootOcspPort = rootOcsp.getPort();
        String rootRespURI = "http://localhost:" + rootOcspPort;
        log("Root OCSP Responder URI is " + rootRespURI);

        // Now that we have the root keystore and OCSP responder we can
        // create our intermediate CA.
        cbld.reset();
        cbld.setSubjectName("CN=Intermediate CA Cert, O=SomeCompany");
        cbld.setPublicKey(intCaKP.getPublic());
        cbld.setSerialNumber(new BigInteger("100"));
        // Make a 2 year validity starting from 30 days ago
        start = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);
        end = start + TimeUnit.DAYS.toMillis(730);
        cbld.setValidity(new Date(start), new Date(end));
        addCommonExts(cbld, intCaKP.getPublic(), rootCaKP.getPublic());
        addCommonCAExts(cbld);
        cbld.addAIAExt(Collections.singletonList(rootRespURI));
        // Make our Intermediate CA Cert!
        X509Certificate intCaCert = cbld.build(rootCert, rootCaKP.getPrivate(),
                "SHA256withRSA");
        log("Intermediate CA Created:\n" + certInfo(intCaCert));

        // Provide intermediate CA cert revocation info to the Root CA
        // OCSP responder.
        Map<BigInteger, SimpleOCSPServer.CertStatusInfo> revInfo =
            new HashMap<>();
        revInfo.put(intCaCert.getSerialNumber(),
                new SimpleOCSPServer.CertStatusInfo(
                        SimpleOCSPServer.CertStatus.CERT_STATUS_GOOD));
        rootOcsp.updateStatusDb(revInfo);

        // Now build a keystore and add the keys, chain and root cert as a TA
        intKeystore = keyStoreBuilder.getKeyStore();
        Certificate[] intChain = {intCaCert, rootCert};
        intKeystore.setKeyEntry(INT_ALIAS, intCaKP.getPrivate(),
                passwd.toCharArray(), intChain);
        intKeystore.setCertificateEntry(ROOT_ALIAS, rootCert);

        // Now fire up the Intermediate CA OCSP responder
        intOcsp = new SimpleOCSPServer(intKeystore, passwd,
                INT_ALIAS, null);
        intOcsp.enableLog(debug);
        intOcsp.setNextUpdateInterval(3600);
        intOcsp.start();

        // Wait 5 seconds for server ready
        for (int i = 0; (i < 100 && !intOcsp.isServerReady()); i++) {
            Thread.sleep(50);
        }
        if (!intOcsp.isServerReady()) {
            throw new RuntimeException("Server not ready yet");
        }

        intOcspPort = intOcsp.getPort();
        String intCaRespURI = "http://localhost:" + intOcspPort;
        log("Intermediate CA OCSP Responder URI is " + intCaRespURI);

        // Last but not least, let's make our SSLCert and add it to its own
        // Keystore
        cbld.reset();
        cbld.setSubjectName("CN=SSLCertificate, O=SomeCompany");
        cbld.setPublicKey(sslKP.getPublic());
        cbld.setSerialNumber(new BigInteger("4096"));
        // Make a 1 year validity starting from 7 days ago
        start = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
        end = start + TimeUnit.DAYS.toMillis(365);
        cbld.setValidity(new Date(start), new Date(end));

        // Add extensions
        addCommonExts(cbld, sslKP.getPublic(), intCaKP.getPublic());
        boolean[] kuBits = {true, false, true, false, false, false,
            false, false, false};
        cbld.addKeyUsageExt(kuBits);
        List<String> ekuOids = new ArrayList<>();
        ekuOids.add("1.3.6.1.5.5.7.3.1");
        ekuOids.add("1.3.6.1.5.5.7.3.2");
        cbld.addExtendedKeyUsageExt(ekuOids);
        cbld.addSubjectAltNameDNSExt(Collections.singletonList("localhost"));
        cbld.addAIAExt(Collections.singletonList(intCaRespURI));
        // Make our SSL Server Cert!
        X509Certificate sslCert = cbld.build(intCaCert, intCaKP.getPrivate(),
                "SHA256withRSA");
        log("SSL Certificate Created:\n" + certInfo(sslCert));

        // Provide SSL server cert revocation info to the Intermeidate CA
        // OCSP responder.
        revInfo = new HashMap<>();
        revInfo.put(sslCert.getSerialNumber(),
                new SimpleOCSPServer.CertStatusInfo(
                        SimpleOCSPServer.CertStatus.CERT_STATUS_GOOD));
        intOcsp.updateStatusDb(revInfo);

        // Now build a keystore and add the keys, chain and root cert as a TA
        serverKeystore = keyStoreBuilder.getKeyStore();
        Certificate[] sslChain = {sslCert, intCaCert, rootCert};
        serverKeystore.setKeyEntry(SSL_ALIAS, sslKP.getPrivate(),
                passwd.toCharArray(), sslChain);
        serverKeystore.setCertificateEntry(ROOT_ALIAS, rootCert);

        // And finally a Trust Store for the client
        trustStore = keyStoreBuilder.getKeyStore();
        trustStore.setCertificateEntry(ROOT_ALIAS, rootCert);
    }

    private static void addCommonExts(CertificateBuilder cbld,
            PublicKey subjKey, PublicKey authKey) throws IOException {
        cbld.addSubjectKeyIdExt(subjKey);
        cbld.addAuthorityKeyIdExt(authKey);
    }

    private static void addCommonCAExts(CertificateBuilder cbld)
            throws IOException {
        cbld.addBasicConstraintsExt(true, true, -1);
        // Set key usage bits for digitalSignature, keyCertSign and cRLSign
        boolean[] kuBitSettings = {true, false, false, false, false, true,
            true, false, false};
        cbld.addKeyUsageExt(kuBitSettings);
    }

    /**
     * Helper routine that dumps only a few cert fields rather than
     * the whole toString() output.
     *
     * @param cert an X509Certificate to be displayed
     *
     * @return the String output of the issuer, subject and
     * serial number
     */
    private static String certInfo(X509Certificate cert) {
        StringBuilder sb = new StringBuilder();
        sb.append("Issuer: ").append(cert.getIssuerX500Principal()).
                append("\n");
        sb.append("Subject: ").append(cert.getSubjectX500Principal()).
                append("\n");
        sb.append("Serial: ").append(cert.getSerialNumber()).append("\n");
        return sb.toString();
    }

    /**
     * Log a message on stdout
     *
     * @param message The message to log
     */
    private static void log(String message) {
        if (debug) {
            System.out.println(message);
        }
    }

    // The following two classes are Simple nested class to group a handful
    // of configuration parameters used before starting a client or server.
    // We'll just access the data members directly for convenience.
    static class ClientParameters {
        boolean enabled = true;
        PKIXBuilderParameters pkixParams = null;
        PKIXRevocationChecker revChecker = null;
        String[] protocols = null;
        String[] ciphers = null;

        ClientParameters() { }
    }

    static class ServerParameters {
        boolean enabled = true;
        int cacheSize = 256;
        int cacheLifetime = 3600;
        int respTimeout = 5000;
        String respUri = "";
        boolean respOverride = false;
        boolean ignoreExts = false;
        String[] protocols = null;
        String[] ciphers = null;

        ServerParameters() { }
    }

    static class CustomizedSocketFactory extends SSLSocketFactory {
        final SSLContext sslc;
        final String[] protocols;
        final String[] cipherSuites;

        CustomizedSocketFactory(SSLContext ctx, String[] prots, String[] suites)
                throws GeneralSecurityException {
            super();
            sslc = (ctx != null) ? ctx : SSLContext.getDefault();
            protocols = prots;
            cipherSuites = suites;

            // Create the Trust Manager Factory using the PKIX variant
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        }

        @Override
        public Socket createSocket(Socket s, String host, int port,
                boolean autoClose) throws IOException {
            Socket sock =  sslc.getSocketFactory().createSocket(s, host, port,
                    autoClose);
            customizeSocket(sock);
            return sock;
        }

        @Override
        public Socket createSocket(InetAddress host, int port)
                throws IOException {
            Socket sock = sslc.getSocketFactory().createSocket(host, port);
            customizeSocket(sock);
            return sock;
        }

        @Override
        public Socket createSocket(InetAddress host, int port,
                InetAddress localAddress, int localPort) throws IOException {
            Socket sock = sslc.getSocketFactory().createSocket(host, port,
                    localAddress, localPort);
            customizeSocket(sock);
            return sock;
        }

        @Override
        public Socket createSocket(String host, int port)
                throws IOException {
            Socket sock =  sslc.getSocketFactory().createSocket(host, port);
            customizeSocket(sock);
            return sock;
        }

        @Override
        public Socket createSocket(String host, int port,
                InetAddress localAddress, int localPort)
                throws IOException {
            Socket sock =  sslc.getSocketFactory().createSocket(host, port,
                    localAddress, localPort);
            customizeSocket(sock);
            return sock;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return sslc.getDefaultSSLParameters().getCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return sslc.getSupportedSSLParameters().getCipherSuites();
        }

        private void customizeSocket(Socket sock) {
            if (sock instanceof SSLSocket) {
                if (protocols != null) {
                    ((SSLSocket)sock).setEnabledProtocols(protocols);
                }
                if (cipherSuites != null) {
                    ((SSLSocket)sock).setEnabledCipherSuites(cipherSuites);
                }
            }
        }
    }

    static class CustomizedServerSocketFactory extends SSLServerSocketFactory {
        final SSLContext sslc;
        final String[] protocols;
        final String[] cipherSuites;

        CustomizedServerSocketFactory(SSLContext ctx, String[] prots, String[] suites)
                throws GeneralSecurityException {
            super();
            sslc = (ctx != null) ? ctx : SSLContext.getDefault();
            protocols = prots;
            cipherSuites = suites;

            // Create the Trust Manager Factory using the PKIX variant
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        }

        @Override
        public ServerSocket createServerSocket(int port) throws IOException {
            ServerSocket sock =
                    sslc.getServerSocketFactory().createServerSocket(port);
            customizeSocket(sock);
            return sock;
        }

        @Override
        public ServerSocket createServerSocket(int port, int backlog)
                throws IOException {
            ServerSocket sock =
                    sslc.getServerSocketFactory().createServerSocket(port,
                            backlog);
            customizeSocket(sock);
            return sock;
        }

        @Override
        public ServerSocket createServerSocket(int port, int backlog,
                InetAddress ifAddress) throws IOException {
            ServerSocket sock =
                    sslc.getServerSocketFactory().createServerSocket(port,
                            backlog, ifAddress);
            customizeSocket(sock);
            return sock;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return sslc.getDefaultSSLParameters().getCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return sslc.getSupportedSSLParameters().getCipherSuites();
        }

        private void customizeSocket(ServerSocket sock) {
            if (sock instanceof SSLServerSocket) {
                if (protocols != null) {
                    ((SSLServerSocket)sock).setEnabledProtocols(protocols);
                }
                if (cipherSuites != null) {
                    ((SSLServerSocket)sock).setEnabledCipherSuites(cipherSuites);
                }
            }
        }
    }


    static class TestResult {
        Exception serverExc = null;
        Exception clientExc = null;
    }

}
