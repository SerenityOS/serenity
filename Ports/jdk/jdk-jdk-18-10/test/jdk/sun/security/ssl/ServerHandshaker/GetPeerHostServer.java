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
 * The server waits for a request from the client.
 * Make sure that the SSLSession.getPeerHost()
 * returns the peer's IP address instead of the peer's
 * host name.
 */

import java.io.*;
import java.security.*;
import java.net.*;
import javax.net.*;
import javax.net.ssl.*;

class GetPeerHostServer extends Thread
{
    private String host;
    ServerSocket ss;
    boolean isHostIPAddr = false;
    int serverPort = 0;

    public GetPeerHostServer ()
    {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            KeyStore ks = KeyStore.getInstance("JKS");
            char[] passphrase = "passphrase".toCharArray();
            String testRoot = System.getProperty("test.src", ".");
            ks.load(new FileInputStream(testRoot
                        + "/../../../../javax/net/ssl/etc/keystore"),
                    passphrase);
            kmf.init(ks, passphrase);
            ctx.init(kmf.getKeyManagers(), null, null);
            ServerSocketFactory ssf = ctx.getServerSocketFactory();
            ss = ssf.createServerSocket(serverPort);
            serverPort = ss.getLocalPort();
        }catch (Exception e) {
            System.err.println("Unexpected exceptions: " + e);
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            System.out.println("SERVER: waiting for requests...");
            Socket socket = ss.accept();
            System.out.println("SERVER: got a request!");
            host = ((javax.net.ssl.SSLSocket)socket).getSession().getPeerHost();
            System.out.println("SERVER: Host IP address (not the name): "
                                + host);
        } catch (Exception e) {
            System.err.println("Unexpected exceptions: " + e);
            e.printStackTrace();
          }

        if (host != null && (host.charAt(0) > '9') ||
                            (host.charAt(0) < '0')) {
            System.out.println("Error: bug 4302026 may not be fixed.");
        } else {
             isHostIPAddr = true;
             System.out.println("Passed!");
         }
    }


    boolean getPassStatus () {
        return isHostIPAddr;
    }

    int getServerPort() {
        return serverPort;
    }
}
