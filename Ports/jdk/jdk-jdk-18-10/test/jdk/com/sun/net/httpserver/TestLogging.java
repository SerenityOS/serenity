/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6422914
 * @library /test/lib
 * @summary change httpserver exception printouts
 * @run main TestLogging
 * @run main/othervm -Djava.net.preferIPv6Addresses=true TestLogging
 */

import com.sun.net.httpserver.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;
import jdk.test.lib.net.URIBuilder;

public class TestLogging extends Test {

    public static void main (String[] args) throws Exception {
        HttpServer s1 = null;
        ExecutorService executor=null;

        try {
            System.out.print ("Test9: ");
            String root = System.getProperty ("test.src")+ "/docs";
            InetAddress loopback = InetAddress.getLoopbackAddress();
            InetSocketAddress addr = new InetSocketAddress(loopback, 0);
            Logger logger = Logger.getLogger ("com.sun.net.httpserver");
            logger.setLevel (Level.ALL);
            Handler h1 = new ConsoleHandler ();
            h1.setLevel (Level.ALL);
            logger.addHandler (h1);
            s1 = HttpServer.create (addr, 0);
            logger.info (root);
            HttpHandler h = new FileServerHandler (root);
            HttpContext c1 = s1.createContext ("/test1", h);
            executor = Executors.newCachedThreadPool();
            s1.setExecutor (executor);
            s1.start();

            int p1 = s1.getAddress().getPort();

            URL url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(p1)
                .path("/test1/smallfile.txt")
                .toURL();
            System.out.println("URL: " + url);
            HttpURLConnection urlc = (HttpURLConnection)url.openConnection(Proxy.NO_PROXY);
            InputStream is = urlc.getInputStream();
            while (is.read() != -1) ;
            is.close();

            url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(p1)
                .path("/test1/doesntexist.txt")
                .toURLUnchecked();
            System.out.println("URL: " + url);
            urlc = (HttpURLConnection)url.openConnection();
            try {
                is = urlc.getInputStream();
                while (is.read() != -1) ;
                is.close();
            } catch (IOException e) {
                System.out.println ("caught expected exception");
            }

            Socket s = new Socket (InetAddress.getLoopbackAddress(), p1);
            OutputStream os = s.getOutputStream();
            //os.write ("GET xxx HTTP/1.1\r\n".getBytes());
            os.write ("HELLO WORLD\r\n".getBytes());
            is = s.getInputStream();
            while (is.read() != -1) ;
            os.close(); is.close(); s.close();
            System.out.println ("OK");
        } finally {
            delay();
            if (s1 != null)
                s1.stop(2);
            if (executor != null)
                executor.shutdown();
        }
    }
}
