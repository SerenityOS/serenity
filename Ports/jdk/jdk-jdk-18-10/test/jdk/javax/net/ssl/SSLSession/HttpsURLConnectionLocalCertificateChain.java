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
 * @bug 4395238 4354003 4387961 4395266
 * @summary A test of many of the new functionality to go into JSSE 1.1
 *      Fixed 4395238: The new certificate chains APIs should really be
 *          returning certs, not x509 certs
 *      Fixed 4354003: Need API to get client certificate chain
 *      Fixed 4387961: HostnameVerifier needs to pass various hostnames
 *      Fixed 4395266: HttpsURLConnection should be made protected
 * @run main/othervm HttpsURLConnectionLocalCertificateChain
 *
 *     SunJSSE does not support dynamic system properties, no way to re-use
 *     system properties in samevm/agentvm mode.
 * @author Brad Wetmore
 */

import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.security.cert.*;

public class HttpsURLConnectionLocalCertificateChain
        implements HandshakeCompletedListener,
        HostnameVerifier {

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

    /*
     * Is the server ready to serve?
     */
    volatile static boolean serverReady = false;

    /*
     * Default Verifier
     */
    public boolean verify(String hostname, SSLSession session) {
        try {
            Certificate [] certs = session.getPeerCertificates();

            for (int i = 0; i< certs.length; i++) {
                if (certs[i] instanceof X509Certificate) {
                    System.out.println("Hostname Verification cert #1: ");
                    // System.out.println(certs[i].toString());
                }
            }
        } catch (Exception e) {
            serverException = e;
        }
        return true;
    }

    /*
     * The event sent by the app.
     */
    HandshakeCompletedEvent event;

    /*
     * Provide the Listener for the HandshakeCompletedEvent
     * Store the event now, we'll examine it later as we're
     * finishing the test...
     */
    public void handshakeCompleted(HandshakeCompletedEvent theEvent) {
        event = theEvent;
    }

    void examineHandshakeCompletedEvent() throws Exception {
        /*
         * Also check the types during compile.  We changed
         * from cert.x509 to certs.
         */
        dumpCerts("examineHandshakeCompletedEvent received",
            event.getPeerCertificates());
        dumpCerts("examineHandshakeCompletedEvent sent",
            event.getLocalCertificates());
    }

    synchronized void dumpCerts(String where, Certificate [] certs)
            throws Exception {

        System.out.println("");
        System.out.println(where + ":");

        if (certs == null) {
            throw new Exception("certs == null");
        }

        for (int i = 0; i< certs.length; i++) {
            if (certs[i] instanceof X509Certificate) {
                System.out.println("cert #1: " +
                    ((X509Certificate) certs[i]).getSubjectDN());
            }
        }
    }

    void doServerSide() throws Exception {

        SSLServerSocketFactory sslssf;
        SSLServerSocket sslServerSocket;

        System.out.println("Starting Server...");
        sslssf =
            (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        sslServerSocket =
            (SSLServerSocket) sslssf.createServerSocket(serverPort);
        serverPort = sslServerSocket.getLocalPort();
        System.out.println("Kicking off Client...");

        serverReady = true;

        SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
        sslSocket.setNeedClientAuth(true);
        sslSocket.addHandshakeCompletedListener(this);

        InputStream sslIS = sslSocket.getInputStream();
        OutputStream sslOS = sslSocket.getOutputStream();
        DataOutputStream out = new DataOutputStream(sslOS);

        System.out.println("Server reading request...");
        sslIS.read();

        System.out.println("Server replying...");
        try {
            out.writeBytes("HTTP/1.0 200 OK\r\n");
            out.writeBytes("Content-Length: " + 1 + "\r\n");
            out.writeBytes("Content-Type: text/html\r\n\r\n");
            out.write(57);
            out.flush();
        } catch (IOException ie) {
            serverException = ie;
        }

        System.out.println("Server getting certs...");
        SSLSession sslSession = sslSocket.getSession();
        dumpCerts("ServerSide sent", sslSession.getLocalCertificates());
        dumpCerts("ServerSide received", sslSession.getPeerCertificates());

        /*
         * Won't bother closing IS/sockets this time, we're exiting...
         */

        /*
         * We'll eventually get this event, wait for it.
         */
        while (event == null) {
            Thread.sleep(1000);
        }

        System.out.println("Server examining Event...");
        examineHandshakeCompletedEvent();
    }

    void doClientSide() throws Exception {

        /*
         * Wait for server to get started.
         */
        while (!serverReady) {
            Thread.sleep(50);
        }

        System.out.println("Starting Client...");

        String url = "https://localhost:" + serverPort;
        System.out.println("connecting to: " + url);
        URL myURL = new URL(url);
        HttpsURLConnection myURLc;

        System.out.println("Client setting up URL/connecting...");
        myURLc = (HttpsURLConnection) myURL.openConnection();
        myURLc.setHostnameVerifier(this);
        myURLc.connect();

        InputStream sslIS = myURLc.getInputStream();

        System.out.println("Client reading...");
        sslIS.read();

        System.out.println("Client dumping certs...");

        dumpCerts("ClientSide received", myURLc.getServerCertificates());
        dumpCerts("ClientSide sent", myURLc.getLocalCertificates());

        /*
         * Won't bother closing IS/sockets this time, we're exiting...
         */
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

        /*
         * Start the tests.
         */
        new HttpsURLConnectionLocalCertificateChain();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    HttpsURLConnectionLocalCertificateChain () throws Exception {
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
                        System.out.println("Server died...");
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
                        System.out.println("Client died...");
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
