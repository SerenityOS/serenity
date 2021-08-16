/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.net.Socket;
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

/**
 * Helper class to create instances of DummySecureWebSocketServer which
 * can support both plain and secure connections.
 * The caller should invoke DummySecureWebSocketServer::secure before
 * DummySecureWebSocketServer::open in order to enable secure connection.
 * When secure, the DummySecureWebSocketServer currently only support using the
 * default SSLEngine through the default SSLSocketServerFacrtory.
 */
public class SecureSupport {

    private SecureSupport() { }

    public static DummySecureWebSocketServer serverWithCannedData(int... data) {
        return serverWithCannedDataAndAuthentication(null, null, data);
    }

    public static DummySecureWebSocketServer serverWithCannedDataAndAuthentication(
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

    public static DummySecureWebSocketServer serverWithCannedData(byte... data) {
       return serverWithCannedDataAndAuthentication(null, null, data);
    }

    public static DummySecureWebSocketServer serverWithCannedDataAndAuthentication(
            String username,
            String password,
            byte... data)
    {
        byte[] copy = Arrays.copyOf(data, data.length);
        return new DummySecureWebSocketServer(username, password) {
            @Override
            protected void write(WebSocketChannel ch) throws IOException {
                int off = 0; int n = 1; // 1 byte at a time
                while (off + n < copy.length + n) {
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
    public static DummySecureWebSocketServer notReadingServer() {
        return new DummySecureWebSocketServer() {
            @Override
            protected void read(WebSocketChannel ch) throws IOException {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
            }
        };
    }

    public static DummySecureWebSocketServer writingServer(int... data) {
        byte[] copy = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            copy[i] = (byte) data[i];
        }
        return new DummySecureWebSocketServer() {

            @Override
            protected void read(WebSocketChannel ch) throws IOException {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
            }

            @Override
            protected void write(WebSocketChannel ch) throws IOException {
                int off = 0; int n = 1; // 1 byte at a time
                while (off + n < copy.length + n) {
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
