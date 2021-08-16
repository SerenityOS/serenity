/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertFalse;

public class Support {

    private Support() { }

    public static void assertFails(Class<? extends Throwable> clazz,
                                    CompletionStage<?> stage) {
        Support.assertCompletesExceptionally(clazz, stage);
    }

    public static void assertNotDone(CompletableFuture<?> future) {
        assertFalse(future.isDone());
    }

    public static void assertCompletesExceptionally(Class<? extends Throwable> clazz,
                                                    CompletionStage<?> stage) {
        CompletableFuture<?> cf =
                CompletableFuture.completedFuture(null).thenCompose(x -> stage);
        assertThrows(clazz, () -> {
            try {
                cf.join();
            } catch (CompletionException e) {
                throw e.getCause();
            }
        });
    }

    public static void assertHangs(CompletionStage<?> stage) {
        Support.assertDoesNotCompleteWithin(5, TimeUnit.SECONDS, stage);
    }

    public static void assertDoesNotCompleteWithin(long timeout,
                                                   TimeUnit unit,
                                                   CompletionStage<?> stage) {
        CompletableFuture<?> cf =
                CompletableFuture.completedFuture(null).thenCompose(x -> stage);
        assertThrows(TimeoutException.class, () -> cf.get(timeout, unit));
    }

    public static ByteBuffer fullCopy(ByteBuffer src) {
        ByteBuffer copy = ByteBuffer.allocate(src.capacity());
        int p = src.position();
        int l = src.limit();
        src.clear();
        copy.put(src).position(p).limit(l);
        src.position(p).limit(l);
        return copy;
    }

    public static DummyWebSocketServer serverWithCannedData(int... data) {
        return serverWithCannedDataAndAuthentication(null, null, data);
    }

    public static DummyWebSocketServer serverWithCannedDataAndAuthentication(
            String username,
            String password,
            int... data)
    {
        byte[] copy = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            copy[i] = (byte) data[i];
        }
        return serverWithCannedDataAndAuthentication(username, password, copy);
    }

    public static DummyWebSocketServer serverWithCannedData(byte... data) {
       return serverWithCannedDataAndAuthentication(null, null, data);
    }

    public static DummyWebSocketServer serverWithCannedDataAndAuthentication(
            String username,
            String password,
            byte... data)
    {
        byte[] copy = Arrays.copyOf(data, data.length);
        return new DummyWebSocketServer(username, password) {
            @Override
            protected void write(SocketChannel ch) throws IOException {
                int off = 0; int n = 1; // 1 byte at a time
                while (off + n < copy.length + n) {
//                    try {
//                        TimeUnit.MICROSECONDS.sleep(500);
//                    } catch (InterruptedException e) {
//                        return;
//                    }
                    int len = Math.min(copy.length - off, n);
                    ByteBuffer bytes = ByteBuffer.wrap(copy, off, len);
                    off += len;
                    ch.write(bytes);
                }
                super.write(ch);
            }
        };
    }

    /*
     * This server does not read from the wire, allowing its client to fill up
     * their send buffer. Used to test scenarios with outstanding send
     * operations.
     */
    public static DummyWebSocketServer notReadingServer() {
        return new DummyWebSocketServer() {
            volatile Thread reader;
            @Override
            protected void read(SocketChannel ch) throws IOException {
                reader = Thread.currentThread();
                try {
                    System.out.println("Not reading server waiting");
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
            }

            @Override
            protected void closeChannel(SocketChannel channel) {
                try {
                    long read = drain(channel);
                    System.out.printf("Not reading server drained %s bytes%n", read);
                } catch (IOException io) {
                    System.out.println("Not reading server failed to drain channel: " + io);
                }
                super.closeChannel(channel);
            }

            @Override
            public void close() {
                super.close();
                Thread thread = reader;
                if (thread != null && thread.isAlive() && thread != Thread.currentThread()) {
                    try {
                        thread.join();
                        System.out.println("Not reading server: closed");
                    } catch (InterruptedException x) {
                        System.out.println("Not reading server: close interrupted: " + x);
                    }
                }
            }
        };
    }

    static long drain(SocketChannel channel) throws IOException {
        System.out.println("Not reading server: draining socket");
        var blocking = channel.isBlocking();
        if (blocking) channel.configureBlocking(false);
        long count = 0;
        try {
            ByteBuffer buffer = ByteBuffer.allocateDirect(8 * 1024);
            int read;
            while ((read = channel.read(buffer)) > 0) {
                count += read;
                buffer.clear();
            }
            return count;
        } finally {
            if (blocking != channel.isBlocking()) {
                channel.configureBlocking(blocking);
            }
        }
    }

    public static DummyWebSocketServer writingServer(int... data) {
        byte[] copy = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            copy[i] = (byte) data[i];
        }
        return new DummyWebSocketServer() {

            @Override
            protected void read(SocketChannel ch) throws IOException {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
            }

            @Override
            protected void write(SocketChannel ch) throws IOException {
                int off = 0; int n = 1; // 1 byte at a time
                while (off + n < copy.length + n) {
//                    try {
//                        TimeUnit.MICROSECONDS.sleep(500);
//                    } catch (InterruptedException e) {
//                        return;
//                    }
                    int len = Math.min(copy.length - off, n);
                    ByteBuffer bytes = ByteBuffer.wrap(copy, off, len);
                    off += len;
                    ch.write(bytes);
                }
                super.write(ch);
            }
        };

    }

    public static String stringWith2NBytes(int n) {
        // -- Russian Alphabet (33 characters, 2 bytes per char) --
        char[] abc = {
                0x0410, 0x0411, 0x0412, 0x0413, 0x0414, 0x0415, 0x0401, 0x0416,
                0x0417, 0x0418, 0x0419, 0x041A, 0x041B, 0x041C, 0x041D, 0x041E,
                0x041F, 0x0420, 0x0421, 0x0422, 0x0423, 0x0424, 0x0425, 0x0426,
                0x0427, 0x0428, 0x0429, 0x042A, 0x042B, 0x042C, 0x042D, 0x042E,
                0x042F,
        };
        // repeat cyclically
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0, j = 0; i < n; i++, j = (j + 1) % abc.length) {
            sb.append(abc[j]);
        }
        String s = sb.toString();
        assert s.length() == n && s.getBytes(StandardCharsets.UTF_8).length == 2 * n;
        return s;
    }

    public static String malformedString() {
        return new String(new char[]{0xDC00, 0xD800});
    }

    public static String incompleteString() {
        return new String(new char[]{0xD800});
    }

    public static String stringWithNBytes(int n) {
        char[] chars = new char[n];
        Arrays.fill(chars, 'A');
        return new String(chars);
    }
}
