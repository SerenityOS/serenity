/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7188658
 * @summary Add possibility to disable client initiated renegotiation
 * @run main/othervm RejectClientRenego true SSLv3
 * @run main/othervm RejectClientRenego false SSLv3
 * @run main/othervm RejectClientRenego true TLSv1
 * @run main/othervm RejectClientRenego false TLSv1
 * @run main/othervm RejectClientRenego true TLSv1.1
 * @run main/othervm RejectClientRenego false TLSv1.1
 * @run main/othervm RejectClientRenego true TLSv1.2
 * @run main/othervm RejectClientRenego false TLSv1.2
 */

import java.io.*;
import java.net.*;
import java.security.Security;
import javax.net.ssl.*;

public class RejectClientRenego implements
        HandshakeCompletedListener {

    static byte handshakesCompleted = 0;

    /*
     * Define what happens when handshaking is completed
     */
    public void handshakeCompleted(HandshakeCompletedEvent event) {
        synchronized (this) {
            handshakesCompleted++;
            System.out.println("Session: " + event.getSession().toString());
            System.out.println("Seen handshake completed #" +
                handshakesCompleted);
        }
    }

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
    static boolean separateServerThread = false;

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
     * Turn on SSL debugging?
     */
    static boolean debug = false;

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

        SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
        sslSocket.setEnabledProtocols(new String[] { tlsProtocol });
        sslSocket.addHandshakeCompletedListener(this);
        InputStream sslIS = sslSocket.getInputStream();
        OutputStream sslOS = sslSocket.getOutputStream();

        for (int i = 0; i < 10; i++) {
            sslIS.read();
            sslOS.write(85);
            sslOS.flush();
        }

        try {
            for (int i = 0; i < 10; i++) {
                System.out.println("sending/receiving data, iteration: " + i);
                sslIS.read();
                sslOS.write(85);
                sslOS.flush();
            }
            throw new Exception("Not reject client initialized renegotiation");
        } catch (IOException ioe) {
            System.out.println("Got the expected exception");
        } finally {
            sslSocket.close();
        }
    }

    /*
     * Define the client side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doClientSide() throws Exception {

        /*
         * Wait for server to get started.
         */
        while (!serverReady) {
            Thread.sleep(50);
        }

        SSLSocketFactory sslsf =
            (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket)
            sslsf.createSocket("localhost", serverPort);
        sslSocket.setEnabledProtocols(new String[] { tlsProtocol });

        InputStream sslIS = sslSocket.getInputStream();
        OutputStream sslOS = sslSocket.getOutputStream();

        for (int i = 0; i < 10; i++) {
            sslOS.write(280);
            sslOS.flush();
            sslIS.read();
        }

        if (!isAbbreviated) {
            System.out.println("invalidating");
            sslSocket.getSession().invalidate();
        }
        System.out.println("starting new handshake");
        sslSocket.startHandshake();

        try {
            for (int i = 0; i < 10; i++) {
                sslOS.write(280);
                sslOS.flush();
                sslIS.read();
            }
            throw new Exception("Not reject client initialized renegotiation");
        } catch (IOException ioe) {
            System.out.println("Got the expected exception");
        } finally {
            sslSocket.close();
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

    // Is it abbreviated handshake?
    private static boolean isAbbreviated = false;

    // the specified protocol
    private static String tlsProtocol;

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

        // reject client initialized SSL renegotiation.
        System.setProperty(
            "jdk.tls.rejectClientInitiatedRenegotiation", "true");

        if (debug) {
            System.setProperty("javax.net.debug", "all");
        }

        Security.setProperty("jdk.tls.disabledAlgorithms", "");

        // Is it abbreviated handshake?
        if ("true".equals(args[0])) {
            isAbbreviated = true;
        }

        tlsProtocol = args[1];

        /*
         * Start the tests.
         */
        new RejectClientRenego();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    RejectClientRenego() throws Exception {
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
