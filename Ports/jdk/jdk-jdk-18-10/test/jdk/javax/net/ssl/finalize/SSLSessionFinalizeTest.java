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
 * @summary Test behavior related to finalize
 * @run main/othervm  SSLSessionFinalizeTest
 * @run main/othervm/policy=security.policy  SSLSessionFinalizeTest
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.ArrayBlockingQueue;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionBindingEvent;
import javax.net.ssl.SSLSessionBindingListener;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SSLSessionFinalizeTest {

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
    void doServerSide() throws Exception {
        SSLServerSocketFactory sslssf =
            (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket sslServerSocket =
            (SSLServerSocket) sslssf.createServerSocket(serverPort);
        serverPort = sslServerSocket.getLocalPort();

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady = true;

        while (serverReady) {
            SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
//            System.out.printf("  accept: %s%n", sslSocket);
            InputStream sslIS = sslSocket.getInputStream();
            OutputStream sslOS = sslSocket.getOutputStream();

            sslIS.read();
            sslOS.write(85);
            sslOS.flush();

            sslSocket.close();
        }
    }

    /*
     * Define the client side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    SBListener doClientSide() throws Exception {

        /*
         * Wait for server to get started.
         */
        while (!serverReady) {
            Thread.sleep(50);
        }

        SSLSocketFactory sslsf =
            (SSLSocketFactory) SSLSocketFactory.getDefault();

        try {
                SSLSocket sslSocket = (SSLSocket)
                    sslsf.createSocket("localhost", serverPort);
                InputStream sslIS = sslSocket.getInputStream();
                OutputStream sslOS = sslSocket.getOutputStream();

            sslOS.write(280);
            sslOS.flush();
            sslIS.read();

            sslOS.close();
            sslIS.close();

            SSLSession sslSession = sslSocket.getSession();
            System.out.printf(" sslSession: %s %n   %s%n", sslSession, sslSession.getClass());
            SBListener sbListener = new SBListener(sslSession);

            sslSession.putValue("x", sbListener);

            sslSession.invalidate();

            sslSocket.close();

            sslOS = null;
            sslIS = null;
            sslSession = null;
            sslSocket = null;
            Reference.reachabilityFence(sslOS);
            Reference.reachabilityFence(sslIS);
            Reference.reachabilityFence(sslSession);
            Reference.reachabilityFence(sslSocket);

            return sbListener;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
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
        new SSLSessionFinalizeTest();
    }

    ArrayBlockingQueue<Thread> threads = new ArrayBlockingQueue<Thread>(100);

    ArrayBlockingQueue<SBListener> sbListeners = new ArrayBlockingQueue<>(100);

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    SSLSessionFinalizeTest() throws Exception {
        final int count = 1;
        if (separateServerThread) {
            startServer(true);
            startClients(true, count);
        } else {
            startClients(true, count);
            startServer(true);
        }

        /*
         * Wait for other side to close down.
         */
        Thread t;
        while ((t = threads.take()) != Thread.currentThread()) {
            System.out.printf("  joining: %s%n", t);
            t.join(1000L);
        }
        serverReady = false;
        System.gc();
        System.gc();


        SBListener listener = null;
        while ((listener = sbListeners.poll()) != null) {
            if (!listener.check()) {
                System.out.printf("  sbListener not called on finalize: %s%n",
                        listener);
            }
        }

        /*
         * When we get here, the test is pretty much over.
         *
         * If the main thread excepted, that propagates back
         * immediately.  If the other thread threw an exception, we
         * should report back.
         */
        if (serverException != null) {
            System.out.print("Server Exception:");
            throw serverException;
        }
        if (clientException != null) {
            System.out.print("Client Exception:");
            throw clientException;
        }
    }

    void startServer(boolean newThread) throws Exception {
        if (newThread) {
            Thread t = new Thread("Server") {
                public void run() {
                    try {
                        doServerSide();
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
            threads.add(t);
            t.setDaemon(true);
            t.start();
        } else {
            doServerSide();
        }
    }

    void startClients(boolean newThread, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            System.out.printf(" newClient: %d%n", i);
            startClient(newThread);
        }
        serverReady = false;

        threads.add(Thread.currentThread());    // add ourselves at the 'end'
    }
    void startClient(boolean newThread) throws Exception {
        if (newThread) {
            Thread t = new Thread("Client") {
                public void run() {
                    try {
                        sbListeners.add(doClientSide());
                    } catch (Exception e) {
                        /*
                         * Our client thread just died.
                         */
                        System.err.println("Client died..." + e);
                        clientException = e;
                    }
                }
            };
            System.out.printf(" starting: %s%n", t);
            threads.add(t);
            t.start();
        } else {
            sbListeners.add(doClientSide());
        }
    }


    static class SBListener implements SSLSessionBindingListener {
        private volatile int unboundNotified;
        private final WeakReference<SSLSession> session;

        SBListener(SSLSession session) {
            this.unboundNotified = 0;
            this.session = new WeakReference<SSLSession>(session);
        }

        boolean check() {
            System.out.printf("  check: %s%n", this);
            return unboundNotified > 0 && session.get() == null;
        }

        @Override
        public void valueBound(SSLSessionBindingEvent event) {
            System.out.printf(" valueBound: %s%n", event.getName());
        }

        @Override
        public void valueUnbound(SSLSessionBindingEvent event) {
            System.out.printf(" valueUnbound: %s%n", event.getName());
            unboundNotified++;
        }

        public String toString() {
            return "count: " + unboundNotified +
                    ", ref: " + session.get();
        }
    }
}
