/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8250839
 * @summary Improve test template SSLEngineTemplate with SSLContextTemplate
 * @build SSLContextTemplate
 * @run main/othervm SSLEngineTemplate
 */
import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import java.nio.ByteBuffer;

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
 * produced.
 */
public class SSLEngineTemplate implements SSLContextTemplate {
    protected final SSLEngine clientEngine;     // client Engine
    protected final ByteBuffer clientOut;       // write side of clientEngine
    protected final ByteBuffer clientIn;        // read side of clientEngine

    protected final SSLEngine serverEngine;     // server Engine
    protected final ByteBuffer serverOut;       // write side of serverEngine
    protected final ByteBuffer serverIn;        // read side of serverEngine

    // For data transport, this example uses local ByteBuffers.  This
    // isn't really useful, but the purpose of this example is to show
    // SSLEngine concepts, not how to do network transport.
    protected final ByteBuffer cTOs;      // "reliable" transport client->server
    protected final ByteBuffer sTOc;      // "reliable" transport server->client

    protected SSLEngineTemplate() throws Exception {
        serverEngine = configureServerEngine(
                createServerSSLContext().createSSLEngine());

        clientEngine = configureClientEngine(
                createClientSSLContext().createSSLEngine());

        // We'll assume the buffer sizes are the same
        // between client and server.
        SSLSession session = clientEngine.getSession();
        int appBufferMax = session.getApplicationBufferSize();
        int netBufferMax = session.getPacketBufferSize();

        // We'll make the input buffers a bit bigger than the max needed
        // size, so that unwrap()s following a successful data transfer
        // won't generate BUFFER_OVERFLOWS.
        //
        // We'll use a mix of direct and indirect ByteBuffers for
        // tutorial purposes only.  In reality, only use direct
        // ByteBuffers when they give a clear performance enhancement.
        clientIn = ByteBuffer.allocate(appBufferMax + 50);
        serverIn = ByteBuffer.allocate(appBufferMax + 50);

        cTOs = ByteBuffer.allocateDirect(netBufferMax);
        sTOc = ByteBuffer.allocateDirect(netBufferMax);

        clientOut = ByteBuffer.wrap("Hi Server, I'm Client".getBytes());
        serverOut = ByteBuffer.wrap("Hello Client, I'm Server".getBytes());
    }

    //
    // Protected methods could be used to customize the test case.
    //

    /*
     * Configure the client side engine.
     */
    protected SSLEngine configureClientEngine(SSLEngine clientEngine) {
        clientEngine.setUseClientMode(true);

        // Get/set parameters if needed
        // SSLParameters paramsClient = clientEngine.getSSLParameters();
        // clientEngine.setSSLParameters(paramsClient);

        return clientEngine;
    }

    /*
     * Configure the server side engine.
     */
    protected SSLEngine configureServerEngine(SSLEngine serverEngine) {
        serverEngine.setUseClientMode(false);
        serverEngine.setNeedClientAuth(true);

        // Get/set parameters if needed
        //
        // SSLParameters paramsServer = serverEngine.getSSLParameters();
        // serverEngine.setSSLParameters(paramsServer);

        return serverEngine;
    }

    public static void main(String[] args) throws Exception {
        new SSLEngineTemplate().runTest();
    }

    //
    // Private methods that used to build the common part of the test.
    //

    private void runTest() throws Exception {
        SSLEngineResult clientResult;
        SSLEngineResult serverResult;

        boolean dataDone = false;
        while (isOpen(clientEngine) || isOpen(serverEngine)) {
            log("=================");

            // client wrap
            log("---Client Wrap---");
            clientResult = clientEngine.wrap(clientOut, cTOs);
            logEngineStatus(clientEngine, clientResult);
            runDelegatedTasks(clientEngine);

            // server wrap
            log("---Server Wrap---");
            serverResult = serverEngine.wrap(serverOut, sTOc);
            logEngineStatus(serverEngine, serverResult);
            runDelegatedTasks(serverEngine);

            cTOs.flip();
            sTOc.flip();

            // client unwrap
            log("---Client Unwrap---");
            clientResult = clientEngine.unwrap(sTOc, clientIn);
            logEngineStatus(clientEngine, clientResult);
            runDelegatedTasks(clientEngine);

            // server unwrap
            log("---Server Unwrap---");
            serverResult = serverEngine.unwrap(cTOs, serverIn);
            logEngineStatus(serverEngine, serverResult);
            runDelegatedTasks(serverEngine);

            cTOs.compact();
            sTOc.compact();

            // After we've transferred all application data between the client
            // and server, we close the clientEngine's outbound stream.
            // This generates a close_notify handshake message, which the
            // server engine receives and responds by closing itself.
            if (!dataDone && (clientOut.limit() == serverIn.position()) &&
                    (serverOut.limit() == clientIn.position())) {

                // A sanity check to ensure we got what was sent.
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
    }

    static boolean isOpen(SSLEngine engine) {
        return (!engine.isOutboundDone() || !engine.isInboundDone());
    }

    private static void logEngineStatus(SSLEngine engine) {
        log("\tCurrent HS State: " + engine.getHandshakeStatus());
        log("\tisInboundDone() : " + engine.isInboundDone());
        log("\tisOutboundDone(): " + engine.isOutboundDone());
    }

    private static void logEngineStatus(
            SSLEngine engine, SSLEngineResult result) {
        log("\tResult Status    : " + result.getStatus());
        log("\tResult HS Status : " + result.getHandshakeStatus());
        log("\tEngine HS Status : " + engine.getHandshakeStatus());
        log("\tisInboundDone()  : " + engine.isInboundDone());
        log("\tisOutboundDone() : " + engine.isOutboundDone());
        log("\tMore Result      : " + result);
    }

    private static void log(String message) {
        System.err.println(message);
    }

    // If the result indicates that we have outstanding tasks to do,
    // go ahead and run them in this thread.
    protected static void runDelegatedTasks(SSLEngine engine) throws Exception {
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

    // Simple check to make sure everything came across as expected.
    static void checkTransfer(ByteBuffer a, ByteBuffer b)
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
}
