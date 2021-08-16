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
 * @bug 6181108
 * @library /test/lib
 * @summary double encoded URL passed to ResponseCache
 * @author Edward Wang
 */

import java.net.*;
import java.util.*;
import java.io.*;
import jdk.test.lib.net.URIBuilder;
import static java.net.Proxy.NO_PROXY;

public class B6181108 implements Runnable {
    ServerSocket ss;
    static String urlWithSpace;

    /*
     * "Our" http server just return 200
     */
    public void run() {
        try {
            Socket s = ss.accept();

            InputStream is = s.getInputStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            String x;
            while ((x=r.readLine()) != null) {
                if (x.length() ==0) {
                    break;
                }
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { ss.close(); } catch (IOException unused) {}
        }
    }

    static class ResponseCache extends java.net.ResponseCache {
        public CacheResponse get(URI uri, String method, Map<String,List<String>> hdrs) {
            System.out.println("get uri = " + uri);
            if (!urlWithSpace.equals(uri.toString())) {
                throw new RuntimeException("test failed");
            }
            return null;
        }
        public CacheRequest put(URI uri,  URLConnection urlc) {
            System.out.println("put uri = " + uri);
            return null;
        }
    }

    B6181108() throws Exception {
        /* start the server */
        InetAddress loopback = InetAddress.getLoopbackAddress();
        ss = new ServerSocket();
        ss.bind(new InetSocketAddress(loopback, 0));
        (new Thread(this)).start();

        ResponseCache.setDefault(new ResponseCache());
        String base = URIBuilder.newBuilder()
                   .scheme("http")
                   .loopback()
                   .port(ss.getLocalPort())
                   .build()
                   .toString();
        urlWithSpace = base + "/space%20test/page1.html";
        URL url = new URL(urlWithSpace);
        URLConnection urlc = url.openConnection(NO_PROXY);
        int i = ((HttpURLConnection)(urlc)).getResponseCode();
        System.out.println("response code = " + i);
        ResponseCache.setDefault(null);
    }

    public static void main(String args[]) throws Exception {
        new B6181108();
    }

}
