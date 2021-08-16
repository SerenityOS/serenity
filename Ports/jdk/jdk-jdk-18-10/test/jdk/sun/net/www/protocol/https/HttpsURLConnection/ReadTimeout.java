/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4811482 4700777 4905410
 * @summary sun.net.client.defaultConnectTimeout should work with
 *     HttpsURLConnection; HTTP client: Connect and read timeouts;
 *     Https needs to support new tiger features that went into http
 * @library /test/lib
 * @run main/othervm ReadTimeout
 */

import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import jdk.test.lib.net.URIBuilder;

public class ReadTimeout {

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

    /*
     * Message posted
     */
    static String postMsg = "Testing HTTP post on a https server";

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
        InetAddress loopback = InetAddress.getLoopbackAddress();
        SSLServerSocketFactory sslssf =
            (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket sslServerSocket =
            (SSLServerSocket) sslssf.createServerSocket(serverPort, 0, loopback);
        serverPort = sslServerSocket.getLocalPort();

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady = true;
        try {
            try (SSLSocket sslSocket = (SSLSocket)sslServerSocket.accept()) {
                InputStream sslIS = sslSocket.getInputStream();
                BufferedReader br =
                        new BufferedReader(new InputStreamReader(sslIS));
                br.readLine();
                while (!finished())  {
                    Thread.sleep(2000);
                }
            }

            reset();
            // doing second test
            try (SSLSocket sslSocket = (SSLSocket)sslServerSocket.accept()) {
                InputStream sslIS = sslSocket.getInputStream();
                BufferedReader br =
                        new BufferedReader(new InputStreamReader(sslIS));
                br.readLine();
                while (!finished())  {
                    Thread.sleep(2000);
                }
            }
        } catch (Exception e) {
            System.out.println("Should be an expected exception: " + e);
        } finally {
            sslServerSocket.close();
        }
    }

    boolean isFinished = false;

    synchronized boolean finished () {
        return (isFinished);
    }
    synchronized void done () {
        isFinished = true;
    }

    synchronized void reset() {
        isFinished = false;
    }

    /*
     * Define the client side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doClientSide() throws Exception {
        HostnameVerifier reservedHV =
            HttpsURLConnection.getDefaultHostnameVerifier();
        try {
            /*
             * Wait for server to get started.
             */
            while (!serverReady) {
                Thread.sleep(50);
            }
            HttpsURLConnection http = null;
            try {
                URL url = URIBuilder.newBuilder()
                          .scheme("https")
                          .loopback()
                          .port(serverPort)
                          .toURL();

                // set read timeout through system property
                System.setProperty("sun.net.client.defaultReadTimeout", "2000");
                HttpsURLConnection.setDefaultHostnameVerifier(
                                          new NameVerifier());
                http = (HttpsURLConnection)url.openConnection();

                InputStream is = http.getInputStream();

                throw new Exception(
                        "system property timeout configuration does not work");
            } catch (SSLException | SocketTimeoutException ex) {
                System.out.println("Got expected timeout exception for " +
                        "system property timeout configuration: " + getCause(ex));
            } finally {
                done();
                http.disconnect();
            }

            try {
                URL url = URIBuilder.newBuilder()
                          .scheme("https")
                          .loopback()
                          .port(serverPort)
                          .toURL();

                HttpsURLConnection.setDefaultHostnameVerifier(
                                          new NameVerifier());
                http = (HttpsURLConnection)url.openConnection();
                // set read timeout through API
                http.setReadTimeout(2000);

                InputStream is = http.getInputStream();

                throw new Exception(
                        "HttpsURLConnection.setReadTimeout() does not work");
            } catch (SSLException | SocketTimeoutException ex) {
                System.out.println("Got expected timeout exception for " +
                        "HttpsURLConnection.setReadTimeout(): " + getCause(ex));
            } finally {
                done();
                http.disconnect();
            }
        } finally {
            HttpsURLConnection.setDefaultHostnameVerifier(reservedHV);
        }
    }

    private Exception getCause(Exception ex) {
        Exception cause = null;
        if (ex instanceof SSLException) {
            cause = (Exception) ex.getCause();
            if (!(cause instanceof SocketTimeoutException)) {
                throw new RuntimeException("Unexpected cause", cause);
            }
        } else {
            cause = ex;
        }

        return cause;
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

    private boolean sslConnectionFailed() {
        return clientException instanceof SSLHandshakeException;
    }

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

        if (debug)
            System.setProperty("javax.net.debug", "all");

        /*
         * Start the tests.
         */
        new ReadTimeout();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    ReadTimeout() throws Exception {
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
            if (!sslConnectionFailed()) {
                serverThread.join();
            }
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
