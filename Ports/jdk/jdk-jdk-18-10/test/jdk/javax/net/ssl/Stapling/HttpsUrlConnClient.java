/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @run main/othervm HttpsUrlConnClient
 */

import java.io.*;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.net.Socket;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.Security;
import java.security.GeneralSecurityException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.Certificate;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.cert.PKIXRevocationChecker;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import sun.security.testlibrary.SimpleOCSPServer;
import sun.security.testlibrary.CertificateBuilder;
import sun.security.validator.ValidatorException;

public class HttpsUrlConnClient {

    /*
     * =============================================================
     * Set the various variables needed for the tests, then
     * specify what tests to run on each side.
     */

    static final byte[] LINESEP = { 10 };
    static final Base64.Encoder B64E = Base64.getMimeEncoder(64, LINESEP);

    // Turn on TLS debugging
    static boolean debug = true;

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

    private static final String SIMPLE_WEB_PAGE = "<HTML>\n" +
            "<HEAD><Title>Web Page!</Title></HEAD>\n" +
            "<BODY><H1>Web Page!</H1></BODY>\n</HTML>";
    private static final SimpleDateFormat utcDateFmt =
            new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
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

        System.setProperty("javax.net.ssl.keyStore", "");
        System.setProperty("javax.net.ssl.keyStorePassword", "");
        System.setProperty("javax.net.ssl.trustStore", "");
        System.setProperty("javax.net.ssl.trustStorePassword", "");

        // Create the PKI we will use for the test and start the OCSP servers
        createPKI();
        utcDateFmt.setTimeZone(TimeZone.getTimeZone("GMT"));

        testPKIXParametersRevEnabled(TLS12MAX);
        testPKIXParametersRevEnabled(TLS13ONLY);

        // shut down the OCSP responders before finishing the test
        intOcsp.stop();
        rootOcsp.stop();
    }

    /**
     * Do a basic connection using PKIXParameters with revocation checking
     * enabled and client-side OCSP disabled.  It will only pass if all
     * stapled responses are present, valid and have a GOOD status.
     */
    static void testPKIXParametersRevEnabled(String[] allowedProts)
            throws Exception {
        ClientParameters cliParams = new ClientParameters();
        cliParams.protocols = allowedProts;
        ServerParameters servParams = new ServerParameters();
        serverReady = false;

        System.out.println("=====================================");
        System.out.println("Stapling enabled, PKIXParameters with");
        System.out.println("Revocation checking enabled ");
        System.out.println("=====================================");

        // Set the certificate entry in the intermediate OCSP responder
        // with a revocation date of 8 hours ago.
        X509Certificate sslCert =
                (X509Certificate)serverKeystore.getCertificate(SSL_ALIAS);
        Map<BigInteger, SimpleOCSPServer.CertStatusInfo> revInfo =
            new HashMap<>();
        revInfo.put(sslCert.getSerialNumber(),
                new SimpleOCSPServer.CertStatusInfo(
                        SimpleOCSPServer.CertStatus.CERT_STATUS_REVOKED,
                        new Date(System.currentTimeMillis() -
                                TimeUnit.HOURS.toMillis(8))));
        intOcsp.updateStatusDb(revInfo);

        // Set up revocation checking on the client with no client-side
        // OCSP fall-back
        cliParams.pkixParams = new PKIXBuilderParameters(trustStore,
                new X509CertSelector());
        cliParams.pkixParams.setRevocationEnabled(true);
        Security.setProperty("ocsp.enable", "false");

        HttpsUrlConnClient sslTest = new HttpsUrlConnClient(cliParams,
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

        // In this case the server should also have thrown an exception
        // because of the client alert
        if (tr.serverExc instanceof SSLHandshakeException) {
            if (!tr.serverExc.getMessage().contains(
                    "bad_certificate_status_response")) {
                throw tr.serverExc;
            }
        }

        System.out.println("                PASS");
        System.out.println("=====================================\n");
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

        SSLServerSocketFactory sslssf = sslc.getServerSocketFactory();
        SSLServerSocket sslServerSocket =
            (SSLServerSocket) sslssf.createServerSocket(serverPort);

        serverPort = sslServerSocket.getLocalPort();
        log("Server Port is " + serverPort);

        // Dump the private key in PKCS8 format, not encrypted.  This
        // key dump can be used if the traffic was captured using tcpdump
        // or wireshark to look into the encrypted packets for debug purposes.
        if (debug) {
            byte[] keybytes = serverKeystore.getKey(SSL_ALIAS,
                    passwd.toCharArray()).getEncoded();
            PKCS8EncodedKeySpec p8spec = new PKCS8EncodedKeySpec(keybytes);
            StringBuilder keyPem = new StringBuilder();
            keyPem.append("-----BEGIN PRIVATE KEY-----\n");
            keyPem.append(B64E.encodeToString(p8spec.getEncoded())).append("\n");
            keyPem.append("-----END PRIVATE KEY-----\n");
            log("Private key is:\n" + keyPem.toString());
        }

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady = true;

        try (SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(sslSocket.getInputStream()));
                OutputStream out = sslSocket.getOutputStream()) {
            StringBuilder hdrBldr = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                hdrBldr.append(line).append("\n");
            }
            String headerText = hdrBldr.toString();
            log("Header Received: " + headerText.length() + " bytes\n" +
                    headerText);

            StringBuilder sb = new StringBuilder();
            sb.append("HTTP/1.0 200 OK\r\n");
            sb.append("Date: ").append(utcDateFmt.format(new Date())).
                    append("\r\n");
            sb.append("Content-Type: text/html\r\n");
            sb.append("Content-Length: ").append(SIMPLE_WEB_PAGE.length());
            sb.append("\r\n\r\n");
            out.write(sb.toString().getBytes("UTF-8"));
            out.write(SIMPLE_WEB_PAGE.getBytes("UTF-8"));
            out.flush();
            log("Server replied with:\n" + sb.toString() + SIMPLE_WEB_PAGE);
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

        HtucSSLSocketFactory sockFac = new HtucSSLSocketFactory(cliParams);
        HttpsURLConnection.setDefaultSSLSocketFactory(sockFac);
        URL location = new URL("https://localhost:" + serverPort);
        HttpsURLConnection tlsConn =
                (HttpsURLConnection)location.openConnection();
        tlsConn.setConnectTimeout(5000);
        tlsConn.setReadTimeout(5000);
        tlsConn.setDoInput(true);

        try (InputStream in = tlsConn.getInputStream()) {
            // Check the response
            if (debug && tlsConn.getResponseCode() !=
                    HttpURLConnection.HTTP_OK) {
                log("Received HTTP error: " + tlsConn.getResponseCode() +
                        " - " + tlsConn.getResponseMessage());
                throw new IOException("HTTP error: " +
                        tlsConn.getResponseCode());
            }

            int contentLength = tlsConn.getContentLength();
            if (contentLength == -1) {
                contentLength = Integer.MAX_VALUE;
            }
            byte[] response = new byte[contentLength > 2048 ? 2048 :
                contentLength];
            int total = 0;
            while (total < contentLength) {
                int count = in.read(response, total, response.length - total);
                if (count < 0)
                    break;

                total += count;
                log("Read " + count + " bytes (" + total + " total)");
                if (total >= response.length && total < contentLength) {
                    response = Arrays.copyOf(response, total * 2);
                }
            }
            response = Arrays.copyOf(response, total);
            String webPage = new String(response, 0, total);
            if (debug) {
                log("Web page:\n" + webPage);
            }
        }
    }

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    HttpsUrlConnClient(ClientParameters cliParams,
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
     * encapsulates a CertPathValidatorException at some point in the
     * exception stack.
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

        // Locate the CertPathValidatorException.  If one
        // Does not exist, then it's an automatic failure of
        // the test.
        Throwable curExc = e;
        CertPathValidatorException cpve = null;
        while (curExc != null) {
            if (curExc instanceof CertPathValidatorException) {
                cpve = (CertPathValidatorException)curExc;
            }
            curExc = curExc.getCause();
        }

        // If we get through the loop and cpve is null then we
        // we didn't find CPVE and this is a failure
        if (cpve != null) {
            if (cpve.getReason() == reason) {
                result = true;
            } else {
                System.out.println("CPVE Reason Mismatch: Expected = " +
                        reason + ", Actual = " + cpve.getReason());
            }
        } else {
            System.out.println("Failed to find an expected CPVE");
        }

        return result;
    }

    TestResult getResult() {
        TestResult tr = new TestResult();
        tr.clientExc = clientException;
        tr.serverExc = serverException;
        return tr;
    }

    final void startServer(ServerParameters servParams, boolean newThread)
            throws Exception {
        if (newThread) {
            serverThread = new Thread() {
                @Override
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

    final void startClient(ClientParameters cliParams, boolean newThread)
            throws Exception {
        if (newThread) {
            clientThread = new Thread() {
                @Override
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
        String[] cipherSuites = null;

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

        ServerParameters() { }
    }

    static class TestResult {
        Exception serverExc = null;
        Exception clientExc = null;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Test Result:\n").
                append("\tServer Exc = ").append(serverExc).append("\n").
                append("\tClient Exc = ").append(clientExc).append("\n");
            return sb.toString();
        }
    }

    static class HtucSSLSocketFactory extends SSLSocketFactory {
        ClientParameters params;
        SSLContext sslc = SSLContext.getInstance("TLS");

        HtucSSLSocketFactory(ClientParameters cliParams)
                throws GeneralSecurityException {
            super();

            // Create the Trust Manager Factory using the PKIX variant
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");

            // If we have a customized pkixParameters then use it
            if (cliParams.pkixParams != null) {
                // LIf we have a customized PKIXRevocationChecker, add
                // it to the PKIXBuilderParameters.
                if (cliParams.revChecker != null) {
                    cliParams.pkixParams.addCertPathChecker(
                            cliParams.revChecker);
                }

                ManagerFactoryParameters trustParams =
                        new CertPathTrustManagerParameters(
                                cliParams.pkixParams);
                tmf.init(trustParams);
            } else {
                tmf.init(trustStore);
            }

            sslc.init(null, tmf.getTrustManagers(), null);
            params = cliParams;
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
                SSLSocket sslSock = (SSLSocket)sock;
                if (params.protocols != null) {
                    sslSock.setEnabledProtocols(params.protocols);
                }
                if (params.cipherSuites != null) {
                    sslSock.setEnabledCipherSuites(params.cipherSuites);
                }
            }
        }
    }

}
