/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6469663
 * @summary HTTP Request-URI contains fragment when connecting through proxy
 * @modules java.base/sun.net.www
 * @run main/othervm RequestURI
 */

import java.net.*;
import java.io.*;
import sun.net.www.MessageHeader;

// Create a Server listening on port 5001 to act as the proxy. Requests
// never need to be forwared from it. We are only interested in the
// request being sent to it. Set the system proxy properties to the
// value of the RequestURIServer so that the HTTP request will to sent to it.

public class RequestURI
{
    public static void main(String[] args) {
        ServerSocket ss;
        int port;

        try {
            ss = new ServerSocket(5001);
            port = ss.getLocalPort();
        } catch (Exception e) {
            System.out.println ("Exception: " + e);
            return;
        }

        RequestURIServer server = new RequestURIServer(ss);
        server.start();

        try {
            System.getProperties().setProperty("http.proxyHost", "localhost");
            System.getProperties().setProperty("http.proxyPort", Integer.toString(port));

            URL url = new URL("http://boo.bar.com/foo.html#section5");
            HttpURLConnection uc = (HttpURLConnection) url.openConnection();

            int resp = uc.getResponseCode();
            if (resp != 200)
                throw new RuntimeException("Failed: Fragment is being passed as part of the RequestURI");

            ss.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class RequestURIServer extends Thread
{
    ServerSocket ss;

    String replyOK =  "HTTP/1.1 200 OK\r\n" +
                      "Content-Length: 0\r\n\r\n";
    String replyFAILED = "HTTP/1.1 404 Not Found\r\n\r\n";

    public RequestURIServer(ServerSocket ss) {
        this.ss = ss;
    }

    public void run() {
        try {
            Socket sock = ss.accept();
            InputStream is = sock.getInputStream();
            OutputStream os = sock.getOutputStream();

            MessageHeader headers =  new MessageHeader (is);
            String requestLine = headers.getValue(0);

            int first  = requestLine.indexOf(' ');
            int second  = requestLine.lastIndexOf(' ');
            String URIString = requestLine.substring(first+1, second);

            URI requestURI = new URI(URIString);
            if (requestURI.getFragment() != null)
                os.write(replyFAILED.getBytes("UTF-8"));
            else
                os.write(replyOK.getBytes("UTF-8"));

            sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
