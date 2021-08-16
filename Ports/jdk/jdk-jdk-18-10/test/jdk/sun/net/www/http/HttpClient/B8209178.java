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
 * @bug 8209178
 * @modules java.base/sun.net.www java.base/sun.security.x509 java.base/sun.security.tools.keytool
 * @library /test/lib
 * @run main/othervm -Dsun.net.http.retryPost=true B8209178
 * @run main/othervm -Dsun.net.http.retryPost=false B8209178
 * @summary Proxied HttpsURLConnection doesn't send BODY when retrying POST request
 */

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import javax.net.ssl.*;

import com.sun.net.httpserver.*;
import jdk.test.lib.net.URIBuilder;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

public class B8209178 {
    static {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            SSLContext.setDefault(new TestSSLContext().get());
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    static final String RESPONSE = "<html><body><p>Hello World!</body></html>";
    static final String PATH = "/foo/";
    static final String RETRYPOST = System.getProperty("sun.net.http.retryPost");

    static HttpServer createHttpsServer() throws IOException, NoSuchAlgorithmException {
        HttpsServer server = HttpsServer.create();
        HttpContext context = server.createContext(PATH);
        context.setHandler(new HttpHandler() {

            boolean simulateError = true;

            @Override
            public void handle(HttpExchange he) throws IOException {

                System.out.printf("%s - received request on : %s%n",
                        Thread.currentThread().getName(),
                        he.getRequestURI());
                System.out.printf("%s - received request headers : %s%n",
                        Thread.currentThread().getName(),
                        new HashMap(he.getRequestHeaders()));

                InputStream requestBody = he.getRequestBody();
                String body = B8209178.toString(requestBody);

                System.out.printf("%s - received request body : %s%n",
                        Thread.currentThread().getName(), body);

                if (simulateError) {
                    simulateError = false;

                    System.out.printf("%s - closing connection unexpectedly ... %n",
                            Thread.currentThread().getName(), he.getRequestHeaders());

                    he.close(); // try not to respond anything the first time ...
                    return;
                }

                he.getResponseHeaders().add("encoding", "UTF-8");
                he.sendResponseHeaders(200, RESPONSE.length());
                he.getResponseBody().write(RESPONSE.getBytes(StandardCharsets.UTF_8));
                he.close();
            }
        });

        server.setHttpsConfigurator(new Configurator(SSLContext.getDefault()));
        server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        return server;
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        HttpServer server = createHttpsServer();
        server.start();
        try {
            new B8209178().test(server);

        } finally {
            server.stop(0);
            System.out.println("Server stopped");
        }
    }

    public void test(HttpServer server /*, HttpClient.Version version*/) throws IOException {
        System.out.println("System property retryPost: " + RETRYPOST);
        System.out.println("Server is: " + server.getAddress());
        System.out.println("Verifying communication with server");
        URI uri = URIBuilder.newBuilder()
                .scheme("https")
                .host(server.getAddress().getAddress())
                .port(server.getAddress().getPort())
                .path(PATH + "x")
                .buildUnchecked();

        TunnelingProxy proxy = new TunnelingProxy(server);
        proxy.start();

        try {
            System.out.println("Proxy started");
            Proxy p = new Proxy(Proxy.Type.HTTP,
                    InetSocketAddress.createUnresolved("localhost", proxy.getAddress().getPort()));
            System.out.println("Verifying communication with proxy");

            callHttpsServerThroughProxy(uri, p);

        } finally {
            System.out.println("Stopping proxy");
            proxy.stop();
            System.out.println("Proxy stopped");
        }
    }

    private void callHttpsServerThroughProxy(URI uri, Proxy p) throws IOException {
        HttpsURLConnection urlConnection = (HttpsURLConnection) uri.toURL().openConnection(p);

        urlConnection.setConnectTimeout(1000);
        urlConnection.setReadTimeout(3000);
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("POST");
        urlConnection.setUseCaches(false);

        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        urlConnection.setRequestProperty("charset", "utf-8");
        urlConnection.setRequestProperty("Connection", "keep-alive");

        String urlParameters = "param1=a&param2=b&param3=c";
        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

        OutputStream outputStream = urlConnection.getOutputStream();
        outputStream.write(postData);
        outputStream.close();

        int responseCode;

        try {
            responseCode = urlConnection.getResponseCode();
            System.out.printf(" ResponseCode : %s%n", responseCode);
            String output;
            InputStream inputStream = (responseCode < 400) ? urlConnection.getInputStream() : urlConnection.getErrorStream();
            output = toString(inputStream);
            inputStream.close();
            System.out.printf(" Output from server : %s%n", output);

            if (responseCode == 200) {    // OK !
            } else {
                throw new RuntimeException("Bad response Code : " + responseCode);
            }
        } catch (SocketException se) {
            if (RETRYPOST.equals("true")) {    // Should not get here with the fix
                throw new RuntimeException("Unexpected Socket Exception: " + se);
            } else {
                System.out.println("Socket Exception received as expected: " + se);
            }
        }
    }

    static class TunnelingProxy {
        final Thread accept;
        final ServerSocket ss;
        final boolean DEBUG = false;
        final HttpServer serverImpl;

        TunnelingProxy(HttpServer serverImpl) throws IOException {
            this.serverImpl = serverImpl;
            ss = new ServerSocket();
            accept = new Thread(this::accept);
        }

        void start() throws IOException {
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            accept.start();
        }

        // Pipe the input stream to the output stream
        private synchronized Thread pipe(InputStream is, OutputStream os, char tag) {
            return new Thread("TunnelPipe(" + tag + ")") {
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
                    }
                }
            };
        }

        public InetSocketAddress getAddress() {
            return new InetSocketAddress(ss.getInetAddress(), ss.getLocalPort());
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
                if (c == '\n') {
                    break;
                }
                b.appendCodePoint(c);
            }
            if (b.codePointAt(b.length() - 1) == '\r') {
                b.delete(b.length() - 1, b.length());
            }
            return b.toString();
        }

        public void accept() {
            Socket clientConnection = null;
            try {
                while (true) {
                    System.out.println("Tunnel: Waiting for client");
                    Socket previous = clientConnection;
                    try {
                        clientConnection = ss.accept();
                    } catch (IOException io) {
                        if (DEBUG) io.printStackTrace(System.out);
                        break;
                    } finally {
                        // we have only 1 client at a time, so it is safe
                        // to close the previous connection here
                        if (previous != null) previous.close();
                    }
                    System.out.println("Tunnel: Client accepted");
                    Socket targetConnection = null;
                    InputStream ccis = clientConnection.getInputStream();
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
                        while (!requestLine.equals("")) {
                            System.out.println("Tunnel: Reading header: "
                                    + (requestLine = readLine(ccis)));
                        }

                        // Open target connection
                        targetConnection = new Socket(
                                serverImpl.getAddress().getAddress(),
                                serverImpl.getAddress().getPort());

                        // Then send the 200 OK response to the client
                        System.out.println("Tunnel: Sending "
                                + "HTTP/1.1 200 OK\r\n\r\n");
                        pw.print("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n");
                        pw.flush();
                    } else {
                        // This should not happen
                        throw new IOException("Tunnel: Unexpected status line: "
                                + requestLine);
                    }

                    // Pipe the input stream of the client connection to the
                    // output stream of the target connection and conversely.
                    // Now the client and target will just talk to each other.
                    System.out.println("Tunnel: Starting tunnel pipes");
                    Thread t1 = pipe(ccis, targetConnection.getOutputStream(), '+');
                    Thread t2 = pipe(targetConnection.getInputStream(), ccos, '-');
                    t1.start();
                    t2.start();

                    // We have only 1 client... wait until it has finished before
                    // accepting a new connection request.
                    System.out.println("Tunnel: Waiting for pipes to close");
                    t1.join();
                    t2.join();
                    System.out.println("Tunnel: Done - waiting for next client");
                }
            } catch (Throwable ex) {
                try {
                    ss.close();
                } catch (IOException ex1) {
                    ex.addSuppressed(ex1);
                }
                ex.printStackTrace(System.err);
            }
        }

        void stop() throws IOException {
            ss.close();
        }
    }

    static class Configurator extends HttpsConfigurator {
        public Configurator(SSLContext ctx) {
            super(ctx);
        }

        @Override
        public void configure(HttpsParameters params) {
            params.setSSLParameters(getSSLContext().getSupportedSSLParameters());
        }
    }


    static class TestSSLContext {

        SSLContext ssl;

        public TestSSLContext() throws Exception {
            init();
        }

        private void init() throws Exception {

            CertAndKeyGen keyGen = new CertAndKeyGen("RSA", "SHA1WithRSA", null);
            keyGen.generate(1024);

            //Generate self signed certificate
            X509Certificate[] chain = new X509Certificate[1];
            chain[0] = keyGen.getSelfCertificate(new X500Name("CN=ROOT"), (long) 365 * 24 * 3600);

            char[] passphrase = "passphrase".toCharArray();

            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(null, passphrase); // must be "initialized" ...

            ks.setKeyEntry("server", keyGen.getPrivateKey(), passphrase, chain);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, passphrase);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            ssl = SSLContext.getInstance("TLS");
            ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        }

        public SSLContext get() {
            return ssl;
        }
    }

    // ###############################################################################################

    private static String toString(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        int i = bufferedReader.read();
        while (i != -1) {
            sb.append((char) i);
            i = bufferedReader.read();
        }
        bufferedReader.close();
        return sb.toString();
    }
}
