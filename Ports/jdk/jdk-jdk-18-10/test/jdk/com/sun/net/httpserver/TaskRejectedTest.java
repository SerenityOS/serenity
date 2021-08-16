/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8169358
 * @summary  HttpServer does not close client connection when RejectedExecutionException occurs.
 */

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;


public class TaskRejectedTest {

    private static final int BACKLOG = 0;

    private static final String REQUEST_PATH = "/";

    private static final int TIMEOUT = 10000; // 10 sec

    private static void runClient(InetSocketAddress listenAddr)
                                     throws MalformedURLException, IOException {
        URL url = new URL("http", listenAddr.getHostString(),
                                            listenAddr.getPort(), REQUEST_PATH);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setConnectTimeout(TIMEOUT);
        con.setReadTimeout(TIMEOUT);

        try {
            con.connect();
            con.getResponseCode();
        } catch (SocketTimeoutException e) {
            throw new RuntimeException("Connection was not closed by peer.", e);
        } catch (SocketException e) {
            // Expected (connection reset)
        } finally {
            con.disconnect();
        }
    }

    public static void main(String[] args) throws Exception {
        Logger logger = Logger.getLogger(
                            HttpServer.class.getPackage().getName());
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINEST);
        logger.setLevel(Level.FINEST);
        logger.addHandler(consoleHandler);

        Executor executor = Executors.newSingleThreadExecutor(r -> {
            throw new RejectedExecutionException("test");
        });

        InetSocketAddress addr = new InetSocketAddress(
                                         InetAddress.getLoopbackAddress(), 0);
        HttpServer httpServer = HttpServer.create(addr, BACKLOG);
        httpServer.setExecutor(executor);

        httpServer.createContext(REQUEST_PATH, exc -> {
            exc.sendResponseHeaders(200, 0);
            exc.close();
        });

        try {
            httpServer.start();
            runClient(httpServer.getAddress());
        } finally {
            httpServer.stop(0);
        }
    }
}

