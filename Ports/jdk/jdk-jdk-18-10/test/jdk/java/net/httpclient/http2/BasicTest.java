/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8087112
 * @library /test/lib server
 * @build jdk.test.lib.net.SimpleSSLContext
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 * @run testng/othervm -Djdk.httpclient.HttpClient.log=ssl,requests,responses,errors BasicTest
 */

import java.io.IOException;
import java.net.*;
import javax.net.ssl.*;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.Test;
import static java.net.http.HttpClient.Version.HTTP_2;

@Test
public class BasicTest {
    static int httpPort, httpsPort;
    static Http2TestServer httpServer, httpsServer;
    static HttpClient client = null;
    static ExecutorService clientExec;
    static ExecutorService serverExec;
    static SSLContext sslContext;

    static String pingURIString, httpURIString, httpsURIString;

    static void initialize() throws Exception {
        try {
            SimpleSSLContext sslct = new SimpleSSLContext();
            sslContext = sslct.get();
            client = getClient();
            httpServer = new Http2TestServer(false, 0, serverExec, sslContext);
            httpServer.addHandler(new Http2EchoHandler(), "/");
            httpServer.addHandler(new EchoWithPingHandler(), "/ping");
            httpPort = httpServer.getAddress().getPort();

            httpsServer = new Http2TestServer(true, 0, serverExec, sslContext);
            httpsServer.addHandler(new Http2EchoHandler(), "/");

            httpsPort = httpsServer.getAddress().getPort();
            httpURIString = "http://localhost:" + httpPort + "/foo/";
            pingURIString = "http://localhost:" + httpPort + "/ping/";
            httpsURIString = "https://localhost:" + httpsPort + "/bar/";

            httpServer.start();
            httpsServer.start();
        } catch (Throwable e) {
            System.err.println("Throwing now");
            e.printStackTrace();
            throw e;
        }
    }

    static List<CompletableFuture<Long>> cfs = Collections
        .synchronizedList( new LinkedList<>());

    static CompletableFuture<Long> currentCF;

    static class EchoWithPingHandler extends Http2EchoHandler {
        private final Object lock = new Object();

        @Override
        public void handle(Http2TestExchange exchange) throws IOException {
            // for now only one ping active at a time. don't want to saturate
            synchronized(lock) {
                CompletableFuture<Long> cf = currentCF;
                if (cf == null || cf.isDone()) {
                    cf = exchange.sendPing();
                    assert cf != null;
                    cfs.add(cf);
                    currentCF = cf;
                }
            }
            super.handle(exchange);
        }
    }

    @Test
    public static void test() throws Exception {
        try {
            initialize();
            warmup(false);
            warmup(true);
            simpleTest(false, false);
            simpleTest(false, true);
            simpleTest(true, false);
            streamTest(false);
            streamTest(true);
            paramsTest();
            CompletableFuture.allOf(cfs.toArray(new CompletableFuture[0])).join();
            synchronized (cfs) {
                for (CompletableFuture<Long> cf : cfs) {
                    System.out.printf("Ping ack received in %d millisec\n", cf.get());
                }
            }
        } catch (Throwable tt) {
            System.err.println("tt caught");
            tt.printStackTrace();
            throw tt;
        } finally {
            httpServer.stop();
            httpsServer.stop();
            //clientExec.shutdown();
        }
    }

    static HttpClient getClient() {
        if (client == null) {
            serverExec = Executors.newCachedThreadPool();
            clientExec = Executors.newCachedThreadPool();
            client = HttpClient.newBuilder()
                               .executor(clientExec)
                               .sslContext(sslContext)
                               .version(HTTP_2)
                               .build();
        }
        return client;
    }

    static URI getURI(boolean secure) {
        return getURI(secure, false);
    }

    static URI getURI(boolean secure, boolean ping) {
        if (secure)
            return URI.create(httpsURIString);
        else
            return URI.create(ping ? pingURIString: httpURIString);
    }

    static void checkStatus(int expected, int found) throws Exception {
        if (expected != found) {
            System.err.printf ("Test failed: wrong status code %d/%d\n",
                expected, found);
            throw new RuntimeException("Test failed");
        }
    }

    static void checkStrings(String expected, String found) throws Exception {
        if (!expected.equals(found)) {
            System.err.printf ("Test failed: wrong string %s/%s\n",
                expected, found);
            throw new RuntimeException("Test failed");
        }
    }

    static Void compareFiles(Path path1, Path path2) {
        return TestUtil.compareFiles(path1, path2);
    }

    static Path tempFile() {
        return TestUtil.tempFile();
    }

    static final String SIMPLE_STRING = "Hello world Goodbye world";

    static final int LOOPS = 13;
    static final int FILESIZE = 64 * 1024 + 200;

    static void streamTest(boolean secure) throws Exception {
        URI uri = getURI(secure);
        System.err.printf("streamTest %b to %s\n" , secure, uri);

        HttpClient client = getClient();
        Path src = TestUtil.getAFile(FILESIZE * 4);
        HttpRequest req = HttpRequest.newBuilder(uri)
                                     .POST(BodyPublishers.ofFile(src))
                                     .build();

        Path dest = Paths.get("streamtest.txt");
        dest.toFile().delete();
        CompletableFuture<Path> response = client.sendAsync(req, BodyHandlers.ofFile(dest))
                .thenApply(resp -> {
                    if (resp.statusCode() != 200)
                        throw new RuntimeException();
                    return resp.body();
                });
        response.join();
        compareFiles(src, dest);
        System.err.println("streamTest: DONE");
    }

    static void paramsTest() throws Exception {
        httpsServer.addHandler((t -> {
            SSLSession s = t.getSSLSession();
            String prot = s.getProtocol();
            if (prot.equals("TLSv1.2") || prot.equals("TLSv1.3")) {
                t.sendResponseHeaders(200, -1);
            } else {
                System.err.printf("Protocols =%s\n", prot);
                t.sendResponseHeaders(500, -1);
            }
        }), "/");
        URI u = new URI("https://localhost:"+httpsPort+"/foo");
        HttpClient client = getClient();
        HttpRequest req = HttpRequest.newBuilder(u).build();
        HttpResponse<String> resp = client.send(req, BodyHandlers.ofString());
        int stat = resp.statusCode();
        if (stat != 200) {
            throw new RuntimeException("paramsTest failed "
                + Integer.toString(stat));
        }
        System.err.println("paramsTest: DONE");
    }

    static void warmup(boolean secure) throws Exception {
        URI uri = getURI(secure);
        System.err.println("Request to " + uri);

        // Do a simple warmup request

        HttpClient client = getClient();
        HttpRequest req = HttpRequest.newBuilder(uri)
                                     .POST(BodyPublishers.ofString(SIMPLE_STRING))
                                     .build();
        HttpResponse<String> response = client.send(req, BodyHandlers.ofString());
        checkStatus(200, response.statusCode());
        String responseBody = response.body();
        HttpHeaders h = response.headers();
        checkStrings(SIMPLE_STRING, responseBody);
        checkStrings(h.firstValue("x-hello").get(), "world");
        checkStrings(h.firstValue("x-bye").get(), "universe");
    }

    static void simpleTest(boolean secure, boolean ping) throws Exception {
        URI uri = getURI(secure, ping);
        System.err.println("Request to " + uri);

        // Do loops asynchronously

        CompletableFuture[] responses = new CompletableFuture[LOOPS];
        final Path source = TestUtil.getAFile(FILESIZE);
        HttpRequest request = HttpRequest.newBuilder(uri)
                                         .POST(BodyPublishers.ofFile(source))
                                         .build();
        for (int i = 0; i < LOOPS; i++) {
            responses[i] = client.sendAsync(request, BodyHandlers.ofFile(tempFile()))
                //.thenApply(resp -> compareFiles(resp.body(), source));
                .thenApply(resp -> {
                    System.out.printf("Resp status %d body size %d\n",
                                      resp.statusCode(), resp.body().toFile().length());
                    return compareFiles(resp.body(), source);
                });
            Thread.sleep(100);
        }
        CompletableFuture.allOf(responses).join();
        System.err.println("simpleTest: DONE");
    }
}
