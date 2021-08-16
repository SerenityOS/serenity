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

/**
 * @test
 * @bug 8245194
 * @run main/othervm Shutdown
 */

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * Check that half close works
 */
public class Shutdown {

    public static void main(String args[]) throws Exception {
        if (!supported()) {
            System.out.println("Unix domain channels not supported");
            return;
        }
        runTest();
    }

    static boolean supported() {
        try {
            SocketChannel.open(StandardProtocolFamily.UNIX).close();
        } catch (UnsupportedOperationException e) {
            return false;
        } catch (Exception e) {
            return true; // continue test to see what problem is
        }
        return true;
    }

    static void assertTrue(boolean condition, String error) {
        if (!condition)
            throw new RuntimeException(error);
    }

    public static void runTest() throws IOException {
        ServerSocketChannel server = null;
        SocketChannel client = null;
        SocketChannel acceptee = null;
        UnixDomainSocketAddress usa = null;

        try {
            server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            server.bind(null);
            usa = (UnixDomainSocketAddress)server.getLocalAddress();
            System.out.println("Local address " + usa);
            client = SocketChannel.open(usa);
            acceptee = server.accept();
            ByteBuffer buf = ByteBuffer.wrap("Hello world".getBytes(StandardCharsets.ISO_8859_1));
            ByteBuffer rx = ByteBuffer.allocate(buf.capacity());
            client.write(buf);
            buf.rewind();
            while (rx.hasRemaining())
                acceptee.read(rx);

            assertTrue(Arrays.equals(buf.array(), rx.array()), "array contents not equal");

            client.shutdownOutput();
            try {
                client.write(buf);
                throw new RuntimeException("shutdown error");
            } catch (ClosedChannelException e) {
            }

            rx.clear();
            int c = acceptee.read(rx);
            assertTrue(c == -1, "read after remote shutdown");

            client.configureBlocking(false);
            c = client.read(rx);
            assertTrue(c == 0, "expected c == 0");
            client.shutdownInput();
            c = client.read(rx);
            assertTrue(c == -1, "expected c == -1");
        } finally {
            close(server);
            close(client);
            close(acceptee);
            if (usa != null)
                Files.delete(usa.getPath());
        }
        System.out.println("OK");
    }

    static void close(Closeable c) {
        try {
            if (c != null)
                c.close();
        } catch (IOException e) {}
    }
}
