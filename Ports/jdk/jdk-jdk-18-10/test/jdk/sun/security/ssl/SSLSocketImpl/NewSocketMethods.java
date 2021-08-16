/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4429176
 * @summary need to sync up SSL sockets with merlin java.net changes
 * @run main/othervm NewSocketMethods
 *
 *     SunJSSE does not support dynamic system properties, no way to re-use
 *     system properties in samevm/agentvm mode.
 */

import java.io.*;
import java.net.*;
import javax.net.*;
import javax.net.ssl.*;

/**
 * There are new methods for java.net.Socket class added to merlin
 * This test case checks the behaviour of these new methods when overriden
 * by methods of SSLSocket. The following methods are covered in this
 * test case.
 *
 *   public void sendUrgentData (int data) throws IOException
 *   public void setOOBInline(boolean on) throws SocketException
 *   public boolean getOOBInline() throws SocketException
 *   public SocketChannel getChannel() -- call on plain socket
 *   public void setTrafficClass(int tc) -- call on plain socket
 *                    throws SocketException
 *   public int getTrafficClass() -- call on plain socket
 *                   throws SocketException
 *   public void setReuseAddress(boolean on) -- call on plain socket
 *                    throws SocketException
 *   public boolean getReuseAddress()  -- call on plain socket
 *                       throws SocketException
 *   public boolean isInputShutdown()
 *   public boolean isOutputShutdown()
 *
 *   The methods below are covered by the test case located at:
 *                                 ../SocketCreation/SocketCreation.java
 *   public boolean isConnected()
 *   public boolean isBound()
 *
 */

public class NewSocketMethods {

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
     * If some one quickly wants to check the plain socket behaviour
     * as a reference
     */
    static boolean useSSL = true;

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
        Socket socket;
        ServerSocket serverSocket;
        if (useSSL) {
            SSLServerSocketFactory sslssf =
                (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            serverSocket =
                (SSLServerSocket) sslssf.createServerSocket(serverPort);
        } else {
           serverSocket = (ServerSocket) ServerSocketFactory.
                getDefault().createServerSocket(serverPort);
        }
        serverPort = serverSocket.getLocalPort();

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady = true;
        try {
            socket = serverSocket.accept();
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            /**
             * Test some new methods of java.net.Socket added to merlin
             */
            System.out.println("Server getChannel(): "
                         + socket.getChannel());
            try {
                socket.setOOBInline(true);
            } catch (IOException success) {
                // Currently we throw an IOException if this method is called
              }
            try {
                System.out.println("Server getOOBInline(): "
                                + socket.getOOBInline());
            } catch (IOException success) {
                // Currently we throw an IOException if this method is called
              }
            System.out.println("Server read: " + is.read());
            os.write(85);
            os.flush();
            socket.close();
         } catch (Exception unexpected) {
               throw new Exception(" test failed, caught exception: "
                        + unexpected);
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
        Socket socket;
        if (useSSL) {
            SSLSocketFactory sslsf =
                (SSLSocketFactory) SSLSocketFactory.getDefault();
            Socket plainSocket = new Socket("localhost", serverPort);
            socket = (SSLSocket)
                sslsf.createSocket(plainSocket, "localhost", serverPort, true);
        }
        else
            socket = new Socket("localhost", serverPort);
        try {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            /**
             * test some new methods of java.net.Socket added to merlin.
             */
            System.out.println("Client isInputShutdown() "
                        + socket.isInputShutdown());
            socket.setReuseAddress(true);
            System.out.println("Client getReuseAddress(): "
                        + socket.getReuseAddress());

            socket.setTrafficClass(8);
            System.out.println("Client getTrafficClass(): "
                    + socket.getTrafficClass());

            os.write(237);
            os.flush();
            System.out.println("Client read: " + is.read());
            socket.close();
            System.out.println("Client isOutputShutdown() "
                        + socket.isOutputShutdown());
        } catch (Exception unexpected) {
            throw new Exception(" test failed, caught exception: "
                        + unexpected);
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
        new NewSocketMethods();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    NewSocketMethods() throws Exception {
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
                        System.err.println("Server died... ");
                        e.printStackTrace();
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
