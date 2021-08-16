/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4865031
 * @summary Test ScatteringByteChannel/GatheringByteChannel read/write
 * @library .. /test/lib
 * @build jdk.test.lib.Utils TestServers
 * @run main VectorParams
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

public class VectorParams {

    static java.io.PrintStream out = System.out;

    static final int testSize = 10;
    static ByteBuffer[] bufs = null;
    static InetSocketAddress isa = null;

    public static void main(String[] args) throws Exception {
        try (TestServers.DayTimeServer daytimeServer
                = TestServers.DayTimeServer.startNewServer(100)) {
            initBufs(daytimeServer);
            testSocketChannelVectorParams();
            testDatagramChannelVectorParams();
            testPipeVectorParams();
            testFileVectorParams();
        }
    }

    static void initBufs(TestServers.DayTimeServer daytimeServer) throws Exception {
        bufs = new ByteBuffer[testSize];
        for(int i=0; i<testSize; i++) {
            String source = "buffer" + i;
            bufs[i] = ByteBuffer.allocate(source.length());
            bufs[i].put(source.getBytes("8859_1"));
            bufs[i].flip();
        }
        isa = new InetSocketAddress(daytimeServer.getAddress(),
                                    daytimeServer.getPort());
    }

    static void testSocketChannelVectorParams() throws Exception {
        SocketChannel sc = SocketChannel.open(isa);
        tryBadWrite(sc, bufs, 0, -1);
        tryBadWrite(sc, bufs, -1, 0);
        tryBadWrite(sc, bufs, 0, 1000);
        tryBadWrite(sc, bufs, 1000, 1);
        tryBadRead(sc, bufs, 0, -1);
        tryBadRead(sc, bufs, -1, 0);
        tryBadRead(sc, bufs, 0, 1000);
        tryBadRead(sc, bufs, 1000, 1);
        sc.close();
    }

    static void testDatagramChannelVectorParams() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        dc.connect(isa);
        tryBadRead(dc, bufs, 0, -1);
        tryBadRead(dc, bufs, -1, 0);
        tryBadRead(dc, bufs, 0, 1000);
        tryBadRead(dc, bufs, 1000, 1);
        tryBadWrite(dc, bufs, 0, -1);
        tryBadWrite(dc, bufs, -1, 0);
        tryBadWrite(dc, bufs, 0, 1000);
        tryBadWrite(dc, bufs, 1000, 1);
        dc.close();
    }

    static void testPipeVectorParams() throws Exception {
        Pipe p = Pipe.open();
        Pipe.SinkChannel sink = p.sink();
        Pipe.SourceChannel source = p.source();
        tryBadWrite(sink, bufs, 0, -1);
        tryBadWrite(sink, bufs, -1, 0);
        tryBadWrite(sink, bufs, 0, 1000);
        tryBadWrite(sink, bufs, 1000, 1);
        tryBadRead(source, bufs, 0, -1);
        tryBadRead(source, bufs, -1, 0);
        tryBadRead(source, bufs, 0, 1000);
        tryBadRead(source, bufs, 1000, 1);
        sink.close();
        source.close();
    }

    static void testFileVectorParams() throws Exception {
        File testFile = File.createTempFile("filevec", null);
        testFile.deleteOnExit();
        RandomAccessFile raf = new RandomAccessFile(testFile, "rw");
        FileChannel fc = raf.getChannel();
        tryBadWrite(fc, bufs, 0, -1);
        tryBadWrite(fc, bufs, -1, 0);
        tryBadWrite(fc, bufs, 0, 1000);
        tryBadWrite(fc, bufs, 1000, 1);
        tryBadRead(fc, bufs, 0, -1);
        tryBadRead(fc, bufs, -1, 0);
        tryBadRead(fc, bufs, 0, 1000);
        tryBadRead(fc, bufs, 1000, 1);
        fc.close();
    }

    private static void tryBadWrite(GatheringByteChannel gbc,
                                    ByteBuffer[] bufs, int offset, int len)
        throws Exception
    {
        try {
            gbc.write(bufs, offset, len);
            throw new RuntimeException("Expected exception not thrown");
        } catch (IndexOutOfBoundsException ioobe) {
            // Correct result
        }
    }

    private static void tryBadRead(ScatteringByteChannel sbc,
                                   ByteBuffer[] bufs, int offset, int len)
        throws Exception
    {
        try {
            sbc.read(bufs, offset, len);
            throw new RuntimeException("Expected exception not thrown");
        } catch (IndexOutOfBoundsException ioobe) {
            // Correct result
        }
    }

}
