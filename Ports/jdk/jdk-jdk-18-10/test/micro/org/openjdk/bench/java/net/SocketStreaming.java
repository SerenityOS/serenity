/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.net;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * Micro benchmark for streaming data over a Socket.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class SocketStreaming {

    /** The bytes to write/read. */
    public static final int dataLength = 16383;
    /** setTcpNoDelay(noNagle) */
    public static final boolean noNagle = false;

    private WriterThread writerThread;
    private Socket readSocket;
    private byte[] bytes;

    @Setup
    public void prepare() throws Exception {
        bytes = new byte[dataLength];

        // Setup the writer thread
        writerThread = new WriterThread(dataLength, noNagle);
        writerThread.start();

        // Wait for a read socket
        readSocket = writerThread.waitForReadSocket();
    }

    @TearDown
    public void cleanup() throws IOException {
        // Take down the writer thread and the reader socket
        writerThread.finish();
        while (!readSocket.isClosed()) {
            readSocket.close();
        }
        readSocket = null;
    }

    @Benchmark
    public void testSocketInputStreamRead() throws InterruptedException, IOException {
        InputStream in = readSocket.getInputStream();

        // Notify the writer thread to add elements to stream
        writerThread.requestSendBytes();

        // Read these from the stream
        int bytesRead = 0;
        while (bytesRead < dataLength) {
            int lastRead = in.read(bytes);
            if (lastRead < 0) {
                throw new InternalError("Unexpectedly got " + lastRead + " bytes from the socket");
            }
            bytesRead += lastRead;
        }
    }

    /**
     * Thread used to write bytes to a socket.
     */
    private class WriterThread extends Thread {

        /** The number of bytes to write. */
        private int dataLength;
        /** setTcpNoDelay(noNagle) */
        private boolean noNagle;
        /** Lock needed to send sendBytes requests. */
        private final Object sendBytesLock = new Object();
        /** Indicates that a sendBytes has been requested. */
        private boolean sendBytesRequested;
        /** Indicates that no more sendBytes will be requested. Time to shutdown. */
        private boolean sendBytesDone;
        /** Lock needed to protect the connectPort variable. */
        private final Object connectLock = new Object();
        /** The port the read socket should connect to. */
        private int connectPort = -1;

        /**
         * Constructor.
         *
         * @param dataLength The number of bytes to write
         * @param noNagle    setTcpNoDelay(noNagle)
         */
        public WriterThread(int dataLength, boolean noNagle) {
            super("Load producer");
            this.dataLength = dataLength;
            this.noNagle = noNagle;
        }

        /** Entry point for data sending helper thread. */
        @Override
        public void run() {
            try {
                Socket writeSocket;
                ServerSocket serverSocket = new ServerSocket(0);

                /* Tell the other thread that we now know the port number.
                 * The other thread will now start to connect until the following accept() call succeeds.
                 */
                synchronized (connectLock) {
                    connectPort = serverSocket.getLocalPort();
                    connectLock.notify();
                }

                // Wait for the other thread to connect
                writeSocket = serverSocket.accept();
                writeSocket.setTcpNoDelay(noNagle);

                // No more connects so this can be closed
                serverSocket.close();
                serverSocket = null;

                OutputStream out = writeSocket.getOutputStream();

                // Iterate as long as sendBytes are issued
                while (waitForSendBytesRequest()) {
                    sendBytes(out);
                }

                // Time to shutdown
                while (!writeSocket.isClosed()) {
                    writeSocket.close();
                }
                writeSocket = null;
            } catch (Exception e) {
                System.exit(1);
            }
        }

        /**
         * Sends bytes to the output stream
         *
         * @param out The output stream
         * @throws IOException
         */
        private void sendBytes(OutputStream out) throws IOException {
            byte outBytes[] = new byte[dataLength];

            int bytesToSend = dataLength;
            int bytesSent = 0;
            while (bytesSent < bytesToSend) {
                out.write(outBytes);
                bytesSent += outBytes.length;
            }
        }

        /**
         * Waits for the readSocket and returns it when it is ready.
         *
         * @return The socket to read from
         * @throws InterruptedException
         */
        @SuppressWarnings("SleepWhileHoldingLock")
        public Socket waitForReadSocket() throws InterruptedException {
            int theConnectPort = waitForConnectPort();

            while (true) {
                try {
                    return new Socket(InetAddress.getByName(null), theConnectPort);
                } catch (IOException e) {
                    // Wait some more for the server thread to get going
                    Thread.sleep(1000);
                }
            }

        }

        /**
         * Waits for next sendBytes request
         *
         * @return <code>true</code> if it is time to sendBytes, <code>false</code> if it is time to shutdown
         * @throws InterruptedException
         */
        public boolean waitForSendBytesRequest() throws InterruptedException {
            synchronized (sendBytesLock) {
                while (!sendBytesRequested && !sendBytesDone) {
                    sendBytesLock.wait();
                }

                // Clear the flag
                sendBytesRequested = false;

                return !sendBytesDone;
            }
        }

        /** Requests a sendBytes. */
        public void requestSendBytes() {
            synchronized (sendBytesLock) {
                sendBytesRequested = true;
                sendBytesLock.notify();
            }
        }

        /** Tells the writerThread that it is time to shutdown. */
        public void finish() {
            synchronized (sendBytesLock) {
                sendBytesDone = true;
                sendBytesLock.notify();
            }
        }

        private int waitForConnectPort() throws InterruptedException {
            synchronized (connectLock) {
                while (connectPort == -1) {
                    connectLock.wait();
                }
                return connectPort;
            }
        }
    }
}
