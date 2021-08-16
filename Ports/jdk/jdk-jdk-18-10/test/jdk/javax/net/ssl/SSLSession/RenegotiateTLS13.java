/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @run main/othervm -Djavax.net.debug=ssl RenegotiateTLS13
 */

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;

public class RenegotiateTLS13 {

    static final String dataString = "This is a test";

    // Run the server as a thread instead of the client
    static boolean separateServerThread = false;

    static String pathToStores = "../etc";
    static String keyStoreFile = "keystore";
    static String trustStoreFile = "truststore";
    static String passwd = "passphrase";

    // Server ready flag
    volatile static boolean serverReady = false;
    // Turn on SSL debugging
    static boolean debug = false;
    // Server done flag
    static boolean done = false;

    // Main server code

    void doServerSide() throws Exception {
        SSLServerSocketFactory sslssf;
            sslssf = initContext().getServerSocketFactory();
        SSLServerSocket sslServerSocket =
            (SSLServerSocket) sslssf.createServerSocket(serverPort);
        serverPort = sslServerSocket.getLocalPort();

        serverReady = true;

        SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();

        DataInputStream sslIS =
            new DataInputStream(sslSocket.getInputStream());
        String s = "";
        while (s.compareTo("done") != 0) {
            try {
                s = sslIS.readUTF();
                System.out.println("Received: " + s);
            } catch (IOException e) {
                throw e;
            }
        }
        done = true;
        sslSocket.close();
    }

    // Main client code
    void doClientSide() throws Exception {

        while (!serverReady) {
            Thread.sleep(5);
        }

        SSLSocketFactory sslsf;
        sslsf = initContext().getSocketFactory();

        SSLSocket sslSocket = (SSLSocket)
            sslsf.createSocket("localhost", serverPort);

        DataOutputStream sslOS =
            new DataOutputStream(sslSocket.getOutputStream());

        sslOS.writeUTF("With " + dataString);
        sslOS.writeUTF("With " + dataString);
        sslOS.writeUTF("With " + dataString);

        sslSocket.startHandshake();

        sslOS.writeUTF("With " + dataString);
        sslOS.writeUTF("With " + dataString);
        sslOS.writeUTF("With " + dataString);

        sslSocket.startHandshake();

        sslOS.writeUTF("With " + dataString);
        sslOS.writeUTF("With " + dataString);
        sslOS.writeUTF("With " + dataString);
        sslOS.writeUTF("done");

        while (!done) {
            Thread.sleep(5);
        }
        sslSocket.close();
    }

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
            System.setProperty("javax.net.debug", "ssl");

        new RenegotiateTLS13();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    RenegotiateTLS13() throws Exception {
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

    // Initialize context for TLS 1.3
    SSLContext initContext() throws Exception {
        System.out.println("Using TLS13");
        SSLContext sc = SSLContext.getInstance("TLSv1.3");
        KeyStore ks = KeyStore.getInstance(
                new File(System.getProperty("javax.net.ssl.keyStore")),
                passwd.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, passwd.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return sc;
    }
}
