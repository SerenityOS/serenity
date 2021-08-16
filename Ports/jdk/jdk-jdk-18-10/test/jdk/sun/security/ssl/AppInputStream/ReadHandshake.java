/*
 * Copyright (c) 2001, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4514971
 * @summary Verify applications do not read handshake data after failure
 * @run main/othervm ReadHandshake
 */

import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.security.Security;

public class ReadHandshake {

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

    // Note: we use anonymous ciphersuites only, no keys/ trusted certs needed

    private final static String[] CLIENT_SUITES = new String[] {
        "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
    };

    private final static String[] SERVER_SUITES = new String[] {
        "SSL_DH_anon_WITH_RC4_128_MD5",
    };

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
        SSLSocket sslSocket = null;
        SSLServerSocket sslServerSocket = null;
        try {
            SSLServerSocketFactory sslssf =
                (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            sslServerSocket =
                (SSLServerSocket) sslssf.createServerSocket(serverPort);
            serverPort = sslServerSocket.getLocalPort();

            sslServerSocket.setEnabledCipherSuites(SERVER_SUITES);

            /*
             * Signal Client, we're ready for his connect.
             */
            serverReady = true;

            System.out.println("Server waiting for connection");

            sslSocket = (SSLSocket) sslServerSocket.accept();
            InputStream sslIS = sslSocket.getInputStream();
            OutputStream sslOS = sslSocket.getOutputStream();

            System.out.println("Server starting handshake...");


            try {
                sslIS.read();
                throw new Exception("No handshake exception on server side");
            } catch (IOException e) {
                System.out.println("Handshake failed on server side, OK");
            }

            for (int i = 0; i < 3; i++) {
                try {
                    int ch;
                    if ((ch = sslIS.read()) != -1) {
                        throw new Exception("Read succeeded server side: "
                            + ch);
                    }
                } catch (IOException e) {
                    System.out.println("Exception for read() on server, OK");
                }
            }

        } finally {
            closeSocket(sslSocket);
            closeSocket(sslServerSocket);
        }
    }

    private static void closeSocket(Socket s) {
        try {
            if (s != null) {
                s.close();
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private static void closeSocket(ServerSocket s) {
        try {
            if (s != null) {
                s.close();
            }
        } catch (Exception e) {
            // ignore
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
            Thread.sleep(80);
        }

        SSLSocket sslSocket = null;
        try {

            SSLSocketFactory sslsf =
                (SSLSocketFactory) SSLSocketFactory.getDefault();
            sslSocket = (SSLSocket)
                sslsf.createSocket("localhost", serverPort);
            sslSocket.setEnabledCipherSuites(CLIENT_SUITES);

            InputStream sslIS = sslSocket.getInputStream();
            OutputStream sslOS = sslSocket.getOutputStream();

            System.out.println("Client starting handshake...");

            try {
                sslIS.read();
                throw new Exception("No handshake exception on client side");
            } catch (IOException e) {
                System.out.println("Handshake failed on client side, OK");
            }

            for (int i = 0; i < 3; i++) {
                try {
                    int ch;
                    if ((ch = sslIS.read()) != -1) {
                        throw new Exception("Read succeeded on client side: "
                            + ch);
                    }
                } catch (IOException e) {
                    System.out.println("Exception for read() on client, OK");
                }
            }
        } finally {
            sslSocket.close();
        }
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */

    volatile int serverPort = 0;

    volatile Exception serverException = null;
    volatile Exception clientException = null;

    public static void main(String[] args) throws Exception {
        // reset security properties to make sure that the algorithms
        // and keys used in this test are not disabled.
        Security.setProperty("jdk.tls.disabledAlgorithms", "");
        Security.setProperty("jdk.certpath.disabledAlgorithms", "");

        if (debug)
            System.setProperty("javax.net.debug", "all");

        /*
         * Start the tests.
         */
        new ReadHandshake();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    ReadHandshake() throws Exception {
        startServer(true);
        startClient(true);

        serverThread.join();
        clientThread.join();

        /*
         * When we get here, the test is pretty much over.
         *
         * If the main thread excepted, that propagates back
         * immediately.  If the other thread threw an exception, we
         * should report back.
         */
        if (serverException != null) {
            if (clientException != null) {
                System.out.println("Client exception:");
                clientException.printStackTrace(System.out);
            }
            throw serverException;
        }
        if (clientException != null) {
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
