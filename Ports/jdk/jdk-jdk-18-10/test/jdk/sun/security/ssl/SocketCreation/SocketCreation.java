/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4414843
 * @summary This test tries all the different ways in which an SSL
 * connection can be established to exercise different SSLSocketImpl
 * constructors.
 * @run main/othervm/timeout=300 SocketCreation
 *
 *     SunJSSE does not support dynamic system properties, no way to re-use
 *     system properties in samevm/agentvm mode.
 */

import java.io.*;
import java.net.*;
import javax.net.ssl.*;

/**
 * This test has been adapted from JSSEClientServerTemplate.java. It runs
 * the client and server multiple times while it iterates through the
 * different ways in which an SSL connection can be established.
 *
 * The meat of this test is contained in doClientSide() and
 * doServerSide(). The loop is contained in the constructor
 * SocketCreation().
 */
public class SocketCreation {

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
     * Accepts a connection from a client and exchanges an int with it. The
     * connection can be established in one of three different ways:
     *    1. As a regular SSL server socket
     *    2. As an SSL server socket that is first unbound
     *    3. As an SSL socket layered over a regular TCP/IP socket
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doServerSide(int style) throws Exception {

        Socket sslSocket = null;

        // Change the for loop in SocketCreation() if you add more cases
        switch (style) {
        case 0:
            sslSocket = acceptNormally0();
            break;
        case 1:
            sslSocket = acceptNormally1();
            break;
        case 2:
            sslSocket = acceptNormally2();
            break;
        case 3:
            sslSocket = acceptUnbound();
            break;
        case 4:
            sslSocket = acceptLayered();
            break;
        default:
            throw new Exception("Incorrectly written test for server side!");
        }

        InputStream sslIS = sslSocket.getInputStream();
        OutputStream sslOS = sslSocket.getOutputStream();

        System.out.println("Server read: " + sslIS.read());
        sslOS.write(85);
        sslOS.flush();

        sslSocket.close();
    }

    private Socket acceptNormally0() throws Exception {

        SSLServerSocketFactory sslssf =
            (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

        System.out.println("Server: Will call createServerSocket(int)");
        ServerSocket sslServerSocket = sslssf.createServerSocket(0);
        serverPort = sslServerSocket.getLocalPort();

        System.out.println("Server: Will accept on SSL server socket...");

        serverReady = true;

        Socket sslSocket = sslServerSocket.accept();
        sslServerSocket.close();
        return sslSocket;
    }

    private Socket acceptNormally1() throws Exception {

        SSLServerSocketFactory sslssf =
            (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

        System.out.println("Server: Will call createServerSocket(int, int)");
        ServerSocket sslServerSocket = sslssf.createServerSocket(0,
                                                                 1);
        serverPort = sslServerSocket.getLocalPort();

        System.out.println("Server: Will accept on SSL server socket...");

        serverReady = true;

        Socket sslSocket = sslServerSocket.accept();
        sslServerSocket.close();
        return sslSocket;
    }

    private Socket acceptNormally2() throws Exception {

        SSLServerSocketFactory sslssf =
            (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

        System.out.println("Server: Will call createServerSocket(int, " +
                           " int, InetAddress)");
        ServerSocket sslServerSocket = sslssf.createServerSocket(0,
                                         1,
                                         InetAddress.getByName("localhost"));
        serverPort = sslServerSocket.getLocalPort();

        System.out.println("Server: Will accept on SSL server socket...");

        serverReady = true;

        Socket sslSocket = sslServerSocket.accept();
        sslServerSocket.close();
        return sslSocket;
    }

    private Socket acceptUnbound() throws Exception {

        SSLServerSocketFactory sslssf =
            (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

        System.out.println("Server: Will create unbound SSL server socket...");

        ServerSocket sslServerSocket = sslssf.createServerSocket();

        if (sslServerSocket.isBound())
            throw new Exception("Server socket is already bound!");

        sslServerSocket.bind(new java.net.InetSocketAddress(0));

        if (!sslServerSocket.isBound())
            throw new Exception("Server socket is not bound!");

        serverPort = sslServerSocket.getLocalPort();
        System.out.println("Server: Bound SSL server socket to port " +
                serverPort + "...");

        serverReady = true;

        System.out.println("Server: Will accept on SSL server socket...");
        Socket sslSocket = sslServerSocket.accept();
        sslServerSocket.close();
        return sslSocket;
    }

    private Socket acceptLayered() throws Exception {

        SSLSocketFactory sslsf =
            (SSLSocketFactory) SSLSocketFactory.getDefault();

        ServerSocket ss = new ServerSocket(0);
        serverPort = ss.getLocalPort();
        System.out.println("Server: Created normal server socket bound"
                + " to port " + serverPort + "...");
        System.out.println("Server: Will accept on server socket...");
        serverReady = true;
        Socket s = ss.accept();
        ss.close();
        System.out.println("Server: Will layer SSLSocket on top of" +
                           " server socket...");
        SSLSocket sslSocket =
            (SSLSocket) sslsf.createSocket(s,
                                            s.getInetAddress().getHostName(),
                                            s.getPort(),
                                            true);
        sslSocket.setUseClientMode(false);

        return sslSocket;
    }

    /*
     * Connects to a server and exchanges an int with it. The
     * connection can be established in one of three different ways:
     *    1. As a regular SSL socket
     *    2. As an SSL socket that is first unconnected
     *    3. As an SSL socket layered over a regular TCP/IP socket
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doClientSide(int style) throws Exception {

        Socket sslSocket = null;

        /*
         * Wait for server to get started.
         */
        while (!serverReady) {
            Thread.sleep(50);
        }

        // Change the for loop in SocketCreation() if you add more cases
        switch (style) {
        case 0:
            sslSocket = connectNormally0();
            break;
        case 1:
            sslSocket = connectNormally1();
            break;
        case 2:
            sslSocket = connectNormally2();
            break;
        case 3:
            sslSocket = connectNormally3();
            break;
        case 4:
            sslSocket = connectUnconnected();
            break;
        case 5:
            sslSocket = connectLayered();
            break;
        default:
            throw new Exception("Incorrectly written test for client side!");
        }

        InputStream sslIS = sslSocket.getInputStream();
        OutputStream sslOS = sslSocket.getOutputStream();

        sslOS.write(280);
        sslOS.flush();
        System.out.println("Client read: " + sslIS.read());

        sslSocket.close();
    }

    private Socket connectNormally0() throws Exception {

        SSLSocketFactory sslsf =
            (SSLSocketFactory) SSLSocketFactory.getDefault();

        System.out.println("Client: Will call createSocket(String, int)");
        return sslsf.createSocket("localhost", serverPort);
    }

    private Socket connectNormally1() throws Exception {

        SSLSocketFactory sslsf =
            (SSLSocketFactory) SSLSocketFactory.getDefault();

        System.out.println("Client: Will call createSocket(InetAddress, int)");
        return sslsf.createSocket(InetAddress.getByName("localhost"),
                                  serverPort);
    }

    private Socket connectNormally2() throws Exception {

        SSLSocketFactory sslsf =
            (SSLSocketFactory) SSLSocketFactory.getDefault();

        System.out.println("Client: Will call createSocket(String," +
                           " int, InetAddress, int)");
        return sslsf.createSocket("localhost", serverPort,
                                  InetAddress.getByName("localhost"),
                                  0);
    }

    private Socket connectNormally3() throws Exception {

        SSLSocketFactory sslsf =
            (SSLSocketFactory) SSLSocketFactory.getDefault();

        System.out.println("Client: Will call createSocket(InetAddress," +
                           " int, InetAddress, int)");
        return sslsf.createSocket(InetAddress.getByName("localhost"),
                                  serverPort,
                                  InetAddress.getByName("localhost"),
                                  0);
    }

    private Socket connectUnconnected() throws Exception {

        SSLSocketFactory sslsf =
            (SSLSocketFactory) SSLSocketFactory.getDefault();

        System.out.println("Client: Will call createSocket()");
        Socket sslSocket = sslsf.createSocket();

        if (sslSocket.isConnected())
            throw new Exception("Client socket is already connected!");

        System.out.println("Client: Will connect to server on port " +
                           serverPort + "...");
        sslSocket.connect(new java.net.InetSocketAddress("localhost",
                                                         serverPort));

        if (!sslSocket.isConnected())
            throw new Exception("Client socket is not connected!");

        return sslSocket;
    }

    private Socket connectLayered() throws Exception {

        SSLSocketFactory sslsf =
            (SSLSocketFactory) SSLSocketFactory.getDefault();

        System.out.println("Client: Will connect to server on port " +
                           serverPort + "...");
        Socket s = new Socket("localhost", serverPort);

        System.out.println("Client: Will layer SSL socket on top...");
        return sslsf.createSocket(s, "localhost", serverPort, true);

    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */

    // use any free port by default
    volatile int serverPort = 0;

    volatile Exception serverException = null;
    volatile Exception clientException = null;

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
        new SocketCreation();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Performs a loop where each iteration establishes one client-server
     * connection using a particular way of socket creation. There are
     * three ways in each side can create a socket:
     *   1. Normal (The client has 4 variations of this and the server 3)
     *   2. Unbound/Unconnected
     *   3. Layered
     * Each side goes through all three of them giving us a total of 5x6
     * possibilites.
     */
    SocketCreation() throws Exception {

        for (int serverStyle = 0; serverStyle < 5; serverStyle++) {
            System.out.println("-------------------------------------");
            for (int clientStyle = 0; clientStyle < 6; clientStyle++) {

                serverReady = false;

                startServer(separateServerThread, serverStyle);
                startClient(!separateServerThread, clientStyle);

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
                System.out.println();
            }
        }
    }

    void startServer(boolean newThread, final int style) throws Exception {
        if (newThread) {
            serverThread = new Thread() {
                public void run() {
                    try {
                        doServerSide(style);
                    } catch (Exception e) {
                        /*
                         * Our server thread just died.
                         *
                         * Release the client, if not active already...
                         */
                        System.err.println("Server died..." + e);
                        serverReady = true;
                        serverException = e;
                    }
                }
            };
            serverThread.start();
        } else {
            doServerSide(style);
        }
    }

    void startClient(boolean newThread, final int style) throws Exception {
        if (newThread) {
            clientThread = new Thread() {
                public void run() {
                    try {
                        doClientSide(style);
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
            doClientSide(style);
        }
    }
}
