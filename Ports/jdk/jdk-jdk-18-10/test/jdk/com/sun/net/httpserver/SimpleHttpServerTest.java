/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8015692
 * @key intermittent
 * @summary  Test HttpServer instantiation, start, and stop repeated in a loop
 *           Testing for Bind exception on Windows. This test may fail
 *           intermittently if other tests / process manage to bind to
 *           the same port that the test is using in the short window
 *           time where the port might appear available again.
 */

import java.net.InetSocketAddress;
import java.net.ServerSocket;

import com.sun.net.httpserver.HttpServer;


public class SimpleHttpServerTest {

    public static void main(String[] args) throws Exception {

        System.out.println(System.getProperty("java.version"));
        InetSocketAddress serverAddr = new InetSocketAddress(0);
        HttpServer server = HttpServer.create(serverAddr, 0);
        int serverPort = server.getAddress().getPort();
        server.start();
        server.stop(0);
        serverAddr = new InetSocketAddress(serverPort);
        int exceptionCount = 0;
        boolean failedOnce = false;
        System.out.println("Using serverPort == " + serverPort);
        RETRY: while (exceptionCount == 0) {
            for (int i = 0; i < 100; i++) {
                try {
                    server = HttpServer.create(serverAddr, 0);
                    server.start();
                    server.stop(0);
                } catch (Exception ex) {
                    if (!failedOnce) {
                        failedOnce = true;
                        server = HttpServer.create(new InetSocketAddress(0), 0);
                        serverPort = server.getAddress().getPort();
                        server.start();
                        server.stop(0);
                        serverAddr = new InetSocketAddress(serverPort);
                        System.out.println("Retrying with serverPort == " + serverPort);
                        continue RETRY;
                    }
                    System.err.println("Got exception at iteration: " + i );
                    ex.printStackTrace();
                    exceptionCount++;
                }
            }
            break;
        }
        if (exceptionCount > 0) {
           throw new RuntimeException("Test Failed: got "
                 + exceptionCount + " exceptions.");
        }
    }
}
