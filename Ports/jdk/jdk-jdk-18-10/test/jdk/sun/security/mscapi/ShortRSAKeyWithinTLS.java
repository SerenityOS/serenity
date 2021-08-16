/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7106773 8180570
 * @summary 512 bits RSA key cannot work with SHA384 and SHA512
 * @requires os.family == "windows"
 * @modules java.base/sun.security.util
 *          java.base/sun.security.tools.keytool
 *          java.base/sun.security.x509
 * @run main ShortRSAKeyWithinTLS 1024
 * @run main ShortRSAKeyWithinTLS 768
 * @run main ShortRSAKeyWithinTLS 512
 */
import java.io.*;
import java.net.*;
import java.security.cert.Certificate;
import java.util.*;
import java.security.*;
import java.security.cert.*;
import javax.net.*;
import javax.net.ssl.*;

import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.util.KeyUtil;
import sun.security.x509.X500Name;

public class ShortRSAKeyWithinTLS {

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
    static boolean separateServerThread = false;

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

        // load the key store
        serverKS = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
        serverKS.load(null, null);
        System.out.println("Loaded keystore: Windows-MY");

        // check key size
        checkKeySize(serverKS);

        // initialize the SSLContext
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(serverKS, null);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(serverKS);
        TrustManager[] tms = tmf.getTrustManagers();
        if (tms == null || tms.length == 0) {
            throw new Exception("unexpected trust manager implementation");
        } else {
            if (!(tms[0] instanceof X509TrustManager)) {
                throw new Exception("unexpected trust manager" +
                        " implementation: " +
                        tms[0].getClass().getCanonicalName());
            }
        }
        serverTM = new MyExtendedX509TM((X509TrustManager)tms[0]);
        tms = new TrustManager[] {serverTM};

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), tms, null);

        ServerSocketFactory ssf = ctx.getServerSocketFactory();
        SSLServerSocket sslServerSocket = (SSLServerSocket)
                                ssf.createServerSocket(serverPort);
        sslServerSocket.setNeedClientAuth(true);
        serverPort = sslServerSocket.getLocalPort();
        System.out.println("serverPort = " + serverPort);

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady = true;

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
        while (!serverReady) {
            Thread.sleep(50);
        }

        // load the key store
        KeyStore ks = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
        ks.load(null, null);
        System.out.println("Loaded keystore: Windows-MY");

        // initialize the SSLContext
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, null);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

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

    private void checkKeySize(KeyStore ks) throws Exception {
        PrivateKey privateKey = null;
        PublicKey publicKey = null;

        if (ks.containsAlias(keyAlias)) {
            System.out.println("Loaded entry: " + keyAlias);
            privateKey = (PrivateKey)ks.getKey(keyAlias, null);
            publicKey = (PublicKey)ks.getCertificate(keyAlias).getPublicKey();

            int privateKeySize = KeyUtil.getKeySize(privateKey);
            if (privateKeySize != keySize) {
                throw new Exception("Expected key size is " + keySize +
                        ", but the private key size is " + privateKeySize);
            }

            int publicKeySize = KeyUtil.getKeySize(publicKey);
            if (publicKeySize != keySize) {
                throw new Exception("Expected key size is " + keySize +
                        ", but the public key size is " + publicKeySize);
            }
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

    private static String keyAlias;
    private static int keySize;
    private static String clientProtocol = null;
    private static String clientCiperSuite = null;

    public static void main(String[] args) throws Exception {
        if (debug) {
            System.setProperty("javax.net.debug", "all");
        }

        keyAlias = "7106773." + args[0];
        keySize = Integer.parseInt(args[0]);

        KeyStore ks = KeyStore.getInstance("Windows-MY");
        ks.load(null, null);
        if (ks.containsAlias(keyAlias)) {
            ks.deleteEntry(keyAlias);
        }

        CertAndKeyGen gen = new CertAndKeyGen("RSA", "SHA256withRSA");
        gen.generate(keySize);

        ks.setKeyEntry(keyAlias, gen.getPrivateKey(), null,
                new Certificate[] {
                    gen.getSelfCertificate(new X500Name("cn=localhost,c=US"), 100)
                });

        clientProtocol = "TLSv1.2";
        clientCiperSuite = "TLS_DHE_RSA_WITH_AES_128_CBC_SHA";

        try {
            new ShortRSAKeyWithinTLS();
        } finally {
            ks.deleteEntry(keyAlias);
            ks.store(null, null);
        }
    }

    Thread clientThread = null;
    Thread serverThread = null;
    KeyStore serverKS;
    MyExtendedX509TM serverTM;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    ShortRSAKeyWithinTLS() throws Exception {
        try {
            if (separateServerThread) {
                startServer(true);
                startClient(false);
            } else {
                startClient(true);
                startServer(false);
            }
        } catch (Exception e) {
            // swallow for now.  Show later
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
            try {
                doServerSide();
            } catch (Exception e) {
                serverException = e;
            } finally {
                serverReady = true;
            }
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
            try {
                doClientSide();
            } catch (Exception e) {
                clientException = e;
            }
        }
    }


    class MyExtendedX509TM extends X509ExtendedTrustManager
            implements X509TrustManager {

        X509TrustManager tm;

        MyExtendedX509TM(X509TrustManager tm) {
            this.tm = tm;
        }

        public void checkClientTrusted(X509Certificate chain[], String authType)
                throws CertificateException {
            tm.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate chain[], String authType)
                throws CertificateException {
            tm.checkServerTrusted(chain, authType);
        }

        public X509Certificate[] getAcceptedIssuers() {
            List<X509Certificate> certs = new ArrayList<>();
            try {
                for (X509Certificate c : tm.getAcceptedIssuers()) {
                    if (serverKS.getCertificateAlias(c).equals(keyAlias))
                        certs.add(c);
                }
            } catch (KeyStoreException kse) {
                throw new RuntimeException(kse);
            }
            return certs.toArray(new X509Certificate[certs.size()]);
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType,
                Socket socket) throws CertificateException {
            tm.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType,
                Socket socket) throws CertificateException {
            tm.checkServerTrusted(chain, authType);
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType,
            SSLEngine engine) throws CertificateException {
            tm.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType,
            SSLEngine engine) throws CertificateException {
            tm.checkServerTrusted(chain, authType);
        }
    }

}

