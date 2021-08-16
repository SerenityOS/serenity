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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

class JSSEServer extends CipherTest.Server {

    SSLServerSocket serverSocket;

    JSSEServer(CipherTest cipherTest) throws Exception {
        super(cipherTest);
        SSLContext serverContext = SSLContext.getInstance("TLS");
        serverContext.init(
                new KeyManager[] { CipherTest.keyManager },
                new TrustManager[] { CipherTest.trustManager },
                CipherTest.secureRandom);

        SSLServerSocketFactory factory
                = (SSLServerSocketFactory) serverContext.getServerSocketFactory();
        serverSocket
                = (SSLServerSocket) factory.createServerSocket(CipherTest.serverPort);
        CipherTest.serverPort = serverSocket.getLocalPort();

        // JDK-8190492: Enable all supported protocols on server side to test SSLv3
        serverSocket.setEnabledProtocols(serverSocket.getSupportedProtocols());

        serverSocket.setEnabledCipherSuites(factory.getSupportedCipherSuites());
        serverSocket.setWantClientAuth(true);
    }

    public void run() {
        System.out.println("JSSE Server listening on port " + CipherTest.serverPort);
        Executor exec = Executors.newFixedThreadPool
                            (cipherTest.THREADS, DaemonThreadFactory.INSTANCE);
        try {
            while (true) {
                final SSLSocket socket = (SSLSocket)serverSocket.accept();
                socket.setSoTimeout(CipherTest.TIMEOUT);
                Runnable r = new Runnable() {
                    public void run() {
                        try {
                            InputStream in = socket.getInputStream();
                            OutputStream out = socket.getOutputStream();
                            handleRequest(in, out);
                            out.flush();
                            socket.close();
                            socket.getSession().invalidate();
                        } catch (IOException e) {
                            cipherTest.setFailed();
                            e.printStackTrace();
                        } finally {
                            if (socket != null) {
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    cipherTest.setFailed();
                                    System.out.println("Exception closing socket on server side:");
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                };
                exec.execute(r);
            }
        } catch (IOException e) {
            cipherTest.setFailed();
            e.printStackTrace();
            //
        }
    }

}
