/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @test LocalRMIServerSocketFactoryTest.java
 * @bug 6774170
 * @summary Connect to a server socket returned by the LocalRMIServerSocketFactory.
 *
 * @author Daniel Fuchs
 *
 * @run compile -XDignore.symbol.file=true -g LocalRMIServerSocketFactoryTest.java
 * @run main LocalRMIServerSocketFactoryTest
 */

import sun.management.jmxremote.LocalRMIServerSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.SynchronousQueue;

public class LocalRMIServerSocketFactoryTest {

    private static final SynchronousQueue<Exception> queue =
            new SynchronousQueue<Exception>();
    private static volatile boolean isRunning = true;

    static final class Result extends Exception {

        private Result() {
            super("SUCCESS: No exception was thrown");
        }
        static final Result SUCCESS = new Result();
    }

    private static void checkError(String message) throws Exception {

        // Wait for the server to set the error field.
        final Exception x = queue.take();

        if (x == Result.SUCCESS) {
            return;
        }


        // case of 6674166: this is very unlikely to happen, even if
        //     both 6674166 and 6774170 aren't fixed. If it happens
        //     however, it might indicate that neither defects are fixed.

        if (x instanceof NullPointerException) {
            throw new Exception(message + " - " +
                    "Congratulations! it seems you have triggered 6674166. " +
                    "Neither 6674166 nor 6774170 seem to be fixed: " + x, x);
        } else if (x instanceof IOException) {
            throw new Exception(message + " - " +
                    "Unexpected IOException. Maybe you triggered 6674166? " +
                    x, x);
        } else if (x != null) {
            throw new Exception(message + " - " +
                    "Ouch, that's bad. " +
                    "This is a new kind of unexpected exception " +
                    x, x);
        }
    }

    public static void main(String[] args) throws Exception {
        final LocalRMIServerSocketFactory f =
                new LocalRMIServerSocketFactory();
        final ServerSocket s = f.createServerSocket(0);
        final int port = s.getLocalPort();
        Thread t = new Thread() {

            public void run() {
                while (isRunning) {
                    Exception error = Result.SUCCESS;
                    try {
                        System.err.println("Accepting: ");
                        final Socket ss = s.accept();
                        System.err.println(ss.getInetAddress() + " accepted");
                    } catch (Exception x) {
                        if (isRunning) {
                            x.printStackTrace();
                        }
                        error = x;
                    } finally {
                        try {
                            if (isRunning) {
                                // wait for the client to get the exception.
                                queue.put(error);
                            }
                        } catch (Exception x) {
                            // too bad!
                            System.err.println("Could't send result to client!");
                            x.printStackTrace();
                            return;
                        }
                    }
                }
            }
        };

        try {
            t.start();

            System.err.println("new Socket((String)null, port)");
            final Socket s1 = new Socket((String) null, port);
            checkError("new Socket((String)null, port)");
            s1.close();
            System.err.println("new Socket((String)null, port): PASSED");

            System.err.println("new Socket(InetAddress.getByName(null), port)");
            final Socket s2 = new Socket(InetAddress.getByName(null), port);
            checkError("new Socket(InetAddress.getByName(null), port)");
            s2.close();
            System.err.println("new Socket(InetAddress.getByName(null), port): PASSED");

            System.err.println("new Socket(localhost, port)");
            final Socket s3 = new Socket("localhost", port);
            checkError("new Socket(localhost, port)");
            s3.close();
            System.err.println("new Socket(localhost, port): PASSED");

            System.err.println("new Socket(127.0.0.1, port)");
            final Socket s4 = new Socket("127.0.0.1", port);
            checkError("new Socket(127.0.0.1, port)");
            s4.close();
            System.err.println("new Socket(127.0.0.1, port): PASSED");
        }
        finally {
            isRunning = false;
            s.close();
            t.join();
        }
    }
}
