/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8051498 8145849 8170282
 * @summary JEP 244: TLS Application-Layer Protocol Negotiation Extension
 * @compile MyX509ExtendedKeyManager.java
 *
 * @run main/othervm SSLEngineAlpnTest h2          UNUSED   h2          h2
 * @run main/othervm SSLEngineAlpnTest h2          UNUSED   h2,http/1.1 h2
 * @run main/othervm SSLEngineAlpnTest h2,http/1.1 UNUSED   h2,http/1.1 h2
 * @run main/othervm SSLEngineAlpnTest http/1.1,h2 UNUSED   h2,http/1.1 http/1.1
 * @run main/othervm SSLEngineAlpnTest h4,h3,h2    UNUSED   h1,h2       h2
 * @run main/othervm SSLEngineAlpnTest EMPTY       UNUSED   h2,http/1.1 NONE
 * @run main/othervm SSLEngineAlpnTest h2          UNUSED   EMPTY       NONE
 * @run main/othervm SSLEngineAlpnTest H2          UNUSED   h2          ERROR
 * @run main/othervm SSLEngineAlpnTest h2          UNUSED   http/1.1    ERROR
 *
 * @run main/othervm SSLEngineAlpnTest UNUSED      h2       h2          h2
 * @run main/othervm SSLEngineAlpnTest UNUSED      h2       h2,http/1.1 h2
 * @run main/othervm SSLEngineAlpnTest UNUSED      h2       http/1.1,h2 h2
 * @run main/othervm SSLEngineAlpnTest UNUSED      http/1.1 h2,http/1.1 http/1.1
 * @run main/othervm SSLEngineAlpnTest UNUSED      EMPTY    h2,http/1.1 NONE
 * @run main/othervm SSLEngineAlpnTest UNUSED      h2       EMPTY       NONE
 * @run main/othervm SSLEngineAlpnTest UNUSED      H2       h2          ERROR
 * @run main/othervm SSLEngineAlpnTest UNUSED      h2       http/1.1    ERROR
 *
 * @run main/othervm SSLEngineAlpnTest h2          h2       h2          h2
 * @run main/othervm SSLEngineAlpnTest H2          h2       h2,http/1.1 h2
 * @run main/othervm SSLEngineAlpnTest h2,http/1.1 http/1.1 h2,http/1.1 http/1.1
 * @run main/othervm SSLEngineAlpnTest http/1.1,h2 h2       h2,http/1.1 h2
 * @run main/othervm SSLEngineAlpnTest EMPTY       h2       h2          h2
 * @run main/othervm SSLEngineAlpnTest h2,http/1.1 EMPTY    http/1.1    NONE
 * @run main/othervm SSLEngineAlpnTest h2,http/1.1 h2       EMPTY       NONE
 * @run main/othervm SSLEngineAlpnTest UNUSED      UNUSED   http/1.1,h2 NONE
 * @run main/othervm SSLEngineAlpnTest h2          h2       http/1.1    ERROR
 * @run main/othervm SSLEngineAlpnTest h2,http/1.1 H2       http/1.1    ERROR
 */
/**
 * A simple SSLEngine-based client/server that demonstrates the proposed API
 * changes for JEP 244 in support of the TLS ALPN extension (RFC 7301).
 *
 * Usage:
 *     java SSLEngineAlpnTest <server-APs> <callback-AP> <client-APs> <result>
 *
 * where:
 *      EMPTY  indicates that ALPN is disabled
 *      UNUSED indicates that no ALPN values are supplied (server-side only)
 *      ERROR  indicates that an exception is expected
 *      NONE   indicates that no ALPN is expected
 *
 * This example is based on our standard SSLEngineTemplate.
 *
 * The immediate consumer of ALPN will be HTTP/2 (RFC 7540), aka H2. The H2 IETF
 * Working Group wanted to use TLSv1.3+ as the secure transport mechanism, but
 * TLSv1.3 wasn't ready. The H2 folk agreed to a compromise that only TLSv1.2+
 * can be used, and that if TLSv1.2 was selected, non-TLSv.1.3-approved
 * ciphersuites would be blacklisted and their use discouraged.
 *
 * In order to support connections that might negotiate either HTTP/1.1 and H2,
 * the guidance from the IETF Working Group is that the H2 ciphersuites be
 * prioritized/tried first.
 */

/*
 * The original SSLEngineTemplate comments follow.
 *
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
import java.security.*;
import java.nio.*;
import java.util.Arrays;

public class SSLEngineAlpnTest {

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
    private static final boolean debug = false;

    private static boolean hasServerAPs; // whether server APs are present
    private static boolean hasCallback; // whether a callback is present

    private final SSLContext sslc;

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
    private static final String pathToStores = "../etc";
    private static final String keyStoreFile = "keystore";
    private static final String trustStoreFile = "truststore";
    private static final String passwd = "passphrase";

    private static final String keyFilename
            = System.getProperty("test.src", ".") + "/" + pathToStores
            + "/" + keyStoreFile;
    private static final String trustFilename
            = System.getProperty("test.src", ".") + "/" + pathToStores
            + "/" + trustStoreFile;

    /*
     * Main entry point for this test.
     */
    public static void main(String args[]) throws Exception {
        if (debug) {
            System.setProperty("javax.net.debug", "all");
        }
        System.setProperty("jdk.tls.acknowledgeCloseNotify", "true");
        System.out.println("Test args: " + Arrays.toString(args));

        // Validate parameters
        if (args.length != 4) {
            throw new Exception("Invalid number of test parameters");
        }

        hasServerAPs = !args[0].equals("UNUSED"); // are server APs being used?
        hasCallback = !args[1].equals("UNUSED"); // is callback being used?

        SSLEngineAlpnTest test = new SSLEngineAlpnTest(args[3]);
        try {
            test.runTest(convert(args[0]), args[1], convert(args[2]), args[3]);
        } catch (SSLHandshakeException she) {
            if (args[3].equals("ERROR")) {
                System.out.println("Caught the expected exception: " + she);
            } else {
                throw she;
            }
        }

        System.out.println("Test Passed.");
    }

    /*
     * Create an initialized SSLContext to use for these tests.
     */
    public SSLEngineAlpnTest(String expectedAP) throws Exception {

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");

        char[] passphrase = "passphrase".toCharArray();

        ks.load(new FileInputStream(keyFilename), passphrase);
        ts.load(new FileInputStream(trustFilename), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        KeyManager [] kms = kmf.getKeyManagers();
        if (!(kms[0] instanceof X509ExtendedKeyManager)) {
            throw new Exception("kms[0] not X509ExtendedKeyManager");
        }

        kms = new KeyManager[] { new MyX509ExtendedKeyManager(
                (X509ExtendedKeyManager) kms[0], expectedAP,
                !hasCallback && hasServerAPs) };

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);

        SSLContext sslCtx = SSLContext.getInstance("TLS");

        sslCtx.init(kms, tmf.getTrustManagers(), null);

        sslc = sslCtx;
    }

    /*
     * Convert a comma-separated list into an array of strings.
     */
    private static String[] convert(String list) {
        if (list.equals("UNUSED")) {
            return null;
        }

        if (list.equals("EMPTY")) {
            return new String[0];
        }

        String[] strings;
        if (list.indexOf(',') > 0) {
            strings = list.split(",");
        } else {
            strings = new String[]{ list };
        }

        return strings;
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
    private void runTest(String[] serverAPs, String callbackAP,
            String[] clientAPs, String expectedAP) throws Exception {

        boolean dataDone = false;

        createSSLEngines(serverAPs, callbackAP, clientAPs);
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
        while (!isEngineClosed(clientEngine)
                || !isEngineClosed(serverEngine)) {

            log("================");

            clientResult = clientEngine.wrap(clientOut, cTOs);
            log("client wrap: ", clientResult);
            runDelegatedTasks(clientResult, clientEngine);
            checkAPResult(clientEngine, clientResult, expectedAP);

            serverResult = serverEngine.wrap(serverOut, sTOc);
            log("server wrap: ", serverResult);
            runDelegatedTasks(serverResult, serverEngine);
            checkAPResult(serverEngine, serverResult, expectedAP);

            cTOs.flip();
            sTOc.flip();

            log("----");

            clientResult = clientEngine.unwrap(sTOc, clientIn);
            log("client unwrap: ", clientResult);
            runDelegatedTasks(clientResult, clientEngine);
            checkAPResult(clientEngine, clientResult, expectedAP);

            serverResult = serverEngine.unwrap(cTOs, serverIn);
            log("server unwrap: ", serverResult);
            runDelegatedTasks(serverResult, serverEngine);
            checkAPResult(serverEngine, serverResult, expectedAP);

            cTOs.compact();
            sTOc.compact();

            /*
             * After we've transfered all application data between the client
             * and server, we close the clientEngine's outbound stream.
             * This generates a close_notify handshake message, which the
             * server engine receives and responds by closing itself.
             */
            if (!dataDone && (clientOut.limit() == serverIn.position())
                    && (serverOut.limit() == clientIn.position())) {

                /*
                 * A sanity check to ensure we got what was sent.
                 */
                checkTransfer(serverOut, clientIn);
                checkTransfer(clientOut, serverIn);

                log("\tClosing clientEngine's *OUTBOUND*...");
                clientEngine.closeOutbound();
                // serverEngine.closeOutbound();
                dataDone = true;
            }
        }
    }

    /*
     * Check that the resulting connection meets our defined ALPN
     * criteria.  If we were connecting to a non-JSSE implementation,
     * the server might have negotiated something we shouldn't accept.
     *
     * If we were expecting an ALPN value from server, let's make sure
     * the conditions match.
     */
    private static void checkAPResult(SSLEngine engine, SSLEngineResult result,
            String expectedAP) throws Exception {

        if (result.getHandshakeStatus() != HandshakeStatus.FINISHED) {
            return;
        }

        if (engine.getHandshakeApplicationProtocol() != null) {
            throw new Exception ("getHandshakeApplicationProtocol() should "
                    + "return null after the handshake is completed");
        }

        String ap = engine.getApplicationProtocol();
        System.out.println("Application Protocol: \"" + ap + "\"");

        if (ap == null) {
            throw new Exception(
                "Handshake was completed but null was received");
        }
        if (expectedAP.equals("NONE")) {
            if (!ap.isEmpty()) {
                throw new Exception("Expected no ALPN value");
            } else {
                System.out.println("No ALPN value negotiated, as expected");
            }
        } else if (!expectedAP.equals(ap)) {
            throw new Exception(expectedAP +
                " ALPN value not available on negotiated connection");
        }
    }

    /*
     * Using the SSLContext created during object creation,
     * create/configure the SSLEngines we'll use for this test.
     */
    private void createSSLEngines(String[] serverAPs, String callbackAP,
            String[] clientAPs) throws Exception {
        /*
         * Configure the serverEngine to act as a server in the SSL/TLS
         * handshake.  Also, require SSL client authentication.
         */
        serverEngine = sslc.createSSLEngine();
        serverEngine.setUseClientMode(false);

        SSLParameters sslp = serverEngine.getSSLParameters();

        sslp.setNeedClientAuth(true);

        /*
         * The default ciphersuite ordering from the SSLContext may not
         * reflect "h2" ciphersuites as being preferred, additionally the
         * client may not send them in an appropriate order. We could resort
         * the suite list if so desired.
         */
        String[] suites = sslp.getCipherSuites();
        sslp.setCipherSuites(suites);
        if (serverAPs != null) {
            sslp.setApplicationProtocols(serverAPs);
        }
        sslp.setUseCipherSuitesOrder(true);  // Set server side order

        serverEngine.setSSLParameters(sslp);

        // check that no callback has been registered
        if (serverEngine.getHandshakeApplicationProtocolSelector() != null) {
            throw new Exception("getHandshakeApplicationProtocolSelector() " +
                "should return null");
        }

        if (hasCallback) {
            serverEngine.setHandshakeApplicationProtocolSelector(
                (sslEngine, clientProtocols) -> {
                    return callbackAP.equals("EMPTY") ? "" : callbackAP;
                });

            // check that the callback can be retrieved
            if (serverEngine.getHandshakeApplicationProtocolSelector()
                    == null) {
                throw new Exception("getHandshakeApplicationProtocolSelector()"
                    + " should return non-null");
            }
        }

        /*
         * Similar to above, but using client mode instead.
         */
        clientEngine = sslc.createSSLEngine("client", 80);
        clientEngine.setUseClientMode(true);
        sslp = clientEngine.getSSLParameters();
        if (clientAPs != null) {
            sslp.setApplicationProtocols(clientAPs);
        }
        clientEngine.setSSLParameters(sslp);

        if ((clientEngine.getHandshakeApplicationProtocol() != null) ||
                (serverEngine.getHandshakeApplicationProtocol() != null)) {
            throw new Exception ("getHandshakeApplicationProtocol() should "
                    + "return null before the handshake starts");
        }
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
            System.out.println("The format of the SSLEngineResult is: \n"
                    + "\t\"getStatus() / getHandshakeStatus()\" +\n"
                    + "\t\"bytesConsumed() / bytesProduced()\"\n");
        }
        HandshakeStatus hsStatus = result.getHandshakeStatus();
        log(str
                + result.getStatus() + "/" + hsStatus + ", "
                + result.bytesConsumed() + "/" + result.bytesProduced()
                + " bytes");
        if (hsStatus == HandshakeStatus.FINISHED) {
            log("\t...ready for application data");
        }
    }

    private static void log(String str) {
        if (logging) {
            System.out.println(str);
        }
    }
}
