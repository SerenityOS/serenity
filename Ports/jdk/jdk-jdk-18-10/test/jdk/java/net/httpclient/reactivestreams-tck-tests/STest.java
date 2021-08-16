/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class STest {

    @DataProvider(name = "bufferSizes")
    public static Object[][] bufferSizes() {
        return new Object[][]{
                { 1},
                { 2},
                { 3},
                { 4},
                {16},
                {17},
        };
    }

    @DataProvider
    public static Object[][] inputStream() {
        return new Object[][] {
                {  0,   1},
                {  1,   2},
                {  1,   3},
                {  1,   4},
                {  2,   1},
                {  2,   2},
                {  2,   3},
                {  2,   4},
                {  2,  13},
                {  3,   1},
                {  3,   2},
                {  3,   3},
                {  3,   4},
                {  3,  17},
                {  4,   1},
                {  4,   2},
                {  4,   3},
                {  4,   4},
                {  4,   5},
                { 13,   1},
                { 13,   2},
                { 13,  13},
                { 16,  18},
                { 17,   2},
                {255,   1},
                {256, 255},
                {257, 267},
        };
    }

    @Test
    public void testScatter0() {
        List<ByteBuffer> buffers = S.scatterBuffer(
                ByteBuffer.allocate(0));
        assertEquals(buffers.size(), 0);
    }

    @Test(dataProvider = "bufferSizes")
    public void testScatterN(int n) {
        final ByteBuffer src = S.bufferOfNRandomBytes(n);
        final int srcLength = src.remaining();
        ByteBuffer copy = ByteBuffer.wrap(Arrays.copyOf(src.array(),
                                                        src.array().length));
        List<ByteBuffer> buffers = S.scatterBuffer(src);
        int m = 0;
        for (ByteBuffer b : buffers) {
            m += b.remaining();
            while (b.hasRemaining() & copy.hasRemaining()) {
                assertEquals(b.get(), copy.get());
            }
        }
        assertEquals(m, srcLength);
    }

    @Test(dataProvider = "inputStream")
    public void testInputStreamOfNReads(int n, int capacity) throws IOException {
        InputStream s = S.inputStreamOfNReads(n);
        int count = 0;
        byte[] b = new byte[capacity];
        while (s.read(b) != -1) {
            count++;
        }
        assertEquals(count, n);
        assertTrue(s.read() == -1);
        assertTrue(s.read(b) == -1);
    }
}
