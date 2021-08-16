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
 * @run main/othervm/native SocketAcceptInterruptTest 0
 * @run main/othervm/native SocketAcceptInterruptTest 5000
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

public class SocketAcceptInterruptTest {

    public static void main(String[] args) throws Exception {
        System.loadLibrary("NativeThread");
        InetAddress loopback = InetAddress.getLoopbackAddress();
        ExecutorService executor = Executors.newFixedThreadPool(1);

        try ( ServerSocket ss = new ServerSocket(0, 50, loopback);) {
            Server server = new Server(ss, Integer.parseInt(args[0]));
            Future<Result> future = executor.submit(server);
            long threadId = server.getID();

            sendSignal(threadId, ss);
            sleep(100);
            // In failing case server socket will be closed, so we do need to check first
            if (!ss.isClosed()) {
                // After sending SIGPIPE, create client socket and connect to server
                try ( Socket s = new Socket(loopback, ss.getLocalPort());  InputStream in = s.getInputStream();) {
                    in.read(); // reading one byte is enought for test.
                }
            }
            Result result = future.get();
            if (result.status == Result.FAIL) {
                throw result.exception;
            }
        } finally {
            executor.shutdown();
        }
        System.out.println("OK!");
    }

    private static void sendSignal(long threadId, ServerSocket ss) {
        System.out.println("Sending SIGPIPE to ServerSocket thread.");
        int count = 0;
        while (!ss.isClosed() && count++ < 20) {
            sleep(10);
            if (NativeThread.signal(threadId, NativeThread.SIGPIPE) != 0) {
                throw new RuntimeException("Failed to interrupt the server thread.");
            }
        }
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // ignore the exception.
        }
    }

    static class Server implements Callable<Result> {

        private volatile long threadId;
        private final ServerSocket serverSocket;
        private final int timeout;

        public Server(ServerSocket ss, int timeout) {
            serverSocket = ss;
            this.timeout = timeout;
        }

        @Override
        public Result call() {
            try {
                threadId = NativeThread.getID();
                serverSocket.setSoTimeout(timeout);
                try ( Socket socket = serverSocket.accept();
                    OutputStream outputStream = socket.getOutputStream();) {
                    outputStream.write("Hello!".getBytes());
                    return new Result(Result.SUCCESS, null);
                }
            } catch (IOException e) {
                close();
                return new Result(Result.FAIL, e);
            }
        }

        long getID() {
            while (threadId == 0) {
                sleep(5);
            }
            return threadId;
        }

        private void close() {
            if (!serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException ex) {
                    // ignore the exception
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
