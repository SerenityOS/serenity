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
 * @bug 6270015
 * @library /test/lib
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run main/othervm -Dsun.net.httpserver.selCacheTimeout=2 SelCacheTest
 * @run main/othervm -Djava.net.preferIPv6Addresses=true
                     -Dsun.net.httpserver.selCacheTimeout=2 SelCacheTest
 * @summary  Light weight HTTP server
 */

import com.sun.net.httpserver.*;
import jdk.test.lib.net.SimpleSSLContext;
import jdk.test.lib.net.URIBuilder;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;

/* basic http/s connectivity test
 * (based on Test1)
 */

public class SelCacheTest extends Test {

    static SSLContext ctx;

    public static void main(String[] args) throws Exception {
        HttpServer s1 = null;
        HttpsServer s2 = null;
        ExecutorService executor=null;
        InetAddress loopback = InetAddress.getLoopbackAddress();
        try {
            String root = System.getProperty("test.src")+ "/docs";
            System.out.print("Test1: ");
            InetSocketAddress addr = new InetSocketAddress(loopback, 0);
            s1 = HttpServer.create(addr, 0);
            if (s1 instanceof HttpsServer) {
                throw new RuntimeException("should not be httpsserver");
            }
            s2 = HttpsServer.create(addr, 0);
            HttpHandler h = new FileServerHandler(root);
            HttpContext c1 = s1.createContext("/test1", h);
            HttpContext c2 = s2.createContext("/test1", h);
            executor = Executors.newCachedThreadPool();
            s1.setExecutor(executor);
            s2.setExecutor(executor);
            ctx = new SimpleSSLContext().get();
            s2.setHttpsConfigurator(new HttpsConfigurator(ctx));
            s1.start();
            s2.start();

            int port = s1.getAddress().getPort();
            int httpsport = s2.getAddress().getPort();
            test(true, "http", root+"/test1", loopback, port, "smallfile.txt", 23);
            test(true, "http", root+"/test1", loopback, port, "largefile.txt", 2730088);
            test(true, "https", root+"/test1", loopback, httpsport, "smallfile.txt", 23);
            test(true, "https", root+"/test1", loopback, httpsport, "largefile.txt", 2730088);
            test(false, "http", root+"/test1", loopback, port, "smallfile.txt", 23);
            test(false, "http", root+"/test1", loopback, port, "largefile.txt", 2730088);
            test(false, "https", root+"/test1", loopback, httpsport, "smallfile.txt", 23);
            test(false, "https", root+"/test1", loopback, httpsport, "largefile.txt", 2730088);
            System.out.println("OK");
        } finally {
            delay();
            s1.stop(2);
            s2.stop(2);
            executor.shutdown();
        }
    }

    static void test(boolean fixedLen, String protocol, String root,
                     InetAddress address, int port, String f, int size) throws Exception {
        Thread.sleep(2000);
        URL url = URIBuilder.newBuilder()
                  .scheme(protocol)
                  .host(address)
                  .port(port)
                  .path("/test1/"+f)
                  .toURL();
        HttpURLConnection urlc = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        if (urlc instanceof HttpsURLConnection) {
            HttpsURLConnection urlcs = (HttpsURLConnection) urlc;
            urlcs.setHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String s, SSLSession s1) {
                    return true;
                }
            });
            urlcs.setSSLSocketFactory(ctx.getSocketFactory());
        }
        byte [] buf = new byte [4096];

        if (fixedLen) {
            urlc.setRequestProperty("XFixed", "yes");
        }
        InputStream is = urlc.getInputStream();
        File temp = File.createTempFile("Test1", null);
        temp.deleteOnExit();
        OutputStream fout = new BufferedOutputStream(new FileOutputStream(temp));
        int c, count = 0;
        while ((c=is.read(buf)) != -1) {
            count += c;
            fout.write(buf, 0, c);
        }
        is.close();
        fout.close();

        if (count != size) {
            throw new RuntimeException("wrong amount of data returned");
        }
        String orig = root + "/" + f;
        compare(new File(orig), temp);
        temp.delete();
    }

    /* compare the contents of the two files */

    static void compare(File f1, File f2) throws IOException {
        InputStream i1 = new BufferedInputStream(new FileInputStream(f1));
        InputStream i2 = new BufferedInputStream(new FileInputStream(f2));

        int c1,c2;

        try {
            while ((c1=i1.read()) != -1) {
                c2 = i2.read();
                if (c1 != c2) {
                    throw new RuntimeException("file compare failed 1");
                }
            }
            if (i2.read() != -1) {
                throw new RuntimeException("file compare failed 2");
            }
        } finally {
            i1.close();
            i2.close();
        }
    }
}
