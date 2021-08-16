/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8144566
 * @summary Custom HostnameVerifier disables SNI extension
 * @run main/othervm ImpactOnSNI
 */

import java.io.*;
import java.net.*;
import javax.net.ssl.*;

public class ImpactOnSNI {

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
    private static final boolean separateServerThread = true;

    /*
     * Where do we find the keystores?
     */
    private static final String pathToStores =
                                        "../../../../../../javax/net/ssl/etc";
    private static final String keyStoreFile = "keystore";
    private static final String trustStoreFile = "truststore";
    private static final String passwd = "passphrase";

    /*
     * Is the server ready to serve?
     */
    private static volatile boolean serverReady = false;

    /*
     * Is the connection ready to close?
     */
    private static volatile boolean closeReady = false;

    /*
     * Turn on SSL debugging?
     */
    private static final boolean debug = false;

    /*
     * Message posted
     */
    private static final String postMsg = "HTTP post on a https server";

    /*
     * the fully qualified domain name of localhost
     */
    private static String hostname = null;

    /*
     * If the client or server is doing some kind of object creation
     * that the other side depends on, and that thread prematurely
     * exits, you may experience a hang.  The test harness will
     * terminate all hung threads after its timeout has expired,
     * currently 3 minutes by default, but you might try to be
     * smart about it....
     */

    private SSLServerSocket createServerSocket(SSLServerSocketFactory sslssf)
        throws Exception {
        SSLServerSocket sslServerSocket =
            (SSLServerSocket)sslssf.createServerSocket();
        InetAddress localHost = InetAddress.getLocalHost();
        InetSocketAddress address = new InetSocketAddress(localHost, serverPort);
        sslServerSocket.bind(address);
        return sslServerSocket;
    }

    /*
     * Define the server side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    private void doServerSide() throws Exception {
        SSLServerSocketFactory sslssf =
            (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
        try (SSLServerSocket sslServerSocket = createServerSocket(sslssf)) {

            serverPort = sslServerSocket.getLocalPort();

            /*
             * Signal Client, we're ready for his connect.
             */
            serverReady = true;

            /*
             * Accept connections
             */
            try (SSLSocket sslSocket = (SSLSocket)sslServerSocket.accept()) {
                InputStream sslIS = sslSocket.getInputStream();
                OutputStream sslOS = sslSocket.getOutputStream();
                BufferedReader br =
                        new BufferedReader(new InputStreamReader(sslIS));
                PrintStream ps = new PrintStream(sslOS);

                // process HTTP POST request from client
                System.out.println("status line: " + br.readLine());
                String msg = null;
                while ((msg = br.readLine()) != null && msg.length() > 0);

                msg = br.readLine();
                if (msg.equals(postMsg)) {
                    ps.println("HTTP/1.1 200 OK\n\n");
                } else {
                    ps.println("HTTP/1.1 500 Not OK\n\n");
                }
                ps.flush();

                ExtendedSSLSession session =
                        (ExtendedSSLSession)sslSocket.getSession();
                if (session.getRequestedServerNames().isEmpty()) {
                    throw new Exception("No expected Server Name Indication");
                }

                // close the socket
                while (!closeReady) {
                    Thread.sleep(50);
                }
            }
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

        // Send HTTP POST request to server
        URL url = new URL("https://" + hostname + ":" + serverPort);

        HttpsURLConnection.setDefaultHostnameVerifier(new NameVerifier());
        HttpsURLConnection http = (HttpsURLConnection)url.openConnection();
        http.setDoOutput(true);

        http.setRequestMethod("POST");
        PrintStream ps = new PrintStream(http.getOutputStream());
        try {
            ps.println(postMsg);
            ps.flush();
            if (http.getResponseCode() != 200) {
                throw new RuntimeException("test Failed");
            }
        } finally {
            ps.close();
            http.disconnect();
            closeReady = true;
        }
    }

    private static class NameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */

    // use any free port by default
    private volatile int serverPort = 0;

    private volatile Exception serverException = null;
    private volatile Exception clientException = null;

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

        if (debug) {
            System.setProperty("javax.net.debug", "all");
        }

        try {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException uhe) {
            System.out.println(
                "Ignore the test as the local hostname cannot be determined");

            return;
        }

        System.out.println(
                "The fully qualified domain name of the local host is " +
                hostname);
        // Ignore the test if the hostname does not sound like a domain name.
        if ((hostname == null) || hostname.isEmpty() ||
                !hostname.contains(".") || hostname.endsWith(".") ||
                hostname.startsWith("localhost") ||
                Character.isDigit(hostname.charAt(hostname.length() - 1))) {

            System.out.println("Ignore the test as the local hostname " +
                    "cannot be determined as fully qualified domain name");

            return;
        }

        /*
         * Start the tests.
         */
        new ImpactOnSNI();
    }

    private Thread clientThread = null;
    private Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    ImpactOnSNI() throws Exception {
        Exception startException = null;
        try {
            if (separateServerThread) {
                startServer(true);
                startClient(false);
            } else {
                startClient(true);
                startServer(false);
            }
        } catch (Exception e) {
            startException = e;
        }

        /*
         * Wait for other side to close down.
         */
        if (separateServerThread) {
            if (serverThread != null) {
                serverThread.join();
            }
        } else {
            if (clientThread != null) {
                clientThread.join();
            }
        }

        /*
         * When we get here, the test is pretty much over.
         * Which side threw the error?
         */
        Exception local;
        Exception remote;

        if (separateServerThread) {
            remote = serverException;
            local = clientException;
        } else {
            remote = clientException;
            local = serverException;
        }

        Exception exception = null;

        /*
         * Check various exception conditions.
         */
        if ((local != null) && (remote != null)) {
            // If both failed, return the curthread's exception.
            local.initCause(remote);
            exception = local;
        } else if (local != null) {
            exception = local;
        } else if (remote != null) {
            exception = remote;
        } else if (startException != null) {
            exception = startException;
        }

        /*
         * If there was an exception *AND* a startException,
         * output it.
         */
        if (exception != null) {
            if (exception != startException && startException != null) {
                exception.addSuppressed(startException);
            }
            throw exception;
        }

        // Fall-through: no exception to throw!
    }

    private void startServer(boolean newThread) throws Exception {
        if (newThread) {
            serverThread = new Thread() {
                @Override
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
            try {
                doServerSide();
            } catch (Exception e) {
                serverException = e;
            } finally {
                serverReady = true;
            }
        }
    }

    private void startClient(boolean newThread) throws Exception {
        if (newThread) {
            clientThread = new Thread() {
                @Override
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
            try {
                doClientSide();
            } catch (Exception e) {
                clientException = e;
            }
        }
    }
}
