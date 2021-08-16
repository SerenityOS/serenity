/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6618387
 * @summary SSL client sessions do not close cleanly. A TCP reset occurs
 *      instead of a close_notify alert.
 * @run main/othervm CloseKeepAliveCached
 *
 *     SunJSSE does not support dynamic system properties, no way to re-use
 *     system properties in samevm/agentvm mode.
 *
 * @ignore
 *    After run the test manually, at the end of the debug output,
 *    if "MainThread, called close()" found, the test passed. Otherwise,
 *    if "Keep-Alive-Timer: called close()", the test failed.
 */

import java.net.*;
import java.util.*;
import java.io.*;
import javax.net.ssl.*;

public class CloseKeepAliveCached {
    static Map cookies;
    ServerSocket ss;

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
    static String pathToStores = "../../../../../../javax/net/ssl/etc";
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

    private SSLServerSocket sslServerSocket = null;

    /*
     * Define the server side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doServerSide() throws Exception {
        SSLServerSocketFactory sslssf =
            (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        sslServerSocket =
            (SSLServerSocket) sslssf.createServerSocket(serverPort);
        serverPort = sslServerSocket.getLocalPort();

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady = true;
        SSLSocket sslSocket = null;
        try {
            sslSocket = (SSLSocket) sslServerSocket.accept();
            for (int i = 0; i < 3 && !sslSocket.isClosed(); i++) {
                // read request
                InputStream is = sslSocket.getInputStream ();

                BufferedReader r = new BufferedReader(
                                                new InputStreamReader(is));
                String x;
                while ((x=r.readLine()) != null) {
                    if (x.length() ==0) {
                        break;
                    }
                }


                PrintStream out = new PrintStream(
                                 new BufferedOutputStream(
                                    sslSocket.getOutputStream() ));

                /* send the header */
                out.print("HTTP/1.1 200 OK\r\n");
                out.print("Keep-Alive: timeout=15, max=100\r\n");
                out.print("Connection: Keep-Alive\r\n");
                out.print("Content-Type: text/html; charset=iso-8859-1\r\n");
                out.print("Content-Length: 9\r\n");
                out.print("\r\n");
                out.print("Testing\r\n");
                out.flush();

                Thread.sleep(50);
            }
            sslSocket.close();
            sslServerSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
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

        HostnameVerifier reservedHV =
            HttpsURLConnection.getDefaultHostnameVerifier();
        try {
            HttpsURLConnection http = null;

            /* establish http connection to server */
            URL url = new URL("https://localhost:" + serverPort+"/");
            HttpsURLConnection.setDefaultHostnameVerifier(new NameVerifier());
            http = (HttpsURLConnection)url.openConnection();
            InputStream is = http.getInputStream ();
            while (is.read() != -1);
            is.close();

            url = new URL("https://localhost:" + serverPort+"/");
            http = (HttpsURLConnection)url.openConnection();
            is = http.getInputStream ();
            while (is.read() != -1);

            // if inputstream.close() called, the http.disconnect() will
            // not able to close the cached connection. If application
            // wanna close the keep-alive cached connection immediately
            // with httpURLConnection.disconnect(), they should not call
            // inputstream.close() explicitly, the
            // httpURLConnection.disconnect() will do it internally.
            // is.close();

            // close the connection, sending close_notify to peer.
            // otherwise, the connection will be closed by Finalizer or
            // Keep-Alive-Timer if timeout.
            http.disconnect();
            // Thread.sleep(5000);
        } catch (IOException ioex) {
            if (sslServerSocket != null)
                sslServerSocket.close();
            throw ioex;
        } finally {
            HttpsURLConnection.setDefaultHostnameVerifier(reservedHV);
        }
    }

    static class NameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
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

    public static void main(String args[]) throws Exception {
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

        if (debug)
            System.setProperty("javax.net.debug", "all");

        /*
         * Start the tests.
         */
        new CloseKeepAliveCached();
    }

    Thread clientThread = null;
    Thread serverThread = null;
    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    CloseKeepAliveCached() throws Exception {
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
        if (serverException != null)
            throw serverException;
        if (clientException != null)
            throw clientException;
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
