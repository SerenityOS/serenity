/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6207322
 * @summary SSLEngine is returning a premature FINISHED message when doing
 *     an abbreviated handshake.
 * @run main/othervm RehandshakeFinished
 * @author Brad Wetmore
 */

/*
 * This test may need some updating if the messages change order.
 * Currently I'm expecting that there is a simple renegotiation, with
 * each message being contained in a single SSL packet.
 *
 *      ClientHello
 *                              Server Hello
 *                              CCS
 *                              FINISHED
 *      CCS
 *      FINISHED
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
import java.security.*;
import java.nio.*;

public class RehandshakeFinished {

    /*
     * Enables logging of the SSLEngine operations.
     */
    private static boolean logging = true;

    /*
     * Enables the JSSE system debugging system property:
     *
     *     -Djavax.net.debug=all
     *
     * This gives a lot of low-level information about operations underway,
     * including specific handshake messages, and might be best examined
     * after gaining some familiarity with this application.
     */
    private static boolean debug = false;

    static private SSLContext sslc;

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
    private static String pathToStores = "../../../../javax/net/ssl/etc";
    private static String keyStoreFile = "keystore";
    private static String trustStoreFile = "truststore";
    private static String passwd = "passphrase";

    private static String keyFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + keyStoreFile;
    private static String trustFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + trustStoreFile;

    private static Exception loadException = null;

    static {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            KeyStore ts = KeyStore.getInstance("JKS");

            char[] passphrase = "passphrase".toCharArray();

            ks.load(new FileInputStream(keyFilename), passphrase);
            ts.load(new FileInputStream(trustFilename), passphrase);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, passphrase);

            TrustManagerFactory tmf =
                TrustManagerFactory.getInstance("SunX509");
            tmf.init(ts);

            SSLContext sslCtx = SSLContext.getInstance("TLSv1.2");
            sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            sslc = sslCtx;
        } catch (Exception e) {
            loadException = e;
        }
    }

    /*
     * Main entry point for this test.
     */
    public static void main(String args[]) throws Exception {
        if (debug) {
            System.setProperty("javax.net.debug", "all");
        }

        if (loadException != null) {
            throw loadException;
        }

        // Prime the session cache with a good session
        // Second connection should be a simple session resumption.
        if ((new RehandshakeFinished().runTest()) !=
                new RehandshakeFinished().runRehandshake()) {
            throw new Exception("Sessions not equivalent");
        }

        System.out.println("Test Passed.");
    }

    private void checkResult(SSLEngine engine, SSLEngineResult result,
            HandshakeStatus rqdHsStatus,
            boolean consumed, boolean produced) throws Exception {

        HandshakeStatus hsStatus = result.getHandshakeStatus();

        if (hsStatus == HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = engine.getDelegatedTask()) != null) {
                runnable.run();
            }
            hsStatus = engine.getHandshakeStatus();
        }

        if (hsStatus != rqdHsStatus) {
            throw new Exception("Required " + rqdHsStatus +
                ", got " + hsStatus);
        }

        int bc = result.bytesConsumed();
        int bp = result.bytesProduced();

        if (consumed) {
            if (bc <= 0) {
                throw new Exception("Should have consumed bytes");
            }
        } else {
            if (bc > 0) {
                throw new Exception("Should not have consumed bytes");
            }
        }

        if (produced) {
            if (bp <= 0) {
                throw new Exception("Should have produced bytes");
            }
        } else {
            if (bp > 0) {
                throw new Exception("Should not have produced bytes");
            }
        }
    }

    private SSLSession runRehandshake() throws Exception {

        log("\n\n==============================================");
        log("Staring actual test.");

        createSSLEngines();
        createBuffers();
        SSLEngineResult result;

        log("Client's ClientHello");
        checkResult(clientEngine,
            clientEngine.wrap(clientOut, cTOs), HandshakeStatus.NEED_UNWRAP,
            false, true);
        cTOs.flip();
        checkResult(serverEngine,
            serverEngine.unwrap(cTOs, serverIn), HandshakeStatus.NEED_WRAP,
            true, false);
        cTOs.compact();

        log("Server's ServerHello/ServerHelloDone");
        checkResult(serverEngine,
            serverEngine.wrap(serverOut, sTOc), HandshakeStatus.NEED_WRAP,
            false, true);
        sTOc.flip();
        checkResult(clientEngine,
            clientEngine.unwrap(sTOc, clientIn), HandshakeStatus.NEED_UNWRAP,
            true, false);
        sTOc.compact();

        log("Server's CCS");
        checkResult(serverEngine,
            serverEngine.wrap(serverOut, sTOc), HandshakeStatus.NEED_WRAP,
            false, true);
        sTOc.flip();
        checkResult(clientEngine,
            clientEngine.unwrap(sTOc, clientIn), HandshakeStatus.NEED_UNWRAP,
            true, false);
        sTOc.compact();

        log("Server's FINISHED");
        checkResult(serverEngine,
            serverEngine.wrap(serverOut, sTOc), HandshakeStatus.NEED_UNWRAP,
            false, true);
        sTOc.flip();
        checkResult(clientEngine,
            clientEngine.unwrap(sTOc, clientIn), HandshakeStatus.NEED_WRAP,
            true, false);
        sTOc.compact();

        log("Client's CCS");
        checkResult(clientEngine,
            clientEngine.wrap(clientOut, cTOs), HandshakeStatus.NEED_WRAP,
            false, true);
        cTOs.flip();
        checkResult(serverEngine,
            serverEngine.unwrap(cTOs, serverIn), HandshakeStatus.NEED_UNWRAP,
            true, false);
        cTOs.compact();

        log("Client's FINISHED should trigger FINISHED messages all around.");
        checkResult(clientEngine,
            clientEngine.wrap(clientOut, cTOs), HandshakeStatus.FINISHED,
            false, true);
        cTOs.flip();
        checkResult(serverEngine,
            serverEngine.unwrap(cTOs, serverIn), HandshakeStatus.FINISHED,
            true, false);
        cTOs.compact();

        return clientEngine.getSession();
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
    private SSLSession runTest() throws Exception {
        boolean dataDone = false;

        createSSLEngines();
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

        return clientEngine.getSession();
    }

    /*
     * Using the SSLContext created during object creation,
     * create/configure the SSLEngines we'll use for this test.
     */
    private void createSSLEngines() throws Exception {
        /*
         * Configure the serverEngine to act as a server in the SSL/TLS
         * handshake.  Also, require SSL client authentication.
         */
        serverEngine = sslc.createSSLEngine();
        serverEngine.setUseClientMode(false);
        serverEngine.setNeedClientAuth(true);

        /*
         * Similar to above, but using client mode instead.
         */
        clientEngine = sslc.createSSLEngine("client", 80);
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
}
