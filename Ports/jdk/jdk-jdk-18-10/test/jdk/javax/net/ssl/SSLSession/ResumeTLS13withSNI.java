/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8211806
 * @summary TLS 1.3 handshake server name indication is missing on a session resume
 * @run main/othervm ResumeTLS13withSNI
 */

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import java.io.*;
import java.security.*;
import java.nio.*;
import java.util.List;

public class ResumeTLS13withSNI {

    /*
     * Enables logging of the SSLEngine operations.
     */
    private static final boolean logging = false;

    /*
     * Enables the JSSE system debugging system property:
     *
     *     -Djavax.net.debug=ssl:handshake
     *
     * This gives a lot of low-level information about operations underway,
     * including specific handshake messages, and might be best examined
     * after gaining some familiarity with this application.
     */
    private static final boolean debug = true;

    private static final ByteBuffer clientOut =
            ByteBuffer.wrap("Hi Server, I'm Client".getBytes());
    private static final ByteBuffer serverOut =
            ByteBuffer.wrap("Hello Client, I'm Server".getBytes());

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

    private static final String HOST_NAME = "arf.yak.foo";
    private static final SNIHostName SNI_NAME = new SNIHostName(HOST_NAME);
    private static final SNIMatcher SNI_MATCHER =
            SNIHostName.createSNIMatcher("arf\\.yak\\.foo");

    /*
     * Main entry point for this test.
     */
    public static void main(String args[]) throws Exception {
        if (debug) {
            System.setProperty("javax.net.debug", "ssl:handshake");
        }

        KeyManagerFactory kmf = makeKeyManagerFactory(keyFilename,
                passphrase);
        TrustManagerFactory tmf = makeTrustManagerFactory(trustFilename,
                passphrase);

        SSLContext sslCtx = SSLContext.getInstance("TLS");
        sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        // Make client and server engines, then customize as needed
        SSLEngine clientEngine = makeEngine(sslCtx, kmf, tmf, true);
        SSLParameters cliSSLParams = clientEngine.getSSLParameters();
        cliSSLParams.setServerNames(List.of(SNI_NAME));
        clientEngine.setSSLParameters(cliSSLParams);
        clientEngine.setEnabledProtocols(new String[] { "TLSv1.3" });

        SSLEngine serverEngine = makeEngine(sslCtx, kmf, tmf, false);
        SSLParameters servSSLParams = serverEngine.getSSLParameters();
        servSSLParams.setSNIMatchers(List.of(SNI_MATCHER));
        serverEngine.setSSLParameters(servSSLParams);

        initialHandshake(clientEngine, serverEngine);

        // Create a new client-side engine which can initiate TLS session
        // resumption
        SSLEngine newCliEngine = makeEngine(sslCtx, kmf, tmf, true);
        newCliEngine.setEnabledProtocols(new String[] { "TLSv1.3" });
        ByteBuffer resCliHello = getResumptionClientHello(newCliEngine);

        dumpBuffer("Resumed ClientHello Data", resCliHello);

        // Parse the client hello message and make sure it is a resumption
        // hello and has SNI in it.
        checkResumedClientHelloSNI(resCliHello);
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
    private static void initialHandshake(SSLEngine clientEngine,
            SSLEngine serverEngine) throws Exception {
        boolean dataDone = false;

        // Create all the buffers
        SSLSession session = clientEngine.getSession();
        int appBufferMax = session.getApplicationBufferSize();
        int netBufferMax = session.getPacketBufferSize();
        ByteBuffer clientIn = ByteBuffer.allocate(appBufferMax + 50);
        ByteBuffer serverIn = ByteBuffer.allocate(appBufferMax + 50);
        ByteBuffer cTOs = ByteBuffer.allocateDirect(netBufferMax);
        ByteBuffer sTOc = ByteBuffer.allocateDirect(netBufferMax);

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

        while (!dataDone) {
            log("================");

            try {
                clientResult = clientEngine.wrap(clientOut, cTOs);
                log("client wrap: ", clientResult);
            } catch (Exception e) {
                clientException = e;
                System.err.println("Client wrap() threw: " + e.getMessage());
            }
            logEngineStatus(clientEngine);
            runDelegatedTasks(clientEngine);

            log("----");

            try {
                serverResult = serverEngine.wrap(serverOut, sTOc);
                log("server wrap: ", serverResult);
            } catch (Exception e) {
                serverException = e;
                System.err.println("Server wrap() threw: " + e.getMessage());
            }
            logEngineStatus(serverEngine);
            runDelegatedTasks(serverEngine);

            cTOs.flip();
            sTOc.flip();

            log("--------");

            try {
                clientResult = clientEngine.unwrap(sTOc, clientIn);
                log("client unwrap: ", clientResult);
            } catch (Exception e) {
                clientException = e;
                System.err.println("Client unwrap() threw: " + e.getMessage());
            }
            logEngineStatus(clientEngine);
            runDelegatedTasks(clientEngine);

            log("----");

            try {
                serverResult = serverEngine.unwrap(cTOs, serverIn);
                log("server unwrap: ", serverResult);
            } catch (Exception e) {
                serverException = e;
                System.err.println("Server unwrap() threw: " + e.getMessage());
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

                dataDone = true;
            }
        }
    }

    /**
     * The goal of this function is to start a simple TLS session resumption
     * and get the client hello message data back so it can be inspected.
     *
     * @param clientEngine
     *
     * @return a ByteBuffer consisting of the ClientHello TLS record.
     *
     * @throws Exception if any processing goes wrong.
     */
    private static ByteBuffer getResumptionClientHello(SSLEngine clientEngine)
            throws Exception {
        // Create all the buffers
        SSLSession session = clientEngine.getSession();
        int appBufferMax = session.getApplicationBufferSize();
        int netBufferMax = session.getPacketBufferSize();
        ByteBuffer cTOs = ByteBuffer.allocateDirect(netBufferMax);
        Exception clientException = null;

        // results from client's last operation
        SSLEngineResult clientResult;

        // results from server's last operation
        SSLEngineResult serverResult;

        log("================");

        // Start by having the client create a new ClientHello.  It should
        // contain PSK info that allows it to attempt session resumption.
        try {
            clientResult = clientEngine.wrap(clientOut, cTOs);
            log("client wrap: ", clientResult);
        } catch (Exception e) {
            clientException = e;
            System.err.println("Client wrap() threw: " + e.getMessage());
        }
        logEngineStatus(clientEngine);
        runDelegatedTasks(clientEngine);

        log("----");

        cTOs.flip();
        return cTOs;
    }

    /**
     * This method walks a ClientHello TLS record, looking for a matching
     * server_name hostname value from the original handshake and a PSK
     * extension, which indicates (in the context of this test) that this
     * is a resumed handshake.
     *
     * @param resCliHello a ByteBuffer consisting of a complete TLS handshake
     *      record that is a ClientHello message.  The position of the buffer
     *      must be at the beginning of the TLS record header.
     *
     * @throws Exception if any of the consistency checks for the TLS record,
     *      or handshake message fails.  It will also throw an exception if
     *      either the server_name extension doesn't have a matching hostname
     *      field or the pre_shared_key extension is not present.
     */
    private static void checkResumedClientHelloSNI(ByteBuffer resCliHello)
            throws Exception {
        boolean foundMatchingSNI = false;
        boolean foundPSK = false;

        // Advance past the following fields:
        // TLS Record header (5 bytes)
        resCliHello.position(resCliHello.position() + 5);

        // Get the next byte and make sure it is a Client Hello
        byte hsMsgType = resCliHello.get();
        if (hsMsgType != 0x01) {
            throw new Exception("Message is not a ClientHello, MsgType = " +
                    hsMsgType);
        }

        // Skip past the length (3 bytes)
        resCliHello.position(resCliHello.position() + 3);

        // Protocol version should be TLSv1.2 (0x03, 0x03)
        short chProto = resCliHello.getShort();
        if (chProto != 0x0303) {
            throw new Exception(
                    "Client Hello protocol version is not TLSv1.2: Got " +
                            String.format("0x%04X", chProto));
        }

        // Skip 32-bytes of random data
        resCliHello.position(resCliHello.position() + 32);

        // Get the legacy session length and skip that many bytes
        int sessIdLen = Byte.toUnsignedInt(resCliHello.get());
        resCliHello.position(resCliHello.position() + sessIdLen);

        // Skip over all the cipher suites
        int csLen = Short.toUnsignedInt(resCliHello.getShort());
        resCliHello.position(resCliHello.position() + csLen);

        // Skip compression methods
        int compLen = Byte.toUnsignedInt(resCliHello.get());
        resCliHello.position(resCliHello.position() + compLen);

        // Parse the extensions.  Get length first, then walk the extensions
        // List and look for the presence of the PSK extension and server_name.
        // For server_name, make sure it is the same as what was provided
        // in the original handshake.
        System.err.println("ClientHello Extensions Check");
        int extListLen = Short.toUnsignedInt(resCliHello.getShort());
        while (extListLen > 0) {
            // Get the Extension type and length
            int extType = Short.toUnsignedInt(resCliHello.getShort());
            int extLen = Short.toUnsignedInt(resCliHello.getShort());
            switch (extType) {
                case 0:                 // server_name
                    System.err.println("* Found server_name Extension");
                    int snListLen = Short.toUnsignedInt(resCliHello.getShort());
                    while (snListLen > 0) {
                        int nameType = Byte.toUnsignedInt(resCliHello.get());
                        if (nameType == 0) {            // host_name
                            int hostNameLen =
                                    Short.toUnsignedInt(resCliHello.getShort());
                            byte[] hostNameData = new byte[hostNameLen];
                            resCliHello.get(hostNameData);
                            String hostNameStr = new String(hostNameData);
                            System.err.println("\tHostname: " + hostNameStr);
                            if (hostNameStr.equals(HOST_NAME)) {
                                foundMatchingSNI = true;
                            }
                            snListLen -= 3 + hostNameLen;   // type, len, data
                        } else {                        // something else
                            // We don't support anything else and cannot
                            // know how to advance.  Throw an exception
                            throw new Exception("Unknown server name type: " +
                                    nameType);
                        }
                    }
                    break;
                case 41:                // pre_shared_key
                    // We're not going to bother checking the value.  The
                    // presence of the extension in the context of this test
                    // is good enough to tell us this is a resumed ClientHello.
                    foundPSK = true;
                    System.err.println("* Found pre_shared_key Extension");
                    resCliHello.position(resCliHello.position() + extLen);
                    break;
                default:
                    System.err.format("* Found extension %d (%d bytes)\n",
                            extType, extLen);
                    resCliHello.position(resCliHello.position() + extLen);
                    break;
            }
            extListLen -= extLen + 4;   // Ext type(2), length(2), data(var.)
        }

        // At the end of all the extension processing, either we've found
        // both extensions and the server_name matches our expected value
        // or we throw an exception.
        if (!foundMatchingSNI) {
            throw new Exception("Could not find a matching server_name");
        } else if (!foundPSK) {
            throw new Exception("Missing PSK extension, not a resumption?");
        }
    }

    /**
     * Create a TrustManagerFactory from a given keystore.
     *
     * @param tsPath the path to the trust store file.
     * @param pass the password for the trust store.
     *
     * @return a new TrustManagerFactory built from the trust store provided.
     *
     * @throws GeneralSecurityException if any processing errors occur
     *      with the Keystore instantiation or TrustManagerFactory creation.
     * @throws IOException if any loading error with the trust store occurs.
     */
    private static TrustManagerFactory makeTrustManagerFactory(String tsPath,
            char[] pass) throws GeneralSecurityException, IOException {
        TrustManagerFactory tmf;
        KeyStore ts = KeyStore.getInstance("JKS");

        try (FileInputStream fsIn = new FileInputStream(tsPath)) {
            ts.load(fsIn, pass);
            tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ts);
        }
        return tmf;
    }

    /**
     * Create a KeyManagerFactory from a given keystore.
     *
     * @param ksPath the path to the keystore file.
     * @param pass the password for the keystore.
     *
     * @return a new TrustManagerFactory built from the keystore provided.
     *
     * @throws GeneralSecurityException if any processing errors occur
     *      with the Keystore instantiation or KeyManagerFactory creation.
     * @throws IOException if any loading error with the keystore occurs
     */
    private static KeyManagerFactory makeKeyManagerFactory(String ksPath,
            char[] pass) throws GeneralSecurityException, IOException {
        KeyManagerFactory kmf;
        KeyStore ks = KeyStore.getInstance("JKS");

        try (FileInputStream fsIn = new FileInputStream(ksPath)) {
            ks.load(fsIn, pass);
            kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, pass);
        }
        return kmf;
    }

    /**
     * Create an SSLEngine instance from a given protocol specifier,
     * KeyManagerFactory and TrustManagerFactory.
     *
     * @param ctx the SSLContext used to create the SSLEngine
     * @param kmf an initialized KeyManagerFactory.  May be null.
     * @param tmf an initialized TrustManagerFactory.  May be null.
     * @param isClient true if it intended to create a client engine, false
     *      for a server engine.
     *
     * @return an SSLEngine instance configured as a server and with client
     *      authentication disabled.
     *
     * @throws GeneralSecurityException if any errors occur during the
     *      creation of the SSLEngine.
     */
    private static SSLEngine makeEngine(SSLContext ctx,
            KeyManagerFactory kmf, TrustManagerFactory tmf, boolean isClient)
            throws GeneralSecurityException {
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        SSLEngine ssle = ctx.createSSLEngine("localhost", 8443);
        ssle.setUseClientMode(isClient);
        ssle.setNeedClientAuth(false);
        return ssle;
    }

    private static void logEngineStatus(SSLEngine engine) {
        log("\tCurrent HS State  " + engine.getHandshakeStatus().toString());
        log("\tisInboundDone():  " + engine.isInboundDone());
        log("\tisOutboundDone(): " + engine.isOutboundDone());
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

    private static void dumpBuffer(String header, ByteBuffer data) {
        data.mark();
        System.err.format("========== %s ==========\n", header);
        int i = 0;
        while (data.remaining() > 0) {
            if (i != 0 && i % 16 == 0) {
                System.err.print("\n");
            }
            System.err.format("%02X ", data.get());
            i++;
        }
        System.err.println();
        data.reset();
    }

}
