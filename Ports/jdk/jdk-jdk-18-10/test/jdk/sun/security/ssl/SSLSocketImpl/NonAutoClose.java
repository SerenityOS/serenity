/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4404399
 * @ignore this test does not work any more as the TLS spec changes the
 *         behaviors of close_notify.
 * @summary When a layered SSL socket is closed, it should wait for close_notify
 * @run main/othervm NonAutoClose
 * @author Brad Wetmore
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;


public class NonAutoClose {
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
    private static boolean separateServerThread = true;

    /*
     * Where do we find the keystores?
     */
    private final static String pathToStores = "../../../../javax/net/ssl/etc";
    private final static String keyStoreFile = "keystore";
    private final static String trustStoreFile = "truststore";
    private final static String passwd = "passphrase";
    private final static char[] cpasswd = "passphrase".toCharArray();

    /*
     * Is the server ready to serve?
     */
    volatile static boolean serverReady = false;

    /*
     * Turn on SSL debugging?
     */
    private final static boolean DEBUG = false;
    private final static boolean VERBOSE = true;
    private final static int NUM_ITERATIONS  = 10;
    private final static int PLAIN_SERVER_VAL = 1;
    private final static int PLAIN_CLIENT_VAL = 2;
    private final static int TLS_SERVER_VAL = 3;
    private final static int TLS_CLIENT_VAL = 4;

    /*
     * If the client or server is doing some kind of object creation
     * that the other side depends on, and that thread prematurely
     * exits, you may experience a hang.  The test harness will
     * terminate all hung threads after its timeout has expired,
     * currently 3 minutes by default, but you might try to be
     * smart about it....
     */

    void expectValue(int got, int expected, String msg) throws IOException {
        if (VERBOSE) {
            System.err.println(msg + ": read (" + got + ")");
        }
        if (got != expected) {
            throw new IOException(msg + ": read (" + got
                + ") but expecting(" + expected + ")");
        }
    }


    /*
     * Define the server side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */

     void doServerSide() throws Exception {
        if (VERBOSE) {
            System.err.println("Starting server");
        }

        /*
         * Setup the SSL stuff
         */
        SSLSocketFactory sslsf =
             (SSLSocketFactory) SSLSocketFactory.getDefault();

        ServerSocket serverSocket = new ServerSocket(SERVER_PORT);

        SERVER_PORT = serverSocket.getLocalPort();

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady = true;

        Socket plainSocket = serverSocket.accept();
        InputStream is = plainSocket.getInputStream();
        OutputStream os = plainSocket.getOutputStream();

        expectValue(is.read(), PLAIN_CLIENT_VAL, "Server");

        os.write(PLAIN_SERVER_VAL);
        os.flush();

        for (int i = 1; i <= NUM_ITERATIONS; i++) {
            if (VERBOSE) {
                System.err.println("=================================");
                System.err.println("Server Iteration #" + i);
            }

            SSLSocket ssls = (SSLSocket) sslsf.createSocket(plainSocket,
                SERVER_NAME, plainSocket.getPort(), false);

            ssls.setUseClientMode(false);
            InputStream sslis = ssls.getInputStream();
            OutputStream sslos = ssls.getOutputStream();

            expectValue(sslis.read(), TLS_CLIENT_VAL, "Server");

            sslos.write(TLS_SERVER_VAL);
            sslos.flush();

            sslis.close();
            sslos.close();
            ssls.close();

            if (VERBOSE) {
                System.err.println("TLS socket is closed");
            }
        }

        expectValue(is.read(), PLAIN_CLIENT_VAL, "Server");

        os.write(PLAIN_SERVER_VAL);
        os.flush();

        is.close();
        os.close();
        plainSocket.close();

        if (VERBOSE) {
            System.err.println("Server plain socket is closed");
        }
    }

    /*
     * Define the client side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    private void doClientSide() throws Exception {
        /*
         * Wait for server to get started.
         */
        while (!serverReady) {
            Thread.sleep(50);
        }

        if (VERBOSE) {
            System.err.println("Starting client");
        }

        /*
         * Setup the SSL stuff
         */
        SSLSocketFactory sslsf =
             (SSLSocketFactory) SSLSocketFactory.getDefault();

        Socket plainSocket = new Socket(SERVER_NAME, SERVER_PORT);
        InputStream is = plainSocket.getInputStream();
        OutputStream os = plainSocket.getOutputStream();

        os.write(PLAIN_CLIENT_VAL);
        os.flush();

        expectValue(is.read(), PLAIN_SERVER_VAL, "Client");

        for (int i = 1; i <= NUM_ITERATIONS; i++) {
            if (VERBOSE) {
                System.err.println("===================================");
                System.err.println("Client Iteration #" + i);
              }

            SSLSocket ssls = (SSLSocket) sslsf.createSocket(plainSocket,
               SERVER_NAME, plainSocket.getPort(), false);

            ssls.setUseClientMode(true);

            InputStream sslis = ssls.getInputStream();
            OutputStream sslos = ssls.getOutputStream();

            sslos.write(TLS_CLIENT_VAL);
            sslos.flush();

            expectValue(sslis.read(), TLS_SERVER_VAL, "Client");

            sslis.close();
            sslos.close();
            ssls.close();

            if (VERBOSE) {
                System.err.println("Client TLS socket is closed");
            }
        }

        os.write(PLAIN_CLIENT_VAL);
        os.flush();

        expectValue(is.read(), PLAIN_SERVER_VAL, "Client");

        is.close();
        os.close();
        plainSocket.close();

        if (VERBOSE) {
            System.err.println("Client plain socket is closed");
        }
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */

    private volatile int SERVER_PORT = 0;
    private final static String SERVER_NAME = "localhost";

    private volatile Exception serverException = null;
    private volatile Exception clientException = null;

    private final static String keyFilename =
        System.getProperty("test.src", ".") + "/" + pathToStores +
        "/" + keyStoreFile;
    private final static String trustFilename =
        System.getProperty("test.src", ".") + "/" + pathToStores +
        "/" + trustStoreFile;


   // Used for running test standalone
    public static void main(String[] args) throws Exception {
        System.setProperty("javax.net.ssl.keyStore", keyFilename);
        System.setProperty("javax.net.ssl.keyStorePassword", passwd);
        System.setProperty("javax.net.ssl.trustStore", trustFilename);
        System.setProperty("javax.net.ssl.trustStorePassword", passwd);

        if (DEBUG)
            System.setProperty("javax.net.debug", "all");

        /*
         * Start the tests.
         */
        new NonAutoClose();
    }

    private Thread clientThread = null;
    private Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    NonAutoClose() throws Exception {
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
            System.err.print("Server Exception:");
            throw serverException;
        }
        if (clientException != null) {
            System.err.print("Client Exception:");
            throw clientException;
        }
    }

    private void startServer(boolean newThread) throws Exception {
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

    private void startClient(boolean newThread) throws Exception {
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
