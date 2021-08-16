/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6226610 6973030
 * @summary HTTP tunnel connections send user headers to proxy
 * @modules java.base/sun.net.www
 * @run main/othervm B6226610
 */

/* This class includes a proxy server that processes the HTTP CONNECT request,
 * and validates that the request does not have the user defined header in it.
 * The proxy server always returns 400 Bad Request so that the Http client
 * will not try to proceed with the connection as there is no back end http server.
 */

import java.io.*;
import java.net.*;
import sun.net.www.MessageHeader;

public class B6226610 {
    static HeaderCheckerProxyTunnelServer proxy;

    public static void main(String[] args) throws Exception
    {
        proxy = new HeaderCheckerProxyTunnelServer();
        proxy.start();

        InetAddress localHost = InetAddress.getLocalHost();
        String hostname = localHost.getHostName();
        String hostAddress = localHost.getHostAddress();

        try {
           URL u = new URL("https://" + hostname + "/");
           System.out.println("Connecting to " + u);
           InetSocketAddress proxyAddr = InetSocketAddress.createUnresolved(hostAddress, proxy.getLocalPort());
           java.net.URLConnection c = u.openConnection(new Proxy(Proxy.Type.HTTP, proxyAddr));

           /* I want this header to go to the destination server only, protected
            * by SSL
            */
           c.setRequestProperty("X-TestHeader", "value");
           c.connect();

         } catch (IOException e) {
            if ( e.getMessage().equals("Unable to tunnel through proxy. Proxy returns \"HTTP/1.1 400 Bad Request\"") )
            {
               // OK. Proxy will always return 400 so that the main thread can terminate correctly.
            }
            else
               System.out.println(e);
         } finally {
             if (proxy != null) proxy.shutdown();
         }

         if (HeaderCheckerProxyTunnelServer.failed)
            throw new RuntimeException("Test failed; see output");
    }
}

class HeaderCheckerProxyTunnelServer extends Thread
{
    public static boolean failed = false;

    private static ServerSocket ss = null;

    // client requesting for a tunnel
    private Socket clientSocket = null;

    /*
     * Origin server's address and port that the client
     * wants to establish the tunnel for communication.
     */
    private InetAddress serverInetAddr;
    private int serverPort;

    public HeaderCheckerProxyTunnelServer() throws IOException
    {
       if (ss == null) {
           ss = new ServerSocket();
           ss.bind(new InetSocketAddress(InetAddress.getLocalHost(), 0));
       }
    }

    void shutdown() {
        try { ss.close(); } catch (IOException e) {}
    }

    public void run()
    {
        try {
            clientSocket = ss.accept();
            processRequests();
        } catch (IOException e) {
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

    /**
     * Returns the port on which the proxy is accepting connections.
     */
    public int getLocalPort() {
        return ss.getLocalPort();
    }

    /*
     * Processes the CONNECT request
     */
    private void processRequests() throws IOException
    {
        InputStream in = clientSocket.getInputStream();
        MessageHeader mheader = new MessageHeader(in);
        String statusLine = mheader.getValue(0);

        if (statusLine.startsWith("CONNECT")) {
           // retrieve the host and port info from the status-line
           retrieveConnectInfo(statusLine);

           if (mheader.findValue("X-TestHeader") != null) {
               System.out.println("Proxy should not receive user defined headers for tunneled requests");
               failed = true;
           }

           // 6973030
           String value;
           if ((value = mheader.findValue("Proxy-Connection")) == null ||
                !value.equals("keep-alive")) {
               System.out.println("Proxy-Connection:keep-alive not being sent");
               failed = true;
           }

           //This will allow the main thread to terminate without trying to perform the SSL handshake.
           send400();

           in.close();
           clientSocket.close();
           ss.close();
        }
        else {
            System.out.println("proxy server: processes only "
                                   + "CONNECT method requests, recieved: "
                                   + statusLine);
        }
    }

    private void send400() throws IOException
    {
        OutputStream out = clientSocket.getOutputStream();
        PrintWriter pout = new PrintWriter(out);

        pout.println("HTTP/1.1 400 Bad Request");
        pout.println();
        pout.flush();
    }

    private void restart() throws IOException {
         (new Thread(this)).start();
    }

    /*
     * This method retrieves the hostname and port of the destination
     * that the connect request wants to establish a tunnel for
     * communication.
     * The input, connectStr is of the form:
     *                          CONNECT server-name:server-port HTTP/1.x
     */
    private void retrieveConnectInfo(String connectStr) throws IOException {

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
                                  + connectStr, e);
        }
        serverInetAddr = InetAddress.getByName(serverName);
    }
}
