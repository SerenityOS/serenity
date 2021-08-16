/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4726087
 * @modules java.base/sun.net.www
 * @library ../../httptest/
 * @build HttpCallback TestHttpServer ClosedChannelList HttpTransaction
 * @run main RelativeRedirect
 * @run main/othervm -Djava.net.preferIPv6Addresses=true RelativeRedirect
 * @summary URLConnection cannot handle redirects
 */

import java.io.*;
import java.net.*;

public class RelativeRedirect implements HttpCallback {
    static int count = 0;
    static TestHttpServer server;

    static class MyAuthenticator extends Authenticator {
        public MyAuthenticator () {
            super ();
        }

        public PasswordAuthentication getPasswordAuthentication ()
        {
            return (new PasswordAuthentication ("user", "Wrongpassword".toCharArray()));
        }
    }

    void firstReply (HttpTransaction req) throws IOException {
        req.addResponseHeader ("Connection", "close");
        req.addResponseHeader ("Location", "/redirect/file.html");
        req.sendResponse (302, "Moved Permamently");
        req.orderlyClose();
    }

    void secondReply (HttpTransaction req) throws IOException {
        if (req.getRequestURI().toString().equals("/redirect/file.html") &&
            req.getRequestHeader("Host").equals(authority(server.getLocalPort()))) {
            req.setResponseEntityBody ("Hello .");
            req.sendResponse (200, "Ok");
        } else {
            req.setResponseEntityBody (req.getRequestURI().toString());
            req.sendResponse (400, "Bad request");
        }
        req.orderlyClose();

    }
    public void request (HttpTransaction req) {
        try {
            switch (count) {
            case 0:
                // server redirect to /redirect/file.html
                firstReply (req);
                break;
            case 1:
                // client retry to /redirect/file.html on same server
                secondReply (req);
                break;
            }
            count ++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

   static String authority(int port) {
       InetAddress loopback = InetAddress.getLoopbackAddress();
       String hostaddr = loopback.getHostAddress();
       if (hostaddr.indexOf(':') > -1) {
           hostaddr = "[" + hostaddr + "]";
       }
       return hostaddr + ":" + port;
   }

    public static void main (String[] args) throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        MyAuthenticator auth = new MyAuthenticator ();
        Authenticator.setDefault (auth);
        try {
            server = new TestHttpServer (new RelativeRedirect(), 1, 10, loopback, 0);
            System.out.println ("Server: listening on port: " + server.getLocalPort());
            URL url = new URL("http://" + authority(server.getLocalPort()));
            System.out.println ("client opening connection to: " + url);
            HttpURLConnection urlc = (HttpURLConnection)url.openConnection (Proxy.NO_PROXY);
            InputStream is = urlc.getInputStream ();
            is.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (server != null) {
                server.terminate();
            }
        }
    }
}
