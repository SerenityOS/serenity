/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4799427
 * @summary Https can not retry request
 * @library /test/lib
 * @run main/othervm RetryHttps
 * @run main/othervm -Djava.net.preferIPv6Addresses=true RetryHttps
 *
 *     SunJSSE does not support dynamic system properties, no way to re-use
 *     system properties in samevm/agentvm mode.
 * @author Yingxian Wang
 */

import java.net.*;
import java.util.*;
import java.io.*;
import javax.net.ssl.*;
import jdk.test.lib.net.URIBuilder;

public class RetryHttps {
    static Map cookies;
    ServerSocket ss;

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
    static String pathToStores = "../../../../../../javax/net/ssl/etc";
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
    static boolean debug = true;

    private SSLServerSocket sslServerSocket = null;

    /*
     * Define the server side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doServerSide() throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        SSLServerSocketFactory sslssf =
            (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        sslServerSocket =
            (SSLServerSocket) sslssf.createServerSocket(serverPort, 0, loopback);
        serverPort = sslServerSocket.getLocalPort();

        System.out.println("Starting server at: "
                            +  sslServerSocket.getInetAddress()
                            + ":" + serverPort);

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady = true;
        SSLSocket sslSocket = null;
        try {
            for (int i = 0; i < 2; i++) {
            sslSocket = (SSLSocket) sslServerSocket.accept();
            // read request
            InputStream is = sslSocket.getInputStream ();
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            boolean flag = false;
            String x;
            while ((x=r.readLine()) != null) {
                if (x.length() ==0) {
                    break;
                }
            }

            PrintStream out = new PrintStream(
                                 new BufferedOutputStream(
                                    sslSocket.getOutputStream() ));

            /* send the header */
            out.print("HTTP/1.1 200 OK\r\n");
            out.print("Content-Type: text/html; charset=iso-8859-1\r\n");
            out.print("Content-Length: "+10+"\r\n");
            out.print("\r\n");
            out.print("Testing"+i+"\r\n");
            out.flush();
            sslSocket.close();
            }


            sslServerSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Define the client side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doClientSide() throws Exception {
        HostnameVerifier reservedHV =
            HttpsURLConnection.getDefaultHostnameVerifier();
        try {
            /*
             * Wait for server to get started.
             */
            while (!serverReady) {
                Thread.sleep(50);
            }
            try {
                HttpsURLConnection http = null;
                /* establish http connection to server */
               URL url = URIBuilder.newBuilder()
                      .scheme("https")
                      .loopback()
                      .port(serverPort)
                      .path("/file1")
                      .toURL();
                System.out.println("url is "+url.toString());
                HttpsURLConnection.setDefaultHostnameVerifier(
                                                        new NameVerifier());
                http = (HttpsURLConnection)url.openConnection(Proxy.NO_PROXY);
                int respCode = http.getResponseCode();
                int cl = http.getContentLength();
                InputStream is = http.getInputStream ();
                int count = 0;
                while (is.read() != -1 && count++ < cl);
                System.out.println("respCode1 = "+respCode);
                Thread.sleep(2000);
                url = URIBuilder.newBuilder()
                      .scheme("https")
                      .loopback()
                      .port(serverPort)
                      .path("/file2")
                      .toURL();
                http = (HttpsURLConnection)url.openConnection(Proxy.NO_PROXY);
                respCode = http.getResponseCode();
                System.out.println("respCode2 = "+respCode);
            } catch (IOException ioex) {
                if (sslServerSocket != null)
                    sslServerSocket.close();
                throw ioex;
            }
        } finally {
            HttpsURLConnection.setDefaultHostnameVerifier(reservedHV);
        }
    }

    static class NameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
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

    public static void main(String args[]) throws Exception {
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
        new RetryHttps();
    }

    Thread clientThread = null;
    Thread serverThread = null;
    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    RetryHttps() throws Exception {
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
                        System.err.println("Server died...");
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
