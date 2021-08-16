/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test CompressIndexes
 * @author Jean-Francois Denise
 * @modules java.base/jdk.internal.jimage.decompressor
 * @run main CompressIndexesTest
 */

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import jdk.internal.jimage.decompressor.CompressIndexes;

public class CompressIndexesTest {

    public static void main(String[] args) throws IOException {
        new CompressIndexesTest().test();
    }

    public void test() throws IOException {
        int[] data = {
                // compressed length 1
                0x00000000,
                0x00000001,
                0x0000000F,
                0x0000001F,
                // compressed length 2
                0x0000002F,
                0x00000100,
                0x00001FFF,
                // compressed length 3
                0x00002FFF,
                0x00010000,
                0x001FFFFF,
                // compressed length 4
                0x00200000,
                0x01000000,
                Integer.MAX_VALUE
        };
        int[] intervals = {4, 7, 10, data.length};
        List<byte[]> arrays = new ArrayList<>();
        int length = 0;
        int begin = 0;
        for (int interval : intervals) {
            ++length;
            for (int j = begin; j < interval; ++j) {
                arrays.add(check(data[j], length));
            }
            begin = interval;
        }

        int totalLength = arrays.stream().mapToInt(b -> b.length).sum();
        ByteBuffer all = ByteBuffer.allocate(totalLength);
        arrays.forEach(all::put);
        byte[] flow = all.array();
        check(flow, arrays);
        System.err.println(arrays.size() * 4 + " compressed in " + flow.length
                + " gain of " + (100 - ((flow.length * 100) / (arrays.size() * 4))) + "%");
        try (DataInputStream is = new DataInputStream(new ByteArrayInputStream(flow))) {
            int index = 0;
            while (is.available() > 0) {
                int d = CompressIndexes.readInt(is);
                if (data[index] != d) {
                    throw new AssertionError("Expected: " + data[index] + ", got: " + d);
                }
                ++index;
            }
        }
    }

    private void check(byte[] flow, List<byte[]> arrays) {
        List<Integer> d = CompressIndexes.decompressFlow(flow);
        List<Integer> dd = new ArrayList<>();
        for (byte[] b : arrays) {
            int i = CompressIndexes.decompress(b, 0);
            dd.add(i);
        }
        if (!d.equals(dd)) {
            System.err.println(dd);
            System.err.println(d);
            throw new AssertionError("Invalid flow " + d);
        } else {
            System.err.println("OK for flow");
        }
    }

    private byte[] check(int val, int size) {
        byte[] c = CompressIndexes.compress(val);
        if (c.length != size) {
            throw new AssertionError("Invalid compression size " + c.length);
        }
        int d = CompressIndexes.decompress(c, 0);
        if (val != d) {
            throw new AssertionError("Invalid " + d);
        } else {
            System.err.println("Ok for " + val);
        }
        return c;
    }
}
