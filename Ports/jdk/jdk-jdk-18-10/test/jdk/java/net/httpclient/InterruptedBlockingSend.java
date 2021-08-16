/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import static java.lang.System.out;

/**
 * @test
 * @bug 8245462
 * @summary Basic test for interrupted blocking send
 * @run main/othervm InterruptedBlockingSend
 */

public class InterruptedBlockingSend {

    static volatile Throwable throwable;

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        try (ServerSocket ss = new ServerSocket()) {
            ss.setReuseAddress(false);
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            int port = ss.getLocalPort();
            URI uri = new URI("http://localhost:" + port + "/");

            HttpRequest request = HttpRequest.newBuilder(uri).build();

            Thread t = new Thread(() -> {
                try {
                    client.send(request, BodyHandlers.discarding());
                } catch (InterruptedException e) {
                    throwable = e;
                } catch (Throwable th) {
                    throwable = th;
                }
            });
            t.start();
            Thread.sleep(5000);
            t.interrupt();
            t.join();

            if (!(throwable instanceof InterruptedException)) {
                throw new RuntimeException("Expected InterruptedException, got " + throwable);
            } else {
                out.println("Caught expected InterruptedException: " + throwable);
            }

            out.println("Interrupting before send");
            try {
                Thread.currentThread().interrupt();
                client.send(request, BodyHandlers.discarding());
                throw new AssertionError("Expected InterruptedException not thrown");
            } catch (InterruptedException x) {
                out.println("Got expected exception: " + x);
            }
        }
    }
}
