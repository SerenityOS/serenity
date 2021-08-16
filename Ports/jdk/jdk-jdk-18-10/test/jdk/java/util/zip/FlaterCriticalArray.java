/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * ZIP inflater/deflater performance.
 */

/*
 * Run this test on JDK 6 and on later
 * JDKs to compare results:
 *     java -server -Xms1024M -Xmx1024M -Ddebug=true FlaterCriticalArray
 *
 * The performance issues can be readily seen on JDK 6, and so this code is
 * written to compile on that platform: it does *not* use any JDK 7 - specific
 * features.
 */

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;

public class FlaterCriticalArray {
    // If true, print information about performance
    private static final boolean debug = System.getProperty("debug") != null;

    private static void debug(String s) {
        if (debug) System.out.println(s);
    }

    private static void debug(String name, String inOut, long time, int length) {
        debug(name + ": Duration of " + inOut + "(in ms): " + time);
        debug(name + ": " + inOut + "d data length: " + length + " bytes");
    }

    private static byte[] grow(byte[] a, int capacity) {
        while (a.length < capacity) {
            byte[] a2 = new byte[a.length * 2];
            System.arraycopy(a, 0, a2, 0, a.length);
            a = a2;
        }
        return a;
    }

    private static byte[] trim(byte[] a, int length) {
        byte[] res = new byte[length];
        System.arraycopy(a, 0, res, 0, length);
        return res;
    }

    /*
     * Base class for individual test cases
     */
    private abstract static class TestCase {
        protected String name;  // For information in debug messages
        protected byte data[];  // Data to be deflated and subsequently inflated
        protected int level;    // Compression level for deflater

        protected TestCase(String name, byte data[]) {
            this(name, data, -1);
        }

        protected TestCase(String name, byte data[], int level) {
            this.name = name;
            this.data = data;
            this.level = level;
        }

        public void runTest() throws Throwable {
            long time0, time1;
            byte deflated[], inflated[];

            debug("");

            time0 = System.currentTimeMillis();
            deflated = deflate(data, level);
            time1 = System.currentTimeMillis();
            inform("Deflate", time1 - time0, deflated.length);

            time0 = System.currentTimeMillis();
            inflated = inflate(deflated);
            time1 = System.currentTimeMillis();
            inform("Inflate", time1 - time0, inflated.length);

            check(Arrays.equals(data, inflated),
                  name + ": Inflated and deflated arrays do not match");
        }

        private void inform(String inOut, long duration, int length) {
            debug(name, inOut, duration, length);
        }

        protected abstract byte[] deflate(byte data[], int level) throws Throwable;

        protected abstract byte[] inflate(byte deflated[]) throws Throwable;
    }

    /*
     * Following are  the individual test cases
     */

    private static class StrideTest extends TestCase {
        static final int STRIDE = 1024;

        public StrideTest(byte data[], int level) {
            super("STRIDE", data, level);
        }

        protected byte[] deflate(byte in[], int level) throws Throwable {
            final int len = in.length;
            final Deflater deflater = new Deflater(level);
            final byte[] smallBuffer = new byte[32];
            byte[] flated = new byte[32];
            int count = 0;
            for (int i = 0; i<len; i+= STRIDE) {
                deflater.setInput(in, i, Math.min(STRIDE, len-i));
                while (!deflater.needsInput()) {
                    int n = deflater.deflate(smallBuffer);
                    flated = grow(flated, count + n);
                    System.arraycopy(smallBuffer, 0, flated, count, n);
                    count += n;
                }
            }
            deflater.finish();
            int n;
            do {
                n = deflater.deflate(smallBuffer);
                flated = grow(flated, count + n);
                System.arraycopy(smallBuffer, 0, flated, count, n);
                count += n;
            } while (n > 0);
            return trim(flated, count);
        }

        protected byte[] inflate(byte in[]) throws Throwable {
            final int len = in.length;
            final Inflater inflater = new Inflater();
            final byte[] smallBuffer = new byte[3200];

            byte[] flated = new byte[32];
            int count = 0;

            for (int i = 0; i<len; i+= STRIDE) {
                inflater.setInput(in, i, Math.min(STRIDE, len-i));
                while (!inflater.needsInput()) {
                    int n;
                    while ((n = inflater.inflate(smallBuffer)) > 0) {
                        flated = grow(flated, count + n);
                        System.arraycopy(smallBuffer, 0, flated, count, n);
                        count += n;
                    }
                }
            }
            return trim(flated, count);
        }
    }

    private static class NoStrideTest extends TestCase {
        public NoStrideTest(byte data[], int level) {
            super("NO STRIDE", data, level);
        }

        public byte[] deflate(byte in[], int level) throws Throwable {
            final Deflater flater = new Deflater(level);
            flater.setInput(in);
            flater.finish();
            final byte[] smallBuffer = new byte[32];
            byte[] flated = new byte[32];
            int count = 0;
            int n;
            while ((n = flater.deflate(smallBuffer)) > 0) {
                flated = grow(flated, count + n);
                System.arraycopy(smallBuffer, 0, flated, count, n);
                count += n;
            }
            return trim(flated, count);
        }

        public byte[] inflate(byte in[]) throws Throwable {
            final Inflater flater = new Inflater();
            flater.setInput(in);
            final byte[] smallBuffer = new byte[32];
            byte[] flated = new byte[32];
            int count = 0;
            int n;
            while ((n = flater.inflate(smallBuffer)) > 0) {
                flated = grow(flated, count + n);
                System.arraycopy(smallBuffer, 0, flated, count, n);
                count += n;
            }
            return trim(flated, count);
        }
    }

    /**
     * Check Deflater{In,Out}putStream by way of GZIP{In,Out}putStream
     */
    private static class GZIPTest extends TestCase {
        public GZIPTest(byte data[]) {
            super("GZIP", data);
        }

        public byte[] deflate(byte data[], int ignored) throws Throwable {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStream gzos = new GZIPOutputStream(baos);
            gzos.write(data, 0, data.length);
            gzos.close();
            return baos.toByteArray();
        }

        public byte[] inflate(byte deflated[]) throws Throwable {
            InputStream bais = new ByteArrayInputStream(deflated);
            GZIPInputStream gzis = new GZIPInputStream(bais);
            byte[] inflated = new byte[data.length];
            int numRead = 0;
            int count = 0;
            while ((numRead = gzis.read(inflated, count, data.length - count)) > 0) {
                count += numRead;
            }
            check(count == data.length, name + ": Read " + count + "; expected " + data.length);
            return inflated;
        }
    }

    public static void realMain(String[] args) throws Throwable {
        byte data[];
        int level = -1;
        if (args.length > 0) {
          level = Integer.parseInt(args[0]);
        }
        debug("Using level " + level);

        if (args.length > 1) {
            FileInputStream fis = new FileInputStream(args[1]);
            int len = fis.available();
            data = new byte[len];
            check(fis.read(data, 0, len) == len, "Did not read complete file");
            debug("Original data from " + args[1]);
            fis.close();
        } else {
            ByteBuffer bb = ByteBuffer.allocate(8);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (int i = 0; i < 1024 * 64; i++) { // data length
                bb.putDouble(0, Math.random());
                baos.write(bb.array(), 0, 8);
            }
            data = baos.toByteArray();
            debug("Original data from random byte array");
        }
        debug("Original data length: " + data.length + " bytes");

        new StrideTest(data, level).runTest();
        new NoStrideTest(data, level).runTest();
        new GZIPTest(data).runTest();
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void pass(String msg) {System.out.println(msg); passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void unexpected(Throwable t, String msg) {
        System.out.println(msg); failed++; t.printStackTrace();}
    static boolean check(boolean cond) {if (cond) pass(); else fail(); return cond;}
    static boolean check(boolean cond, String msg) {if (cond) pass(); else fail(msg); return cond;}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
