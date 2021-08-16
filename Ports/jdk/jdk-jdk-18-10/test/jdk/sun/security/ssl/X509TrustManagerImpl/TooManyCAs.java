/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8206925
 * @library /javax/net/ssl/templates
 * @summary Support the certificate_authorities extension
 * @run main/othervm TooManyCAs
 * @run main/othervm -Djdk.tls.client.enableCAExtension=true TooManyCAs
 */
import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * Check if the connection can be established if the client or server trusts
 * more CAs such that it exceeds the size limit of the certificate_authorities
 * extension (2^16).
 */
public class TooManyCAs implements SSLContextTemplate {

    private static final String[][][] protocols = {
            {{"TLSv1.3"}, {"TLSv1.3"}},
            {{"TLSv1.3", "TLSv1.2"}, {"TLSv1.3"}},
            {{"TLSv1.3"}, {"TLSv1.3", "TLSv1.2"}},
    };

    private final String[] clientProtocols;
    private final String[] serverProtocols;
    private final boolean needClientAuth;

    TooManyCAs(int index, boolean needClientAuth) {
        this.clientProtocols = protocols[index][0];
        this.serverProtocols = protocols[index][1];
        this.needClientAuth = needClientAuth;
    }

    // Servers are configured before clients, increment test case after.
    void configureClientSocket(SSLSocket clientSocket) {
        System.err.print("Setting client protocol(s): ");
        Arrays.stream(clientProtocols).forEachOrdered(System.err::print);
        System.err.println();

        clientSocket.setEnabledProtocols(clientProtocols);
    }

    void configureServerSocket(SSLServerSocket serverSocket) {
        System.err.print("Setting server protocol(s): ");
        Arrays.stream(serverProtocols).forEachOrdered(System.err::print);
        System.err.println();

        serverSocket.setEnabledProtocols(serverProtocols);
        if (needClientAuth) {
            serverSocket.setNeedClientAuth(true);
        }
    }

    @Override
    public TrustManager createClientTrustManager() throws Exception {
        TrustManager trustManager =
                SSLContextTemplate.super.createClientTrustManager();
        return new BogusX509TrustManager(
                (X509TrustManager)trustManager);
    }

    @Override
    public TrustManager createServerTrustManager() throws Exception {
        TrustManager trustManager =
                SSLContextTemplate.super.createServerTrustManager();
        return new BogusX509TrustManager(
                (X509TrustManager)trustManager);
    }

    /*
     * Run the test case.
     */
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < protocols.length; i++) {
            (new TooManyCAs(i, false)).run();
            (new TooManyCAs(i, true)).run();
        }
    }

    private void run() throws Exception {
        SSLServerSocket listenSocket = null;
        SSLSocket serverSocket = null;
        ClientSocket clientSocket = null;
        try {
            SSLServerSocketFactory serversocketfactory =
                    createServerSSLContext().getServerSocketFactory();
            listenSocket =
                    (SSLServerSocket)serversocketfactory.createServerSocket(0);
            listenSocket.setNeedClientAuth(false);
            listenSocket.setEnableSessionCreation(true);
            listenSocket.setUseClientMode(false);
            configureServerSocket(listenSocket);

            System.err.println("Starting client");
            clientSocket = new ClientSocket(listenSocket.getLocalPort());
            clientSocket.start();

            System.err.println("Accepting client requests");
            serverSocket = (SSLSocket)listenSocket.accept();

            if (!clientSocket.isDone) {
                System.err.println("Waiting 3 seconds for client ");
                Thread.sleep(3000);
            }

            System.err.println("Sending data to client ...");
            String serverData = "Hi, I am server";
            BufferedWriter os = new BufferedWriter(
                    new OutputStreamWriter(serverSocket.getOutputStream()));
            os.write(serverData, 0, serverData.length());
            os.newLine();
            os.flush();
        } finally {
            if (listenSocket != null) {
                listenSocket.close();
            }

            if (serverSocket != null) {
                serverSocket.close();
            }
        }

        if (clientSocket != null && clientSocket.clientException != null) {
            throw clientSocket.clientException;
        }
    }

    private class ClientSocket extends Thread{
        boolean isDone = false;
        int serverPort = 0;
        Exception clientException;

        public ClientSocket(int serverPort) {
            this.serverPort = serverPort;
        }

        @Override
        public void run() {
            SSLSocket clientSocket = null;
            String clientData = "Hi, I am client";
            try {
                System.err.println(
                        "Connecting to server at port " + serverPort);
                SSLSocketFactory sslSocketFactory =
                        createClientSSLContext().getSocketFactory();
                clientSocket = (SSLSocket)sslSocketFactory.createSocket(
                        InetAddress.getLocalHost(), serverPort);
                configureClientSocket(clientSocket);

                System.err.println("Sending data to server ...");

                BufferedWriter os = new BufferedWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream()));
                os.write(clientData, 0, clientData.length());
                os.newLine();
                os.flush();

                System.err.println("Reading data from server");
                BufferedReader is = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                String data = is.readLine();
                System.err.println("Received Data from server: " + data);
            } catch (Exception e) {
                clientException = e;
                System.err.println("unexpected client exception: " + e);
            } finally {
                if (clientSocket != null) {
                    try {
                        clientSocket.close();
                        System.err.println("client socket closed");
                    } catch (IOException ioe) {
                        clientException = ioe;
                    }
                }

                isDone = true;
            }
        }
    }

    // Construct a bogus trust manager which has more CAs such that exceed
    // the size limit of the certificate_authorities extension (2^16).
    private static final class BogusX509TrustManager
            extends X509ExtendedTrustManager implements X509TrustManager {
        private final X509ExtendedTrustManager tm;

        private BogusX509TrustManager(X509TrustManager trustManager) {
            this.tm = (X509ExtendedTrustManager)trustManager;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain,
               String authType, Socket socket) throws CertificateException {
            tm.checkClientTrusted(chain, authType, socket);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain,
               String authType, Socket socket) throws CertificateException {
            tm.checkServerTrusted(chain, authType, socket);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain,
            String authType, SSLEngine sslEngine) throws CertificateException {

            tm.checkClientTrusted(chain, authType, sslEngine);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain,
            String authType, SSLEngine sslEngine) throws CertificateException {

            tm.checkServerTrusted(chain, authType, sslEngine);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain,
               String authType) throws CertificateException {
            tm.checkServerTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain,
               String authType) throws CertificateException {
            tm.checkServerTrusted(chain, authType);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            X509Certificate[] trustedCerts = tm.getAcceptedIssuers();
            int sizeAccount = 0;
            for (X509Certificate cert: trustedCerts) {
                X500Principal x500Principal = cert.getSubjectX500Principal();
                byte[] encodedPrincipal = x500Principal.getEncoded();
                sizeAccount += encodedPrincipal.length;
            }

            // 0xFFFF: the size limit of the certificate_authorities extension
            int duplicated = (0xFFFF + sizeAccount) / sizeAccount;
            X509Certificate[] returnedCAs =
                    new X509Certificate[trustedCerts.length * duplicated];
            for (int i = 0; i < duplicated; i++) {
                System.arraycopy(trustedCerts, 0,
                    returnedCAs,
                    i * trustedCerts.length + 0, trustedCerts.length);
            }

            return returnedCAs;
        }
    }
}
