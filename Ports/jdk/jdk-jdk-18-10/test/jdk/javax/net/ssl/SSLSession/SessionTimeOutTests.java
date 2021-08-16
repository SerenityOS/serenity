/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug   4366807
 * @summary Need new APIs to get/set session timeout and session cache size.
 * @run main/othervm SessionTimeOutTests
 */

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

import javax.net.ssl.*;
import java.util.*;
import java.security.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Session reuse time-out tests cover the cases below:
 * 1. general test, i.e timeout is set to x and session invalidates when
 * its lifetime exceeds x.
 * 2. Effect of changing the timeout limit.
 * The test suite does not cover the default timeout(24 hours) usage. This
 * case has been tested independently.
 *
 * Invariant for passing this test is, at any given time,
 * lifetime of a session < current_session_timeout, such that
 * current_session_timeout > 0, for all sessions cached by the session
 * context.
 */

public class SessionTimeOutTests {

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
    static String pathToStores = "../etc";
    static String keyStoreFile = "keystore";
    static String trustStoreFile = "truststore";
    static String passwd = "passphrase";

    private static int PORTS = 3;

    /*
     * Is the server ready to serve?
     */
    private final CountDownLatch serverCondition = new CountDownLatch(PORTS);

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
     */

    /*
     * A limit on the number of connections at any given time
     */
    static int MAX_ACTIVE_CONNECTIONS = 3;

    /*
     * Divide the max connections among the available server ports.
     * The use of more than one server port ensures creation of more
     * than one session.
     */
    private static final int serverConns = MAX_ACTIVE_CONNECTIONS / PORTS;
    private static final int remainingConns = MAX_ACTIVE_CONNECTIONS % PORTS;

    private static final int TIMEOUT = 30000; // in millisecond

    void doServerSide(int slot, int serverConns) throws Exception {

        SSLServerSocket sslServerSocket
                = (SSLServerSocket) sslssf.createServerSocket(0);
        sslServerSocket.setSoTimeout(TIMEOUT);
        serverPorts[slot] = sslServerSocket.getLocalPort();

        /*
         * Signal Client, one server is ready for its connect.
         */
        serverCondition.countDown();

        for (int nConnections = 0; nConnections < serverConns; nConnections++) {
            SSLSocket sslSocket = null;
            try {
                sslSocket = (SSLSocket) sslServerSocket.accept();
            }  catch (SocketTimeoutException ste) {
                System.out.println(
                        "No incoming client connection. Ignore in server side.");
                continue;
            }
            InputStream sslIS = sslSocket.getInputStream();
            OutputStream sslOS = sslSocket.getOutputStream();
            sslIS.read();
            sslSocket.getSession();
            sslOS.write(85);
            sslOS.flush();
            sslSocket.close();
        }
    }

    /*
     * Define the client side of the test.
     *
     * If the server prematurely exits, serverReady will be set to zero
     * to avoid infinite hangs.
     */
    void doClientSide() throws Exception {
        /*
         * Wait for server to get started.
         */
        if (!serverCondition.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
            System.out.println(
                    "The server side is not ready yet. Ignore in client side.");
            return;
        }

        SSLSocket sslSockets[] = new SSLSocket[MAX_ACTIVE_CONNECTIONS];
        Vector<SSLSession> sessions = new Vector<>();
        SSLSessionContext sessCtx = sslctx.getClientSessionContext();

        sessCtx.setSessionTimeout(10); // in secs
        int timeout = sessCtx.getSessionTimeout();
        for (int nConnections = 0; nConnections < MAX_ACTIVE_CONNECTIONS;
                nConnections++) {
            // divide the connections among the available server ports
            try {
                SSLSocket sslSocket = (SSLSocket) sslsf.createSocket();
                sslSocket.connect(new InetSocketAddress("localhost",
                        serverPorts[nConnections % serverPorts.length]),
                        TIMEOUT);
                sslSockets[nConnections] = sslSocket;
            } catch (IOException ioe) {
                // The server side may be impacted by naughty test cases or
                // third party routines, and cannot accept connections.
                //
                // Just ignore the test if the connection cannot be
                // established.
                System.out.println(
                        "Cannot make a connection in time. Ignore in client side.");
                continue;
            }

            InputStream sslIS = sslSockets[nConnections].getInputStream();
            OutputStream sslOS = sslSockets[nConnections].getOutputStream();
            sslOS.write(237);
            sslOS.flush();
            sslIS.read();
            SSLSession sess = sslSockets[nConnections].getSession();
            if (!sessions.contains(sess))
                sessions.add(sess);
        }
        System.out.println();
        System.out.println("Current timeout is set to: " + timeout);
        System.out.println("Testing SSLSessionContext.getSession()......");
        System.out.println("========================================"
                                + "=======================");
        System.out.println("Session                                 "
                                + "Session-     Session");
        System.out.println("                                        "
                                + "lifetime     timedout?");
        System.out.println("========================================"
                                + "=======================");

        for (int i = 0; i < sessions.size(); i++) {
            SSLSession session = (SSLSession) sessions.elementAt(i);
            long currentTime  = System.currentTimeMillis();
            long creationTime = session.getCreationTime();
            long lifetime = (currentTime - creationTime) / 1000;

            System.out.print(session + "      " + lifetime + "            ");
            if (sessCtx.getSession(session.getId()) == null) {
                if (lifetime < timeout)
                    // sessions can be garbage collected before the timeout
                    // limit is reached
                    System.out.println("Invalidated before timeout");
                else
                    System.out.println("YES");
            } else {
                System.out.println("NO");
                if ((timeout != 0) && (lifetime > timeout)) {
                    throw new Exception("Session timeout test failed for the"
                        + " obove session");
                }
            }
            // change the timeout
            if (i == ((sessions.size()) / 2)) {
                System.out.println();
                sessCtx.setSessionTimeout(2); // in secs
                timeout = sessCtx.getSessionTimeout();
                System.out.println("timeout is changed to: " + timeout);
                System.out.println();
           }
        }

        // check the ids returned by the enumerator
        Enumeration<byte[]> e = sessCtx.getIds();
        System.out.println("----------------------------------------"
                                + "-----------------------");
        System.out.println("Testing SSLSessionContext.getId()......");
        System.out.println();

        SSLSession nextSess = null;
        SSLSession sess;
        for (int i = 0; i < sessions.size(); i++) {
            sess = (SSLSession)sessions.elementAt(i);
            String isTimedout = "YES";
            long currentTime  = System.currentTimeMillis();
            long creationTime  = sess.getCreationTime();
            long lifetime = (currentTime - creationTime) / 1000;

            if (nextSess != null) {
                if (isEqualSessionId(nextSess.getId(), sess.getId())) {
                    isTimedout = "NO";
                    nextSess = null;
                }
            } else if (e.hasMoreElements()) {
                nextSess = sessCtx.getSession((byte[]) e.nextElement());
                if ((nextSess != null) && isEqualSessionId(nextSess.getId(),
                                        sess.getId())) {
                    nextSess = null;
                    isTimedout = "NO";
                }
            }

            /*
             * A session not invalidated even after it's timeout?
             */
            if ((timeout != 0) && (lifetime > timeout) &&
                        (isTimedout.equals("NO"))) {
                throw new Exception("Session timeout test failed for session: "
                                + sess + " lifetime: " + lifetime);
            }
            System.out.print(sess + "      " + lifetime);
            if (((timeout == 0) || (lifetime < timeout)) &&
                                  (isTimedout.equals("YES"))) {
                isTimedout = "Invalidated before timeout";
            }

            System.out.println("            " + isTimedout);
        }
        for (int i = 0; i < sslSockets.length; i++) {
            sslSockets[i].close();
        }
        System.out.println("----------------------------------------"
                                 + "-----------------------");
        System.out.println("Session timeout test passed");
    }

    boolean isEqualSessionId(byte[] id1, byte[] id2) {
        if (id1.length != id2.length)
           return false;
        else {
            for (int i = 0; i < id1.length; i++) {
                if (id1[i] != id2[i]) {
                   return false;
                }
            }
            return true;
        }
    }


    /*
     * =============================================================
     * The remainder is just support stuff
     */

    int serverPorts[] = new int[PORTS];
    static SSLServerSocketFactory sslssf;
    static SSLSocketFactory sslsf;
    static SSLContext sslctx;

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

        sslctx = SSLContext.getInstance("TLS");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(keyFilename), passwd.toCharArray());
        kmf.init(ks, passwd.toCharArray());
        sslctx.init(kmf.getKeyManagers(), null, null);
        sslssf = (SSLServerSocketFactory) sslctx.getServerSocketFactory();
        sslsf = (SSLSocketFactory) sslctx.getSocketFactory();

        /*
         * Start the tests.
         */
        new SessionTimeOutTests();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    SessionTimeOutTests() throws Exception {

        /*
         * create the SSLServerSocket and SSLSocket factories
         */

        Exception startException = null;
        try {
            if (separateServerThread) {
                for (int i = 0; i < serverPorts.length; i++) {
                    // distribute remaining connections among the
                    // available ports
                    if (i < remainingConns)
                        startServer(i, (serverConns + 1), true);
                    else
                        startServer(i, serverConns, true);
                }
                startClient(false);
            } else {
                startClient(true);
                for (int i = 0; i < PORTS; i++) {
                    if (i < remainingConns)
                        startServer(i, (serverConns + 1), false);
                    else
                        startServer(i, serverConns, false);
                }
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

    void startServer(final int slot, final int nConns, boolean newThread)
            throws Exception {
        if (newThread) {
            serverThread = new Thread() {
                public void run() {
                    try {
                        doServerSide(slot, nConns);
                    } catch (Exception e) {
                        /*
                         * Our server thread just died.
                         *
                         * Release the client, if not active already...
                         */
                        System.err.println("Server died...");
                        e.printStackTrace();
                        serverException = e;
                    }
                }
            };
            serverThread.start();
        } else {
            try {
                doServerSide(slot, nConns);
            } catch (Exception e) {
                serverException = e;
            }
        }
    }

    void startClient(boolean newThread)
                 throws Exception {
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
            try {
                doClientSide();
            } catch (Exception e) {
                clientException = e;
            }
        }
    }
}
