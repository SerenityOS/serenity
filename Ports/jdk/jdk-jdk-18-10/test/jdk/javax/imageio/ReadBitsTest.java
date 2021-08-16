/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4416800
 * @summary Checks that ImageInputStreamImpl.readBit and readBits handle the bit
 *          offset correctly
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;

public class ReadBitsTest {
    public static void main(String[] args) throws IOException {
        byte[] buffer = new byte[] {(byte)169, (byte)85}; // 10101001 01010101
        InputStream ins = new ByteArrayInputStream(buffer);
        ImageInputStream in = new FileCacheImageInputStream(ins,null);

        if (in.getBitOffset() != 0) {
            throw new RuntimeException("Initial bit offset != 0!");
        }

        int bit0 = in.readBit(); // 1
        if (bit0 != 1) {
            throw new RuntimeException("First bit != 1");
        }
        if (in.getBitOffset() != 1) {
            throw new RuntimeException("Second bit offset != 1");
        }

        long bits1 = in.readBits(5); // 01010 = 10
        if (bits1 != 10) {
            throw new RuntimeException("Bits 1-5 != 10 (= " + bits1 + ")");
        }
        if (in.getBitOffset() != 6) {
            throw new RuntimeException("Third bit offset != 6");
        }

        int bit1 = in.readBit(); // 0
        if (bit1 != 0) {
            throw new RuntimeException("Bit 6 != 0");
        }
        if (in.getBitOffset() != 7) {
            throw new RuntimeException("Third bit offset != 7");
        }

        long bits2 = in.readBits(8); // 10101010 = 170
        if (bits2 != 170) {
            throw new RuntimeException("Bits 7-14 != 170 (= " + bits2 + ")");
        }
        if (in.getBitOffset() != 7) {
            throw new RuntimeException("Fourth bit offset != 7");
        }

        int bit2 = in.readBit(); // 1
        if (bit2 != 1) {
            throw new RuntimeException("Bit 15 != 1");
        }
        if (in.getBitOffset() != 0) {
            throw new RuntimeException("Fifth bit offset != 0");
        }

        in.close();
    }
}
