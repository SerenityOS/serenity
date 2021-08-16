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
 * @test
 * @bug 6571338
 * @summary Inflater should not require a buffer to the inflate() methods
 * larger than 1 byte.
 * @key randomness
 */

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;

public class InflaterBufferSize {
    private static final int DATA_LEN = 1024 *64;
    private static byte[] data;

    // If true, print extra info.
    private static final boolean debug = System.getProperty("debug") != null;

    private static void debug(String s) {
        if (debug) System.out.println(s);
    }

    private static void createData() {
        ByteBuffer bb = ByteBuffer.allocate(8);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < DATA_LEN; i++) {
            bb.putDouble(0, Math.random());
            baos.write(bb.array(), 0, 8);
        }
        data = baos.toByteArray();
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

    private static byte[] deflate(byte[] in, int level) throws Throwable {
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

    private static byte[] inflate(byte[] in) throws Throwable {
        final Inflater flater = new Inflater();
        flater.setInput(in);
        // This is the buffer of interest.  It should be possible to use any
        // non-zero size.
        final byte[] smallBuffer = new byte[1];
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

    public static void realMain(String[] args) throws Throwable {
        byte deflated[], inflated[];

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
            createData();
            debug("Original data from random byte array");
        }
        debug("Original data length: " + data.length + " bytes");

        debug("");
        deflated = deflate(data, level);
        debug("Deflated data length: " + deflated.length + " bytes");

        inflated = inflate(deflated);
        debug("Inflated data length: "+ inflated.length + " bytes" );

        if (!check(Arrays.equals(data, inflated),
                   "Inflated and deflated arrays do not match")) {
            OutputStream os = new BufferedOutputStream(new FileOutputStream("deflated.zip"));
            try {
                os.write(deflated);
            } finally {
                os.close();
            }
        }
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
