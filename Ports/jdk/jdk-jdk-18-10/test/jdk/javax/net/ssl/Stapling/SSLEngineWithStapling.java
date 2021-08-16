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
 * @run main/othervm SSLEngineWithStapling
 */

/**
 * A SSLEngine usage example which simplifies the presentation
 * by removing the I/O and multi-threading concerns.
 *
 * The test creates two SSLEngines, simulating a client and server.
 * The "transport" layer consists two byte buffers:  think of them
 * as directly connected pipes.
 *
 * Note, this is a *very* simple example: real code will be much more
 * involved.  For example, different threading and I/O models could be
 * used, transport mechanisms could close unexpectedly, and so on.
 *
 * When this application runs, notice that several messages
 * (wrap/unwrap) pass before any application data is consumed or
 * produced.  (For more information, please see the SSL/TLS
 * specifications.)  There may several steps for a successful handshake,
 * so it's typical to see the following series of operations:
 *
 *      client          server          message
 *      ======          ======          =======
 *      wrap()          ...             ClientHello
 *      ...             unwrap()        ClientHello
 *      ...             wrap()          ServerHello/Certificate
 *      unwrap()        ...             ServerHello/Certificate
 *      wrap()          ...             ClientKeyExchange
 *      wrap()          ...             ChangeCipherSpec
 *      wrap()          ...             Finished
 *      ...             unwrap()        ClientKeyExchange
 *      ...             unwrap()        ChangeCipherSpec
 *      ...             unwrap()        Finished
 *      ...             wrap()          ChangeCipherSpec
 *      ...             wrap()          Finished
 *      unwrap()        ...             ChangeCipherSpec
 *      unwrap()        ...             Finished
 */

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.nio.*;
import java.security.cert.CertPathValidatorException;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509Certificate;
import java.security.cert.X509CertSelector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import sun.security.testlibrary.SimpleOCSPServer;
import sun.security.testlibrary.CertificateBuilder;

public class SSLEngineWithStapling {

    /*
     * Enables logging of the SSLEngine operations.
     */
    private static final boolean logging = true;

    /*
     * Enables the JSSE system debugging system property:
     *
     *     -Djavax.net.debug=all
     *
     * This gives a lot of low-level information about operations underway,
     * including specific handshake messages, and might be best examined
     * after gaining some familiarity with this application.
     */
    private static final boolean debug = true;

    private SSLEngine clientEngine;     // client Engine
    private ByteBuffer clientOut;       // write side of clientEngine
    private ByteBuffer clientIn;        // read side of clientEngine

    private SSLEngine serverEngine;     // server Engine
    private ByteBuffer serverOut;       // write side of serverEngine
    private ByteBuffer serverIn;        // read side of serverEngine

    /*
     * For data transport, this example uses local ByteBuffers.  This
     * isn't really useful, but the purpose of this example is to show
     * SSLEngine concepts, not how to do network transport.
     */
    private ByteBuffer cTOs;            // "reliable" transport client->server
    private ByteBuffer sTOc;            // "reliable" transport server->client

    /*
     * The following is to set up the keystores.
     */
    static final String passwd = "passphrase";
    static final String ROOT_ALIAS = "root";
    static final String INT_ALIAS = "intermediate";
    static final String SSL_ALIAS = "ssl";

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
     * Main entry point for this test.
     */
    public static void main(String args[]) throws Exception {
        if (debug) {
            System.setProperty("javax.net.debug", "ssl:handshake");
        }

        // Create the PKI we will use for the test and start the OCSP servers
        createPKI();

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

        // Create a list of TLS protocol configurations we can use to
        // drive tests with different handshaking models.
        List<String[]> allowedProtList = List.of(TLS12MAX, TLS13ONLY);

        for (String[] protocols : allowedProtList) {
            SSLEngineWithStapling test = new SSLEngineWithStapling();
            try {
                test.runTest(protocols);
                throw new RuntimeException("Expected failure due to " +
                        "revocation did not occur");
            } catch (Exception e) {
                if (!checkClientValidationFailure(e,
                        CertPathValidatorException.BasicReason.REVOKED)) {
                    System.out.println(
                            "*** Didn't find the exception we wanted");
                    throw e;
                }
            }
        }

        System.out.println("Test Passed.");
    }

    /*
     * Create an initialized SSLContext to use for these tests.
     */
    public SSLEngineWithStapling() throws Exception {
        System.setProperty("javax.net.ssl.keyStore", "");
        System.setProperty("javax.net.ssl.keyStorePassword", "");
        System.setProperty("javax.net.ssl.trustStore", "");
        System.setProperty("javax.net.ssl.trustStorePassword", "");

        // Enable OCSP Stapling on both client and server sides, but turn off
        // client-side OCSP for revocation checking.  This ensures that the
        // revocation information from the test has to come via stapling.
        System.setProperty("jdk.tls.client.enableStatusRequestExtension",
                Boolean.toString(true));
        System.setProperty("jdk.tls.server.enableStatusRequestExtension",
                Boolean.toString(true));
        Security.setProperty("ocsp.enable", "false");
    }

    /*
     * Run the test.
     *
     * Sit in a tight loop, both engines calling wrap/unwrap regardless
     * of whether data is available or not.  We do this until both engines
     * report back they are closed.
     *
     * The main loop handles all of the I/O phases of the SSLEngine's
     * lifetime:
     *
     *     initial handshaking
     *     application data transfer
     *     engine closing
     *
     * One could easily separate these phases into separate
     * sections of code.
     */
    private void runTest(String[] protocols) throws Exception {
        boolean dataDone = false;

        createSSLEngines(protocols);
        createBuffers();

        SSLEngineResult clientResult;   // results from client's last operation
        SSLEngineResult serverResult;   // results from server's last operation

        /*
         * Examining the SSLEngineResults could be much more involved,
         * and may alter the overall flow of the application.
         *
         * For example, if we received a BUFFER_OVERFLOW when trying
         * to write to the output pipe, we could reallocate a larger
         * pipe, but instead we wait for the peer to drain it.
         */
        while (!isEngineClosed(clientEngine) ||
                !isEngineClosed(serverEngine)) {

            log("================");

            clientResult = clientEngine.wrap(clientOut, cTOs);
            log("client wrap: ", clientResult);
            runDelegatedTasks(clientResult, clientEngine);

            serverResult = serverEngine.wrap(serverOut, sTOc);
            log("server wrap: ", serverResult);
            runDelegatedTasks(serverResult, serverEngine);

            cTOs.flip();
            sTOc.flip();

            log("----");

            clientResult = clientEngine.unwrap(sTOc, clientIn);
            log("client unwrap: ", clientResult);
            runDelegatedTasks(clientResult, clientEngine);

            serverResult = serverEngine.unwrap(cTOs, serverIn);
            log("server unwrap: ", serverResult);
            runDelegatedTasks(serverResult, serverEngine);

            cTOs.compact();
            sTOc.compact();

            /*
             * After we've transfered all application data between the client
             * and server, we close the clientEngine's outbound stream.
             * This generates a close_notify handshake message, which the
             * server engine receives and responds by closing itself.
             */
            if (!dataDone && (clientOut.limit() == serverIn.position()) &&
                    (serverOut.limit() == clientIn.position())) {

                /*
                 * A sanity check to ensure we got what was sent.
                 */
                checkTransfer(serverOut, clientIn);
                checkTransfer(clientOut, serverIn);

                log("\tClosing clientEngine's *OUTBOUND*...");
                clientEngine.closeOutbound();
                dataDone = true;
            }
        }
    }

    /*
     * Using the SSLContext created during object creation,
     * create/configure the SSLEngines we'll use for this test.
     */
    private void createSSLEngines(String[] protocols) throws Exception {
        // Initialize the KeyManager and TrustManager for the server
        KeyManagerFactory servKmf = KeyManagerFactory.getInstance("PKIX");
        servKmf.init(serverKeystore, passwd.toCharArray());
        TrustManagerFactory servTmf =
                TrustManagerFactory.getInstance("PKIX");
        servTmf.init(trustStore);

        // Initialize the TrustManager for the client with revocation checking
        PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustStore,
                new X509CertSelector());
        pkixParams.setRevocationEnabled(true);
        ManagerFactoryParameters mfp =
                new CertPathTrustManagerParameters(pkixParams);
        TrustManagerFactory cliTmf =
                TrustManagerFactory.getInstance("PKIX");
        cliTmf.init(mfp);

        // Create the SSLContexts from the factories
        SSLContext servCtx = SSLContext.getInstance("TLS");
        servCtx.init(servKmf.getKeyManagers(), servTmf.getTrustManagers(),
                null);
        SSLContext cliCtx = SSLContext.getInstance("TLS");
        cliCtx.init(null, cliTmf.getTrustManagers(), null);


        /*
         * Configure the serverEngine to act as a server in the SSL/TLS
         * handshake.
         */
        serverEngine = servCtx.createSSLEngine();
        serverEngine.setEnabledProtocols(protocols);
        serverEngine.setUseClientMode(false);
        serverEngine.setNeedClientAuth(false);

        /*
         * Similar to above, but using client mode instead.
         */
        clientEngine = cliCtx.createSSLEngine("client", 80);
        clientEngine.setEnabledProtocols(protocols);
        clientEngine.setUseClientMode(true);
    }

    /*
     * Create and size the buffers appropriately.
     */
    private void createBuffers() {

        /*
         * We'll assume the buffer sizes are the same
         * between client and server.
         */
        SSLSession session = clientEngine.getSession();
        int appBufferMax = session.getApplicationBufferSize();
        int netBufferMax = session.getPacketBufferSize();

        /*
         * We'll make the input buffers a bit bigger than the max needed
         * size, so that unwrap()s following a successful data transfer
         * won't generate BUFFER_OVERFLOWS.
         *
         * We'll use a mix of direct and indirect ByteBuffers for
         * tutorial purposes only.  In reality, only use direct
         * ByteBuffers when they give a clear performance enhancement.
         */
        clientIn = ByteBuffer.allocate(appBufferMax + 50);
        serverIn = ByteBuffer.allocate(appBufferMax + 50);

        cTOs = ByteBuffer.allocateDirect(netBufferMax);
        sTOc = ByteBuffer.allocateDirect(netBufferMax);

        clientOut = ByteBuffer.wrap("Hi Server, I'm Client".getBytes());
        serverOut = ByteBuffer.wrap("Hello Client, I'm Server".getBytes());
    }

    /*
     * If the result indicates that we have outstanding tasks to do,
     * go ahead and run them in this thread.
     */
    private static void runDelegatedTasks(SSLEngineResult result,
            SSLEngine engine) throws Exception {

        if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = engine.getDelegatedTask()) != null) {
                log("\trunning delegated task...");
                runnable.run();
            }
            HandshakeStatus hsStatus = engine.getHandshakeStatus();
            if (hsStatus == HandshakeStatus.NEED_TASK) {
                throw new Exception(
                    "handshake shouldn't need additional tasks");
            }
            log("\tnew HandshakeStatus: " + hsStatus);
        }
    }

    private static boolean isEngineClosed(SSLEngine engine) {
        return (engine.isOutboundDone() && engine.isInboundDone());
    }

    /*
     * Simple check to make sure everything came across as expected.
     */
    private static void checkTransfer(ByteBuffer a, ByteBuffer b)
            throws Exception {
        a.flip();
        b.flip();

        if (!a.equals(b)) {
            throw new Exception("Data didn't transfer cleanly");
        } else {
            log("\tData transferred cleanly");
        }

        a.position(a.limit());
        b.position(b.limit());
        a.limit(a.capacity());
        b.limit(b.capacity());
    }

    /*
     * Logging code
     */
    private static boolean resultOnce = true;

    private static void log(String str, SSLEngineResult result) {
        if (!logging) {
            return;
        }
        if (resultOnce) {
            resultOnce = false;
            System.out.println("The format of the SSLEngineResult is: \n" +
                "\t\"getStatus() / getHandshakeStatus()\" +\n" +
                "\t\"bytesConsumed() / bytesProduced()\"\n");
        }
        HandshakeStatus hsStatus = result.getHandshakeStatus();
        log(str +
            result.getStatus() + "/" + hsStatus + ", " +
            result.bytesConsumed() + "/" + result.bytesProduced() +
            " bytes");
        if (hsStatus == HandshakeStatus.FINISHED) {
            log("\t...ready for application data");
        }
    }

    private static void log(String str) {
        if (logging) {
            System.out.println(str);
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
        java.security.cert.Certificate[] rootChain = {rootCert};
        rootKeystore.setKeyEntry(ROOT_ALIAS, rootCaKP.getPrivate(),
                passwd.toCharArray(), rootChain);

        // Now fire up the OCSP responder
        rootOcsp = new SimpleOCSPServer(rootKeystore, passwd, ROOT_ALIAS, null);
        rootOcsp.enableLog(logging);
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
        java.security.cert.Certificate[] intChain = {intCaCert, rootCert};
        intKeystore.setKeyEntry(INT_ALIAS, intCaKP.getPrivate(),
                passwd.toCharArray(), intChain);
        intKeystore.setCertificateEntry(ROOT_ALIAS, rootCert);

        // Now fire up the Intermediate CA OCSP responder
        intOcsp = new SimpleOCSPServer(intKeystore, passwd,
                INT_ALIAS, null);
        intOcsp.enableLog(logging);
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
        java.security.cert.Certificate[] sslChain = {sslCert, intCaCert, rootCert};
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
            CertPathValidatorException.BasicReason reason) {
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
}
