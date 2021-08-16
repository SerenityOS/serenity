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

//
// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.
//

/*
 * @test
 * @bug 4366807
 * @summary Need new APIs to get/set session timeout and session cache size.
 * @run main/othervm SessionCacheSizeTests
 */

import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.util.*;
import java.security.*;

/**
 * Session cache size tests cover the following cases:
 * 1. Effect of system property javax.net.ssl.SessionCacheSize (this
 * property is not documented for public).
 * 2. Reducing the cache size, results in uncaching of sessions if #of
 * sessions present exceeds the new size.
 * 3. Increasing the cache size, results in accomodating  new sessions if the
 * number of cached sessions is the current size limit.
 *
 * Invairant for passing this test is, at any given time,
 * #cached_sessions <= current_cache_size , for current_cache_size > 0
 */

public class SessionCacheSizeTests {

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

    /*
     * A limit on the number of connections at any given time
     */
    static int MAX_ACTIVE_CONNECTIONS = 4;

    static final int FREE_PORT = 0;

    void doServerSide(int serverConns) throws Exception {
        try (SSLServerSocket sslServerSocket =
                (SSLServerSocket) sslssf.createServerSocket(FREE_PORT)) {

            // timeout to accept a connection
            sslServerSocket.setSoTimeout(45000);

            // make sure createdPorts++ is atomic
            synchronized(serverPorts) {
                int serverPort = sslServerSocket.getLocalPort();
                System.out.printf("server #%d started on port %d%n",
                        createdPorts, serverPort);
                serverPorts[createdPorts++] = serverPort;

                /*
                 * Signal Client, we're ready for his connect.
                 */
                if (createdPorts == serverPorts.length) {
                    serverReady = true;
                }
            }
            int read = 0;
            int nConnections = 0;

            /*
             * Divide the max connections among the available server ports.
             * The use of more than one server port ensures creation of more
             * than one session.
             */
            SSLSession sessions [] = new SSLSession [serverConns];
            SSLSessionContext sessCtx = sslctx.getServerSessionContext();

            while (nConnections < serverConns) {
                try (SSLSocket sslSocket =
                        (SSLSocket)sslServerSocket.accept()) {
                    sslSocket.setSoTimeout(90000);      // timeout to read
                    InputStream sslIS = sslSocket.getInputStream();
                    OutputStream sslOS = sslSocket.getOutputStream();
                    read = sslIS.read();
                    sessions[nConnections] = sslSocket.getSession();
                    sslOS.write(85);
                    sslOS.flush();
                    nConnections++;
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
    void doClientSide() throws Exception {

        /*
         * Wait for server to get started.
         */
        while (!serverReady) {
            Thread.sleep(50);
        }

        int nConnections = 0;
        SSLSocket sslSockets[] = new SSLSocket [MAX_ACTIVE_CONNECTIONS];
        Vector sessions = new Vector();
        SSLSessionContext sessCtx = sslctx.getClientSessionContext();
        sessCtx.setSessionTimeout(0); // no limit

        try {
            while (nConnections < (MAX_ACTIVE_CONNECTIONS - 1)) {
                // divide the connections among the available server ports
                int serverPort = serverPorts [nConnections % (serverPorts.length)];
                System.out.printf("client #%d connects to port %d%n",
                        nConnections, serverPort);
                sslSockets[nConnections] = (SSLSocket) sslsf.
                            createSocket("localhost",
                            serverPort);
                InputStream sslIS = sslSockets[nConnections].getInputStream();
                OutputStream sslOS = sslSockets[nConnections].getOutputStream();
                sslOS.write(237);
                sslOS.flush();
                int read = sslIS.read();
                SSLSession sess = sslSockets[nConnections].getSession();
                if (!sessions.contains(sess))
                    sessions.add(sess);
                nConnections++;
            }
            System.out.println("Current cacheSize is set to: " +
                                    sessCtx.getSessionCacheSize());
            System.out.println();
            System.out.println("Currently cached Sessions......");
            System.out.println("============================================"
                                    + "============================");
            System.out.println("Session                                     "
                                    + "      Session-last-accessTime");
            System.out.println("============================================"
                                    + "============================");
            checkCachedSessions(sessCtx, nConnections);
            // Change session cache size
            sessCtx.setSessionCacheSize(2);
            System.out.println("Session cache size changed to: "
                            + sessCtx.getSessionCacheSize());
            System.out.println();
            checkCachedSessions(sessCtx, nConnections);

            // Test the effect of increasing the cache size
            sessCtx.setSessionCacheSize(3);
            System.out.println("Session cache size changed to: "
                            + sessCtx.getSessionCacheSize());
            // create a new session
            int serverPort = serverPorts [nConnections % (serverPorts.length)];
            System.out.printf("client #%d connects to port %d%n",
                    nConnections, serverPort);
            sslSockets[nConnections] = (SSLSocket) sslsf.
                            createSocket("localhost",
                            serverPort);
            InputStream sslIS = sslSockets[nConnections].getInputStream();
            OutputStream sslOS = sslSockets[nConnections].getOutputStream();
            sslOS.write(237);
            sslOS.flush();
            int read = sslIS.read();
            SSLSession sess = sslSockets[nConnections].getSession();
            if (!sessions.contains(sess))
                sessions.add(sess);
            nConnections++;

            // test the number of sessions cached against the cache size
            checkCachedSessions(sessCtx, nConnections);
        } finally {
            for (int i = 0; i < nConnections; i++) {
                if (sslSockets[i] != null) {
                    sslSockets[i].close();
                }
            }
        }
        System.out.println("Session cache size tests passed");
    }

    void checkCachedSessions(SSLSessionContext sessCtx,
                int nConn) throws Exception {
        int nSessions = 0;
        Enumeration e = sessCtx.getIds();
        int cacheSize = sessCtx.getSessionCacheSize();
        SSLSession sess;

        while (e.hasMoreElements()) {
            sess = sessCtx.getSession((byte[]) e.nextElement());
            long lastAccessedTime  = sess.getLastAccessedTime();
                System.out.println(sess + "       "
                        +  new Date(lastAccessedTime));

            nSessions++;
        }
        System.out.println("--------------------------------------------"
                                + "----------------------------");
        if ((cacheSize > 0) && (nSessions > cacheSize)) {

            // close all active connections before exiting
            for (int conn = nConn; conn < MAX_ACTIVE_CONNECTIONS; conn++) {
                SSLSocket s = (SSLSocket) sslsf.createSocket("localhost",
                        serverPorts [conn % (serverPorts.length)]);
                s.close();
            }
            throw new Exception("Session cache size test failed,"
                + " current cache size: " + cacheSize + " #sessions cached: "
                + nSessions);
        }
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */

    /*
     * #of ports > 1, guarantees creation of more than one session.
     * Using four ports (one per each connection), we are able to create
     * alteast four sessions.
     */
    int serverPorts[] = new int[]{0, 0, 0, 0};  // MAX_ACTIVE_CONNECTIONS: 4
    int createdPorts = 0;
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

        // test the effect of javax.net.ssl.sessionCacheSize
        System.setProperty("javax.net.ssl.sessionCacheSize", String.valueOf(0));

        sslctx = SSLContext.getInstance("TLS");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keyFilename)) {
            ks.load(fis, passwd.toCharArray());
        }
        kmf.init(ks, passwd.toCharArray());
        sslctx.init(kmf.getKeyManagers(), null, null);
        sslssf = (SSLServerSocketFactory) sslctx.getServerSocketFactory();
        sslsf = (SSLSocketFactory) sslctx.getSocketFactory();
        if (debug)
            System.setProperty("javax.net.debug", "all");

        /*
         * Start the tests.
         */
        new SessionCacheSizeTests();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    SessionCacheSizeTests() throws Exception {
        /*
         * create the SSLServerSocket and SSLSocket factories
         */

        /*
         * Divide the max connections among the available server ports.
         * The use of more than one server port ensures creation of more
         * than one session.
         */
        int serverConns = MAX_ACTIVE_CONNECTIONS / (serverPorts.length);
        int remainingConns = MAX_ACTIVE_CONNECTIONS % (serverPorts.length);

        Exception startException = null;
        try {
            if (separateServerThread) {
                for (int i = 0; i < serverPorts.length; i++) {
                    // distribute remaining connections among the
                    // available ports
                    if (i < remainingConns) {
                        startServer(serverConns + 1, true);
                    } else {
                        startServer(serverConns, true);
                    }
                }
                startClient(false);
            } else {
                startClient(true);
                for (int i = 0; i < serverPorts.length; i++) {
                    if (i < remainingConns) {
                        startServer(serverConns + 1, false);
                    } else {
                        startServer(serverConns, false);
                    }
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

    void startServer(final int nConns, boolean newThread) throws Exception {
        if (newThread) {
            serverThread = new Thread() {
                public void run() {
                    try {
                        doServerSide(nConns);
                    } catch (Exception e) {
                        /*
                         * Our server thread just died.
                         *
                         * Release the client, if not active already...
                         */
                        System.err.println("Server died...");
                        e.printStackTrace();
                        serverReady = true;
                        serverException = e;
                    }
                }
            };
            serverThread.start();
        } else {
            try {
                doServerSide(nConns);
            } catch (Exception e) {
                serverException = e;
            } finally {
                serverReady = true;
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
