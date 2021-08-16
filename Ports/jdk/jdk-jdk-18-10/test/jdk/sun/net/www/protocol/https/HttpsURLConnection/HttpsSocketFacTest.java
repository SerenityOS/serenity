/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6614957
 * @summary HttpsURLConnection not using the set SSLSocketFactory for creating all its Sockets
 * @modules jdk.httpserver
 * @library /test/lib
 * @run main/othervm HttpsSocketFacTest
 *
 *     SunJSSE does not support dynamic system properties, no way to re-use
 *     system properties in samevm/agentvm mode.
 */

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.Proxy;
import java.security.NoSuchAlgorithmException;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import jdk.test.lib.net.URIBuilder;

/*
 * This class tests that the HTTPS protocol handler is using its socket factory for
 * creating new Sockets. It does this by wrapping the default SSLSocketFactory with
 * its own socket factory, SimpleSSLSocketFactory, and verifying that when a https
 * connection is made one of the socket factories createSocket methods, that
 * actually creates a Socket, is being invoked by the protocol handler.
 */

public class HttpsSocketFacTest
{
    /*
     * Where do we find the keystores?
     */
    static String pathToStores = "../../../../../../javax/net/ssl/etc";
    static String keyStoreFile = "keystore";
    static String trustStoreFile = "truststore";
    static String passwd = "passphrase";

    com.sun.net.httpserver.HttpsServer httpsServer;
    MyHandler httpHandler;

    public static void main(String[] args) {
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

        new HttpsSocketFacTest();
    }

    public HttpsSocketFacTest() {
        try {
            startHttpsServer();
            doClient();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
           httpsServer.stop(1);
        }
    }

    void doClient() throws IOException {
        InetSocketAddress address = httpsServer.getAddress();
        URL url = URIBuilder.newBuilder()
                .scheme("https")
                .loopback()
                .port(address.getPort())
                .path("/test6614957/")
                .toURLUnchecked();
        System.out.println("trying to connect to " + url + "...");

        HttpsURLConnection uc = (HttpsURLConnection) url.openConnection(Proxy.NO_PROXY);
        SimpleSSLSocketFactory sssf = new SimpleSSLSocketFactory();
        uc.setSSLSocketFactory(sssf);
        uc.setHostnameVerifier(new AllHostnameVerifier());
        InputStream is = uc.getInputStream();

        byte[] ba = new byte[1024];
        int read = 0;
        while ((read = is.read(ba)) != -1) {
            System.out.println(new String(ba, 0, read));
        }

        System.out.println("SimpleSSLSocketFactory.socketCreated = " + sssf.socketCreated);
        System.out.println("SimpleSSLSocketFactory.socketWrapped = " + sssf.socketWrapped);

        if (!sssf.socketCreated)
            throw new RuntimeException("Failed: Socket Factory not being called to create Socket");
    }

    /**
     * Https Server
     */
    public void startHttpsServer() throws IOException, NoSuchAlgorithmException  {
        httpsServer = com.sun.net.httpserver.HttpsServer
                .create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        httpsServer.createContext("/test6614957/", new MyHandler());
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(SSLContext.getDefault()));
        httpsServer.start();
    }

    class MyHandler implements HttpHandler {
        private String message = "This is a message!";

        @Override
        public void handle(HttpExchange t) throws IOException {
            t.sendResponseHeaders(200, message.length());
            BufferedWriter writer = new BufferedWriter( new OutputStreamWriter(t.getResponseBody(), "ISO8859-1"));
            writer.write(message, 0, message.length());
            writer.close();
            t.close();
        }
    }

    /**
     * Simple wrapper on default SSLSocketFactory
     */
    class SimpleSSLSocketFactory extends SSLSocketFactory
    {
        /*
         * true if this factory has been used to create a new Socket, i.e.
         * one of the SocketFactory methods has been called.
         */
        boolean socketCreated = false;

        /*
         * true if this factory has been used to wrap a Socket, i.e.
         * the SSLSocketFactory method,
         * createSocket(Socket, String, int, boolean), has been called.
         */
        boolean socketWrapped = false;

        // methods for SocketFactory
        @Override
        public Socket createSocket() throws IOException {
            socketCreated = true;
            return SocketFactory.getDefault().createSocket();
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            socketCreated = true;
            return SocketFactory.getDefault().createSocket(host, port);
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress,
                                   int localPort) throws IOException {
            socketCreated = true;
            return SocketFactory.getDefault().createSocket(address, port, localAddress, localPort);
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            socketCreated = true;
            return SocketFactory.getDefault().createSocket(host, port);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost,
                                   int localPort) throws IOException {
            socketCreated = true;
            return SocketFactory.getDefault().createSocket(host, port, localHost, localPort);
        }

        // methods from SSLSocketFactory
        @Override
        public Socket createSocket(Socket s, String host, int port,
                                   boolean autoClose) throws IOException {
            socketWrapped = true;
            return ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket
                                                               (s, host, port, autoClose);
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return ((SSLSocketFactory) SSLSocketFactory.getDefault()).getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites()  {
             return ((SSLSocketFactory) SSLSocketFactory.getDefault()).getSupportedCipherSuites();
        }
    }

    class AllHostnameVerifier implements HostnameVerifier
    {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }
}
