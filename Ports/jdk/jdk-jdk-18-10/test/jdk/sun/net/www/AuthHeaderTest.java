/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4804309
 * @modules java.base/sun.net.www
 * @library ../../../sun/net/www/httptest/
 * @build HttpCallback TestHttpServer ClosedChannelList HttpTransaction
 * @run main AuthHeaderTest
 * @summary AuthHeaderTest bug
 */

import java.io.*;
import java.net.*;

public class AuthHeaderTest implements HttpCallback {

    static int count = 0;
    static String authstring;

    void errorReply (HttpTransaction req, String reply) throws IOException {
        req.addResponseHeader ("Connection", "close");
        req.addResponseHeader ("Www-authenticate", reply);
        req.sendResponse (401, "Unauthorized");
        req.orderlyClose();
    }

    void okReply (HttpTransaction req) throws IOException {
        req.setResponseEntityBody ("Hello .");
        req.sendResponse (200, "Ok");
        req.orderlyClose();
    }

    public void request (HttpTransaction req) {
        try {
            authstring = req.getRequestHeader ("Authorization");
            System.out.println (authstring);
            switch (count) {
            case 0:
                errorReply (req, "Basic realm=\"wallyworld\"");
                break;
            case 1:
                /* client stores a username/pw for wallyworld
                 */
                okReply (req);
                break;
            }
            count ++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void read (InputStream is) throws IOException {
        int c;
        System.out.println ("reading");
        while ((c=is.read()) != -1) {
            System.out.write (c);
        }
        System.out.println ("");
        System.out.println ("finished reading");
    }

    static void client (String u) throws Exception {
        URL url = new URL (u);
        System.out.println ("client opening connection to: " + u);
        URLConnection urlc = url.openConnection (Proxy.NO_PROXY);
        InputStream is = urlc.getInputStream ();
        read (is);
        is.close();
    }

    static TestHttpServer server;

    public static void main (String[] args) throws Exception {
        MyAuthenticator auth = new MyAuthenticator ();
        Authenticator.setDefault (auth);
        InetAddress loopback = InetAddress.getLoopbackAddress();
        try {
            server = new TestHttpServer (new AuthHeaderTest(), 1, 10, loopback, 0);
            System.out.println ("Server: listening on port: " + server.getAuthority());
            client ("http://" + server.getAuthority() + "/d1/foo.html");
        } catch (Exception e) {
            if (server != null) {
                server.terminate();
            }
            throw e;
        }
        int f = auth.getCount();
        if (f != 1) {
            except ("Authenticator was called "+f+" times. Should be 1");
        }
        server.terminate();
    }

    public static void except (String s) {
        server.terminate();
        throw new RuntimeException (s);
    }

    static class MyAuthenticator extends Authenticator {
        MyAuthenticator () {
            super ();
        }

        int count = 0;

        public PasswordAuthentication getPasswordAuthentication () {
            PasswordAuthentication pw;
            pw = new PasswordAuthentication ("user", "pass2".toCharArray());
            count ++;
            return pw;
        }

        public int getCount () {
            return (count);
        }
    }
}
