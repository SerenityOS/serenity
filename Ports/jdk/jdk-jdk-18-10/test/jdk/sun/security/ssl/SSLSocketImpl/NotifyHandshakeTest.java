/*
 * Copyright (c) 2002, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * This class may have some race conditions that I haven't
 * accounted for.  I've tried to put in sufficient sleeps and triggers
 * that should cause everything to run correctly.
 *
 * This was hackish, but to make sure that there were no problems
 * with permissions.
 *
 * Create a client, server, and interested party thread.  The
 * client and interested threads should receive the same
 * session notification.
 */
package com;

import java.net.*;
import java.io.*;
import javax.net.ssl.*;
import java.security.cert.*;
import java.security.*;

public class NotifyHandshakeTest implements HandshakeCompletedListener {

    static String pathToStores = "../../../../javax/net/ssl/etc";
    static String keyStoreFile = "keystore";
    static String trustStoreFile = "truststore";
    static String passwd = "passphrase";
    volatile static int serverPort = 0;

    public boolean set;
    SSLSession sess;

    static public int triggerState = 0;

    static public void trigger() {
        triggerState++;
    }

    public void handshakeCompleted(HandshakeCompletedEvent event) {
        set = true;
        sess = event.getSession();
        trigger();
    }

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

        SSLSocketFactory sslsf =
                (SSLSocketFactory)SSLSocketFactory.getDefault();
        SSLServerSocketFactory sslssf =
                (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();

        /*
         * Start off the Server, give time to initialize.
         */
        SSLServerSocket sslss =
            (SSLServerSocket)sslssf.createServerSocket(serverPort);
        sslss.setSoTimeout(30000);  // 30 seconds
        serverPort = sslss.getLocalPort();
        Server server = new Server(sslss);
        server.start();

        System.out.println("Server started...");

        /*
         * Create the socket.
         */
        SSLSocket socket =
            (SSLSocket)sslsf.createSocket("localhost", serverPort);

        /*
         * Create a second thread also interested in this socket
         */
        edu.NotifyHandshakeTestHeyYou heyYou =
            new edu.NotifyHandshakeTestHeyYou(socket);
        heyYou.start();
        while (triggerState < 1) {
            Thread.sleep(500);
        }
        System.out.println("HeyYou thread ready...");

        NotifyHandshakeTest listener = new NotifyHandshakeTest();
        socket.addHandshakeCompletedListener(listener);

        System.out.println("Client starting handshake...");
        socket.startHandshake();
        System.out.println("Client done handshaking...");

        InputStream is = socket.getInputStream();
        if ((byte)is.read() != (byte)0x77) {
            throw new Exception("problem reading byte");
        }

        /*
         * Wait for HeyYou and the client to get a slice, so
         * they can receive their SSLSessions.
         */
        while (triggerState < 3) {
            Thread.sleep(500);
        }

        /*
         * Grab the variables before reaping the thread.
         */
        boolean heyYouSet = heyYou.set;
        AccessControlContext heyYouACC = heyYou.acc;
        SSLSession  heyYouSess = heyYou.ssls;

        heyYou.interrupt();
        heyYou.join();
        server.join();

        socket.close();

        if (!heyYouSet) {
            throw new Exception("HeyYou's wasn't set");
        }
        if (!listener.set) {
            throw new Exception("This' wasn't set");
        }

        if (heyYouACC.equals(AccessController.getContext())) {
            throw new Exception("Access Control Contexts were the same");
        }

        if (!heyYouSess.equals(listener.sess)) {
            throw new Exception("SSLSessions were not equal");
        }

        System.out.println("Everything Passed");
    }

    static class Server extends Thread {

        SSLServerSocket ss;

        Server(SSLServerSocket ss) {
            this.ss = ss;
        }

        public void run() {
            try {
                System.out.println("Server accepting socket...");
                SSLSocket s = (SSLSocket) ss.accept();
                System.out.println(
                    "Server accepted socket...starting handshake");
                s.startHandshake();
                System.out.println("Server done handshaking");
                OutputStream os = s.getOutputStream();
                os.write(0x77);
                os.flush();
                System.out.println("Server returning");
            } catch (Exception e) {
                System.out.println("Server died");
                e.printStackTrace();
            }
        }
    }
}
