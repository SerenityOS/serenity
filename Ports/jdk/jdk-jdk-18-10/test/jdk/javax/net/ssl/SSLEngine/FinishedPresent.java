/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8233619
 * @summary SSLEngine has not yet caused Solaris kernel to panic
 * @run main/othervm FinishedPresent
 */
import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import java.io.*;
import java.security.*;
import java.nio.*;

public class FinishedPresent {

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
    private static final char[] passphrase = "passphrase".toCharArray();

    private static final String keyFilename =
            System.getProperty("test.src", ".") + "/" + pathToStores +
                "/" + keyStoreFile;
    private static final String trustFilename =
            System.getProperty("test.src", ".") + "/" + pathToStores +
                "/" + trustStoreFile;

    /*
     * Main entry point for this test.
     */
    public static void main(String args[]) throws Exception {
        if (debug) {
            System.setProperty("javax.net.debug", "all");
        }

        FinishedPresent test = new FinishedPresent();
        test.runTest();

        log("Test Passed.");
    }

    /*
     * Create an initialized SSLContext to use for these tests.
     */
    public FinishedPresent() throws Exception {

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");

        ks.load(new FileInputStream(keyFilename), passphrase);
        ts.load(new FileInputStream(trustFilename), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);

        SSLContext sslCtx = SSLContext.getInstance("TLSv1.3");

        sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        sslc = sslCtx;
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
    private void runTest() throws Exception {
        boolean dataDone = false;

        createSSLEngines();
        createBuffers();

        // results from client's last operation
        SSLEngineResult clientResult;

        // results from server's last operation
        SSLEngineResult serverResult;

        /*
         * Examining the SSLEngineResults could be much more involved,
         * and may alter the overall flow of the application.
         *
         * For example, if we received a BUFFER_OVERFLOW when trying
         * to write to the output pipe, we could reallocate a larger
         * pipe, but instead we wait for the peer to drain it.
         */
        Exception clientException = null;
        Exception serverException = null;

        boolean clientFinishedPresent = false;
        boolean serverFinishedPresent = false;
        boolean client2ndFinishedPresent = false;
        boolean server2ndFinishedPresent = false;
        while (!isEngineClosed(clientEngine)
                || !isEngineClosed(serverEngine)) {

            log("================");

            try {
                clientResult = clientEngine.wrap(clientOut, cTOs);
                if (clientFinishedPresent) {
                    client2ndFinishedPresent |= hasFinished(
                        "posthandshake client wrap", clientResult);
                } else {
                    clientFinishedPresent |= hasFinished(
                        "client wrap", clientResult);
                }
                log("client wrap: ", clientResult);
            } catch (Exception e) {
                clientException = e;
                log("Client wrap() threw: " + e.getMessage());
            }
            logEngineStatus(clientEngine);
            runDelegatedTasks(clientEngine);

            log("----");

            try {
                serverResult = serverEngine.wrap(serverOut, sTOc);
                if (serverFinishedPresent) {
                    server2ndFinishedPresent |= hasFinished(
                        "posthandshake server wrap", serverResult);
                } else {
                    serverFinishedPresent |= hasFinished(
                        "server wrap", serverResult);
                }
                log("server wrap: ", serverResult);
            } catch (Exception e) {
                serverException = e;
                log("Server wrap() threw: " + e.getMessage());
            }
            logEngineStatus(serverEngine);
            runDelegatedTasks(serverEngine);

            cTOs.flip();
            sTOc.flip();

            log("--------");

            try {
                clientResult = clientEngine.unwrap(sTOc, clientIn);
                if (clientFinishedPresent) {
                    client2ndFinishedPresent |= hasFinished(
                        "posthandshake client unwrap", clientResult);
                } else {
                    clientFinishedPresent |= hasFinished(
                        "client unwrap", clientResult);
                }
                log("client unwrap: ", clientResult);
            } catch (Exception e) {
                clientException = e;
                log("Client unwrap() threw: " + e.getMessage());
            }
            logEngineStatus(clientEngine);
            runDelegatedTasks(clientEngine);

            log("----");

            try {
                serverResult = serverEngine.unwrap(cTOs, serverIn);
                if (serverFinishedPresent) {
                    server2ndFinishedPresent |= hasFinished(
                        "posthandshake server unwrap", serverResult);
                } else {
                    serverFinishedPresent |= hasFinished(
                        "server unwrap", serverResult);
                }
                log("server unwrap: ", serverResult);
            } catch (Exception e) {
                serverException = e;
                log("Server unwrap() threw: " + e.getMessage());
            }
            logEngineStatus(serverEngine);
            runDelegatedTasks(serverEngine);

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
                logEngineStatus(clientEngine);

                dataDone = true;
                log("\tClosing serverEngine's *OUTBOUND*...");
                serverEngine.closeOutbound();
                logEngineStatus(serverEngine);
            }
        }

        if (!clientFinishedPresent) {
            throw new Exception("No client FINISHED status present");
        }

        if (!serverFinishedPresent) {
            throw new Exception("No server FINISHED status present");
        }

        if (!client2ndFinishedPresent) {
            throw new Exception(
                    "No posthandshake client FINISHED status present");
        }

        // Note: the server side did not finish the handshake unless the
        // posthandshake message get delivered.  This behaviro may be
        // updated in the future.
        //
        // if (!server2ndFinishedPresent) {
        //     throw new Exception(
        //             "No posthandshake server FINISHED status present");
        // }
    }

    private static void logEngineStatus(SSLEngine engine) {
        log("\tCurrent HS State  " + engine.getHandshakeStatus().toString());
        log("\tisInboundDone():  " + engine.isInboundDone());
        log("\tisOutboundDone(): " + engine.isOutboundDone());
    }

    private static boolean hasFinished(
            String prefix, SSLEngineResult engineResult) {
        if (engineResult.getHandshakeStatus() == HandshakeStatus.FINISHED) {
            log(prefix + " finished present: " + engineResult);
            return true;
        }

        return false;
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

        // Get/set parameters if needed
        SSLParameters paramsServer = serverEngine.getSSLParameters();
        serverEngine.setSSLParameters(paramsServer);

        /*
         * Similar to above, but using client mode instead.
         */
        clientEngine = sslc.createSSLEngine("client", 80);
        clientEngine.setUseClientMode(true);

        // Get/set parameters if needed
        SSLParameters paramsClient = clientEngine.getSSLParameters();
        clientEngine.setSSLParameters(paramsClient);
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
    private static void runDelegatedTasks(SSLEngine engine) throws Exception {

        if (engine.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = engine.getDelegatedTask()) != null) {
                log("    running delegated task...");
                runnable.run();
            }
            HandshakeStatus hsStatus = engine.getHandshakeStatus();
            if (hsStatus == HandshakeStatus.NEED_TASK) {
                throw new Exception(
                    "handshake shouldn't need additional tasks");
            }
            logEngineStatus(engine);
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
            System.err.println("The format of the SSLEngineResult is: \n" +
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
            System.err.println(str);
        }
    }
}
