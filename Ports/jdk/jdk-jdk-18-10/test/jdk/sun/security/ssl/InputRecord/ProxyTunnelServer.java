/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

/**
 *
 * This class is used by the regression test ClientHelloRead.java
 * This class includes a proxy server that processes HTTP CONNECT requests,
 * and tunnels the data between the client and server once the CONNECT
 * request is accepted. It is provided to introduce some delay in the network
 * traffic between the client and server.
 * This test is to make sure that ClientHello is properly read by the server,
 * while facing network delays.
 */

import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import javax.net.ServerSocketFactory;
import sun.net.www.MessageHeader;

public class ProxyTunnelServer extends Thread {

    private static ServerSocket ss = null;

    // client requesting for a tunnel
    private Socket clientSocket = null;

    /*
     * Origin server's address and port that the client
     * wants to establish the tunnel for communication.
     */
    private InetAddress serverInetAddr;
    private int serverPort;

    public ProxyTunnelServer() throws IOException {
        if (ss == null) {
          ss = (ServerSocket) ServerSocketFactory.getDefault().
          createServerSocket(0);
        }
    }

    public void run() {
        try {
            clientSocket = ss.accept();
            processRequests();
        } catch (Exception e) {
            System.out.println("Proxy Failed: " + e);
            e.printStackTrace();
            try {
                ss.close();
            }
            catch (IOException excep) {
                System.out.println("ProxyServer close error: " + excep);
                excep.printStackTrace();
            }
          }
    }

    /*
     * Processes the CONNECT requests
     */
    private void processRequests() throws Exception {

        InputStream in = clientSocket.getInputStream();
        MessageHeader response = new MessageHeader(in);
        String statusLine = response.getValue(0);

        if (statusLine.startsWith("CONNECT")) {
            // retrieve the host and port info from the response line
            retrieveConnectInfo(statusLine);
            respondForConnect();
            doTunnel();
            ss.close();
        } else {
            System.out.println("proxy server: processes only "
                                   + "CONNECT method requests, recieved: "
                                   + statusLine);
        }
    }

    private void respondForConnect() throws Exception {
        OutputStream out = clientSocket.getOutputStream();
        PrintWriter pout = new PrintWriter(out);
        pout.println("HTTP/1.1 200 OK");
        pout.println();
        pout.flush();
    }

    /*
     * note: Tunneling has to be provided in both directions, i.e
     * from client->server and server->client, even if the application
     * data may be unidirectional, SSL handshaking data flows in either
     * direction.
     */
    private void doTunnel() throws Exception {
        Socket serverSocket = new Socket(serverInetAddr, serverPort);

        // delay the write from client -> server
        ProxyTunnel clientToServer = new ProxyTunnel(
                                clientSocket, serverSocket, true);
        ProxyTunnel serverToClient = new ProxyTunnel(
                                serverSocket, clientSocket, false);
        clientToServer.start();
        serverToClient.start();

        clientToServer.join();
        serverToClient.join();

        clientToServer.close();
        serverToClient.close();
    }

    /*
     * This inner class provides unidirectional data flow through the sockets
     * by continuously copying bytes from the input socket onto the output
     * socket, until both sockets are open and EOF has not been received.
     */
    class ProxyTunnel extends Thread {
        Socket sockIn;
        Socket sockOut;
        InputStream input;
        OutputStream output;
        boolean delayedWrite;

        public ProxyTunnel(Socket sockIn, Socket sockOut, boolean delayedWrite)
        throws Exception {
            this.sockIn = sockIn;
            this.sockOut = sockOut;
            input = sockIn.getInputStream();
            output = sockOut.getOutputStream();
            this.delayedWrite = delayedWrite;
        }

        public void run() {
            // the buffer size of < 47 introduces delays in availability
            // of chunks of client handshake data
            int BUFFER_SIZE = 40;
            byte[] buf = new byte[BUFFER_SIZE];
            int bytesRead = 0;
            int count = 0;  // keep track of the amount of data transfer

            try {
                while ((bytesRead = input.read(buf)) >= 0) {
                    if (delayedWrite) {
                        try {
                            this.sleep(1);
                        } catch (InterruptedException excep) {
                            System.out.println(excep);
                          }
                    }
                    output.write(buf, 0, bytesRead);
                    output.flush();
                    count += bytesRead;
                }
            } catch (IOException e) {
                /*
                 * The peer end has closed the connection
                 * we will close the tunnel
                 */
                close();
              }
        }

        public void close() {
            try {
                if (!sockIn.isClosed())
                    sockIn.close();
                if (!sockOut.isClosed())
                    sockOut.close();
            } catch (IOException ignored) { }
        }
    }

    /*
     ***************************************************************
     *                  helper methods follow
     ***************************************************************
     */

    /*
     * This method retrieves the hostname and port of the destination
     * that the connect request wants to establish a tunnel for
     * communication.
     * The input, connectStr is of the form:
     *                          CONNECT server-name:server-port HTTP/1.x
     */
    void retrieveConnectInfo(String connectStr) throws Exception {
        int starti;
        int endi;
        String connectInfo;
        String serverName = null;
        try {
            starti = connectStr.indexOf(' ');
            endi = connectStr.lastIndexOf(' ');
            connectInfo = connectStr.substring(starti+1, endi).trim();
            // retrieve server name and port
            endi = connectInfo.indexOf(':');
            serverName = connectInfo.substring(0, endi);
            serverPort = Integer.parseInt(connectInfo.substring(endi+1));
        } catch (Exception e) {
            throw new IOException("Proxy recieved a request: "
                                        + connectStr);
          }
        serverInetAddr = InetAddress.getByName(serverName);
    }

    public int getPort() {
        return ss.getLocalPort();
    }
}
