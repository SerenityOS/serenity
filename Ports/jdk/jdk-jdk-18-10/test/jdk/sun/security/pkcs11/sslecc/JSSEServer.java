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
import java.net.SocketTimeoutException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

        SSLServerSocketFactory factory = (SSLServerSocketFactory)serverContext.getServerSocketFactory();
        serverSocket = (SSLServerSocket)factory.createServerSocket(0);
        serverSocket.setSoTimeout(CipherTest.TIMEOUT);
        CipherTest.serverPort = serverSocket.getLocalPort();

        // JDK-8190492: Enable all supported protocols on server side to test SSLv3
        serverSocket.setEnabledProtocols(serverSocket.getSupportedProtocols());

        serverSocket.setEnabledCipherSuites(factory.getSupportedCipherSuites());
        serverSocket.setWantClientAuth(true);
    }

    @Override
    public void run() {
        System.out.println("JSSE Server listening on port " + CipherTest.serverPort);
        Executor exec = Executors.newFixedThreadPool
                            (CipherTest.THREADS, DaemonThreadFactory.INSTANCE);

        try {
            if (!CipherTest.clientCondition.await(CipherTest.TIMEOUT,
                    TimeUnit.MILLISECONDS)) {
                System.out.println(
                        "The client is not the expected one or timeout. "
                                + "Ignore in server side.");
                return;
            }

            while (true) {
                final SSLSocket socket = (SSLSocket)serverSocket.accept();
                socket.setSoTimeout(CipherTest.TIMEOUT);
                Runnable r = new Runnable() {
                    @Override
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
        } catch (SocketTimeoutException ste) {
            System.out.println("The server got timeout for waiting for the connection, "
                    + "so ignore the test.");
        } catch (Exception e) {
            cipherTest.setFailed();
            e.printStackTrace();
        }
    }
}
