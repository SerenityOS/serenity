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

/*
 * @test
 * @bug 4473210
 * @summary SSLSessionContext should be accessible from SSLContext
 * @run main/othervm -Djdk.tls.server.enableSessionTicketExtension=false
 *   SSLCtxAccessToSessCtx
 *
 *
 *     SunJSSE does not support dynamic system properties, no way to re-use
 *     system properties in samevm/agentvm mode.
 */

import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.security.KeyStore;

public class SSLCtxAccessToSessCtx  {

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
    AtomicInteger serverReady = new AtomicInteger(1);   // only one port now

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
    void doServerSide(int serverPort) throws Exception {

        SSLServerSocket sslServerSocket =
            (SSLServerSocket) sslssf.createServerSocket(serverPort);
        int slot = createdPorts.getAndIncrement();
        serverPorts[slot] = sslServerSocket.getLocalPort();

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady.getAndDecrement();
        int read = 0;
        SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
        InputStream sslIS = sslSocket.getInputStream();
        OutputStream sslOS = sslSocket.getOutputStream();
        read = sslIS.read();
        SSLSessionContext sslctxCache = sslctx.getServerSessionContext();
        SSLSessionContext sessCache = sslSocket.getSession().
                                getSessionContext();
        if (sessCache != sslctxCache)
            throw new Exception("Test failed, session_cache != sslctx_cache");
        sslOS.write(85);
        sslOS.flush();
        sslSocket.close();
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
        while (serverReady.get() > 0) {
            Thread.sleep(50);
        }
        /*
         * first connection to serverPorts[0] -- a new session, session11
         * gets created, and is cached.
         */
        SSLSocket sslSocket;
        sslSocket = (SSLSocket) sslsf.
                createSocket("localhost", serverPorts[0]);
        InputStream sslIS = sslSocket.getInputStream();
        OutputStream sslOS = sslSocket.getOutputStream();
        sslOS.write(237);
        sslOS.flush();

        SSLSession sess = sslSocket.getSession();
        SSLSessionContext sessCache = sess.getSessionContext();
        SSLSessionContext sslctxCache = sslctx.getClientSessionContext();
        if (sessCache != sslctxCache)
            throw new Exception("Test failed, session_cache != sslctx_cache");

        int read = sslIS.read();
        sslSocket.close();
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */

    int serverPorts[] = new int[]{0};           // only one port at present
    AtomicInteger createdPorts = new AtomicInteger(0);
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

        sslctx = SSLContext.getInstance("TLS");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(keyFilename), passwd.toCharArray());
        kmf.init(ks, passwd.toCharArray());
        sslctx.init(kmf.getKeyManagers(), null, null);

        sslssf = (SSLServerSocketFactory) sslctx.getServerSocketFactory();
        sslsf = (SSLSocketFactory) sslctx.getSocketFactory();

        if (debug)
            System.setProperty("javax.net.debug", "all");

        /*
         * Start the tests.
         */
        new SSLCtxAccessToSessCtx();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    SSLCtxAccessToSessCtx() throws Exception {

        /*
         * create the SSLServerSocket and SSLSocket factories
         */
        if (separateServerThread) {
            for (int i = 0; i < serverPorts.length; i++) {
                startServer(serverPorts[i], true);
            }
            startClient(false);
        } else {
            startClient(true);
            for (int i = 0; i < serverPorts.length; i++) {
                startServer(serverPorts[i], false);
            }
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
        System.out.println("The Session context tests passed");
    }

    void startServer(final int port,
                        boolean newThread) throws Exception {
        if (newThread) {
            serverThread = new Thread() {
                public void run() {
                    try {
                        doServerSide(port);
                    } catch (Exception e) {
                        /*
                         * Our server thread just died.
                         *
                         * Release the client, if not active already...
                         */
                        System.err.println("Server died...");
                        e.printStackTrace();
                        serverReady.set(0);
                        serverException = e;
                    }
                }
            };
            serverThread.start();
        } else {
            try {
                doServerSide(port);
            } catch (Exception e) {
                serverException = e;
            } finally {
                serverReady.set(0);
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
