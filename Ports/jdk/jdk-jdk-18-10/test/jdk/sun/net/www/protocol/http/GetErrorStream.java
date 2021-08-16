/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4160499
 * @modules jdk.httpserver
 * @summary sun.net.www.protocol.http.HttpURLConnection.getErrorStream not hooked up
 */
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class GetErrorStream {
    public static void main(String[] args) throws Exception {
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        HttpServer server = HttpServer.create(addr, 10);
        server.createContext("/" + HTTP_NOT_FOUND, he -> {
            final String RESPONSE = "Test: File Not Found.";
            he.sendResponseHeaders(HTTP_NOT_FOUND, RESPONSE.length());
            OutputStream os = he.getResponseBody();
            os.write(RESPONSE.getBytes());
            os.close();
        });
        int port = server.getAddress().getPort();
        System.out.println("Server port = " + port);

        ExecutorService executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.start();

        URL url = new URL("http://localhost:" + port + "/" + HTTP_NOT_FOUND);
        URLConnection conn = url.openConnection();

        try {
            InputStream is = conn.getInputStream();
            throw new RuntimeException("Expect HTTP_NOT_FOUND!");
        } catch (FileNotFoundException e) {
            try {
                int respCode = ((HttpURLConnection) conn).getResponseCode();
                InputStream es = ((HttpURLConnection) conn).getErrorStream();
                if (respCode == HTTP_NOT_FOUND && es != null) {
                    System.out.println("Passed!");
                } else {
                    throw new RuntimeException("getErrorStream failure.");
                }
            } catch (Exception ex) {
            }
        } finally {
            server.stop(0);
            executor.shutdownNow();
        }

    }
}
