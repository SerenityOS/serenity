/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8027308
 * @modules jdk.httpserver
 * @summary  verifies that HttpURLConnection does not send the zone id in the
 *           'Host' field of the header:
 *              Host: [fe80::a00:27ff:aaaa:aaaa] instead of
 *              Host: [fe80::a00:27ff:aaaa:aaaa%eth0]"
 */

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import static java.lang.System.out;
import static java.net.Proxy.NO_PROXY;

public class ZoneId {

    public static void main(String[] args) throws Exception {

        InetAddress address = getIPv6LookbackAddress();

        if (address == null) {
            out.println("Cannot find the IPv6 loopback address. Skipping test.");
            return;
        }
        String ip6_literal = address.getHostAddress();

        out.println("Found an appropriate IPv6 address: " + address);

        out.println("Starting http server...");
        HttpServer server = HttpServer.create(new InetSocketAddress(address, 0), 0);
        CompletableFuture<Headers> headers = new CompletableFuture<>();
        server.createContext("/", createCapturingHandler(headers));
        server.start();
        out.println("Started at " + server.getAddress());

        try {
            int port = server.getAddress().getPort();
            String spec = "http://[" + address.getHostAddress() + "]:" + port;
            out.println("Client is connecting to: " + spec);
            URL url = new URL(spec);
            HttpURLConnection uc = (HttpURLConnection)url.openConnection(NO_PROXY);
            uc.getResponseCode();
        } finally {
            out.println("Shutting down the server...");
            server.stop(0);
        }

        int idx = ip6_literal.lastIndexOf('%');
        String ip6_address = ip6_literal.substring(0, idx);
        List<String> hosts = headers.get().get("Host");

        out.println("Host: " + hosts);

        if (hosts.size() != 1 || hosts.get(0).contains("%") ||
                                !hosts.get(0).contains(ip6_address)) {
            throw new RuntimeException("FAIL");
        }
    }

    static InetAddress getIPv6LookbackAddress() throws SocketException {
        out.println("Searching for the IPv6 loopback address...");
        Enumeration<NetworkInterface> is = NetworkInterface.getNetworkInterfaces();

        // The IPv6 loopback address contains a scope id, and is "connect-able".
        while (is.hasMoreElements()) {
            NetworkInterface i = is.nextElement();
            if (!i.isLoopback())
                continue;
            Optional<InetAddress> addr = i.inetAddresses()
                    .filter(x -> x instanceof Inet6Address)
                    .filter(y -> y.toString().contains("%"))
                    .findFirst();
            if (addr.isPresent())
                return addr.get();
        }

        return null;
    }

    static HttpHandler createCapturingHandler(CompletableFuture<Headers> headers) {
        return new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                headers.complete(exchange.getRequestHeaders());
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);
                exchange.close();
            }
        };
    }
}
