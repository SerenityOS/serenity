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
 * @run main/othervm Test8a
 * @run main/othervm -Djava.net.preferIPv6Addresses=true Test8a
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
 * Test POST large file via fixed len encoding
 */

public class Test8a extends Test {

    public static void main (String[] args) throws Exception {
        //Logger log = Logger.getLogger ("com.sun.net.httpserver");
        //ConsoleHandler h = new ConsoleHandler();
        //h.setLevel (Level.INFO);
        //log.addHandler (h);
        //log.setLevel (Level.INFO);
        HttpsServer server = null;
        ExecutorService executor = null;
        try {
            Handler handler = new Handler();
            InetAddress loopback = InetAddress.getLoopbackAddress();
            InetSocketAddress addr = new InetSocketAddress(loopback, 0);
            server = HttpsServer.create (addr, 0);
            HttpContext ctx = server.createContext ("/test", handler);
            executor = Executors.newCachedThreadPool();
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

            System.out.print ("Test8a: " );
            HttpsURLConnection urlc = (HttpsURLConnection)url.openConnection(Proxy.NO_PROXY);
            urlc.setDoOutput (true);
            urlc.setRequestMethod ("POST");
            urlc.setHostnameVerifier (new DummyVerifier());
            urlc.setSSLSocketFactory (ssl.getSocketFactory());
            OutputStream os = new BufferedOutputStream (urlc.getOutputStream(), 8000);
            for (int i=0; i<SIZE; i++) {
                os.write (i % 250);
            }
            os.close();
            int resp = urlc.getResponseCode();
            if (resp != 200) {
                throw new RuntimeException ("test failed response code");
            }
            InputStream is = urlc.getInputStream ();
            for (int i=0; i<SIZE; i++) {
                int f = is.read();
                if (f != (i % 250)) {
                    System.out.println ("Setting error(" +f +")("+i+")" );
                    error = true;
                    break;
                }
            }
            is.close();
        } finally {
            delay();
            if (server != null) server.stop(2);
            if (executor != null) executor.shutdown();
        }
        if (error) {
            throw new RuntimeException ("test failed error");
        }
        System.out.println ("OK");

    }

    public static boolean error = false;
    //final static int SIZE = 999999;
    final static int SIZE = 9999;

    static class Handler implements HttpHandler {
        int invocation = 1;
        public void handle (HttpExchange t)
            throws IOException
        {
        System.out.println ("Handler.handle");
            InputStream is = t.getRequestBody();
            Headers map = t.getRequestHeaders();
            Headers rmap = t.getResponseHeaders();
            int c, count=0;
            while ((c=is.read ()) != -1) {
                if (c != (count % 250)) {
                System.out.println ("Setting error 1");
                    error = true;
                    break;
                }
                count ++;
            }
            if (count != SIZE) {
                System.out.println ("Setting error 2");
                error = true;
            }
            is.close();
            t.sendResponseHeaders (200, SIZE);
                System.out.println ("Sending 200 OK");
            OutputStream os = new BufferedOutputStream(t.getResponseBody(), 8000);
            for (int i=0; i<SIZE; i++) {
                os.write (i % 250);
            }
            os.close();
                System.out.println ("Finished");
        }
    }
}
