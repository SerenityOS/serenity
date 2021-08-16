/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary This test will timeout if the ALPN CF is not completed
 *          when a 'Connection reset by peer' exception is raised
 *          during the handshake.
 * @bug 8217094
 * @modules java.net.http
 *          java.logging
 * @build ALPNFailureTest
 * @run main/othervm -Djdk.internal.httpclient.debug=true ALPNFailureTest HTTP_1_1
 * @run main/othervm ALPNFailureTest HTTP_2
 */
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ALPNFailureTest {


    public static void main(String[] args) throws Exception{
        if (args == null || args.length == 0) {
            args = new String[] {HttpClient.Version.HTTP_1_1.name()};
        }
        ServerSocket socket = ServerSocketFactory.getDefault()
                .createServerSocket(0, 10, InetAddress.getLoopbackAddress());

        test(socket, null, null, args);
    }

    public static void test(ServerSocket socket, SSLContext context,
                            ProxySelector ps, String... args)
            throws Exception
    {
        System.out.println("Tests a race condition in SSLTube/SSLFlowDelegate");
        System.out.println("This test will timeout if the ALPN CF is not completed" +
                " when a 'Connection reset by peer' exception is raised" +
                " during the handshake - see 8217094.");

        URI uri = new URI("https", null,
                socket.getInetAddress().getHostAddress(), socket.getLocalPort(),
                "/ReadOnlyServer/https_1_1/", null, null);
        HttpRequest request1 = HttpRequest.newBuilder(uri)
                .GET().build();
        HttpRequest request2 = HttpRequest.newBuilder(uri)
                .POST(HttpRequest.BodyPublishers.ofString("foo")).build();

        ReadOnlyServer server = new ReadOnlyServer(socket);
        Thread serverThread = new Thread(server, "ServerThread");
        serverThread.start();
        try {
            for (var arg : args) {
                var version = HttpClient.Version.valueOf(arg);
                HttpClient.Builder builder = HttpClient.newBuilder()
                        .version(version);
                if (ps != null) builder.proxy(ps);
                if (context != null) builder.sslContext(context);

                HttpClient client = builder.build();
                for (var request : List.of(request1, request2)) {
                    System.out.println("Server is " + socket.getLocalSocketAddress()
                            + ", Version is " + version + ", Method is " + request.method()
                            + (ps == null ? ", no proxy"
                            : (", Proxy is " + ps.select(request.uri()))));
                    try {
                        HttpResponse<String> resp =
                                client.send(request, HttpResponse.BodyHandlers.ofString());
                        throw new AssertionError(
                                "Client should not have received any response: " + resp);
                    } catch (HttpTimeoutException x) {
                        System.out.println("Unexpected " + x);
                        x.printStackTrace();
                        throw new AssertionError("Unexpected exception " + x, x);
                    } catch (Exception x) {
                        // We expect IOException("Connection reset by peer"), but
                        // any exception would do: we just don't want to linger
                        // forever.
                        System.err.println("Client got expected exception: " + x);
                        x.printStackTrace(System.out);
                    }
                }
            }
        } finally {
            server.close();
        }
    }

    public static class ReadOnlyServer  implements Runnable, Closeable {
        final ServerSocket socket;
        final AtomicReference<Throwable> errorRef = new AtomicReference<>();
        final AtomicBoolean closing = new AtomicBoolean();
        ReadOnlyServer(ServerSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            int count = 0;
            int all = 0;
            try {
                System.out.println("Server starting");
                while (!closing.get()) {
                    all += count;
                    count = 0;
                    try (Socket client = socket.accept()) {
                        client.setSoTimeout(1000);
                        client.setOption(StandardSocketOptions.SO_LINGER, 0);
                        InputStream is = client.getInputStream();
                        OutputStream os = client.getOutputStream();
                        boolean drain = true;
                        int timeouts = 0;
                        // now read some byte from the ClientHello
                        // and abruptly close the socket.
                        while (drain) {
                            try {
                                is.read();
                                count++;
                                if (count >= 50) {
                                    drain = false;
                                }
                            } catch (SocketTimeoutException so) {
                                // make sure we read something
                                if (count > 0) timeouts++;
                                if (timeouts == 5) {
                                    // presumably the client is
                                    // waiting for us to answer...
                                    // but we should not reach here.
                                    drain = false;
                                }
                            }
                        }
                        System.out.println("Got " + count + " bytes");
                    }
                }
            } catch (Throwable t) {
                if (!closing.get()) {
                    errorRef.set(t);
                    t.printStackTrace();
                }
            } finally {
                System.out.println("Server existing after reading " + (all + count) + " bytes");
                close();
            }

        }

        @Override
        public void close() {
            if (closing.getAndSet(true))
                return; // already closed
            try {
                socket.close();
            } catch (IOException x) {
                System.out.println("Exception while closing: " + x);
            }
        }
    }
}
