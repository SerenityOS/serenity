/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

//
// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.
//

/*
 * @test
 * @bug 6447412
 * @summary Issue with socket.close() for ssl sockets when poweroff on
 *          other system
 * @run main/othervm AsyncSSLSocketClose
 */

import javax.net.ssl.*;
import java.io.*;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AsyncSSLSocketClose implements Runnable {
    SSLSocket socket;
    SSLServerSocket ss;

    // Is the socket ready to close?
    private final CountDownLatch closeCondition = new CountDownLatch(1);

    // Where do we find the keystores?
    static String pathToStores = "../../../../javax/net/ssl/etc";
    static String keyStoreFile = "keystore";
    static String trustStoreFile = "truststore";
    static String passwd = "passphrase";

    public static void main(String[] args) throws Exception {
        String keyFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + keyStoreFile;
        String trustFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + trustStoreFile;

        System.setProperty("javax.net.ssl.keyStore", keyFilename);
        System.setProperty("javax.net.ssl.keyStorePassword", passwd);
        System.setProperty("javax.net.ssl.trustStore", trustFilename);
        System.setProperty("javax.net.ssl.trustStorePassword", passwd);

        new AsyncSSLSocketClose();
    }

    public AsyncSSLSocketClose() throws Exception {
        SSLServerSocketFactory sslssf =
                (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
        ss = (SSLServerSocket) sslssf.createServerSocket(0);

        SSLSocketFactory sslsf =
            (SSLSocketFactory)SSLSocketFactory.getDefault();
        socket = (SSLSocket)sslsf.createSocket("localhost", ss.getLocalPort());
        SSLSocket serverSoc = (SSLSocket)ss.accept();
        ss.close();

        (new Thread(this)).start();
        serverSoc.startHandshake();

        boolean closeIsReady = closeCondition.await(90L, TimeUnit.SECONDS);
        if (!closeIsReady) {
            System.out.println(
                    "Ignore, the closure is not ready yet in 90 seconds.");
            return;
        }

        socket.setSoLinger(true, 10);
        System.out.println("Calling Socket.close");
        socket.close();
        System.out.println("ssl socket get closed");
        System.out.flush();
    }

    // block in write
    public void run() {
        byte[] ba = new byte[1024];
        for (int i = 0; i < ba.length; i++) {
            ba[i] = 0x7A;
        }

        try {
            OutputStream os = socket.getOutputStream();
            int count = 0;

            // 1st round write
            count += ba.length;
            System.out.println(count + " bytes to be written");
            os.write(ba);
            System.out.println(count + " bytes written");

            // Signal, ready to close.
            closeCondition.countDown();

            // write more
            while (true) {
                count += ba.length;
                System.out.println(count + " bytes to be written");
                os.write(ba);
                System.out.println(count + " bytes written");
            }
        } catch (SocketException se) {
            // the closing may be in progress
            System.out.println("interrupted? " + se);
        } catch (Exception e) {
            if (socket.isClosed() || socket.isOutputShutdown()) {
                System.out.println("interrupted, the socket is closed");
            } else {
                throw new RuntimeException("interrupted?", e);
            }
        }
    }
}

