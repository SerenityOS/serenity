/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6393710
 * @library /test/lib
 * @summary  Non authenticated call followed by authenticated call never returns
 * @run main B6393710
 * @run main/othervm -Djava.net.preferIPv6Addresses=true B6393710
 */

import com.sun.net.httpserver.*;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;

import jdk.test.lib.Utils;

/*
 * Test checks for following bug(s) when a POST containing a request body
 * needs to be authenticated
 *
 * 1) we were not reading the request body
 *
 * 2) we were not re-enabling the interestops for the socket channel
 */

public class B6393710 {

    static String CRLF = "\r\n";

    /* Two post requests containing data. The second one
     * has the expected authorization credentials
     */
    static String cmd =
        "POST /test/foo HTTP/1.1"+CRLF+
        "Content-Length: 22"+CRLF+
        "Pragma: no-cache"+CRLF+
        "Cache-Control: no-cache"+CRLF+ CRLF+
        "<item desc=\"excuse\" />"+
        "POST /test/foo HTTP/1.1"+CRLF+
        "Content-Length: 22"+CRLF+
        "Pragma: no-cache"+CRLF+
        "Authorization: Basic ZnJlZDpmcmVkcGFzc3dvcmQ="+CRLF+
        "Cache-Control: no-cache"+CRLF+ CRLF+
        "<item desc=\"excuse\" />";

    public static void main (String[] args) throws Exception {
        Handler handler = new Handler();
        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetSocketAddress addr = new InetSocketAddress (loopback, 0);
        HttpServer server = HttpServer.create (addr, 0);
        HttpContext ctx = server.createContext ("/test", handler);
        ctx.setAuthenticator (new BasicAuthenticator ("test") {
            public boolean checkCredentials (String user, String pass) {
                return user.equals ("fred") && pass.equals("fredpassword");
            }
        });

        server.start ();

        Socket s = new Socket (loopback, server.getAddress().getPort());
        s.setSoTimeout ((int) Utils.adjustTimeout(5000));

        OutputStream os = s.getOutputStream();
        os.write (cmd.getBytes());
        InputStream is = s.getInputStream ();
        try {
            ok = readAndCheck (is, "401 Unauthorized") &&
                 readAndCheck (is, "200 OK");
        } catch (SocketTimeoutException e) {
            System.out.println ("Did not received expected data");
            ok = false;
        } finally {
            s.close();
            server.stop(2);
        }

        if (requests != 1) {
            throw new RuntimeException ("server handler did not receive the request");
        }
        if (!ok) {
            throw new RuntimeException ("did not get 200 OK");
        }
        System.out.println ("OK");
    }

    /* check for expected string and return true if found in stream */

    static boolean readAndCheck (InputStream is, String expected) throws IOException {
        int c;
        int count = 0;
        int expLen = expected.length();
        expected = expected.toLowerCase();

        while ((c=is.read()) != -1) {
            c = Character.toLowerCase (c);
            if (c == expected.charAt (count)) {
                count ++;
                if (count == expLen) {
                    return true;
                }
            } else {
                count = 0;
            }
        }
        return false;
    }

    public static volatile boolean ok = false;
    static volatile int requests = 0;

    static class Handler implements HttpHandler {
        int invocation = 1;
        public void handle (HttpExchange t)
            throws IOException
        {
            int count = 0;
            InputStream is = t.getRequestBody();
            Headers map = t.getRequestHeaders();
            Headers rmap = t.getResponseHeaders();
            while (is.read () != -1) {
                count ++;
            }
            if (count != 22) {
                System.out.println ("Handler expected 22. got " + count);
                ok = false;
            }
            is.close();
            t.sendResponseHeaders (200, -1);
            t.close();
            requests ++;
        }
    }
}
