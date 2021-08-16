/*
 * Copyright (c) 2021, Amazon and/or its affiliates. All rights reserved.
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
 * @bug 8214339 8259662
 * @summary When a SocketException is thrown by the underlying layer, It
 *      should be thrown as is and not be transformed to an SSLException.
 * @library /javax/net/ssl/templates
 * @run main/othervm SSLSocketShouldThrowSocketException
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;
import javax.net.ssl.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SSLSocketShouldThrowSocketException extends SSLSocketTemplate {

    boolean handshake;

    private final CountDownLatch clientTerminatedCondition = new CountDownLatch(1);

    SSLSocketShouldThrowSocketException(boolean handshake) {
        this.handshake = handshake;
    }

    @Override
    protected boolean isCustomizedClientConnection() {
        return true;
    }

    @Override
    protected void runServerApplication(SSLSocket socket) throws Exception {
        clientTerminatedCondition.await(30L, TimeUnit.SECONDS);
    }

    @Override
    protected void runClientApplication(int serverPort) throws Exception {
        Socket baseSocket = new Socket("localhost", serverPort);

        SSLSocketFactory sslsf =
                (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket)
                sslsf.createSocket(baseSocket, "localhost", serverPort, false);

        if (this.handshake) {
            testHandshakeClose(baseSocket, sslSocket);
        } else {
            testDataClose(baseSocket, sslSocket);
        }

        clientTerminatedCondition.countDown();

    }

    private void testHandshakeClose(Socket baseSocket, SSLSocket sslSocket) throws Exception {
        Thread aborter = new Thread() {
            @Override
            public void run() {

                try {
                    Thread.sleep(10);
                    System.err.println("Closing the client socket : " + System.nanoTime());
                    baseSocket.close();
                } catch (Exception ieo) {
                    ieo.printStackTrace();
                }
            }
        };

        aborter.start();

        try {
            // handshaking
            System.err.println("Client starting handshake: " + System.nanoTime());
            sslSocket.startHandshake();
            throw new Exception("Start handshake did not throw an exception");
        } catch (SocketException se) {
            System.err.println("Caught Expected SocketException");
        }

        aborter.join();
    }

    private void testDataClose(Socket baseSocket, SSLSocket sslSocket) throws Exception{

        CountDownLatch handshakeCondition = new CountDownLatch(1);

        Thread aborter = new Thread() {
            @Override
            public void run() {

                try {
                    handshakeCondition.await(10L, TimeUnit.SECONDS);
                    System.err.println("Closing the client socket : " + System.nanoTime());
                    baseSocket.close();
                } catch (Exception ieo) {
                    ieo.printStackTrace();
                }
            }
        };

        aborter.start();

        try {
            // handshaking
            System.err.println("Client starting handshake: " + System.nanoTime());
            sslSocket.startHandshake();
            handshakeCondition.countDown();
            System.err.println("Reading data from server");
            BufferedReader is = new BufferedReader(
                    new InputStreamReader(sslSocket.getInputStream()));
            String data = is.readLine();
            throw new Exception("Start handshake did not throw an exception");
        } catch (SocketException se) {
            System.err.println("Caught Expected SocketException");
        }

        aborter.join();
    }

    public static void main(String[] args) throws Exception {
        // SocketException should be throws during a handshake phase.
        (new SSLSocketShouldThrowSocketException(true)).run();
        // SocketException should be throw during the application data phase.
        (new SSLSocketShouldThrowSocketException(false)).run();
    }
}
