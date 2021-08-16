/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4957695
 * @run main/othervm -Djava.io.tmpdir=. B4957695
 * @summary URLJarFile.retrieve does not delete tmpFile on IOException
 */

import java.io.*;
import java.net.*;

public class B4957695 {

    static Server server;

    static class Server extends Thread {
        final ServerSocket srv;
        static final byte[] requestEnd = new byte[] {'\r', '\n', '\r', '\n'};

        Server(ServerSocket s) {
            srv = s;
        }

        void readOneRequest(InputStream is) throws IOException {
            int requestEndCount = 0, r;
            while ((r = is.read()) != -1) {
                if (r == requestEnd[requestEndCount]) {
                    requestEndCount++;
                    if (requestEndCount == 4) {
                        break;
                    }
                } else {
                    requestEndCount = 0;
                }
            }
        }

        public void run() {
            try (Socket s = srv.accept()) {
                // read HTTP request from client
                readOneRequest(s.getInputStream());
                try (OutputStreamWriter ow =
                     new OutputStreamWriter((s.getOutputStream()))) {
                    FileInputStream fin = new FileInputStream(new File(
                        System.getProperty("test.src", "."), "foo1.jar"));
                    int length = fin.available();
                    byte[] b = new byte[length-10];
                    fin.read(b, 0, length-10);
                    ow.write("HTTP/1.0 200 OK\r\n");

                    // Note: The client expects length bytes.
                    ow.write("Content-Length: " + length + "\r\n");
                    ow.write("Content-Type: text/html\r\n");
                    ow.write("\r\n");

                    // Note: The (buggy) server only sends length-10 bytes.
                    ow.write(new String(b));
                    ow.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static void read (InputStream is) throws IOException {
        int c,len=0;
        while ((c=is.read()) != -1) {
            len += c;
        }
        System.out.println ("read " + len + " bytes");
    }

    public static void main (String[] args) throws Exception {
        String tmpdir = System.getProperty("java.io.tmpdir");
        String[] list1 = listTmpFiles(tmpdir);
        InetAddress localHost = InetAddress.getByName("localhost");
        InetSocketAddress address = new InetSocketAddress(localHost, 0);
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(address);
        server = new Server(serverSocket);
        server.start();
        int port = serverSocket.getLocalPort();
        System.out.println ("Server: listening on port: " + port);
        URL url = new URL ("jar:http://localhost:"+port+"!/COPYRIGHT");
        try {
            URLConnection urlc = url.openConnection ();
            InputStream is = urlc.getInputStream();
            read (is);
            is.close();
        } catch (IOException e) {
            System.out.println ("Received IOException as expected: " + e);
        } finally {
            try {serverSocket.close();} catch (IOException x) {}
        }
        String[] list2 = listTmpFiles(tmpdir);
        if (!sameList (list1, list2)) {
            throw new RuntimeException ("some jar_cache files left behind");
        }
    }

    static String[] listTmpFiles (String d) {
        File dir = new File (d);
        return dir.list (new FilenameFilter () {
            public boolean accept (File dr, String name) {
                return (name.startsWith ("jar_cache"));
            }
        });
    }

    static boolean sameList (String[] list1, String[] list2) {
        if (list1.length != list2.length) {
            return false;
        }
        for (int i=0; i<list1.length; i++) {
            String s1 = list1[i];
            String s2 = list2[i];
            if ((s1 == null && s2 != null)) {
                return false;
            } else if ((s2 == null && s1 != null)) {
                return false;
            } else if (s1 == null) {
                return true;
            } else if (!s1.equals(s2)) {
                return false;
            }
        }
        return true;
    }
}
