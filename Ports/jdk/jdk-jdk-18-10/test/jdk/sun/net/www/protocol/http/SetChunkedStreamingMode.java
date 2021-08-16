/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5049976
 * @modules java.base/sun.net.www
 * @library ../../httptest/
 * @library /test/lib
 * @build HttpCallback TestHttpServer ClosedChannelList HttpTransaction
 * @run main SetChunkedStreamingMode
 * @summary Unspecified NPE is thrown when streaming output mode is enabled
 */

import java.io.*;
import java.net.*;
import jdk.test.lib.net.URIBuilder;

public class SetChunkedStreamingMode implements HttpCallback {

    void okReply (HttpTransaction req) throws IOException {
        req.setResponseEntityBody ("Hello .");
        req.sendResponse (200, "Ok");
            System.out.println ("Server: sent response");
        req.orderlyClose();
    }

    public void request (HttpTransaction req) {
        try {
            okReply (req);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void read (InputStream is) throws IOException {
        int c;
        System.out.println ("reading");
        while ((c=is.read()) != -1) {
            System.out.write (c);
        }
        System.out.println ("");
        System.out.println ("finished reading");
    }

    static TestHttpServer server;

    public static void main (String[] args) throws Exception {
        try {
            server = new TestHttpServer(new SetChunkedStreamingMode(), 1, 10,
                    InetAddress.getLoopbackAddress(), 0);
            System.out.println ("Server: listening on port: " + server.getLocalPort());
            URL url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(server.getLocalPort())
                .path("/")
                .toURL();
            System.out.println ("Client: connecting to " + url);
            HttpURLConnection urlc = (HttpURLConnection)url.openConnection();
            urlc.setChunkedStreamingMode (0);
            urlc.setRequestMethod("POST");
            urlc.setDoOutput(true);
            InputStream is = urlc.getInputStream();
        } catch (Exception e) {
            if (server != null) {
                server.terminate();
            }
            throw e;
        }
        server.terminate();
    }

    public static void except (String s) {
        server.terminate();
        throw new RuntimeException (s);
    }
}
