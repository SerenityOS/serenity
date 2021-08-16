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

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static org.testng.Assert.*;

/*
 * @test
 * @bug 8246707
 * @library /test/lib
 * @summary Reading or Writing to a closed SocketChannel should throw a ClosedChannelException
 * @run testng/othervm ReadWriteAfterClose
 */

public class ReadWriteAfterClose {

    private ServerSocketChannel listener;
    private SocketAddress saddr;
    private static final int bufCapacity = 4;
    private static final int bufArraySize = 4;
    private static final Class<ClosedChannelException> CCE = ClosedChannelException.class;

    @BeforeTest
    public void setUp() throws IOException {
        listener = ServerSocketChannel.open();
        listener.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        saddr = listener.getLocalAddress();
    }

    @Test
    public void testWriteAfterClose1() throws IOException {
        SocketChannel sc = SocketChannel.open(saddr);
        sc.close();
        ByteBuffer bufWrite = ByteBuffer.allocate(bufCapacity);
        Throwable ex = expectThrows(CCE, () -> sc.write(bufWrite));
        assertEquals(ex.getClass(), CCE);
    }

    @Test
    public void testWriteAfterClose2() throws IOException {
        SocketChannel sc = SocketChannel.open(saddr);
        sc.close();
        ByteBuffer[] bufArrayWrite = allocateBufArray();
        Throwable ex = expectThrows(CCE, () -> sc.write(bufArrayWrite));
        assertEquals(ex.getClass(), CCE);
    }

    @Test
    public void testWriteAfterClose3() throws IOException {
        SocketChannel sc = SocketChannel.open(saddr);
        sc.close();
        ByteBuffer[] bufArrayWrite = allocateBufArray();
        Throwable ex = expectThrows(CCE, () -> sc.write(bufArrayWrite, 0, bufArraySize));
        assertEquals(ex.getClass(), CCE);
    }

    @Test
    public void testReadAfterClose1() throws IOException {
        SocketChannel sc = SocketChannel.open(saddr);
        sc.close();
        ByteBuffer dst = ByteBuffer.allocate(bufCapacity);
        Throwable ex = expectThrows(CCE, () -> sc.read(dst));
        assertEquals(ex.getClass(), CCE);
    }

    @Test
    public void testReadAfterClose2() throws IOException {
        SocketChannel sc = SocketChannel.open(saddr);
        sc.close();
        ByteBuffer[] dstArray = allocateBufArray();
        Throwable ex = expectThrows(CCE, () -> sc.read(dstArray));
        assertEquals(ex.getClass(), CCE);
    }

    @Test
    public void testReadAfterClose3() throws IOException {
        SocketChannel sc = SocketChannel.open(saddr);
        sc.close();
        ByteBuffer[] dstArray = allocateBufArray();
        Throwable ex = expectThrows(CCE, () -> sc.read(dstArray, 0, bufArraySize));
        assertEquals(ex.getClass(), CCE);
    }

    public ByteBuffer[] allocateBufArray() {
        ByteBuffer[] bufArr = new ByteBuffer[bufArraySize];
        for (int i = 0; i < bufArraySize; i++)
            bufArr[i] = ByteBuffer.allocate(bufCapacity);
        return bufArr;
    }

    @AfterTest
    public void tearDown() throws IOException {
        listener.close();
    }
}