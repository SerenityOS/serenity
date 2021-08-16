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

/*
 * @test
 * @bug 4453053
 * @summary If a server shuts down correctly during handshaking, the client
 *     doesn't see it.
 * @run main/othervm NullCerts
 *
 *     SunJSSE does not support dynamic system properties, no way to re-use
 *     system properties in samevm/agentvm mode.
 * @author Brad Wetmore
 */

import java.io.*;
import java.net.*;
import java.security.*;
import javax.net.ssl.*;

public class NullCerts {

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
    private void doServerSide() throws Exception {
        SSLServerSocketFactory sslssf =
            (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket sslServerSocket =
            (SSLServerSocket) sslssf.createServerSocket(serverPort, 3);
        sslServerSocket.setNeedClientAuth(true);

        serverPort = sslServerSocket.getLocalPort();

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady = true;

        SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
        InputStream sslIS = sslSocket.getInputStream();
        OutputStream sslOS = sslSocket.getOutputStream();

        try {
            sslIS.read();
            sslOS.write(85);
            sslOS.flush();
        } catch (SSLHandshakeException e) {
            System.out.println(
                "Should see a null cert chain exception for server: "
                + e.toString());
        }

        sslSocket.close();
        System.out.println("Server done and exiting!");
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

        System.out.println("Starting test");

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore uks = KeyStore.getInstance("JKS");
        SSLContext ctx = SSLContext.getInstance("TLS");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");

        uks.load(new FileInputStream(unknownFilename), cpasswd);
        kmf.init(uks, cpasswd);

        ks.load(new FileInputStream(trustFilename), cpasswd);
        tmf.init(ks);

        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        SSLSocketFactory sslsf =
            (SSLSocketFactory) ctx.getSocketFactory();
        SSLSocket sslSocket = (SSLSocket)
            sslsf.createSocket("localhost", serverPort);

        InputStream sslIS = sslSocket.getInputStream();
        OutputStream sslOS = sslSocket.getOutputStream();

        try {
            sslOS.write(280);
            sslOS.flush();
            sslIS.read();

            sslSocket.close();
        } catch (IOException e) {
            String str =
                "\nYou will either see a bad_certificate SSLException\n" +
                "or an IOException if the server shutdown while the\n" +
                "client was still sending the remainder of its \n" +
                "handshake data.";
            System.out.println(str + e.toString());
        }
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */

    // use any free port by default
    volatile int serverPort = 0;

    private volatile Exception serverException = null;
    private volatile Exception clientException = null;

    private final static String keyFilename =
        System.getProperty("test.src", ".") + "/" + pathToStores +
        "/" + keyStoreFile;
    private final static String trustFilename =
        System.getProperty("test.src", ".") + "/" + pathToStores +
        "/" + trustStoreFile;
    private final static String unknownFilename =
        System.getProperty("test.src", ".") + "/" + pathToStores +
        "/" + "unknown_keystore";

   // Used for running test standalone
    public static void main(String[] args) throws Exception {

        String testRoot = System.getProperty("test.src", ".");
        System.setProperty("javax.net.ssl.keyStore", keyFilename);
        System.setProperty("javax.net.ssl.keyStorePassword", passwd);
        System.setProperty("javax.net.ssl.trustStore", trustFilename);
        System.setProperty("javax.net.ssl.trustStorePassword", passwd);

        if (DEBUG)
            System.setProperty("javax.net.debug", "all");

        /*
         * Start the tests.
         */
        new NullCerts();
    }

    private Thread clientThread = null;
    private Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    NullCerts() throws Exception {

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
