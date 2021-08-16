/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4372248
   @summary Java2D demo throws exceptions on MerlinB32
   */
import java.io.*;
import java.net.*;
class XServer extends Thread {
    ServerSocket srv;
    Socket s;
    InputStream is;
    OutputStream os;

    XServer (ServerSocket s) {
        srv = s;
    }

    Socket getSocket () {
        return (s);
    }

    public void run() {
        try {
            String x;
            String msg = "Message from the server\r\n";
            s = srv.accept ();
            is = s.getInputStream ();
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            os = s.getOutputStream ();
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os));
            while ((x=r.readLine()) != null) {
                if (x.length() == 0)
                    break;
            }
            w.write("HTTP/1.1 200\r\n");
            w.write("Content-Type: text/html\r\n");
            w.write("Content-Length: "+msg.length()+"\r\n");
            w.write("\r\n");
            w.write(msg);
            w.flush();

            // second round
            while ((x=r.readLine()) != null) {
                if (x.length() == 0)
                    break;
            }
            w.write("HTTP/1.1 200\r\n");
            w.write("Content-Type: text/html\r\n");
            w.write("Content-Length: "+msg.length()+"\r\n");
            w.write("\r\n");
            w.write(msg);
            w.flush();
            s.close ();
            srv.close ();
        } catch (IOException e) {}
    }
}

public class Finalizer {
    public static void main (String args[]) {
        ServerSocket serversocket = null;
        try {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            serversocket = new ServerSocket();
            serversocket.bind(new InetSocketAddress(loopback, 0));
            int port = serversocket.getLocalPort ();
            XServer server = new XServer (serversocket);
            server.start ();
            Thread.sleep (2000);
            URL url = new URL ("http://localhost:"+port+"/index.html");
            URLConnection urlc = url.openConnection ();
            urlc.connect ();
            InputStream is = urlc.getInputStream ();
            is = urlc.getInputStream ();
            sink (is);
            HttpURLConnection ht = (HttpURLConnection) urlc;

            URLConnection url1 = url.openConnection ();
            is = url1.getInputStream ();

            ht = null; urlc = null;
            System.gc();
            System.runFinalization ();

            sink (is);

            System.out.println("Passed!");
        } catch (IOException e) {
            throw new RuntimeException("finalize method failure."+e);
        } catch (InterruptedException ie) {
        } finally {
            if (serversocket != null) {
                try {serversocket.close();} catch (IOException io) {}
            }
        }
    }

    static void sink (InputStream is) {
        int c;
        byte [] b = new byte [1024];
        try {
            while ((c = is.read (b)) != -1);
        } catch (Exception e) {
            throw new RuntimeException("finalize method failure."+e);
        }
    }
}
