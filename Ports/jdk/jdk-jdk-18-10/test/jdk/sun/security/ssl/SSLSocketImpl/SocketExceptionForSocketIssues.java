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

//
// Please run in othervm mode.  SunJSSE does not support dynamic system
// properties, no way to re-use system properties in samevm/agentvm mode.
//

/*
 * @test
 * @bug 8214339
 * @summary SSLSocketImpl erroneously wraps SocketException
 * @library /javax/net/ssl/templates
 * @run main/othervm SocketExceptionForSocketIssues
 */

import javax.net.ssl.*;
import java.io.*;
import java.net.*;

public class SocketExceptionForSocketIssues implements SSLContextTemplate {

    public static void main(String[] args) throws Exception {
        System.err.println("===================================");
        new SocketExceptionForSocketIssues().test();
    }

    private void test() throws Exception {
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
        } catch (SSLProtocolException | SSLHandshakeException sslhe) {
            throw sslhe;
        } catch (SocketException se) {
            // the expected exception, ignore it
            System.err.println("server exception: " + se);
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
                clientSocket.setSoLinger(true, 3);
                clientSocket.setSoTimeout(100);


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
            } catch (SSLProtocolException | SSLHandshakeException sslhe) {
                clientException = sslhe;
                System.err.println("unexpected client exception: " + sslhe);
            } catch (SSLException | SocketTimeoutException ssle) {
                // the expected exception, ignore it
                System.err.println("expected client exception: " + ssle);
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
}
