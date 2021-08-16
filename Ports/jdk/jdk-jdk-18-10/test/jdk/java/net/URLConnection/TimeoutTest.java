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

/*
 * @test
 * @bug 4389976
 * @library /test/lib
 * @summary    can't unblock read() of InputStream from URL connection
 * @run main/timeout=40/othervm -Dsun.net.client.defaultReadTimeout=2000 TimeoutTest
 */

import java.io.*;
import java.net.*;
import jdk.test.lib.net.URIBuilder;

public class TimeoutTest {

    class Server extends Thread {
        ServerSocket server;
        Server (ServerSocket server) {
            super ();
            this.server = server;
        }
        public void run () {
            try {
                Socket s = server.accept ();
                while (!finished ()) {
                    Thread.sleep (1000);
                }
                s.close();
            } catch (Exception e) {
            }
        }
        boolean isFinished = false;

        synchronized boolean finished () {
            return (isFinished);
        }
        synchronized void done () {
            isFinished = true;
        }
    }

    public static void main(String[] args) throws Exception {
        TimeoutTest t = new TimeoutTest ();
        t.test ();
    }

    public void test() throws Exception {
        ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        Server s = new Server (ss);
        try{
            URL url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(ss.getLocalPort())
                .toURL();
            System.out.println("URL: " + url);
            URLConnection urlc = url.openConnection ();
            InputStream is = urlc.getInputStream ();
            throw new RuntimeException("Should have received timeout");
        } catch (SocketTimeoutException e) {
            return;
        } finally {
            s.done();
            ss.close();
        }
    }
}
