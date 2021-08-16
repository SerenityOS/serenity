/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6270015
 * @library /test/lib
 * @build jdk.test.lib.net.SimpleSSLContext jdk.test.lib.net.URIBuilder
 * @run main/othervm Test7a
 * @run main/othervm -Djava.net.preferIPv6Addresses=true Test7a
 * @summary Light weight HTTP server
 */

import com.sun.net.httpserver.*;

import java.util.concurrent.*;
import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import jdk.test.lib.net.SimpleSSLContext;
import jdk.test.lib.net.URIBuilder;

/**
 * Test POST large file via chunked encoding (large chunks)
 */

public class Test7a extends Test {

    public static void main (String[] args) throws Exception {
        //Logger log = Logger.getLogger ("com.sun.net.httpserver");
        //log.setLevel (Level.FINE);
        //ConsoleHandler h = new ConsoleHandler();
        //h.setLevel (Level.ALL);
        //log.addHandler (h);
        Handler handler = new Handler();
        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetSocketAddress addr = new InetSocketAddress(loopback, 0);
        HttpsServer server = HttpsServer.create (addr, 0);
        HttpContext ctx = server.createContext ("/test", handler);
        ExecutorService executor = Executors.newCachedThreadPool();
        SSLContext ssl = new SimpleSSLContext().get();
        server.setHttpsConfigurator(new HttpsConfigurator (ssl));
        server.setExecutor (executor);
        server.start ();

        URL url = URIBuilder.newBuilder()
            .scheme("https")
            .loopback()
            .port(server.getAddress().getPort())
            .path("/test/foo.html")
            .toURL();

        System.out.print ("Test7a: " );
        HttpsURLConnection urlc = (HttpsURLConnection)url.openConnection(Proxy.NO_PROXY);
        urlc.setDoOutput (true);
        urlc.setRequestMethod ("POST");
        urlc.setChunkedStreamingMode (16 * 1024); // big chunks
        urlc.setHostnameVerifier (new DummyVerifier());
        urlc.setSSLSocketFactory (ssl.getSocketFactory());
        OutputStream os = new BufferedOutputStream (urlc.getOutputStream(), 8000);
        for (int i=0; i<SIZE; i++) {
            os.write (i % 100);
        }
        os.close();
        int resp = urlc.getResponseCode();
        if (resp != 200) {
            throw new RuntimeException ("test failed response code");
        }
        if (error) {
            throw new RuntimeException ("test failed error");
        }
        delay();
        server.stop(2);
        executor.shutdown();
        System.out.println ("OK");

    }

    public static boolean error = false;
    final static int SIZE = 999999;

    static class Handler implements HttpHandler {
        int invocation = 1;
        public void handle (HttpExchange t)
            throws IOException
        {
            InputStream is = t.getRequestBody();
            Headers map = t.getRequestHeaders();
            Headers rmap = t.getResponseHeaders();
            int c, count=0;
            while ((c=is.read ()) != -1) {
                if (c != (count % 100)) {
                    error = true;
                    break;
                }
                count ++;
            }
            if (count != SIZE) {
                error = true;
            }
            is.close();
            t.sendResponseHeaders (200, -1);
            t.close();
        }
    }
}
