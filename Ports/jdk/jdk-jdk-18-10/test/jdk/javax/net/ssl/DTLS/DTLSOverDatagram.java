/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8043758
 * @summary Datagram Transport Layer Security (DTLS)
 * @modules java.base/sun.security.util
 * @library /test/lib
 * @run main/othervm DTLSOverDatagram
 */

import java.nio.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.*;

import jdk.test.lib.security.KeyStoreUtils;
import jdk.test.lib.security.SSLContextBuilder;

import java.util.concurrent.*;

import jdk.test.lib.hexdump.HexPrinter;

/**
 * An example to show the way to use SSLEngine in datagram connections.
 */
public class DTLSOverDatagram {

    private static final int MAX_HANDSHAKE_LOOPS = 200;
    private static final int MAX_APP_READ_LOOPS = 60;
    private static final int SOCKET_TIMEOUT = 10 * 1000; // in millis
    private static final int BUFFER_SIZE = 1024;
    private static final int MAXIMUM_PACKET_SIZE = 1024;

    /*
     * The following is to set up the keystores.
     */
    private static final String PATH_TO_STORES = "../etc";
    private static final String KEY_STORE_FILE = "keystore";
    private static final String TRUST_STORE_FILE = "truststore";

    private static final String KEY_FILENAME =
            System.getProperty("test.src", ".") + "/" + PATH_TO_STORES +
                "/" + KEY_STORE_FILE;
    private static final String TRUST_FILENAME =
            System.getProperty("test.src", ".") + "/" + PATH_TO_STORES +
                "/" + TRUST_STORE_FILE;

    private static final ByteBuffer SERVER_APP =
                ByteBuffer.wrap("Hi Client, I'm Server".getBytes());
    private static final ByteBuffer CLIENT_APP =
                ByteBuffer.wrap("Hi Server, I'm Client".getBytes());

    private static Exception clientException = null;
    private static Exception serverException = null;
    /*
     * =============================================================
     * The test case
     */
    public static void main(String[] args) throws Exception {
        DTLSOverDatagram testCase = new DTLSOverDatagram();
        testCase.runTest(testCase);
    }

    /*
     * Define the server side of the test.
     */
    void doServerSide(DatagramSocket socket, InetSocketAddress clientSocketAddr)
            throws Exception {
        String side = "Server";

        // create SSLEngine
        SSLEngine engine = createSSLEngine(false);

        // handshaking
        handshake(engine, socket, clientSocketAddr, side);

        // read client application data
        receiveAppData(engine, socket, CLIENT_APP);

        // write server application data
        deliverAppData(engine, socket, SERVER_APP, clientSocketAddr, side);
    }

    /*
     * Define the client side of the test.
     */
    void doClientSide(DatagramSocket socket, InetSocketAddress serverSocketAddr)
            throws Exception {
        String side = "Client";

        // create SSLEngine
        SSLEngine engine = createSSLEngine(true);

        // handshaking
        handshake(engine, socket, serverSocketAddr, side);

        // write client application data
        deliverAppData(engine, socket, CLIENT_APP, serverSocketAddr, side);

        // read server application data
        receiveAppData(engine, socket, SERVER_APP);
    }

    /*
     * =============================================================
     * The remainder is support stuff for DTLS operations.
     */
    SSLEngine createSSLEngine(boolean isClient) throws Exception {
        SSLContext context = getDTLSContext();
        SSLEngine engine = context.createSSLEngine();

        SSLParameters paras = engine.getSSLParameters();
        paras.setMaximumPacketSize(MAXIMUM_PACKET_SIZE);

        engine.setUseClientMode(isClient);
        engine.setSSLParameters(paras);

        return engine;
    }

    // handshake
    void handshake(SSLEngine engine, DatagramSocket socket,
            SocketAddress peerAddr, String side) throws Exception {

        boolean endLoops = false;
        int loops = MAX_HANDSHAKE_LOOPS;
        engine.beginHandshake();
        while (!endLoops &&
                (serverException == null) && (clientException == null)) {

            if (--loops < 0) {
                throw new RuntimeException(
                        "Too many loops to produce handshake packets");
            }

            SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();
            log(side, "=======handshake(" + loops + ", " + hs + ")=======");

            switch (hs) {
                case NEED_UNWRAP, NEED_UNWRAP_AGAIN -> {
                    log(side, "Receive DTLS records, handshake status is " + hs);

                    ByteBuffer iNet;
                    ByteBuffer iApp;

                    if (hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                        byte[] buf = new byte[BUFFER_SIZE];
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        try {
                            socket.receive(packet);
                        } catch (SocketTimeoutException ste) {
                            log(side, "Warning: " + ste);

                            List<DatagramPacket> packets = new ArrayList<>();
                            boolean finished = onReceiveTimeout(
                                    engine, peerAddr, side, packets);

                            log(side, "Reproduced " + packets.size() + " packets");
                            for (DatagramPacket p : packets) {
                                printHex("Reproduced packet",
                                        p.getData(), p.getOffset(), p.getLength());
                                socket.send(p);
                            }

                            if (finished) {
                                log(side, "Handshake status is FINISHED "
                                        + "after calling onReceiveTimeout(), "
                                        + "finish the loop");
                                endLoops = true;
                            }

                            log(side, "New handshake status is "
                                    + engine.getHandshakeStatus());

                            continue;
                        }

                        iNet = ByteBuffer.wrap(buf, 0, packet.getLength());
                    } else {
                        iNet = ByteBuffer.allocate(0);
                    }

                    iApp = ByteBuffer.allocate(BUFFER_SIZE);

                    SSLEngineResult r = engine.unwrap(iNet, iApp);
                    hs = r.getHandshakeStatus();

                    verifySSLEngineResultStatus(r, side);
                    if (hs == SSLEngineResult.HandshakeStatus.FINISHED) {
                        log(side, "Handshake status is FINISHED, finish the loop");
                        endLoops = true;
                    }
                }
                case NEED_WRAP -> {
                    List<DatagramPacket> packets = new ArrayList<>();
                    boolean finished = produceHandshakePackets(
                            engine, peerAddr, side, packets);

                    log(side, "Produced " + packets.size() + " packets");
                    for (DatagramPacket p : packets) {
                        socket.send(p);
                    }

                    if (finished) {
                        log(side, "Handshake status is FINISHED "
                                + "after producing handshake packets, "
                                + "finish the loop");
                        endLoops = true;
                    }
                }
                case NEED_TASK -> runDelegatedTasks(engine);
                case NOT_HANDSHAKING -> {
                    log(side,
                            "Handshake status is NOT_HANDSHAKING, finish the loop");
                    endLoops = true;
                }
                case FINISHED -> throw new Exception( "Unexpected status, " +
                        "SSLEngine.getHandshakeStatus() shouldn't return FINISHED");
                default -> throw new Exception("Can't reach here, " +
                        "handshake status is " + hs);
            }
        }

        SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();
        log(side, "Handshake finished, status is " + hs);

        if (engine.getHandshakeSession() != null) {
            throw new Exception(
                    "Handshake finished, but handshake session is not null");
        }

        SSLSession session = engine.getSession();
        if (session == null) {
            throw new Exception("Handshake finished, but session is null");
        }
        log(side, "Negotiated protocol is " + session.getProtocol());
        log(side, "Negotiated cipher suite is " + session.getCipherSuite());

        // handshake status should be NOT_HANDSHAKING
        //
        // According to the spec, SSLEngine.getHandshakeStatus() can't
        // return FINISHED.
        if (hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            throw new Exception("Unexpected handshake status " + hs);
        }
    }

    void verifySSLEngineResultStatus(SSLEngineResult r, String side) throws Exception {
        SSLEngineResult.Status rs = r.getStatus();
        SSLEngineResult.HandshakeStatus hs = r.getHandshakeStatus();
        switch (rs) {
            case OK -> log(side, "SSLEngineResult status OK");
            case BUFFER_OVERFLOW -> {
                log(side, "BUFFER_OVERFLOW, handshake status is " + hs);
                // the client maximum fragment size config does not work?
                throw new Exception("Buffer overflow: " +
                        "incorrect client maximum fragment size");
            }
            case BUFFER_UNDERFLOW -> {
                log(side, "BUFFER_UNDERFLOW, handshake status is " + hs);
                // bad packet, or the client maximum fragment size
                // config does not work?
                if (hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                    throw new Exception("Buffer underflow: " +
                            "incorrect client maximum fragment size");
                } // otherwise, ignore this packet
            }
            case CLOSED -> throw new Exception(
                    "SSL engine closed, handshake status is " + hs);
            default -> throw new Exception("Can't reach here, result is " + rs);
        }
    }

    // deliver application data
    void deliverAppData(SSLEngine engine, DatagramSocket socket,
            ByteBuffer appData, SocketAddress peerAddr, String side) throws Exception {

        // Note: have not consider the packet loses
        List<DatagramPacket> packets =
                produceApplicationPackets(engine, appData, peerAddr, side);
        appData.flip();
        for (DatagramPacket p : packets) {
            socket.send(p);
        }
    }

    // receive application data
    void receiveAppData(SSLEngine engine,
            DatagramSocket socket, ByteBuffer expectedApp) throws Exception {

        int loops = MAX_APP_READ_LOOPS;
        while ((serverException == null) && (clientException == null)) {
            if (--loops < 0) {
                throw new RuntimeException(
                        "Too much loops to receive application data");
            }

            byte[] buf = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            ByteBuffer netBuffer = ByteBuffer.wrap(buf, 0, packet.getLength());
            ByteBuffer recBuffer = ByteBuffer.allocate(BUFFER_SIZE);
            SSLEngineResult rs = engine.unwrap(netBuffer, recBuffer);
            recBuffer.flip();
            if (recBuffer.remaining() != 0) {
                printHex("Received application data", recBuffer);
                if (!recBuffer.equals(expectedApp)) {
                    System.out.println("Engine status is " + rs);
                    throw new Exception("Not the right application data");
                }
                break;
            }
        }
    }

    // produce handshake packets
    boolean produceHandshakePackets(SSLEngine engine, SocketAddress socketAddr,
            String side, List<DatagramPacket> packets) throws Exception {

        boolean endLoops = false;
        int loops = MAX_HANDSHAKE_LOOPS / 2;
        while (!endLoops &&
                (serverException == null) && (clientException == null)) {

            if (--loops < 0) {
                throw new RuntimeException(
                        "Too many loops to produce handshake packets");
            }

            ByteBuffer oNet = ByteBuffer.allocate(32768);
            ByteBuffer oApp = ByteBuffer.allocate(0);
            SSLEngineResult r = engine.wrap(oApp, oNet);
            oNet.flip();

            SSLEngineResult.Status rs = r.getStatus();
            SSLEngineResult.HandshakeStatus hs = r.getHandshakeStatus();
            log(side, "----produce handshake packet(" +
                    loops + ", " + rs + ", " + hs + ")----");

            verifySSLEngineResultStatus(r, side);

            if (oNet.hasRemaining()) {
                byte[] ba = new byte[oNet.remaining()];
                oNet.get(ba);
                DatagramPacket packet = createHandshakePacket(ba, socketAddr);
                packets.add(packet);
            }

            if (hs == SSLEngineResult.HandshakeStatus.FINISHED) {
                log(side, "Produce handshake packets: "
                            + "Handshake status is FINISHED, finish the loop");
                return true;
            }

            boolean endInnerLoop = false;
            SSLEngineResult.HandshakeStatus nhs = hs;
            while (!endInnerLoop) {
                switch (nhs) {
                    case NEED_TASK -> runDelegatedTasks(engine);
                    case NEED_UNWRAP, NEED_UNWRAP_AGAIN, NOT_HANDSHAKING -> {
                        endInnerLoop = true;
                        endLoops = true;
                    }
                    case NEED_WRAP -> endInnerLoop = true;
                    case FINISHED ->  throw new Exception(
                            "Unexpected status, SSLEngine.getHandshakeStatus() "
                                    + "should not return FINISHED");
                    default -> throw new Exception("Can't reach here, handshake status is "
                            + nhs);
                }

                nhs = engine.getHandshakeStatus();
            }
        }

        return false;
    }

    DatagramPacket createHandshakePacket(byte[] ba, SocketAddress socketAddr) {
        return new DatagramPacket(ba, ba.length, socketAddr);
    }

    // produce application packets
    List<DatagramPacket> produceApplicationPackets(
            SSLEngine engine, ByteBuffer source,
            SocketAddress socketAddr, String side) throws Exception {

        List<DatagramPacket> packets = new ArrayList<>();
        ByteBuffer appNet = ByteBuffer.allocate(32768);
        SSLEngineResult r = engine.wrap(source, appNet);
        appNet.flip();

        verifySSLEngineResultStatus(r, side);

        if (appNet.hasRemaining()) {
            byte[] ba = new byte[appNet.remaining()];
            appNet.get(ba);
            DatagramPacket packet =
                    new DatagramPacket(ba, ba.length, socketAddr);
            packets.add(packet);
        }

        return packets;
    }

    // Get a datagram packet for the specified handshake type.
    static DatagramPacket getPacket(
            List<DatagramPacket> packets, byte handshakeType) {
        boolean matched = false;
        for (DatagramPacket packet : packets) {
            byte[] data = packet.getData();
            int offset = packet.getOffset();
            int length = packet.getLength();

            // Normally, this packet should be a handshake message
            // record.  However, even if the underlying platform
            // splits the record more, we don't really worry about
            // the improper packet loss because DTLS implementation
            // should be able to handle packet loss properly.
            //
            // See RFC 6347 for the detailed format of DTLS records.
            if (handshakeType == -1) {      // ChangeCipherSpec
                // Is it a ChangeCipherSpec message?
                matched = (length == 14) && (data[offset] == 0x14);
            } else if ((length >= 25) &&    // 25: handshake mini size
                (data[offset] == 0x16)) {   // a handshake message

                // check epoch number for initial handshake only
                if (data[offset + 3] == 0x00) {     // 3,4: epoch
                    if (data[offset + 4] == 0x00) { // plaintext
                        matched =
                            (data[offset + 13] == handshakeType);
                    } else {                        // ciphertext
                        // The 1st ciphertext is a Finished message.
                        //
                        // If it is not proposed to loss the Finished
                        // message, it is not necessary to check the
                        // following packets any more as a Finished
                        // message is the last handshake message.
                        matched = (handshakeType == 20);
                    }
                }
            }

            if (matched) {
                return packet;
            }
        }

        return null;
    }

    // run delegated tasks
    void runDelegatedTasks(SSLEngine engine) throws Exception {
        Runnable runnable;
        while ((runnable = engine.getDelegatedTask()) != null) {
            runnable.run();
        }

        SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();
        if (hs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            throw new Exception("handshake shouldn't need additional tasks");
        }
    }

    // retransmission if timeout
    boolean onReceiveTimeout(SSLEngine engine, SocketAddress socketAddr,
            String side, List<DatagramPacket> packets) throws Exception {

        SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();
        if (hs == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            return false;
        } else {
            // retransmission of handshake messages
            return produceHandshakePackets(engine, socketAddr, side, packets);
        }
    }

    // get DTSL context
    SSLContext getDTLSContext() throws Exception {
        String passphrase = "passphrase";
        return SSLContextBuilder.builder()
                .trustStore(KeyStoreUtils.loadKeyStore(TRUST_FILENAME, passphrase))
                .keyStore(KeyStoreUtils.loadKeyStore(KEY_FILENAME, passphrase))
                .kmfPassphrase(passphrase)
                .protocol("DTLS")
                .build();
    }


    /*
     * =============================================================
     * The remainder is support stuff to kickstart the testing.
     */

    // Will the handshaking and application data exchange succeed?
    public boolean isGoodJob() {
        return true;
    }

    public final void runTest(DTLSOverDatagram testCase) throws Exception {
        InetSocketAddress serverSocketAddress = new InetSocketAddress
                (InetAddress.getLoopbackAddress(), 0);
        InetSocketAddress clientSocketAddress = new InetSocketAddress
                (InetAddress.getLoopbackAddress(), 0);

        try (DatagramSocket serverSocket = new DatagramSocket(serverSocketAddress);
                DatagramSocket clientSocket = new DatagramSocket(clientSocketAddress)) {

            serverSocket.setSoTimeout(SOCKET_TIMEOUT);
            clientSocket.setSoTimeout(SOCKET_TIMEOUT);

            InetSocketAddress serverSocketAddr = new InetSocketAddress(
                    InetAddress.getLoopbackAddress(), serverSocket.getLocalPort());

            InetSocketAddress clientSocketAddr = new InetSocketAddress(
                    InetAddress.getLoopbackAddress(), clientSocket.getLocalPort());

            ExecutorService pool = Executors.newFixedThreadPool(2);
            Future<String> server, client;

            try {
                server = pool.submit(new ServerCallable(
                        testCase, serverSocket, clientSocketAddr));
                client = pool.submit(new ClientCallable(
                        testCase, clientSocket, serverSocketAddr));
            } finally {
                pool.shutdown();
            }

            boolean failed = false;

            // wait for client to finish
            try {
                System.out.println("Client finished: " + client.get());
            } catch (CancellationException | InterruptedException
                        | ExecutionException e) {
                System.out.println("Exception on client side: ");
                e.printStackTrace(System.out);
                failed = true;
            }

            // wait for server to finish
            try {
                System.out.println("Client finished: " + server.get());
            } catch (CancellationException | InterruptedException
                        | ExecutionException e) {
                System.out.println("Exception on server side: ");
                e.printStackTrace(System.out);
                failed = true;
            }

            if (failed) {
                throw new RuntimeException("Test failed");
            }
        }
    }

    record ServerCallable(DTLSOverDatagram testCase, DatagramSocket socket,
                          InetSocketAddress clientSocketAddr) implements Callable<String> {

        @Override
        public String call() throws Exception {
            try {
                testCase.doServerSide(socket, clientSocketAddr);
            } catch (Exception e) {
                System.out.println("Exception in  ServerCallable.call():");
                e.printStackTrace(System.out);
                serverException = e;

                if (testCase.isGoodJob()) {
                    throw e;
                } else {
                    return "Well done, server!";
                }
            }

            if (testCase.isGoodJob()) {
                return "Well done, server!";
            } else {
                throw new Exception("No expected exception");
            }
        }
    }

    record ClientCallable(DTLSOverDatagram testCase, DatagramSocket socket,
                          InetSocketAddress serverSocketAddr) implements Callable<String> {

        @Override
        public String call() throws Exception {
            try {
                testCase.doClientSide(socket, serverSocketAddr);
            } catch (Exception e) {
                System.out.println("Exception in ClientCallable.call():");
                e.printStackTrace(System.out);
                clientException = e;

                if (testCase.isGoodJob()) {
                    throw e;
                } else {
                    return "Well done, client!";
                }
            }

            if (testCase.isGoodJob()) {
                return "Well done, client!";
            } else {
                throw new Exception("No expected exception");
            }
        }
    }

    static void printHex(String prefix, ByteBuffer bb) {

        synchronized (System.out) {
            System.out.println(prefix);
            try {
                HexPrinter.simple().format(bb.slice());
            } catch (Exception e) {
                // ignore
            }
            System.out.flush();
        }
    }

    static void printHex(String prefix,
            byte[] bytes, int offset, int length) {

        synchronized (System.out) {
            System.out.println(prefix);
            try {
                HexPrinter.simple().format(bytes, offset, length);
            } catch (Exception e) {
                // ignore
            }
            System.out.flush();
        }
    }

    static void log(String side, String message) {
        System.out.println(side + ": " + message);
    }
}
