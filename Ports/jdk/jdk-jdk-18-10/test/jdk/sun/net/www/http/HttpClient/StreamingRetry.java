/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6672144 8050983
 * @summary Do not retry failed request with a streaming body.
 * @library /test/lib
 * @run main StreamingRetry
 * @run main/othervm -Djava.net.preferIPv6Addresses=true StreamingRetry
 */

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import static java.lang.System.out;

import jdk.test.lib.net.URIBuilder;

public class StreamingRetry implements Runnable {
    static final int ACCEPT_TIMEOUT = 20 * 1000; // 20 seconds
    volatile ServerSocket ss;

    public static void main(String[] args) throws Exception {
        (new StreamingRetry()).instanceMain();
    }

    void instanceMain() throws Exception {
        out.println("Test with default method");
        test(null);
        out.println("Test with POST method");
        test("POST");
        out.println("Test with PUT method");
        test("PUT");

        if (failed > 0) throw new RuntimeException("Some tests failed");
    }

    void test(String method) throws Exception {
        ss = new ServerSocket();
        ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        ss.setSoTimeout(ACCEPT_TIMEOUT);
        int port = ss.getLocalPort();

        Thread otherThread = new Thread(this);
        otherThread.start();

        try {
            URL url = URIBuilder.newBuilder()
                      .scheme("http")
                      .host(ss.getInetAddress())
                      .port(port)
                      .path("/")
                      .toURL();
            HttpURLConnection uc = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
            uc.setDoOutput(true);
            if (method != null)
                uc.setRequestMethod(method);
            uc.setChunkedStreamingMode(4096);
            OutputStream os = uc.getOutputStream();
            os.write("Hello there".getBytes());

            InputStream is = uc.getInputStream();
            is.close();
        } catch (IOException expected) {
            //expected.printStackTrace();
        } finally {
            ss.close();
            otherThread.join();
        }
    }

    // Server
    public void run() {
        try {
            (ss.accept()).close();
            (ss.accept()).close();
            ss.close();
            fail("The server shouldn't accept a second connection");
         } catch (IOException e) {
            //OK, the client will close the server socket if successful
        }
    }

    volatile int failed = 0;
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
}
