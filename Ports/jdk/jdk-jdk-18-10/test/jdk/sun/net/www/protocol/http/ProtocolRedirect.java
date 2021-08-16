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
 * @bug 4620571
 * @summary  urlconnection following redirect uses protocol of original request
 */
import java.io.*;
import java.net.*;

public class ProtocolRedirect {
    public static void main(String [] args) throws Exception {
        try (Redirect server = new Redirect(new ServerSocket())) {
            ServerSocket ss = server.ssock;
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            new Thread(server).start();

            URL url = new URL("http", ss.getInetAddress().getHostAddress(), ss.getLocalPort(), "/");
            String page = url.toString();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            if (conn.getResponseCode() != 302) {
                System.err.println("Bad response code received from: " + page);
                throw new RuntimeException("Test failed. Should get RespCode: 302. Got:" + conn.getResponseCode());
            }
            System.out.println("Received expected response code from: " + page);
        }
    }
}

class Redirect implements Runnable, Closeable {
    final ServerSocket ssock;
    volatile boolean stopped;
    Redirect(ServerSocket ss) {
        ssock = ss;
    }

    // Send a header redirect to the peer telling it to go to the
    // https server on the host it sent the connection request to.
    private void sendReply() throws IOException {
        OutputStream out = sock.getOutputStream();
        StringBuffer reply = new StringBuffer();
        reply.append("HTTP/1.0 302 Found\r\n"
                     + "Location: https://" + sock.getLocalAddress().getHostAddress()
                     + "/\r\n\r\n");
        out.write(reply.toString().getBytes());
    }

    volatile Socket sock;
    public void run() {
        try {
            Socket s = sock = ssock.accept();
            s.setTcpNoDelay(true);
            sendReply();
            s.shutdownOutput();
        } catch(Throwable t) {
            if (!stopped) {
                t.printStackTrace();
                throw new RuntimeException(String.valueOf(t), t);
            }
        }
    }

    public void close() {
        Socket s = sock;
        boolean done = stopped;
        if (done) return;
        stopped = true;
        try {
            if (s != null) s.close();
        } catch (Throwable x) {
        } finally {
            try { ssock.close(); } catch (Throwable x) {}
        }
    }

}
