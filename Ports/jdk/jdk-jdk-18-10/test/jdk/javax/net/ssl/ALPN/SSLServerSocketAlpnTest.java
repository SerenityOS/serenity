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

// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.

/*
 * @test
 * @bug 8051498 8145849 8158978 8170282
 * @summary JEP 244: TLS Application-Layer Protocol Negotiation Extension
 * @compile MyX509ExtendedKeyManager.java
 *
 * @run main/othervm SSLServerSocketAlpnTest h2          UNUSED   h2          h2
 * @run main/othervm SSLServerSocketAlpnTest h2          UNUSED   h2,http/1.1 h2
 * @run main/othervm SSLServerSocketAlpnTest h2,http/1.1 UNUSED   h2,http/1.1 h2
 * @run main/othervm SSLServerSocketAlpnTest http/1.1,h2 UNUSED   h2,http/1.1 http/1.1
 * @run main/othervm SSLServerSocketAlpnTest h4,h3,h2    UNUSED   h1,h2       h2
 * @run main/othervm SSLServerSocketAlpnTest EMPTY       UNUSED   h2,http/1.1 NONE
 * @run main/othervm SSLServerSocketAlpnTest h2          UNUSED   EMPTY       NONE
 * @run main/othervm SSLServerSocketAlpnTest H2          UNUSED   h2          ERROR
 * @run main/othervm SSLServerSocketAlpnTest h2          UNUSED   http/1.1    ERROR
 *
 * @run main/othervm SSLServerSocketAlpnTest UNUSED      h2       h2          h2
 * @run main/othervm SSLServerSocketAlpnTest UNUSED      h2       h2,http/1.1 h2
 * @run main/othervm SSLServerSocketAlpnTest UNUSED      h2       http/1.1,h2 h2
 * @run main/othervm SSLServerSocketAlpnTest UNUSED      http/1.1 h2,http/1.1 http/1.1
 * @run main/othervm SSLServerSocketAlpnTest UNUSED      EMPTY    h2,http/1.1 NONE
 * @run main/othervm SSLServerSocketAlpnTest UNUSED      h2       EMPTY       NONE
 * @run main/othervm SSLServerSocketAlpnTest UNUSED      H2       h2          ERROR
 * @run main/othervm SSLServerSocketAlpnTest UNUSED      h2       http/1.1    ERROR
 *
 * @run main/othervm SSLServerSocketAlpnTest h2          h2       h2          h2
 * @run main/othervm SSLServerSocketAlpnTest H2          h2       h2,http/1.1 h2
 * @run main/othervm SSLServerSocketAlpnTest h2,http/1.1 http/1.1 h2,http/1.1 http/1.1
 * @run main/othervm SSLServerSocketAlpnTest http/1.1,h2 h2       h2,http/1.1 h2
 * @run main/othervm SSLServerSocketAlpnTest EMPTY       h2       h2          h2
 * @run main/othervm SSLServerSocketAlpnTest h2,http/1.1 EMPTY    http/1.1    NONE
 * @run main/othervm SSLServerSocketAlpnTest h2,http/1.1 h2       EMPTY       NONE
 * @run main/othervm SSLServerSocketAlpnTest UNUSED      UNUSED   http/1.1,h2 NONE
 * @run main/othervm SSLServerSocketAlpnTest h2          h2       http/1.1    ERROR
 * @run main/othervm SSLServerSocketAlpnTest h2,http/1.1 H2       http/1.1    ERROR
 *
 * @author Brad Wetmore
 */
/**
 * A simple SSLSocket-based client/server that demonstrates the proposed API
 * changes for JEP 244 in support of the TLS ALPN extension (RFC 7301).
 *
 * Usage:
 *     java SSLServerSocketAlpnTest
 *             <server-APs> <callback-AP> <client-APs> <result>
 *
 * where:
 *      EMPTY  indicates that ALPN is disabled
 *      UNUSED indicates that no ALPN values are supplied (server-side only)
 *      ERROR  indicates that an exception is expected
 *      NONE   indicates that no ALPN is expected
 *
 * This example is based on our standard SSLSocketTemplate.
 */
import java.io.*;
import java.security.KeyStore;
import java.util.Arrays;

import javax.net.ssl.*;

public class SSLServerSocketAlpnTest {

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
     * Where do we find the keystores?
     */
    static String pathToStores = "../etc";
    static String keyStoreFile = "keystore";
    static String trustStoreFile = "truststore";
    static String passwd = "passphrase";

    static String keyFilename = System.getProperty("test.src", ".") + "/"
            + pathToStores + "/" + keyStoreFile;
    static String trustFilename = System.getProperty("test.src", ".") + "/"
            + pathToStores + "/" + trustStoreFile;

    private static boolean hasServerAPs; // whether server APs are present
    private static boolean hasCallback; // whether a callback is present

    /*
     * SSLContext
     */
    SSLContext mySSLContext = null;

    /*
     * Is the server ready to serve?
     */
    volatile static boolean serverReady = false;

    /*
     * Turn on SSL debugging?
     */
    static boolean debug = false;

    static String[] serverAPs;
    static String callbackAP;
    static String[] clientAPs;
    static String expectedAP;

    /*
     * If the client or server is doing some kind of object creation
     * that the other side depends on, and that thread prematurely
     * exits, you may experience a hang. The test harness will
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
        SSLServerSocketFactory sslssf = mySSLContext.getServerSocketFactory();
        SSLServerSocket sslServerSocket
                = (SSLServerSocket) sslssf.createServerSocket(serverPort);
        sslServerSocket.setNeedClientAuth(true);

        SSLParameters sslp = sslServerSocket.getSSLParameters();

        // for both client/server to call into X509KM
        sslp.setNeedClientAuth(true);

        /*
         * The default ciphersuite ordering from the SSLContext may not
         * reflect "h2" ciphersuites as being preferred, additionally the
         * client may not send them in an appropriate order. We could resort
         * the suite list if so desired.
         */
        String[] suites = sslp.getCipherSuites();
        sslp.setCipherSuites(suites);
        sslp.setUseCipherSuitesOrder(true); // Set server side order

        // Set the ALPN selection.
        if (serverAPs != null) {
            sslp.setApplicationProtocols(serverAPs);
        }
        sslServerSocket.setSSLParameters(sslp);

        serverPort = sslServerSocket.getLocalPort();

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady = true;

        SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();

        if (sslSocket.getHandshakeApplicationProtocol() != null) {
            throw new Exception ("getHandshakeApplicationProtocol() should "
                    + "return null before the handshake starts");
        }

        // check that no callback has been registered
        if (sslSocket.getHandshakeApplicationProtocolSelector() != null) {
            throw new Exception("getHandshakeApplicationProtocolSelector() " +
                "should return null");
        }

        if (hasCallback) {
            sslSocket.setHandshakeApplicationProtocolSelector(
                (serverSocket, clientProtocols) -> {
                    return callbackAP.equals("EMPTY") ? "" : callbackAP;
                });

            // check that the callback can be retrieved
            if (sslSocket.getHandshakeApplicationProtocolSelector() == null) {
                throw new Exception("getHandshakeApplicationProtocolSelector()"
                    + " should return non-null");
            }
        }

        sslSocket.startHandshake();

        if (sslSocket.getHandshakeApplicationProtocol() != null) {
            throw new Exception ("getHandshakeApplicationProtocol() should "
                    + "return null after the handshake is completed");
        }

        String ap = sslSocket.getApplicationProtocol();
        System.out.println("Application Protocol: \"" + ap + "\"");

        if (ap == null) {
            throw new Exception(
                    "Handshake was completed but null was received");
        }
        if (expectedAP.equals("NONE")) {
            if (!ap.isEmpty()) {
                throw new Exception("Expected no ALPN value");
            } else {
                System.out.println("No ALPN value negotiated, as expected");
            }
        } else if (!expectedAP.equals(ap)) {
            throw new Exception(expectedAP
                    + " ALPN value not available on negotiated connection");
        }

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

        SSLSocketFactory sslsf = mySSLContext.getSocketFactory();
        SSLSocket sslSocket
                = (SSLSocket) sslsf.createSocket("localhost", serverPort);

        SSLParameters sslp = sslSocket.getSSLParameters();

        /*
         * The default ciphersuite ordering from the SSLContext may not
         * reflect "h2" ciphersuites as being preferred, additionally the
         * client may not send them in an appropriate order. We could resort
         * the suite list if so desired.
         */
        String[] suites = sslp.getCipherSuites();
        sslp.setCipherSuites(suites);
        sslp.setUseCipherSuitesOrder(true); // Set server side order

        // Set the ALPN selection.
        sslp.setApplicationProtocols(clientAPs);
        sslSocket.setSSLParameters(sslp);

        if (sslSocket.getHandshakeApplicationProtocol() != null) {
            throw new Exception ("getHandshakeApplicationProtocol() should "
                    + "return null before the handshake starts");
        }

        sslSocket.startHandshake();

        if (sslSocket.getHandshakeApplicationProtocol() != null) {
            throw new Exception ("getHandshakeApplicationProtocol() should "
                    + "return null after the handshake is completed");
        }

        /*
         * Check that the resulting connection meets our defined ALPN
         * criteria.  If we were connecting to a non-JSSE implementation,
         * the server might have negotiated something we shouldn't accept.
         */
        String ap = sslSocket.getApplicationProtocol();
        System.out.println("Application Protocol: \"" + ap + "\"");

        if (ap == null) {
            throw new Exception(
                    "Handshake was completed but null was received");
        }
        if (expectedAP.equals("NONE")) {
            if (!ap.isEmpty()) {
                throw new Exception("Expected no ALPN value");
            } else {
                System.out.println("No ALPN value negotiated, as expected");
            }
        } else if (!expectedAP.equals(ap)) {
            throw new Exception(expectedAP
                    + " ALPN value not available on negotiated connection");
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

    public static void main(String[] args) throws Exception {

        if (debug) {
            System.setProperty("javax.net.debug", "all");
        }
        System.out.println("Test args: " + Arrays.toString(args));

        // Validate parameters
        if (args.length != 4) {
            throw new Exception("Invalid number of test parameters");
        }
        serverAPs = convert(args[0]);
        callbackAP = args[1];
        clientAPs = convert(args[2]);
        expectedAP = args[3];

        hasServerAPs = !args[0].equals("UNUSED"); // are server APs being used?
        hasCallback = !callbackAP.equals("UNUSED"); // is callback being used?

        /*
         * Start the tests.
         */
        try {
            new SSLServerSocketAlpnTest();
        } catch (SSLHandshakeException she) {
            if (args[3].equals("ERROR")) {
                System.out.println("Caught the expected exception: " + she);
            } else {
                throw she;
            }
        }

        System.out.println("Test Passed.");
    }

    SSLContext getSSLContext(String keyFilename, String trustFilename)
            throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");

        // Keystores
        KeyStore keyKS = KeyStore.getInstance("JKS");
        keyKS.load(new FileInputStream(keyFilename), passwd.toCharArray());

        KeyStore trustKS = KeyStore.getInstance("JKS");
        trustKS.load(new FileInputStream(trustFilename), passwd.toCharArray());

        // Generate KeyManager and TrustManager
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyKS, passwd.toCharArray());

        KeyManager[] kms = kmf.getKeyManagers();
        if (!(kms[0] instanceof X509ExtendedKeyManager)) {
            throw new Exception("kms[0] not X509ExtendedKeyManager");
        }

        kms = new KeyManager[] { new MyX509ExtendedKeyManager(
                (X509ExtendedKeyManager) kms[0], expectedAP,
                !hasCallback && hasServerAPs) };

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(trustKS);
        TrustManager[] tms = tmf.getTrustManagers();

        // initial SSLContext
        ctx.init(kms, tms, null);

        return ctx;
    }

    /*
     * Convert a comma-separated list into an array of strings.
     */
    private static String[] convert(String list) {
        if (list.equals("UNUSED")) {
            return null;
        }

        if (list.equals("EMPTY")) {
            return new String[0];
        }

        String[] strings;
        if (list.indexOf(',') > 0) {
            strings = list.split(",");
        } else {
            strings = new String[]{ list };
        }

        return strings;
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    SSLServerSocketAlpnTest() throws Exception {
        Exception startException = null;
        mySSLContext = getSSLContext(keyFilename, trustFilename);
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

    void startServer(boolean newThread) throws Exception {
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

    void startClient(boolean newThread) throws Exception {
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
