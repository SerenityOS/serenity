/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6565543
 * @summary Minimal test for unsafe.copyMemory() and unsafe.setMemory()
 * @modules java.base/sun.nio.ch
 *          jdk.unsupported
 * @key randomness
 */

import java.util.*;
import java.lang.reflect.*;
import java.nio.*;

import sun.misc.Unsafe;

import sun.nio.ch.DirectBuffer;

public class CopyMemory {

    private final static int BUFFER_SIZE = 1024;
    private final static int N = 16 * 1024;

    private final static int FILLER = 0x55;
    private final static int FILLER2 = 0x33;

    private final static Random random = new Random();

    private static void set(byte[] b, int ofs, int len, int value) {
        for (int i = 0; i < len; i++) {
            b[ofs + i] = (byte)value;
        }
    }

    private static void check(byte[] b, int ofs, int len, int value) {
        for (int i = 0; i < len; i++) {
            int r = b[ofs + i] & 0xff;
            if (r != value) {
                throw new RuntimeException("mismatch");
            }
        }
    }

    private static void set(Unsafe unsafe, long addr, int ofs, int len, int value) {
        for (int i = 0; i < len; i++) {
            unsafe.putByte(null, addr + ofs + i, (byte)value);
        }
    }

    private static void check(Unsafe unsafe, long addr, int ofs, int len, int value) {
        for (int i = 0; i < len; i++) {
            int r = unsafe.getByte(null, addr + ofs + i) & 0xff;
            if (r != value) {
                throw new RuntimeException("mismatch");
            }
        }
    }

    private static final List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();

    private static long getMemory(int n) {
        ByteBuffer b = ByteBuffer.allocateDirect(n);
        if (b instanceof DirectBuffer == false) {
            throw new RuntimeException("Not a direct buffer");
        }
        buffers.add(b); // make sure the buffer does not get GCed
        return ((DirectBuffer)b).address();
    }

    private static void testSetByteArray(Unsafe unsafe) throws Exception {
        System.out.println("Testing setMemory() for byte[]...");
        byte[] b = new byte[BUFFER_SIZE];
        for (int i = 0; i < N; i++) {
            set(b, 0, BUFFER_SIZE, FILLER);
            int ofs = random.nextInt(BUFFER_SIZE / 2);
            int len = random.nextInt(BUFFER_SIZE / 2);
            int val = random.nextInt(256);
            unsafe.setMemory(b, Unsafe.ARRAY_BYTE_BASE_OFFSET + ofs, len, (byte)val);
            check(b, 0, ofs - 1, FILLER);
            check(b, ofs, len, val);
            check(b, ofs + len, BUFFER_SIZE - (ofs + len), FILLER);
        }
    }

    private static void testSetRawMemory(Unsafe unsafe) throws Exception {
        System.out.println("Testing setMemory() for raw memory...");
        long b = getMemory(BUFFER_SIZE);
        for (int i = 0; i < N; i++) {
            set(unsafe, b, 0, BUFFER_SIZE, FILLER);
            int ofs = random.nextInt(BUFFER_SIZE / 2);
            int len = random.nextInt(BUFFER_SIZE / 2);
            int val = random.nextInt(256);
            unsafe.setMemory(null, b + ofs, len, (byte)val);
            check(unsafe, b, 0, ofs - 1, FILLER);
            check(unsafe, b, ofs, len, val);
            check(unsafe, b, ofs + len, BUFFER_SIZE - (ofs + len), FILLER);
        }
    }

    private static void testCopyByteArrayToByteArray(Unsafe unsafe) throws Exception {
        System.out.println("Testing copyMemory() for byte[] to byte[]...");
        byte[] b1 = new byte[BUFFER_SIZE];
        byte[] b2 = new byte[BUFFER_SIZE];
        for (int i = 0; i < N; i++) {
            set(b1, 0, BUFFER_SIZE, FILLER);
            set(b2, 0, BUFFER_SIZE, FILLER2);
            int ofs = random.nextInt(BUFFER_SIZE / 2);
            int len = random.nextInt(BUFFER_SIZE / 2);
            int val = random.nextInt(256);
            set(b1, ofs, len, val);
            int ofs2 = random.nextInt(BUFFER_SIZE / 2);
            unsafe.copyMemory(b1, Unsafe.ARRAY_BYTE_BASE_OFFSET + ofs,
                b2, Unsafe.ARRAY_BYTE_BASE_OFFSET + ofs2, len);
            check(b2, 0, ofs2 - 1, FILLER2);
            check(b2, ofs2, len, val);
            check(b2, ofs2 + len, BUFFER_SIZE - (ofs2 + len), FILLER2);
        }
    }

    private static void testCopyByteArrayToRawMemory(Unsafe unsafe) throws Exception {
        System.out.println("Testing copyMemory() for byte[] to raw memory...");
        byte[] b1 = new byte[BUFFER_SIZE];
        long b2 = getMemory(BUFFER_SIZE);
        for (int i = 0; i < N; i++) {
            set(b1, 0, BUFFER_SIZE, FILLER);
            set(unsafe, b2, 0, BUFFER_SIZE, FILLER2);
            int ofs = random.nextInt(BUFFER_SIZE / 2);
            int len = random.nextInt(BUFFER_SIZE / 2);
            int val = random.nextInt(256);
            set(b1, ofs, len, val);
            int ofs2 = random.nextInt(BUFFER_SIZE / 2);
            unsafe.copyMemory(b1, Unsafe.ARRAY_BYTE_BASE_OFFSET + ofs,
                null, b2 + ofs2, len);
            check(unsafe, b2, 0, ofs2 - 1, FILLER2);
            check(unsafe, b2, ofs2, len, val);
            check(unsafe, b2, ofs2 + len, BUFFER_SIZE - (ofs2 + len), FILLER2);
        }
    }

    private static void testCopyRawMemoryToByteArray(Unsafe unsafe) throws Exception {
        System.out.println("Testing copyMemory() for raw memory to byte[]...");
        long b1 = getMemory(BUFFER_SIZE);
        byte[] b2 = new byte[BUFFER_SIZE];
        for (int i = 0; i < N; i++) {
            set(unsafe, b1, 0, BUFFER_SIZE, FILLER);
            set(b2, 0, BUFFER_SIZE, FILLER2);
            int ofs = random.nextInt(BUFFER_SIZE / 2);
            int len = random.nextInt(BUFFER_SIZE / 2);
            int val = random.nextInt(256);
            set(unsafe, b1, ofs, len, val);
            int ofs2 = random.nextInt(BUFFER_SIZE / 2);
            unsafe.copyMemory(null, b1 + ofs,
                b2, Unsafe.ARRAY_BYTE_BASE_OFFSET + ofs2, len);
            check(b2, 0, ofs2 - 1, FILLER2);
            check(b2, ofs2, len, val);
            check(b2, ofs2 + len, BUFFER_SIZE - (ofs2 + len), FILLER2);
        }
    }

    private static void testCopyRawMemoryToRawMemory(Unsafe unsafe) throws Exception {
        System.out.println("Testing copyMemory() for raw memory to raw memory...");
        long b1 = getMemory(BUFFER_SIZE);
        long b2 = getMemory(BUFFER_SIZE);
        for (int i = 0; i < N; i++) {
            set(unsafe, b1, 0, BUFFER_SIZE, FILLER);
            set(unsafe, b2, 0, BUFFER_SIZE, FILLER2);
            int ofs = random.nextInt(BUFFER_SIZE / 2);
            int len = random.nextInt(BUFFER_SIZE / 2);
            int val = random.nextInt(256);
            set(unsafe, b1, ofs, len, val);
            int ofs2 = random.nextInt(BUFFER_SIZE / 2);
            unsafe.copyMemory(null, b1 + ofs,
                null, b2 + ofs2, len);
            check(unsafe, b2, 0, ofs2 - 1, FILLER2);
            check(unsafe, b2, ofs2, len, val);
            check(unsafe, b2, ofs2 + len, BUFFER_SIZE - (ofs2 + len), FILLER2);
        }
    }

    private static Unsafe getUnsafe() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe)f.get(null);
    }

    public static void main(String[] args) throws Exception {
        Unsafe unsafe = getUnsafe();

        testSetByteArray(unsafe);
        testSetRawMemory(unsafe);
        testCopyByteArrayToByteArray(unsafe);
        testCopyByteArrayToRawMemory(unsafe);
        testCopyRawMemoryToByteArray(unsafe);
        testCopyRawMemoryToRawMemory(unsafe);

        System.out.println("OK");
    }

}
