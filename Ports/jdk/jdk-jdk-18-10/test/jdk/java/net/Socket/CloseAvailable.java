/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4091859 8189366
 * @library /test/lib
 * @summary Test Socket.available()
 * @run main CloseAvailable
 * @run main/othervm -Djava.net.preferIPv4Stack=true CloseAvailable
 */

import java.net.*;
import java.io.*;
import jdk.test.lib.net.IPSupport;


public class CloseAvailable {

    public static void main(String[] args) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();

        testClose();

        testEOF(true);
        testEOF(false);
        testIOEOnClosed(true);
        testIOEOnClosed(false);
    }

    static void testClose() throws IOException {
        boolean error = true;
        InetAddress addr = InetAddress.getLocalHost();
        ServerSocket ss = new ServerSocket(0, 0, addr);
        int port = ss.getLocalPort();

        Thread t = new Thread(new Thread("Close-Available-1") {
            public void run() {
                try {
                    Socket s = new Socket(addr, port);
                    s.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        t.start();

        Socket  soc = ss.accept();
        ss.close();

        DataInputStream is = new DataInputStream(soc.getInputStream());
        is.close();

        try {
            is.available();
        }
        catch (IOException ex) {
            error = false;
        }
        if (error)
            throw new RuntimeException("Available() can be called after stream closed.");
    }

    // Verifies consistency of `available` behaviour when EOF reached, both
    // explicitly and implicitly.
    static void testEOF(boolean readUntilEOF) throws IOException {
        System.out.println("testEOF, readUntilEOF: " + readUntilEOF);
        InetAddress addr = InetAddress.getLoopbackAddress();
        ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress(addr, 0), 0);
        int port = ss.getLocalPort();

        try (Socket s = new Socket(addr, port)) {
            s.getOutputStream().write(0x42);
            s.shutdownOutput();

            try (Socket soc = ss.accept()) {
                ss.close();

                InputStream is = soc.getInputStream();
                int b = is.read();
                assert b == 0x42;
                assert !s.isClosed();
                if (readUntilEOF) {
                    b = is.read();
                    assert b == -1;
                }

                int a;
                for (int i = 0; i < 100; i++) {
                    a = is.available();
                    System.out.print(a + ", ");
                    if (a != 0)
                        throw new RuntimeException("Unexpected non-zero available: " + a);
                }
                assert !s.isClosed();
                assert is.read() == -1;
            }
        }
        System.out.println("\ncomplete");
    }

    // Verifies IOException thrown by `available`, on a closed input stream
    // that may, or may not, have reached EOF prior to closure.
    static void testIOEOnClosed(boolean readUntilEOF) throws IOException {
        System.out.println("testIOEOnClosed, readUntilEOF: " + readUntilEOF);
        InetAddress addr = InetAddress.getLoopbackAddress();
        ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress(addr, 0), 0);
        int port = ss.getLocalPort();

        try (Socket s = new Socket(addr, port)) {
            s.getOutputStream().write(0x43);
            s.shutdownOutput();

            try (Socket soc = ss.accept()) {
                ss.close();

                InputStream is = soc.getInputStream();
                int b = is.read();
                assert b == 0x43;
                assert !s.isClosed();
                if (readUntilEOF) {
                    b = is.read();
                    assert b == -1;
                }
                is.close();
                try {
                    b = is.available();
                    throw new RuntimeException("UNEXPECTED successful read: " + b);
                } catch (IOException expected) {
                    System.out.println("caught expected IOException:" + expected);
                }
            }
        }
        System.out.println("\ncomplete");
    }
}
