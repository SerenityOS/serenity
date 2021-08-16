/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8184328 8253368 8260923
 * @summary JDK8u131-b34-socketRead0 hang at SSL read
 * @run main/othervm SSLSocketCloseHang TLSv1.2
 * @run main/othervm SSLSocketCloseHang TLSv1.2 shutdownInput
 * @run main/othervm SSLSocketCloseHang TLSv1.2 shutdownOutput
 * @run main/othervm SSLSocketCloseHang TLSv1.3
 * @run main/othervm SSLSocketCloseHang TLSv1.3 shutdownInput
 * @run main/othervm SSLSocketCloseHang TLSv1.3 shutdownOutput
 */


import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;
import javax.net.ssl.*;

public class SSLSocketCloseHang {
    /*
     * =============================================================
     * Set the various variables needed for the tests, then
     * specify what tests to run on each side.
     */

    /*
     * Should we run the client or server in a separate thread?
     * Both sides can throw exceptions, but do you have a preference
     * as to which side should be the main thread.
     */
    static boolean separateServerThread = true;

    /*
     * Where do we find the keystores?
     */
    static String pathToStores = "../../../../javax/net/ssl/etc";
    static String keyStoreFile = "keystore";
    static String trustStoreFile = "truststore";
    static String passwd = "passphrase";

    /*
     * Is the server ready to serve?
     */
    volatile static boolean serverReady = false;

    /*
     * Was the client responsible for closing the socket
     */
    volatile static boolean clientClosed = false;

    /*
     * Turn on SSL debugging?
     */
    static boolean debug = false;

    static String socketCloseType;

    /*
     * If the client or server is doing some kind of object creation
     * that the other side depends on, and that thread prematurely
     * exits, you may experience a hang.  The test harness will
     * terminate all hung threads after its timeout has expired,
     * currently 3 minutes by default, but you might try to be
     * smart about it....
     */

    /*
     * Define the server side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doServerSide() throws Exception {
        SSLServerSocketFactory sslssf =
            (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket sslServerSocket =
            (SSLServerSocket) sslssf.createServerSocket(serverPort);

        serverPort = sslServerSocket.getLocalPort();

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady = true;

        System.err.println("Server accepting: " + System.nanoTime());
        SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
        System.err.println("Server accepted: " + System.nanoTime());
        sslSocket.startHandshake();
        System.err.println("Server handshake complete: " + System.nanoTime());
        while (!clientClosed) {
            Thread.sleep(500);
        }
    }

    /*
     * Define the client side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doClientSide() throws Exception {
        boolean caught = false;

        /*
         * Wait for server to get started.
         */
        System.out.println("waiting on server");
        while (!serverReady) {
            Thread.sleep(50);
        }
        Thread.sleep(500);
        System.out.println("server ready");

        Socket baseSocket = new Socket("localhost", serverPort);
        baseSocket.setSoTimeout(1000);

        SSLSocketFactory sslsf =
            (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket)
            sslsf.createSocket(baseSocket, "localhost", serverPort, false);

        // handshaking
        System.err.println("Client starting handshake: " + System.nanoTime());
        sslSocket.startHandshake();
        System.err.println("Client handshake done: " + System.nanoTime());

        Thread.sleep(500);
        System.err.println("Client closing: " + System.nanoTime());

        closeConnection(sslSocket);

        clientClosed = true;
        System.err.println("Client closed: " + System.nanoTime());
    }

    private void closeConnection(SSLSocket sslSocket) throws IOException {
        if ("shutdownInput".equals(socketCloseType)) {
            shutdownInput(sslSocket);
            // second call to shutdownInput() should just return,
            // shouldn't throw any exception
            sslSocket.shutdownInput();
            // invoking shutdownOutput() just after shutdownInput()
            sslSocket.shutdownOutput();
        } else if ("shutdownOutput".equals(socketCloseType)) {
            sslSocket.shutdownOutput();
            // second call to shutdownInput() should just return,
            // shouldn't throw any exception
            sslSocket.shutdownOutput();
            // invoking shutdownInput() just after shutdownOutput()
            shutdownInput(sslSocket);
        } else {
            sslSocket.close();
        }
    }

    private void shutdownInput(SSLSocket sslSocket) throws IOException {
        try {
            sslSocket.shutdownInput();
        } catch (SSLException e) {
            if (!e.getMessage().contains
                    ("closing inbound before receiving peer's close_notify")) {
                throw new RuntimeException("expected different exception "
                        + "message. " + e.getMessage());
            }
        }
        if (!sslSocket.getSession().isValid()) {
            throw new RuntimeException("expected session to remain valid");
        }
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */

    // use any free port by default
    volatile int serverPort = 0;

    volatile Exception serverException = null;
    volatile Exception clientException = null;

    volatile byte[] serverDigest = null;

    public static void main(String[] args) throws Exception {
        String keyFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + keyStoreFile;
        String trustFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + trustStoreFile;

        System.setProperty("javax.net.ssl.keyStore", keyFilename);
        System.setProperty("javax.net.ssl.keyStorePassword", passwd);
        System.setProperty("javax.net.ssl.trustStore", trustFilename);
        System.setProperty("javax.net.ssl.trustStorePassword", passwd);
        System.setProperty("jdk.tls.client.protocols", args[0]);

        if (debug)
            System.setProperty("javax.net.debug", "all");

        socketCloseType = args.length > 1 ? args[1] : "";


        /*
         * Start the tests.
         */
        new SSLSocketCloseHang();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    SSLSocketCloseHang() throws Exception {
        if (separateServerThread) {
            startServer(true);
            startClient(false);
        } else {
            startClient(true);
            startServer(false);
        }

        /*
         * Wait for other side to close down.
         */
        if (separateServerThread) {
            serverThread.join();
        } else {
            clientThread.join();
        }

        /*
         * When we get here, the test is pretty much over.
         *
         * If the main thread excepted, that propagates back
         * immediately.  If the other thread threw an exception, we
         * should report back.
         */
        if (serverException != null) {
            System.out.print("Server Exception:");
            throw serverException;
        }
        if (clientException != null) {
            System.out.print("Client Exception:");
            throw clientException;
        }
    }

    void startServer(boolean newThread) throws Exception {
        if (newThread) {
            serverThread = new Thread() {
                public void run() {
                    try {
                        doServerSide();
                    } catch (Exception e) {
                        /*
                         * Our server thread just died.
                         *
                         * Release the client, if not active already...
                         */
                        System.err.println("Server died...");
                        System.err.println(e);
                        serverReady = true;
                        serverException = e;
                    }
                }
            };
            serverThread.start();
        } else {
            doServerSide();
        }
    }

    void startClient(boolean newThread) throws Exception {
        if (newThread) {
            clientThread = new Thread() {
                public void run() {
                    try {
                        doClientSide();
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
            doClientSide();
        }
    }
}
