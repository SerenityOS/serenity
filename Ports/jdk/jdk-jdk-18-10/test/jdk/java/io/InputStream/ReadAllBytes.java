/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;
import jdk.test.lib.RandomFactory;

/*
 * @test
 * @bug 8080835 8193832
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main ReadAllBytes
 * @summary Basic test for InputStream.readAllBytes
 * @key randomness
 */

public class ReadAllBytes {

    private static Random generator = RandomFactory.getRandom();

    public static void main(String[] args) throws IOException {
        test(new byte[]{});
        test(new byte[]{1, 2, 3});
        test(createRandomBytes(1024));
        for (int shift : new int[] {13, 14, 15, 17}) {
            for (int offset : new int[] {-1, 0, 1}) {
                test(createRandomBytes((1 << shift) + offset));
            }
        }
    }

    static void test(byte[] expectedBytes) throws IOException {
        int expectedLength = expectedBytes.length;
        WrapperInputStream in = new WrapperInputStream(new ByteArrayInputStream(expectedBytes));
        byte[] readBytes = in.readAllBytes();

        int x;
        byte[] tmp = new byte[10];
        check((x = in.read()) == -1,
              "Expected end of stream from read(), got " + x);
        check((x = in.read(tmp)) == -1,
              "Expected end of stream from read(byte[]), got " + x);
        check((x = in.read(tmp, 0, tmp.length)) == -1,
              "Expected end of stream from read(byte[], int, int), got " + x);
        check(in.readAllBytes().length == 0,
              "Expected readAllBytes to return empty byte array");
        check(expectedLength == readBytes.length,
              "Expected length " + expectedLength + ", got " + readBytes.length);
        check(Arrays.equals(expectedBytes, readBytes),
              "Expected[" + expectedBytes + "], got:[" + readBytes + "]");
        check(!in.isClosed(), "Stream unexpectedly closed");
    }

    static byte[] createRandomBytes(int size) {
        byte[] bytes = new byte[size];
        generator.nextBytes(bytes);
        return bytes;
    }

    static void check(boolean cond, Object ... failedArgs) {
        if (cond)
            return;
        StringBuilder sb = new StringBuilder();
        for (Object o : failedArgs)
            sb.append(o);
        throw new RuntimeException(sb.toString());
    }

    static class WrapperInputStream extends FilterInputStream {
        private boolean closed;
        WrapperInputStream(InputStream in) { super(in); }
        @Override public void close() throws IOException { closed = true; in.close(); }
        boolean isClosed() { return closed; }
    }
}
