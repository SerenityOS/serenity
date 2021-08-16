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
 * @bug 4680160
 * @summary The deprecated Thread.stop exposes un-checked JNI calls
 *          that result in crashes when NULL is passed into subsequent
 *          JNI calls.
 */

import java.net.*;
import java.io.IOException;

public class ThreadStop {

    static class Server implements Runnable {

        ServerSocket ss;

        Server() throws IOException {
            ss = new ServerSocket();
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        }

        public int localPort() {
            return ss.getLocalPort();
        }


        public void run() {
            try {
                Socket s = ss.accept();
            } catch (IOException ioe) {
            } catch (ThreadDeath x) {
            } finally {
                try {
                    ss.close();
                } catch (IOException x) { }
            }
        }
    }

    public static void main(String args[]) throws Exception {

        // start a server
        Server svr = new Server();
        Thread thr = new Thread(svr);
        thr.start();

        // give server time to block in ServerSocket.accept()
        Thread.sleep(2000);

        // "stop" the thread
        thr.stop();

        // give thread time to stop
        Thread.sleep(2000);

        // it's platform specific if Thread.stop interrupts the
        // thread - on Linux/Windows most likely that thread is
        // still in accept() so we connect to server which causes
        // it to unblock and do JNI-stuff with a pending exception

        try (Socket s = new Socket(svr.ss.getInetAddress(), svr.localPort())) {
        } catch (IOException ioe) {
        }
        thr.join();
    }

}
