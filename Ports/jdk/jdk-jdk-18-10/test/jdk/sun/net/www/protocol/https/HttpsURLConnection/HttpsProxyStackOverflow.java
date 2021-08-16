/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6670868
 * @summary StackOverFlow with bad authenticated Proxy tunnels
 * @run main/othervm HttpsProxyStackOverflow
 *
 * No way to reserve default Authenticator, need to run in othervm mode.
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.Proxy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

public class HttpsProxyStackOverflow {

    public static void main(String[] args) throws IOException {
        BadAuthProxyServer server = startServer();
        doClient(server);
    }

    static void doClient(BadAuthProxyServer server) throws IOException {
        // url doesn't matter since we will never make the connection
        URL url = new URL("https://anythingwilldo/");
        InetAddress loopback = InetAddress.getLoopbackAddress();
        String loopbackAddress = loopback.getHostAddress();
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection(
                      new Proxy(Proxy.Type.HTTP,
                      InetSocketAddress.createUnresolved(loopbackAddress, server.getPort())));
        try (InputStream is = conn.getInputStream()) {
        } catch(IOException unused) {
            // no real server, IOException is expected.
            // failure if StackOverflowError
        } finally {
            server.done();
        }
    }

    static BadAuthProxyServer startServer() throws IOException {
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("xyz", "xyz".toCharArray());
            }
            });
        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetSocketAddress address = new InetSocketAddress(loopback, 0);
        ServerSocket ss = new ServerSocket();
        ss.bind(address);
        BadAuthProxyServer server = new BadAuthProxyServer(ss);
        Thread serverThread = new Thread(server);
        serverThread.start();
        return server;
    }

    static class BadAuthProxyServer implements Runnable {
        private ServerSocket ss;
        private boolean done;

        BadAuthProxyServer(ServerSocket ss) { this.ss = ss; }

        public void run() {
            try {
               while (!done) {
                    Socket s = ss.accept();
                    s.getOutputStream().write(
                            ("HTTP/1.1 407\nProxy-Authenticate:Basic " +
                            "realm=\"WallyWorld\"\n\n").getBytes("US-ASCII"));

                    s.close();

                    s = ss.accept();
                    s.close();
                }
            } catch (IOException e) {
                // Ignore IOException when the main thread calls done
            } finally {
                try { ss.close(); } catch (IOException e) {}
            }
        }

        int getPort() {
            return ss.getLocalPort();
        }

        void done() {
            try { ss.close(); } catch (IOException e) {}
            done = true;
        }
    }
}
