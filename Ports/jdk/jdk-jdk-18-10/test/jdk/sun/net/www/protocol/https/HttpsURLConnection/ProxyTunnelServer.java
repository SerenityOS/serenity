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

/**
 *
 * This class includes a proxy server that processes HTTP CONNECT requests,
 * and tunnels the data from the client to the server, once the CONNECT
 * request is accepted.
 * The proxy server processes only one transaction, i.e  a CONNECT request
 * followed by the corresponding tunnel data.
 */

import java.io.*;
import java.net.*;
import javax.net.ServerSocketFactory;
import sun.net.www.*;
import java.util.Base64;

public class ProxyTunnelServer extends Thread {

    private static final int TIMEOUT = 30000;

    private static ServerSocket ss = null;
    /*
     * holds the registered user's username and password
     * only one such entry is maintained
     */
    private String userPlusPass;

    // client requesting for a tunnel
    private Socket clientSocket = null;

    /*
     * Origin server's address and port that the client
     * wants to establish the tunnel for communication.
     */
    private InetAddress serverInetAddr;
    private int serverPort;

    /*
     * denote whether the proxy needs to authorize
     * CONNECT requests.
     */
    static boolean needAuth = false;

    volatile long connectCount;

    public ProxyTunnelServer() throws IOException {
        if (ss == null) {
            ss = (ServerSocket) ServerSocketFactory.getDefault()
                    .createServerSocket(0);
            ss.setSoTimeout(TIMEOUT);
            System.out.println("Proxy server created: " + ss);
        }
    }

    public ProxyTunnelServer(InetAddress address) throws IOException {
        if (ss == null) {
            ss = (ServerSocket) ServerSocketFactory.getDefault()
                    .createServerSocket(0, 0, address);
            ss.setSoTimeout(TIMEOUT);
            System.out.println("Proxy server created: " + ss);
        }
    }

    public void needUserAuth(boolean auth) {
        needAuth = auth;
    }

    /*
     * register users with the proxy, by providing username and
     * password. The username and password are used for authorizing the
     * user when a CONNECT request is made and needAuth is set to true.
     */
    public void setUserAuth(String uname, String passwd) {
        userPlusPass = uname + ":" + passwd;
    }

    public void run() {
        try {
            System.out.println("Proxy server listening at: " + ss);
            clientSocket = ss.accept();
            System.out.println("Proxy server accepted connection: " + clientSocket);
            processRequests();
        } catch (SocketTimeoutException e) {
            System.out.println(
                    "Proxy can not get response in time: " + e.getMessage());
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
         } finally {
            System.out.println("Proxy server: request served");
         }
    }

    /*
     * Processes the CONNECT requests, if needAuth is set to true, then
     * the name and password are extracted from the Proxy-Authorization header
     * of the request. They are checked against the one that is registered,
     * if there is a match, connection is set in tunneling mode. If
     * needAuth is set to false, Proxy-Authorization checks are not made
     */
    private void processRequests() throws Exception {

        InputStream in = clientSocket.getInputStream();
        MessageHeader mheader = new MessageHeader(in);
        String statusLine = mheader.getValue(0);

        if (statusLine.startsWith("CONNECT")) {
            synchronized(this) { connectCount++; }
            // retrieve the host and port info from the status-line
            retrieveConnectInfo(statusLine);
            if (needAuth) {
                String authInfo;
                if ((authInfo = mheader.findValue("Proxy-Authorization"))
                                         != null) {
                   if (authenticate(authInfo)) {
                        needAuth = false;
                        System.out.println(
                                "Proxy: client authentication successful");
                   }
                }
            }
            respondForConnect(needAuth);

            // connection set to the tunneling mode
            if (!needAuth) {
                doTunnel();
                /*
                 * done with tunneling, we process only one successful
                 * tunneling request
                 */
                ss.close();
            } else {
                // we may get another request with Proxy-Authorization set
                in.close();
                clientSocket.close();
                restart();
            }
        } else {
            System.out.println("proxy server: processes only "
                                   + "CONNECT method requests, recieved: "
                                   + statusLine);
        }
    }

    public long getConnectCount() {
        return connectCount;
    }

    private void respondForConnect(boolean needAuth) throws Exception {

        OutputStream out = clientSocket.getOutputStream();
        PrintWriter pout = new PrintWriter(out);

        if (needAuth) {
            pout.println("HTTP/1.1 407 Proxy Auth Required");
            pout.println("Proxy-Authenticate: Basic realm=\"WallyWorld\"");
            pout.println();
            pout.flush();
            out.close();
        } else {
            pout.println("HTTP/1.1 200 OK");
            pout.println();
            pout.flush();
        }
    }

    private void restart() throws IOException {
         (new Thread(this)).start();
    }

    /*
     * note: Tunneling has to be provided in both directions, i.e
     * from client->server and server->client, even if the application
     * data may be unidirectional, SSL handshaking data flows in either
     * direction.
     */
    private void doTunnel() throws Exception {

        Socket serverSocket = new Socket(serverInetAddr, serverPort);
        ProxyTunnel clientToServer = new ProxyTunnel(
                                clientSocket, serverSocket);
        ProxyTunnel serverToClient = new ProxyTunnel(
                                serverSocket, clientSocket);
        clientToServer.start();
        serverToClient.start();
        System.out.println("Proxy: Started tunneling.......");

        clientToServer.join(TIMEOUT);
        serverToClient.join(TIMEOUT);
        System.out.println("Proxy: Finished tunneling........");

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

        public ProxyTunnel(Socket sockIn, Socket sockOut)
        throws Exception {
            this.sockIn = sockIn;
            this.sockOut = sockOut;
            input = sockIn.getInputStream();
            output = sockOut.getOutputStream();
        }

        public void run() {
            int BUFFER_SIZE = 400;
            byte[] buf = new byte[BUFFER_SIZE];
            int bytesRead = 0;

            try {
                while ((bytesRead = input.read(buf)) >= 0) {
                    output.write(buf, 0, bytesRead);
                    output.flush();
                }
            } catch (IOException e) {
                /*
                 * The peer end has closed the connection
                 * we will close the tunnel
                 */
                close();
              }
        }

        private void close() {
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
    private void retrieveConnectInfo(String connectStr) throws Exception {

        int starti;
        int endi;
        String connectInfo;
        String serverName = null;
        try {
            starti = connectStr.indexOf(' ');
            endi = connectStr.lastIndexOf(' ');
            connectInfo = connectStr.substring(starti+1, endi).trim();
            // retrieve server name and port
            if (connectInfo.charAt(0) == '[') {
                 endi = connectInfo.indexOf(']');
                 serverName = connectInfo.substring(1, endi++);
                 assert connectInfo.charAt(endi) == ':' : "Expected [IPv6]:port";
            } else {
                 endi = connectInfo.indexOf(':');
                 serverName = connectInfo.substring(0, endi);
            }
            serverPort = Integer.parseInt(connectInfo.substring(endi+1));
        } catch (Exception e) {
            throw new IOException("Proxy recieved a request: "
                                        + connectStr, e);
        }
        serverInetAddr = InetAddress.getByName(serverName);
    }

    public int getPort() {
        return ss.getLocalPort();
    }

    /*
     * do "basic" authentication, authInfo is of the form:
     *                                  Basic <encoded username":"password>
     * reference RFC 2617
     */
    private boolean authenticate(String authInfo) throws IOException {
        boolean matched = false;
        try {
            authInfo.trim();
            int ind = authInfo.indexOf(' ');
            String recvdUserPlusPass = authInfo.substring(ind + 1).trim();
            // extract encoded (username:passwd
            if (userPlusPass.equals(
                            new String( Base64.getMimeDecoder()
                                        .decode(recvdUserPlusPass))))
            {
                matched = true;
            }
        } catch (Exception e) {
              throw new IOException(
                "Proxy received invalid Proxy-Authorization value: "
                 + authInfo);
          }
        return matched;
    }
}
