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


/* @test
 * @bug 7109837
 * @summary Test Adler32/CRC32.update(ByteBuffer)
 * @key randomness
 */

import java.util.*;
import java.util.zip.*;
import java.nio.*;

public class TimeChecksum {

    static long time(Adler32 adler32, byte[] data, int iters, int len) {
        long start_t = System.nanoTime();
        for (int i = 0; i < iters; i++) {
            adler32.reset();
            adler32.update(data, 0, len);
        }
        long t = System.nanoTime() - start_t;
        System.out.printf("%,12d", t / len);
        return t;
    }

    static long time(Adler32 adler32, ByteBuffer buf, int iters) {
        long start_t = System.nanoTime();
        for (int i = 0; i < iters; i++) {
            adler32.reset();
            buf.mark();
            adler32.update(buf);
            buf.reset();
        }
        long t = System.nanoTime() - start_t;
        System.out.printf("%,12d", t / buf.remaining());
        return t;
    }

    static void testPosLimit(Adler32 adler32, ByteBuffer buf) {
        int pos = buf.position();
        int limit = buf.limit();
        adler32.update(buf);
        if (limit != buf.position() || limit != buf.limit()) {
            System.out.printf("%nFAILED: pos,limit=(%d, %d), expected (%d, %d)%n",
                    buf.position(), buf.limit(), limit, limit);
            throw new RuntimeException();
        }
        buf.position(pos);
    }

    static long time(CRC32 crc32, byte[] data, int iters, int len) {
        long start_t = System.nanoTime();
        for (int i = 0; i < iters; i++) {
            crc32.reset();
            crc32.update(data, 0, len);
        }
        long t = System.nanoTime() - start_t;
        System.out.printf("%,12d", t / len);
        return t;
    }

    static long time(CRC32 crc32, ByteBuffer buf, int iters) {
        long start_t = System.nanoTime();
        for (int i = 0; i < iters; i++) {
            crc32.reset();
            buf.mark();
            crc32.update(buf);
            buf.reset();
        }
        long t = System.nanoTime() - start_t;
        System.out.printf("%,12d", t / buf.remaining());
        return t;
    }

    static void testPosLimit(CRC32 crc32, ByteBuffer buf) {
        int pos = buf.position();
        int limit = buf.limit();
        crc32.update(buf);
        if (limit != buf.position() || limit != buf.limit()) {
            System.out.printf("%nFAILED: pos,limit=(%d, %d), expected (%d, %d)%n",
                    buf.position(), buf.limit(), limit, limit);
            throw new RuntimeException();
        }
        buf.position(pos);
    }

    public static void main(String[] args) {
        int len     = 1024 * 32;
        int iters   = 1;
        if (args.length != 0 && "-benchmark".equals(args[0]))
            iters = 100000;
        Adler32 adler32 = new Adler32();
        CRC32 crc32 = new CRC32();
        Random rdm = new Random();
        byte[] data = new byte[len];
        new Random().nextBytes(data);
        ByteBuffer buf;

        System.out.println("---------- Adler32 ----------");
        System.out.print("Warmup...");
        time(adler32, data, iters, len);
        time(adler32, ByteBuffer.wrap(data), iters);
        buf = ByteBuffer.allocateDirect(len);
        buf.put(data, 0, len);
        buf.flip();
        time(adler32, buf, iters);
        System.out.println("\n");

        System.out.println("Length    byte[](ns/len)  ByteBuffer(direct)   ByteBuffer");
        for (int testlen = 1; testlen < data.length; testlen <<= 1) {
            System.out.print(testlen + "\t");
            long baT = time(adler32, data, iters, testlen);
            long baV = adler32.getValue();
            System.out.print("\t");

            buf = ByteBuffer.allocateDirect(testlen);
            buf.put(data, 0, testlen);
            buf.flip();
            long bbdT = time(adler32, buf, iters);
            long bbdV = adler32.getValue();
            if (baV != bbdV) {
                System.out.printf("%nFAILED: baV=%x,bbdV=%x%n", baV, bbdV);
                throw new RuntimeException();
            }
            System.out.printf(" (%.2f)", (float)bbdT/baT);
            testPosLimit(adler32, buf);

            buf = ByteBuffer.allocate(testlen);
            buf.put(data, 0, testlen);
            buf.flip();
            long bbT = time(adler32, buf, iters);
            long bbV = adler32.getValue();
            if (baV != bbV) {
                System.out.printf("%nFAILED: baV=%x,bbV=%x%n", baV, bbV);
                throw new RuntimeException();
            }
            testPosLimit(adler32, buf);
            System.out.printf(" (%.2f)     checksum=%x%n", (float)bbT/baT, bbV);
        }

        System.out.println("\n---------- CRC32 ----------");
        System.out.print("Warmup...");
        time(crc32, data, iters, len);
        time(crc32, ByteBuffer.wrap(data), iters);
        buf = ByteBuffer.allocateDirect(len);
        buf.put(data, 0, len);
        buf.flip();
        time(crc32, buf, iters);
        System.out.println("\n");


        System.out.println("Length    byte[](ns/len)  ByteBuffer(direct)   ByteBuffer");
        for (int testlen = 1; testlen < data.length; testlen <<= 1) {
            System.out.print(testlen + "\t");
            long baT = time(crc32, data, iters, testlen);
            long baV = crc32.getValue();
            System.out.print("\t");

            buf = ByteBuffer.allocateDirect(testlen);
            buf.put(data, 0,  testlen);
            buf.flip();
            long bbdT = time(crc32, buf, iters);
            long bbdV = crc32.getValue();
            if (baV != bbdV) {
                System.out.printf("%nFAILED: baV=%x,bbdV=%x%n", baV, bbdV);
                throw new RuntimeException();
            }
            System.out.printf(" (%.2f)", (float)bbdT/baT);
            testPosLimit(crc32, buf);

            buf = ByteBuffer.allocate(testlen);
            buf.put(data, 0, testlen);
            buf.flip();
            long bbT = time(crc32, buf, iters);
            long bbV = crc32.getValue();
            if (baV != bbV) {
                System.out.printf("%nFAILED: baV=%x,bbV=%x%n", baV, bbV);
                throw new RuntimeException();
            }
            testPosLimit(crc32, buf);
            System.out.printf(" (%.2f)     checksum=%x%n", (float)bbT / baT, bbV);
        }
    }
}
