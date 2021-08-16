/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8237858
 * @summary SocketImpl.socketAccept() handles EINTR incorrectly
 * @requires (os.family != "windows")
 * @compile NativeThread.java
 * @run main/othervm/native SocketReadInterruptTest 2000 3000
 * @run main/othervm/native SocketReadInterruptTest 2000 0
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;

import java.net.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SocketReadInterruptTest {

    public static void main(String[] args) throws Exception {
        System.loadLibrary("NativeThread");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        InetAddress loopback = InetAddress.getLoopbackAddress();

        try ( ServerSocket ss = new ServerSocket(0, 50, loopback);
            Socket s1 = new Socket(loopback, ss.getLocalPort());) {
            Server server = new Server(ss, Integer.parseInt(args[0]));
            Future<Result> f1 = executor.submit(server);

            Client client = new Client(s1, Integer.parseInt(args[1]));
            Future<Result> f2 = executor.submit(client);
            long threadId = client.getID();

            sleep(200);
            System.out.println("Sending SIGPIPE to client thread.");
            if (NativeThread.signal(threadId, NativeThread.SIGPIPE) != 0) {
                throw new RuntimeException("Failed to interrupt the thread.");
            }

            Result r1 = f1.get();
            if (r1.status == Result.FAIL) {
                throw r1.exception;
            }

            Result r2 = f2.get();
            if (r2.status == Result.FAIL) {
                throw r2.exception;
            }
            System.out.println("OK!");
        } finally {
            executor.shutdown();
        }
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static class Client implements Callable<Result> {

        private volatile long threadId;
        private final Socket client;
        private final int timeout;

        public Client(Socket s, int timeout) {
            client = s;
            this.timeout = timeout;
        }

        @Override
        public Result call() {
            threadId = NativeThread.getID();
            byte[] arr = new byte[64];
            try ( InputStream in = client.getInputStream();) {
                client.setSoTimeout(timeout);
                in.read(arr);
                return new Result(Result.SUCCESS, null);
            } catch (IOException ex) {
                close();
                return new Result(Result.FAIL, ex);
            }
        }

        long getID() {
            while (threadId == 0) {
                sleep(5);
            }
            return threadId;
        }

        void close() {
            if (!client.isClosed()) {
                try {
                    client.close();
                } catch (IOException ex) {
                    // ignore the exception.
                }
            }
        }
    }

    static class Server implements Callable<Result> {

        private final ServerSocket serverSocket;
        private final int timeout;

        public Server(ServerSocket ss, int timeout) {
            serverSocket = ss;
            this.timeout = timeout;
        }

        @Override
        public Result call() {
            try {
                try ( Socket client = serverSocket.accept();  OutputStream outputStream = client.getOutputStream();) {
                    sleep(timeout);
                    outputStream.write("This is just a test string.".getBytes());
                    return new Result(Result.SUCCESS, null);
                }
            } catch (IOException e) {
                close();
                return new Result(Result.FAIL, e);
            }
        }

        public void close() {
            if (!serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    static class Result {

        static final int SUCCESS = 0;
        static final int FAIL = 1;
        final int status;
        final Exception exception;

        public Result(int status, Exception ex) {
            this.status = status;
            exception = ex;
        }
    }
}
