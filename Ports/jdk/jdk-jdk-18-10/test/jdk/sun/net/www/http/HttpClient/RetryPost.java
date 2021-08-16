/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6427251 6382788
 * @modules jdk.httpserver
 * @library /test/lib
 * @run main RetryPost
 * @run main/othervm -Dsun.net.http.retryPost=false RetryPost noRetry
 * @summary HttpURLConnection automatically retries non-idempotent method POST
 */

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import jdk.test.lib.net.URIBuilder;

public class RetryPost
{
    static boolean shouldRetry = true;

    com.sun.net.httpserver.HttpServer httpServer;
    MyHandler httpHandler;
    ExecutorService executorService;

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("noRetry"))
            shouldRetry = false;

        new RetryPost();
    }

    public RetryPost() throws Exception {
        startHttpServer(shouldRetry);
        doClient();
    }

    void doClient() throws Exception {
        try {
            InetSocketAddress address = httpServer.getAddress();
            URL url = URIBuilder.newBuilder()
                      .scheme("http")
                      .host(address.getAddress())
                      .port(address.getPort())
                      .path("/test/")
                      .toURLUnchecked();
            HttpURLConnection uc = (HttpURLConnection)url.openConnection(Proxy.NO_PROXY);
            uc.setDoOutput(true);
            uc.setRequestMethod("POST");
            uc.getResponseCode();

            // if we reach here then we have failed
            throw new RuntimeException("Failed: POST request being retried");

        } catch (SocketException se) {
            System.out.println("Got expected exception: " + se);
            // this is what we expect to happen and is OK.
            if (shouldRetry && httpHandler.getCallCount() != 2) {
                se.printStackTrace(System.out);
                throw new RuntimeException("Failed: Handler should have been called twice. " +
                                           "It was called "+ httpHandler.getCallCount() + " times");
            } else if (!shouldRetry && httpHandler.getCallCount() != 1) {
                se.printStackTrace(System.out);
                throw new RuntimeException("Failed: Handler should have only been called once" +
                                           "It was called "+ httpHandler.getCallCount() + " times");
            }
        } finally {
            httpServer.stop(1);
            executorService.shutdown();
        }
    }

    /**
     * Http Server
     */
    public void startHttpServer(boolean shouldRetry) throws IOException {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        httpServer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(loopback, 0), 0);
        httpHandler = new MyHandler(shouldRetry);

        HttpContext ctx = httpServer.createContext("/test/", httpHandler);

        executorService = Executors.newCachedThreadPool();
        httpServer.setExecutor(executorService);
        httpServer.start();
    }

    class MyHandler implements HttpHandler {
        volatile int callCount = 0;
        final boolean shouldRetry;

        public MyHandler(boolean shouldRetry) {
            this.shouldRetry = shouldRetry;
        }

        public void handle(HttpExchange t) throws IOException {
            callCount++;

            if (callCount > 1 && !shouldRetry) {
                // if this bug has been fixed then this method will not be called twice
                // when -Dhttp.retryPost=false
                t.sendResponseHeaders(400, -1);  // indicate failure by returning 400
            } else {
                // simply close out the stream without sending any data.
                OutputStream os = t.getResponseBody();
                os.close();
            }
        }

        public int getCallCount() {
            return callCount;
        }
    }

}
