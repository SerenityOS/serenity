/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.SocketTimeoutException;
import java.time.Duration;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jdk.test.lib.net.SimpleSSLContext;

/**
 * @test
 * @bug 8207966
 * @library /test/lib
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run main/othervm -Djdk.httpclient.enableAllMethodRetry
 *                   -Djdk.tls.acknowledgeCloseNotify=true UnknownBodyLengthTest plain false
 * @run main/othervm -Djdk.httpclient.enableAllMethodRetry
 *                   -Djdk.tls.acknowledgeCloseNotify=true UnknownBodyLengthTest SSL false
 * @run main/othervm -Djdk.httpclient.enableAllMethodRetry
 *                   -Djdk.tls.acknowledgeCloseNotify=true UnknownBodyLengthTest plain true
 * @run main/othervm -Djdk.httpclient.enableAllMethodRetry
 *                   -Djdk.tls.acknowledgeCloseNotify=true UnknownBodyLengthTest SSL true
 */

public class UnknownBodyLengthTest {
    static final byte[] BUF = new byte[32 * 10234 + 2];

    volatile SSLContext ctx;
    volatile ServerSocketFactory factory;
    volatile String clientURL;
    volatile int port;
    final ServerSocket ss;
    final List<Socket> acceptedList = new CopyOnWriteArrayList<>();

    UnknownBodyLengthTest(boolean useSSL) throws Exception {
        ctx = new SimpleSSLContext().get();
        SSLContext.setDefault(ctx);
        factory = useSSL ? SSLServerSocketFactory.getDefault()
                         : ServerSocketFactory.getDefault();
        ss = factory.createServerSocket();
        ss.setReuseAddress(true);
        ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        System.out.println("ServerSocket = " + ss.getClass() + " " + ss);
        port = ss.getLocalPort();
        clientURL = (useSSL ? "https" : "http") + "://localhost:"
            + Integer.toString(port) + "/test";
    }

    static void fillBuf(byte[] buf) {
        for (int i=0; i<buf.length; i++)
            buf[i] = (byte)i;
    }

    static void checkBuf(byte[] buf) {
        if (buf.length != BUF.length)
            throw new RuntimeException("buffer lengths not the same");
        for (int i=0; i<buf.length; i++)
            if (buf[i] != BUF[i])
                throw new RuntimeException("error at position " + i);
    }

    volatile boolean stopped;

    void server(final boolean withContentLength) {
        fillBuf(BUF);
        try {
            while (!stopped) {
                try {
                    Socket s = ss.accept();
                    acceptedList.add(s);
                    s.setTcpNoDelay(true);
                    // if we use linger=1 we still see some
                    // intermittent failures caused by IOException
                    // "Connection reset by peer".
                    // The client side is expecting EOF, but gets reset instead.
                    // 30 is a 'magic' value that may need to be adjusted again.
                    s.setSoLinger(true, 30);
                    System.out.println("Accepted: " + s.getRemoteSocketAddress());
                    System.out.println("Accepted: " + s);
                    OutputStream os = s.getOutputStream();
                    InputStream is = s.getInputStream();
                    boolean done = false;
                    byte[] buf = new byte[1024];
                    String rsp = "";
                    while (!done) {
                        int c = is.read(buf);
                        if (c < 0) break;
                        String s1 = new String(buf, 0, c, "ISO-8859-1");
                        rsp += s1;
                        done = rsp.endsWith("!#!#");
                    }
                    String r = "HTTP/1.0 200 OK\r\nConnection: close\r\nContent-Type:" +
                            " text/xml; charset=UTF-8\r\n";
                    os.write(r.getBytes());
                    String chdr = "Content-Length: " + Integer.toString(BUF.length) +
                            "\r\n";
                    System.out.println(chdr);
                    if (withContentLength)
                        os.write(chdr.getBytes());
                    os.write("\r\n".getBytes());
                    os.write(BUF);
                    if (is.available() > 0) {
                        System.out.println("Draining input: " + s);
                        is.read(buf);
                    }
                    os.flush();
                    s.shutdownOutput();
                    System.out.println("Closed output: " + s);
                } catch(Exception e) {
                    if (!stopped) {
                        System.out.println("Unexpected server exception: " + e);
                        e.printStackTrace();
                    }
                }
            }
        } catch(final Throwable t) {
            if (!stopped) t.printStackTrace();
        } finally {
            stop();
        }
    }

    void client(boolean useSSL) throws Exception {
        SSLContext ctx = SSLContext.getDefault();
        HttpClient.Builder clientB = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2);
        if (useSSL) {
            clientB = clientB.sslContext(ctx)
                .sslParameters(ctx.getSupportedSSLParameters());
        }
        final HttpClient client = clientB.build();

        System.out.println("URL: " + clientURL);
        final HttpResponse<byte[]> response = client
            .send(HttpRequest
                .newBuilder(new URI(clientURL))
                .timeout(Duration.ofMillis(120_000))
                .POST(HttpRequest.BodyPublishers.ofString("body!#!#"))
                .build(), HttpResponse.BodyHandlers.ofByteArray());

        System.out.println("Received reply: " + response.statusCode());
        byte[] bb = response.body();
        checkBuf(bb);
    }

    public static void main(final String[] args) throws Exception {
        boolean ssl = args[0].equals("SSL");
        boolean fixedlen = args[1].equals("true");
        UnknownBodyLengthTest test = new UnknownBodyLengthTest(ssl);
        try {
            test.run(ssl, fixedlen);
        } finally {
            test.stop();
        }
    }

    public void run(boolean ssl, boolean fixedlen) throws Exception {
        new Thread(()->server(fixedlen)).start();
        client(ssl);
    }

    public void stop() {
        stopped = true;
        try { ss.close(); } catch (Throwable t) { }
        for (Socket s : acceptedList) {
            try { s.close(); } catch (Throwable t) { }
        }
    }
}
