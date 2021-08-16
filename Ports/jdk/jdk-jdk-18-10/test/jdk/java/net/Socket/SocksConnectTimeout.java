/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6223635
 * @library /test/lib
 * @summary Code hangs at connect call even when Timeout is specified
 * @run main SocksConnectTimeout
 * @run main/othervm -Djava.net.preferIPv4Stack=true SocksConnectTimeout
 */

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.io.IOException;
import java.io.Closeable;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import jdk.test.lib.net.IPSupport;

public class SocksConnectTimeout {
    static ServerSocket serverSocket;
    static final boolean debug = true;
    static final Phaser startPhaser = new Phaser(2);
    static final Phaser finishPhaser = new Phaser(2);
    static int failed, passed;

    public static void main(String[] args) {
        IPSupport.throwSkippedExceptionIfNonOperational();

        try {
            serverSocket = new ServerSocket();
            InetAddress localHost = InetAddress.getLocalHost();
            serverSocket.bind(new InetSocketAddress(localHost, 0));

            (new Thread() {
                @Override
                public void run() { serve(); }
            }).start();

            Proxy socksProxy = new Proxy(Proxy.Type.SOCKS,
                    new InetSocketAddress(localHost, serverSocket.getLocalPort()));

            test(socksProxy);
        } catch (IOException e) {
            unexpected(e);
        } finally {
            close(serverSocket);

            if (failed > 0)
                throw new RuntimeException("Test Failed: passed:" + passed + ", failed:" + failed);
        }
    }

    static void test(Proxy proxy) {
        startPhaser.arriveAndAwaitAdvance();
        Socket socket = null;
        try {
            socket = new Socket(proxy);
            connectWithTimeout(socket);
            failed("connected successfully!");
        } catch (SocketTimeoutException socketTimeout) {
            debug("Passed: Received: " + socketTimeout);
            passed();
        } catch (Exception exception) {
            failed("Connect timeout test failed", exception);
        } finally {
            finishPhaser.arriveAndAwaitAdvance();
            close(socket);
        }
    }

    static void connectWithTimeout(Socket socket) throws IOException {
        socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), 1234), 500);
    }

    static void serve() {
        Socket client = null;
        try {
            startPhaser.arriveAndAwaitAdvance();
            client = serverSocket.accept();
            finishPhaser.awaitAdvanceInterruptibly(finishPhaser.arrive(), 5, TimeUnit.SECONDS);
        } catch (Exception e) {
            unexpected(e);
        } finally {
            close(client);
        }
    }

    static void debug(String message) {
        if (debug)
            System.out.println(message);
    }

    static void unexpected(Exception e ) {
        System.out.println("Unexcepted Exception: " + e);
    }

    static void close(Closeable closeable) {
        if (closeable != null) try { closeable.close(); } catch (IOException e) {unexpected(e);}
    }

    static void failed(String message) {
        System.out.println(message);
        failed++;
    }

    static void failed(String message, Exception e) {
        System.out.println(message);
        System.out.println(e);
        failed++;
    }

    static void passed() { passed++; };

}
