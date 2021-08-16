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

import java.io.*;
import java.net.*;
import java.security.KeyStore;
import javax.net.*;
import javax.net.ssl.*;

import jdk.test.lib.net.URIBuilder;

/*
 * @test
 * @bug 4423074
 * @modules java.base/sun.net.www
 * @summary This test case is written to test the https POST through a proxy.
 *          There is no proxy authentication done. It includes a simple server
 *          that serves http POST method requests in secure channel, and a client
 *          that makes https POST request through a proxy.
 * @library /test/lib
 * @compile OriginServer.java ProxyTunnelServer.java
 * @run main/othervm PostThruProxy
 */
public class PostThruProxy {

    private static final String TEST_SRC = System.getProperty("test.src", ".");
    private static final int TIMEOUT = 30000;

    /*
     * Where do we find the keystores?
     */
    static String pathToStores = "../../../../../../javax/net/ssl/etc";
    static String keyStoreFile = "keystore";
    static String trustStoreFile = "truststore";
    static String passwd = "passphrase";

    private static int serverPort = 0;
    private static ProxyTunnelServer pserver;
    private static TestServer server;
    static final String RESPONSE_MSG = "Https POST thru proxy is successful";

    /*
     * The TestServer implements a OriginServer that
     * processes HTTP requests and responses.
     */
    static class TestServer extends OriginServer {
        public TestServer(ServerSocket ss) throws Exception {
            super(ss);
        }

        /*
         * Returns an array of bytes containing the bytes for
         * the data sent in the response.
         *
         * @return bytes for the data in the response
         */
        public byte[] getBytes() {
            return RESPONSE_MSG.getBytes();
        }
    }

    /*
     * Main method to create the server and client
     */
    public static void main(String args[]) throws Exception {

        String keyFilename = TEST_SRC + "/" + pathToStores + "/" + keyStoreFile;
        String trustFilename = TEST_SRC + "/" + pathToStores + "/"
                + trustStoreFile;

        System.setProperty("javax.net.ssl.keyStore", keyFilename);
        System.setProperty("javax.net.ssl.keyStorePassword", passwd);
        System.setProperty("javax.net.ssl.trustStore", trustFilename);
        System.setProperty("javax.net.ssl.trustStorePassword", passwd);

        InetAddress loopback = InetAddress.getLoopbackAddress();
        boolean useSSL = true;
        /*
         * setup the server
         */
        try {
            ServerSocketFactory ssf = getServerSocketFactory(useSSL);
            ServerSocket ss = ssf.createServerSocket(serverPort, 0, loopback);
            ss.setSoTimeout(TIMEOUT);  // 30 seconds
            serverPort = ss.getLocalPort();
            server = new TestServer(ss);
            System.out.println("Server started at: " + ss);
        } catch (Exception e) {
            System.out.println("Server side failed:" +
                                e.getMessage());
            throw e;
        }
        // trigger the client
        try {
            doClientSide();
        } catch (Exception e) {
            System.out.println("Client side failed: " +
                                e.getMessage());
            throw e;
        }
        long connectCount = pserver.getConnectCount();
        if (connectCount == 0) {
            throw new AssertionError("Proxy was not used!");
        } else {
            System.out.println("Proxy CONNECT count: " + connectCount);
        }
    }

    private static ServerSocketFactory getServerSocketFactory
                   (boolean useSSL) throws Exception {
        if (useSSL) {
            // set up key manager to do server authentication
            SSLContext ctx = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            KeyStore ks = KeyStore.getInstance("JKS");
            char[] passphrase = passwd.toCharArray();

            ks.load(new FileInputStream(System.getProperty(
                        "javax.net.ssl.keyStore")), passphrase);
            kmf.init(ks, passphrase);
            ctx.init(kmf.getKeyManagers(), null, null);

            return ctx.getServerSocketFactory();
        } else {
            return ServerSocketFactory.getDefault();
        }
    }

    /*
     * Message to be posted
     */
    static String postMsg = "Testing HTTP post on a https server";

    static void doClientSide() throws Exception {
        HostnameVerifier reservedHV =
            HttpsURLConnection.getDefaultHostnameVerifier();
        try {
            /*
             * setup up a proxy
             */
            SocketAddress pAddr = setupProxy();

            /*
             * we want to avoid URLspoofCheck failures in cases where the cert
             * DN name does not match the hostname in the URL.
             */
            HttpsURLConnection.setDefaultHostnameVerifier(
                                          new NameVerifier());
            URL url = URIBuilder.newBuilder()
                      .scheme("https")
                      .loopback()
                      .port(serverPort)
                      .toURL();

            Proxy p = new Proxy(Proxy.Type.HTTP, pAddr);
            System.out.println("Client connecting to: " + url);
            System.out.println("Through proxy: " + pAddr);
            HttpsURLConnection https = (HttpsURLConnection)url.openConnection(p);
            https.setConnectTimeout(TIMEOUT);
            https.setReadTimeout(TIMEOUT);
            https.setDoOutput(true);
            https.setRequestMethod("POST");
            PrintStream ps = null;
            try {
               ps = new PrintStream(https.getOutputStream());
               ps.println(postMsg);
               ps.flush();
               if (https.getResponseCode() != 200) {
                    throw new RuntimeException("test Failed");
               }
               ps.close();

               // clear the pipe
               BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                    https.getInputStream()));
               String inputLine;
               boolean msgFound = false;
               while ((inputLine = in.readLine()) != null) {
                    System.out.println("Client received: " + inputLine);
                    if (inputLine.contains(RESPONSE_MSG)) msgFound = true;
               }
               in.close();
               if (!msgFound) {
                   throw new RuntimeException("POST message not found.");
               }
            } catch (SSLException e) {
                if (ps != null)
                    ps.close();
                throw e;
            } catch (SocketTimeoutException e) {
                System.out.println("Client can not get response in time: "
                        + e.getMessage());
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

    static SocketAddress setupProxy() throws IOException {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        pserver = new ProxyTunnelServer(loopback);

        // disable proxy authentication
        pserver.needUserAuth(false);
        pserver.start();
        return new InetSocketAddress(loopback, pserver.getPort());
    }

}
