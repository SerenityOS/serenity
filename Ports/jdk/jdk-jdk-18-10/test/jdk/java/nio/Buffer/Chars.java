/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8014854
 * @summary Exercises CharBuffer#chars on each of the CharBuffer types
 * @run testng Chars
 * @key randomness
 */

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class Chars {

    static final Random RAND = new Random();

    static final int SIZE = 128 + RAND.nextInt(1024);

    /**
     * Randomize the char buffer's position and limit.
     */
    static CharBuffer randomizeRange(CharBuffer cb) {
        int mid = cb.capacity() >>> 1;
        int start = RAND.nextInt(mid + 1); // from 0 to mid
        int end = mid + RAND.nextInt(cb.capacity() - mid + 1); // from mid to capacity
        cb.position(start);
        cb.limit(end);
        return cb;
    }

    /**
     * Randomize the char buffer's contents, position and limit.
     */
    static CharBuffer randomize(CharBuffer cb) {
        while (cb.hasRemaining()) {
            cb.put((char)RAND.nextInt());
        }
        return randomizeRange(cb);
    }

    /**
     * Sums the remaining chars in the char buffer.
     */
    static int intSum(CharBuffer cb) {
        int sum = 0;
        cb.mark();
        while (cb.hasRemaining()) {
            sum += cb.get();
        }
        cb.reset();
        return sum;
    }

    /**
     * Creates char buffers to test, adding them to the given list.
     */
    static void addCases(CharBuffer cb, List<CharBuffer> buffers) {
        randomize(cb);
        buffers.add(cb);

        buffers.add(cb.slice());
        buffers.add(cb.duplicate());
        buffers.add(cb.asReadOnlyBuffer());

        buffers.add(randomizeRange(cb.slice()));
        buffers.add(randomizeRange(cb.duplicate()));
        buffers.add(randomizeRange(cb.asReadOnlyBuffer()));
    }

    @DataProvider(name = "charbuffers")
    public Object[][] createCharBuffers() {
        List<CharBuffer> buffers = new ArrayList<>();

        // heap
        addCases(CharBuffer.allocate(SIZE), buffers);
        addCases(CharBuffer.wrap(new char[SIZE]), buffers);
        addCases(ByteBuffer.allocate(SIZE*2).order(ByteOrder.BIG_ENDIAN).asCharBuffer(),
                 buffers);
        addCases(ByteBuffer.allocate(SIZE*2).order(ByteOrder.LITTLE_ENDIAN).asCharBuffer(),
                 buffers);

        // direct
        addCases(ByteBuffer.allocateDirect(SIZE*2).order(ByteOrder.BIG_ENDIAN).asCharBuffer(),
                 buffers);
        addCases(ByteBuffer.allocateDirect(SIZE*2).order(ByteOrder.LITTLE_ENDIAN).asCharBuffer(),
                 buffers);

        // read-only buffer backed by a CharSequence
        buffers.add(CharBuffer.wrap(randomize(CharBuffer.allocate(SIZE))));

        Object[][] params = new Object[buffers.size()][];
        for (int i = 0; i < buffers.size(); i++) {
            CharBuffer cb = buffers.get(i);
            params[i] = new Object[] { cb.getClass().getName(), cb };
        }

        return params;
    }

    @Test(dataProvider = "charbuffers")
    public void testChars(String type, CharBuffer cb) {
        System.out.format("%s position=%d, limit=%d%n", type, cb.position(), cb.limit());
        int expected = intSum(cb);
        assertEquals(cb.chars().sum(), expected);
        assertEquals(cb.chars().parallel().sum(), expected);
    }
}
