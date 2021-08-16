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
 * @bug 4512200
 * @library /test/lib
 * @modules java.base/sun.net.www
 * @run main/othervm -Dhttp.agent=foo UserAgent
 * @run main/othervm -Dhttp.agent=foo -Djava.net.preferIPv6Addresses=true UserAgent
 * @summary  HTTP header "User-Agent" format incorrect
 */

import java.io.*;
import java.util.*;
import java.net.*;
import jdk.test.lib.net.URIBuilder;
import sun.net.www.MessageHeader;

class Server extends Thread {
    Server (ServerSocket server) {
        this.server = server;
    }
    public void run () {
        try {
            String version = System.getProperty ("java.version");
            String expected = "foo Java/"+version;
            Socket s = server.accept ();
            MessageHeader header = new MessageHeader (s.getInputStream());
            String v = header.findValue ("User-Agent");
            if (!expected.equals (v)) {
                error ("Got unexpected User-Agent: " + v);
            } else {
                success ();
            }
            OutputStream w = s.getOutputStream();
            w.write("HTTP/1.1 200 OK\r\n".getBytes());
            w.write("Content-Type: text/plain\r\n".getBytes());
            w.write("Content-Length: 5\r\n".getBytes());
            w.write("\r\n".getBytes());
            w.write("12345\r\n".getBytes());
        } catch (Exception e) {
            error (e.toString());
        }
    }

    String msg;
    ServerSocket server;
    boolean success;

    synchronized String getMessage () {
        return msg;
    }

    synchronized boolean succeeded () {
        return success;
    }

    synchronized void success () {
        success = true;
    }
    synchronized void error (String s) {
        success = false;
        msg = s;
    }
}

public class UserAgent {

    public static void main(String[] args) throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        ServerSocket server = new ServerSocket ();
        server.bind(new InetSocketAddress(loopback, 0));
        Server s = new Server (server);
        s.start ();
        int port = server.getLocalPort ();
        URL url = URIBuilder.newBuilder()
            .scheme("http")
            .loopback()
            .port(port)
            .toURL();
        System.out.println("URL: " + url);
        URLConnection urlc = url.openConnection (Proxy.NO_PROXY);
        urlc.getInputStream ();
        s.join ();
        if (!s.succeeded()) {
            throw new RuntimeException (s.getMessage());
        }
    }
}
