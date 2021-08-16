/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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
 *
 * Test for bug 4302026
 * Summary: make sure the server side doesn't do DNS lookup.
 * The client just sends a request to the server.
 */

import java.net.*;
import java.io.*;
import javax.net.ssl.*;

class GetPeerHostClient extends Thread
{
    SSLSocket s;
    String server;

    public GetPeerHostClient (int serverPort)
    {
        try {
            SSLSocketFactory factory = (SSLSocketFactory)SSLSocketFactory
                                        .getDefault();
            server = InetAddress.getLocalHost().getHostName();
            s = (SSLSocket) factory.createSocket(server, serverPort);
            System.out.println("CLIENT: connected to the server- " + server);
        } catch (Exception e) {
                System.err.println("Unexpected exceptions: " + e);
                e.printStackTrace();
          }
    }

    public void run ()
    {
        try {
            // send http request
            // before any application data gets sent or received,
            // ssl socket will do ssl handshaking first to set up
            // the security associates
            s.startHandshake(); // Asynchronous call
            PrintWriter out = new PrintWriter(
                              new BufferedWriter(
                              new OutputStreamWriter(
                              s.getOutputStream())));
            out.println("GET http://" + server +":9999/index.html HTTP/1.1");
            out.println();
            out.flush();
        } catch (Exception e) {
                System.err.println("Unexpected exceptions: " + e);
                e.printStackTrace();
                return;
          }
     }
}
