/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4938185 7106773
 * @summary KeyStore support for NSS cert/key databases
 *          512 bits RSA key cannot work with SHA384 and SHA512
 * @library /test/lib ..
 * @run testng/othervm ClientAuth
 */

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.*;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import javax.net.*;
import javax.net.ssl.*;

public class ClientAuth extends PKCS11Test {

    /*
     * =============================================================
     * Set the various variables needed for the tests, then
     * specify what tests to run on each side.
     */

    private static Provider provider;
    private static final String NSS_PWD = "test12";
    private static final String JKS_PWD = "passphrase";
    private static final String SERVER_KS = "server.keystore";
    private static final String TS = "truststore";
    private static String p11config;

    private static final Path TEST_DATA_PATH = Path.of(BASE)
            .resolve("ClientAuthData");

    private static final String DIR = TEST_DATA_PATH.toString();

    /*
     * Should we run the client or server in a separate thread?
     * Both sides can throw exceptions, but do you have a preference
     * as to which side should be the main thread.
     */
    static boolean separateServerThread = false;

    /*
     * Is the server ready to serve?
     */
    private final CountDownLatch serverReadyLatch = new CountDownLatch(1);

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

    @BeforeClass
    public void setUp() throws Exception {
        copyNssCertKeyToClassesDir(TEST_DATA_PATH);
        setCommonSystemProps();
        System.setProperty("CUSTOM_P11_CONFIG",
                TEST_DATA_PATH.resolve("p11-nss.txt").toString());
        Security.setProperty("jdk.tls.disabledAlgorithms", "");
        Security.setProperty("jdk.certpath.disabledAlgorithms", "");
    }

    @Test
    public void testClientAuthTLSv1() throws Exception {
        String[] args = { "TLSv1" };
        runTest(args);
    }

    @Test
    public void testClientAuthTLSv11() throws Exception {
        String[] args = { "TLSv1.1" };
        runTest(args);
    }

    @Test
    public void testClientAuthTLSv12AndCipherSuite() throws Exception {
        String[] args = { "TLSv1.2", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA" };
        runTest(args);
    }

    private void runTest(String[] args) throws Exception {
        System.out.println("Running with args: " + Arrays.toString(args));
        parseArguments(args);
        main(new ClientAuth());
    }

    /*
     * Define the server side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doServerSide() throws Exception {

        SSLContext ctx = SSLContext.getInstance("TLS");
        char[] passphrase = JKS_PWD.toCharArray();

        // server gets KeyStore from JKS keystore
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(new File(DIR, SERVER_KS)), passphrase);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        // server gets TrustStore from PKCS#11 token
/*
        passphrase = NSS_PWD.toCharArray();
        KeyStore ts = KeyStore.getInstance("PKCS11", "SunPKCS11-nss");
        ts.load(null, passphrase);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);
*/

        //ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        ctx.init(kmf.getKeyManagers(), null, null);
        ServerSocketFactory ssf = ctx.getServerSocketFactory();
        InetSocketAddress socketAddress =
                new InetSocketAddress(InetAddress.getLoopbackAddress(), serverPort);
        SSLServerSocket sslServerSocket = (SSLServerSocket) ssf.createServerSocket();
        sslServerSocket.bind(socketAddress);
        sslServerSocket.setNeedClientAuth(true);
        serverPort = sslServerSocket.getLocalPort();
        System.out.println("serverPort = " + serverPort);

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReadyLatch.countDown();

        SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
        InputStream sslIS = sslSocket.getInputStream();
        OutputStream sslOS = sslSocket.getOutputStream();

        sslIS.read();
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
        serverReadyLatch.await();

        SSLContext ctx = SSLContext.getInstance("TLS");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");

        // client gets KeyStore from PKCS#11 token,
        // and gets TrustStore from JKS KeyStore (using system properties)
        char[] passphrase = NSS_PWD.toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS11", "SunPKCS11-nss");
        ks.load(null, passphrase);

        kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);
        ctx.init(kmf.getKeyManagers(), null, null);

        SSLSocketFactory sslsf = ctx.getSocketFactory();
        SSLSocket sslSocket = (SSLSocket)
            sslsf.createSocket("localhost", serverPort);

        if (clientProtocol != null) {
            sslSocket.setEnabledProtocols(new String[] {clientProtocol});
        }

        if (clientCiperSuite != null) {
            sslSocket.setEnabledCipherSuites(new String[] {clientCiperSuite});
        }

        InputStream sslIS = sslSocket.getInputStream();
        OutputStream sslOS = sslSocket.getOutputStream();

        sslOS.write(280);
        sslOS.flush();
        sslIS.read();

        sslSocket.close();
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */

    // use any free port by default
    volatile int serverPort = 0;

    volatile Exception serverException = null;
    volatile Exception clientException = null;

    private static String clientProtocol = null;
    private static String clientCiperSuite = null;

    private static void parseArguments(String[] args) {
        if (args.length > 0) {
            clientProtocol = args[0];
        }

        if (args.length > 1) {
            clientCiperSuite = args[1];
        }
    }

    public void main(Provider p) throws Exception {
        // SSL RSA client auth currently needs an RSA cipher
        // (cf. NONEwithRSA hack), which is currently not available in
        // open builds.
        try {
            javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding", p);
        } catch (GeneralSecurityException e) {
            System.out.println("Not supported by provider, skipping");
            return;
        }

        this.provider = p;

        System.setProperty("javax.net.ssl.trustStore",
                                        new File(DIR, TS).toString());
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStoreProvider", "SUN");
        System.setProperty("javax.net.ssl.trustStorePassword", JKS_PWD);

        // perform Security.addProvider of P11 provider
        Security.addProvider(getSunPKCS11(System.getProperty("CUSTOM_P11_CONFIG")));

        if (debug) {
            System.setProperty("javax.net.debug", "all");
        }

        /*
         * Start the tests.
         */
        go();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Fork off the other side, then do your work.
     */
    private void go() throws Exception {
        try {
            if (separateServerThread) {
                startServer(true);
                startClient(false);
            } else {
                startClient(true);
                startServer(false);
            }
        } catch (Exception e) {
            //swallow for now.  Show later
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
         * Which side threw the error?
         */
        Exception local;
        Exception remote;
        String whichRemote;

        if (separateServerThread) {
            remote = serverException;
            local = clientException;
            whichRemote = "server";
        } else {
            remote = clientException;
            local = serverException;
            whichRemote = "client";
        }

        /*
         * If both failed, return the curthread's exception, but also
         * print the remote side Exception
         */
        if ((local != null) && (remote != null)) {
            System.out.println(whichRemote + " also threw:");
            remote.printStackTrace();
            System.out.println();
            throw local;
        }

        if (remote != null) {
            throw remote;
        }

        if (local != null) {
            throw local;
        }
    }

    void startServer (boolean newThread) {
        if (newThread) {
            serverThread = new Thread(() -> {
                try {
                    doServerSide();
                } catch (Exception e) {
                    /*
                     * Our server thread just died.
                     *
                     * Release the client, if not active already...
                     */
                    System.err.println("Server died...");
                    serverReadyLatch.countDown();
                    serverException = e;
                }
            });
            serverThread.start();
        } else {
            try {
                doServerSide();
            } catch (Exception e) {
                serverException = e;
            } finally {
                serverReadyLatch.countDown();
            }
        }
    }

    void startClient (boolean newThread) {
        if (newThread) {
            clientThread = new Thread(() -> {
                try {
                    doClientSide();
                } catch (Exception e) {
                    /*
                     * Our client thread just died.
                     */
                    System.err.println("Client died...");
                    clientException = e;
                }
            });
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
