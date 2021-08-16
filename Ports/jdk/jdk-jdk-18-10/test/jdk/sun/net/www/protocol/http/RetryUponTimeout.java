/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4772077
 * @library /test/lib
 * @summary  using defaultReadTimeout appear to retry request upon timeout
 * @modules java.base/sun.net.www
 */

import java.net.*;
import java.io.*;
import jdk.test.lib.net.URIBuilder;
import sun.net.www.*;

public class RetryUponTimeout implements Runnable {
    // run server
    public void run(){
        Socket socket = null;
        try {
            for (int i = 0; i < 2; i++) {
                socket = server.accept();
                InputStream is = socket.getInputStream ();
                MessageHeader header = new MessageHeader (is);
                count++;
            }
        } catch (Exception ex) {
            // ignored
        } finally {
            try {
                socket.close();
                server.close();
            } catch (IOException ioex) {
            }
        }

    }

    static ServerSocket server;
    static int count = 0;
    public static void main(String[] args) throws Exception {
        try {
            server = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
            int port = server.getLocalPort ();
            new Thread(new RetryUponTimeout()).start ();

            URL url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(port)
                .toURL();
            System.out.println("URL: " + url);
            java.net.URLConnection uc = url.openConnection();
            uc.setReadTimeout(1000);
            uc.getInputStream();
        } catch (SocketTimeoutException stex) {
            // expected exception
            server.close();
        }

        if (count > 1) {
            throw new RuntimeException("Server received "+count+" requests instead of one.");
        }

    }
}
