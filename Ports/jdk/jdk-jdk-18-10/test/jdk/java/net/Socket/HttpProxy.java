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
 * @bug 6370908 8220663
 * @library /test/lib
 * @summary Add support for HTTP_CONNECT proxy in Socket class.
 * This test uses the wildcard address and is susceptible to fail intermittently.
 * @key intermittent
 * @modules java.base/sun.net.www
 * @run main HttpProxy
 * @run main/othervm -Djava.net.preferIPv4Stack=true HttpProxy
 * @run main/othervm -Djava.net.preferIPv6Addresses=true HttpProxy
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import static java.lang.System.out;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import jdk.test.lib.net.IPSupport;
import sun.net.www.MessageHeader;

public class HttpProxy {
    final String proxyHost;
    final int proxyPort;
    static final int SO_TIMEOUT = 15000;

    public static void main(String[] args) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();

        String host;
        int port;
        ConnectProxyTunnelServer proxy = null;
        if (args.length == 0) {
            // Start internal proxy
            proxy = new ConnectProxyTunnelServer();
            proxy.start();
            host = InetAddress.getLoopbackAddress().getHostAddress();
            port = proxy.getLocalPort();
            out.println("Running with internal proxy: " + host + ":" + port);
        } else if (args.length == 2) {
            host = args[0];
            port = Integer.valueOf(args[1]);
            out.println("Running against specified proxy server: " + host + ":" + port);
        } else {
            System.err.println("Usage: java HttpProxy [<proxy host> <proxy port>]");
            return;
        }

        try {
            HttpProxy p = new HttpProxy(host, port);
            p.test();
        } finally {
            if (proxy != null)
                proxy.close();
        }
    }

    public HttpProxy(String proxyHost, int proxyPort) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    static boolean canUseIPv6() {
        return IPSupport.hasIPv6() && !IPSupport.preferIPv4Stack();
    }

    void test() throws Exception {
        InetSocketAddress proxyAddress = new InetSocketAddress(proxyHost, proxyPort);
        Proxy httpProxy = new Proxy(Proxy.Type.HTTP, proxyAddress);

        // Wildcard address is needed here
        try (ServerSocket ss = new ServerSocket(0)) {
            List<InetSocketAddress> externalAddresses = new ArrayList<>();
            externalAddresses.add(
                new InetSocketAddress(InetAddress.getLocalHost(), ss.getLocalPort()));

            if (canUseIPv6()) {
                byte[] bytes = new byte[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1};
                var address = InetAddress.getByAddress(bytes);
                externalAddresses.add(
                        new InetSocketAddress(address, ss.getLocalPort()));
            }

            for (SocketAddress externalAddress : externalAddresses) {
                try (Socket sock = new Socket(httpProxy)) {
                    sock.setSoTimeout(SO_TIMEOUT);
                    sock.setTcpNoDelay(false);

                    out.println("Trying to connect to server socket on " + externalAddress);
                    sock.connect(externalAddress);
                    try (Socket externalSock = ss.accept()) {
                        // perform some simple checks
                        check(sock.isBound(), "Socket is not bound");
                        check(sock.isConnected(), "Socket is not connected");
                        check(!sock.isClosed(), "Socket should not be closed");
                        check(sock.getSoTimeout() == SO_TIMEOUT,
                                "Socket should have a previously set timeout");
                        check(sock.getTcpNoDelay() == false, "NODELAY should be false");

                        simpleDataExchange(sock, externalSock);
                    }
                }
            }
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) out.println(message);
    }

    static Exception unexpected(Exception e) {
        out.println("Unexpected Exception: " + e);
        e.printStackTrace();
        return e;
    }

    // performs a simple exchange of data between the two sockets
    // and throws an exception if there is any problem.
    void simpleDataExchange(Socket s1, Socket s2) throws Exception {
        try (final InputStream i1 = s1.getInputStream();
             final InputStream i2 = s2.getInputStream();
             final OutputStream o1 = s1.getOutputStream();
             final OutputStream o2 = s2.getOutputStream()) {
            startSimpleWriter("simpleWriter1", o1, 100);
            startSimpleWriter("simpleWriter2", o2, 200);
            simpleRead(i2, 100);
            simpleRead(i1, 200);
        }
    }

    void startSimpleWriter(String threadName, final OutputStream os, final int start) {
        (new Thread(new Runnable() {
            public void run() {
                try { simpleWrite(os, start); }
                catch (Exception e) {unexpected(e); }
                finally { out.println(threadName + ": done"); }
            }}, threadName)).start();
    }

    void simpleWrite(OutputStream os, int start) throws Exception {
        byte b[] = new byte[2];
        for (int i=start; i<start+100; i++) {
            b[0] = (byte) (i / 256);
            b[1] = (byte) (i % 256);
            os.write(b);
        }
        out.println("Wrote " + start + " -> " + (start + 100));
    }

    void simpleRead(InputStream is, int start) throws Exception {
        byte b[] = new byte [2];
        for (int i=start; i<start+100; i++) {
            int x = is.read(b);
            if (x == 1)
                x += is.read(b,1,1);
            if (x!=2)
                throw new Exception("read error");
            int r = bytes(b[0], b[1]);
            if (r != i)
                throw new Exception("read " + r + " expected " +i);
        }
        out.println("Read " + start + " -> " + (start + 100));
    }

    int bytes(byte b1, byte b2) {
        int i1 = (int)b1 & 0xFF;
        int i2 = (int)b2 & 0xFF;
        return i1 * 256 + i2;
    }

    static class ConnectProxyTunnelServer extends Thread implements AutoCloseable {

        private final ServerSocket ss;
        private volatile boolean closed;

        public ConnectProxyTunnelServer() throws IOException {
            ss = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
        }

        @Override
        public void run() {
            try {
                while (!closed) {
                    try (Socket clientSocket = ss.accept()) {
                        processRequest(clientSocket);
                    }
                }
            } catch (Exception e) {
                if (!closed) {
                    out.println("Proxy Failed: " + e);
                    e.printStackTrace();
                }
            } finally {
                if (!closed)
                    try { ss.close(); } catch (IOException x) { unexpected(x); }
            }
        }

        /**
         * Returns the port on which the proxy is accepting connections.
         */
        public int getLocalPort() {
            return ss.getLocalPort();
        }

        @Override
        public void close() throws Exception {
            closed = true;
            ss.close();
        }

        /*
         * Processes the CONNECT request
         */
        private void processRequest(Socket clientSocket) throws Exception {
            MessageHeader mheader = new MessageHeader(clientSocket.getInputStream());
            String statusLine = mheader.getValue(0);

            if (!statusLine.startsWith("CONNECT")) {
                out.println("proxy server: processes only "
                                  + "CONNECT method requests, received: "
                                  + statusLine);
                return;
            }

            // retrieve the host and port info from the status-line
            InetSocketAddress serverAddr = getConnectInfo(statusLine);
            out.println("Proxy serving CONNECT request to " + serverAddr);

            //open socket to the server
            try (Socket serverSocket = new Socket(serverAddr.getAddress(),
                                                  serverAddr.getPort())) {
                Forwarder clientFW = new Forwarder(clientSocket.getInputStream(),
                                                   serverSocket.getOutputStream());
                Thread clientForwarderThread = new Thread(clientFW, "ClientForwarder");
                clientForwarderThread.start();
                send200(clientSocket);
                Forwarder serverFW = new Forwarder(serverSocket.getInputStream(),
                                                   clientSocket.getOutputStream());
                serverFW.run();
                clientForwarderThread.join();
            }
        }

        private void send200(Socket clientSocket) throws IOException {
            OutputStream out = clientSocket.getOutputStream();
            PrintWriter pout = new PrintWriter(out);

            pout.println("HTTP/1.1 200 OK");
            pout.println();
            pout.flush();
        }

        /*
         * This method retrieves the hostname and port of the tunnel destination
         * from the request line.
         * @param connectStr
         *        of the form: <i>CONNECT server-name:server-port HTTP/1.x</i>
         */
        static InetSocketAddress getConnectInfo(String connectStr)
            throws Exception
        {
            try {
                int starti = connectStr.indexOf(' ');
                int endi = connectStr.lastIndexOf(' ');
                String connectInfo = connectStr.substring(starti+1, endi).trim();
                // retrieve server name and port
                endi = connectInfo.lastIndexOf(':');
                String name = connectInfo.substring(0, endi);

                if (name.contains(":")) {
                    if (!(name.startsWith("[") && name.endsWith("]"))) {
                        throw new IOException("Invalid host:" + name);
                    }
                    name = name.substring(1, name.length() - 1);
                }
                int port = Integer.parseInt(connectInfo.substring(endi+1));
                return new InetSocketAddress(name, port);
            } catch (Exception e) {
                out.println("Proxy received a request: " + connectStr);
                throw unexpected(e);
            }
        }
    }

    /* Reads from the given InputStream and writes to the given OutputStream */
    static class Forwarder implements Runnable
    {
        private final InputStream in;
        private final OutputStream os;

        Forwarder(InputStream in, OutputStream os) {
            this.in = in;
            this.os = os;
        }

        @Override
        public void run() {
            try {
                byte[] ba = new byte[1024];
                int count;
                while ((count = in.read(ba)) != -1) {
                    os.write(ba, 0, count);
                }
            } catch (IOException e) {
                unexpected(e);
            }
        }
    }
}
