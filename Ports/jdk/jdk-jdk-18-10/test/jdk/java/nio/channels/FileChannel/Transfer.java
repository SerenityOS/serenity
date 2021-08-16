/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4434723 4482726 4559072 4795550 5081340 5103988 6984545
 * @summary Test FileChannel.transferFrom and transferTo (use -Dseed=X to set PRNG seed)
 * @library ..
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run testng/timeout=300 Transfer
 * @key randomness
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import jdk.test.lib.RandomFactory;

import org.testng.annotations.Test;

public class Transfer {

    private static Random generator = RandomFactory.getRandom();

    @Test
    public void testFileChannel() throws Exception {
        File source = File.createTempFile("source", null);
        source.deleteOnExit();
        File sink = File.createTempFile("sink", null);
        sink.deleteOnExit();

        FileOutputStream fos = new FileOutputStream(source);
        FileChannel sourceChannel = fos.getChannel();
        sourceChannel.write(ByteBuffer.wrap(
            "Use the source, Luke!".getBytes()));
        sourceChannel.close();

        FileInputStream fis = new FileInputStream(source);
        sourceChannel = fis.getChannel();

        RandomAccessFile raf = new RandomAccessFile(sink, "rw");
        FileChannel sinkChannel = raf.getChannel();
        long oldSinkPosition = sinkChannel.position();
        long oldSourcePosition = sourceChannel.position();

        long bytesWritten = sinkChannel.transferFrom(sourceChannel, 0, 10);
        if (bytesWritten != 10)
            throw new RuntimeException("Transfer failed");

        if (sourceChannel.position() == oldSourcePosition)
            throw new RuntimeException("Source position didn't change");

        if (sinkChannel.position() != oldSinkPosition)
            throw new RuntimeException("Sink position changed");

        if (sinkChannel.size() != 10)
            throw new RuntimeException("Unexpected sink size");

        bytesWritten = sinkChannel.transferFrom(sourceChannel, 1000, 10);

        if (bytesWritten > 0)
            throw new RuntimeException("Wrote past file size");

        sourceChannel.close();
        sinkChannel.close();

        source.delete();
        sink.delete();
    }

    @Test
    public void testReadableByteChannel() throws Exception {
        int[] testSizes = { 0, 10, 1023, 1024, 1025, 2047, 2048, 2049 };

        for (int size : testSizes) {
            SelectorProvider sp = SelectorProvider.provider();
            Pipe p = sp.openPipe();
            Pipe.SinkChannel sink = p.sink();
            Pipe.SourceChannel source = p.source();
            sink.configureBlocking(false);

            ByteBuffer outgoingdata = ByteBuffer.allocateDirect(size + 10);
            byte[] someBytes = new byte[size + 10];
            generator.nextBytes(someBytes);
            outgoingdata.put(someBytes);
            outgoingdata.flip();

            int totalWritten = 0;
            while (totalWritten < size + 10) {
                int written = sink.write(outgoingdata);
                if (written < 0)
                    throw new Exception("Write failed");
                totalWritten += written;
            }

            File f = File.createTempFile("blah"+size, null);
            f.deleteOnExit();
            RandomAccessFile raf = new RandomAccessFile(f, "rw");
            FileChannel fc = raf.getChannel();
            long oldPosition = fc.position();

            long bytesWritten = fc.transferFrom(source, 0, size);
            fc.force(true);
            if (bytesWritten != size)
                throw new RuntimeException("Transfer failed");

            if (fc.position() != oldPosition)
                throw new RuntimeException("Position changed");

            if (fc.size() != size)
                throw new RuntimeException("Unexpected sink size "+ fc.size());

            fc.close();
            sink.close();
            source.close();

            f.delete();
        }
    }

    @Test
    public void xferTest02() throws Exception { // for bug 4482726
        byte[] srcData = new byte[5000];
        for (int i=0; i<5000; i++)
            srcData[i] = (byte)generator.nextInt();

        // get filechannel for the source file.
        File source = File.createTempFile("source", null);
        source.deleteOnExit();
        RandomAccessFile raf1 = new RandomAccessFile(source, "rw");
        FileChannel fc1 = raf1.getChannel();

        // write out data to the file channel
        long bytesWritten = 0;
        while (bytesWritten < 5000) {
            bytesWritten = fc1.write(ByteBuffer.wrap(srcData));
        }

        // get filechannel for the dst file.
        File dest = File.createTempFile("dest", null);
        dest.deleteOnExit();
        RandomAccessFile raf2 = new RandomAccessFile(dest, "rw");
        FileChannel fc2 = raf2.getChannel();

        int bytesToWrite = 3000;
        int startPosition = 1000;

        bytesWritten = fc1.transferTo(startPosition, bytesToWrite, fc2);

        fc1.close();
        fc2.close();
        raf1.close();
        raf2.close();

        source.delete();
        dest.delete();
    }

    @Test
    public void xferTest03() throws Exception { // for bug 4559072
        byte[] srcData = new byte[] {1,2,3,4} ;

        // get filechannel for the source file.
        File source = File.createTempFile("source", null);
        source.deleteOnExit();
        RandomAccessFile raf1 = new RandomAccessFile(source, "rw");
        FileChannel fc1 = raf1.getChannel();
        fc1.truncate(0);

        // write out data to the file channel
        int bytesWritten = 0;
        while (bytesWritten < 4) {
            bytesWritten = fc1.write(ByteBuffer.wrap(srcData));
        }

        // get filechannel for the dst file.
        File dest = File.createTempFile("dest", null);
        dest.deleteOnExit();
        RandomAccessFile raf2 = new RandomAccessFile(dest, "rw");
        FileChannel fc2 = raf2.getChannel();
        fc2.truncate(0);

        fc1.transferTo(0, srcData.length + 1, fc2);

        if (fc2.size() > 4)
            throw new Exception("xferTest03 failed");

        fc1.close();
        fc2.close();
        raf1.close();
        raf2.close();

        source.delete();
        dest.delete();
    }

    // xferTest04() and xferTest05() moved to Transfer4GBFile.java

    static void checkFileData(File file, String expected) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        Reader r = new BufferedReader(new InputStreamReader(fis, "ASCII"));
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = r.read()) != -1)
            sb.append((char)c);
        String contents = sb.toString();
        if (! contents.equals(expected))
            throw new Exception("expected: " + expected
                                + ", got: " + contents);
        r.close();
    }

    // Test transferFrom asking for more bytes than remain in source
    @Test
    public void xferTest06() throws Exception { // for bug 5081340
        String data = "Use the source, Luke!";

        File source = File.createTempFile("source", null);
        source.deleteOnExit();
        File sink = File.createTempFile("sink", null);
        sink.deleteOnExit();

        FileOutputStream fos = new FileOutputStream(source);
        fos.write(data.getBytes("ASCII"));
        fos.close();

        FileChannel sourceChannel =
            new RandomAccessFile(source, "rw").getChannel();
        sourceChannel.position(7);
        long remaining = sourceChannel.size() - sourceChannel.position();
        FileChannel sinkChannel =
            new RandomAccessFile(sink, "rw").getChannel();
        long n = sinkChannel.transferFrom(sourceChannel, 0L,
                                          sourceChannel.size()); // overflow
        if (n != remaining)
            throw new Exception("n == " + n + ", remaining == " + remaining);

        sinkChannel.close();
        sourceChannel.close();

        checkFileData(source, data);
        checkFileData(sink, data.substring(7,data.length()));

        source.delete();
    }

    // Test transferTo to non-blocking socket channel
    @Test
    public void xferTest07() throws Exception { // for bug 5103988
        File source = File.createTempFile("source", null);
        source.deleteOnExit();

        FileChannel sourceChannel = new RandomAccessFile(source, "rw")
            .getChannel();
        sourceChannel.position(32000L)
            .write(ByteBuffer.wrap("The End".getBytes()));

        // The sink is a non-blocking socket channel
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress(0));
        InetSocketAddress sa = new InetSocketAddress(
            InetAddress.getLocalHost(), ssc.socket().getLocalPort());
        SocketChannel sink = SocketChannel.open(sa);
        sink.configureBlocking(false);
        SocketChannel other = ssc.accept();

        long size = sourceChannel.size();

        // keep sending until congested
        long n;
        do {
            n = sourceChannel.transferTo(0, size, sink);
        } while (n > 0);

        sourceChannel.close();
        sink.close();
        other.close();
        ssc.close();
        source.delete();
    }

    // xferTest08() moved to TransferTo6GBFile.java

    // Test that transferFrom with FileChannel source that is not readable
    // throws NonReadableChannelException
    @Test
    public void xferTest09() throws Exception { // for bug 6984545
        File source = File.createTempFile("source", null);
        source.deleteOnExit();

        File target = File.createTempFile("target", null);
        target.deleteOnExit();

        FileChannel fc1 = new FileOutputStream(source).getChannel();
        FileChannel fc2 = new RandomAccessFile(target, "rw").getChannel();
        try {
            fc2.transferFrom(fc1, 0L, 0);
            throw new RuntimeException("NonReadableChannelException expected");
        } catch (NonReadableChannelException expected) {
        } finally {
            fc1.close();
            fc2.close();
        }
    }
}
