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
 * @bug 4432868
 * @summary A client-hello message may not always be read correctly
 * @modules java.base/sun.net.www
 * @run main/othervm ClientHelloRead
 *
 *     SunJSSE does not support dynamic system properties, no way to re-use
 *     system properties in samevm/agentvm mode.
 */

import java.io.*;
import java.net.*;
import java.security.KeyStore;
import javax.net.*;
import javax.net.ssl.*;
import java.security.cert.*;

/*
 * ClientHelloRead.java -- includes a simple server that can serve
 * Http get request in both clear and secure channel, and a client
 * that makes https requests behind the firewall through a
 * proxy. The proxy delays the bytes sent from client to the server
 * The server should read all delayed bytes to construct a correct
 * V2 ClientHello Message, if it doesn't, it will throw a SSL
 * handshake exception
 */

public class ClientHelloRead {
    /*
     * Where do we find the keystores?
     */
    static String pathToStores = "../../../../javax/net/ssl/etc";
    static String keyStoreFile = "keystore";
    static String trustStoreFile = "truststore";
    static String passwd = "passphrase";

    volatile private static int serverPort = 0;

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
            return "SSL V2 Client Hello read was successful..".
                        getBytes();
        }
    }

    /*
     * Main method to create the server and client
     */
    public static void main(String args[]) throws Exception
    {
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

        boolean useSSL = true;
        /*
         * setup the server
         */
        try {
            ServerSocketFactory ssf =
                ClientHelloRead.getServerSocketFactory(useSSL);
            ServerSocket ss = ssf.createServerSocket(serverPort);
            serverPort = ss.getLocalPort();
            new TestServer(ss);
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
    }

    private static ServerSocketFactory getServerSocketFactory
                   (boolean useSSL) throws Exception {
        if (useSSL) {
            SSLServerSocketFactory ssf = null;
            // set up key manager to do server authentication
            SSLContext ctx;
            KeyManagerFactory kmf;
            KeyStore ks;
            char[] passphrase = passwd.toCharArray();

            ctx = SSLContext.getInstance("TLS");
            kmf = KeyManagerFactory.getInstance("SunX509");
            ks = KeyStore.getInstance("JKS");

            ks.load(new FileInputStream(System.getProperty(
                        "javax.net.ssl.keyStore")), passphrase);
            kmf.init(ks, passphrase);
            ctx.init(kmf.getKeyManagers(), null, null);

            ssf = ctx.getServerSocketFactory();
            return ssf;
        } else {
            return ServerSocketFactory.getDefault();
        }
    }

    static void doClientSide() throws Exception {
        /*
         * setup up a proxy
         */
        setupProxy();

        /*
         * we want to avoid URLspoofCheck failures in cases where the cert
         * DN name does not match the hostname in the URL.
         */
        HostnameVerifier reservedHV =
            HttpsURLConnection.getDefaultHostnameVerifier();
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(
                                          new NameVerifier());
            URL url = new URL("https://" + "localhost:" + serverPort
                                    + "/index.html");
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(
                                   url.openStream()));
                String inputLine;
                System.out.print("Client recieved from the server: ");
                while ((inputLine = in.readLine()) != null)
                    System.out.println(inputLine);
                in.close();
            } catch (SSLException e) {
                if (in != null)
                    in.close();
                throw e;
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

    static void setupProxy() throws IOException {
        ProxyTunnelServer pserver = new ProxyTunnelServer();
        pserver.start();
        System.setProperty("https.proxyHost", "localhost");
        System.setProperty("https.proxyPort", String.valueOf(
                                        pserver.getPort()));
    }

}
