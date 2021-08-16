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
 * @bug 6189206
 * @modules java.base/sun.net.www
 * @library /test/lib
 * @run main/othervm -Dhttp.keepAlive=false CloseOptionHeader
 * @summary  HTTP client should set "Connection: close" header in request when keepalive is disabled
 */

import java.net.*;
import java.util.*;
import java.io.*;
import sun.net.www.MessageHeader;
import jdk.test.lib.net.URIBuilder;

public class CloseOptionHeader implements Runnable {
    static ServerSocket ss;
    static boolean hasCloseHeader = false;

    /*
     * "Our" http server
     */
    public void run() {
        try {
            Socket s = ss.accept();

            /* check the request to find close connection option header */
            InputStream is = s.getInputStream ();
            MessageHeader mh = new MessageHeader(is);
            String connHeader = mh.findValue("Connection");
            if (connHeader != null && connHeader.equalsIgnoreCase("close")) {
                hasCloseHeader = true;
            }

            PrintStream out = new PrintStream(
                                 new BufferedOutputStream(
                                    s.getOutputStream() ));

            /* response 200 */
            out.print("HTTP/1.1 200 OK\r\n");
            out.print("Content-Type: text/html; charset=iso-8859-1\r\n");
            out.print("Content-Length: 0\r\n");
            out.print("Connection: close\r\n");
            out.print("\r\n");
            out.print("\r\n");

            out.flush();

            s.close();
            ss.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws Exception {
        Thread tester = new Thread(new CloseOptionHeader());

        /* start the server */
        InetAddress loopback = InetAddress.getLoopbackAddress();
        ss = new ServerSocket();
        ss.bind(new InetSocketAddress(loopback, 0));
        tester.start();

        /* connect to the server just started
         * server then check the request to see whether
         * there is a close connection option header in it
         */
        URL url = URIBuilder.newBuilder()
            .scheme("http")
            .host(ss.getInetAddress())
            .port(ss.getLocalPort())
            .toURL();
        HttpURLConnection huc = (HttpURLConnection)url.openConnection();
        huc.connect();
        huc.getResponseCode();
        huc.disconnect();

        tester.join();

        if (!hasCloseHeader) {
            throw new RuntimeException("Test failed : should see 'close' connection header");
        }
    }

}
