/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4854838
 * @summary Verify that SSL_NULL_WITH_NULL_NULL is returned as ciphersuite
 *      if the handshake fails
 * @author Andreas Sterbenz
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.*;

public class SSL_NULL {
    private static volatile Boolean result;

    public static void main(String[] args) throws Exception {
        final SSLServerSocket serverSocket = (SSLServerSocket)
            SSLServerSocketFactory.getDefault().createServerSocket(0);
        serverSocket.setEnabledCipherSuites(
            serverSocket.getSupportedCipherSuites());
        new Thread() {
            public void run() {
                try {
                    SSLSocket socket = (SSLSocket) serverSocket.accept();
                    String suite = socket.getSession().getCipherSuite();
                    if (!"SSL_NULL_WITH_NULL_NULL".equals(suite)) {
                        System.err.println(
                            "Wrong suite for failed handshake: " +
                            "got " + suite +
                            ", expected SSL_NULL_WITH_NULL_NULL");
                    } else {
                        result = Boolean.TRUE;
                        return;
                    }
                } catch (IOException e) {
                    System.err.println("Unexpected exception");
                    e.printStackTrace();
                } finally {
                    if (result == null) {
                        result = Boolean.FALSE;
                    }
                }
            }
        }.start();

        SSLSocket socket = (SSLSocket)
            SSLSocketFactory.getDefault().createSocket(
                "localhost", serverSocket.getLocalPort());
        socket.setEnabledCipherSuites(
            new String[] { "TLS_DHE_RSA_WITH_AES_128_CBC_SHA" });
        try {
            OutputStream out = socket.getOutputStream();
            out.write(0);
            out.flush();
            throw new RuntimeException("No exception received");
        } catch (SSLHandshakeException e) {
            System.out.println("Expected handshake exception: " + e);
        }

        System.out.println("client: " + socket.getSession().getCipherSuite());

        // wait for other thread to set result
        while (result == null) {
            Thread.sleep(50);
        }
        if (result.booleanValue()) {
            System.out.println("Test passed");
        } else {
            throw new Exception("Test failed");
        }
    }
}
