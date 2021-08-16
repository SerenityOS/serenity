/*
 * Copyright (c) 2004, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8133632
 * @summary javax.net.ssl.SSLEngine does not properly handle received
 *      SSL fatal alerts
 * @ignore the dependent implementation details are changed
 * @run main/othervm EngineCloseOnAlert
 */

import java.io.FileInputStream;
import java.io.IOException;
import javax.net.ssl.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.security.*;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;

public class EngineCloseOnAlert {

    private static final String pathToStores = "../etc";
    private static final String keyStoreFile = "keystore";
    private static final String trustStoreFile = "truststore";
    private static final String passwd = "passphrase";
    private static final String keyFilename =
            System.getProperty("test.src", ".") + "/" + pathToStores +
                "/" + keyStoreFile;
    private static final String trustFilename =
            System.getProperty("test.src", ".") + "/" + pathToStores +
                "/" + trustStoreFile;

    private static KeyManagerFactory KMF;
    private static TrustManagerFactory TMF;
    private static TrustManagerFactory EMPTY_TMF;

    private static final String[] TLS10ONLY = { "TLSv1" };
    private static final String[] TLS12ONLY = { "TLSv1.2" };
    private static final String[] ONECIPHER =
            { "TLS_RSA_WITH_AES_128_CBC_SHA" };

    public interface TestCase {
        public void runTest() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        int failed = 0;
        List<TestCase> testMatrix = new LinkedList<TestCase>() {{
            add(clientReceivesAlert);
            add(serverReceivesAlert);
        }};

        // Create the various key/trust manager factories we'll need
        createManagerFactories();

        for (TestCase test : testMatrix) {
            try {
                test.runTest();
            } catch (Exception e) {
                System.out.println("Exception in test:\n" + e);
                e.printStackTrace(System.out);
                failed++;
            }
        }

        System.out.println("Total tests: " + testMatrix.size() + ", passed: " +
                (testMatrix.size() - failed) + ", failed: " + failed);
        if (failed > 0) {
            throw new RuntimeException("One or more tests failed.");
        }
    }

    private static final TestCase clientReceivesAlert = new TestCase() {
        @Override
        public void runTest() throws Exception {
            System.out.println("");
            System.out.println("=======================================");
            System.out.println("Test: Client receives alert from server");
            System.out.println("=======================================");

            // For this test, we won't initialize any keystore so the
            // server will throw an exception because it has no key/cert to
            // match the requested ciphers offered by the client.  This
            // will generate an alert from the server to the client.

            SSLContext context = SSLContext.getDefault();
            SSLEngine client = context.createSSLEngine();
            SSLEngine server = context.createSSLEngine();
            client.setUseClientMode(true);
            server.setUseClientMode(false);
            SSLEngineResult clientResult;
            SSLEngineResult serverResult;

            ByteBuffer raw = ByteBuffer.allocate(32768);
            ByteBuffer plain = ByteBuffer.allocate(32768);

            // Generate the client hello and have the server unwrap it
            client.wrap(plain, raw);
            checkEngineState(client, NEED_UNWRAP, false, false);
            raw.flip();
            System.out.println("Client-to-Server:\n-----------------\n" +
                    dumpHexBytes(raw, 16, "\n", ":"));


            // The server should need to run a delegated task while processing
            // the client hello data.
            serverResult = server.unwrap(raw, plain);
            checkEngineState(server, NEED_TASK, false, false);
            System.out.println("Server result: " + serverResult);
            runDelegatedTasks(serverResult, server);
            checkEngineState(server, NEED_WRAP, true, false);

            try {
                raw.clear();
                serverResult = server.wrap(plain, raw);
                System.out.println("Server result: " + serverResult);
                runDelegatedTasks(serverResult, server);
            } catch (SSLException e) {
                // This is the expected code path
                System.out.println("Server throws exception: " + e);
                System.out.println("Server engine state: " +
                        "isInboundDone = "+ server.isInboundDone() +
                        ", isOutboundDone = " + server.isOutboundDone() +
                        ", handshake status = " + server.getHandshakeStatus());
                checkEngineState(server, NEED_WRAP, true, false);
            }
            raw.clear();

            // The above should show that isInboundDone returns true, and
            // handshake status is NEED_WRAP. That is the correct behavior,
            // wrap will put a fatal alert message in the buffer.
            serverResult = server.wrap(plain, raw);
            System.out.println("Server result (wrap after exception): " +
                    serverResult);
            System.out.println("Server engine closure state: isInboundDone="
                    + server.isInboundDone() + ", isOutboundDone="
                    + server.isOutboundDone());
            checkEngineState(server, NEED_UNWRAP, true, true);
            raw.flip();

            System.out.println("Server-to-Client:\n-----------------\n" +
                    dumpHexBytes(raw, 16, "\n", ":"));

            // Client side will read the fatal alert and throw exception.
            try {
                clientResult = client.unwrap(raw, plain);
                System.out.println("Client result (unwrap alert): " +
                    clientResult);
            } catch (SSLException e) {
                System.out.println("Client throws exception: " + e);
                System.out.println("Engine closure status: isInboundDone="
                        + client.isInboundDone() + ", isOutboundDone="
                        + client.isOutboundDone() + ", handshake status="
                        + client.getHandshakeStatus());
                checkEngineState(client, NOT_HANDSHAKING, true, true);
            }
            raw.clear();

            // Last test, we try to unwrap
            clientResult = client.unwrap(raw, plain);
            checkEngineState(client, NOT_HANDSHAKING, true, true);
            System.out.println("Client result (wrap after exception): " +
                    clientResult);
        }
    };

    private static final TestCase serverReceivesAlert = new TestCase() {
        @Override
        public void runTest() throws Exception {
            SSLContext cliContext = SSLContext.getDefault();
            SSLContext servContext = SSLContext.getInstance("TLS");
            servContext.init(KMF.getKeyManagers(), TMF.getTrustManagers(),
                    null);
            SSLEngine client = cliContext.createSSLEngine();
            SSLEngine server = servContext.createSSLEngine();
            client.setUseClientMode(true);
            client.setEnabledProtocols(TLS12ONLY);
            client.setEnabledCipherSuites(ONECIPHER);
            server.setUseClientMode(false);
            server.setEnabledProtocols(TLS10ONLY);
            SSLEngineResult clientResult;
            SSLEngineResult serverResult;
            ByteBuffer raw = ByteBuffer.allocate(32768);
            ByteBuffer plain = ByteBuffer.allocate(32768);

            System.out.println("");
            System.out.println("=======================================");
            System.out.println("Test: Server receives alert from client");
            System.out.println("=======================================");

            // Generate the client hello and have the server unwrap it
            checkEngineState(client, NOT_HANDSHAKING, false, false);
            client.wrap(plain, raw);
            checkEngineState(client, NEED_UNWRAP, false, false);
            raw.flip();
            System.out.println("Client-to-Server:\n-----------------\n" +
                    dumpHexBytes(raw, 16, "\n", ":"));

            // The server should need to run a delegated task while processing
            // the client hello data.
            serverResult = server.unwrap(raw, plain);
            checkEngineState(server, NEED_TASK, false, false);
            runDelegatedTasks(serverResult, server);
            checkEngineState(server, NEED_WRAP, false, false);
            raw.compact();

            // The server should now wrap the response back to the client
            server.wrap(plain, raw);
            checkEngineState(server, NEED_UNWRAP, false, false);
            raw.flip();
            System.out.println("Server-to-Client:\n-----------------\n" +
                    dumpHexBytes(raw, 16, "\n", ":"));

            // The client should parse this and throw an exception because
            // It is unwiling to do TLS 1.0
            clientResult = client.unwrap(raw, plain);
            checkEngineState(client, NEED_TASK, false, false);
            runDelegatedTasks(clientResult, client);
            checkEngineState(client, NEED_UNWRAP, false, false);

            try {
                client.unwrap(raw, plain);
            } catch (SSLException e) {
                System.out.println("Client throws exception: " + e);
                System.out.println("Engine closure status: isInboundDone="
                        + client.isInboundDone() + ", isOutboundDone="
                        + client.isOutboundDone() + ", handshake status="
                        + client.getHandshakeStatus());
                checkEngineState(client, NEED_WRAP, true, false);
            }
            raw.clear();

            // Now the client should wrap the exception
            client.wrap(plain, raw);
            checkEngineState(client, NEED_UNWRAP, true, true);
            raw.flip();
            System.out.println("Client-to-Server:\n-----------------\n" +
                    dumpHexBytes(raw, 16, "\n", ":"));

            try {
                server.unwrap(raw, plain);
                checkEngineState(server, NEED_UNWRAP, false, false);
            } catch (SSLException e) {
                System.out.println("Server throws exception: " + e);
                System.out.println("Engine closure status: isInboundDone="
                        + server.isInboundDone() + ", isOutboundDone="
                        + server.isOutboundDone() + ", handshake status="
                        + server.getHandshakeStatus());
                checkEngineState(server, NOT_HANDSHAKING, true, true);
            }
            raw.clear();
        }
    };


    /*
     * If the result indicates that we have outstanding tasks to do,
     * go ahead and run them in this thread.
     */
    private static void runDelegatedTasks(SSLEngineResult result,
            SSLEngine engine) throws Exception {

        if (result.getHandshakeStatus() ==
                SSLEngineResult.HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = engine.getDelegatedTask()) != null) {
                System.out.println("\trunning delegated task...");
                runnable.run();
            }
            SSLEngineResult.HandshakeStatus hsStatus =
                    engine.getHandshakeStatus();
            if (hsStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                throw new Exception(
                    "handshake shouldn't need additional tasks");
            }
            System.out.println("\tnew HandshakeStatus: " + hsStatus);
        }
    }

    /**
     *
     * @param data The array of bytes to dump to stdout.
     * @param itemsPerLine The number of bytes to display per line
     * if the {@code lineDelim} character is blank then all bytes will be
     * printed on a single line.
     * @param lineDelim The delimiter between lines
     * @param itemDelim The delimiter between bytes
     *
     * @return The hexdump of the byte array
     */
    private static String dumpHexBytes(ByteBuffer data, int itemsPerLine,
            String lineDelim, String itemDelim) {
        StringBuilder sb = new StringBuilder();

        if (data != null) {
            data.mark();
            for (int i = 0; i < data.limit(); i++) {
                if (i % itemsPerLine == 0 && i != 0) {
                    sb.append(lineDelim);
                }
                sb.append(String.format("%02X", data.get(i)));
                if (i % itemsPerLine != (itemsPerLine - 1) &&
                        i != (data.limit() -1)) {
                    sb.append(itemDelim);
                }
            }
            data.reset();
        }

        return sb.toString();
    }

    private static void createManagerFactories()
            throws GeneralSecurityException, IOException {
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        KeyStore truststore = KeyStore.getInstance("PKCS12");
        KeyStore empty_ts = KeyStore.getInstance("PKCS12");
        char[] passphrase = passwd.toCharArray();

        keystore.load(new FileInputStream(keyFilename), passphrase);
        truststore.load(new FileInputStream(trustFilename), passphrase);
        empty_ts.load(null, "".toCharArray());

        KMF = KeyManagerFactory.getInstance("PKIX");
        KMF.init(keystore, passphrase);
        TMF = TrustManagerFactory.getInstance("PKIX");
        TMF.init(truststore);
        EMPTY_TMF = TrustManagerFactory.getInstance("PKIX");
        EMPTY_TMF.init(truststore);
    }

    private static void checkEngineState(SSLEngine engine,
            SSLEngineResult.HandshakeStatus expectedHSStat,
            boolean expectedInboundDone, boolean expectedOutboundDone) {
        if (engine.getHandshakeStatus() != expectedHSStat ||
                engine.isInboundDone() != expectedInboundDone ||
                engine.isOutboundDone() != expectedOutboundDone) {
            throw new RuntimeException("Error: engine not in expected state\n" +
                    "Expected: state = " + expectedHSStat +
                    ", inDone = " + expectedInboundDone +
                    ", outDone = " + expectedOutboundDone + "\n" +
                    "Actual: state = " + engine.getHandshakeStatus() +
                    ", inDone = " + engine.isInboundDone() +
                    ", outDone = " + engine.isOutboundDone());
        } else {
            System.out.println((engine.getUseClientMode() ?
                    "Client" : "Server") + " handshake status: " +
                    engine.getHandshakeStatus() + ", inDone = " +
                    engine.isInboundDone() + ", outDone = " +
                    engine.isOutboundDone());
        }
    }
}
