/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4926314 8266014
 * @summary Test for Reader#read(CharBuffer).
 * @run testng ReadCharBuffer
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.io.IOException;
import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Objects;

import static org.testng.Assert.assertEquals;

@Test(groups = "unit")
public class ReadCharBuffer {

    private static final int BUFFER_SIZE = 8 + 8192 + 2;

    @DataProvider(name = "buffers")
    public Object[][] createBuffers() {
        // test both on-heap and off-heap buffers as they make use different code paths
        return new Object[][]{
                new Object[]{CharBuffer.allocate(BUFFER_SIZE)},
                new Object[]{ByteBuffer.allocateDirect(BUFFER_SIZE * 2).asCharBuffer()}
        };
    }

    @Test(dataProvider = "buffers")
    public void read(CharBuffer buffer) throws IOException {
        fillBuffer(buffer);

        StringBuilder input = new StringBuilder(BUFFER_SIZE - 2 + 1);
        input.append("ABCDEF");
        for (int i = 0; i < 8192; i++) {
            input.append('y');
        }
        input.append("GH");

        try (Reader reader = new UnoptimizedStringReader(input.toString())) {
            // put only between position and limit in the target buffer
            int limit = 1 + 6;
            buffer.limit(limit);
            buffer.position(1);
            assertEquals(reader.read(buffer), 6);
            assertEquals(buffer.position(), limit);
            assertEquals(buffer.limit(), limit);

            // read the full temporary buffer
            // and then accurately reduce the next #read call
            limit = 8 + 8192 + 1;
            buffer.limit(8 + 8192 + 1);
            buffer.position(8);
            assertEquals(reader.read(buffer), 8192 + 1);
            assertEquals(buffer.position(), limit);
            assertEquals(buffer.limit(), limit);

            assertEquals(reader.read(), 'H');
            assertEquals(reader.read(), -1);
        }

        buffer.clear();
        StringBuilder expected = new StringBuilder(BUFFER_SIZE);
        expected.append("xABCDEFx");
        for (int i = 0; i < 8192; i++) {
            expected.append('y');
        }
        expected.append("Gx");
        assertEquals(buffer.toString(), expected.toString());
    }

    @Test
    public void readZeroLength() {
        char[] buf = new char[] {1, 2, 3};
        BufferedReader r = new BufferedReader(new CharArrayReader(buf));
        int n = -1;
        try {
            n = r.read(CharBuffer.allocate(0));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertEquals(n, 0);
    }

    private void fillBuffer(CharBuffer buffer) {
        char[] filler = new char[buffer.remaining()];
        Arrays.fill(filler, 'x');
        buffer.put(filler);
        buffer.clear();
    }

    /**
     * Unoptimized version of StringReader in case StringReader overrides
     * #read(CharBuffer)
     */
    static final class UnoptimizedStringReader extends Reader {

        private String str;
        private int next = 0;

        UnoptimizedStringReader(String s) {
            this.str = s;
        }

        @Override
        public int read() throws IOException {
            synchronized (lock) {
                if (next >= str.length())
                    return -1;
                return str.charAt(next++);
            }
        }

        @Override
        public int read(char cbuf[], int off, int len) throws IOException {
            synchronized (lock) {
                Objects.checkFromIndexSize(off, len, cbuf.length);
                if (next >= str.length())
                    return -1;
                int n = Math.min(str.length() - next, len);
                str.getChars(next, next + n, cbuf, off);
                next += n;
                return n;
            }
        }

        @Override
        public void close() throws IOException {

        }
    }

}
