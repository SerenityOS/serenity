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
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run main/othervm Test12
 * @run main/othervm -Djava.net.preferIPv6Addresses=true Test12
 * @summary Light weight HTTP server
 */

import com.sun.net.httpserver.*;

import java.util.concurrent.*;
import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import jdk.test.lib.net.SimpleSSLContext;
import jdk.test.lib.net.URIBuilder;

/* basic http/s connectivity test
 * Tests:
 *      - same as Test1, but in parallel
 */

public class Test12 extends Test {

    static SSLContext ctx;

    static boolean fail = false;

    public static void main (String[] args) throws Exception {
        HttpServer s1 = null;
        HttpsServer s2 = null;
        ExecutorService executor=null;
        try {
            String root = System.getProperty ("test.src")+ "/docs";
            System.out.print ("Test12: ");
            InetAddress loopback = InetAddress.getLoopbackAddress();
            InetSocketAddress addr = new InetSocketAddress(loopback, 0);
            s1 = HttpServer.create (addr, 0);
            s2 = HttpsServer.create (addr, 0);
            HttpHandler h = new FileServerHandler (root);
            HttpContext c1 = s1.createContext ("/test1", h);
            HttpContext c2 = s2.createContext ("/test1", h);
            executor = Executors.newCachedThreadPool();
            s1.setExecutor (executor);
            s2.setExecutor (executor);
            ctx = new SimpleSSLContext().get();
            s2.setHttpsConfigurator(new HttpsConfigurator (ctx));
            s1.start();
            s2.start();

            int port = s1.getAddress().getPort();
            int httpsport = s2.getAddress().getPort();
            Runner r[] = new Runner[8];
            r[0] = new Runner (true, "http", root+"/test1", port, "smallfile.txt", 23);
            r[1] = new Runner (true, "http", root+"/test1", port, "largefile.txt", 2730088);
            r[2] = new Runner (true, "https", root+"/test1", httpsport, "smallfile.txt", 23);
            r[3] = new Runner (true, "https", root+"/test1", httpsport, "largefile.txt", 2730088);
            r[4] = new Runner (false, "http", root+"/test1", port, "smallfile.txt", 23);
            r[5] = new Runner (false, "http", root+"/test1", port, "largefile.txt", 2730088);
            r[6] = new Runner (false, "https", root+"/test1", httpsport, "smallfile.txt", 23);
            r[7] = new Runner (false, "https", root+"/test1", httpsport, "largefile.txt", 2730088);
            start (r);
            join (r);
            System.out.println ("OK");
        } finally {
            delay();
            if (s1 != null)
                s1.stop(2);
            if (s2 != null)
                s2.stop(2);
            if (executor != null)
                executor.shutdown ();
        }
    }

    static void start (Runner[] x) {
        for (int i=0; i<x.length; i++) {
            x[i].start();
        }
    }

    static void join (Runner[] x) {
        for (int i=0; i<x.length; i++) {
            try {
                x[i].join();
            } catch (InterruptedException e) {}
        }
    }


    static class Runner extends Thread {

        boolean fixedLen;
        String protocol;
        String root;
        int port;
        String f;
        int size;

        Runner (boolean fixedLen, String protocol, String root, int port, String f, int size) {
            this.fixedLen=fixedLen;
            this.protocol=protocol;
            this.root=root;
            this.port=port;
            this.f=f;
            this.size = size;
        }

        public void run () {
            try {
                URL url = URIBuilder.newBuilder()
                          .scheme(protocol)
                          .loopback()
                          .port(port)
                          .path("/test1/"+f)
                          .toURL();
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
                if (urlc instanceof HttpsURLConnection) {
                    HttpsURLConnection urlcs = (HttpsURLConnection) urlc;
                    urlcs.setHostnameVerifier (new HostnameVerifier () {
                        public boolean verify (String s, SSLSession s1) {
                            return true;
                        }
                    });
                    urlcs.setSSLSocketFactory (ctx.getSocketFactory());
                }
                byte [] buf = new byte [4096];

                if (fixedLen) {
                    urlc.setRequestProperty ("XFixed", "yes");
                }
                InputStream is = urlc.getInputStream();
                File temp = File.createTempFile ("Test1", null);
                temp.deleteOnExit();
                OutputStream fout = new BufferedOutputStream (new FileOutputStream(temp));
                int c, count = 0;
                while ((c=is.read(buf)) != -1) {
                    count += c;
                    fout.write (buf, 0, c);
                }
                is.close();
                fout.close();

                if (count != size) {
                    throw new RuntimeException ("wrong amount of data returned");
                }
                String orig = root + "/" + f;
                compare (new File(orig), temp);
                temp.delete();
            } catch (Exception e) {
                e.printStackTrace();
                fail = true;
            }
        }
    }

    /* compare the contents of the two files */

    static void compare (File f1, File f2) throws IOException {
        InputStream i1 = new BufferedInputStream (new FileInputStream(f1));
        InputStream i2 = new BufferedInputStream (new FileInputStream(f2));

        int c1,c2;
        try {
            while ((c1=i1.read()) != -1) {
                c2 = i2.read();
                if (c1 != c2) {
                    throw new RuntimeException ("file compare failed 1");
                }
            }
            if (i2.read() != -1) {
                throw new RuntimeException ("file compare failed 2");
            }
        } finally {
            i1.close();
            i2.close();
        }
    }
}
