/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6520665 6357133
 * @modules java.base/sun.net.www
 * @library /test/lib
 * @run main/othervm NTLMTest
 * @summary 6520665 & 6357133: NTLM authentication issues.
 */

import java.net.*;
import java.io.*;
import sun.net.www.MessageHeader;
import jdk.test.lib.net.URIBuilder;

public class NTLMTest
{
    public static void main(String[] args) throws Exception {
        Authenticator.setDefault(new NullAuthenticator());

        try {
            InetAddress loopback = InetAddress.getLoopbackAddress();

            // Test with direct connection.
            try (NTLMServer server = startServer(new ServerSocket(0, 0, loopback), false)) {
                runClient(Proxy.NO_PROXY, server.getLocalPort());
            }
            // Test with proxy.
            try (NTLMServer server =
                    startServer(new ServerSocket(0, 0, loopback), true /*proxy*/)) {
                SocketAddress proxyAddr = new InetSocketAddress(loopback, server.getLocalPort());
                runClient(new Proxy(java.net.Proxy.Type.HTTP, proxyAddr), 8888);
            }
        } catch (IOException e) {
            throw e;
        }
    }

    static void runClient(Proxy proxy, int serverPort) {
        try {
            URL url = URIBuilder.newBuilder()
                      .scheme("http")
                      .loopback()
                      .port(serverPort)
                      .path("/")
                      .toURLUnchecked();
            HttpURLConnection uc = (HttpURLConnection) url.openConnection(proxy);
            uc.getInputStream().readAllBytes();

        } catch (ProtocolException e) {
            /* java.net.ProtocolException: Server redirected too many  times (20) */
            throw new RuntimeException("Failed: ProtocolException", e);
        } catch (IOException ioe) {
            /* IOException is OK. We are expecting "java.io.IOException: Server
             * returned HTTP response code: 401 for URL: ..."
             */
            //ioe.printStackTrace();
            System.out.println("Got expected " + ioe);
        } catch (NullPointerException npe) {
            throw new RuntimeException("Failed: NPE thrown ", npe);
        }
    }

    static String[] serverResp = new String[] {
                "HTTP/1.1 401 Unauthorized\r\n" +
                "Content-Length: 0\r\n" +
                "WWW-Authenticate: NTLM\r\n\r\n",

                "HTTP/1.1 401 Unauthorized\r\n" +
                "Content-Length: 0\r\n" +
                "WWW-Authenticate: NTLM TlRMTVNTUAACAAAAAAAAACgAAAABggAAU3J2Tm9uY2UAAAAAAAAAAA==\r\n\r\n"};

    static String[] proxyResp = new String[] {
                "HTTP/1.1 407 Proxy Authentication Required\r\n" +
                "Content-Length: 0\r\n" +
                "Proxy-Authenticate: NTLM\r\n\r\n",

                "HTTP/1.1 407 Proxy Authentication Required\r\n" +
                "Content-Length: 0\r\n" +
                "Proxy-Authenticate: NTLM TlRMTVNTUAACAAAAAAAAACgAAAABggAAU3J2Tm9uY2UAAAAAAAAAAA==\r\n\r\n"};

    static class NTLMServer extends Thread implements AutoCloseable {
        final ServerSocket ss;
        final boolean isProxy;
        volatile boolean closed;

        NTLMServer(ServerSocket serverSS, boolean proxy) {
            super();
            setDaemon(true);
            ss = serverSS;
            isProxy = proxy;
        }

       public int getLocalPort() { return ss.getLocalPort(); }

        @Override
        public void run() {
            boolean doing2ndStageNTLM = false;
            while (!closed) {
                try {
                    Socket s = ss.accept();
                    if (!doing2ndStageNTLM) {
                        handleConnection(s, isProxy ? proxyResp : serverResp, 0, 1);
                        doing2ndStageNTLM = true;
                    } else {
                        handleConnection(s, isProxy ? proxyResp : serverResp, 1, 2);
                        doing2ndStageNTLM = false;
                    }
                    connectionCount++;
                    //System.out.println("connectionCount = " + connectionCount);
                } catch (IOException ioe) {
                    if (!closed) ioe.printStackTrace();
                }
            }
        }

        @Override
        public void close() {
           if (closed) return;
           synchronized(this) {
               if (closed) return;
               closed = true;
           }
           try { ss.close(); } catch (IOException x) { };
        }
    }

    public static NTLMServer startServer(ServerSocket serverSS, boolean proxy) {
        NTLMServer server = new NTLMServer(serverSS, proxy);
        server.start();
        return server;
    }

    static int connectionCount = 0;

    static void handleConnection(Socket s, String[] resp, int start, int end) {
        try {
            OutputStream os = s.getOutputStream();

            for (int i=start; i<end; i++) {
                MessageHeader header = new MessageHeader (s.getInputStream());
                //System.out.println("Input :" + header);
                //System.out.println("Output:" + resp[i]);
                os.write(resp[i].getBytes("ASCII"));
            }

            s.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    static class NullAuthenticator extends java.net.Authenticator
    {
        public int count = 0;

        protected PasswordAuthentication getPasswordAuthentication() {
            count++;
            System.out.println("NullAuthenticator.getPasswordAuthentication called " + count + " times");

            return null;
        }

        public int getCallCount() {
            return count;
        }
    }

}
