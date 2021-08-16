/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4473092
 * @library /test/lib
 * @summary Method throws IOException when object should be returned
 * @run main HttpResponseCode
 * @run main/othervm -Djava.net.preferIPv6Addresses=true HttpResponseCode
 */
import java.net.*;
import java.io.*;
import jdk.test.lib.net.URIBuilder;

public class HttpResponseCode implements Runnable {
    ServerSocket ss;
    /*
     * Our "http" server
     */
    public void run() {
        try {
            Socket s = ss.accept();

            BufferedReader in = new BufferedReader(
                new InputStreamReader(s.getInputStream()) );
            String req = in.readLine();

            PrintStream out = new PrintStream(
                                 new BufferedOutputStream(
                                    s.getOutputStream() ));


            /* send the header */
            out.print("HTTP/1.1 403 Forbidden\r\n");
            out.print("\r\n");
            out.flush();

            s.close();
            ss.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    HttpResponseCode() throws Exception {
        /* start the server */
        InetAddress loopback = InetAddress.getLoopbackAddress();
        ss = new ServerSocket();
        ss.bind(new InetSocketAddress(loopback, 0));
        (new Thread(this)).start();

        /* establish http connection to server */
        URL url = URIBuilder.newBuilder()
            .scheme("http")
            .loopback()
            .port(ss.getLocalPort())
            .path("/missing.nothtml")
            .toURL();
        URLConnection uc = url.openConnection(Proxy.NO_PROXY);
        int respCode1 = ((HttpURLConnection)uc).getResponseCode();
        ((HttpURLConnection)uc).disconnect();
        int respCode2 = ((HttpURLConnection)uc).getResponseCode();
        if (respCode1 != 403 || respCode2 != 403) {
            throw new RuntimeException("Testing Http response code failed");
        }
    }

    public static void main(String args[]) throws Exception {
        new HttpResponseCode();
    }
}
