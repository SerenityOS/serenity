/*
 * Copyright (c) 2002, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Comprehensive test for FileChannel.transfer{From,To}
 * @bug 4708120
 * @author Mark Reinhold
 * @run main/timeout=300 Transfers
 */

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;


public class Transfers {

    static PrintStream out = System.out;

    private static class Failure
        extends RuntimeException
    {

        Failure(Exception x) {
            super(x);
        }

        Failure(String s) {
            super(s);
        }

    }


    // -- Writing and reading random bytes --

    private static void writeBytes(byte[] ba, FileChannel fc,
                                   int off, int len)
        throws IOException
    {
        fc.position(off);
        if (fc.write(ByteBuffer.wrap(ba, 0, len)) != len)
            throw new IOException("Incomplete write");
        fc.position(0);
    }

    private static void writeRandomBytes(long seed,
                                         FileChannel fc, int off, int len)
        throws IOException
    {
        Random r = new Random(seed);
        byte[] ba = new byte[len];
        r.nextBytes(ba);
        writeBytes(ba, fc, off, len);
    }

    private static void writeZeroBytes(FileChannel fc, int off, int len)
        throws IOException
    {
        byte[] ba = new byte[len];
        writeBytes(ba, fc, off, len);
    }

    private static void checkBytes(FileChannel fc, int off, int len,
                                   byte[] bytes)
        throws IOException
    {
        ByteBuffer bb = ByteBuffer.allocate(len);
        fc.position(off);
        if (fc.read(bb) != len)
            throw new IOException("Incomplete read");
        bb.flip();
        ByteBuffer bab = ByteBuffer.wrap(bytes, 0, len);
        if (!bb.equals(bab))
            throw new Failure("Wrong data written");
    }

    private static void checkRandomBytes(FileChannel fc, int off, int len,
                                         long seed)
        throws IOException
    {
        byte[] ba = new byte[len];
        Random r = new Random(seed);
        r.nextBytes(ba);
        checkBytes(fc, off, len, ba);
    }

    private static void checkZeroBytes(FileChannel fc, int off, int len)
        throws IOException
    {
        byte[] ba = new byte[len];
        checkBytes(fc, off, len, ba);
    }

    // For debugging
    //
    private static void dump(FileChannel fc)
        throws IOException
    {
        int sz = (int)fc.size();
        ByteBuffer bb = ByteBuffer.allocate(sz);
        fc.position(0);
        if (fc.read(bb) != sz)
            throw new IOException("Incomplete read");
        bb.flip();
        byte prev = -1;
        int r = 0;                      // Repeats
        int n = 0;
        while (bb.hasRemaining() && (n < 32)) {
            byte b = bb.get();
            if (b == prev) {
                r++;
                continue;
            }
            if (r > 0) {
                int c = prev & 0xff;
                if (c < 0x10)
                    out.print('0');
                out.print(Integer.toHexString(c));
                if (r > 1) {
                    out.print("[");
                    out.print(r);
                    out.print("]");
                }
                n++;
            }
            prev = b;
            r = 1;
        }
        if (r > 0) {
            int c = prev & 0xff;
            if (c < 0x10)
                out.print('0');
            out.print(Integer.toHexString(c));
            if (r > 1) {
                out.print("[");
                out.print(r);
                out.print("]");
            }
            n++;
        }
        if (bb.hasRemaining())
            out.print("...");
        out.println();
    }



    static File sourceFile;
    static File targetFile;

    // -- Self-verifying sources and targets --

    static abstract class Source {

        protected final int size;
        protected final long seed;
        private final String name;

        Source(int size, long seed, String name) {
            this.size = size;
            this.seed = seed;
            this.name = name;
        }

        String name() {
            return name;
        }

        abstract ReadableByteChannel channel();

        abstract void verify() throws IOException;

    }

    static class FileSource
        extends Source
    {
        private final File fn;
        private final RandomAccessFile raf;
        private final FileChannel fc;

        FileSource(int size, long seed) throws IOException {
            super(size, seed, "FileChannel");
            fn = sourceFile;
            raf = new RandomAccessFile(fn, "rw");
            fc = raf.getChannel();
            fc.position(0);
            writeRandomBytes(seed, fc, 0, size);
        }

        ReadableByteChannel channel() {
            return fc;
        }

        void verify() throws IOException {
            if (fc.position() != size)
                throw new Failure("Wrong position: "
                                  + fc.position() + " (expected " + size +
                                  ")");
            checkRandomBytes(fc, 0, size, seed);
            fc.close();
            raf.close();                // Bug in 1.4.0
        }

    }

    static class UserSource
        extends Source
    {
        private ReadableByteChannel ch;
        private final ByteBuffer src;

        UserSource(int size, long seed) {
            super(size, seed, "UserChannel");

            final byte[] bytes = new byte[size + 1];
            Random r = new Random(seed);
            r.nextBytes(bytes);
            src = ByteBuffer.wrap(bytes);

            ch = new ReadableByteChannel() {
                    public int read(ByteBuffer dst) {
                        if (!src.hasRemaining())
                            return -1;
                        int nr = Math.min(src.remaining(), dst.remaining());
                        ByteBuffer s = src.duplicate();
                        s.limit(s.position() + nr);
                        dst.put(s);
                        src.position(src.position() + nr);
                        return nr;
                    }
                    public boolean isOpen() {
                        return true;
                    }
                    public void close() { }
                };
        }

        ReadableByteChannel channel() {
            return ch;
        }

        void verify() {
            if (src.remaining() != 1)
                throw new Failure("Source has " + src.remaining()
                                  + " bytes remaining (expected 1)");
        }

    }

    static abstract class Target {

        protected final int size;
        protected final long seed;
        private final String name;

        Target(int size, long seed, String name) {
            this.size = size;
            this.seed = seed;
            this.name = name;
        }

        String name() {
            return name;
        }

        abstract WritableByteChannel channel();

        abstract void verify() throws IOException;

    }

    static class FileTarget
        extends Target
    {
        private final File fn;
        private final RandomAccessFile raf;
        private final FileChannel fc;

        FileTarget(int size, long seed) throws IOException {
            super(size, seed, "FileChannel");
            fn = targetFile;
            raf = new RandomAccessFile(fn, "rw");
            fc = raf.getChannel();
            fc.position(0);
        }

        WritableByteChannel channel() {
            return fc;
        }

        void verify() throws IOException {
            if (fc.position() != size)
                throw new Failure("Wrong position: "
                                  + fc.position() + " (expected " + size + ")");
            checkRandomBytes(fc, 0, size, seed);
            fc.close();
            raf.close();                // Bug in 1.4.0
        }

    }

    static class UserTarget
        extends Target
    {
        private WritableByteChannel ch;
        private final ByteBuffer dst;

        UserTarget(int size, long seed) {
            super(size, seed, "UserChannel");
            dst = ByteBuffer.wrap(new byte[size + 1]);

            ch = new WritableByteChannel() {
                    public int write(ByteBuffer src) {
                        int nr = Math.min(src.remaining(), dst.remaining());
                        ByteBuffer s = src.duplicate();
                        s.limit(s.position() + nr);
                        dst.put(s);
                        src.position(src.position() + nr);
                        return nr;
                    }
                    public boolean isOpen() {
                        return true;
                    }
                    public void close() { }
                };
        }

        WritableByteChannel channel() {
            return ch;
        }

        void verify() {
            if (dst.remaining() != 1)
                throw new Failure("Destination has " + dst.remaining()
                                  + " bytes remaining (expected 1)");
            byte[] ba = new byte[size];
            Random r = new Random(seed);
            r.nextBytes(ba);
            dst.flip();
            ByteBuffer rbb = ByteBuffer.wrap(ba, 0, size);
            if (!dst.equals(rbb))
                throw new Failure("Wrong data written");
        }

    }


    // Generates a sequence of ints of the form 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
    // 15, 16, 17, 31, 32, 33, ..., 2^i-1, 2^i, 2^i+1, ..., max.

    static class IntGenerator {

        private int max;
        private int cur = -1;
        private int p2 = 8;

        IntGenerator(int max) {
            this.max = max;
        }

        boolean hasNext() {
            return cur < max;
        }

        int next() {
            if (cur >= max)
                throw new IllegalStateException();
            if (cur < 6) {
                cur++;
                return cur;
            }
            if (cur == p2 + 1) {
                p2 <<= 1;
                cur = p2 - 1;
                return cur;
            }
            cur++;
            return cur;
        }

    }


    // -- Tests --

    private static final int MAX_XFER_SIZE = 1 << 14;
    private static final int MAX_FILE_SIZE = MAX_XFER_SIZE << 1;

    private static boolean debug = false;
    private static boolean verbose = false;

    static void show(String dir, String channelName, int off, int len) {
        if (!verbose)
            return;
        out.println(dir + " " + channelName +
                    ": offset " + off + ", length " + len);
    }

    static void testTo(long seed, FileChannel fc, int off, int len, Target tgt)
        throws IOException
    {
        show("To", tgt.name(), off, len);

        // Clear source, then randomize just the source region
        writeZeroBytes(fc, 0, MAX_FILE_SIZE);
        writeRandomBytes(seed, fc, off, len);

        // Randomize position
        int pos = (int)seed & 0xfff;
        fc.position(pos);

        int n = (int)fc.transferTo(off, len, tgt.channel());
        if (n != len)
            throw new Failure("Incorrect transfer length: " + n
                              + " (expected " + len + ")");

        // Check that source wasn't changed
        if (fc.position() != pos)
            throw new Failure("Position changed");
        if (debug)
            dump(fc);
        checkRandomBytes(fc, off, len, seed);
        writeZeroBytes(fc, off, len);
        checkZeroBytes(fc, 0, MAX_FILE_SIZE);

        // Check that target was updated correctly
        tgt.verify();
    }

    static void testFrom(long seed, Source src, FileChannel fc, int off, int len)
        throws IOException
    {
        show("From", src.name(), off, len);

        // Clear target
        writeZeroBytes(fc, 0, MAX_FILE_SIZE);

        // Randomize position
        int pos = (int)seed & 0xfff;
        fc.position(pos);

        int n = (int)fc.transferFrom(src.channel(), off, len);
        if (n != len)
            throw new Failure("Incorrect transfer length: " + n
                              + " (expected " + len + ")");

        // Check that source didn't change, and was read correctly
        src.verify();

        // Check that target was updated correctly
        if (fc.position() != pos)
            throw new Failure("Position changed");
        if (debug)
            dump(fc);
        checkRandomBytes(fc, off, len, seed);
        writeZeroBytes(fc, off, len);
        checkZeroBytes(fc, 0, MAX_FILE_SIZE);
    }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length > 0) {
            if (args[0].indexOf('v') >= 0)
                verbose = true;
            if (args[0].indexOf('d') >= 0)
                debug = verbose = true;
        }

        File testDir = new File(System.getProperty("test.dir", "."));

        sourceFile = File.createTempFile("xfer.src.", "", testDir);
        sourceFile.deleteOnExit();
        targetFile = File.createTempFile("xfer.tgt.", "", testDir);
        targetFile.deleteOnExit();

        File fn = File.createTempFile("xfer.fch.", "", testDir);
        fn.deleteOnExit();

        Random rnd = new Random();
        int failures = 0;

        try (FileChannel fc = new RandomAccessFile(fn, "rw").getChannel()) {
            for (boolean to = false;; to = true) {
                for (boolean user = false;; user = true) {
                    if (!verbose)
                        out.print((to ? "To " : "From ") +
                                  (user ? "user channel" : "file channel")
                                  + ":");
                    IntGenerator offGen = new IntGenerator(MAX_XFER_SIZE + 2);
                    while (offGen.hasNext()) {
                        int off = offGen.next();
                        if (!verbose) out.print(" " + off);
                        IntGenerator lenGen = new IntGenerator(MAX_XFER_SIZE + 2);
                        while (lenGen.hasNext()) {
                            int len = lenGen.next();
                            long s = rnd.nextLong();
                            String chName = null;
                            try {
                                if (to) {
                                    Target tgt;
                                    if (user)
                                        tgt = new UserTarget(len, s);
                                    else
                                        tgt = new FileTarget(len, s);
                                    chName = tgt.name();
                                    testTo(s, fc, off, len, tgt);
                                }
                                else {
                                    Source src;
                                    if (user)
                                        src = new UserSource(len, s);
                                    else
                                        src = new FileSource(len, s);
                                    chName = src.name();
                                    testFrom(s, src, fc, off, len);
                                }
                            } catch (Failure x) {
                                out.println();
                                out.println("FAILURE: " + chName
                                            + ", offset " + off
                                            + ", length " + len);
                                x.printStackTrace(out);
                                failures++;
                            }
                        }
                    }
                    if (!verbose)
                        out.println();
                    if (user)
                        break;
                }
                if (to)
                    break;
            }
        }

        sourceFile.delete();
        targetFile.delete();
        fn.delete();

        if (failures > 0) {
            out.println();
            throw new RuntimeException("Some tests failed");
        }

        out.println("Test succeeded.");
    }
}
