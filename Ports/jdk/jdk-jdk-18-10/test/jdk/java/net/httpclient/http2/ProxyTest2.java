/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import jdk.test.lib.net.SimpleSSLContext;
import java.util.concurrent.*;

/**
 * @test
 * @bug 8181422
 * @summary  Verifies that you can access an HTTP/2 server over HTTPS by
 *           tunnelling through an HTTP/1.1 proxy.
 * @modules java.net.http
 * @library /test/lib server
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 * @build jdk.test.lib.net.SimpleSSLContext ProxyTest2
 * @run main/othervm ProxyTest2
 * @author danielfuchs
 */
public class ProxyTest2 {

    static {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
            SSLContext.setDefault(new SimpleSSLContext().get());
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    static final String RESPONSE = "<html><body><p>Hello World!</body></html>";
    static final String PATH = "/foo/";

    static Http2TestServer createHttpsServer(ExecutorService exec) throws Exception {
        Http2TestServer server = new Http2TestServer(true, 0, exec, SSLContext.getDefault());
        server.addHandler(new Http2Handler() {
            @Override
            public void handle(Http2TestExchange he) throws IOException {
                he.getResponseHeaders().addHeader("encoding", "UTF-8");
                he.sendResponseHeaders(200, RESPONSE.length());
                he.getResponseBody().write(RESPONSE.getBytes(StandardCharsets.UTF_8));
                he.close();
            }
        }, PATH);

        return server;
    }

    public static void main(String[] args)
            throws Exception
    {
        ExecutorService exec = Executors.newCachedThreadPool();
        Http2TestServer server = createHttpsServer(exec);
        server.start();
        try {
            // Http2TestServer over HTTPS does not support HTTP/1.1
            // => only test with a HTTP/2 client
            test(server, HttpClient.Version.HTTP_2);
        } finally {
            server.stop();
            exec.shutdown();
            System.out.println("Server stopped");
        }
    }

    public static void test(Http2TestServer server, HttpClient.Version version)
            throws Exception
    {
        System.out.println("Server is: " + server.getAddress().toString());
        URI uri = new URI("https://localhost:" + server.getAddress().getPort() + PATH + "x");
        TunnelingProxy proxy = new TunnelingProxy(server);
        proxy.start();
        try {
            System.out.println("Proxy started");
            Proxy p = new Proxy(Proxy.Type.HTTP,
                    InetSocketAddress.createUnresolved("localhost", proxy.getAddress().getPort()));
            System.out.println("Setting up request with HttpClient for version: "
                    + version.name() + "URI=" + uri);
            ProxySelector ps = ProxySelector.of(
                    InetSocketAddress.createUnresolved("localhost", proxy.getAddress().getPort()));
            HttpClient client = HttpClient.newBuilder()
                .version(version)
                .proxy(ps)
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

            System.out.println("Sending request with HttpClient");
            HttpResponse<String> response
                = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Got response");
            String resp = response.body();
            System.out.println("Received: " + resp);
            if (!RESPONSE.equals(resp)) {
                throw new AssertionError("Unexpected response");
            }
        } finally {
            System.out.println("Stopping proxy");
            proxy.stop();
            System.out.println("Proxy stopped");
        }
    }

    static class TunnelingProxy {
        final Thread accept;
        final ServerSocket ss;
        final boolean DEBUG = false;
        final Http2TestServer serverImpl;
        final CopyOnWriteArrayList<CompletableFuture<Void>> connectionCFs
                = new CopyOnWriteArrayList<>();
        private volatile boolean stopped;
        TunnelingProxy(Http2TestServer serverImpl) throws IOException {
            this.serverImpl = serverImpl;
            ss = new ServerSocket();
            accept = new Thread(this::accept);
            accept.setDaemon(true);
        }

        void start() throws IOException {
            ss.setReuseAddress(false);
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            accept.start();
        }

        // Pipe the input stream to the output stream.
        private synchronized Thread pipe(InputStream is, OutputStream os,
                                         char tag, CompletableFuture<Void> end) {
            return new Thread("TunnelPipe("+tag+")") {
                @Override
                public void run() {
                    try {
                        try {
                            int c;
                            while ((c = is.read()) != -1) {
                                os.write(c);
                                os.flush();
                                // if DEBUG prints a + or a - for each transferred
                                // character.
                                if (DEBUG) System.out.print(tag);
                            }
                            is.close();
                        } finally {
                            os.close();
                        }
                    } catch (IOException ex) {
                        if (DEBUG) ex.printStackTrace(System.out);
                    } finally {
                        end.complete(null);
                    }
                }
            };
        }

        public InetSocketAddress getAddress() {
            return new InetSocketAddress( InetAddress.getLoopbackAddress(), ss.getLocalPort());
        }

        // This is a bit shaky. It doesn't handle continuation
        // lines, but our client shouldn't send any.
        // Read a line from the input stream, swallowing the final
        // \r\n sequence. Stops at the first \n, doesn't complain
        // if it wasn't preceded by '\r'.
        //
        String readLine(InputStream r) throws IOException {
            StringBuilder b = new StringBuilder();
            int c;
            while ((c = r.read()) != -1) {
                if (c == '\n') break;
                b.appendCodePoint(c);
            }
            if (b.codePointAt(b.length() -1) == '\r') {
                b.delete(b.length() -1, b.length());
            }
            return b.toString();
        }

        public void accept() {
            Socket clientConnection = null;
            try {
                while (!stopped) {
                    System.out.println("Tunnel: Waiting for client");
                    Socket toClose;
                    try {
                        toClose = clientConnection = ss.accept();
                    } catch (IOException io) {
                        if (DEBUG) io.printStackTrace(System.out);
                        break;
                    }
                    System.out.println("Tunnel: Client accepted");
                    Socket targetConnection = null;
                    InputStream  ccis = clientConnection.getInputStream();
                    OutputStream ccos = clientConnection.getOutputStream();
                    Writer w = new OutputStreamWriter(ccos, "UTF-8");
                    PrintWriter pw = new PrintWriter(w);
                    System.out.println("Tunnel: Reading request line");
                    String requestLine = readLine(ccis);
                    System.out.println("Tunnel: Request status line: " + requestLine);
                    if (requestLine.startsWith("CONNECT ")) {
                        // We should probably check that the next word following
                        // CONNECT is the host:port of our HTTPS serverImpl.
                        // Some improvement for a followup!

                        // Read all headers until we find the empty line that
                        // signals the end of all headers.
                        while(!requestLine.equals("")) {
                            System.out.println("Tunnel: Reading header: "
                                    + (requestLine = readLine(ccis)));
                        }

                        // Open target connection
                        targetConnection = new Socket(
                                InetAddress.getLoopbackAddress(),
                                serverImpl.getAddress().getPort());

                        // Then send the 200 OK response to the client
                        System.out.println("Tunnel: Sending "
                                + "HTTP/1.1 200 OK\r\n\r\n");
                        pw.print("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n");
                        pw.flush();
                    } else {
                        // This should not happen. If it does then just print an
                        // error - both on out and err, and close the accepted
                        // socket
                        System.out.println("WARNING: Tunnel: Unexpected status line: "
                                + requestLine + " received by "
                                + ss.getLocalSocketAddress()
                                + " from "
                                + toClose.getRemoteSocketAddress()
                                + " - closing accepted socket");
                        // Print on err
                        System.err.println("WARNING: Tunnel: Unexpected status line: "
                                + requestLine + " received by "
                                + ss.getLocalSocketAddress()
                                + " from "
                                + toClose.getRemoteSocketAddress());
                        // close accepted socket.
                        toClose.close();
                        System.err.println("Tunnel: accepted socket closed.");
                        continue;
                    }

                    // Pipe the input stream of the client connection to the
                    // output stream of the target connection and conversely.
                    // Now the client and target will just talk to each other.
                    System.out.println("Tunnel: Starting tunnel pipes");
                    CompletableFuture<Void> end, end1, end2;
                    Thread t1 = pipe(ccis, targetConnection.getOutputStream(), '+',
                            end1 = new CompletableFuture<>());
                    Thread t2 = pipe(targetConnection.getInputStream(), ccos, '-',
                            end2 = new CompletableFuture<>());
                    end = CompletableFuture.allOf(end1, end2);
                    end.whenComplete(
                            (r,t) -> {
                                try { toClose.close(); } catch (IOException x) { }
                                finally {connectionCFs.remove(end);}
                            });
                    connectionCFs.add(end);
                    t1.start();
                    t2.start();
                }
            } catch (Throwable ex) {
                try {
                    ss.close();
                } catch (IOException ex1) {
                    ex.addSuppressed(ex1);
                }
                ex.printStackTrace(System.err);
            } finally {
                System.out.println("Tunnel: exiting (stopped=" + stopped + ")");
                connectionCFs.forEach(cf -> cf.complete(null));
            }
        }

        public void stop() throws IOException {
            stopped = true;
            ss.close();
        }

    }

    static class Configurator extends HttpsConfigurator {
        public Configurator(SSLContext ctx) {
            super(ctx);
        }

        @Override
        public void configure (HttpsParameters params) {
            params.setSSLParameters (getSSLContext().getSupportedSSLParameters());
        }
    }

}
