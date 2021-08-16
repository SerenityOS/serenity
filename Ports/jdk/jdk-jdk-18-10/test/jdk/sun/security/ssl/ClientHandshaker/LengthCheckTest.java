/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8044860
 * @summary Vectors and fixed length fields should be verified
 *          for allowed sizes.
 * @library /test/lib
 * @modules java.base/sun.security.ssl
 * @run main/othervm LengthCheckTest
 * @key randomness
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
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import jdk.test.lib.security.SecurityUtils;

public class LengthCheckTest {

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
    private static final boolean dumpBufs = true;

    private final SSLContext sslc;

    private SSLEngine clientEngine;     // client Engine
    private ByteBuffer clientOut;       // write side of clientEngine
    private ByteBuffer clientIn;        // read side of clientEngine

    private SSLEngine serverEngine;     // server Engine
    private ByteBuffer serverOut;       // write side of serverEngine
    private ByteBuffer serverIn;        // read side of serverEngine

    private HandshakeTest handshakeTest;

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
    private static final String pathToStores = "../../../../javax/net/ssl/etc";
    private static final String keyStoreFile = "keystore";
    private static final String trustStoreFile = "truststore";
    private static final String passwd = "passphrase";

    private static final String keyFilename =
            System.getProperty("test.src", ".") + "/" + pathToStores +
                "/" + keyStoreFile;
    private static final String trustFilename =
            System.getProperty("test.src", ".") + "/" + pathToStores +
                "/" + trustStoreFile;

    // Define a few basic TLS record and message types we might need
    private static final int TLS_RECTYPE_CCS = 0x14;
    private static final int TLS_RECTYPE_ALERT = 0x15;
    private static final int TLS_RECTYPE_HANDSHAKE = 0x16;
    private static final int TLS_RECTYPE_APPDATA = 0x17;

    private static final int TLS_HS_HELLO_REQUEST = 0x00;
    private static final int TLS_HS_CLIENT_HELLO = 0x01;
    private static final int TLS_HS_SERVER_HELLO = 0x02;
    private static final int TLS_HS_CERTIFICATE = 0x0B;
    private static final int TLS_HS_SERVER_KEY_EXCHG = 0x0C;
    private static final int TLS_HS_CERT_REQUEST = 0x0D;
    private static final int TLS_HS_SERVER_HELLO_DONE = 0x0E;
    private static final int TLS_HS_CERT_VERIFY = 0x0F;
    private static final int TLS_HS_CLIENT_KEY_EXCHG = 0x10;
    private static final int TLS_HS_FINISHED = 0x14;

    // We're not going to define all the alert types in TLS, just
    // the ones we think we'll need to reference by name.
    private static final int TLS_ALERT_LVL_WARNING = 0x01;
    private static final int TLS_ALERT_LVL_FATAL = 0x02;

    private static final int TLS_ALERT_UNEXPECTED_MSG = 0x0A;
    private static final int TLS_ALERT_HANDSHAKE_FAILURE = 0x28;
    private static final int TLS_ALERT_INTERNAL_ERROR = 0x50;
    private static final int TLS_ALERT_ILLEGAL_PARAMETER = 0x2F;

    public interface HandshakeTest {
        void execTest() throws Exception;
    }

    public final HandshakeTest servSendLongID = new HandshakeTest() {
        @Override
        public void execTest() throws Exception {
            boolean gotException = false;
            SSLEngineResult clientResult;   // results from client's last op
            SSLEngineResult serverResult;   // results from server's last op

            log("\n==== Test: Client receives 64-byte session ID ====");

            // Send Client Hello
            clientResult = clientEngine.wrap(clientOut, cTOs);
            log("client wrap: ", clientResult);
            runDelegatedTasks(clientResult, clientEngine);
            cTOs.flip();
            dumpByteBuffer("CLIENT-TO-SERVER", cTOs);

            // Server consumes Client Hello
            serverResult = serverEngine.unwrap(cTOs, serverIn);
            log("server unwrap: ", serverResult);
            runDelegatedTasks(serverResult, serverEngine);
            cTOs.compact();

            // Server generates ServerHello/Cert/Done record
            serverResult = serverEngine.wrap(serverOut, sTOc);
            log("server wrap: ", serverResult);
            runDelegatedTasks(serverResult, serverEngine);
            sTOc.flip();

            // Intercept the ServerHello messages and instead send
            // one that has a 64-byte session ID.
            if (isTlsMessage(sTOc, TLS_RECTYPE_HANDSHAKE,
                        TLS_HS_SERVER_HELLO)) {
                ArrayList<ByteBuffer> recList = splitRecord(sTOc);

                // Use the original ServerHello as a template to craft one
                // with a longer-than-allowed session ID.
                ByteBuffer servHelloBuf =
                        createEvilServerHello(recList.get(0), 64);

                recList.set(0, servHelloBuf);

                // Now send each ByteBuffer (each being a complete
                // TLS record) into the client-side unwrap.
                // for (ByteBuffer bBuf : recList) {

                Iterator<ByteBuffer> iter = recList.iterator();
                while (!gotException && (iter.hasNext())) {
                    ByteBuffer bBuf = iter.next();
                    dumpByteBuffer("SERVER-TO-CLIENT", bBuf);
                    try {
                        clientResult = clientEngine.unwrap(bBuf, clientIn);
                    } catch (SSLProtocolException e) {
                        log("Received expected SSLProtocolException: " + e);
                        gotException = true;
                    }
                    log("client unwrap: ", clientResult);
                    runDelegatedTasks(clientResult, clientEngine);
                }
            } else {
                dumpByteBuffer("SERVER-TO-CLIENT", sTOc);
                log("client unwrap: ", clientResult);
                runDelegatedTasks(clientResult, clientEngine);
            }
            sTOc.compact();

            // The Client should now send a TLS Alert
            clientResult = clientEngine.wrap(clientOut, cTOs);
            log("client wrap: ", clientResult);
            runDelegatedTasks(clientResult, clientEngine);
            cTOs.flip();
            dumpByteBuffer("CLIENT-TO-SERVER", cTOs);

            // At this point we can verify that both an exception
            // was thrown and the proper action (a TLS alert) was
            // sent back to the server.
            if (gotException == false ||
                    !isTlsMessage(cTOs, TLS_RECTYPE_ALERT, TLS_ALERT_LVL_FATAL,
                            TLS_ALERT_ILLEGAL_PARAMETER)) {
                throw new SSLException(
                    "Client failed to throw Alert:fatal:internal_error");
            }
        }
    };

    public final HandshakeTest clientSendLongID = new HandshakeTest() {
        @Override
        public void execTest() throws Exception {
            boolean gotException = false;
            SSLEngineResult clientResult;   // results from client's last op
            SSLEngineResult serverResult;   // results from server's last op

            log("\n==== Test: Server receives 64-byte session ID ====");

            // Send Client Hello
            ByteBuffer evilClientHello = createEvilClientHello(64);
            dumpByteBuffer("CLIENT-TO-SERVER", evilClientHello);

            // Server consumes Client Hello
            serverResult = serverEngine.unwrap(evilClientHello, serverIn);
            log("server unwrap: ", serverResult);
            runDelegatedTasks(serverResult, serverEngine);
            evilClientHello.compact();

            // Under normal circumstances this should be a ServerHello
            // But should throw an exception instead due to the invalid
            // session ID.
            try {
                serverResult = serverEngine.wrap(serverOut, sTOc);
                log("server wrap: ", serverResult);
                runDelegatedTasks(serverResult, serverEngine);
            } catch (SSLProtocolException ssle) {
                log("Received expected SSLProtocolException: " + ssle);
                gotException = true;
            }

            // We expect to see the server generate an alert here
            serverResult = serverEngine.wrap(serverOut, sTOc);
            log("server wrap: ", serverResult);
            runDelegatedTasks(serverResult, serverEngine);
            sTOc.flip();
            dumpByteBuffer("SERVER-TO-CLIENT", sTOc);

            // At this point we can verify that both an exception
            // was thrown and the proper action (a TLS alert) was
            // sent back to the client.
            if (gotException == false ||
                    !isTlsMessage(sTOc, TLS_RECTYPE_ALERT, TLS_ALERT_LVL_FATAL,
                        TLS_ALERT_ILLEGAL_PARAMETER)) {
                throw new SSLException(
                    "Server failed to throw Alert:fatal:internal_error");
            }
        }
    };


    /*
     * Main entry point for this test.
     */
    public static void main(String args[]) throws Exception {
        // Re-enable TLSv1 since test depends on it.
        SecurityUtils.removeFromDisabledTlsAlgs("TLSv1");

        List<LengthCheckTest> ccsTests = new ArrayList<>();

        if (debug) {
            System.setProperty("javax.net.debug", "ssl");
        }

        ccsTests.add(new LengthCheckTest("ServSendLongID"));
        ccsTests.add(new LengthCheckTest("ClientSendLongID"));

        for (LengthCheckTest test : ccsTests) {
            test.runTest();
        }

        System.out.println("Test Passed.");
    }

    /*
     * Create an initialized SSLContext to use for these tests.
     */
    public LengthCheckTest(String testName) throws Exception {

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");

        char[] passphrase = "passphrase".toCharArray();

        ks.load(new FileInputStream(keyFilename), passphrase);
        ts.load(new FileInputStream(trustFilename), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);

        SSLContext sslCtx = SSLContext.getInstance("TLS");

        sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        sslc = sslCtx;

        switch (testName) {
            case "ServSendLongID":
                handshakeTest = servSendLongID;
                break;
            case "ClientSendLongID":
                handshakeTest = clientSendLongID;
                break;
            default:
                throw new IllegalArgumentException("Unknown test name: " +
                        testName);
        }
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

        handshakeTest.execTest();
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
        serverEngine.setNeedClientAuth(false);

        /*
         * Similar to above, but using client mode instead.
         */
        clientEngine = sslc.createSSLEngine("client", 80);
        clientEngine.setUseClientMode(true);

        // In order to make a test that will be backwards compatible
        // going back to JDK 5, force the handshake to be TLS 1.0 and
        // use one of the older cipher suites.
        clientEngine.setEnabledProtocols(new String[]{"TLSv1"});
        clientEngine.setEnabledCipherSuites(
                new String[]{"TLS_RSA_WITH_AES_128_CBC_SHA"});
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
     * Split a record consisting of multiple TLS handshake messages
     * into individual TLS records, each one in a ByteBuffer of its own.
     *
     * @param tlsRecord A ByteBuffer containing the tls record data.
     *        The position of the buffer should be at the first byte
     *        in the TLS record data.
     *
     * @return An ArrayList consisting of one or more ByteBuffers.  Each
     *         ByteBuffer will contain a single TLS record with one message.
     *         That message will be taken from the input record.  The order
     *         of the messages in the ArrayList will be the same as they
     *         were in the input record.
     */
    private ArrayList<ByteBuffer> splitRecord(ByteBuffer tlsRecord) {
        SSLSession session = clientEngine.getSession();
        int netBufferMax = session.getPacketBufferSize();
        ArrayList<ByteBuffer> recordList = new ArrayList<>();

        if (tlsRecord.hasRemaining()) {
            int type = Byte.toUnsignedInt(tlsRecord.get());
            byte ver_major = tlsRecord.get();
            byte ver_minor = tlsRecord.get();
            int recLen = Short.toUnsignedInt(tlsRecord.getShort());
            byte[] newMsgData = null;
            while (tlsRecord.hasRemaining()) {
                ByteBuffer newRecord = ByteBuffer.allocateDirect(netBufferMax);
                switch (type) {
                    case TLS_RECTYPE_CCS:
                    case TLS_RECTYPE_ALERT:
                    case TLS_RECTYPE_APPDATA:
                        // None of our tests have multiple non-handshake
                        // messages coalesced into a single record.
                        break;
                    case TLS_RECTYPE_HANDSHAKE:
                        newMsgData = getHandshakeMessage(tlsRecord);
                        break;
                }

                // Put a new TLS record on the destination ByteBuffer
                newRecord.put((byte)type);
                newRecord.put(ver_major);
                newRecord.put(ver_minor);
                newRecord.putShort((short)newMsgData.length);

                // Now add the message content itself and attach to the
                // returned ArrayList
                newRecord.put(newMsgData);
                newRecord.flip();
                recordList.add(newRecord);
            }
        }

        return recordList;
    }

    private static ByteBuffer createEvilClientHello(int sessIdLen) {
        ByteBuffer newRecord = ByteBuffer.allocateDirect(4096);

        // Lengths will initially be place holders until we determine the
        // finished length of the ByteBuffer.  Then we'll go back and scribble
        // in the correct lengths.

        newRecord.put((byte)TLS_RECTYPE_HANDSHAKE);     // Record type
        newRecord.putShort((short)0x0301);              // Protocol (TLS 1.0)
        newRecord.putShort((short)0);                   // Length place holder

        newRecord.putInt(TLS_HS_CLIENT_HELLO << 24);    // HS type and length
        newRecord.putShort((short)0x0301);
        newRecord.putInt((int)(System.currentTimeMillis() / 1000));
        SecureRandom sr = new SecureRandom();
        byte[] randBuf = new byte[28];
        sr.nextBytes(randBuf);
        newRecord.put(randBuf);                         // Client Random
        newRecord.put((byte)sessIdLen);                 // Session ID length
        if (sessIdLen > 0) {
            byte[] sessId = new byte[sessIdLen];
            sr.nextBytes(sessId);
            newRecord.put(sessId);                      // Session ID
        }
        newRecord.putShort((short)2);                   // 2 bytes of ciphers
        newRecord.putShort((short)0x002F);              // TLS_RSA_AES_CBC_SHA
        newRecord.putShort((short)0x0100);              // only null compression
        newRecord.putShort((short)5);                   // 5 bytes of extensions
        newRecord.putShort((short)0xFF01);              // Renegotiation info
        newRecord.putShort((short)1);
        newRecord.put((byte)0);                         // No reneg info exts

        // Go back and fill in the correct length values for the record
        // and handshake message headers.
        int recordLength = newRecord.position();
        newRecord.putShort(3, (short)(recordLength - 5));
        int newTypeAndLen = (newRecord.getInt(5) & 0xFF000000) |
                ((recordLength - 9) & 0x00FFFFFF);
        newRecord.putInt(5, newTypeAndLen);

        newRecord.flip();
        return newRecord;
    }

    private static ByteBuffer createEvilServerHello(ByteBuffer origHello,
            int newSessIdLen) {
        if (newSessIdLen < 0 || newSessIdLen > Byte.MAX_VALUE) {
            throw new RuntimeException("Length must be 0 <= X <= 127");
        }

        ByteBuffer newRecord = ByteBuffer.allocateDirect(4096);
        // Copy the bytes from the old hello to the new up to the session ID
        // field.  We will go back later and fill in a new length field in
        // the record header.  This includes the record header (5 bytes), the
        // Handshake message header (4 bytes), protocol version (2 bytes),
        // and the random (32 bytes).
        ByteBuffer scratchBuffer = origHello.slice();
        scratchBuffer.limit(43);
        newRecord.put(scratchBuffer);

        // Advance the position in the originial hello buffer past the
        // session ID.
        origHello.position(43);
        int origIDLen = Byte.toUnsignedInt(origHello.get());
        if (origIDLen > 0) {
            // Skip over the session ID
            origHello.position(origHello.position() + origIDLen);
        }

        // Now add our own sessionID to the new record
        SecureRandom sr = new SecureRandom();
        byte[] sessId = new byte[newSessIdLen];
        sr.nextBytes(sessId);
        newRecord.put((byte)newSessIdLen);
        newRecord.put(sessId);

        // Create another slice in the original buffer, based on the position
        // past the session ID.  Copy the remaining bytes into the new
        // hello buffer.  Then go back and fix up the length
        newRecord.put(origHello.slice());

        // Go back and fill in the correct length values for the record
        // and handshake message headers.
        int recordLength = newRecord.position();
        newRecord.putShort(3, (short)(recordLength - 5));
        int newTypeAndLen = (newRecord.getInt(5) & 0xFF000000) |
                ((recordLength - 9) & 0x00FFFFFF);
        newRecord.putInt(5, newTypeAndLen);

        newRecord.flip();
        return newRecord;
    }

    /**
     * Look at an incoming TLS record and see if it is the desired
     * record type, and where appropriate the correct subtype.
     *
     * @param srcRecord The input TLS record to be evaluated.  This
     *        method will only look at the leading message if multiple
     *        TLS handshake messages are coalesced into a single record.
     * @param reqRecType The requested TLS record type
     * @param recParams Zero or more integer sub type fields.  For CCS
     *        and ApplicationData, no params are used.  For handshake records,
     *        one value corresponding to the HandshakeType is required.
     *        For Alerts, two values corresponding to AlertLevel and
     *        AlertDescription are necessary.
     *
     * @return true if the proper handshake message is the first one
     *         in the input record, false otherwise.
     */
    private boolean isTlsMessage(ByteBuffer srcRecord, int reqRecType,
            int... recParams) {
        boolean foundMsg = false;

        if (srcRecord.hasRemaining()) {
            srcRecord.mark();

            // Grab the fields from the TLS Record
            int recordType = Byte.toUnsignedInt(srcRecord.get());
            byte ver_major = srcRecord.get();
            byte ver_minor = srcRecord.get();
            int recLen = Short.toUnsignedInt(srcRecord.getShort());

            if (recordType == reqRecType) {
                // For any zero-length recParams, making sure the requested
                // type is sufficient.
                if (recParams.length == 0) {
                    foundMsg = true;
                } else {
                    switch (recordType) {
                        case TLS_RECTYPE_CCS:
                        case TLS_RECTYPE_APPDATA:
                            // We really shouldn't find ourselves here, but
                            // if someone asked for these types and had more
                            // recParams we can ignore them.
                            foundMsg = true;
                            break;
                        case TLS_RECTYPE_ALERT:
                            // Needs two params, AlertLevel and AlertDescription
                            if (recParams.length != 2) {
                                throw new RuntimeException(
                                    "Test for Alert requires level and desc.");
                            } else {
                                int level = Byte.toUnsignedInt(srcRecord.get());
                                int desc = Byte.toUnsignedInt(srcRecord.get());
                                if (level == recParams[0] &&
                                        desc == recParams[1]) {
                                    foundMsg = true;
                                }
                            }
                            break;
                        case TLS_RECTYPE_HANDSHAKE:
                            // Needs one parameter, HandshakeType
                            if (recParams.length != 1) {
                                throw new RuntimeException(
                                    "Test for Handshake requires only HS type");
                            } else {
                                // Go into the first handhshake message in the
                                // record and grab the handshake message header.
                                // All we need to do is parse out the leading
                                // byte.
                                int msgHdr = srcRecord.getInt();
                                int msgType = (msgHdr >> 24) & 0x000000FF;
                                if (msgType == recParams[0]) {
                                foundMsg = true;
                            }
                        }
                        break;
                    }
                }
            }

            srcRecord.reset();
        }

        return foundMsg;
    }

    private byte[] getHandshakeMessage(ByteBuffer srcRecord) {
        // At the start of this routine, the position should be lined up
        // at the first byte of a handshake message.  Mark this location
        // so we can return to it after reading the type and length.
        srcRecord.mark();
        int msgHdr = srcRecord.getInt();
        int type = (msgHdr >> 24) & 0x000000FF;
        int length = msgHdr & 0x00FFFFFF;

        // Create a byte array that has enough space for the handshake
        // message header and body.
        byte[] data = new byte[length + 4];
        srcRecord.reset();
        srcRecord.get(data, 0, length + 4);

        return (data);
    }

    /**
     * Hex-dumps a ByteBuffer to stdout.
     */
    private static void dumpByteBuffer(String header, ByteBuffer bBuf) {
        if (dumpBufs == false) {
            return;
        }

        int bufLen = bBuf.remaining();
        if (bufLen > 0) {
            bBuf.mark();

            // We expect the position of the buffer to be at the
            // beginning of a TLS record.  Get the type, version and length.
            int type = Byte.toUnsignedInt(bBuf.get());
            int ver_major = Byte.toUnsignedInt(bBuf.get());
            int ver_minor = Byte.toUnsignedInt(bBuf.get());
            int recLen = Short.toUnsignedInt(bBuf.getShort());

            log("===== " + header + " (" + tlsRecType(type) + " / " +
                ver_major + "." + ver_minor + " / " + bufLen + " bytes) =====");
            bBuf.reset();
            for (int i = 0; i < bufLen; i++) {
                if (i != 0 && i % 16 == 0) {
                    System.out.print("\n");
                }
                System.out.format("%02X ", bBuf.get(i));
            }
            log("\n===============================================");
            bBuf.reset();
        }
    }

    private static String tlsRecType(int type) {
        switch (type) {
            case 20:
                return "Change Cipher Spec";
            case 21:
                return "Alert";
            case 22:
                return "Handshake";
            case 23:
                return "Application Data";
            default:
                return ("Unknown (" + type + ")");
        }
    }
}
