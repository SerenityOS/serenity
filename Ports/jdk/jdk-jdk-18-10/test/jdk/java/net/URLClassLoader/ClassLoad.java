/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4151665
 * @modules jdk.httpserver
 * @library /test/lib
 * @summary Test for FileNotFoundException when loading bogus class
 */

import java.io.InputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jdk.test.lib.net.URIBuilder;

public class ClassLoad {
     public static void main(String[] args) throws Exception {
         boolean error = true;

         // Start a dummy server to return 404
         HttpServer server = HttpServer.create();
         server.bind(new InetSocketAddress(
                 InetAddress.getLoopbackAddress(), 0), 0);
         HttpHandler handler = new HttpHandler() {
             public void handle(HttpExchange t) throws IOException {
                 InputStream is = t.getRequestBody();
                 while (is.read() != -1);
                 t.sendResponseHeaders (404, -1);
                 t.close();
             }
         };
         server.createContext("/", handler);
         server.start();

         // Client request
         try {
             URL url = URIBuilder.newBuilder()
                     .scheme("http")
                     .loopback()
                     .port(server.getAddress().getPort())
                     .toURL();
             String name = args.length >= 2 ? args[1] : "foo.bar.Baz";
             ClassLoader loader = new URLClassLoader(new URL[] { url });
             System.out.println(url);
             Class c = loader.loadClass(name);
             System.out.println("Loaded class \"" + c.getName() + "\".");
         } catch (ClassNotFoundException ex) {
             System.out.println(ex);
             error = false;
         } finally {
             server.stop(0);
         }
         if (error)
             throw new RuntimeException("No ClassNotFoundException generated");
    }
}
