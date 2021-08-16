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
 * @modules java.net.http
 *          java.logging
 *          jdk.httpserver
 * @library /test/lib
 * @build jdk.test.lib.net.SimpleSSLContext
 * @compile ../../../../com/sun/net/httpserver/LogFilter.java
 * @compile ../../../../com/sun/net/httpserver/FileServerHandler.java
 * @compile ../ProxyServer.java
 *
 * @run main/othervm/secure=java.lang.SecurityManager/policy=0.policy Security 0
 * @run main/othervm/secure=java.lang.SecurityManager/policy=2.policy Security 2
 * @run main/othervm/secure=java.lang.SecurityManager/policy=3.policy Security 3
 * @run main/othervm/secure=java.lang.SecurityManager/policy=4.policy Security 4
 * @run main/othervm/secure=java.lang.SecurityManager/policy=5.policy Security 5
 * @run main/othervm/secure=java.lang.SecurityManager/policy=6.policy Security 6
 * @run main/othervm/secure=java.lang.SecurityManager/policy=7.policy Security 7
 * @run main/othervm/secure=java.lang.SecurityManager/policy=8.policy Security 8
 * @run main/othervm/secure=java.lang.SecurityManager/policy=9.policy Security 9
 * @run main/othervm/secure=java.lang.SecurityManager/policy=0.policy Security 13
 * @run main/othervm/secure=java.lang.SecurityManager/policy=14.policy Security 14
 * @run main/othervm/secure=java.lang.SecurityManager/policy=15.policy -Djava.security.debug=access:domain,failure Security 15
 */

// Tests 1, 10, 11 and 12 executed from Driver

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLClassLoader;
import java.net.URL;
import java.net.http.HttpHeaders;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.InvocationTargetException;
import static java.net.http.HttpResponse.BodyHandlers.ofString;

/**
 * Security checks test
 */
public class Security {

    static HttpServer s1;
    static ExecutorService executor;
    static int port, proxyPort;
    static HttpClient client;
    static String httproot, fileuri, fileroot, redirectroot;
    static List<HttpClient> clients = new LinkedList<>();
    static URI uri;

    interface ThrowingRunnable { void run() throws Throwable; }

    static class TestAndResult {
        private final ThrowingRunnable runnable;
        private final boolean expectSecurityException;

        TestAndResult(boolean expectSecurityException, ThrowingRunnable runnable) {
            this.expectSecurityException = expectSecurityException;
            this.runnable = runnable;
        }

        static TestAndResult of(boolean expectSecurityException,
                                ThrowingRunnable runnable) {
            return new TestAndResult(expectSecurityException, runnable);
        }

        void runWithPolicy(String policy) {
            System.out.println("Using policy file: " + policy);
            try {
                runnable.run();
                if (expectSecurityException) {
                    String msg = "FAILED: expected security exception not thrown";
                    System.out.println(msg);
                    throw new RuntimeException(msg);
                }
                System.out.println (policy + " succeeded as expected");
            } catch (BindException e) {
                System.exit(10);
            } catch (SecurityException e) {
                if (!expectSecurityException) {
                    System.out.println("UNEXPECTED security Exception: " + e);
                    throw new RuntimeException("UNEXPECTED security Exception", e);
                }
                System.out.println(policy + " threw SecurityException as expected: " + e);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    static TestAndResult[] tests = createTests();
    static String testclasses;
    static File subdir;

    /**
     * The ProxyServer class is compiled by jtreg, but we want to
     * move it so it is not on the application claspath. We want to
     * load it through a separate classloader so that it has a separate
     * protection domain and security permissions.
     *
     * Its permissions are in the second grant block in each policy file
     */
    static void setupProxy() throws IOException, ClassNotFoundException, NoSuchMethodException {
        testclasses = System.getProperty("test.classes");
        subdir = new File (testclasses, "proxydir");
        subdir.mkdir();

        movefile("ProxyServer.class");
        movefile("ProxyServer$Connection.class");
        movefile("ProxyServer$1.class");

        URL url = subdir.toURI().toURL();
        System.out.println("URL for class loader = " + url);
        URLClassLoader urlc = new URLClassLoader(new URL[] {url});
        proxyClass = Class.forName("ProxyServer", true, urlc);
        proxyConstructor = proxyClass.getConstructor(Integer.class, Boolean.class);
    }

    static void movefile(String f) throws IOException {
        Path src = Paths.get(testclasses, f);
        Path dest = subdir.toPath().resolve(f);
        if (!dest.toFile().exists()) {
            System.out.printf("moving %s to %s\n", src.toString(), dest.toString());
            Files.move(src, dest,  StandardCopyOption.REPLACE_EXISTING);
        } else if (src.toFile().exists()) {
            System.out.printf("%s exists, deleting %s\n", dest.toString(), src.toString());
            Files.delete(src);
        } else {
            System.out.printf("NOT moving %s to %s\n", src.toString(), dest.toString());
        }
    }

    static Object createProxy(int port, boolean b) throws Throwable {
        try {
            return proxyConstructor.newInstance(port, b);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    static Class<?> proxyClass;
    static Constructor<?> proxyConstructor;

    static TestAndResult[] createTests() {
        return new TestAndResult[] {
            // (0) policy does not have permission for file. Should fail
            TestAndResult.of(true, () -> { // Policy 0
                URI u = URI.create("http://localhost:" + port + "/files/foo.txt");
                HttpRequest request = HttpRequest.newBuilder(u).GET().build();
                HttpResponse<?> response = client.send(request, ofString());
                System.out.println("Received response:" + response);
            }),
            // (1) policy has permission for file URL
            TestAndResult.of(false, () -> { //Policy 1
                URI u = URI.create("http://localhost:" + port + "/files/foo.txt");
                HttpRequest request = HttpRequest.newBuilder(u).GET().build();
                HttpResponse<?> response = client.send(request, ofString());
                System.out.println("Received response:" + response);
            }),
            // (2) policy has permission for all file URLs under /files
            TestAndResult.of(false, () -> { // Policy 2
                URI u = URI.create("http://localhost:" + port + "/files/foo.txt");
                HttpRequest request = HttpRequest.newBuilder(u).GET().build();
                HttpResponse<?> response = client.send(request, ofString());
                System.out.println("Received response:" + response);
            }),
            // (3) policy has permission for first URL but not redirected URL
            TestAndResult.of(true, () -> { // Policy 3
                URI u = URI.create("http://localhost:" + port + "/redirect/foo.txt");
                HttpRequest request = HttpRequest.newBuilder(u).GET().build();
                HttpResponse<?> response = client.send(request, ofString());
                System.out.println("Received response:" + response);
            }),
            // (4) policy has permission for both first URL and redirected URL
            TestAndResult.of(false, () -> { // Policy 4
                URI u = URI.create("http://localhost:" + port + "/redirect/foo.txt");
                HttpRequest request = HttpRequest.newBuilder(u).GET().build();
                HttpResponse<?> response = client.send(request, ofString());
                System.out.println("Received response:" + response);
            }),
            // (5) policy has permission for redirected but not first URL
            TestAndResult.of(true, () -> { // Policy 5
                URI u = URI.create("http://localhost:" + port + "/redirect/foo.txt");
                HttpRequest request = HttpRequest.newBuilder(u).GET().build();
                HttpResponse<?> response = client.send(request, ofString());
                System.out.println("Received response:" + response);
            }),
            // (6) policy has permission for file URL, but not method
            TestAndResult.of(true, () -> { //Policy 6
                URI u = URI.create("http://localhost:" + port + "/files/foo.txt");
                HttpRequest request = HttpRequest.newBuilder(u).GET().build();
                HttpResponse<?> response = client.send(request, ofString());
                System.out.println("Received response:" + response);
            }),
            // (7) policy has permission for file URL, method, but not header
            TestAndResult.of(true, () -> { //Policy 7
                URI u = URI.create("http://localhost:" + port + "/files/foo.txt");
                HttpRequest request = HttpRequest.newBuilder(u)
                                                 .header("X-Foo", "bar")
                                                 .GET()
                                                 .build();
                HttpResponse<?> response = client.send(request, ofString());
                System.out.println("Received response:" + response);
            }),
            // (8) policy has permission for file URL, method and header
            TestAndResult.of(false, () -> { //Policy 8
                URI u = URI.create("http://localhost:" + port + "/files/foo.txt");
                HttpRequest request = HttpRequest.newBuilder(u)
                                                 .header("X-Foo", "bar")
                                                 .GET()
                                                 .build();
                HttpResponse<?> response = client.send(request, ofString());
                System.out.println("Received response:" + response);
            }),
            // (9) policy has permission for file URL, method and header
            TestAndResult.of(false, () -> { //Policy 9
                URI u = URI.create("http://localhost:" + port + "/files/foo.txt");
                HttpRequest request = HttpRequest.newBuilder(u)
                                                 .headers("X-Foo", "bar", "X-Bar", "foo")
                                                 .GET()
                                                 .build();
                HttpResponse<?> response = client.send(request, ofString());
                System.out.println("Received response:" + response);
            }),
            // (10) policy has permission for destination URL but not for proxy
            TestAndResult.of(true, () -> { //Policy 10
                directProxyTest(proxyPort, true);
            }),
            // (11) policy has permission for both destination URL and proxy
            TestAndResult.of(false, () -> { //Policy 11
                directProxyTest(proxyPort, true);
            }),
            // (12) policy has permission for both destination URL and proxy
            TestAndResult.of(true, () -> { //Policy 12 ( 11 & 12 are the same )
                directProxyTest(proxyPort, false);
            }),
            // (13) async version of test 0
            TestAndResult.of(true, () -> { // Policy 0
                URI u = URI.create("http://localhost:" + port + "/files/foo.txt");
                HttpRequest request = HttpRequest.newBuilder(u).GET().build();
                try {
                    HttpResponse<?> response = client.sendAsync(request, ofString()).get();
                    System.out.println("Received response:" + response);
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof SecurityException) {
                        throw (SecurityException)e.getCause();
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            }),
            // (14) async version of test 1
            TestAndResult.of(false, () -> { //Policy 1
                URI u = URI.create("http://localhost:" + port + "/files/foo.txt");
                HttpRequest request = HttpRequest.newBuilder(u).GET().build();
                try {
                    HttpResponse<?> response = client.sendAsync(request, ofString()).get();
                    System.out.println("Received response:" + response);
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof SecurityException) {
                        throw (SecurityException)e.getCause();
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            }),
            // (15) check that user provided unprivileged code running on a worker
            //      thread does not gain ungranted privileges.
            TestAndResult.of(true, () -> { //Policy 12
                URI u = URI.create("http://localhost:" + port + "/files/foo.txt");
                HttpRequest request = HttpRequest.newBuilder(u).GET().build();
                HttpResponse.BodyHandler<String> sth = ofString();

                CompletableFuture<HttpResponse<String>> cf =
                    client.sendAsync(request, new HttpResponse.BodyHandler<String>() {
                        @Override
                        public HttpResponse.BodySubscriber<String> apply(HttpResponse.ResponseInfo rinfo) {
                            final HttpResponse.BodySubscriber<String> stproc = sth.apply(rinfo);
                            return new HttpResponse.BodySubscriber<String>() {
                                @Override
                                public CompletionStage<String> getBody() {
                                    return stproc.getBody();
                                }
                                @Override
                                public void onNext(List<ByteBuffer> item) {
                                    SecurityManager sm = System.getSecurityManager();
                                    // should succeed.
                                    sm.checkPermission(new RuntimePermission("foobar"));
                                    // do some mischief here
                                    System.setSecurityManager(null);
                                    System.setSecurityManager(sm);
                                    // problem if we get this far
                                    stproc.onNext(item);
                                }
                                @Override
                                public void onSubscribe(Flow.Subscription subscription) {
                                    stproc.onSubscribe(subscription);
                                }
                                @Override
                                public void onError(Throwable throwable) {
                                    stproc.onError(throwable);
                                }
                                @Override
                                public void onComplete() {
                                    stproc.onComplete();
                                }
                            };
                        }
                    }
                );
                try {
                    HttpResponse<String> response = cf.join();
                    System.out.println("Received response:" + response);
                } catch (CompletionException e) {
                    Throwable t = e.getCause();
                    if (t instanceof SecurityException)
                        throw (SecurityException)t;
                    else if ((t instanceof IOException)
                              && (t.getCause() instanceof SecurityException))
                        throw ((SecurityException)t.getCause());
                    else
                        throw new RuntimeException(t);
                }
            }),
            // (16) allowed to set Host header but does not have permission
            TestAndResult.of(true, () -> { //Policy 16
                URI u = URI.create("http://localhost:" + port + "/files/foo.txt");
                HttpRequest request = HttpRequest.newBuilder(u)
                        .header("Host", "foohost:123")
                        .GET().build();
                HttpResponse<?> response = client.send(request, ofString());
                System.out.println("Received response:" + response);
            }),
            // (17) allowed to set Host header and does have permission
            TestAndResult.of(false, () -> { //Policy 17
                URI u = URI.create("http://localhost:" + port + "/files/foo.txt");
                HttpRequest request = HttpRequest.newBuilder(u)
                        .header("Host", "foohost:123")
                        .GET().build();
                HttpResponse<?> response = client.send(request, ofString());
                System.out.println("Received response:" + response);
            })
        };
    }

    private static void directProxyTest(int proxyPort, boolean samePort)
        throws IOException, InterruptedException
    {
        System.out.println("proxyPort:" + proxyPort + ", samePort:" + samePort);

        int p = proxyPort;
        if (samePort) {
            Object proxy;
            try {
                proxy = createProxy(p, true);
            } catch (BindException e) {
                System.out.println("Bind failed");
                throw e;
            } catch (Throwable ee) {
                throw new RuntimeException(ee);
            }
        } else {
            while (p == proxyPort || p == port) {
                // avoid ports that may be granted permission
                p++;
                if (p > 65535) {
                    p = 32000; // overflow
                }
            }
        }
        System.out.println("Proxy port, p:" + p);

        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLoopbackAddress(),
                                                       p);
        HttpClient cl = HttpClient.newBuilder()
                                    .proxy(ProxySelector.of(addr))
                                    .build();
        clients.add(cl);

        URI u = URI.create("http://localhost:" + port + "/files/foo.txt");
        HttpRequest request = HttpRequest.newBuilder(u)
                                         .headers("X-Foo", "bar", "X-Bar", "foo")
                                         .build();
        HttpResponse<?> response = cl.send(request, ofString());
        System.out.println("Received response:" + response);
    }

    public static void main(String[] args) throws Exception {
        try {
            initServer();
            setupProxy();
        } catch (BindException e) {
            System.exit(10);
        }
        fileroot = System.getProperty("test.src")+ "/docs";
        int testnum = Integer.parseInt(args[0]);
        String policy = args[0];

        client = HttpClient.newBuilder()
                           .followRedirects(HttpClient.Redirect.ALWAYS)
                           .build();

        clients.add(client);

        try {
            TestAndResult tr = tests[testnum];
            tr.runWithPolicy(policy);
        } finally {
            s1.stop(0);
            executor.shutdownNow();
        }
    }

    public static void initServer() throws Exception {
        String portstring = System.getProperty("port.number");
        port = portstring != null ? Integer.parseInt(portstring) : 0;
        portstring = System.getProperty("port.number1");
        proxyPort = portstring != null ? Integer.parseInt(portstring) : 0;

        Logger logger = Logger.getLogger("com.sun.net.httpserver");
        ConsoleHandler ch = new ConsoleHandler();
        logger.setLevel(Level.ALL);
        ch.setLevel(Level.ALL);
        logger.addHandler(ch);
        String root = System.getProperty("test.src")+ "/docs";
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
        s1 = HttpServer.create (addr, 0);
        if (s1 instanceof HttpsServer) {
            throw new RuntimeException("should not be httpsserver");
        }
        s1.createContext("/files", new FileServerHandler(root));
        s1.createContext("/redirect", new RedirectHandler("/redirect"));

        executor = Executors.newCachedThreadPool();
        s1.setExecutor(executor);
        s1.start();

        if (port == 0)
            port = s1.getAddress().getPort();
        else {
            if (s1.getAddress().getPort() != port)
                throw new RuntimeException("Error wrong port");
            System.out.println("Port was assigned by Driver");
        }
        System.out.println("HTTP server port = " + port);
        httproot = "http://localhost:" + port + "/files/";
        redirectroot = "http://localhost:" + port + "/redirect/";
        uri = new URI(httproot);
        fileuri = httproot + "foo.txt";
    }

    static class RedirectHandler implements HttpHandler {

        String root;
        int count = 0;

        RedirectHandler(String root) {
            this.root = root;
        }

        synchronized int count() {
            return count;
        }

        synchronized void increment() {
            count++;
        }

        @Override
        public synchronized void handle(HttpExchange t) throws IOException {
            System.out.println("Server: " + t.getRequestURI());
            try (InputStream is = t.getRequestBody()) {
               is.readAllBytes();
            }
            increment();
            if (count() == 1) {
                Headers map = t.getResponseHeaders();
                String redirect = "/redirect/bar.txt";
                map.add("Location", redirect);
                t.sendResponseHeaders(301, -1);
                t.close();
            } else {
                String response = "Hello world";
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes(StandardCharsets.ISO_8859_1));
                t.close();
            }
        }
    }
}
