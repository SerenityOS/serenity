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

/**
 * @test
 * @bug 8218554
 * @summary  test that the handler can request a connection close.
 * @library /test/lib
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run main/othervm HandlerConnectionClose
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import jdk.test.lib.net.SimpleSSLContext;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

public class HandlerConnectionClose
{
    static final int ONEK = 1024;
    static final long POST_SIZE = ONEK * 1L;
    SSLContext sslContext;
    Logger logger;

    void test(String[] args) throws Exception {

        HttpServer httpServer = startHttpServer("http");
        try {
            testHttpURLConnection(httpServer, "http","/close/legacy/http/chunked");
            testHttpURLConnection(httpServer, "http","/close/legacy/http/fixed");
            testPlainSocket(httpServer, "http","/close/plain/http/chunked");
            testPlainSocket(httpServer, "http","/close/plain/http/fixed");
        } finally {
            httpServer.stop(0);
        }
        sslContext = new SimpleSSLContext().get();
        HttpServer httpsServer = startHttpServer("https");
        try {
            testHttpURLConnection(httpsServer, "https","/close/legacy/https/chunked");
            testHttpURLConnection(httpsServer, "https","/close/legacy/https/fixed");
            testPlainSocket(httpsServer, "https","/close/plain/https/chunked");
            testPlainSocket(httpsServer, "https","/close/plain/https/fixed");
        } finally{
            httpsServer.stop(0);
        }
    }

    void testHttpURLConnection(HttpServer httpServer, String protocol, String path) throws Exception {
        int port = httpServer.getAddress().getPort();
        String host = httpServer.getAddress().getHostString();
        URL url = new URI(protocol, null, host, port, path, null, null).toURL();
        HttpURLConnection uc = (HttpURLConnection) url.openConnection();
        if ("https".equalsIgnoreCase(protocol)) {
            ((HttpsURLConnection)uc).setSSLSocketFactory(sslContext.getSocketFactory());
            ((HttpsURLConnection)uc).setHostnameVerifier((String hostname, SSLSession session) -> true);
        }
        uc.setDoOutput(true);
        uc.setRequestMethod("POST");
        uc.setFixedLengthStreamingMode(POST_SIZE);
        OutputStream os = uc.getOutputStream();

        /* create a 1K byte array with data to POST */
        byte[] ba = new byte[ONEK];
        for (int i = 0; i < ONEK; i++)
            ba[i] = (byte) i;

        System.out.println("\n" + uc.getClass().getSimpleName() +": POST " + url + " HTTP/1.1");
        long times = POST_SIZE / ONEK;
        for (int i = 0; i < times; i++) {
            os.write(ba);
        }

        os.close();
        InputStream is = uc.getInputStream();
        int read;
        long count = 0;
        while ((read = is.read(ba)) != -1) {
            for (int i = 0; i < read; i++) {
                byte expected = (byte) count++;
                if (ba[i] != expected) {
                    throw new IOException("byte mismatch at "
                            + (count - 1) + ": expected " + expected + " got " + ba[i]);
                }
            }
        }
        if (count != POST_SIZE) {
            throw new IOException("Unexpected length: " + count + " expected " + POST_SIZE);
        }
        is.close();

        pass();
    }

    void testPlainSocket(HttpServer httpServer, String protocol, String path) throws Exception {
        int port = httpServer.getAddress().getPort();
        String host = httpServer.getAddress().getHostString();
        URL url = new URI(protocol, null, host, port, path, null, null).toURL();
        Socket socket;
        if ("https".equalsIgnoreCase(protocol)) {
            socket = sslContext.getSocketFactory().createSocket(host, port);
        } else {
            socket = new Socket(host, port);
        }
        try (Socket sock = socket) {
            OutputStream os = socket.getOutputStream();

            // send request headers
            String request = new StringBuilder()
                    .append("POST ").append(path).append(" HTTP/1.1").append("\r\n")
                    .append("host: ").append(host).append(':').append(port).append("\r\n")
                    .append("Content-Length: ").append(POST_SIZE).append("\r\n")
                    .append("\r\n")
                    .toString();
            os.write(request.getBytes(StandardCharsets.US_ASCII));

            /* create a 1K byte array with data to POST */
            byte[] ba = new byte[ONEK];
            for (int i = 0; i < ONEK; i++)
                ba[i] = (byte) i;

            // send request data
            long times = POST_SIZE / ONEK;
            for (int i = 0; i < times; i++) {
                os.write(ba);
            }
            os.flush();

            InputStream is = socket.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            // read all response headers
            int c;
            int crlf = 0;
            while ((c = is.read()) != -1) {
                if (c == '\r') continue;
                if (c == '\n') crlf++;
                else crlf = 0;
                bos.write(c);
                if (crlf == 2) break;
            }
            String responseHeadersStr = bos.toString(StandardCharsets.US_ASCII);
            List<String> responseHeaders = List.of(responseHeadersStr.split("\n"));
            System.out.println("\nPOST " + url + " HTTP/1.1");
            responseHeaders.stream().forEach(s -> System.out.println("[reply]\t" + s));
            String statusLine = responseHeaders.get(0);
            if (!statusLine.startsWith("HTTP/1.1 200 "))
                throw new IOException("Unexpected status: " + statusLine);
            String cl = responseHeaders.stream()
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .filter(s -> s.startsWith("content-length: "))
                    .findFirst()
                    .orElse(null);
            String te = responseHeaders.stream()
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .filter(s -> s.startsWith("transfer-encoding: "))
                    .findFirst()
                    .orElse(null);

            // check content-length and transfer-encoding are as expected
            int read = 0;
            long count = 0;
            if (path.endsWith("/fixed")) {
                if (!("content-length: " + POST_SIZE).equalsIgnoreCase(cl)) {
                    throw new IOException("Unexpected Content-Length: [" + cl + "]");
                }
                if (te != null) {
                    throw new IOException("Unexpected Transfer-Encoding: [" + te + "]");
                }
                // Got expected Content-Length: 1024 - read response data
                while ((read = is.read()) != -1) {
                    int expected = (int) (count & 0xFF);
                    if ((read & 0xFF) != expected) {
                        throw new IOException("byte mismatch at "
                                + (count - 1) + ": expected " + expected + " got " + read);
                    }
                    if (++count == POST_SIZE) break;
                }
            } else if (cl != null) {
                throw new IOException("Unexpected Content-Length: [" + cl + "]");
            } else {
                if (!("transfer-encoding: chunked").equalsIgnoreCase(te)) {
                    throw new IOException("Unexpected Transfer-Encoding: [" + te + "]");
                }
                // This is a quick & dirty implementation of
                // chunk decoding - no trailers - no extensions
                StringBuilder chunks = new StringBuilder();
                int cs = -1;
                while (cs != 0) {
                    cs = 0;
                    chunks.setLength(0);

                    // read next chunk length
                    while ((read = is.read()) != -1) {
                        if (read == '\r') continue;
                        if (read == '\n') break;
                        chunks.append((char) read);
                    }
                    cs = Integer.parseInt(chunks.toString().trim(), 16);
                    System.out.println("Got chunk length: " + cs);

                    // If chunk size is 0, then we have read the last chunk.
                    if (cs == 0) break;

                    // Read the chunk data
                    while (--cs >= 0) {
                        read = is.read();
                        if (read == -1) break; // EOF
                        int expected = (int) (count & 0xFF);
                        if ((read & 0xFF) != expected) {
                            throw new IOException("byte mismatch at "
                                    + (count - 1) + ": expected " + expected + " got " + read);
                        }
                        // This is cheating: we know the size :-)
                        if (++count == POST_SIZE) break;
                    }

                    if (read == -1) {
                        throw new IOException("Unexpected EOF after " + count + " data bytes");
                    }

                    // read CRLF
                    if ((read = is.read()) != '\r') {
                        throw new IOException("Expected CR at " + count + "after chunk data - got " + read);
                    }
                    if ((read = is.read()) != '\n') {
                        throw new IOException("Expected LF at " + count + "after chunk data - got " + read);
                    }

                    if (cs == 0 && count == POST_SIZE) {
                        cs = -1;
                    }

                    if (cs != -1) {
                        // count == POST_SIZE, but some chunk data still to be read?
                        throw new IOException("Unexpected chunk size, "
                                + cs + " bytes still to read after " + count +
                                " data bytes received.");
                    }
                }
                // Last CRLF?
                for (int i = 0; i < 2; i++) {
                    if ((read = is.read()) == -1) break;
                }
            }

            if (count != POST_SIZE) {
                throw new IOException("Unexpected length: " + count + " expected " + POST_SIZE);
            }

            if (!sock.isClosed()) {
                try {
                    // We send an end request to the server to verify that the
                    // connection is closed. If the server has not closed the
                    // connection, it will reply. If we receive a response,
                    // we should fail...
                    String endrequest = new StringBuilder()
                            .append("GET ").append("/close/end").append(" HTTP/1.1").append("\r\n")
                            .append("host: ").append(host).append(':').append(port).append("\r\n")
                            .append("Content-Length: ").append(0).append("\r\n")
                            .append("\r\n")
                            .toString();
                    os.write(endrequest.getBytes(StandardCharsets.US_ASCII));
                    os.flush();
                    StringBuilder resp = new StringBuilder();
                    crlf = 0;

                    // read all headers.
                    // If the server closed the connection as expected
                    // we should immediately read EOF
                    while ((read = is.read()) != -1) {
                        if (read == '\r') continue;
                        if (read == '\n') crlf++;
                        else crlf = 0;
                        if (crlf == 2) break;
                        resp.append((char) read);
                    }

                    List<String> lines = List.of(resp.toString().split("\n"));
                    if (read != -1 || resp.length() != 0) {
                        System.err.println("Connection not closed!");
                        System.err.println("Got: ");
                        lines.stream().forEach(s -> System.err.println("[end]\t" + s));
                        throw new AssertionError("EOF not received after " + count + " data bytes");
                    }
                    if (read != -1) {
                        throw new AssertionError("EOF was expected after " + count + " bytes, but got: " + read);
                    } else {
                        System.out.println("Got expected EOF (" + read + ")");
                    }
                } catch (IOException x) {
                    // expected! all is well
                    System.out.println("Socket closed as expected, got exception writing to it.");
                }
            } else {
                System.out.println("Socket closed as expected");
            }
            pass();
        }
    }

    /**
     * Http Server
     */
    HttpServer startHttpServer(String protocol) throws IOException {
        if (debug) {
            logger = Logger.getLogger("com.sun.net.httpserver");
            Handler outHandler = new StreamHandler(System.out,
                                     new SimpleFormatter());
            outHandler.setLevel(Level.FINEST);
            logger.setLevel(Level.FINEST);
            logger.addHandler(outHandler);
        }

        InetSocketAddress serverAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        HttpServer httpServer = null;
        if ("http".equalsIgnoreCase(protocol)) {
            httpServer = HttpServer.create(serverAddress, 0);
        }
        if ("https".equalsIgnoreCase(protocol)) {
            HttpsServer httpsServer = HttpsServer.create(serverAddress, 0);
            httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
            httpServer = httpsServer;
        }
        httpServer.createContext("/close/", new MyHandler(POST_SIZE));
        System.out.println("Server created at: " + httpServer.getAddress());
        httpServer.start();
        return httpServer;
    }

    class MyHandler implements HttpHandler {
        static final int BUFFER_SIZE = 512;
        final long expected;

        MyHandler(long expected){
            this.expected = expected;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            System.out.println("Server: serving " + t.getRequestURI());
            boolean chunked = t.getRequestURI().getPath().endsWith("/chunked");
            boolean fixed = t.getRequestURI().getPath().endsWith("/fixed");
            boolean end = t.getRequestURI().getPath().endsWith("/end");
            long responseLength = fixed ? POST_SIZE : 0;
            responseLength = end ? -1 : responseLength;
            responseLength = chunked ? 0 : responseLength;

            if (!end) t.getResponseHeaders().add("connection", "CLose");
            t.sendResponseHeaders(200, responseLength);

            if (!end) {
                OutputStream os = t.getResponseBody();
                InputStream is = t.getRequestBody();
                byte[] ba = new byte[BUFFER_SIZE];
                int read;
                long count = 0L;
                while ((read = is.read(ba)) != -1) {
                    count += read;
                    os.write(ba, 0, read);
                }
                is.close();

                check(count == expected, "Expected: " + expected + ", received "
                        + count);
                debug("Received " + count + " bytes");
                os.close();
            }

            t.close();
        }
    }

         //--------------------- Infrastructure ---------------------------
    boolean debug = true;
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void check(boolean cond, String failMessage) {if (cond) pass(); else fail(failMessage);}
    void debug(String message) {if(debug) { System.out.println(message); }  }
    public static void main(String[] args) throws Throwable {
        Class<?> k = new Object(){}.getClass().getEnclosingClass();
        try {k.getMethod("instanceMain",String[].class)
                .invoke( k.newInstance(), (Object) args);}
        catch (Throwable e) {throw e.getCause();}}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}

}
