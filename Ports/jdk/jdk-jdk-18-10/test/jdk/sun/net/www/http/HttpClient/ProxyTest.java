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

/*
 * @test
 * @bug 4720715
 * @summary FTP with user and password doesn't work through proxy
 */

import java.io.*;
import java.net.*;
import java.util.regex.*;


/*
 * The goal here is to simulate a simplified (a lot) HTTP proxy server to see
 * what kind of URL is passed down the line by the URLConnection.
 * In particular, we want to make sure no information is lost (like username
 * and password).
 */

public class ProxyTest {

    /*
     * Proxy server as an innerclass. Has to run in a separate thread
     */
    private class HttpProxyServer extends Thread {
        private ServerSocket    server;
        private int port;
        private volatile boolean done = false;
        private String askedUrl;

        /**
         * This Inner class will handle ONE client at a time.
         * That's where 99% of the protocol handling is done.
         */

        private class HttpProxyHandler extends Thread {
            BufferedReader in;
            PrintWriter out;
            Socket client;

            public HttpProxyHandler(Socket cl) {
                client = cl;
            }

            public void run() {
                boolean done = false;

                try {
                    in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    out = new PrintWriter(client.getOutputStream(), true);
                } catch (Exception ex) {
                    return;
                }
                /*
                 * Look for the actual GET request and extract the URL
                 * A regex should do the trick.
                 */
                Pattern p = Pattern.compile("^GET (.*) HTTP/1\\.1");
                while (!done) {
                    try {
                        String str = in.readLine();
                        Matcher m = p.matcher(str);
                        if (m.find())
                            askedUrl = m.group(1);
                        if ("".equals(str))
                            done = true;
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                        try {
                            out.close();
                        } catch (Exception ex2) {
                        }
                        done = true;
                    }
                }
                /*
                 * sends back a 'dummy' document for completness sake.
                 */
                out.println("HTTP/1.0 200 OK");
                out.println("Server: Squid/2.4.STABLE6");
                out.println("Mime-Version: 1.0");
                out.println("Date: Fri, 26 Jul 2002 17:56:00 GMT");
                out.println("Content-Type: text/html");
                out.println("Last-Modified: Fri, 26 Jul 2002 01:49:57 GMT");
                out.println("Age: 168");
                out.println("X-Cache: HIT from javinator");
                out.println("Proxy-Connection: close");
                out.println();
                out.println("<!doctype html public \"-//w3c//dtd html 4.0 transitional//en\">");
                out.println("<html>");
                out.println("<head>");
                out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
                out.println("<TITLE>Hoth Downloads</TITLE>");
                out.println("</head>");
                out.println("<body background=\"/images/background.gif\">");
                out.println("<center>");
                out.println("<h1>");
                out.println("<b>Hoth Downloads</b></h1></center>");
                out.println("</body>");
                out.println("</html>");
                out.flush();
                out.close();
            }
        }

        public HttpProxyServer() throws IOException {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            server = new ServerSocket();
            server.bind(new InetSocketAddress(loopback, 0));
        }

        public int getPort() {
            if (server != null)
                return server.getLocalPort();
            return 0;
        }

        public String getURL() {
            return askedUrl;
        }

        /**
         * A way to tell the server that it can stop.
         */
        synchronized public void terminate() {
            done = true;
            try { server.close(); } catch (IOException unused) {}
        }

        public void run() {
            try {
                Socket client;
                while (!done) {
                    client = server.accept();
                    (new HttpProxyHandler(client)).start();
                }
            } catch (Exception e) {
            } finally {
                try { server.close(); } catch (IOException unused) {}
            }
        }
    }

    private static boolean hasFtp() {
        try {
            return new java.net.URL("ftp://") != null;
        } catch (java.net.MalformedURLException x) {
            System.out.println("FTP not supported by this runtime.");
            return false;
        }
    }

    public static void main(String[] args) throws Exception {
        if (hasFtp())
           new ProxyTest();
    }

    public ProxyTest() throws Exception {
        BufferedReader in = null;
        String testURL = "ftp://anonymous:password@myhost.mydomain/index.html";
        HttpProxyServer server = new HttpProxyServer();
        try {
            server.start();
            int port = server.getPort();

            InetAddress loopback = InetAddress.getLoopbackAddress();
            Proxy ftpProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(loopback, port));
            URL url = new URL(testURL);
            InputStream ins = (url.openConnection(ftpProxy)).getInputStream();
            in = new BufferedReader(new InputStreamReader(ins));
            String line;
            do {
                line = in.readLine();
            } while (line != null);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.terminate();
            try { in.close(); } catch (IOException unused) {}
        }
        /*
         * If the URLs don't match, we've got a bug!
         */
        if (!testURL.equals(server.getURL())) {
            throw new RuntimeException(server.getURL() + " != " + testURL);
        }
    }

}
